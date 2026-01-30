package com.kaze.devicefp.model;

import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * CPU信息读取器
 * 负责读取CPU相关的所有信息，包括核心数、频率等
 */
public class CpuInfoReader {
    private static final String CPU_INFO_PATH = "/proc/cpuinfo";
    private static final String TAG = "CpuInfoReader";
    
    /**
     * 获取CPU核心数
     * @return CPU核心数，如果获取失败返回-1
     */
    public int getCpuCoreCount() {
        try {
            // 方法1: 使用 Runtime.availableProcessors()
            int cores = Runtime.getRuntime().availableProcessors();
            if (cores > 0) {
                return cores;
            }
            
            // 方法2: 读取 /proc/cpuinfo
            String cpuInfo = readCpuInfoFile();
            if (cpuInfo != null && !cpuInfo.isEmpty()) {
                int count = parseCpuCoreCount(cpuInfo);
                if (count > 0) {
                    return count;
                }
            }
            
            // 方法3: 读取 /sys/devices/system/cpu/present
            File presentFile = new File("/sys/devices/system/cpu/present");
            if (presentFile.exists()) {
                String content = readFileContent(presentFile);
                if (content != null && !content.isEmpty()) {
                    // 格式通常是 "0-3" 表示 0,1,2,3 四个核心
                    if (content.contains("-")) {
                        String[] parts = content.split("-");
                        if (parts.length == 2) {
                            try {
                                int max = Integer.parseInt(parts[1].trim());
                                return max + 1; // 从0开始，所以+1
                            } catch (NumberFormatException e) {
                                // 忽略
                            }
                        }
                    } else {
                        // 单个数字
                        try {
                            return Integer.parseInt(content.trim()) + 1;
                        } catch (NumberFormatException e) {
                            // 忽略
                        }
                    }
                }
            }
            
            // 方法4: 遍历 /sys/devices/system/cpu/cpuX 目录
            int count = 0;
            for (int i = 0; i < 32; i++) { // 最多检查32个核心
                File cpuDir = new File("/sys/devices/system/cpu/cpu" + i);
                if (cpuDir.exists() && cpuDir.isDirectory()) {
                    count++;
                } else {
                    break;
                }
            }
            if (count > 0) {
                return count;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "获取CPU核心数失败", e);
        }
        return -1;
    }
    
    /**
     * CPU频率信息数据类
     */
    public static class CpuFrequencyData {
        public long curFreq;  // 当前频率 (kHz)
        public long maxFreq;  // 最大频率 (kHz)
        public long minFreq;  // 最小频率 (kHz)
        
        public CpuFrequencyData(long curFreq, long maxFreq, long minFreq) {
            this.curFreq = curFreq;
            this.maxFreq = maxFreq;
            this.minFreq = minFreq;
        }
        
        public boolean isValid() {
            return maxFreq > 0 && minFreq > 0 && curFreq >= 0;
        }
    }
    
    /**
     * 获取所有CPU核心的频率数据
     * @return CPU频率数据列表，索引对应核心编号
     */
    public java.util.List<CpuFrequencyData> getAllCpuFrequencyData() {
        java.util.List<CpuFrequencyData> frequencyList = new java.util.ArrayList<>();
        int cpuCores = getCpuCoreCount();
        
        if (cpuCores <= 0) {
            return frequencyList;
        }
        
        for (int i = 0; i < cpuCores; i++) {
            CpuFrequencyData data = getCpuCoreFrequencyData(i);
            if (data != null && data.isValid()) {
                frequencyList.add(data);
            } else {
                // 即使获取失败也添加一个无效数据，保持索引对应
                frequencyList.add(new CpuFrequencyData(0, 0, 0));
            }
        }
        
        return frequencyList;
    }
    
    /**
     * 获取单个CPU核心的频率数据（原始数值，单位kHz）
     * @param coreIndex CPU核心索引（从0开始）
     * @return CPU频率数据，如果获取失败返回null
     */
    public CpuFrequencyData getCpuCoreFrequencyData(int coreIndex) {
        try {
            String basePath = "/sys/devices/system/cpu/cpu" + coreIndex + "/cpufreq";
            File cpufreqDir = new File(basePath);
            
            if (!cpufreqDir.exists() || !cpufreqDir.isDirectory()) {
                return null;
            }
            
            // 读取频率文件（原始数值，单位kHz）
            // 实时频率：优先尝试 scaling_cur_freq，如果不存在再尝试 cpuinfo_cur_freq
            long curFreq = readFrequencyValue(new File(basePath, "scaling_cur_freq"));
            if (curFreq == 0) {
                curFreq = readFrequencyValue(new File(basePath, "cpuinfo_cur_freq"));
            }
            
            long maxFreq = readFrequencyValue(new File(basePath, "cpuinfo_max_freq"));
            if (maxFreq == 0) {
                maxFreq = readFrequencyValue(new File(basePath, "scaling_max_freq"));
            }
            
            long minFreq = readFrequencyValue(new File(basePath, "cpuinfo_min_freq"));
            if (minFreq == 0) {
                minFreq = readFrequencyValue(new File(basePath, "scaling_min_freq"));
            }
            
            if (maxFreq > 0 && minFreq > 0) {
                return new CpuFrequencyData(curFreq, maxFreq, minFreq);
            }
            
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "获取CPU核心 " + coreIndex + " 频率失败", e);
            return null;
        }
    }
    
    /**
     * 读取频率文件的原始数值（单位：kHz）
     * @param file 频率文件
     * @return 频率值（kHz），如果读取失败返回0
     */
    private long readFrequencyValue(File file) {
        try {
            if (!file.exists()) {
                return 0;
            }
            
            // 即使文件不可读，也尝试读取（某些系统文件可能没有读权限但可以读取）
            String content = readFileContent(file);
            if (content == null || content.trim().isEmpty()) {
                return 0;
            }
            
            try {
                long value = Long.parseLong(content.trim());
                if (value > 0) {
                    return value;
                }
            } catch (NumberFormatException e) {
                Log.d(TAG, "解析频率值失败: " + file.getPath() + ", content: " + content);
                return 0;
            }
            
        } catch (Exception e) {
            Log.d(TAG, "读取频率文件失败: " + file.getPath(), e);
            return 0;
        }
        return 0;
    }
    
    /**
     * 获取CPU频率信息（根据核心数）- 保留用于文本显示
     * @return 格式化的频率信息字符串，每行显示一个核心的信息
     */
    public String getCpuFrequencyInfo() {
        int cpuCores = getCpuCoreCount();
        if (cpuCores <= 0) {
            return "未获取";
        }
        
        StringBuilder freqInfo = new StringBuilder();
        boolean hasAnyFreq = false;
        
        for (int i = 0; i < cpuCores; i++) {
            CpuFrequencyData data = getCpuCoreFrequencyData(i);
            if (data != null && data.isValid()) {
                if (hasAnyFreq) {
                    freqInfo.append("\n");
                }
                String curFreqStr = formatFrequency(data.curFreq);
                String maxFreqStr = formatFrequency(data.maxFreq);
                String minFreqStr = formatFrequency(data.minFreq);
                freqInfo.append("CPU").append(i).append(": 当前=").append(curFreqStr)
                        .append(", 最大=").append(maxFreqStr)
                        .append(", 最小=").append(minFreqStr);
                hasAnyFreq = true;
            }
        }
        
        return hasAnyFreq ? freqInfo.toString() : "未获取";
    }
    
    /**
     * 读取频率文件并格式化
     * @param file 频率文件（cpuinfo_cur_freq, cpuinfo_max_freq, cpuinfo_min_freq）
     * @return 格式化后的频率字符串（如 "2.4 GHz"），如果读取失败返回null
     */
    private String readFrequencyFile(File file) {
        try {
            if (!file.exists() || !file.canRead()) {
                return null;
            }
            
            String content = readFileContent(file);
            if (content == null || content.trim().isEmpty()) {
                return null;
            }
            
            // 频率通常以 kHz 为单位
            try {
                long freqKhz = Long.parseLong(content.trim());
                return formatFrequency(freqKhz);
            } catch (NumberFormatException e) {
                // 如果不是数字，直接返回原始内容
                return content.trim();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "读取频率文件失败: " + file.getPath(), e);
            return null;
        }
    }
    
    /**
     * 格式化频率（将 kHz 转换为合适的单位）
     * @param freqKhz 频率（单位：kHz）
     * @return 格式化后的字符串，如 "2.4 GHz", "1800 MHz", "500 kHz"
     */
    private String formatFrequency(long freqKhz) {
        if (freqKhz >= 1000000) {
            // 大于等于 1000 MHz，显示为 GHz
            double freqGhz = freqKhz / 1000000.0;
            return String.format("%.2f GHz", freqGhz);
        } else if (freqKhz >= 1000) {
            // 大于等于 1 MHz，显示为 MHz
            double freqMhz = freqKhz / 1000.0;
            return String.format("%.0f MHz", freqMhz);
        } else {
            // 小于 1 MHz，显示为 kHz
            return freqKhz + " kHz";
        }
    }
    
    /**
     * 解析CPU核心数量（从 /proc/cpuinfo）
     * @param cpuInfo cpuinfo文件内容
     * @return CPU核心数
     */
    private int parseCpuCoreCount(String cpuInfo) {
        // 统计"processor : "出现的次数
        int count = 0;
        String[] lines = cpuInfo.split("\n");

        for (String line : lines) {
            if (line.trim().startsWith("processor")) {
                count++;
            }
        }

        return count;
    }
    
    /**
     * 读取cpuinfo文件内容
     */
    private String readCpuInfoFile() throws IOException {
        StringBuilder content = new StringBuilder();
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(CPU_INFO_PATH));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "关闭文件流失败", e);
                }
            }
        }

        return content.toString();
    }
    
    /**
     * 读取文件内容（单行）
     * @param file 要读取的文件
     * @return 文件的第一行内容，如果读取失败返回null
     */
    private String readFileContent(File file) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            if (line != null) {
                return line.trim();
            }
            return null;
        } catch (IOException e) {
            Log.d(TAG, "读取文件失败: " + file.getPath() + ", " + e.getMessage());
            return null;
        } catch (Exception e) {
            Log.d(TAG, "读取文件异常: " + file.getPath() + ", " + e.getMessage());
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // 忽略关闭异常
                }
            }
        }
    }
}
