/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.couriers.api.console;



import org.tavall.couriers.api.console.style.LogColors;
import org.tavall.couriers.api.console.style.LogText;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Log {
    private static final BlockingQueue<String> asyncQueue = new LinkedBlockingQueue<>();
    private static final Thread logThread;

    static {
        logThread = new Thread(() -> {
            while (true) {
                try {
                    String message = asyncQueue.take();
                    System.out.println(message);
                } catch (InterruptedException ignored) {
                }
            }
        }, "LogThread");
        logThread.setDaemon(true);
        logThread.start();
    }

    public static void success(String msg) {
        log("[SUCCESS] ", LogColors.GREEN, msg);
    }

    public static void success(LogText text) {
        success(text.build());
    }

    public static void info(String msg) {
        log("[INFO] ", LogColors.WHITE, msg);
    }

    public static void info(LogText text) {
        info(text.build());
    }

    public static void warn(String msg) {
        log("[WARN] ", LogColors.YELLOW, msg);
    }

    public static void warn(LogText text) {
        warn(text.build());
    }

    public static void error(String msg) {
        log("[ERROR] ", LogColors.RED, msg);
    }

    public static void error(LogText text) {
        error(text.build());
    }

    public static void critical(String msg) {
        log("[CRITICAL] ", LogColors.WHITE, LogColors.bgRed(msg));
    }

    public static void critical(LogText text) {
        critical(text.build());
    }

    public static LogText text() {
        return LogText.create();
    }

    private static void log(String level, String color, String msg) {
        String processed = LogColors.applyPlaceholders(msg);
        String output = color + level + " " + processed + LogColors.RESET;
        asyncQueue.offer(output);
    }

    public static void exception(Throwable t) {
        if (t == null) {
            error("Exception: No exception to log, is it null? ");
            return;
        }

        Throwable root = rootCause(t);
        StackTraceElement[] stack = t.getStackTrace();
        StackTraceElement appTop = firstAppFrame(stack);

        // Main exception info
        error("Exception: " + t.getClass().getName() + (t.getMessage() != null ? ": " + t.getMessage() : ""));
        if (appTop != null) {
            error("Source: " + formatFrame(appTop));
        } else if (stack.length > 0) {
            error("Source: " + formatFrame(stack[0]));
        }

        // Cause chain with first app frame per cause
        if (root != t) {
            error("Cause chain:");
            Throwable current = t.getCause();
            while (current != null) {
                StackTraceElement causeAppFrame = firstAppFrame(current.getStackTrace());
                String causeInfo = current.getClass().getName() +
                        (current.getMessage() != null ? ": " + current.getMessage() : "");
                if (causeAppFrame != null) {
                    error("  -> " + causeInfo + " at " + formatFrame(causeAppFrame));
                } else {
                    error("  -> " + causeInfo);
                }
                current = current.getCause();
                if (current == root) break; // avoid infinite loops
            }
        }

        // App stack with metadata-aware verbosity
        int printed = 0;
        error("App stack:");
        Set<Class<?>> culpritClasses = new HashSet<>();

        for (StackTraceElement ste : stack) {
            if (isAppFrame(ste)) {
                error("  at " + formatFrame(ste));

                // Track culprit classes for summary
                try {
                    Class<?> frameClass = Class.forName(ste.getClassName());
                    culpritClasses.add(frameClass);
                } catch (ClassNotFoundException ignored) {
                }

                if (++printed >= 8) break;
            }
        }

        if (printed == 0) {
            int lim = Math.min(5, stack.length);
            for (int i = 0; i < lim; i++) {
                error("  at " + formatFrame(stack[i]));
            }
        }
        // Culprit summary using metadata
//        if (!culpritClasses.isEmpty()) {
//            error("Culprit summary (classes in exception):");
//            for (Class<?> clazz : culpritClasses) {
//                if (getAbstractClassMetaDataInstance() != null) {
//                    error("  " + clazz.getSimpleName() +
//                          " (registrations=" +   getRegistrationCount() +
//                          ", instantiations=" +   getInstantiationCount() +
//                          ", dependencies=" +   getAttachedDependencies().size() + ")");
//                } else {
//                    error("  " + clazz.getSimpleName() + " (metadata unavailable)");
//                }
//            }
//        }
    }

    private static boolean isAppFrame(StackTraceElement ste) {
        String cn = ste.getClassName();
        return cn.startsWith("com.tjxjnoobie.") || cn.startsWith("com.tjxnjoobie");
    }

    private static String formatFrame(StackTraceElement ste) {
        String file = ste.getFileName();
        int line = ste.getLineNumber();
        return ste.getClassName() + "." + ste.getMethodName() + "(" + (file != null ? file : "Unknown Source") + (line >= 0 ? ":" + line : "") + ")";
    }

    private static StackTraceElement firstAppFrame(StackTraceElement[] stack) {
        if (stack == null) return null;
        for (StackTraceElement ste : stack) {
            if (isAppFrame(ste)) return ste;
        }
        return null;
    }

    private static Throwable rootCause(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur;
    }
    //TODO: Move method logic to concerned location



}