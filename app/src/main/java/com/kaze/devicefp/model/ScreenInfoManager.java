package com.kaze.devicefp.model;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;
import android.view.WindowMetrics;

import androidx.appcompat.app.AppCompatActivity;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class ScreenInfoManager {
    private static final String TAG = "ScreenInfoManager";

    /**
     * 获取完整的屏幕信息（整合所有功能）
     */
    public static ScreenInfo getCompleteScreenInfo(Context context) {
        ScreenInfo info = new ScreenInfo();

        try {
            // 1. 获取DisplayMetrics信息（对应cz.l.C()的功能）
            DisplayMetrics metrics = getDisplayMetrics(context);
            populateDisplayMetrics(info, metrics);

            // 2. 计算额外信息
            calculateExtraInfo(info, metrics);

            // 3. 获取系统UI高度
            getSystemUIHeights(context, info);

            // 4. 获取屏幕超时设置
            getScreenTimeoutSettings(context, info);

            // 5. 获取刷新率信息
            getRefreshRateInfo(context, info);

            // 6. 获取亮度信息
            getBrightnessInfo(context, info);

            // 7. 获取设备信息
            getDeviceInfo(info);

            // 8. 生成原始格式字符串
            info.setDisplayMetricsString(generateOriginalFormatString(metrics));

        } catch (Exception e) {
            Log.e(TAG, "获取屏幕信息失败", e);
        }

        return info;
    }

    /**
     * 获取DisplayMetrics（对应cz.l.C()的核心逻辑）
     */
    private static DisplayMetrics getDisplayMetrics(Context context) {
        // 原代码使用 KSProxy.applyOneRefs，我们这里用标准方法
        // 如果原代码通过代理获取，这里先用标准方法，实际使用时可以替换
        return context.getResources().getDisplayMetrics();
    }

    /**
     * 填充DisplayMetrics数据
     */
    private static void populateDisplayMetrics(ScreenInfo info, DisplayMetrics metrics) {
        info.setDensity(metrics.density);
        info.setWidthPixels(metrics.widthPixels);
        info.setHeightPixels(metrics.heightPixels);
        info.setScaledDensity(metrics.scaledDensity);
        info.setXdpi(metrics.xdpi);
        info.setYdpi(metrics.ydpi);
    }

    /**
     * 计算额外信息（屏幕尺寸、PPI、宽高比等）
     */
    private static void calculateExtraInfo(ScreenInfo info, DisplayMetrics metrics) {
        try {
            // 计算屏幕物理尺寸（英寸）
            float widthInches = metrics.widthPixels / metrics.xdpi;
            float heightInches = metrics.heightPixels / metrics.ydpi;
            float diagonalInches = (float) Math.sqrt(
                    Math.pow(widthInches, 2) + Math.pow(heightInches, 2)
            );

            info.setDiagonalInches(roundFloat(diagonalInches, 2));

            // 计算PPI
            int ppi = (int) (Math.sqrt(
                    Math.pow(metrics.widthPixels, 2) + Math.pow(metrics.heightPixels, 2)
            ) / diagonalInches);
            info.setPpi(ppi);

            // 计算宽高比
            int gcd = gcd(metrics.widthPixels, metrics.heightPixels);
            int widthRatio = metrics.widthPixels / gcd;
            int heightRatio = metrics.heightPixels / gcd;
            info.setAspectRatio(widthRatio + ":" + heightRatio);

        } catch (Exception e) {
            Log.e(TAG, "计算额外屏幕信息失败", e);
        }
    }

    /**
     * 获取系统UI高度（状态栏、导航栏、ActionBar）
     */
    private static void getSystemUIHeights(Context context, ScreenInfo info) {
        try {
            // 1. 状态栏高度
            int statusBarHeight = 0;
            int resourceId = context.getResources().getIdentifier(
                    "status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
            }
            info.setStatusBarHeight(statusBarHeight);

            // 2. 导航栏高度
            int navigationBarHeight = 0;
            resourceId = context.getResources().getIdentifier(
                    "navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                navigationBarHeight = context.getResources().getDimensionPixelSize(resourceId);
            } else {
                // 备选方法
                resourceId = context.getResources().getIdentifier(
                        "navigation_bar_height_landscape", "dimen", "android");
                if (resourceId > 0) {
                    navigationBarHeight = context.getResources().getDimensionPixelSize(resourceId);
                }
            }
            info.setNavigationBarHeight(navigationBarHeight);

            // 3. ActionBar高度
            int actionBarHeight = 0;
            TypedValue tv = new TypedValue();
            if (context.getTheme().resolveAttribute(
                    android.R.attr.actionBarSize, tv, true)) {
                actionBarHeight = TypedValue.complexToDimensionPixelSize(
                        tv.data, context.getResources().getDisplayMetrics());
            }
            info.setActionBarHeight(actionBarHeight);

        } catch (Exception e) {
            Log.e(TAG, "获取系统UI高度失败", e);
        }
    }

    /**
     * 获取屏幕超时设置
     */
    @SuppressLint("NewApi")
    private static void getScreenTimeoutSettings(Context context, ScreenInfo info) {
        try {
            ContentResolver contentResolver = context.getContentResolver();

            // 获取屏幕超时设置（毫秒）
            int screenOffTimeout;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6.0+ 使用新API
                screenOffTimeout = Settings.System.getInt(
                        contentResolver,
                        Settings.System.SCREEN_OFF_TIMEOUT,
                        30000  // 默认30秒
                );
            } else {
                // 旧版本
                screenOffTimeout = Settings.System.getInt(
                        contentResolver,
                        "screen_off_timeout",
                        30000
                );
            }

            info.setScreenTimeout(screenOffTimeout);
            info.setScreenTimeoutSeconds(screenOffTimeout / 1000);

        } catch (Exception e) {
            Log.e(TAG, "获取屏幕超时设置异常", e);
            info.setScreenTimeout(30000);
            info.setScreenTimeoutSeconds(30);
        }
    }

    /**
     * 获取刷新率信息
     */
    private static void getRefreshRateInfo(Context context, ScreenInfo info) {
        try {
            WindowManager windowManager = (WindowManager)
                    context.getSystemService(Context.WINDOW_SERVICE);
            if (windowManager == null) return;

            Display display = windowManager.getDefaultDisplay();

            // 获取当前刷新率
            info.setRefreshRate(display.getRefreshRate());

            // 获取支持的刷新率（需要API 21+）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    android.view.Display.Mode[] modes = display.getSupportedModes();
                    if (modes != null && modes.length > 0) {
                        float[] refreshRates = new float[modes.length];
                        for (int i = 0; i < modes.length; i++) {
                            refreshRates[i] = modes[i].getRefreshRate();
                        }
                        info.setSupportedRefreshRates(refreshRates);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "获取支持的刷新率失败", e);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "获取刷新率信息失败", e);
        }
    }

    /**
     * 获取亮度信息
     */
    @SuppressLint("NewApi")
    private static void getBrightnessInfo(Context context, ScreenInfo info) {
        try {
            ContentResolver contentResolver = context.getContentResolver();

            // 获取当前亮度（0-255）
            int brightness = Settings.System.getInt(
                    contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    128  // 默认值
            );

            info.setBrightness(brightness);
            info.setBrightnessPercent((float) brightness / 255 * 100);

        } catch (Exception e) {
            Log.e(TAG, "获取亮度信息异常", e);
            info.setBrightness(128);
            info.setBrightnessPercent(50);
        }
    }

    /**
     * 获取设备信息
     */
    private static void getDeviceInfo(ScreenInfo info) {
        info.setManufacturer(Build.MANUFACTURER);
        info.setModel(Build.MODEL);
        info.setDevice(Build.DEVICE);
    }

    /**
     * 生成原始格式字符串（仿照cz.l.C()的格式）
     */
    private static String generateOriginalFormatString(DisplayMetrics metrics) {
        String str = ",";
        String raw = "[" + metrics.density + str + metrics.widthPixels + str +
                metrics.heightPixels + str + metrics.scaledDensity + str +
                metrics.xdpi + str + metrics.ydpi + "]";
        // 移除可能引起问题的字符
        return raw.replace("=", "").replace("&", "");
    }

    /**
     * 直接获取原始格式字符串（与cz.l.C()方法兼容）
     */
    public static String getDisplayMetricsOriginalString(Context context) {
        try {
            // 这里可以添加代理逻辑，如果原代码使用KSProxy
            // 为了兼容，我们先实现标准方法

            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            return generateOriginalFormatString(metrics);

        } catch (Exception e) {
            Log.e(TAG, "获取DisplayMetrics字符串失败", e);
            return "[0,0,0,0,0,0]";
        }
    }

    /**
     * 计算最大公约数（用于计算宽高比）
     */
    private static int gcd(int a, int b) {
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    /**
     * 浮点数四舍五入
     */
    private static float roundFloat(float value, int decimalPlaces) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 0f;
        }
        BigDecimal bd = new BigDecimal(Float.toString(value));
        bd = bd.setScale(decimalPlaces, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    /**
     * 获取屏幕可用高度（排除状态栏和导航栏）
     */
    public static int getUsableHeight(Context context) {
        ScreenInfo info = getCompleteScreenInfo(context);
        return info.getHeightPixels() - info.getStatusBarHeight() - info.getNavigationBarHeight();
    }

    /**
     * 检查是否为全面屏
     */
    public static boolean isFullScreenDevice(Context context) {
        ScreenInfo info = getCompleteScreenInfo(context);
        float aspectRatio = (float) info.getWidthPixels() / info.getHeightPixels();
        return aspectRatio > 1.85f; // 宽高比大于18:9
    }

    /**
     * 检查是否有导航栏
     */
    public static boolean hasNavigationBar(Context context) {
        ScreenInfo info = getCompleteScreenInfo(context);
        return info.getNavigationBarHeight() > 0;
    }

    /**
     * 设置屏幕超时时间（需要权限）
     */
    @SuppressLint("NewApi")
    public static boolean setScreenTimeout(Context context, int timeoutSeconds) {
        try {
            ContentResolver contentResolver = context.getContentResolver();
            int timeoutMs = timeoutSeconds * 1000;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return Settings.System.putInt(
                        contentResolver,
                        Settings.System.SCREEN_OFF_TIMEOUT,
                        timeoutMs
                );
            } else {
                return Settings.System.putInt(
                        contentResolver,
                        "screen_off_timeout",
                        timeoutMs
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "设置屏幕超时失败", e);
            return false;
        }
    }

    /**
     * 获取屏幕信息报告（简版）
     */
    public static String getScreenSummary(Context context) {
        ScreenInfo info = getCompleteScreenInfo(context);
        return info.getDetailedReport();
    }

    /**
     * 获取屏幕信息JSON
     */
    public static String getScreenInfoJson(Context context) {
        ScreenInfo info = getCompleteScreenInfo(context);
        return info.toJson();
    }

    /**
     * 获取特定信息的方法（方便单独调用）
     */
    public static int getStatusBarHeight(Context context) {
        ScreenInfo info = getCompleteScreenInfo(context);
        return info.getStatusBarHeight();
    }

    public static int getNavigationBarHeight(Context context) {
        ScreenInfo info = getCompleteScreenInfo(context);
        return info.getNavigationBarHeight();
    }

    public static int getScreenTimeoutSeconds(Context context) {
        ScreenInfo info = getCompleteScreenInfo(context);
        return info.getScreenTimeoutSeconds();
    }

    public static String getResolution(Context context) {
        ScreenInfo info = getCompleteScreenInfo(context);
        return info.getResolution();
    }
    /**
     * 获取设备屏幕的物理尺寸（宽高，单位：毫米）
     * 通过像素密度(DPI)和像素数量计算实际的物理尺寸
     */
    public static String getScreenPhysicalSize(Context context) {
        int widthPixels, heightPixels;

        // 1. 获取屏幕尺寸（像素）
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 使用新的API
            WindowMetrics windowMetrics = windowManager.getMaximumWindowMetrics();
            Rect bounds = windowMetrics.getBounds();
            widthPixels = bounds.width();
            heightPixels = bounds.height();
        } else {
            // 旧版本使用getRealSize
            Point realSize = new Point();
            display.getRealSize(realSize);
            widthPixels = realSize.x;
            heightPixels = realSize.y;
        }

        // 2. 获取屏幕的物理DPI（每英寸像素数）
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        float xdpi = metrics.xdpi;  // 水平方向的DPI
        float ydpi = metrics.ydpi;  // 垂直方向的DPI

        // 3. 检查DPI是否有效（不为0）
        if (xdpi > 0 && ydpi > 0) {
            // 4. 计算物理尺寸（毫米）
            // 公式：物理尺寸(mm) = (像素数 / DPI) * 25.4
            int widthMm = Math.round((widthPixels / xdpi) * 25.4f);
            int heightMm = Math.round((heightPixels / ydpi) * 25.4f);

            // 5. 确保高度<宽度（竖屏时的方向）
            if (widthMm > heightMm) {
                return heightMm + "mm * " + widthMm + "mm";
            } else {
                return widthMm + "mm * " + heightMm + "mm";
            }
        } else {
            return "";  // DPI无效时返回空
        }
    }
    public static int getScreenBright(Context context0) {
        try {
            return Settings.System.getInt(context0.getContentResolver(), "screen_brightness", 0xFF);
        }
        catch(Throwable unused_ex) {
            return 0;
        }
    }
}
