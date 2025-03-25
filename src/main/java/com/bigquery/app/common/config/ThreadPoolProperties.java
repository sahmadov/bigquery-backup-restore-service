package com.bigquery.app.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@ConfigurationProperties(prefix = "threadpool")
@EnableAsync
@Data
public class ThreadPoolProperties {

    private ThreadPoolConfig backup = new ThreadPoolConfig();
    private ThreadPoolConfig restore = new ThreadPoolConfig();

    @Data
    public static class ThreadPoolConfig {
        private int coreSize = 1;
        private int maxSize = 10;
        private int queueCapacity = 25;
        private String namePrefix = "task-";
    }

    @Bean(name = "backupTaskExecutor")
    public ThreadPoolTaskExecutor backupTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(backup.getCoreSize());
        executor.setMaxPoolSize(backup.getMaxSize());
        executor.setQueueCapacity(backup.getQueueCapacity());
        executor.setThreadNamePrefix(backup.getNamePrefix());
        executor.initialize();
        return executor;
    }

    @Bean(name = "restoreTaskExecutor")
    public ThreadPoolTaskExecutor restoreTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(restore.getCoreSize());
        executor.setMaxPoolSize(restore.getMaxSize());
        executor.setQueueCapacity(restore.getQueueCapacity());
        executor.setThreadNamePrefix(restore.getNamePrefix());
        executor.initialize();
        return executor;
    }
}