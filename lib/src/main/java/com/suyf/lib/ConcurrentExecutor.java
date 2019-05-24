package com.suyf.lib;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentExecutor {

  private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
  private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;

  private static ExecutorService sExecutorService;

  public synchronized static ExecutorService get() {
    if (sExecutorService == null) {
      sExecutorService = new ThreadPoolExecutor(0,
          MAXIMUM_POOL_SIZE,
          60L,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<Runnable>(128),
          create("lib_thread", false));
    }
    return sExecutorService;
  }

  static ThreadFactory create(final String name, final boolean daemon) {
    return new ThreadFactory() {
      private final AtomicInteger mCount = new AtomicInteger(1);

      @Override
      public Thread newThread(Runnable runnable) {
        Thread result = new Thread(runnable, name + "#" + mCount.getAndIncrement());
        result.setPriority(Thread.MIN_PRIORITY);
        result.setDaemon(daemon);
        return result;
      }
    };
  }
}
