package org.tavall.couriers.api.concurrent;


import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;

public class AsyncTask {

    private static final AtomicLong VT_COUNTER = new AtomicLong();


    private AsyncTask() {

    }


    /**
     * Configuration knobs for the scope. Mirrors what the JDK exposes: thread factory, name, timeout. :contentReference[oaicite:5]{index=5}
     */
    public record ScopeOptions(
            ThreadFactory threadFactory,
            String name,
            Duration timeout
    ) {
        public static ScopeOptions defaults() {

            return new ScopeOptions(null, null, null);
        }


        public ScopeOptions withThreadFactory(ThreadFactory tf) {

            return new ScopeOptions(tf, name, timeout);
        }


        public ScopeOptions withName(String name) {

            return new ScopeOptions(threadFactory, name, timeout);
        }


        public ScopeOptions withTimeout(Duration timeout) {

            return new ScopeOptions(threadFactory, name, timeout);
        }
    }

    /**
     * Outcome per task, in fork order (index matches input order).
     */
    public record Outcome<T>(
            int index,
            StructuredTaskScope.Subtask.State state,
            T result,
            Throwable error
    ) {
    }

    /**
     * Batch result, even when failures happen (unless you choose to throw).
     */
    public record BatchResult<T>(
            List<Outcome<T>> outcomes,
            boolean cancelled,
            boolean timedOut
    ) {
        public List<T> successes() {

            return outcomes.stream()
                    .filter(o -> o.state == StructuredTaskScope.Subtask.State.SUCCESS)
                    .map(Outcome::result)
                    .toList();
        }


        public List<Throwable> failures() {

            return outcomes.stream()
                    .filter(o -> o.state == StructuredTaskScope.Subtask.State.FAILED)
                    .map(Outcome::error)
                    .toList();
        }


        public boolean hasFailures() {

            return outcomes.stream().anyMatch(o -> o.state == StructuredTaskScope.Subtask.State.FAILED);
        }


        public Throwable firstFailureOrNull() {

            return outcomes.stream()
                    .filter(o -> o.state == StructuredTaskScope.Subtask.State.FAILED)
                    .map(Outcome::error)
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * Thrown when you choose "throwOnFailure=true" for batch runs.
     * Carries the BatchResult so you can still inspect partial outcomes.
     */
    public static final class BatchFailedException extends Exception {
        private final BatchResult<?> result;


        public BatchFailedException(String message, Throwable cause, BatchResult<?> result) {

            super(message, cause);
            this.result = result;
        }


        public BatchResult<?> result() {

            return result;
        }
    }


    /**
     * Runs one task "async inside, sync outside": fork in a new (virtual) thread, join, return result.
     * Uses Joiner.anySuccessfulResultOrThrow() (single task: result-or-throw). :contentReference[oaicite:6]{index=6}
     */
    public static <T> T runAsync(Callable<? extends T> task, ScopeOptions options)
            throws InterruptedException, StructuredTaskScope.TimeoutException, StructuredTaskScope.FailedException {

        Objects.requireNonNull(task, "task");
        ScopeOptions opt = (options == null) ? ScopeOptions.defaults() : options;

        try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.<T>anySuccessfulResultOrThrow(), configFn(opt))) {
            scope.fork(task::call);
            return scope.join();
        }
    }


    public static <T> T runAsync(Callable<? extends T> task)
            throws InterruptedException, StructuredTaskScope.TimeoutException, StructuredTaskScope.FailedException {

        return runAsync(task, ScopeOptions.defaults());
    }


    /**
     * Runs tasks concurrently and returns results, FAIL-FAST style (throws if any task fails).
     * This is the clean "all must succeed" policy. :contentReference[oaicite:7]{index=7}
     */
    public static <T> List<T> runMultipleAsync(Collection<? extends Callable<? extends T>> tasks, ScopeOptions options)
            throws InterruptedException, StructuredTaskScope.TimeoutException, StructuredTaskScope.FailedException {

        Objects.requireNonNull(tasks, "tasks");
        if (tasks.isEmpty()) return List.of();
        ScopeOptions opt = (options == null) ? ScopeOptions.defaults() : options;

        try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.<T>allSuccessfulOrThrow(), configFn(opt))) {
            for (var task : tasks) {
                scope.fork(task::call);
            }
            // join returns a stream of subtasks in fork order when all succeed :contentReference[oaicite:8]{index=8}
            return scope.join().map(StructuredTaskScope.Subtask::get).toList();
        }
    }


    public static <T> List<T> runMultipleAsync(Collection<? extends Callable<? extends T>> tasks)
            throws InterruptedException, StructuredTaskScope.TimeoutException, StructuredTaskScope.FailedException {

        return runMultipleAsync(tasks, ScopeOptions.defaults());
    }


    /**
     * The "boolean knobs" batch runner:
     * <p>
     * cancelAfterFailures:
     * - 0 => never cancel early (wait for all)
     * - 1 => cancel on first failure (fail-fast cancellation)
     * - N => cancel after N failures
     * <p>
     * throwOnFailure:
     * - true => throw BatchFailedException if any FAILED outcomes exist
     * - false => always return BatchResult
     * <p>
     * Uses Joiner.allUntil(predicate): cancels scope when predicate returns true, and still yields all subtasks
     * (some may be UNAVAILABLE). :contentReference[oaicite:9]{index=9}
     */
    public static <T> BatchResult<T> runMultipleAsync(
            List<? extends Callable<? extends T>> tasks,
            ScopeOptions options,
            int cancelAfterFailures,
            boolean throwOnFailure
    ) throws InterruptedException, StructuredTaskScope.TimeoutException, BatchFailedException {

        Objects.requireNonNull(tasks, "tasks");
        if (tasks.isEmpty()) return new BatchResult<>(List.of(), false, false);

        ScopeOptions opt = (options == null) ? ScopeOptions.defaults() : options;
        int threshold = Math.max(0, cancelAfterFailures);

        AtomicInteger failureCount = new AtomicInteger(0);
        Predicate<StructuredTaskScope.Subtask<? extends T>> cancelPredicate = subtask -> {
            if (threshold == 0) return false;
            if (subtask.state() == StructuredTaskScope.Subtask.State.FAILED) {
                return failureCount.incrementAndGet() >= threshold;
            }
            return false;
        };

        List<StructuredTaskScope.Subtask<T>> forked = new ArrayList<>(tasks.size());
        StructuredTaskScope.TimeoutException timeoutEx = null;

        try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.<T>allUntil(cancelPredicate), configFn(opt))) {
            for (var task : tasks) {
                @SuppressWarnings("unchecked")
                Callable<T> cast = (Callable<T>) task;
                forked.add(scope.fork(cast));
            }

            List<StructuredTaskScope.Subtask<T>> finishedInOrder;
            try {
                // yields all subtasks in fork order; may include UNAVAILABLE if cancelled :contentReference[oaicite:10]{index=10}
                finishedInOrder = scope.join().toList();
            } catch (StructuredTaskScope.TimeoutException te) {
                // Timeout cancels the scope and join throws. :contentReference[oaicite:11]{index=11}
                timeoutEx = te;
                finishedInOrder = forked;
            }

            var outcomes = new ArrayList<Outcome<T>>(finishedInOrder.size());
            for (int i = 0; i < finishedInOrder.size(); i++) {
                StructuredTaskScope.Subtask<T> st = finishedInOrder.get(i);
                StructuredTaskScope.Subtask.State state = st.state();
                T value = null;
                Throwable err = null;

                if (state == StructuredTaskScope.Subtask.State.SUCCESS) {
                    value = st.get();
                } else if (state == StructuredTaskScope.Subtask.State.FAILED) {
                    err = st.exception();
                } else {
                    // UNAVAILABLE: not completed, completed after cancellation, or never started :contentReference[oaicite:12]{index=12}
                }

                outcomes.add(new Outcome<>(i, state, value, err));
            }

            BatchResult<T> result = new BatchResult<>(outcomes, scope.isCancelled(), timeoutEx != null);

            if (throwOnFailure && result.hasFailures()) {
                throw new BatchFailedException(
                        "One or more subtasks failed",
                        result.firstFailureOrNull(),
                        result
                );
            }

            if (timeoutEx != null) {
                // If you want "timeouts always throw", do it at the call-site. Returning structured info is useful.
                throw timeoutEx;
            }

            return result;
        }
    }


    /**
     * "First success wins" helper: cancels the other subtasks when one succeeds. :contentReference[oaicite:13]{index=13}
     */
    public static <T> T runAnySuccessAsync(Collection<? extends Callable<? extends T>> tasks, ScopeOptions options)
            throws InterruptedException, StructuredTaskScope.TimeoutException, StructuredTaskScope.FailedException {

        Objects.requireNonNull(tasks, "tasks");
        if (tasks.isEmpty()) throw new IllegalArgumentException("tasks must not be empty");

        ScopeOptions opt = (options == null) ? ScopeOptions.defaults() : options;

        try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.<T>anySuccessfulResultOrThrow(), configFn(opt))) {
            for (var task : tasks) {
                scope.fork(task::call);
            }
            return scope.join();
        }
    }


    // Non-lock
    public static <T> CompletableFuture<T> runFuture(Callable<? extends T> task, ScopeOptions options) {

        Objects.requireNonNull(task, "task");
        ScopeOptions opt = (options == null) ? ScopeOptions.defaults() : options;

        CompletableFuture<T> future = new CompletableFuture<>();

        newThread(opt.name != null ? opt.name : "async-task", () -> {
            try {
                // Important: scope owner thread must be the same thread that calls open/join/close.
                T value = runAsync(task, opt); // your existing blocking structured method
                future.complete(value);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });

        return future;
    }


    public static <T> CompletableFuture<T> runFuture(Callable<? extends T> task) {

        return runFuture(task, ScopeOptions.defaults());
    }


    public static Thread newThread(String baseName, Runnable runnable) {

        String name = baseName + "-" + VT_COUNTER.incrementAndGet();
        return Thread.ofVirtual().name(name).start(runnable);
    }


    public static String unwrapMessage(Throwable ex) {

        Throwable t = ex;
        while (t.getCause() != null && t != t.getCause()) t = t.getCause();
        return (t.getMessage() != null) ? t.getMessage() : t.getClass().getSimpleName();
    }


    private static Function<StructuredTaskScope.Configuration, StructuredTaskScope.Configuration> configFn(ScopeOptions opt) {

        return cfg -> {
            StructuredTaskScope.Configuration c = cfg;
            if (opt.threadFactory != null) c = c.withThreadFactory(opt.threadFactory);
            if (opt.name != null) c = c.withName(opt.name);
            if (opt.timeout != null) c = c.withTimeout(opt.timeout);
            return c;
        };
    }
}