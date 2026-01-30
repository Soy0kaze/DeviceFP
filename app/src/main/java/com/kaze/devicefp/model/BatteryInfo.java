package com.kaze.devicefp.model;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 电池信息实体类
 */
public class BatteryInfo {
    // 基本信息
    private int level;           // 当前电量 (0-100)
    private int scale;           // 电量最大值 (通常是100)
    private int status;          // 充电状态
    private int health;          // 电池健康状态
    private int plugged;         // 充电类型
    private int voltage;         // 电池电压 (mV)
    private int temperature;     // 电池温度 (°C * 10)
    private String technology;   // 电池技术 (如Li-ion)
    private boolean present;     // 电池是否存在
    private int capacity;        // 电池容量 (mAh，可能为估计值)

    // 充电相关
    private boolean isCharging;      // 是否正在充电
    private boolean isUsbCharging;   // 是否通过USB充电
    private boolean isAcCharging;    // 是否通过交流电充电
    private boolean isWirelessCharging; // 是否无线充电

    // 计算字段
    private double percentage;   // 电量百分比

    // Getter 和 Setter 方法
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getScale() { return scale; }
    public void setScale(int scale) { this.scale = scale; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }

    public int getPlugged() { return plugged; }
    public void setPlugged(int plugged) { this.plugged = plugged; }

    public int getVoltage() { return voltage; }
    public void setVoltage(int voltage) { this.voltage = voltage; }

    public int getTemperature() { return temperature; }
    public void setTemperature(int temperature) { this.temperature = temperature; }

    public String getTechnology() { return technology; }
    public void setTechnology(String technology) { this.technology = technology; }

    public boolean isPresent() { return present; }
    public void setPresent(boolean present) { this.present = present; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public boolean isCharging() { return isCharging; }
    public void setCharging(boolean charging) { isCharging = charging; }

    public boolean isUsbCharging() { return isUsbCharging; }
    public void setUsbCharging(boolean usbCharging) { isUsbCharging = usbCharging; }

    public boolean isAcCharging() { return isAcCharging; }
    public void setAcCharging(boolean acCharging) { isAcCharging = acCharging; }

    public boolean isWirelessCharging() { return isWirelessCharging; }
    public void setWirelessCharging(boolean wirelessCharging) { isWirelessCharging = wirelessCharging; }

    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }

    /**
     * 获取格式化后的温度（实际温度）
     */
    public double getTemperatureCelsius() {
        return temperature / 10.0;
    }

    /**
     * 获取电压（伏特）
     */
    public double getVoltageVolts() {
        return voltage / 1000.0;
    }

    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                return "正在充电";
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                return "放电中";
            case BatteryManager.BATTERY_STATUS_FULL:
                return "已充满";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                return "未充电";
            case BatteryManager.BATTERY_STATUS_UNKNOWN:
            default:
                return "未知状态";
        }
    }

    /**
     * 获取健康状态描述
     */
    public String getHealthDescription() {
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_COLD:
                return "过冷";
            case BatteryManager.BATTERY_HEALTH_DEAD:
                return "损坏";
            case BatteryManager.BATTERY_HEALTH_GOOD:
                return "良好";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                return "过热";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                return "过压";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                return "故障";
            case BatteryManager.BATTERY_HEALTH_UNKNOWN:
            default:
                return "未知";
        }
    }

    /**
     * 获取充电类型描述
     */
    public String getPluggedDescription() {
        switch (plugged) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                return "交流电充电";
            case BatteryManager.BATTERY_PLUGGED_USB:
                return "USB充电";
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                return "无线充电";
            default:
                return "未充电";
        }
    }

    @Override
    public String toString() {
        return String.format(
                "电量: %d%%, 状态: %s, 健康: %s, 温度: %.1f°C, 电压: %.2fV",
                level, getStatusDescription(), getHealthDescription(),
                getTemperatureCelsius(), getVoltageVolts()
        );
    }
}
