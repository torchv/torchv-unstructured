package com.torchv.infra.unstructured.job;

import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ypc add
 * 文件清理管理器，用于定时清理临时文件
 */
@Slf4j
public class FileCleanupManager {
    
    private static final FileCleanupManager INSTANCE = new FileCleanupManager();
    
    // 清理队列，存储待删除的文件
    private final BlockingQueue<File> cleanupQueue = new LinkedBlockingQueue<>();
    
    // 线程池用于执行清理任务
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // 控制清理任务运行状态
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    // 清理任务
    private ScheduledFuture<?> cleanupTask;
    
    private FileCleanupManager() {
        // 不在构造函数中启动任务，而是在添加第一个文件时启动
    }
    
    public static FileCleanupManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 添加文件到清理队列
     * @param file 待清理的文件
     */
    public void addToCleanupQueue(File file) {
        if (file != null && file.exists()) {
            cleanupQueue.offer(file);
            log.info("文件已添加到清理队列: {}", file.getAbsolutePath());
            
            // 在添加第一个文件时启动清理任务
            startCleanupTaskIfNeeded();
        }
    }
    
    /**
     * 在需要时启动清理任务（仅当任务未启动时）
     */
    private void startCleanupTaskIfNeeded() {
        if (running.compareAndSet(false, true)) {
            // 每30秒执行一次清理任务
            cleanupTask = scheduler.scheduleAtFixedRate(this::performCleanup, 0, 30, TimeUnit.SECONDS);
            log.info("文件清理任务已启动");
        }
    }
    
    /**
     * 执行清理操作
     */
    private void performCleanup() {
        log.info("开始执行文件清理任务，队列中有 {} 个文件等待清理", cleanupQueue.size());
        
        int successCount = 0;
        int failCount = 0;
        File file;
        
        // 批量处理，每次最多处理100个文件
        int batchSize = 100;
        for (int i = 0; i < batchSize; i++) {
            file = cleanupQueue.poll();
            if (file == null) {
                break;
            }
            
            if (deleteFileWithRetry(file, 3)) {
                successCount++;
            } else {
                failCount++;
                // 如果删除失败，重新加入队列
                if (file.exists()) {
                    cleanupQueue.offer(file);
                    log.warn("文件删除失败，重新加入队列: {}", file.getAbsolutePath());
                }
            }
        }
        
        log.info("本次清理任务完成: 成功 {} 个，失败 {} 个，剩余 {} 个文件待清理",
                successCount, failCount, cleanupQueue.size());
    }
    
    /**
     * 带重试机制的文件删除
     * @param file 要删除的文件
     * @param maxRetries 最大重试次数
     * @return 是否删除成功
     */
    private boolean deleteFileWithRetry(File file, int maxRetries) {
        int retryCount = 0;
        while (retryCount < maxRetries) {
            try {
                if (file.exists() && file.delete()) {
                    log.debug("文件删除成功: {}", file.getAbsolutePath());
                    return true;
                } else if (!file.exists()) {
                    // 文件已经不存在，视为删除成功
                    return true;
                }
            } catch (Exception e) {
                log.warn("删除文件时发生异常: {}, 重试次数: {}/{}",
                        file.getAbsolutePath(), retryCount + 1, maxRetries, e);
            }
            
            retryCount++;
            if (retryCount < maxRetries) {
                try {
                    Thread.sleep(100 * retryCount); // 递增延迟重试
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        log.warn("文件删除失败，已达到最大重试次数: {}", file.getAbsolutePath());
        return false;
    }
    
    /**
     * 立即执行一次清理（用于系统关闭前）
     */
    public void cleanupImmediately() {
        log.info("执行立即清理，队列中有 {} 个文件", cleanupQueue.size());
        File file;
        while ((file = cleanupQueue.poll()) != null) {
            deleteFileWithRetry(file, 2);
        }
    }
    
    /**
     * 关闭清理管理器
     */
    public void shutdown() {
        if (running.compareAndSet(true, false)) {
            // 先执行一次立即清理
            cleanupImmediately();
            
            if (cleanupTask != null) {
                cleanupTask.cancel(false);
            }
            
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            
            log.info("文件清理管理器已关闭");
        }
    }
    
    /**
     * 获取队列中待清理文件数量
     * @return 文件数量
     */
    public int getPendingFileCount() {
        return cleanupQueue.size();
    }
}
