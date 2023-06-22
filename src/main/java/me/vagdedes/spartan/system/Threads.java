package me.vagdedes.spartan.system;

import java.util.LinkedList;
import java.util.Queue;

public class Threads {

    // Object

    public static class ThreadPool {

        private final Queue<Runnable> runnables;

        public ThreadPool() {
            runnables = new LinkedList<>();
            new Thread(() -> {
                while (enabled) {
                    Runnable runnable = runnables.poll();

                    if (runnable != null) {
                        runnable.run();
                    }
                }
            }).start();
        }

        private synchronized boolean execute(Runnable runnable, boolean force, boolean executeRegardless) {
            if (!force) {
                Runnable current = runnables.peek();

                if (current != null) {
                    if (executeRegardless) {
                        runnable.run();
                    }
                    return false;
                }
            }
            return runnables.add(runnable);
        }

        public boolean executeIfFreeElseHere(Runnable runnable) {
            return execute(runnable, false, true);
        }

        public boolean executeIfFree(Runnable runnable) {
            return execute(runnable, false, false);
        }

        public boolean execute(Runnable runnable) {
            return execute(runnable, true, false);
        }

    }

    // Methods

    private static boolean enabled = true;

    static void disable() {
        enabled = false;
    }
}
