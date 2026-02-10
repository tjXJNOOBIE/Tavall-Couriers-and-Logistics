package org.tavall.couriers.api.concurrent;


import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class TestAsyncTask {
    private static final Logger log = Logger.getLogger(TestAsyncTask.class.getName());

    @Test
    void runAsync_returnsValue_andRunsOnVirtualThreadByDefault() throws Exception {
        log.info("runAsync_returnsValue_andRunsOnVirtualThreadByDefault");

        String value = AsyncTask.runAsync(() -> {
            log.info("Task thread: " + Thread.currentThread());
            assertTrue(Thread.currentThread().isVirtual(), "Expected a virtual thread by default");
            return "ok";
        });

        assertEquals("ok", value);
    }

    @Test
    void runAsync_timesOut() {
        log.info("runAsync_timesOut");

        var opts = AsyncTask.ScopeOptions.defaults()
                .withTimeout(Duration.ofMillis(50));

        StructuredTaskScope.TimeoutException ex = assertThrows(
                StructuredTaskScope.TimeoutException.class,
                () -> AsyncTask.runAsync(() -> {
                    log.info("Sleeping inside task to force timeout");
                    sleepMillis(500);
                    return "never";
                }, opts)
        );

        log.info("Caught expected timeout: " + ex);
    }

    @Test
    void runMultipleAsync_returnsResultsInForkOrder() throws Exception {
        log.info("runMultipleAsync_returnsResultsInForkOrder");

        List<Callable<Integer>> tasks = List.of(
                () -> { sleepMillis(120); log.info("Task 0 done"); return 0; },
                () -> { sleepMillis(20);  log.info("Task 1 done"); return 1; },
                () -> { sleepMillis(60);  log.info("Task 2 done"); return 2; }
        );

        List<Integer> results = AsyncTask.runMultipleAsync(tasks);

        assertEquals(List.of(0, 1, 2), results);
    }

    @Test
    void runMultipleAsync_throwsOnFailure_failFastStyle() {
        log.info("runMultipleAsync_throwsOnFailure_failFastStyle");

        List<Callable<Integer>> tasks = List.of(
                () -> { sleepMillis(30); return 1; },
                () -> { throw new IllegalStateException("boom"); },
                () -> { sleepMillis(300); return 3; }
        );

        StructuredTaskScope.FailedException ex = assertThrows(
                StructuredTaskScope.FailedException.class,
                () -> AsyncTask.runMultipleAsync(tasks)
        );

        log.info("Caught expected FailedException: " + ex);
    }

    @Test
    void batchMode_returnsOutcomes_whenThrowOnFailureFalse() throws Exception {
        log.info("batchMode_returnsOutcomes_whenThrowOnFailureFalse");

        List<Callable<Integer>> tasks = List.of(
                () -> 10,
                () -> { throw new RuntimeException("nope"); },
                () -> 30
        );

        BatchResult<Integer> result = AsyncTask.runMultipleAsync(
                tasks,
                AsyncTask.ScopeOptions.defaults(),
                0,      // cancelAfterFailures=0 means "never cancel early"
                false   // throwOnFailure=false means "always return outcomes"
        );

        log.info("Cancelled: " + result.cancelled() + ", TimedOut: " + result.timedOut());
        result.outcomes().forEach(o ->
                log.info("Outcome idx=" + o.index() + " state=" + o.state()
                        + " result=" + o.result() + " error=" + o.error())
        );

        assertTrue(result.hasFailures());
        assertEquals(2, result.successes().size());
        assertEquals(1, result.failures().size());

        assertEquals(StructuredTaskScope.Subtask.State.SUCCESS, result.outcomes().get(0).state());
        assertEquals(StructuredTaskScope.Subtask.State.FAILED, result.outcomes().get(1).state());
        assertEquals(StructuredTaskScope.Subtask.State.SUCCESS, result.outcomes().get(2).state());
    }

    @Test
    void batchMode_cancelsAfterFirstFailure_producesUnavailableOutcomes() throws Exception {
        log.info("batchMode_cancelsAfterFirstFailure_producesUnavailableOutcomes");

        List<Callable<Integer>> tasks = List.of(
                () -> { throw new RuntimeException("fail fast"); },
                () -> { sleepMillis(500); return 2; },
                () -> { sleepMillis(500); return 3; }
        );

        BatchResult<Integer> result = AsyncTask.runMultipleAsync(
                tasks,
                AsyncTask.ScopeOptions.defaults(),
                1,      // cancelAfterFailures=1 -> cancel once first failure is observed
                false
        );

        log.info("Cancelled: " + result.cancelled() + ", TimedOut: " + result.timedOut());
        result.outcomes().forEach(o ->
                log.info("Outcome idx=" + o.index() + " state=" + o.state()
                        + " result=" + o.result() + " error=" + o.error())
        );

        assertTrue(result.hasFailures());

        boolean sawUnavailable = result.outcomes().stream()
                .anyMatch(o -> o.state() == StructuredTaskScope.Subtask.State.UNAVAILABLE);

        // Depending on timing, slow tasks may be UNAVAILABLE after cancellation kicks in.
        assertTrue(sawUnavailable, "Expected at least one UNAVAILABLE subtask after cancellation");
    }

    @Test
    void runAnySuccessAsync_returnsFirstSuccessfulResult() throws Exception {
        log.info("runAnySuccessAsync_returnsFirstSuccessfulResult");

        List<Callable<String>> tasks = List.of(
                () -> { throw new RuntimeException("replica down"); },
                () -> { sleepMillis(20);  log.info("fast success"); return "fast"; },
                () -> { sleepMillis(200); log.info("slow success"); return "slow"; }
        );

        String winner = AsyncTask.runAnySuccessAsync(tasks,
                AsyncTask.ScopeOptions.defaults().withTimeout(Duration.ofSeconds(1)));

        log.info("Winner: " + winner);
        assertEquals("fast", winner);
    }

    private static void sleepMillis(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // If cancellation happens, this is normal. Re-assert interrupt so structured cancellation behaves.
            Thread.currentThread().interrupt();
        }
    }
    @Test
    void runFuture_completesSuccessfully_defaultOptions() throws Exception {
        log.info("runFuture_completesSuccessfully_defaultOptions");

        CompletableFuture<String> f = AsyncTask.runFuture(() -> {
            log.info("Task thread: {}" + Thread.currentThread());
            assertTrue(Thread.currentThread().isVirtual(), "Expected task to run on a virtual thread (scope fork)");
            return "ok";
        });

        String result = f.get(2, TimeUnit.SECONDS);
        assertEquals("ok", result);
    }

    @Test
    void runFuture_completesSuccessfully_withCustomOptions() throws Exception {
        log.info("runFuture_completesSuccessfully_withCustomOptions");

        var opts = AsyncTask.ScopeOptions.defaults()
                .withName("unit-test-runFuture")
                .withTimeout(Duration.ofSeconds(5));

        CompletableFuture<Integer> f = AsyncTask.runFuture(() -> 123, opts);
        Integer result = f.get(2, TimeUnit.SECONDS);
        assertEquals(123, result);
    }

    @Test
    void runFuture_nullOptionsFallsBackToDefaults() throws Exception {
        log.info("runFuture_nullOptionsFallsBackToDefaults");

        CompletableFuture<String> f = AsyncTask.runFuture(() -> "ok", null);

        String result = f.get(2, TimeUnit.SECONDS);
        assertEquals("ok", result);
    }

    @Test
    void runFuture_completesExceptionally_whenTaskThrows() throws ExecutionException, InterruptedException {
        log.info("runFuture_completesExceptionally_whenTaskThrows");

        CompletableFuture<String> f = AsyncTask.runFuture(() -> {
            throw new IllegalStateException("boom");
        });

        ExecutionException ex = assertThrows(ExecutionException.class, () -> f.get(2, TimeUnit.SECONDS));
        assertNotNull(ex.getCause());
        log.info(ex.getCause() +"");
        assertEquals("java.lang.IllegalStateException: boom", ex.getCause().getMessage());
    }

    @Test
    void runFuture_rejectsNullTask() {
        log.info("runFuture_rejectsNullTask");

        assertThrows(NullPointerException.class, () -> AsyncTask.runFuture(null));
        assertThrows(NullPointerException.class, () -> AsyncTask.runFuture(null, AsyncTask.ScopeOptions.defaults()));
    }

    @Test
    void newThread_createsVirtualThread_withNamePrefix() throws Exception {
        log.info("newThread_createsVirtualThread_withNamePrefix");

        CompletableFuture<Thread> seen = new CompletableFuture<>();

        Thread t = AsyncTask.newThread("scan-worker", () -> {
            Thread current = Thread.currentThread();
            log.info("NewThread running as: {}" + current);
            seen.complete(current);
        });

        Thread running = seen.get(2, TimeUnit.SECONDS);

        assertTrue(running.isVirtual(), "Expected virtual thread from Thread.ofVirtual()");
        assertNotNull(running.getName());
        assertTrue(running.getName().startsWith("scan-worker-"),
                "Expected name to start with 'scan-worker-' but was: " + running.getName());

        // t should be the same instance as running (it is the thread we started)
        assertSame(t, running);
    }

    @Test
    void unwrapMessage_unwrapsDeepestCauseMessage() {
        log.info("unwrapMessage_unwrapsDeepestCauseMessage");

        Throwable deep = new RuntimeException("deep-message");
        Throwable mid = new IllegalArgumentException("mid-message", deep);
        Throwable top = new RuntimeException("top-message", mid);

        String msg = AsyncTask.unwrapMessage(top);
        assertEquals("deep-message", msg);
    }

    @Test
    void unwrapMessage_fallsBackToClassNameIfMessageNull() {
        log.info("unwrapMessage_fallsBackToClassNameIfMessageNull");

        Throwable deep = new NullPointerException(); // message usually null
        Throwable top = new RuntimeException(deep);

        String msg = AsyncTask.unwrapMessage(top);

        // If NPE message is null, it should return "NullPointerException"
        assertEquals("NullPointerException", msg);
    }
}

