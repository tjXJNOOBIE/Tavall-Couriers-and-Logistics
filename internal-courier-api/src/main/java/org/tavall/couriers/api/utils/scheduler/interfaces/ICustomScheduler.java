/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.couriers.api.utils.scheduler.interfaces;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;

public interface ICustomScheduler {



    /**
     * Run multi-threaded task later
     */
    default ScheduledFuture<?> runTaskLaterAsync(Runnable task, long delayMs) {
        return null;
    }

    /**
     * Run single-threaded task later
     */
    default ScheduledFuture<?> runTaskLater(Runnable task, long delayMs) {
        return null;
    }

    default ScheduledFuture<?> runTaskLaterAsync(Callable<?> task, long delayMs) {
        return null;
    }

    /**
     * Run single-threaded task later
     */
    default ScheduledFuture<?> runTaskLater(Callable<?> task, long delayMs) {
        return null;
    }

    /**
     * Repeat async task
     */
    default ScheduledFuture<?> runTaskRepeatingAsync(Runnable task, long delayMs, long periodMs) {
        return null;
    }

    /**
     * Repeat sync-like task
     */
    default ScheduledFuture<?> runTaskRepeating(Runnable task, long delayMs, long periodMs) {
        return null;
    }

    default void shutdown() {
    }

    default void shutdownGracefully(long timeoutMs) {
    }

    default boolean cancelTask(ScheduledFuture<?> task) {
        return false;
    }

    default void cancelAllTasks() {
    }

    default void removeTask(ScheduledFuture<?> t){

    }
}