package me.vagdedes.spartan.system;

import me.vagdedes.spartan.Register;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Threads {

    private static final Map<ExecutorService, Future<?>> accessed = new ConcurrentHashMap<>();
    private static boolean enabled = true;

    static void disable() {
        enabled = false;

        if (accessed.size() > 0) {
            Iterator<Map.Entry<ExecutorService, Future<?>>> iterator = accessed.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<ExecutorService, Future<?>> entry = iterator.next();
                entry.getValue().cancel(true);
                entry.getKey().shutdownNow();
                iterator.remove();
            }
        }
    }

    static ExecutorService create() {
        return Executors.newFixedThreadPool(1);
    }

    private static boolean execute(ExecutorService service, Runnable runnable, boolean force, boolean executeRegardless) {
        if (enabled) {
            Future<?> future = accessed.get(service);

            if (future != null && !future.isDone() && !force) { // In-use always first to not lose tracking
                if (executeRegardless) {
                    runnable.run();
                }
                return false;
            }
            accessed.put(service,
                    service.submit(() -> {
                        if (enabled && Register.isPluginEnabled()) {
                            runnable.run();
                        }
                    })
            );
            return true;
        }
        return false;
    }

    public static boolean executeIfFreeElseHere(ExecutorService service, Runnable runnable) {
        return execute(service, runnable, false, true);
    }

    public static boolean executeIfFree(ExecutorService service, Runnable runnable) {
        return execute(service, runnable, false, false);
    }

    public static boolean execute(ExecutorService service, Runnable runnable) {
        return execute(service, runnable, true, false);
    }
}
