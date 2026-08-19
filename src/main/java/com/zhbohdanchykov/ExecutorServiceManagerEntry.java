package com.zhbohdanchykov;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public record ExecutorServiceManagerEntry<T>(ExecutorService executorService,
                                             List<? extends Callable<T>> tasks,
                                             List<Future<T>> results) {
}
