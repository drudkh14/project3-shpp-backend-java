package com.zhbohdanchykov;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ExecutorServiceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorServiceManager.class);
    public static final int TIMEOUT = 600;

    private final ExecutorService producerPool;
    private final ExecutorService consumerPool;
    private final ExecutorService writerPool;

    private final List<Future<Integer>> producerResults = new ArrayList<>();
    private final List<Future<Integer>> consumerResults = new ArrayList<>();
    private final List<Future<Integer>> writerResults = new ArrayList<>();

    public ExecutorServiceManager(int threadsNumber) {
        this.producerPool = Executors.newFixedThreadPool(threadsNumber);
        this.consumerPool = Executors.newFixedThreadPool(threadsNumber);
        this.writerPool = Executors.newFixedThreadPool(2);
    }

    public void launch(ArrayList<Producer> producers, ArrayList<Consumer> consumers, ArrayList<WriterCSV> writers) {
        launchPool(producers, producerPool, producerResults);
        launchPool(consumers, consumerPool, consumerResults);
        launchPool(writers, writerPool, writerResults);
    }

    private void launchPool(ArrayList<? extends Callable<Integer>> tasks, ExecutorService pool,
                            List<Future<Integer>> results) {
        tasks.forEach(task -> results.add(pool.submit(task)));
    }

    public void terminate() {
        shutdownGraceful(producerPool);
        shutdownGraceful(consumerPool);
        shutdownGraceful(writerPool);
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

    public ProcessingResults getResults() {
        int totalMessagesSent = getResults(producerResults);
        int totalMessagesReceived = getResults(consumerResults);
        int totalMessagesWritten = getResults(writerResults);

        return new ProcessingResults(totalMessagesSent, totalMessagesReceived, totalMessagesWritten);
    }

    private int getResults(List<Future<Integer>> results) {
        int res = 0;

        for (Future<Integer> future : results) {
            try {
                res += future.get();
            } catch (InterruptedException e) {
                LOGGER.error("Failed getting result from {} because of interruption.", results);
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                LOGGER.error("Task from {} was failed.", results, e.getCause());
            }
        }

        return res;
    }
}
