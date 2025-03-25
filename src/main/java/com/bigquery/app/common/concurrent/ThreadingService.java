package com.bigquery.app.common.concurrent;

import com.bigquery.app.backup.dto.ExportOptions;
import com.bigquery.app.restore.dto.ImportOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThreadingService {
    private final ThreadPoolTaskExecutor backupTaskExecutor;
    private final ThreadPoolTaskExecutor restoreTaskExecutor;

    public void configureThreadPoolForExport(ExportOptions exportOptions) {
        var threadPoolSize = exportOptions.threadPoolSize();
        var queueCapacity = exportOptions.threadQueueCapacity();

        log.info("Configuring backup thread pool with size: {}, queue capacity: {}",
                threadPoolSize, queueCapacity);

        backupTaskExecutor.setCorePoolSize(threadPoolSize);
        backupTaskExecutor.setMaxPoolSize(threadPoolSize);

        Optional.ofNullable(queueCapacity)
                .ifPresent(backupTaskExecutor::setQueueCapacity);
    }

    public void configureThreadPoolForImport(ImportOptions importOptions) {
        var threadPoolSize = importOptions.threadPoolSize();
        var queueCapacity = importOptions.threadQueueCapacity();

        log.info("Configuring restore thread pool with size: {}, queue capacity: {}",
                threadPoolSize, queueCapacity);

        restoreTaskExecutor.setCorePoolSize(threadPoolSize);
        restoreTaskExecutor.setMaxPoolSize(threadPoolSize);

        Optional.ofNullable(queueCapacity)
                .ifPresent(restoreTaskExecutor::setQueueCapacity);
    }

    public CompletableFuture<Void> submitBackupTask(Runnable task) {
        return CompletableFuture.runAsync(task, backupTaskExecutor);
    }

    public CompletableFuture<Void> submitRestoreTask(Runnable task) {
        return CompletableFuture.runAsync(task, restoreTaskExecutor);
    }
}
