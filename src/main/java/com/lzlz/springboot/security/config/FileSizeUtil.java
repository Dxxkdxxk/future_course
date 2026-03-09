package com.lzlz.springboot.security.config;

/**
 * 文件大小格式化工具类
 */
public class FileSizeUtil {
    private static final double KB = 1024.0;
    private static final double MB = 1024.0 * 1024.0;
    private static final double GB = 1024.0 * 1024.0 * 1024.0;

    public static String formatFileSize(Long bytes) {
        if (bytes == null || bytes == 0) {
            return "0 B";
        }
        if (bytes < KB) {
            return bytes + " B";
        } else if (bytes < MB) {
            return String.format("%.2f KB", bytes / KB);
        } else if (bytes < GB) {
            return String.format("%.2f MB", bytes / MB);
        } else {
            return String.format("%.2f GB", bytes / GB);
        }
    }
}
