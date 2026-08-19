package com.zhbohdanchykov;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;

public class ExecutorServiceManager<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorServiceManager.class);

    public static final int TIMEOUT = 600;

    private final List<ExecutorServiceManagerEntry<T>> entries;

    public ExecutorServiceManager(List<ExecutorServiceManagerEntry<T>> entries) {
        this.entries = entries;
    }

    public void launch() {
        for (ExecutorServiceManagerEntry<T> entry : entries) {
            launchPool(entry.tasks(), entry.executorService(), entry.results());
        }
    }

    private void launchPool(List<? extends Callable<T>> tasks, ExecutorService pool,
                            List<Future<T>> results) {
        tasks.forEach(task -> results.add(pool.submit(task)));
    }

    public void terminate() {
        for (ExecutorServiceManagerEntry<T> entry : entries) {
            shutdownGraceful(entry.executorService());
        }
    }

    private void shutdownGraceful(ExecutorService pool) {
        pool.shutdown();

        try {
            if (!pool.awaitTermination(TIMEOUT, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(TIMEOUT, TimeUnit.SECONDS)) {
                    LOGGER.error("{} pool did not terminate.", pool);
                }
            }
        } catch (InterruptedException e) {
            LOGGER.error("Shutdown of {} was interrupted.", pool);
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}