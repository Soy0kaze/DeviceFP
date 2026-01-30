package com.kaze.devicefp.service;

import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;

import com.kaze.devicefp.model.BatteryInfo;
import com.kaze.devicefp.model.BatteryInfoManager;
import com.kaze.devicefp.model.CameraInfoManager;
import com.kaze.devicefp.model.CpuInfoReader;
import com.kaze.devicefp.model.DeviceFingerprint;
import com.kaze.devicefp.model.MemoryInfoReader;
import com.kaze.devicefp.model.ScreenInfo;
import com.kaze.devicefp.model.ScreenInfoManager;
import com.kaze.devicefp.model.SettingsSettings;
import com.kaze.devicefp.model.SimCardUtil;
import com.kaze.devicefp.model.StorageInfoManager;
import com.kaze.devicefp.model.WifiInfo;
import com.kaze.devicefp.util.FileHelper;
import com.kaze.devicefp.util.ShellExecutor;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;

/**
 * 设备指纹收集服务
 * 提供收集各种设备指纹信息的方法框架
 */
public class DeviceFingerprintService {
    
    private static final String TAG = "DeviceFingerprintService";

    // ========== Native 方法声明（暂时不使用，保留代码） ==========
    static {
        try {
            System.loadLibrary("devicefp");
            Log.d(TAG, "Native library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            Log.d(TAG, "Native library not available (this is OK if using Java API)");
        }
    }
    
    // Native 方法声明（保留，暂时不使用）
    @SuppressWarnings("unused")
    public native static String baseDeviceInfo();
    
    @SuppressWarnings("unused")
    public native static String getProperty(String key);
    
    @SuppressWarnings("unused")
    public native static String[] getAllProperties();
    // ========== Native 代码结束 ==========
    
    private Context context;
    private Map<String, String> propertyCache; // 缓存属性（使用 Java API 获取）
    
    public DeviceFingerprintService(Context context) {
        this.context = context;
        this.propertyCache = new HashMap<>();
        Log.d(TAG, "DeviceFingerprintService constructor called");
        loadProperties();
    }
    
    /**
     * 加载属性到缓存（使用 Java API）
     */
    private void loadProperties() {
        Log.d(TAG, "loadProperties() called - using Java API");
        try {
            // 使用 Java API 获取系统属性
            loadPropertiesFromJava();
            Log.d(TAG, "Loaded " + propertyCache.size() + " properties into cache");
        } catch (Exception e) {
            Log.e(TAG, "Exception in loadProperties()", e);
            e.printStackTrace();
        }
    }
    public static String getKernelVersionFromUname() {
        try {
            Process process = Runtime.getRuntime().exec("uname -a");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            process.waitFor();
            reader.close();

            if (line != null) {
                // 示例输出: Linux localhost 4.14.117-gabc123 #1 SMP PREEMPT ...
                // 内核版本通常是第三个字段
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    return parts[2]; // 返回内核版本，如 "4.14.117-gabc123"
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 使用 Java API 加载属性
     */
    private void loadPropertiesFromJava() {
        // 使用 Build 类获取设备信息
        propertyCache.put("ro.product.brand", Build.BRAND);
        propertyCache.put("ro.product.model", Build.MODEL);
        propertyCache.put("ro.product.manufacturer", Build.MANUFACTURER);
        propertyCache.put("ro.product.name", Build.PRODUCT);
        propertyCache.put("ro.product.device", Build.DEVICE);
        propertyCache.put("ro.hardware", Build.HARDWARE);
        propertyCache.put("ro.build.version.release", Build.VERSION.RELEASE);
        propertyCache.put("ro.build.version.sdk", String.valueOf(Build.VERSION.SDK_INT));
        propertyCache.put("ro.build.version.incremental", Build.VERSION.INCREMENTAL);
        
        // 尝试使用反射获取 SystemProperties（如果可用）
        try {
            Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method getMethod = systemPropertiesClass.getMethod("get", String.class, String.class);
            
            // 尝试获取一些额外的属性
            String[] additionalProps = {
                "ro.build.version.security_patch",
                "ro.kernel.version",
                    "ro.product.cpu.abi"
            };

            for (String prop : additionalProps) {
                try {
                    String value = (String) getMethod.invoke(null, prop, "");

                    if ("ro.kernel.version".equals(prop)) {
                        if (value == null || value.isEmpty()) {
                            // 只有系统属性为空时才调用uname
                            String kernelVersion = getKernelVersionFromUname();
                            if (kernelVersion != null && !kernelVersion.isEmpty()) {
                                propertyCache.put(prop, kernelVersion);
                            } else {
                                // 如果uname也获取不到，可以缓存一个默认值或null
                                propertyCache.put(prop, "unknown");
                            }
                        } else {
                            // 系统属性有值，直接使用
                            propertyCache.put(prop, value);
                        }
                    } else {
                        // 非kernel.version属性，按原逻辑处理
                        if (value != null && !value.isEmpty()) {
                            propertyCache.put(prop, value);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // 可以考虑缓存一个错误标记
                    if ("ro.kernel.version".equals(prop)) {
                        propertyCache.put(prop, "error");
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "SystemProperties not available, using Build class only");
        }
    }
    
    /**
     * 重新加载属性（用于刷新数据）
     */
    public void reloadProperties() {
        Log.d(TAG, "reloadProperties() called");
        propertyCache.clear();
        loadProperties();
    }
    
    /**
     * 从属性缓存中获取值
     */
    private String getPropertyValue(String key) {
        String value = propertyCache.get(key);
        if (value == null || value.isEmpty()) {
            return "未获取";
        }
        return value;
    }
    
    /**
     * 获取所有设备指纹信息
     * @return 设备指纹信息列表
     */
    public List<DeviceFingerprint> getAllFingerprints() {
        Log.d(TAG, "getAllFingerprints() called, cache size: " + propertyCache.size());
        
        // 如果缓存为空，尝试重新加载
        if (propertyCache.isEmpty()) {
            Log.d(TAG, "Cache is empty, reloading properties");
            reloadProperties();
        }
        
        List<DeviceFingerprint> fingerprints = new ArrayList<>();
        
        // 设备基本信息
        fingerprints.addAll(getDeviceInfo());
        
        // 系统信息
        fingerprints.addAll(getSystemInfo());
        
        // 硬件信息
        fingerprints.addAll(getHardwareInfo());
        
        // 屏幕信息
        fingerprints.addAll(getDisplayInfo());

        //摄像头信息
        fingerprints.addAll(getCameraInfo());
        
        // 网络信息
        fingerprints.addAll(getNetworkInfo());
        
        // 应用信息
        fingerprints.addAll(getAppInfo());
        
        // 其他标识符
        fingerprints.addAll(getIdentifiers());

        return fingerprints;
    }
    
    /**
     * 获取设备基本信息
     */
    private List<DeviceFingerprint> getDeviceInfo() {
        List<DeviceFingerprint> list = new ArrayList<>();
        String category = "设备信息";
        
        // 使用 PropertyParser 解析的属性
        String brand = getPropertyValue("ro.product.brand");
        String model = getPropertyValue("ro.product.model");
        String manufacturer = getPropertyValue("ro.product.manufacturer");
        String productName = getPropertyValue("ro.product.name");
        String deviceName = getPropertyValue("ro.product.device");
        String hardware = getPropertyValue("ro.hardware");
        String radioV = Build.getRadioVersion();
        
        list.add(new DeviceFingerprint(category, "设备品牌", brand, 
                brand.equals("未获取") ? "未获取" : "已获取"));
        list.add(new DeviceFingerprint(category, "设备型号", model, 
                model.equals("未获取") ? "未获取" : "已获取"));
        list.add(new DeviceFingerprint(category, "制造商", manufacturer, 
                manufacturer.equals("未获取") ? "未获取" : "已获取"));
        list.add(new DeviceFingerprint(category, "产品名称", productName, 
                productName.equals("未获取") ? "未获取" : "已获取"));
        list.add(new DeviceFingerprint(category, "设备名称", deviceName, 
                deviceName.equals("未获取") ? "未获取" : "已获取"));
        list.add(new DeviceFingerprint(category, "硬件", hardware, 
                hardware.equals("未获取") ? "未获取" : "已获取"));
        list.add(new DeviceFingerprint(category, "基带", radioV, radioV.isEmpty()?"未获取":"已获取"));
        
        return list;
    }
    
    /**
     * 获取系统信息
     */
    private List<DeviceFingerprint> getSystemInfo() {
        List<DeviceFingerprint> list = new ArrayList<>();
        String category = "系统信息";
        
        // 使用 Java API 获取系统信息
        String androidVersion = getPropertyValue("ro.build.version.release");
        String apiLevel = getPropertyValue("ro.build.version.sdk");
        String buildVersion = getPropertyValue("ro.build.version.incremental");
        String securityPatch = getPropertyValue("ro.build.version.security_patch");
        String kernelVersion = getPropertyValue("ro.kernel.version");
        
        // 使用 Java API 获取语言和时区
        String language = Locale.getDefault().getLanguage();
        String timezone = TimeZone.getDefault().getID();
        
        list.add(new DeviceFingerprint(category, "Android版本", androidVersion, 
                androidVersion.equals("未获取") ? "未获取" : "已获取"));
        list.add(new DeviceFingerprint(category, "API级别", apiLevel, 
                apiLevel.equals("未获取") ? "未获取" : "已获取"));
        list.add(new DeviceFingerprint(category, "构建版本", buildVersion, 
                buildVersion.equals("未获取") ? "未获取" : "已获取"));
        list.add(new DeviceFingerprint(category, "安全补丁级别", securityPatch, 
                securityPatch.equals("未获取") ? "未获取" : "已获取"));
        list.add(new DeviceFingerprint(category, "内核版本", kernelVersion, 
                kernelVersion.equals("未获取") ? "未获取" : "已获取"));
        list.add(new DeviceFingerprint(category, "系统语言", language, 
                language.isEmpty() ? "未获取" : "已获取"));
        list.add(new DeviceFingerprint(category, "时区", timezone, 
                timezone.isEmpty() ? "未获取" : "已获取"));
        
        return list;
    }

    /**
     * 获取硬件信息
     */
    private List<DeviceFingerprint> getHardwareInfo() {
        List<DeviceFingerprint> list = new ArrayList<>();
        String category = "硬件信息";
        
        // 使用 CpuInfoReader 获取CPU相关信息
        CpuInfoReader cpuInfoReader = new CpuInfoReader();
        
        // CPU架构
        String cpuAbi = getPropertyValue("ro.product.cpu.abi");
        list.add(new DeviceFingerprint(category, "CPU架构", cpuAbi, 
                cpuAbi.equals("未获取") ? "未获取" : "已获取"));
        
        // CPU核心数
        int cpuCoreCount = cpuInfoReader.getCpuCoreCount();
        String coreCountStr = cpuCoreCount > 0 ? String.valueOf(cpuCoreCount) : "未获取";
        list.add(new DeviceFingerprint(category, "CPU核心数", coreCountStr, 
                cpuCoreCount > 0 ? "已获取" : "未获取"));
        
        // CPU频率（根据核心数展示）
        String cpuFreq = cpuInfoReader.getCpuFrequencyInfo();
        list.add(new DeviceFingerprint(category, "CPU频率", cpuFreq, 
                cpuFreq.equals("未获取") ? "未获取" : "已获取"));
        
        // 内存信息
        MemoryInfoReader memoryInfoReader = new MemoryInfoReader();
        String memoryInfo = memoryInfoReader.getFormattedMemoryInfo();
        list.add(new DeviceFingerprint(category, "内存信息", memoryInfo, 
                memoryInfo.equals("未获取") ? "未获取" : "已获取"));
        
        // 存储信息（合并总存储空间和可用存储空间）
        try {
            long totalStorage = StorageInfoManager.getTotalStorageCapacity(context);
            long availableStorage = StorageInfoManager.getTotalAvailableStorage(context);
            String storageInfo = totalStorage + "/" + availableStorage;
            list.add(new DeviceFingerprint(category, "存储信息", storageInfo, "已获取"));
        } catch (Exception e) {
            Log.e(TAG, "Error getting storage info", e);
            list.add(new DeviceFingerprint(category, "存储信息", "获取失败", "未获取"));
        }
        
        // 电池信息（拆分为多个条目）
        try {
            BatteryInfo batteryInfo = BatteryInfoManager.getBatteryInfo(context);
            if (batteryInfo != null) {
                // 1. 电量
                String levelStr = batteryInfo.getLevel() + "%";
                list.add(new DeviceFingerprint(category, "电量", levelStr, "已获取"));
                
                // 2. 健康状态
                String healthStr = batteryInfo.getHealthDescription();
                list.add(new DeviceFingerprint(category, "健康状态", healthStr, "已获取"));
                
                // 3. 电压（动态变化展示）
                String voltageStr = String.format("%.2fV", batteryInfo.getVoltageVolts());
                list.add(new DeviceFingerprint(category, "电压", voltageStr, "已获取"));
                
                // 4. 温度（动态变化展示）
                String temperatureStr = String.format("%.1f°C", batteryInfo.getTemperatureCelsius());
                list.add(new DeviceFingerprint(category, "温度", temperatureStr, "已获取"));
                
                // 5. 容量
                if (batteryInfo.getCapacity() > 0) {
                    String capacityStr = batteryInfo.getCapacity() + "mAh";
                    list.add(new DeviceFingerprint(category, "容量", capacityStr, "已获取"));
                } else {
                    list.add(new DeviceFingerprint(category, "容量", "未获取", "未获取"));
                }
                
                // 6. 充电方式
                String chargingMethod;
                if (batteryInfo.getPlugged() == 0) {
                    // 没有连接充电器
                    chargingMethod = "没充电";
                } else {
                    // 根据充电类型显示
                    if (batteryInfo.isAcCharging()) {
                        chargingMethod = "交流电充电";
                    } else if (batteryInfo.isUsbCharging()) {
                        chargingMethod = "USB充电";
                    } else if (batteryInfo.isWirelessCharging()) {
                        chargingMethod = "无线充电";
                    } else {
                        // 其他充电方式，使用描述
                        chargingMethod = batteryInfo.getPluggedDescription();
                    }
                }
                list.add(new DeviceFingerprint(category, "充电方式", chargingMethod, "已获取"));
            } else {
                list.add(new DeviceFingerprint(category, "电量", "获取失败", "未获取"));
                list.add(new DeviceFingerprint(category, "健康状态", "获取失败", "未获取"));
                list.add(new DeviceFingerprint(category, "电压", "获取失败", "未获取"));
                list.add(new DeviceFingerprint(category, "温度", "获取失败", "未获取"));
                list.add(new DeviceFingerprint(category, "容量", "获取失败", "未获取"));
                list.add(new DeviceFingerprint(category, "充电方式", "获取失败", "未获取"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting battery info", e);
            list.add(new DeviceFingerprint(category, "电量", "获取失败", "未获取"));
            list.add(new DeviceFingerprint(category, "健康状态", "获取失败", "未获取"));
            list.add(new DeviceFingerprint(category, "电压", "获取失败", "未获取"));
            list.add(new DeviceFingerprint(category, "温度", "获取失败", "未获取"));
            list.add(new DeviceFingerprint(category, "容量", "获取失败", "未获取"));
            list.add(new DeviceFingerprint(category, "充电方式", "获取失败", "未获取"));
        }
        
        return list;
    }
    
    /**
     * 获取屏幕信息（使用 ScreenInfoManager / ScreenInfo）
     */
    private List<DeviceFingerprint> getDisplayInfo() {
        List<DeviceFingerprint> list = new ArrayList<>();
        String category = "屏幕信息";

        try {
            String screenPhysicalSize = ScreenInfoManager.getScreenPhysicalSize(context);

            ScreenInfo info = ScreenInfoManager.getCompleteScreenInfo(context);

            // 分辨率（宽x高）
            String resolution = info.getResolution();
            list.add(new DeviceFingerprint(category, "分辨率", resolution, "已获取"));
            list.add(new DeviceFingerprint(category, "屏幕计算尺寸", screenPhysicalSize, (TextUtils.isEmpty(screenPhysicalSize))?"为获取":"已获取"));

            // 屏幕宽度/高度（像素）
            list.add(new DeviceFingerprint(category, "屏幕宽度", String.valueOf(info.getWidthPixels()) + " px", "已获取"));
            list.add(new DeviceFingerprint(category, "屏幕高度", String.valueOf(info.getHeightPixels()) + " px", "已获取"));

            // 宽高比
            String aspectRatio = info.getAspectRatio();
            list.add(new DeviceFingerprint(category, "宽高比", aspectRatio != null ? aspectRatio : "未知", "已获取"));

            // 屏幕尺寸与类别
            list.add(new DeviceFingerprint(category, "屏幕尺寸", String.format("%.1f 英寸", info.getDiagonalInches()), "已获取"));
            list.add(new DeviceFingerprint(category, "屏幕类别", info.getSizeCategory(), "已获取"));

            // 密度相关
            list.add(new DeviceFingerprint(category, "屏幕密度", String.valueOf(info.getDensity()), "已获取"));
            list.add(new DeviceFingerprint(category, "密度类别", info.getDensityCategory(), "已获取"));
            list.add(new DeviceFingerprint(category, "PPI", String.valueOf(info.getPpi()), "已获取"));
            list.add(new DeviceFingerprint(category, "XDPI / YDPI", String.format("%.1f / %.1f", info.getXdpi(), info.getYdpi()), "已获取"));

            // 系统 UI 高度
            list.add(new DeviceFingerprint(category, "状态栏高度", info.getStatusBarHeight() + " px", "已获取"));
            list.add(new DeviceFingerprint(category, "导航栏高度", info.getNavigationBarHeight() + " px", "已获取"));

            // 刷新率
            list.add(new DeviceFingerprint(category, "刷新率", String.format("%.1f Hz", info.getRefreshRate()), "已获取"));
            if (info.getSupportedRefreshRates() != null && info.getSupportedRefreshRates().length > 0) {
                StringBuilder rates = new StringBuilder();
                for (float r : info.getSupportedRefreshRates()) {
                    if (rates.length() > 0) rates.append(", ");
                    rates.append(String.format("%.0f Hz", r));
                }
                list.add(new DeviceFingerprint(category, "支持刷新率", rates.toString(), "已获取"));
            }

            // 亮度
            list.add(new DeviceFingerprint(category, "当前亮度", String.format("%d (%.0f%%)", info.getBrightness(), info.getBrightnessPercent()), "已获取"));

            // 屏幕超时
            list.add(new DeviceFingerprint(category, "屏幕超时", info.getScreenTimeoutFormatted(), "已获取"));

            // 设备信息（与屏幕相关）
            String manufacturer = info.getManufacturer();
            String model = info.getModel();
            list.add(new DeviceFingerprint(category, "屏幕制造商", manufacturer != null ? manufacturer : "未知", "已获取"));
            list.add(new DeviceFingerprint(category, "屏幕型号", model != null ? model : "未知", "已获取"));

            int screenBrightnessMode = Settings.System.getInt(context.getContentResolver(), "screen_brightness_mode");
            list.add(new DeviceFingerprint(category, "亮度模式", screenBrightnessMode == 0 ? "手动":"自动", "已获取"));
            int screenBright = ScreenInfoManager.getScreenBright(context);
            list.add(new DeviceFingerprint(category, "亮度大小", String.valueOf(screenBright), "已获取"));

            int accelerometer_rotationMode = Settings.System.getInt(context.getContentResolver(), "accelerometer_rotation");
            list.add(new DeviceFingerprint(category, "旋转模式", accelerometer_rotationMode == 0 ? "锁定":"自动", "已获取"));



        } catch (Exception e) {
            Log.e(TAG, "获取屏幕信息失败", e);
            list.add(new DeviceFingerprint(category, "屏幕信息", "获取失败", "未获取"));
        }

        return list;
    }
    private List<DeviceFingerprint> getCameraInfo() {
        List<DeviceFingerprint> list = new ArrayList<>();
        String category = "摄像头信息";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CameraInfoManager cameraHelper = new CameraInfoManager (context);
            // 获取摄像头总数
            int totalCameras = cameraHelper.getCameraCount();
            Log.d("Camera", "Total cameras: " + totalCameras);
            list.add(new DeviceFingerprint(category, "总摄像头数量", String.valueOf(totalCameras), "已获取"));

            // 获取前后置摄像头数量
            int frontCount = cameraHelper.getFrontCameraCount();
            int backCount = cameraHelper.getBackCameraCount();
            list.add(new DeviceFingerprint(category, "前置数量", String.valueOf(frontCount), "已获取"));
            list.add(new DeviceFingerprint(category, "后置数量", String.valueOf(backCount), "已获取"));

            // 获取所有摄像头详细信息
            List<CameraInfoManager .CameraInfo> allCameras = cameraHelper.getAllCameras();
            StringBuilder stringBuilder = new StringBuilder();
            for (CameraInfoManager .CameraInfo camera : allCameras) {
                stringBuilder.append(camera.toString()).append("\n");
                List<Size> previewSizes = camera.getPreviewSizes();
                if (!previewSizes.isEmpty()) {
                    stringBuilder.append("Max preview: ").append(previewSizes.get(0).getWidth()).append("x").append(previewSizes.get(0).getHeight()).append("\n");
                }
            }
            list.add(new DeviceFingerprint(category, "摄像头详细信息", stringBuilder.toString(), (TextUtils.isEmpty(stringBuilder))?"未获取":"已获取"));

            // 获取主摄像头
            CameraInfoManager .CameraInfo mainCamera = cameraHelper.getMainCamera();
            if (mainCamera != null) {
                list.add(new DeviceFingerprint(category, "主摄像头", mainCamera.getCameraId() + "号", "已获取"));
            } else {
                list.add(new DeviceFingerprint(category, "主摄像头", "没有获取到主摄像头", "未获取"));
            }
        }

        return list;
    }
    
    /**
     * 获取网络信息
     */
    private List<DeviceFingerprint> getNetworkInfo() {
        List<DeviceFingerprint> list = new ArrayList<>();
        String category = "网络信息";
        WifiInfo wifiInfo = new WifiInfo(this.context);
        boolean wifiStatus = wifiInfo.getWifiEnabled();
        String connectType = wifiInfo.getConnectionState();
        String ipinfo = wifiInfo.getIpInfo();
        String gatway = wifiInfo.getGateway();
        wifiInfo.setSimInfo(context);
        int simCount = WifiInfo.simCount(context);
        int maxSub = WifiInfo.getMaxActiveSubscriptionCount(context);
        int mobileNetStatus = WifiInfo.mobileNetStatus(context);

        list.add(new DeviceFingerprint(category, "WiFi是否开启", String.valueOf(wifiStatus), "已获取"));
        list.add(new DeviceFingerprint(category, "移动数据状态", mobileNetStatus == 0?"开启":"关闭/异常", "已获取"));
        list.add(new DeviceFingerprint(category, "连接类型", connectType, "已获取"));
        list.add(new DeviceFingerprint(category, "IP地址", ipinfo, "已获取"));
        list.add(new DeviceFingerprint(category, "网关", gatway, "已获取"));
        list.add(new DeviceFingerprint(category, "卡槽数量", String.valueOf(simCount), (simCount > 0) ? "已获取":"未获取"));
        list.add(new DeviceFingerprint(category, "卡最大订阅数量", String.valueOf(maxSub), (maxSub > 0) ? "已获取":"未获取"));
        list.add(new DeviceFingerprint(category, "是否插卡", wifiInfo.getSimCard(), "已获取"));
        if ("插卡".equals(wifiInfo.getSimCard())){
            String simOperatorName = SimCardUtil.getSimOperatorName(context);
            list.add(new DeviceFingerprint(category, "运营商名字", simOperatorName, "已获取"));
            list.add(new DeviceFingerprint(category, "sim国家", wifiInfo.getSimCountry(), "已获取"));
            list.add(new DeviceFingerprint(category, "mnc|mcc", wifiInfo.getMncMcc(), "已获取"));
        }
        try {
            String netName = WifiInfo.netWorkName();
            list.add(new DeviceFingerprint(category, "NetName", netName, (TextUtils.isEmpty(netName))?"未获取":"已获取"));
        } catch (Exception e){

        }
        String httpAgent = WifiInfo.get_httpAgent();
        list.add(new DeviceFingerprint(category, "HttpAgent", httpAgent, (TextUtils.isEmpty(httpAgent))?"未获取/无内容":"已获取"));
        return list;
    }
    
    /**
     * 获取应用信息
     */
    private List<DeviceFingerprint> getAppInfo() {
        List<DeviceFingerprint> list = new ArrayList<>();
        String category = "应用信息";
        
        // 使用 Java API 获取应用信息
        try {
            String packageName = context.getPackageName();
            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);
            String signature = SettingsSettings.getAppSignature(context);

            
            // 应用包名
            list.add(new DeviceFingerprint(category, "应用包名", packageName, "已获取"));
            // 签名
            list.add(new DeviceFingerprint(category, "应用签名", signature, "已获取"));
            
            // 应用版本
            String versionName = packageInfo.versionName != null ? packageInfo.versionName : "未知";
            int versionCode = packageInfo.versionCode;
            String version = versionName + " (" + versionCode + ")";
            list.add(new DeviceFingerprint(category, "应用版本", version, "已获取"));
            
            // 安装时间
            long installTime = packageInfo.firstInstallTime;
            String installTimeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", 
                    Locale.getDefault()).format(new Date(installTime));
            list.add(new DeviceFingerprint(category, "安装时间", installTimeStr, "已获取"));
        } catch (Exception e) {
            Log.e(TAG, "Error getting app info", e);
            list.add(new DeviceFingerprint(category, "应用包名", "获取失败", "未获取"));
            list.add(new DeviceFingerprint(category, "应用版本", "获取失败", "未获取"));
            list.add(new DeviceFingerprint(category, "安装时间", "获取失败", "未获取"));
        }
        
        // 是否为系统预装应用
        boolean isSystemApp = isSystemPreInstalledApp();
        String systemAppValue = isSystemApp ? "是" : "否";
        String systemAppStatus = "已获取";
        list.add(new DeviceFingerprint(category, "是否为系统预装应用", systemAppValue, systemAppStatus));
        String android_id = SettingsSettings.getAndroidId(this.context);
        list.add(new DeviceFingerprint(category, "Android ID", android_id, (!(TextUtils.isEmpty(android_id))) ? "已获取":"未获取"));
        String path = this.context.getPackageResourcePath().replace("/data/app/", "").replace("/base.apk", "");
        list.add(new DeviceFingerprint(category, "APP路径", path, (!(TextUtils.isEmpty(path))) ? "已获取":"未获取"));
        return list;
    }

    /**
     * 检查当前应用是否为系统预装应用
     * @return true表示是系统预装应用，false表示不是
     */
    public boolean isSystemPreInstalledApp() {
        try {
            // 获取当前应用的包名
            String packageName = context.getPackageName();

            // 获取PackageManager
            PackageManager packageManager = context.getPackageManager();

            // 获取应用的ApplicationInfo
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);

            // 检查 FLAG_SYSTEM 标志位
            // FLAG_SYSTEM = 1 (0x00000001)
            // 如果 flags 的第0位为1，说明是系统应用
            return (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;

        } catch (PackageManager.NameNotFoundException e) {
            // 理论上不会发生，因为是自己应用的包名
            // 如果发生异常，返回false
            return false;
        }
    }
    
    /**
     * 获取设备标识符
     */
    private List<DeviceFingerprint> getIdentifiers() {
        List<DeviceFingerprint> list = new ArrayList<>();
        String category = "其他信息";
        long cpuTime = SystemClock.uptimeMillis();
        long bootime = SystemClock.elapsedRealtime();
        List<String> systemFontPaths1 = SettingsSettings.getSystemFontPaths();
        String systemFontPaths = SettingsSettings.list2String(systemFontPaths1);
        String fontHash = SettingsSettings.calculateSHA1(systemFontPaths);
        String boot_id = FileHelper.readFileAsString("/proc/sys/kernel/random/boot_id");
        Set mediaCodecList = SettingsSettings.getMediaCodec();
        String mediaCodecSha1 = SettingsSettings.calculateSHA1(mediaCodecList.toString());
        String hardwareFeaturesString = SettingsSettings.getHardwareFeaturesString(this.context);
        long androidUptime = SettingsSettings.getAndroidUptime();
        String androidSystemSignatureHash = SettingsSettings.getAndroidSystemSignatureHash(context);

        list.add(new DeviceFingerprint(category, "android包签名", androidSystemSignatureHash, "已获取"));
        list.add(new DeviceFingerprint(category, "系统启动时间", String.valueOf(androidUptime), "已获取"));
        list.add(new DeviceFingerprint(category, "CPU运行时间", String.valueOf(cpuTime), ((TextUtils.isEmpty(String.valueOf(cpuTime)))) ? "未获取":"已获取"));
        list.add(new DeviceFingerprint(category, "开机运行时间", String.valueOf(bootime), ((TextUtils.isEmpty(String.valueOf(bootime)))) ? "未获取":"已获取"));
//        list.add(new DeviceFingerprint(category, "字体列表", systemFontPaths, (systemFontPaths.isEmpty()) ? "未获取":"已获取"));
        list.add(new DeviceFingerprint(category, "字体数量和哈希", systemFontPaths1.size() + "个/" + fontHash, (systemFontPaths1.isEmpty()) ? "未获取":"已获取"));
        list.add(new DeviceFingerprint(category, "媒体解码器数量和哈希", mediaCodecList.size() + "个/" + mediaCodecSha1, (mediaCodecList.isEmpty()) ? "未获取":"已获取"));
        list.add(new DeviceFingerprint(category, "硬件功能", hardwareFeaturesString, "已获取"));


        list.add(new DeviceFingerprint(category, "boot id", boot_id, (boot_id.isEmpty()) ? "未获取":"已获取"));

        String bootCount = SettingsSettings.getBootCount(this.context);
        list.add(new DeviceFingerprint(category, "开机计数(两种获取)", bootCount, ("/".equals(bootCount)) ? "未获取":"已获取"));

        int ringerMode = SettingsSettings.getRingerMode(context);
        String ringerModeS = "";
        if (ringerMode == 0){
            ringerModeS = "静音";
        }
        if (ringerMode == 1){
            ringerModeS = "震动";
        }
        if (ringerMode == 2){
            ringerModeS = "初音";
        }
        list.add(new DeviceFingerprint(category, "铃声模式", ringerModeS, ("".equals(ringerModeS)) ? "未获取":"已获取"));
        StringBuilder audioInfo = SettingsSettings.getAudioInfo(context);
        list.add(new DeviceFingerprint(category, "铃声大小", audioInfo.toString(),"已获取"));

        int adbEnabled = Settings.Secure.getInt(context.getContentResolver(), "adb_enabled", 0);
        list.add(new DeviceFingerprint(category, "是否开启adb", adbEnabled == 1?"开启":"关闭", "已获取"));

        boolean keyguard = ((KeyguardManager) context.getSystemService("keyguard")).inKeyguardRestrictedInputMode();
        list.add(new DeviceFingerprint(category, "是否处于锁屏", keyguard?"是":"否", "已获取"));

        String keyPinTime = SettingsSettings.getKeyPinTime(context);
        list.add(new DeviceFingerprint(category, "锁屏密码和编辑时间", keyPinTime, "已获取"));

        String fileHash = SettingsSettings.getFileHash();
        list.add(new DeviceFingerprint(category, "系统文件哈希", fileHash, "已获取"));
        int appOP = SettingsSettings.getAppOP(context);
        list.add(new DeviceFingerprint(category, "是否被授予悬浮窗", appOP == 1?"是":"否/检查异常", "已获取"));

        String installedAccessibilityServices = SettingsSettings.getInstalledAccessibilityServices(context);
        list.add(new DeviceFingerprint(category, "已安装辅助服务列表", installedAccessibilityServices, "已获取"));

        String inputMethod = SettingsSettings.getInputMethod(context);
        list.add(new DeviceFingerprint(category, "输入法列表", inputMethod, "已获取"));

        String inputDeviceInfo = SettingsSettings.getInputDeviceInfo(context);
        list.add(new DeviceFingerprint(category, "物理输入设备", inputDeviceInfo, "已获取"));

        String phoneMode = SettingsSettings.phoneMode(context);
        list.add(new DeviceFingerprint(category, "设备模式信息", phoneMode, "已获取"));

        String dexClassLoaderPath = SettingsSettings.getDexClassLoaderPath(context);
        list.add(new DeviceFingerprint(category, "DexClassLoader的路径列表", dexClassLoaderPath, "已获取"));

        List<String> serverList1 = SettingsSettings.getServerList();
        String serverList = SettingsSettings.calculateSHA1(serverList1.toString());
        list.add(new DeviceFingerprint(category,  "服务列表哈希", serverList1.size() + "项/" + serverList, "已获取"));

        String sensorDetails = SettingsSettings.getSensorDetails(context);
        list.add(new DeviceFingerprint(category,  "传感器哈希", SettingsSettings.calculateSHA1(sensorDetails), "已获取"));

        FeatureInfo[] systemFeatures = SettingsSettings.getSystemFeatures(context);
        StringBuilder result = new StringBuilder();
        assert systemFeatures != null;
        for (FeatureInfo feature : systemFeatures) {
            if (feature.name != null && !feature.name.isEmpty()) {
                result.append(feature.name).append("\n");
            }
        }
        list.add(new DeviceFingerprint(category,  "硬件软件列表哈希", SettingsSettings.calculateSHA1(result.toString()), "已获取"));
        list.add(new DeviceFingerprint(category, "Attestation", SettingsSettings.collectAttestationInfo(context), "已获取"));
        list.add(new DeviceFingerprint(category, "KeyStoreAttestation", SettingsSettings.tryBackupAttestation(context), "已获取"));

        String openGLInfo = SettingsSettings.getOpenGLInfo(context);
        list.add(new DeviceFingerprint(category, "OpenGL", openGLInfo, "已获取"));


        return list;
    }

}
