package com.kaze.devicefp.model;

import android.util.Log;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MemoryInfoReader {
    private static final String TAG = "MemoryInfoReader";
    private static final String MEMINFO_PATH = "/proc/meminfo";

    private Map<String, Long> memoryData = new HashMap<>();

    public boolean readMemoryInfo() {
        try (BufferedReader reader = new BufferedReader(new FileReader(MEMINFO_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                parseMemoryLine(line);
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "读取内存信息失败", e);
            return false;
        }
    }

    private void parseMemoryLine(String line) {
        // 格式: "MemTotal:       15538492 kB"
        String[] parts = line.split("\\s+");
        if (parts.length >= 2) {
            String key = parts[0].replace(":", "");
            String value = parts[1];
            try {
                memoryData.put(key, Long.parseLong(value));
            } catch (NumberFormatException e) {
                Log.w(TAG, "无法解析数值: " + line);
            }
        }
    }

    // 获取各种内存信息
    public long getTotalMemory() {
        return memoryData.getOrDefault("MemTotal", 0L);
    }

    public long getAvailableMemory() {
        return memoryData.getOrDefault("MemAvailable", 0L);
    }

    public long getFreeMemory() {
        return memoryData.getOrDefault("MemFree", 0L);
    }

    public long getCachedMemory() {
        return memoryData.getOrDefault("Cached", 0L);
    }

    public long getSwapTotal() {
        return memoryData.getOrDefault("SwapTotal", 0L);
    }

    public long getSwapFree() {
        return memoryData.getOrDefault("SwapFree", 0L);
    }

    public long getUsedMemory() {
        long total = getTotalMemory();
        long free = getFreeMemory();
        long buffers = memoryData.getOrDefault("Buffers", 0L);
        long cached = getCachedMemory();

        return total - free - buffers - cached;
    }

    public double getMemoryUsagePercentage() {
        long total = getTotalMemory();
        long used = getUsedMemory();

        if (total > 0) {
            return (double) used / total * 100;
        }
        return 0;
    }

    public double getAvailableMemoryPercentage() {
        long total = getTotalMemory();
        long available = getAvailableMemory();

        if (total > 0) {
            return (double) available / total * 100;
        }
        return 0;
    }

    // 获取详细内存报告
    public String getMemoryReport() {
        StringBuilder report = new StringBuilder();

        report.append("=== 内存使用报告 ===\n\n");

        // 物理内存
        long totalKB = getTotalMemory();
        long usedKB = getUsedMemory();
        long freeKB = getFreeMemory();
        long availableKB = getAvailableMemory();
        long cachedKB = getCachedMemory();

        report.append("物理内存:\n");
        report.append(String.format("  总计: %.2f GB\n", kbToGB(totalKB)));
        report.append(String.format("  已用: %.2f GB (%.1f%%)\n",
                kbToGB(usedKB), getMemoryUsagePercentage()));
        report.append(String.format("  空闲: %.2f GB\n", kbToGB(freeKB)));
        report.append(String.format("  可用: %.2f GB (%.1f%%)\n",
                kbToGB(availableKB), getAvailableMemoryPercentage()));
        report.append(String.format("  缓存: %.2f GB\n\n", kbToGB(cachedKB)));

        // 交换空间
        long swapTotalKB = getSwapTotal();
        long swapFreeKB = getSwapFree();
        long swapUsedKB = swapTotalKB - swapFreeKB;

        if (swapTotalKB > 0) {
            double swapUsage = (double) swapUsedKB / swapTotalKB * 100;
            report.append("交换空间:\n");
            report.append(String.format("  总计: %.2f GB\n", kbToGB(swapTotalKB)));
            report.append(String.format("  已用: %.2f GB (%.1f%%)\n",
                    kbToGB(swapUsedKB), swapUsage));
            report.append(String.format("  空闲: %.2f GB\n\n", kbToGB(swapFreeKB)));
        }

        // 详细缓存信息
        long buffersKB = memoryData.getOrDefault("Buffers", 0L);
        long sReclaimableKB = memoryData.getOrDefault("SReclaimable", 0L);
        long slabKB = memoryData.getOrDefault("Slab", 0L);

        report.append("缓存详情:\n");
        report.append(String.format("  Buffers: %.2f MB\n", kbToMB(buffersKB)));
        report.append(String.format("  Cached: %.2f MB\n", kbToMB(cachedKB)));
        report.append(String.format("  Slab缓存: %.2f MB\n", kbToMB(slabKB)));
        report.append(String.format("  可回收Slab: %.2f MB\n\n", kbToMB(sReclaimableKB)));

        // 内核内存
        long kernelStackKB = memoryData.getOrDefault("KernelStack", 0L);
        long pageTablesKB = memoryData.getOrDefault("PageTables", 0L);

        report.append("内核内存:\n");
        report.append(String.format("  内核栈: %.2f MB\n", kbToMB(kernelStackKB)));
        report.append(String.format("  页表: %.2f MB\n\n", kbToMB(pageTablesKB)));

        // Android特定内存
        long ionUsedKB = memoryData.getOrDefault("IonTotalUsed", 0L);
        long gpuUsedKB = memoryData.getOrDefault("GPUTotalUsed", 0L);

        if (ionUsedKB > 0 || gpuUsedKB > 0) {
            report.append("Android专用内存:\n");
            if (ionUsedKB > 0) {
                report.append(String.format("  ION内存: %.2f MB\n", kbToMB(ionUsedKB)));
            }
            if (gpuUsedKB > 0) {
                report.append(String.format("  GPU内存: %.2f MB\n", kbToMB(gpuUsedKB)));
            }
        }

        return report.toString();
    }

    private double kbToMB(long kb) {
        return kb / 1024.0;
    }

    private double kbToGB(long kb) {
        return kb / (1024.0 * 1024.0);
    }
    
    /**
     * 获取格式化的内存信息字符串（用于UI显示）
     * @return 格式化的内存信息
     */
    public String getFormattedMemoryInfo() {
        if (!readMemoryInfo()) {
            return "未获取";
        }
        
        StringBuilder info = new StringBuilder();
        
        // MemTotal
        long totalKB = getTotalMemory();
        if (totalKB > 0) {
            info.append("MemTotal: ").append(totalKB).append(" KB");
        } else {
            info.append("MemTotal: 不适用");
        }
        
        // MemAvailable
        long availableKB = getAvailableMemory();
        if (availableKB > 0) {
            info.append("\nMemAvailable: ").append(availableKB).append(" KB");
        } else {
            info.append("\nMemAvailable: 不适用");
        }
        
        // MemFree
        long freeKB = getFreeMemory();
        if (freeKB > 0) {
            info.append("\nMemFree: ").append(freeKB).append(" KB");
        } else {
            info.append("\nMemFree: 不适用");
        }
        
        // GPUTotalUsed
        long gpuUsedKB = memoryData.getOrDefault("GPUTotalUsed", 0L);
        if (gpuUsedKB > 0) {
            info.append("\nGPUTotalUsed: ").append(gpuUsedKB).append(" KB");
        } else {
            info.append("\nGPUTotalUsed: 不适用");
        }
        
        return info.toString();
    }

    // 获取JSON格式数据
    public String getMemoryInfoJson() {
        return String.format(
                "{\"totalMB\": %.2f, \"usedMB\": %.2f, \"availableMB\": %.2f, " +
                        "\"usagePercent\": %.1f, \"availablePercent\": %.1f, " +
                        "\"cachedMB\": %.2f, \"swapTotalMB\": %.2f, \"swapUsedMB\": %.2f}",
                kbToMB(getTotalMemory()),
                kbToMB(getUsedMemory()),
                kbToMB(getAvailableMemory()),
                getMemoryUsagePercentage(),
                getAvailableMemoryPercentage(),
                kbToMB(getCachedMemory()),
                kbToMB(getSwapTotal()),
                kbToMB(getSwapTotal() - getSwapFree())
        );
    }

    // 监控内存变化
    public static class MemoryMonitor {
        private MemoryInfoReader reader;
        private Map<String, Long> lastValues = new HashMap<>();

        public MemoryMonitor() {
            this.reader = new MemoryInfoReader();
        }

        public boolean checkMemoryChanges() {
            if (!reader.readMemoryInfo()) {
                return false;
            }

            Map<String, Long> currentValues = reader.memoryData;
            boolean hasChanges = false;

            for (Map.Entry<String, Long> entry : currentValues.entrySet()) {
                String key = entry.getKey();
                long current = entry.getValue();
                long last = lastValues.getOrDefault(key, 0L);

                if (Math.abs(current - last) > 1024) { // 变化超过1MB
                    Log.d(TAG, String.format("%s: %d kB -> %d kB (变化: %d kB)",
                            key, last, current, current - last));
                    hasChanges = true;
                }
            }

            lastValues = new HashMap<>(currentValues);
            return hasChanges;
        }
    }
}
