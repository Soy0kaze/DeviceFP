package com.kaze.devicefp.model;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import androidx.annotation.RequiresApi;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StorageInfoManager {

    private static final String TAG = "StorageInfoManager";

    /**
     * 存储类型枚举
     */
    public enum StorageType {
        INTERNAL,      // 内部存储
        EXTERNAL,      // 外部存储（SD卡）
        SECONDARY,     // 第二存储
        PRIMARY        // 主存储
    }

    /**
     * 存储信息实体类
     */
    public static class StorageInfo {
        private StorageType type;
        private String path;
        private long totalBytes;      // 总容量（字节）
        private long availableBytes;  // 可用容量（字节）
        private long usedBytes;       // 已用容量（字节）
        private boolean isMounted;    // 是否已挂载
        private boolean isRemovable;  // 是否可移除（如SD卡）
        private String description;   // 描述

        // 构造方法、getter/setter...
        public StorageType getType() { return type; }
        public void setType(StorageType type) { this.type = type; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public long getTotalBytes() { return totalBytes; }
        public void setTotalBytes(long totalBytes) { this.totalBytes = totalBytes; }
        public long getAvailableBytes() { return availableBytes; }
        public void setAvailableBytes(long availableBytes) { this.availableBytes = availableBytes; }
        public long getUsedBytes() { return usedBytes; }
        public void setUsedBytes(long usedBytes) { this.usedBytes = usedBytes; }
        public boolean isMounted() { return isMounted; }
        public void setMounted(boolean mounted) { isMounted = mounted; }
        public boolean isRemovable() { return isRemovable; }
        public void setRemovable(boolean removable) { isRemovable = removable; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        // 格式化方法
        public String getTotalGB() {
            return formatBytesToGB(totalBytes);
        }

        public String getAvailableGB() {
            return formatBytesToGB(availableBytes);
        }

        public String getUsedGB() {
            return formatBytesToGB(usedBytes);
        }

        public double getUsagePercentage() {
            if (totalBytes > 0) {
                return (double) usedBytes / totalBytes * 100;
            }
            return 0;
        }

        private String formatBytesToGB(long bytes) {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }

        @Override
        public String toString() {
            return String.format("类型: %s, 路径: %s, 总容量: %.2f GB, 可用: %.2f GB, 已用: %.1f%%",
                    type, path, totalBytes / (1024.0 * 1024.0 * 1024.0),
                    availableBytes / (1024.0 * 1024.0 * 1024.0), getUsagePercentage());
        }
    }

    /**
     * 获取所有存储信息
     */
    public static List<StorageInfo> getAllStorageInfo(Context context) {
        List<StorageInfo> storageList = new ArrayList<>();

        // 1. 获取内部存储
        StorageInfo internalStorage = getInternalStorageInfo();
        if (internalStorage != null) {
            storageList.add(internalStorage);
        }

        // 2. 获取外部存储（SD卡）
        StorageInfo externalStorage = getExternalStorageInfo();
        if (externalStorage != null) {
            storageList.add(externalStorage);
        }

        // 3. 获取第二存储
        StorageInfo secondaryStorage = getSecondaryStorageInfo();
        if (secondaryStorage != null) {
            storageList.add(secondaryStorage);
        }

        // 4. 获取其他存储（Android 7.0+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            List<StorageInfo> otherStorages = getStorageVolumesInfo(context);
            if (otherStorages != null) {
                storageList.addAll(otherStorages);
            }
        }

        return storageList;
    }

    /**
     * 获取内部存储信息（对应 cz.l.H()）
     */
    public static StorageInfo getInternalStorageInfo() {
        try {
            File dataDir = Environment.getDataDirectory();
            StorageInfo info = new StorageInfo();
            info.setType(StorageType.INTERNAL);
            info.setPath(dataDir.getAbsolutePath());
            info.setDescription("内部存储");
            info.setRemovable(false);

            // 使用StatFs获取存储信息
            StatFs stat = new StatFs(dataDir.getPath());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                long blockSize = stat.getBlockSizeLong();
                long totalBlocks = stat.getBlockCountLong();
                long availableBlocks = stat.getAvailableBlocksLong();

                info.setTotalBytes(blockSize * totalBlocks);
                info.setAvailableBytes(blockSize * availableBlocks);
                info.setUsedBytes(info.getTotalBytes() - info.getAvailableBytes());
            } else {
                // 旧版本API
                long blockSize = stat.getBlockSize();
                long totalBlocks = stat.getBlockCount();
                long availableBlocks = stat.getAvailableBlocks();

                info.setTotalBytes(blockSize * totalBlocks);
                info.setAvailableBytes(blockSize * availableBlocks);
                info.setUsedBytes(info.getTotalBytes() - info.getAvailableBytes());
            }

            info.setMounted(true);
            return info;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取外部存储信息（对应 com.kuaishou.weapon.gp.bv.b()）
     */
    public static StorageInfo getExternalStorageInfo() {
        try {
            String state = Environment.getExternalStorageState();
            StorageInfo info = new StorageInfo();
            info.setType(StorageType.EXTERNAL);
            info.setPath(Environment.getExternalStorageDirectory().getAbsolutePath());
            info.setDescription("外部存储");
            info.setRemovable(true);
            info.setMounted(Environment.MEDIA_MOUNTED.equals(state));

            if (info.isMounted()) {
                StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());

                {
                    long blockSize = stat.getBlockSizeLong();
                    long totalBlocks = stat.getBlockCountLong();
                    long availableBlocks = stat.getAvailableBlocksLong();

                    info.setTotalBytes(blockSize * totalBlocks);
                    info.setAvailableBytes(blockSize * availableBlocks);
                    info.setUsedBytes(info.getTotalBytes() - info.getAvailableBytes());
                }
            }

            return info;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取第二存储信息（对应 cz.l.F()）
     */
    public static StorageInfo getSecondaryStorageInfo() {
        try {
            StorageInfo info = new StorageInfo();
            info.setType(StorageType.SECONDARY);
            info.setDescription("第二存储");

            // 检查Android版本
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                // Android 7.0+ 使用新的API
                return null;
            }

            // 从环境变量获取第二存储路径
            String secondaryPath = System.getenv("SECONDARY_STORAGE");
            if (secondaryPath == null || secondaryPath.isEmpty()) {
                return null;
            }

            info.setPath(secondaryPath);
            info.setRemovable(true);

            // 检查路径是否存在
            File secondaryDir = new File(secondaryPath);
            if (!secondaryDir.exists()) {
                info.setMounted(false);
                return info;
            }

            info.setMounted(true);
            StatFs stat = new StatFs(secondaryPath);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                long blockSize = stat.getBlockSizeLong();
                long totalBlocks = stat.getBlockCountLong();
                long availableBlocks = stat.getAvailableBlocksLong();

                info.setTotalBytes(blockSize * totalBlocks);
                info.setAvailableBytes(blockSize * availableBlocks);
                info.setUsedBytes(info.getTotalBytes() - info.getAvailableBytes());
            } else {
                long blockSize = stat.getBlockSize();
                long totalBlocks = stat.getBlockCount();
                long availableBlocks = stat.getAvailableBlocks();

                info.setTotalBytes(blockSize * totalBlocks);
                info.setAvailableBytes(blockSize * availableBlocks);
                info.setUsedBytes(info.getTotalBytes() - info.getAvailableBytes());
            }

            return info;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取所有存储卷信息（Android 7.0+）
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static List<StorageInfo> getStorageVolumesInfo(Context context) {
        List<StorageInfo> storageList = new ArrayList<>();

        try {
            StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            if (storageManager == null) {
                return storageList;
            }

            List<StorageVolume> storageVolumes = storageManager.getStorageVolumes();

            for (StorageVolume volume : storageVolumes) {
                StorageInfo info = new StorageInfo();

                // 设置基本信息
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    info.setPath(volume.getDirectory().getAbsolutePath());
                    info.setDescription(volume.getDescription(context));
                    info.setRemovable(volume.isRemovable());

                    // 使用 getState() 方法检查存储状态
                    String state = volume.getState();
                    info.setMounted(Environment.MEDIA_MOUNTED.equals(state));

                    // 判断存储类型
                    if (volume.isPrimary()) {
                        info.setType(StorageType.PRIMARY);
                        info.setDescription("主存储");
                    } else if (volume.isRemovable()) {
                        info.setType(StorageType.EXTERNAL);
                        info.setDescription("可移除存储");
                    } else {
                        info.setType(StorageType.INTERNAL);
                        info.setDescription("内部存储");
                    }

                    // 获取存储空间信息
                    if (info.isMounted()) {
                        File storageDir = null;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            storageDir = volume.getDirectory();
                        }
                        try {
                            StatFs stat = new StatFs(storageDir.getPath());

                            long blockSize = stat.getBlockSizeLong();
                            long totalBlocks = stat.getBlockCountLong();
                            long availableBlocks = stat.getAvailableBlocksLong();

                            info.setTotalBytes(blockSize * totalBlocks);
                            info.setAvailableBytes(blockSize * availableBlocks);
                            info.setUsedBytes(info.getTotalBytes() - info.getAvailableBytes());
                        } catch (Exception e) {
                            // 如果无法获取StatFs，尝试使用File方法
                            info.setTotalBytes(storageDir.getTotalSpace());
                            info.setAvailableBytes(storageDir.getUsableSpace());
                            info.setUsedBytes(info.getTotalBytes() - info.getAvailableBytes());
                        }
                    }

                    storageList.add(info);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return storageList;
    }

    /**
     * 获取主存储预留空间（简化版，不使用反射）
     * 注意：原始代码中的 isMountedReadable 方法不存在，这里简化处理
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static long getPrimaryStorageReservedSpace(Context context) {
        try {
            StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            if (storageManager == null) {
                return -1L;
            }

            List<StorageVolume> volumes = storageManager.getStorageVolumes();
            for (StorageVolume volume : volumes) {
                if (volume.isPrimary()) {
                    // 使用 getState() 检查是否可读
                    String state = volume.getState();
                    boolean isReadable = Environment.MEDIA_MOUNTED.equals(state) ||
                            Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);

                    if (isReadable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        File file = volume.getDirectory();

                        // 方法1：使用 File 获取空间信息
                        long totalSpace = file.getTotalSpace();
                        long usableSpace = file.getUsableSpace();

                        // 这里简化处理，返回0作为预留空间
                        // 实际上，系统预留空间可能很难准确获取
                        return 0L;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0L;
    }

    /**
     * 获取存储使用情况的摘要信息
     */
    public static String getStorageSummary(Context context) {
        StringBuilder summary = new StringBuilder();
        List<StorageInfo> storageList = getAllStorageInfo(context);

        summary.append("=== 存储空间信息 ===\n\n");

        for (StorageInfo info : storageList) {
            summary.append(info.getDescription()).append(":\n");
            summary.append("  类型: ").append(info.getType()).append("\n");
            summary.append("  路径: ").append(info.getPath()).append("\n");
            summary.append("  状态: ").append(info.isMounted() ? "已挂载" : "未挂载").append("\n");

            if (info.isMounted()) {
                summary.append(String.format("  总容量: %.2f GB\n",
                        info.getTotalBytes() / (1024.0 * 1024.0 * 1024.0)));
                summary.append(String.format("  可用容量: %.2f GB\n",
                        info.getAvailableBytes() / (1024.0 * 1024.0 * 1024.0)));
                summary.append(String.format("  使用率: %.1f%%\n", info.getUsagePercentage()));
            }

            summary.append("\n");
        }

        return summary.toString();
    }

    /**
     * 获取总存储容量（所有存储设备的总和）
     */
    public static long getTotalStorageCapacity(Context context) {
        long total = 0;
        List<StorageInfo> storageList = getAllStorageInfo(context);

        for (StorageInfo info : storageList) {
            if (info.isMounted()) {
                total += info.getTotalBytes();
            }
        }

        return total;
    }

    /**
     * 获取可用存储容量（所有存储设备的总和）
     */
    public static long getTotalAvailableStorage(Context context) {
        long total = 0;
        List<StorageInfo> storageList = getAllStorageInfo(context);

        for (StorageInfo info : storageList) {
            if (info.isMounted()) {
                total += info.getAvailableBytes();
            }
        }

        return total;
    }

    /**
     * 检查存储空间是否充足
     * @param context 上下文
     * @param requiredBytes 需要的字节数
     * @return 是否充足
     */
    public static boolean isStorageSufficient(Context context, long requiredBytes) {
        return getTotalAvailableStorage(context) >= requiredBytes;
    }

    /**
     * 简化版：只获取基础存储信息（避免版本兼容问题）
     */
    public static String getBasicStorageInfo() {
        StringBuilder info = new StringBuilder();

        // 1. 内部存储
        try {
            File dataDir = Environment.getDataDirectory();
            StatFs stat = new StatFs(dataDir.getPath());

            long blockSize, totalBlocks, availableBlocks;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                blockSize = stat.getBlockSizeLong();
                totalBlocks = stat.getBlockCountLong();
                availableBlocks = stat.getAvailableBlocksLong();
            } else {
                blockSize = stat.getBlockSize();
                totalBlocks = stat.getBlockCount();
                availableBlocks = stat.getAvailableBlocks();
            }

            long total = blockSize * totalBlocks;
            long available = blockSize * availableBlocks;
            long used = total - available;
            double usage = total > 0 ? (double) used / total * 100 : 0;

            info.append("内部存储:\n");
            info.append(String.format("  总容量: %d KB\n", total / 1024));
            info.append(String.format("  可用容量: %d KB\n", available / 1024));
            info.append(String.format("  已用容量: %d KB\n", used / 1024));
            info.append(String.format("  使用率: %.1f%%\n\n", usage));
        } catch (Exception e) {
            info.append("内部存储: 获取失败\n\n");
        }

        // 2. 外部存储
        try {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                File externalDir = Environment.getExternalStorageDirectory();
                StatFs stat = new StatFs(externalDir.getPath());

                long blockSize, totalBlocks, availableBlocks;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    blockSize = stat.getBlockSizeLong();
                    totalBlocks = stat.getBlockCountLong();
                    availableBlocks = stat.getAvailableBlocksLong();
                } else {
                    blockSize = stat.getBlockSize();
                    totalBlocks = stat.getBlockCount();
                    availableBlocks = stat.getAvailableBlocks();
                }

                long total = blockSize * totalBlocks;
                long available = blockSize * availableBlocks;
                long used = total - available;
                double usage = total > 0 ? (double) used / total * 100 : 0;

                info.append("外部存储:\n");
                info.append(String.format("  总容量: %d KB\n", total / 1024));
                info.append(String.format("  可用容量: %d KB\n", available / 1024));
                info.append(String.format("  已用容量: %d KB\n", used / 1024));
                info.append(String.format("  使用率: %.1f%%\n\n", usage));
            } else {
                info.append("外部存储: 未挂载或不可用\n\n");
            }
        } catch (Exception e) {
            info.append("外部存储: 获取失败\n\n");
        }

        return info.toString();
    }

    /**
     * 获取存储信息的JSON格式（简化版）
     */
    public static String getStorageInfoJson(Context context) {
        try {
            // 内部存储
            File dataDir = Environment.getDataDirectory();
            long internalTotal = dataDir.getTotalSpace();
            long internalAvailable = dataDir.getUsableSpace();
            long internalUsed = internalTotal - internalAvailable;

            // 外部存储
            String state = Environment.getExternalStorageState();
            long externalTotal = 0;
            long externalAvailable = 0;
            long externalUsed = 0;

            if (Environment.MEDIA_MOUNTED.equals(state)) {
                File externalDir = Environment.getExternalStorageDirectory();
                externalTotal = externalDir.getTotalSpace();
                externalAvailable = externalDir.getUsableSpace();
                externalUsed = externalTotal - externalAvailable;
            }

            return String.format(
                    "{\"internal\": {\"totalKB\": %d, \"availableKB\": %d, \"usedKB\": %d}," +
                            "\"external\": {\"totalKB\": %d, \"availableKB\": %d, \"usedKB\": %d}," +
                            "\"totalKB\": %d, \"totalAvailableKB\": %d}",
                    internalTotal / 1024, internalAvailable / 1024, internalUsed / 1024,
                    externalTotal / 1024, externalAvailable / 1024, externalUsed / 1024,
                    (internalTotal + externalTotal) / 1024,
                    (internalAvailable + externalAvailable) / 1024
            );
        } catch (Exception e) {
            return "{\"error\": \"获取存储信息失败\"}";
        }
    }
}
