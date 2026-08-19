package com.zhbohdanchykov;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;

public class ExecutorServiceManager<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorServiceManager.class);

    private final List<ExecutorServiceManagerEntry<T>> entries;
    private final int timeout;

    public ExecutorServiceManager(List<ExecutorServiceManagerEntry<T>> entries, int timeout) {
        this.entries = entries;
        this.timeout = timeout;
    }

    public void launch() {
        for (ExecutorServiceManagerEntry<T> entry : entries) {
            launchPool(entry.executorService(), entry.tasks(), entry.results());
        }
    }

    private void launchPool(ExecutorService pool, List<? extends Callable<T>> tasks,
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
            if (!pool.awaitTermination(timeout, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(timeout, TimeUnit.SECONDS)) {
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