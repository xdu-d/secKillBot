package com.xdud.seckillbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ThreadPoolConfig {

    /** 多账号并发下单线程池 */
    @Bean("taskExecutorPool")
    public ThreadPoolExecutor taskExecutorPool() {
        int coreSize = Runtime.getRuntime().availableProcessors() * 2;
        return new ThreadPoolExecutor(
                coreSize,
                coreSize * 4,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(500),
                new NamedThreadFactory("task-executor"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /** 调度线程池 */
    @Bean("schedulerPool")
    public ThreadPoolExecutor schedulerPool() {
        return new ThreadPoolExecutor(
                4,
                8,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                new NamedThreadFactory("precision-scheduler"),
                new ThreadPoolExecutor.DiscardPolicy()
        );
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(0);
        private final String prefix;

        NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    }
}
