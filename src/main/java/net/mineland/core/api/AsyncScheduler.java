package net.mineland.core.api;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.mineland.core.utils.Try;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.lang.reflect.Field;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Общий асинхронный пул потоков
 */
public class AsyncScheduler {

    private static final Logger log = Logger.getLogger(AsyncScheduler.class.getName());
    /**
     * Если задача будет выполняться дольше указанного количества
     * миллисекунд, тогда об этом будет выведено сообщение в лог.
     */
    public static int STOP_WATCH_TIME_MILLIS = 500;

    // свои пулы потоков, потому что в банге/bukkit они созданы без ограничения, что плохо
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(32,
                    new ThreadFactoryBuilder().setNameFormat("Schedule Libs Thread %d").build());


    /**
     * Остановить кастомный пул потоков
     */
    public static void shutdown() {
        Try.ignore(() -> scheduler.shutdownNow());
    }

    private AsyncScheduler() {

    }

    /**
     * Запустить таск асинхронно
     *
     * <p>Задача будет обернута в {@link DecoratedRunnable}.
     */
    public static Future<?> run(Runnable runnable) {
        return scheduler.submit(new DecoratedRunnable(runnable));
    }

    /**
     * Запустить таск асинхронно с возращаемым результатом
     *
     * <p>Задача будет обернута в {@link DecoratedCallable}.
     */
    public static <T> Future<T> run(Callable<T> callable) {
        return scheduler.submit(new DecoratedCallable<>(callable));
    }

    /**
     * Выполнить позже
     *
     * <p>Задача будет обернута в {@link DecoratedRunnable}.
     *
     * @param runnable такс
     * @param delay    через сколько выполнить
     * @param time     тип времени
     */
    public static ScheduledFuture<?> later(Runnable runnable, long delay, TimeUnit time) {
        return scheduler.schedule(new DecoratedRunnable(runnable), delay, time);
    }

    /**
     * Запустить таймер
     *
     * <p>Задача будет обернута в {@link DecoratedRunnable}.
     *
     * @param runnable таск
     * @param delay    через сколько выполнить
     * @param period   через сколько повторять
     * @param time     тип времени
     */
    public static ScheduledFuture<?> timer(Runnable runnable, long delay, long period, TimeUnit time) {
        return scheduler.scheduleAtFixedRate(new DecoratedRunnable(runnable), delay, period, time);
    }

    public static void cancel(ScheduledFuture<?> timer) {
        try {
            if (timer != null) {
                timer.cancel(true);
            }
        } catch (Exception ignored) {
        }
    }

    public static ScheduledExecutorService getScheduler() {
        return AsyncScheduler.scheduler;
    }

    /**
     * Обертка для задачи типа {@link Runnable}.
     *
     * <p>Эта обертка имеет следующий функционал:
     * <ul>
     *     <li>Если задача будет выполняться дольше {@link #STOP_WATCH_TIME_MILLIS},
     *     тогда об этом будет выдано сообщение в лог.</li>
     *     <li>Если задача завершится с ошибкой, ошибки не будут выброшены, кроме {@link InterruptedException}.
     *     Об ошибке будет выведено сообщение в лог.</li>
     *     <li>Будет применен декоратор {@link #hotfixDecorator}.</li>
     * </ul>
     */
    public static class DecoratedRunnable implements Runnable {
        // Нужно для hotfix'ов, не удалять, даже если не используется
        private static Function<Runnable, Runnable> hotfixDecorator = runnable -> runnable;

        private final Runnable originalRunnable;
        private final Runnable decoratedRunnable;

        public DecoratedRunnable(Runnable originalRunnable) {
            this.originalRunnable = originalRunnable;
            this.decoratedRunnable = hotfixDecorator.apply(originalRunnable);
        }

        public static void setHotfixDecorator(Function<Runnable, Runnable> hotfixDecorator) {
            DecoratedRunnable.hotfixDecorator = hotfixDecorator;
        }

        @Override
        public void run() {
            long start = System.currentTimeMillis();
            try {
                decoratedRunnable.run();
            } catch (Throwable e) {
                log.severe("Ошибка во время выполнения асинхронной задачи " + AsyncScheduler.toString(originalRunnable));
                e.printStackTrace();

                // мы юзаем Lombok, у нас в такой ситуации возможен InterruptedException
                //noinspection ConstantConditions
                if (e instanceof InterruptedException) {
                    // выкидываем только InterruptedException
                    throw e;
                }
            } finally {
                long after = System.currentTimeMillis() - start;
                if (after > STOP_WATCH_TIME_MILLIS) {
                    log.warning("Долгая задача " + AsyncScheduler.toString(originalRunnable) + ", она выполнялась " + after + "ms.");
                }
            }
        }

        public String toString() {
            return "AsyncScheduler.DecoratedRunnable(originalRunnable=" + this.originalRunnable + ", decoratedRunnable=" + this.decoratedRunnable + ")";
        }
    }

    /**
     * Обертка для задачи типа {@link Callable}.
     *
     * <p>Эта обертка имеет следующий функционал:
     * <ul>
     *     <li>Если задача будет выполняться дольше {@link #STOP_WATCH_TIME_MILLIS},
     *     тогда об этом будет выдано сообщение в лог.</li>
     *     <li>Если задача завершится с ошибкой, об этом будет выведено сообщение в лог.</li>
     *     <li>Будет применен декоратор {@link #hotfixDecorator}.</li>
     * </ul>
     */
    public static class DecoratedCallable<T> implements Callable<T> {
        // Нужно для hotfix'ов, не удалять, даже если не используется
        private static Function<Callable<?>, Callable<?>> hotfixDecorator = callable -> callable;

        private final Callable<T> originalCallable;
        private final Callable<T> decoratedCallable;

        @SuppressWarnings("unchecked")
        public DecoratedCallable(Callable<T> originalCallable) {
            this.originalCallable = originalCallable;
            this.decoratedCallable = (Callable<T>) hotfixDecorator.apply(originalCallable);
        }

        public static void setHotfixDecorator(Function<Callable<?>, Callable<?>> hotfixDecorator) {
            DecoratedCallable.hotfixDecorator = hotfixDecorator;
        }

        @Override
        public T call() throws Exception {
            long start = System.currentTimeMillis();
            try {
                return decoratedCallable.call();
            } catch (Throwable e) {
                log.severe("Ошибка во время выполнения асинхронной задачи " + AsyncScheduler.toString(originalCallable));
                e.printStackTrace();
                throw e;
            } finally {
                long after = System.currentTimeMillis() - start;
                if (after > STOP_WATCH_TIME_MILLIS) {
                    log.warning("Долгая задача " + AsyncScheduler.toString(originalCallable) + ", она выполнялась " + after + "ms.");
                }
            }
        }

        public String toString() {
            return "AsyncScheduler.DecoratedCallable(originalCallable=" + this.originalCallable + ", decoratedCallable=" + this.decoratedCallable + ")";
        }
    }


    /**
     * Преобразовать объект в строку с информацией о его полях.
     */
    private static String toString(Object object) {
        ReflectionToStringBuilder toStringBuilder = new ReflectionToStringBuilder(object) {
            @Override
            protected boolean accept(Field field) {
                if (field.getName().indexOf(ClassUtils.INNER_CLASS_SEPARATOR_CHAR) != -1) {
                    // Переопределяем поведение метода accept по умолчанию,
                    // разрешаем отображать поля из анонимных классов.
                    return true;
                }
                return super.accept(field);
            }
        };
        return toStringBuilder.toString();
    }
}
