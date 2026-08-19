package com.zhbohdanchykov;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ExecutorServiceManagerTest {

    @Test
    void shouldSubmitAllTasks() throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        List<Future<Integer>> results = new ArrayList<>();

        List<Callable<Integer>> tasks = List.of(
                () -> 1,
                () -> 2,
                () -> 3
        );

        ExecutorServiceManagerEntry<Integer> entry = new ExecutorServiceManagerEntry<>(
                executorService,
                tasks,
                results
        );

        ExecutorServiceManager<Integer> manager = new ExecutorServiceManager<>(List.of(entry), 600);

        try {
            manager.launch();

            assertEquals(3, results.size());
            assertEquals(1, (int) results.get(0).get());
            assertEquals(2, (int) results.get(1).get());
            assertEquals(3, (int) results.get(2).get());
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void shouldSubmitAllTasksToAllPolls() throws ExecutionException, InterruptedException {
        ExecutorService executorService1 = Executors.newSingleThreadExecutor();
        ExecutorService executorService2 = Executors.newSingleThreadExecutor();

        List<Future<Integer>> results1 = new ArrayList<>();
        List<Future<Integer>> results2 = new ArrayList<>();

        ExecutorServiceManagerEntry<Integer> entry1 = new ExecutorServiceManagerEntry<>(
                executorService1,
                List.of(() -> 10, () -> 20),
                results1
        );
        ExecutorServiceManagerEntry<Integer> entry2 = new ExecutorServiceManagerEntry<>(
                executorService2,
                List.of(() -> 30),
                results2
        );

        ExecutorServiceManager<Integer> manager = new ExecutorServiceManager<>(List.of(entry1, entry2), 600);

        try {
            manager.launch();

            assertEquals(2, results1.size());
            assertEquals(1, results2.size());

            assertEquals(10, (int) results1.get(0).get());
            assertEquals(20, (int) results1.get(1).get());
            assertEquals(30, (int) results2.get(0).get());
        } finally {
            executorService1.shutdownNow();
            executorService2.shutdownNow();
        }
    }

    @Test
    void shouldShutdownExecutor() {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        ExecutorServiceManagerEntry<Integer> entry = new ExecutorServiceManagerEntry<>(
                executorService,
                List.of(),
                new ArrayList<>()
        );

        ExecutorServiceManager<Integer> manager = new ExecutorServiceManager<>(List.of(entry), 600);

        manager.terminate();
        assertTrue(executorService.isShutdown());
        assertTrue(executorService.isTerminated());
    }

    @Test
    void shouldCallShutDownNow() throws InterruptedException {
        ExecutorService executorService = mock(ExecutorService.class);
        when(executorService.awaitTermination(anyLong(), eq(TimeUnit.SECONDS))).
                thenReturn(false, true);

        ExecutorServiceManagerEntry<Integer> entry = new ExecutorServiceManagerEntry<>(
                executorService,
                List.of(),
                new ArrayList<>()
        );

        ExecutorServiceManager<Integer> manager = new ExecutorServiceManager<>(List.of(entry), 1);

        manager.terminate();

        verify(executorService).shutdown();
        verify(executorService).shutdownNow();
        verify(executorService, times(2)).awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    @SuppressWarnings("all")
    void shouldHandleInterruptionWhileShutdown() throws InterruptedException {
        ExecutorService executorService = mock(ExecutorService.class);
        when(executorService.awaitTermination(anyLong(), eq(TimeUnit.SECONDS))).thenThrow(InterruptedException.class);

        ExecutorServiceManagerEntry<Integer> entry = new ExecutorServiceManagerEntry<>(
                executorService,
                List.of(),
                new ArrayList<>()
        );
        ExecutorServiceManager<Integer> manager = new ExecutorServiceManager<>(List.of(entry), 1);

        manager.launch();

        try {
            manager.terminate();

            verify(executorService).shutdown();
            verify(executorService).shutdownNow();
            verify(executorService).awaitTermination(1, TimeUnit.SECONDS);

            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}