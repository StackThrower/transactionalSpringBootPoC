package com.example.transactional.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.core.task.AsyncTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "demoExecutor")
    public AsyncTaskExecutor demoExecutor() {
        SimpleAsyncTaskExecutor exec = new SimpleAsyncTaskExecutor("demo-");
        exec.setConcurrencyLimit(10);
        return exec;
    }
}

