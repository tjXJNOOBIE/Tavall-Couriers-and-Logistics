/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.couriers.api.utils.scheduler;


import org.tavall.couriers.api.utils.scheduler.interfaces.ICustomScheduler;

import java.util.Set;
import java.util.concurrent.*;

/**
 * CustomScheduler – TODO: implement class functionality
 * Auto-generated skeleton by MondayGPT-style template
 *
 * @author TJ
 * @since 11/15/2025
 */
public class CustomScheduler implements ICustomScheduler {

    private final Set<ScheduledFuture<?>> activeTasks =
            ConcurrentHashMap.newKeySet();
    private final int MULTI_THREADS =
            Math.max(2, Runtime.getRuntime().availableProcessors());

    // Single-thread executor (sync-like)
    private final ScheduledExecutorService SINGLE =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "CustomTaskRunner-Single");
                t.setDaemon(true);
                return t;
            });

    // Multi-thread executor (async)
    private final ScheduledThreadPoolExecutor MULTI =
            new ScheduledThreadPoolExecutor(MULTI_THREADS, r -> {
                Thread t = new Thread(r, "CustomTaskRunner-Multi");
                t.setDaemon(true);
                return t;
            });

    {
        MULTI.setRemoveOnCancelPolicy(true);
    }




    //=========== DELAYED TASKS ============\\

    /**
     * Run multi-threaded task later
     */
    public ScheduledFuture<?> runTaskLaterAsync(Runnable task, long delayMs) {
        ScheduledFuture<?> scheduledFuture = MULTI.schedule(task, delayMs, TimeUnit.MILLISECONDS);
        activeTasks.add(scheduledFuture);
        return scheduledFuture;
    }

    /**
     * Run single-threaded task later
     */
    public ScheduledFuture<?> runTaskLater(Runnable task, long delayMs) {
        ScheduledFuture<?> scheduledFuture = SINGLE.schedule(task, delayMs, TimeUnit.MILLISECONDS);
        activeTasks.add(scheduledFuture);
        return scheduledFuture;
    }

    public ScheduledFuture<?> runTaskLaterAsync(Callable<?> task, long delayMs) {
        ScheduledFuture<?> scheduledFuture = MULTI.schedule(task, delayMs, TimeUnit.MILLISECONDS);
        activeTasks.add(scheduledFuture);
        return scheduledFuture;
    }

    /**
     * Run single-threaded task later
     */
    public ScheduledFuture<?> runTaskLater(Callable<?> task, long delayMs) {
        ScheduledFuture<?> scheduledFuture = SINGLE.schedule(task, delayMs, TimeUnit.MILLISECONDS);
        activeTasks.add(scheduledFuture);
        return scheduledFuture;
    }

    // ------------------------------------------------------------
    // REPEATING TASKS
    // ------------------------------------------------------------

    /**
     * Repeat async task
     */
    public ScheduledFuture<?> runTaskRepeatingAsync(
            Runnable task, long delayMs, long periodMs) {
        ScheduledFuture<?> scheduledFuture = MULTI.scheduleAtFixedRate(task, delayMs, periodMs, TimeUnit.MILLISECONDS);
        activeTasks.add(scheduledFuture);
        return scheduledFuture;
    }

    /**
     * Repeat sync-like task
     */
    public ScheduledFuture<?> runTaskRepeating(
            Runnable task, long delayMs, long periodMs) {
        ScheduledFuture<?> scheduledFuture = SINGLE.scheduleAtFixedRate(task, delayMs, periodMs, TimeUnit.MILLISECONDS);
        activeTasks.add(scheduledFuture);
        return scheduledFuture;
    }

    //========== SHUTDOWN / CANCEL =========\\

    public void shutdown() {
        SINGLE.shutdownNow();
        MULTI.shutdownNow();
        activeTasks.clear();
    }

    public void shutdownGracefully(long timeoutMs) {
        SINGLE.shutdown();
        MULTI.shutdown();
        try {
            if (!SINGLE.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS)) {
                SINGLE.shutdownNow();
            }
            if (!MULTI.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS)) {
                MULTI.shutdownNow();
            }
        } catch (InterruptedException e) {
            SINGLE.shutdownNow();
            MULTI.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public boolean cancelTask(ScheduledFuture<?> task) {
        if (task == null) return false;
        boolean cancelled = task.cancel(false);
        activeTasks.remove(task);
        return cancelled;
    }

    public void cancelAllTasks() {
        for (ScheduledFuture<?> task : activeTasks) {
            task.cancel(false);
        }
        activeTasks.clear();
    }

    public void removeTask(ScheduledFuture<?> t) {
        activeTasks.remove(t);
    }
}