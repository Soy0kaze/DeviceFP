package com.kaze.devicefp.model;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;
import org.json.JSONObject;
import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.JSONObject;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.media.MediaCodecList;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Binder;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.InputDevice;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SettingsSettings {
    public static String getBootCount(Context context){
        String bootCount = Settings.Global.getString(
                context.getContentResolver(),
                "Phenotype_boot_count"
        );
        String bootCount1 = Settings.Global.getString(
                context.getContentResolver(),
                "boot_count"
        );
        return bootCount + "/" + bootCount1;
    }
    // 视频解码器信息
    public static Set getMediaCodec(){
        int codecCount = MediaCodecList.getCodecCount();
        HashSet hashSet = new HashSet();
        for (int i = 0; i < codecCount; i = i + 1) {
            hashSet.add(MediaCodecList.getCodecInfoAt(i).getName());
        }
        return hashSet;
    }
    public static String list2String(List<String> list){
        if (list.isEmpty()){
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++){
            sb.append(list.get(i)).append("\n");
        }
        return sb.toString();
    }
    public static String calculateSHA1(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hashBytes = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(input.hashCode());
        } catch (Exception e) {
            // 其他异常情况
            return "";
        }
    }
    /**
     * 获取系统字体文件路径列表
     *
     * @return 字体文件绝对路径列表
     */
    public static List<String> getSystemFontPaths() {
        List<String> fontPaths = new ArrayList<>();
        File fontsDir = new File("/system/fonts/");
        if (!fontsDir.exists()) {
            return fontPaths;
        }
        File[] fontFiles = fontsDir.listFiles();
        if (fontFiles != null) {
            for (File fontFile : fontFiles) {
                if (fontFile != null && fontFile.isFile()) {
                    fontPaths.add(fontFile.getAbsolutePath());
                }
            }
        }
        return fontPaths;
    }

    public static String getAndroidId(Context context) {
        if (context == null) {
            return "";
        }

        Cursor cursor = null;
        try {
            Uri uri = Uri.parse("content://settings/" + "secure");
            ContentResolver contentResolver = context.getContentResolver();

            cursor = contentResolver.query(uri, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex("name");
                int valueIndex = cursor.getColumnIndex("value");

                do {
                    if (nameIndex == -1 || valueIndex == -1) {
                        break;
                    }

                    String name = cursor.getString(nameIndex);
                    String value = cursor.getString(valueIndex);

                    // 只获取android_id
                    if ("android_id".equals(name) &&
                            !TextUtils.isEmpty(value) &&
                            value.length() < 20) {
                        return value;
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return "";
    }
    public static String getHardwareFeaturesString(Context context) {
        if (context == null) {
            return "Context为空，无法检测硬件功能";
        }

        StringBuilder result = new StringBuilder();
        try {
            PackageManager packageManager = context.getPackageManager();

            // 硬件功能列表与对应的中文描述
            String[][] features = {
                    {"android.hardware.camera", "后置摄像头"},
                    {"android.hardware.camera.autofocus", "自动对焦"},
                    {"android.hardware.camera.flash", "闪光灯"},
                    {"android.hardware.location", "位置服务"},
                    {"android.hardware.location.gps", "GPS"},
                    {"android.hardware.location.network", "网络定位"},
                    {"android.hardware.microphone", "麦克风"},
                    {"android.hardware.sensor.compass", "指南针"},
                    {"android.hardware.sensor.accelerometer", "加速度计"},
                    {"android.hardware.sensor.light", "光线传感器"},
                    {"android.hardware.sensor.proximity", "距离传感器"},
                    {"android.hardware.telephony", "电话功能"},
                    {"android.hardware.telephony.cdma", "CDMA网络"},
                    {"android.hardware.telephony.gsm", "GSM网络"},
                    {"android.hardware.touchscreen", "触摸屏"},
                    {"android.hardware.touchscreen.multitouch", "多点触控"},
                    {"android.hardware.touchscreen.multitouch.distinct", "多点触控区分"},
                    {"android.hardware.camera.front", "前置摄像头"},
                    {"android.hardware.wifi", "WiFi"},
                    {"android.hardware.bluetooth", "蓝牙"},
                    {"android.hardware.nfc", "NFC"},
                    {"android.hardware.fingerprint", "指纹识别"},
                    {"android.hardware.biometrics.face", "面部识别"},
                    {"android.hardware.screen.portrait", "竖屏支持"},
                    {"android.hardware.screen.landscape", "横屏支持"},
                    {"android.hardware.faketouch", "模拟触控"},
                    {"android.hardware.audio.output", "音频输出"}
            };

            // 遍历所有功能，检测并格式化输出
            for (String[] feature : features) {
                String featureKey = feature[0];
                String featureName = feature[1];
                boolean hasFeature = packageManager.hasSystemFeature(featureKey);

                // 格式化输出：功能名称 + 支持状态
                result.append(featureName)
                        .append(":")
                        .append(hasFeature ? "支持" : "不支持")
                        .append("\n");
            }

            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "检测硬件功能时发生异常：" + e.getMessage();
        }
    }
    public static int getRingerMode(Context context0) {
        try {
            return ((AudioManager)context0.getSystemService("audio")).getRingerMode();
        }
        catch(Exception unused_ex) {
            return -2;
        }
    }

    public static long getAndroidUptime() {
        double uptimeSeconds = parseUptimeFromProc();
        long currentTime = System.currentTimeMillis();
        long uptimeMillis = (long) (uptimeSeconds * 1000.0f);
        return currentTime - uptimeMillis;
    }
    private static double parseUptimeFromProc() {
        double uptime = 0;
        DataInputStream dataInputStream = null;
        BufferedReader bufferedReader = null;

        try {
            Process process = Runtime.getRuntime().exec("cat /proc/uptime");
            dataInputStream = new DataInputStream(process.getInputStream());
            bufferedReader = new BufferedReader(new InputStreamReader(dataInputStream));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                // /proc/uptime格式：第一个数字是运行时间（秒），第二个是空闲时间
                String[] parts = line.trim().split("\\s+");
                if (parts.length > 0) {
                    uptime = Double.parseDouble(parts[0]);
                    break; // 只需要第一行
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭资源
            try {
                if (bufferedReader != null) bufferedReader.close();
                if (dataInputStream != null) dataInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return uptime;
    }
    public static StringBuilder getAudioInfo(Context context) {
        StringBuilder result = new StringBuilder();

        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager == null) {
                result.append("服务对象为空");
                return result;
            }

            // 获取各音频流的音量信息
            addAudioStreamInfo(result, audioManager, 0, "通话");
            addAudioStreamInfo(result, audioManager, 1, "系统");
            addAudioStreamInfo(result, audioManager, 2, "铃声");
            addAudioStreamInfo(result, audioManager, 3, "媒体");
            addAudioStreamInfo(result, audioManager, 4, "闹钟");

            // 添加流8（辅助功能）
            addAudioStreamInfo(result, audioManager, 8, "辅助功能");

            // Android 8.0+ 添加流10
            if (Build.VERSION.SDK_INT >= 26) {
                addAudioStreamInfo(result, audioManager, 10, "辅助功能10");
            }

            if (result.length() == 0) {
                result.append("获取字段为空");
            }

        } catch (Throwable unused_ex) {
            result.append("发生异常");
        }

        return result;
    }

    private static void addAudioStreamInfo(StringBuilder builder, AudioManager audioManager,
                                           int streamType, String typeName) {
        int maxVolume = audioManager.getStreamMaxVolume(streamType);
        int currentVolume = audioManager.getStreamVolume(streamType);

        // 格式：声音类型:最大音量:当前音量
        if (builder.length() > 0) {
            builder.append("\n");
        }
        builder.append(typeName)
                .append(":")
                .append(maxVolume)
                .append(":")
                .append(currentVolume);
    }


    private static String calculateFileSHA1(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return null;
        }

        FileInputStream fis = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            fis = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            byte[] hashBytes = digest.digest();

            // 将字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            // SHA-1算法不存在，这通常不会发生
            return null;
        } catch (IOException e) {
            // 文件读取失败
            return null;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // 忽略关闭异常
                }
            }
        }
    }
    public static String getFileHash(){
        StringBuilder stringBuilder = new StringBuilder();
        /**
         *  "/system/lib64/libc.so","/system/lib64/libandroid_runtime.so","/system/lib64/libart.so","/apex/com.android.art/lib64/libart.so","/system/bin/linker64","/system/bin/app_process64";
         *  "/system/lib/libc.so","/system/lib/libandroid_runtime.so","/system/lib/libart.so","/apex/com.android.art/lib/libart.so","/system/bin/linker","/system/bin/app_process32";
         *
         */
        String[] files = new String[]{
                "/system/bin/app_process",
                "/system/bin/servicemanager",
                "/system/framework/framework.jar",
                "/system/lib64/libc.so",
                "/system/lib64/libandroid_runtime.so",
                "/system/lib64/libart.so",
                "/apex/com.android.art/lib64/libart.so",
                "/system/bin/linker64",
                "/system/bin/app_process64"};
        for(String filePath: files){
            stringBuilder.append(filePath).append(":").append(calculateFileSHA1(new File(filePath))).append("\n");
        }
        return stringBuilder.toString();
    }

    public static int getAppOP(Context context0) {
        try {
            Object object0 = context0.getSystemService("appops");
            if(object0 == null) {
                return 0;
            }

            Method method0 = object0.getClass().getMethod("checkOp", Integer.TYPE, Integer.TYPE, String.class);
            if(method0 == null) {
                return 0;
            }

            return ((int)(((Integer)method0.invoke(object0, ((int)24), ((int) Binder.getCallingUid()), context0.getPackageName())))) == 0 ? 1 : 0;
        }
        catch(Exception unused_ex) {
            return -2;
        }
    }
    public static String getInstalledAccessibilityServices(Context context) {
        if (context == null) {
            return null;
        }

        AccessibilityManager accessibilityManager =
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);

        if (accessibilityManager == null) {
            return null;
        }

        StringBuilder resultBuilder = new StringBuilder();
        Set<String> serviceSet = new HashSet<>(); // 用于去重

        try {
            List<AccessibilityServiceInfo> installedServices =
                    accessibilityManager.getInstalledAccessibilityServiceList();

            if (installedServices != null && !installedServices.isEmpty()) {
                for (AccessibilityServiceInfo serviceInfo : installedServices) {
                    // 提取服务ID，格式如：com.package.name/.ServiceClassName
                    String serviceId = getServiceId(serviceInfo);

                    if (serviceId != null && !serviceId.isEmpty() && !serviceSet.contains(serviceId)) {
                        if (resultBuilder.length() > 0) {
                            resultBuilder.append("|").append("\n");
                        }
                        resultBuilder.append(serviceId);
                        serviceSet.add(serviceId);
                    }
                }
            }
        } catch (Exception e) {
            // 忽略异常，返回已收集的信息
        }

        return resultBuilder.toString();
    }
    private static String getServiceId(AccessibilityServiceInfo serviceInfo) {
        if (serviceInfo == null || serviceInfo.getId() == null) {
            return "";
        }
        return serviceInfo.getId();
    }
    public static String getInputMethod(Context context) {
        try {
            if (context == null) {
                return "RISK_GET_FIELD_EMPTY";
            }

            InputMethodManager inputMethodManager =
                    (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

            if (inputMethodManager == null) {
                return "RISK_GET_FIELD_EMPTY";
            }

            List<InputMethodInfo> inputMethods = inputMethodManager.getInputMethodList();

            if (inputMethods == null || inputMethods.isEmpty()) {
                return "RISK_GET_FIELD_EMPTY";
            }

            StringBuilder str = new StringBuilder();
            Iterator<InputMethodInfo> iterator = inputMethods.iterator();

            while (iterator.hasNext()) {
                InputMethodInfo inputMethodInfo = iterator.next();
                str.append(inputMethodInfo.getId()).append("\n");
            }

            String result = str.toString();
            if (TextUtils.isEmpty(result)) {
                return "RISK_GET_FIELD_EMPTY";
            }

            if (result.endsWith(";")) {
                result = result.substring(0, (result.length() - 1));
            }

            return result;

        } catch (Exception e) {
            return "RISK_EXCEPTION_HAPPEN";
        }
    }
    public static String getKeyPinTime(Context context) {
        long latestTime = 0;

        // 1. 检查是否有安全锁屏
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager == null || !keyguardManager.isKeyguardSecure()) {
            return "没开锁屏:0";  // 没有安全锁屏或无法获取服务
        }

        // 2. 安全锁屏相关文件的路径数组
        String[] filePaths = new String[]{
                "/data/system/password.key",          // 密码锁屏文件
                "/data/system/gesture.key",           // 手势锁屏文件
                "/data/system/gatekeeper.password.key", // GateKeeper密码文件
                "/data/system/gatekeeper.gesture.key",  // GateKeeper手势文件
                "/data/system/gatekeeper.pattern.key"   // GateKeeper图案文件
        };

        // 3. 遍历文件，获取最新修改时间
        for (String filePath : filePaths) {
            File file = new File(filePath);
            if (file.exists()) {
                long lastModified = file.lastModified();
                if (lastModified > latestTime) {
                    latestTime = lastModified;
                }
            }
        }

        // 4. 返回结果
        return "开启锁屏:" + latestTime;  // 有安全锁屏:最新时间戳
    }
    public static String getInputDeviceInfo(Context context) {
        StringBuilder str = new StringBuilder();

        try {
            InputManager systemServic = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
            if (systemServic == null) {
                return "";
            }

            int[] inputDeviceI = systemServic.getInputDeviceIds();
            int len = inputDeviceI.length;

            for (int i = 0; i < len; i = i + 1) {
                InputDevice inputDevice = systemServic.getInputDevice(inputDeviceI[i]);
                if (inputDevice != null) {
                    String deviceName = inputDevice.getName();
                    if (deviceName != null && !deviceName.isEmpty()) {
                        str.append(deviceName).append("\n");
                    }
                }
            }

            if (str.length() > 0) {
                str.deleteCharAt((str.length() - 1));
            }

            return str.toString();
        } catch (Exception e) {
            return "";
        }
    }
    public static boolean protectMode(Context p0){
        try {
            // 检查夜光模式
            boolean nightDisplay = Settings.Secure.getInt(p0.getContentResolver(),
                    "night_display_activated", 0) == 1;

            // 检查纸张模式
            boolean paperMode = Settings.System.getInt(p0.getContentResolver(),
                    "screen_paper_mode_enabled", 0) == 1;

            // 检查颜色护眼
            boolean eyeProtect = Settings.System.getInt(p0.getContentResolver(),
                    "color_eyeprotect_enable", 0) == 1;

            // 任意一个开启就返回 true
            return nightDisplay || paperMode || eyeProtect;
        } catch (Exception e) {
            return false;
        }
    }
    public static boolean getPowerSaveMode(Context p0){
        try {
            PowerManager powerManager = (PowerManager) p0.getSystemService("power");

            // 1. 首先检查标准省电模式
            if (powerManager.isPowerSaveMode()) {
                return true;  // 标准省电模式已开启
            }

            // 2. 检查超级省电模式（厂商定制）
            int superSaveMode = Settings.System.getInt(p0.getContentResolver(),
                    "power_supersave_mode_open", 0);

            // 3. 检查其他省电模式（厂商定制）
            int powerSaveOpen = Settings.System.getInt(p0.getContentResolver(),
                    "POWER_SAVE_MODE_OPEN", 0);

            // 4. 检查智能省电模式（厂商定制）
            int smartEnable = Settings.System.getInt(p0.getContentResolver(),
                    "is_smart_enable", 0);

            // 只要有一个为1（开启），就返回true
            if (superSaveMode == 1 || powerSaveOpen == 1 || smartEnable == 1) {
                return true;
            }

            return false;  // 所有模式都未开启

        } catch (Exception e) {
            return false;  // 发生异常返回false
        }
    }

    public static int getNfcStatus(Context context) {
        try {
            // 1. 检查设备是否支持 NFC 硬件
            PackageManager pm = context.getPackageManager();
            boolean hasNfc = pm.hasSystemFeature(PackageManager.FEATURE_NFC);

            if (!hasNfc) {
                return 0;  // 设备不支持 NFC
            }

            // 2. 获取 NFC 适配器
            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(context);
            if (nfcAdapter == null) {
                return 0;  // 支持 NFC 硬件但无法获取适配器，按不支持处理
            }

            // 3. 检查 NFC 是否已启用
            if (nfcAdapter.isEnabled()) {
                return 2;  // NFC 已启用
            } else {
                return 1;  // NFC 支持但未启用
            }

        } catch (Exception e) {
            // 发生异常，按不支持 NFC 处理
            return 0;
        }
    }


    public static String phoneMode(Context context){
        boolean nightMode = false;
        StringBuilder stringBuilder = new StringBuilder();
        if (((context.getResources().getConfiguration().uiMode & 0x30)) == 32) {
            nightMode = true;
        }
        stringBuilder.append("是否处于深色模式:").append(nightMode?"是":"否").append("\n");
        stringBuilder.append("是否开启护眼模式:").append(protectMode(context)?"是":"否").append("\n");
        stringBuilder.append("是否开启省电模式:").append(getPowerSaveMode(context)?"是":"否").append("\n");
        boolean zenMode = Settings.Global.getInt(context.getContentResolver(), "zen_mode", 0) > 0;
        stringBuilder.append("是否开启勿扰模式:").append(zenMode?"是":"否").append("\n");
        int nfcStatus = getNfcStatus(context);
        String nfcStatusS = "";
        if (nfcStatus == 0){
            nfcStatusS = "设备不支持 NFC（或访问失败）";
        }
        if (nfcStatus == 1){
            nfcStatusS = "设备支持 NFC 但未启用";
        }
        if (nfcStatus == 2){
            nfcStatusS = "设备支持 NFC 且已启用";
        }
        stringBuilder.append("NFC状态:").append(nfcStatusS).append("\n");

        return stringBuilder.toString();
    }

    public static String getDexClassLoaderPath(Context context) {
        try {
            ClassLoader classLoader = context.getClassLoader();

            if (classLoader instanceof dalvik.system.BaseDexClassLoader) {
                // 直接调用BaseDexClassLoader的toString()，通常包含路径信息
                return classLoader.toString();
            }

            // 或者获取ClassLoader的父ClassLoader路径
            return classLoader.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    public static String getAppSignature(Context context) {
        try {
            Signature signature;

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                // Android 9.0以下使用旧API
                PackageInfo packageInfo = context.getPackageManager()
                        .getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
                signature = packageInfo.signatures[0];
            } else {
                // Android 9.0及以上使用新API
                PackageInfo packageInfo = context.getPackageManager()
                        .getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
                signature = packageInfo.signingInfo.getApkContentsSigners()[0];
            }

            // 将签名字节数组转换为字符串
            return bytesToHexString(signature.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    private static String bytesToHexString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }


    public static List<String> getServerList() {
        List<String> serviceList = new ArrayList<>();

        try {
            // 执行"service list"命令获取系统服务列表
            Process process = Runtime.getRuntime().exec("service list");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                // 跳过"Found"开头的行（这是命令的标题行）
                if (line.startsWith("Found")) {
                    continue;
                }

                // 提取服务名（去掉前面的制表符）
                int tabIndex = line.indexOf("\t");
                if (tabIndex >= 0) {
                    line = line.substring(tabIndex).trim();
                } else {
                    line = line.trim();
                }

                // 只添加非空行
                if (!line.isEmpty()) {
                    serviceList.add(line+"\n");
                }
            }

            reader.close();
            process.destroy();

        } catch (Exception e) {
            e.printStackTrace();
            // 异常时返回空列表
            return new ArrayList<>();
        }

        return serviceList;
    }
    public static String getSensorDetails(Context context) {
        if (context != null) {
            try {
                SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
                if (sensorManager != null) {
                    List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
                    if (sensorList != null && sensorList.size() > 0) {
                        StringBuilder sensorDetails = new StringBuilder();

                        for (Sensor sensor : sensorList) {
                            sensorDetails.append("Sensor: ")
                                    .append(sensor.getName())
                                    .append("\nVersion: ")
                                    .append(sensor.getVersion())
                                    .append("\nVendor: ")
                                    .append(sensor.getVendor())
                                    .append("\nType: ")
                                    .append(getSensorTypeString(sensor.getType()))
                                    .append("\nPower: ")
                                    .append(sensor.getPower())
                                    .append(" mA\nResolution: ")
                                    .append(sensor.getResolution())
                                    .append("\nMaximum Range: ")
                                    .append(sensor.getMaximumRange())
                                    .append("\nMin Delay: ")
                                    .append(sensor.getMinDelay())
                                    .append(" μs\n")
                                    .append("-".repeat(50))
                                    .append("\n");
                        }

                        return sensorDetails.toString();
                    }
                }
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }

    // 辅助方法：将传感器类型转换为可读字符串
    private static String getSensorTypeString(int type) {
        switch (type) {
            case Sensor.TYPE_ACCELEROMETER: return "Accelerometer";
            case Sensor.TYPE_GYROSCOPE: return "Gyroscope";
            case Sensor.TYPE_MAGNETIC_FIELD: return "Magnetic Field";
            case Sensor.TYPE_PROXIMITY: return "Proximity";
            case Sensor.TYPE_LIGHT: return "Light";
            case Sensor.TYPE_PRESSURE: return "Pressure";
            case Sensor.TYPE_TEMPERATURE: return "Temperature";
            case Sensor.TYPE_RELATIVE_HUMIDITY: return "Relative Humidity";
            case Sensor.TYPE_AMBIENT_TEMPERATURE: return "Ambient Temperature";
            case Sensor.TYPE_GRAVITY: return "Gravity";
            case Sensor.TYPE_LINEAR_ACCELERATION: return "Linear Acceleration";
            case Sensor.TYPE_ROTATION_VECTOR: return "Rotation Vector";
            case Sensor.TYPE_STEP_COUNTER: return "Step Counter";
            case Sensor.TYPE_STEP_DETECTOR: return "Step Detector";
            case Sensor.TYPE_HEART_RATE: return "Heart Rate";
            case Sensor.TYPE_ALL: return "All";
            default: return "Unknown (" + type + ")";
        }
    }

    public static String getAndroidSystemSignatureHash(Context context) {
        if (context == null) {
            return "";
        }

        try {
            // 获取Android系统包的签名信息
            Signature[] signatures = context.getPackageManager()
                    .getPackageInfo("android", PackageManager.GET_SIGNATURES).signatures;

            if (signatures != null && signatures.length > 0) {
                // 获取第一个签名的字符表示
                String signatureString = signatures[0].toCharsString();
                // 使用g.a方法计算哈希值（假设g.a是哈希方法）
                return calculateSHA1(signatureString);
            }
        } catch (Exception e) {
            // 异常时返回空字符串
            return "";
        }

        return "";
    }
    public static FeatureInfo[] getSystemFeatures(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            return packageManager.getSystemAvailableFeatures();

//            StringBuilder result = new StringBuilder();
//
//            for (FeatureInfo feature : features) {
//                if (feature.name != null && !feature.name.isEmpty()) {
//                    result.append(feature.name).append("\n");
//                }
//            }
//
//            return result.toString();
        } catch (Throwable e) {
            return null;
        }
    }

    public static String collectAttestationInfo(Context context) {
        // 检查API版本，低于28直接返回null
        if (Build.VERSION.SDK_INT < 28) {
            return null;
        }

        KeyStore keyStore = null;
        String alias = null;
        String result = null;

        try {
            // 生成8字节随机挑战值
            byte[] challenge = new byte[8];
            new SecureRandom().nextBytes(challenge);

            // 密钥别名
            alias = "no506b3822wb";

            // 初始化AndroidKeyStore
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            // 如果已存在同名密钥，先删除
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias);
            }

            // 创建密钥生成参数
            KeyGenParameterSpec.Builder keySpecBuilder = new KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setAttestationChallenge(challenge)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1);

            // 生成密钥对
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
            keyPairGenerator.initialize(keySpecBuilder.build());
            keyPairGenerator.generateKeyPair();

            // 获取证书链
            Certificate[] certificateChain = keyStore.getCertificateChain(alias);

            // 检查证书链长度
            if (certificateChain.length < 2) {
                return "0f|";
            }

            // 提取认证扩展值
            try {
                X509Certificate firstCert = (X509Certificate) certificateChain[0];
                byte[] extensionValue = firstCert.getExtensionValue("1.3.6.1.4.1.11129.2.1.17");
                if (extensionValue != null) {
                    String extensionBase64 = Base64.encodeToString(extensionValue, Base64.NO_WRAP);
                    result = "1|" + extensionBase64;
                } else {
                    result = "1|";
                }
            } catch (Exception e) {
                result = "1|";
            }

        } catch (Throwable e) {
            result = "0e|";
        } finally {
            // 清理：删除临时密钥
            if (alias != null && keyStore != null) {
                try {
                    keyStore.deleteEntry(alias);
                } catch (Exception e) {
                    // 忽略清理异常
                }
            }
        }

        // 检查结果长度，超过400字符则截断
        if (result != null && result.length() > 400) {
            return "0a|" + result.length();
        }

        return result;
    }
    public static String collectAttestationInfoJson(Context context) {
        JSONObject resultJson = new JSONObject();

        if (context == null) {
            try {
                resultJson.put("e", -1);
                return resultJson.toString();
            } catch (Exception e) {
                return "{\"e\":-1}";
            }
        }

        // 检查API版本
        if (Build.VERSION.SDK_INT <= 26) {
            try {
                resultJson.put("e", -2);
                return resultJson.toString();
            } catch (Exception e) {
                return "{\"e\":-2}";
            }
        }

        if (Build.VERSION.SDK_INT < 28) {
            try {
                resultJson.put("e", -2);
                return resultJson.toString();
            } catch (Exception e) {
                return "{\"e\":-2}";
            }
        }

        KeyStore keyStore = null;
        String alias = "no506b3822wbTB";

        try {
            // 初始化AndroidKeyStore
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            // 如果已存在同名密钥，先删除
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias);
            }

            // 生成挑战值
            byte[] challenge = new byte[8];
            new SecureRandom().nextBytes(challenge);

            // 创建密钥生成参数
            KeyGenParameterSpec.Builder keySpecBuilder = new KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setAttestationChallenge(challenge);

            // 在Android 12 (API 31)及以上版本，设置设备属性认证包含
            if (Build.VERSION.SDK_INT >= 31) {
                try {
                    Method setDevicePropertiesMethod = KeyGenParameterSpec.Builder.class
                            .getDeclaredMethod("setDevicePropertiesAttestationIncluded", boolean.class);
                    if (setDevicePropertiesMethod != null) {
                        setDevicePropertiesMethod.setAccessible(true);
                        setDevicePropertiesMethod.invoke(keySpecBuilder, true);
                        resultJson.put("device_properties_attestation", true);
                    }
                } catch (Exception e) {
                    try {
                        resultJson.put("31_e", -10);
                        resultJson.put("31_exp", e.getMessage());
                    } catch (Exception ex) {
                        // 忽略
                    }
                }
            }

            // 生成密钥对
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
            keyPairGenerator.initialize(keySpecBuilder.build());
            keyPairGenerator.generateKeyPair();

            // 获取证书链
            Certificate[] certificateChain = keyStore.getCertificateChain(alias);

            if (certificateChain == null) {
                resultJson.put("e", -7);
                return resultJson.toString();
            }

            // 记录证书链长度
            resultJson.put("s", certificateChain.length);

            // 记录证书链验证状态
            if (certificateChain.length > 1) {
                for (int i = 1; i < certificateChain.length; i++) {
                    try {
                        ((X509Certificate) certificateChain[i - 1]).checkValidity();
                        certificateChain[i - 1].verify(certificateChain[i].getPublicKey());
                        resultJson.put("v", true);
                    } catch (Throwable t) {
                        resultJson.put("v", false);
                        resultJson.put("v_exp", t.getMessage());
                        break;
                    }
                }
            }

            // 记录第一个证书的完整编码
            try {
                String cert0Base64 = Base64.encodeToString(certificateChain[0].getEncoded(), Base64.NO_WRAP);
                resultJson.put("c0t", cert0Base64);
            } catch (Exception e) {
                resultJson.put("c0t_e", -8);
                resultJson.put("c0t_exp", e.getMessage());
            }

            // 记录第二个证书的公钥
            try {
                String publicKeyBase64 = Base64.encodeToString(certificateChain[1].getPublicKey().getEncoded(), Base64.NO_WRAP);
                resultJson.put("c1pk", publicKeyBase64);
            } catch (Exception e) {
                resultJson.put("c1pk_e", -9);
                resultJson.put("c1pk_exp", e.getMessage());
            }

            // 标记成功
            resultJson.put("ret", true);
            resultJson.put("t", "0");

        } catch (Exception e) {
            try {
                resultJson.put("e", -3);
                resultJson.put("exp", e.getMessage());
            } catch (Exception ex) {
                // 忽略
            }
        } finally {
            // 清理：删除临时密钥
            if (keyStore != null) {
                try {
                    keyStore.deleteEntry(alias);
                } catch (Exception e) {
                    // 忽略清理异常
                }
            }
        }

        // 如果主流程失败，尝试使用备用别名
        if (!resultJson.optBoolean("ret", false)) {
            return tryBackupAttestation(context);
        }

        return resultJson.toString();
    }

    /**
     * 使用备用别名尝试获取认证信息
     */
    public static String tryBackupAttestation(Context context) {
        JSONObject resultJson = new JSONObject();
        KeyStore keyStore = null;
        String alias = "no506b3822wb";

        try {
            // 初始化AndroidKeyStore
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            // 如果已存在同名密钥，先删除
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias);
            }

            // 生成挑战值
            byte[] challenge = new byte[8];
            new SecureRandom().nextBytes(challenge);

            // 创建密钥生成参数
            KeyGenParameterSpec.Builder keySpecBuilder = new KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setAttestationChallenge(challenge);

            // 生成密钥对
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
            keyPairGenerator.initialize(keySpecBuilder.build());
            keyPairGenerator.generateKeyPair();

            // 获取证书链
            Certificate[] certificateChain = keyStore.getCertificateChain(alias);

            if (certificateChain == null) {
                resultJson.put("e", -7);
                return resultJson.toString();
            }

            // 记录证书链长度
            resultJson.put("s", certificateChain.length);

            // 记录证书链验证状态
            if (certificateChain.length > 1) {
                for (int i = 1; i < certificateChain.length; i++) {
                    try {
                        ((X509Certificate) certificateChain[i - 1]).checkValidity();
                        certificateChain[i - 1].verify(certificateChain[i].getPublicKey());
                        resultJson.put("v", true);
                    } catch (Throwable t) {
                        resultJson.put("v", false);
                        resultJson.put("v_exp", t.getMessage());
                        break;
                    }
                }
            }

            // 记录第一个证书的完整编码
            try {
                String cert0Base64 = Base64.encodeToString(certificateChain[0].getEncoded(), Base64.NO_WRAP);
                resultJson.put("c0t", cert0Base64);
            } catch (Exception e) {
                resultJson.put("c0t_e", -8);
                resultJson.put("c0t_exp", e.getMessage());
            }

            // 记录第二个证书的公钥
            try {
                String publicKeyBase64 = Base64.encodeToString(certificateChain[1].getPublicKey().getEncoded(), Base64.NO_WRAP);
                resultJson.put("c1pk", publicKeyBase64);
            } catch (Exception e) {
                resultJson.put("c1pk_e", -9);
                resultJson.put("c1pk_exp", e.getMessage());
            }

            // 标记成功
            resultJson.put("ret", true);
            resultJson.put("t", "1");

        } catch (Exception e) {
            try {
                resultJson.put("e", -3);
                resultJson.put("exp", e.getMessage());
            } catch (Exception ex) {
                // 忽略
            }
        } finally {
            // 清理：删除临时密钥
            if (keyStore != null) {
                try {
                    keyStore.deleteEntry(alias);
                } catch (Exception e) {
                    // 忽略清理异常
                }
            }
        }

        return resultJson.toString();
    }
    public static String getOpenGLInfo(Context context) {
        JSONObject resultJson = new JSONObject();

        // 检查API版本，低于24返回null
        if (Build.VERSION.SDK_INT < 24) {
            return null;
        }

        try {
            // 检查Vulkan支持
            boolean hasVulkanSupport = context.getPackageManager().hasSystemFeature("android.hardware.vulkan.level");
            resultJson.put("is_vulkan_support", hasVulkanSupport ? 1 : 0);

            // 检查API版本，低于17返回null（EGL14需要API 17）
            if (Build.VERSION.SDK_INT < 17) {
                return null;
            }

            // 获取EGL显示
            EGLDisplay eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
                return null;
            }

            // 初始化EGL
            int[] version = new int[2];
            if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
                return null;
            }

            // 配置属性
            int[] configAttribs = {
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_NONE
            };

            // 选择配置
            EGLConfig[] configs = new EGLConfig[1];
            int[] numConfigs = new int[1];
            if (!EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)) {
                EGL14.eglTerminate(eglDisplay);
                return null;
            }

            // 创建OpenGL ES上下文
            int[] contextAttribs = {
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
            };

            EGLContext eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0);
            if (eglContext == EGL14.EGL_NO_CONTEXT) {
                EGL14.eglTerminate(eglDisplay);
                return null;
            }

            // 创建EGL表面
            int[] surfaceAttribs = {
                    EGL14.EGL_WIDTH, 1,
                    EGL14.EGL_HEIGHT, 1,
                    EGL14.EGL_NONE
            };

            EGLSurface eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, configs[0], surfaceAttribs, 0);
            if (eglSurface == EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroyContext(eglDisplay, eglContext);
                EGL14.eglTerminate(eglDisplay);
                return null;
            }

            // 设置当前上下文
            if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                EGL14.eglDestroySurface(eglDisplay, eglSurface);
                EGL14.eglDestroyContext(eglDisplay, eglContext);
                EGL14.eglTerminate(eglDisplay);
                return null;
            }

            // 获取OpenGL信息
            String renderer = GLES20.glGetString(GLES20.GL_RENDERER);
            String vendor = GLES20.glGetString(GLES20.GL_VENDOR);
            String versionStr = GLES20.glGetString(GLES20.GL_VERSION);

            // 将信息放入JSON
            resultJson.put("opengl_version", versionStr != null ? versionStr : "");
            resultJson.put("gpu_renderer", renderer != null ? renderer : "");
            resultJson.put("gpu_vendor", vendor != null ? vendor : "");

            // 清理资源
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(eglDisplay, eglSurface);
            EGL14.eglDestroyContext(eglDisplay, eglContext);
            EGL14.eglTerminate(eglDisplay);

            return resultJson.toString();

        } catch (Throwable e) {
            return null;
        }
    }
    /**
     * 检查当前应用是否为系统预装应用
     * @return true表示是系统预装应用，false表示不是
     */
    public static boolean isSystemPreInstalledApp(Context context) {
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
    public static native boolean checkVpnStatus(Activity activity);

    /** 调用 Native RootCheck 检测，需传入 Context（用于 checkRootManagementApps 等） */
    public static native boolean checkRootStatus(android.content.Context context);

    /** 调用 Native HookCheck 检测（Frida/Xposed 等 Hook 环境） */
    public static native boolean checkHookStatus();

}
