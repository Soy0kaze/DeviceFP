package com.kaze.devicefp.model;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.util.Log;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class BatteryInfoManager {
    private static final String TAG = "BatteryInfoManager";

    // 电池容量文件路径（需要root权限）
    private static final String BATTERY_CAPACITY_PATH = "/sys/class/power_supply/battery/charge_full_design";
    private static final String BATTERY_CAPACITY_NOW_PATH = "/sys/class/power_supply/battery/charge_now";

    /**
     * 获取电池信息
     */
    public static BatteryInfo getBatteryInfo(Context context) {
        BatteryInfo batteryInfo = new BatteryInfo();

        try {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, filter);

            if (batteryStatus != null) {
                parseBatteryIntent(batteryInfo, batteryStatus);
            }

            // 尝试获取电池设计容量（需要root权限或系统权限）
            batteryInfo.setCapacity(getBatteryDesignCapacity(context));

            // 计算百分比
            if (batteryInfo.getScale() > 0) {
                double percentage = (double) batteryInfo.getLevel() / batteryInfo.getScale() * 100;
                batteryInfo.setPercentage(percentage);
            }

        } catch (Exception e) {
            Log.e(TAG, "获取电池信息失败", e);
        }

        return batteryInfo;
    }

    /**
     * 解析电池Intent数据
     */
    private static void parseBatteryIntent(BatteryInfo batteryInfo, Intent intent) {
        // 基本电量信息
        batteryInfo.setLevel(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1));
        batteryInfo.setScale(intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1));
        batteryInfo.setStatus(intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1));
        batteryInfo.setHealth(intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1));
        batteryInfo.setPlugged(intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1));
        batteryInfo.setVoltage(intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1));
        batteryInfo.setTemperature(intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1));
        batteryInfo.setTechnology(intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY));
        batteryInfo.setPresent(intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false));

        // 充电状态
        int status = batteryInfo.getStatus();
        batteryInfo.setCharging(status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL);

        // 充电类型
        int plugged = batteryInfo.getPlugged();
        batteryInfo.setAcCharging(plugged == BatteryManager.BATTERY_PLUGGED_AC);
        batteryInfo.setUsbCharging(plugged == BatteryManager.BATTERY_PLUGGED_USB);

        // 无线充电（API 17+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            batteryInfo.setWirelessCharging(plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS);
        }
    }

    /**
     * 获取电池设计容量（需要root权限）
     */
    private static int getBatteryDesignCapacity(Context context) {
        try {
            // 方法1：从系统文件读取设计容量
            String capacityStr = readSystemFile(BATTERY_CAPACITY_PATH);
            if (capacityStr != null && !capacityStr.isEmpty()) {
                // 文件中的单位可能是微安时(μAh)，转换为毫安时(mAh)
                int capacity = Integer.parseInt(capacityStr.trim());
                return capacity / 1000; // μAh -> mAh
            }

            // 方法2：使用反射获取（部分设备支持）
            try {
                Class<?> powerProfileClass = Class.forName("com.android.internal.os.PowerProfile");
                Method getBatteryCapacityMethod = powerProfileClass.getMethod("getBatteryCapacity");

                Object powerProfile = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // Android 5.0+ 可以通过构造函数创建PowerProfile实例
                    Constructor<?> constructor = powerProfileClass.getConstructor(Context.class);
                    powerProfile = constructor.newInstance(context);
                }

                if (powerProfile != null && getBatteryCapacityMethod != null) {
                    Object capacityObj = getBatteryCapacityMethod.invoke(powerProfile);
                    if (capacityObj instanceof Double) {
                        return (int) Math.round((Double) capacityObj);
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "通过反射获取电池容量失败", e);
            }

        } catch (Exception e) {
            Log.e(TAG, "获取电池容量失败", e);
        }

        return 0;
    }

    /**
     * 读取系统文件（需要root权限）
     */
    private static String readSystemFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            return reader.readLine();
        } catch (Exception e) {
            Log.d(TAG, "无法读取系统文件: " + filePath);
            return null;
        }
    }

    /**
     * 获取当前电池电流（需要API 20+）
     */
    public static int getBatteryCurrentNow(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            if (batteryManager != null) {
                return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
            }
        }
        return 0;
    }

    /**
     * 获取电池充电电流（需要API 20+）
     */
    public static int getBatteryChargeCounter(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            if (batteryManager != null) {
                return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
            }
        }
        return 0;
    }

    /**
     * 获取电池容量（需要API 21+）
     */
    public static int getBatteryCapacity(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            if (batteryManager != null) {
                return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            }
        }
        return 0;
    }

    /**
     * 获取电池能量计数（需要API 28+）
     */
    public static long getBatteryEnergyCounter(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            if (batteryManager != null) {
                return batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);
            }
        }
        return 0;
    }

    /**
     * 获取详细的电池报告
     */
    public static String getBatteryReport(Context context) {
        BatteryInfo batteryInfo = getBatteryInfo(context);
        StringBuilder report = new StringBuilder();

        report.append("=== 电池信息报告 ===\n\n");

        // 电量信息
        report.append("电量信息:\n");
        report.append("  当前电量: ").append(batteryInfo.getLevel()).append("%\n");
        report.append("  充电状态: ").append(batteryInfo.getStatusDescription()).append("\n");
        report.append("  充电类型: ").append(batteryInfo.getPluggedDescription()).append("\n");

        // 电池状态
        report.append("\n电池状态:\n");
        report.append("  健康状态: ").append(batteryInfo.getHealthDescription()).append("\n");
        report.append("  电池技术: ").append(batteryInfo.getTechnology() != null ? batteryInfo.getTechnology() : "未知").append("\n");
        report.append("  电池存在: ").append(batteryInfo.isPresent() ? "是" : "否").append("\n");

        // 物理参数
        report.append("\n物理参数:\n");
        report.append("  电池电压: ").append(String.format("%.2f V", batteryInfo.getVoltageVolts())).append("\n");
        report.append("  电池温度: ").append(String.format("%.1f °C", batteryInfo.getTemperatureCelsius())).append("\n");

        // 充电详情
        report.append("\n充电详情:\n");
        report.append("  正在充电: ").append(batteryInfo.isCharging() ? "是" : "否").append("\n");
        report.append("  USB充电: ").append(batteryInfo.isUsbCharging() ? "是" : "否").append("\n");
        report.append("  交流电充电: ").append(batteryInfo.isAcCharging() ? "是" : "否").append("\n");
        report.append("  无线充电: ").append(batteryInfo.isWirelessCharging() ? "是" : "否").append("\n");

        // 高级信息（需要API支持）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int current = getBatteryCurrentNow(context);
            int chargeCounter = getBatteryChargeCounter(context);
            int capacity = getBatteryCapacity(context);

            report.append("\n高级信息:\n");
            report.append("  当前电流: ").append(current).append(" μA\n");
            report.append("  充电计数: ").append(chargeCounter).append(" μAh\n");
            report.append("  估计容量: ").append(capacity).append(" %\n");

            if (batteryInfo.getCapacity() > 0) {
                report.append("  设计容量: ").append(batteryInfo.getCapacity()).append(" mAh\n");
            }
        }

        // 计算估计剩余时间
        if (batteryInfo.isCharging()) {
            String chargeTime = estimateChargeTime(context, batteryInfo);
            if (!chargeTime.isEmpty()) {
                report.append("\n充电时间估计: ").append(chargeTime).append("\n");
            }
        } else {
            String dischargeTime = estimateDischargeTime(context, batteryInfo);
            if (!dischargeTime.isEmpty()) {
                report.append("\n使用时间估计: ").append(dischargeTime).append("\n");
            }
        }

        return report.toString();
    }

    /**
     * 估计充电时间（简单估计）
     */
    private static String estimateChargeTime(Context context, BatteryInfo batteryInfo) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
                if (batteryManager != null) {
                    long chargeTimeRemaining = batteryManager.computeChargeTimeRemaining();
                    if (chargeTimeRemaining > 0) {
                        return formatTime(chargeTimeRemaining);
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "计算充电时间失败", e);
        }

        // 简单估计算法
        int remainingPercent = 100 - batteryInfo.getLevel();
        if (batteryInfo.isAcCharging()) {
            int minutes = remainingPercent * 2; // AC充电大约每分钟2%
            return formatTime(minutes * 60 * 1000L);
        } else if (batteryInfo.isUsbCharging()) {
            int minutes = remainingPercent * 5; // USB充电大约每分钟0.5%
            return formatTime(minutes * 60 * 1000L);
        }

        return "";
    }

    /**
     * 估计放电时间（简单估计）
     */
    private static String estimateDischargeTime(Context context, BatteryInfo batteryInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int current = Math.abs(getBatteryCurrentNow(context));
            if (current > 0 && batteryInfo.getCapacity() > 0) {
                // 简单计算：剩余电量 / 当前电流
                int remainingMah = batteryInfo.getCapacity() * batteryInfo.getLevel() / 100;
                double hours = (double) remainingMah / (current / 1000.0); // μA -> mA
                return String.format("%.1f 小时", hours);
            }
        }
        return "";
    }

    /**
     * 格式化时间（毫秒 -> 小时:分钟）
     */
    private static String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;

        if (hours > 0) {
            return String.format("%d小时%d分钟", hours, minutes);
        } else {
            return String.format("%d分钟", minutes);
        }
    }

    /**
     * 获取电池信息的JSON格式
     */
    public static String getBatteryInfoJson(Context context) {
        BatteryInfo batteryInfo = getBatteryInfo(context);

        return String.format(
                "{\"level\": %d, \"status\": \"%s\", \"health\": \"%s\", " +
                        "\"plugged\": \"%s\", \"voltage\": %.2f, \"temperature\": %.1f, " +
                        "\"charging\": %b, \"technology\": \"%s\", \"present\": %b}",
                batteryInfo.getLevel(),
                batteryInfo.getStatusDescription(),
                batteryInfo.getHealthDescription(),
                batteryInfo.getPluggedDescription(),
                batteryInfo.getVoltageVolts(),
                batteryInfo.getTemperatureCelsius(),
                batteryInfo.isCharging(),
                batteryInfo.getTechnology() != null ? batteryInfo.getTechnology() : "",
                batteryInfo.isPresent()
        );
    }

    /**
     * 监控电池变化（需要注册广播接收器）
     */
    public static class BatteryChangeListener {
        private Context context;
        private BatteryChangeCallback callback;

        public interface BatteryChangeCallback {
            void onBatteryChanged(BatteryInfo batteryInfo);
        }

        public BatteryChangeListener(Context context, BatteryChangeCallback callback) {
            this.context = context;
            this.callback = callback;
        }

        public void startMonitoring() {
            // 实际应用中需要注册广播接收器
            // 这里简化为直接获取一次
            if (callback != null) {
                BatteryInfo batteryInfo = getBatteryInfo(context);
                callback.onBatteryChanged(batteryInfo);
            }
        }

        public void stopMonitoring() {
            // 取消注册广播接收器
        }
    }

    /**
     * 检查电池是否低电量
     */
    public static boolean isBatteryLow(Context context) {
        BatteryInfo batteryInfo = getBatteryInfo(context);
        return batteryInfo.getLevel() <= 15;
    }

    /**
     * 检查电池是否过热
     */
    public static boolean isBatteryOverheat(Context context) {
        BatteryInfo batteryInfo = getBatteryInfo(context);
        return batteryInfo.getTemperatureCelsius() > 45.0;
    }

    /**
     * 检查是否正在快速充电
     */
    public static boolean isFastCharging(Context context) {
        BatteryInfo batteryInfo = getBatteryInfo(context);

        // 判断标准：交流电充电且电压较高
        return batteryInfo.isAcCharging() && batteryInfo.getVoltageVolts() > 4.3;
    }
    public static String getChargerType(Context context) {
        try {
            // 获取电池状态Intent
            Intent batteryIntent = context.registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

            if (batteryIntent != null) {
                int plugged = batteryIntent.getIntExtra("plugged", -1);

                switch (plugged) {
                    case BatteryManager.BATTERY_PLUGGED_AC:
                        return "AC charger";
                    case BatteryManager.BATTERY_PLUGGED_USB:
                        return "USB charger";
                    case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                        return "Wireless charger";
                    default:
                        return ""; // 未充电
                }
            }
            return null;
        } catch (Throwable unused_ex) {
            return null;
        }
    }
}
