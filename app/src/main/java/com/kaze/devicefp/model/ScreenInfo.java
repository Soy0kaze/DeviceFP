package com.kaze.devicefp.model;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 屏幕信息实体类
 */
public class ScreenInfo {
    // DisplayMetrics 信息（对应 cz.l.C() 的内容）
    private float density;            // 屏幕密度
    private int widthPixels;          // 屏幕宽度（像素）
    private int heightPixels;         // 屏幕高度（像素）
    private float scaledDensity;      // 字体缩放密度
    private float xdpi;               // X轴方向每英寸像素数
    private float ydpi;               // Y轴方向每英寸像素数

    // 额外计算信息
    private float diagonalInches;     // 屏幕对角线尺寸（英寸）
    private int ppi;                  // 每英寸像素数
    private String aspectRatio;       // 宽高比（如16:9）

    // 系统UI高度
    private int statusBarHeight;      // 状态栏高度
    private int navigationBarHeight;  // 导航栏高度
    private int actionBarHeight;      // ActionBar高度

    // 屏幕超时设置
    private int screenTimeout;        // 屏幕超时时间（毫秒）
    private int screenTimeoutSeconds; // 屏幕超时时间（秒）

    // 刷新率相关
    private float refreshRate;        // 屏幕刷新率（Hz）
    private float[] supportedRefreshRates; // 支持的刷新率列表

    // 亮度信息
    private int brightness;           // 当前亮度（0-255）
    private float brightnessPercent;  // 亮度百分比

    // 制造商和型号
    private String manufacturer;
    private String model;
    private String device;

    // 原始 DisplayMetrics 字符串（保持原有格式）
    private String displayMetricsString;

    // Getter 和 Setter 方法
    public float getDensity() { return density; }
    public void setDensity(float density) { this.density = density; }

    public int getWidthPixels() { return widthPixels; }
    public void setWidthPixels(int widthPixels) { this.widthPixels = widthPixels; }

    public int getHeightPixels() { return heightPixels; }
    public void setHeightPixels(int heightPixels) { this.heightPixels = heightPixels; }

    public float getScaledDensity() { return scaledDensity; }
    public void setScaledDensity(float scaledDensity) { this.scaledDensity = scaledDensity; }

    public float getXdpi() { return xdpi; }
    public void setXdpi(float xdpi) { this.xdpi = xdpi; }

    public float getYdpi() { return ydpi; }
    public void setYdpi(float ydpi) { this.ydpi = ydpi; }

    public float getDiagonalInches() { return diagonalInches; }
    public void setDiagonalInches(float diagonalInches) { this.diagonalInches = diagonalInches; }

    public int getPpi() { return ppi; }
    public void setPpi(int ppi) { this.ppi = ppi; }

    public String getAspectRatio() { return aspectRatio; }
    public void setAspectRatio(String aspectRatio) { this.aspectRatio = aspectRatio; }

    public int getStatusBarHeight() { return statusBarHeight; }
    public void setStatusBarHeight(int statusBarHeight) { this.statusBarHeight = statusBarHeight; }

    public int getNavigationBarHeight() { return navigationBarHeight; }
    public void setNavigationBarHeight(int navigationBarHeight) { this.navigationBarHeight = navigationBarHeight; }

    public int getActionBarHeight() { return actionBarHeight; }
    public void setActionBarHeight(int actionBarHeight) { this.actionBarHeight = actionBarHeight; }

    public int getScreenTimeout() { return screenTimeout; }
    public void setScreenTimeout(int screenTimeout) { this.screenTimeout = screenTimeout; }

    public int getScreenTimeoutSeconds() { return screenTimeoutSeconds; }
    public void setScreenTimeoutSeconds(int screenTimeoutSeconds) { this.screenTimeoutSeconds = screenTimeoutSeconds; }

    public float getRefreshRate() { return refreshRate; }
    public void setRefreshRate(float refreshRate) { this.refreshRate = refreshRate; }

    public float[] getSupportedRefreshRates() { return supportedRefreshRates; }
    public void setSupportedRefreshRates(float[] supportedRefreshRates) { this.supportedRefreshRates = supportedRefreshRates; }

    public int getBrightness() { return brightness; }
    public void setBrightness(int brightness) { this.brightness = brightness; }

    public float getBrightnessPercent() { return brightnessPercent; }
    public void setBrightnessPercent(float brightnessPercent) { this.brightnessPercent = brightnessPercent; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }

    public String getDisplayMetricsString() { return displayMetricsString; }
    public void setDisplayMetricsString(String displayMetricsString) { this.displayMetricsString = displayMetricsString; }

    /**
     * 获取屏幕分辨率字符串
     */
    public String getResolution() {
        return widthPixels + "x" + heightPixels;
    }

    /**
     * 获取屏幕尺寸类别
     */
    public String getSizeCategory() {
        if (diagonalInches < 4.0) return "超小屏";
        else if (diagonalInches < 5.0) return "小屏";
        else if (diagonalInches < 6.0) return "中屏";
        else if (diagonalInches < 7.0) return "大屏";
        else return "超大屏";
    }

    /**
     * 获取密度类别
     */
    public String getDensityCategory() {
        if (xdpi <= 120) return "ldpi";
        else if (xdpi <= 160) return "mdpi";
        else if (xdpi <= 240) return "hdpi";
        else if (xdpi <= 320) return "xhdpi";
        else if (xdpi <= 480) return "xxhdpi";
        else if (xdpi <= 640) return "xxxhdpi";
        else return "超xxxhdpi";
    }

    /**
     * 获取屏幕超时时间的可读格式
     */
    public String getScreenTimeoutFormatted() {
        if (screenTimeoutSeconds <= 0) return "永不休眠";

        int minutes = screenTimeoutSeconds / 60;
        int seconds = screenTimeoutSeconds % 60;

        if (minutes > 0) {
            if (seconds > 0) {
                return minutes + "分" + seconds + "秒";
            }
            return minutes + "分钟";
        }
        return seconds + "秒";
    }

    /**
     * 获取所有DisplayMetrics信息的数组形式
     */
    public float[] getDisplayMetricsArray() {
        return new float[]{density, widthPixels, heightPixels, scaledDensity, xdpi, ydpi};
    }

    /**
     * 获取原始格式字符串（仿照 cz.l.C() 的格式）
     */
    public String getOriginalFormatString() {
        String str = ",";
        String raw = "[" + density + str + widthPixels + str + heightPixels +
                str + scaledDensity + str + xdpi + str + ydpi + "]";
        // 移除可能引起问题的字符
        return raw.replace("=", "").replace("&", "");
    }

    @Override
    public String toString() {
        return String.format("屏幕: %dx%d, 尺寸: %.1f\", 密度: %s, 刷新率: %.1fHz",
                widthPixels, heightPixels, diagonalInches, getDensityCategory(), refreshRate);
    }

    /**
     * 获取详细的报告字符串
     */
    public String getDetailedReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 屏幕详细信息 ===\n\n");

        // 基础信息
        sb.append("📱 基础信息:\n");
        sb.append("  分辨率: ").append(getResolution()).append("\n");
        sb.append("  宽高比: ").append(aspectRatio != null ? aspectRatio : "未知").append("\n");
        sb.append("  屏幕尺寸: ").append(String.format("%.1f英寸", diagonalInches)).append("\n");
        sb.append("  屏幕类别: ").append(getSizeCategory()).append("\n");
        sb.append("  PPI: ").append(ppi).append("\n\n");

        // 密度信息
        sb.append("📊 密度信息:\n");
        sb.append("  密度: ").append(density).append("\n");
        sb.append("  缩放密度: ").append(scaledDensity).append("\n");
        sb.append("  XDPI: ").append(String.format("%.1f", xdpi)).append("\n");
        sb.append("  YDPI: ").append(String.format("%.1f", ydpi)).append("\n");
        sb.append("  密度类别: ").append(getDensityCategory()).append("\n\n");

        // 系统UI高度
        sb.append("📐 系统UI高度:\n");
        sb.append("  状态栏高度: ").append(statusBarHeight).append("px\n");
        sb.append("  导航栏高度: ").append(navigationBarHeight).append("px\n");
        sb.append("  ActionBar高度: ").append(actionBarHeight).append("px\n\n");

        // 屏幕设置
        sb.append("⚙️ 屏幕设置:\n");
        sb.append("  屏幕超时: ").append(getScreenTimeoutFormatted()).append("\n");
        sb.append("  刷新率: ").append(String.format("%.1fHz", refreshRate)).append("\n");

        if (supportedRefreshRates != null && supportedRefreshRates.length > 0) {
            sb.append("  支持刷新率: ");
            for (float rate : supportedRefreshRates) {
                sb.append(String.format("%.0fHz ", rate));
            }
            sb.append("\n");
        }

        sb.append("  当前亮度: ").append(brightness).append(" (").append(String.format("%.0f%%", brightnessPercent)).append(")\n\n");

        // 设备信息
        sb.append("📱 设备信息:\n");
        sb.append("  制造商: ").append(manufacturer != null ? manufacturer : "未知").append("\n");
        sb.append("  型号: ").append(model != null ? model : "未知").append("\n");
        sb.append("  设备: ").append(device != null ? device : "未知").append("\n\n");

        // 原始格式
        sb.append("🔧 原始DisplayMetrics:\n");
        sb.append("  ").append(displayMetricsString != null ? displayMetricsString : getOriginalFormatString());

        return sb.toString();
    }

    /**
     * 获取JSON格式数据
     */
    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"density\":").append(density).append(",");
        json.append("\"widthPixels\":").append(widthPixels).append(",");
        json.append("\"heightPixels\":").append(heightPixels).append(",");
        json.append("\"scaledDensity\":").append(scaledDensity).append(",");
        json.append("\"xdpi\":").append(xdpi).append(",");
        json.append("\"ydpi\":").append(ydpi).append(",");
        json.append("\"diagonalInches\":").append(diagonalInches).append(",");
        json.append("\"ppi\":").append(ppi).append(",");
        json.append("\"aspectRatio\":\"").append(aspectRatio != null ? aspectRatio : "").append("\",");
        json.append("\"statusBarHeight\":").append(statusBarHeight).append(",");
        json.append("\"navigationBarHeight\":").append(navigationBarHeight).append(",");
        json.append("\"actionBarHeight\":").append(actionBarHeight).append(",");
        json.append("\"screenTimeoutMs\":").append(screenTimeout).append(",");
        json.append("\"screenTimeoutSeconds\":").append(screenTimeoutSeconds).append(",");
        json.append("\"refreshRate\":").append(refreshRate).append(",");
        json.append("\"brightness\":").append(brightness).append(",");
        json.append("\"brightnessPercent\":").append(brightnessPercent).append(",");
        json.append("\"manufacturer\":\"").append(manufacturer != null ? manufacturer : "").append("\",");
        json.append("\"model\":\"").append(model != null ? model : "").append("\",");
        json.append("\"device\":\"").append(device != null ? device : "").append("\"");
        json.append("}");
        return json.toString();
    }
}
