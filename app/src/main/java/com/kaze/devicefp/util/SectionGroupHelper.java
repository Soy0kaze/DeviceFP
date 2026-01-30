package com.kaze.devicefp.util;

import com.kaze.devicefp.model.DeviceFingerprint;
import com.kaze.devicefp.model.FingerprintSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 将设备指纹列表按功能分组为可折叠板块
 */
public final class SectionGroupHelper {

    // 定义各种键的集合
    private static final Set<String> DEVICE_KEYS = Set.of(
            "设备品牌", "设备型号", "型号", "制造商", "产品名称", "设备名称",
            "硬件", "Android版本", "API级别", "构建版本", "安全补丁级别",
            "内核版本", "系统语言", "时区", "基带"
    );

    private static final Set<String> CPU_KEYS = Set.of(
            "CPU架构", "CPU核心数", "CPU频率", "CPU型号", "CPU厂商",
            "CPU实现者", "CPU特性"
    );

    private static final Set<String> MEMORY_KEYS = Set.of(
            "总内存", "可用内存", "空闲内存", "内存信息"
    );

    private static final Set<String> STORAGE_KEYS = Set.of(
            "存储信息", "总存储", "可用存储", "内部存储", "外部存储"
    );

    private static final Set<String> BATTERY_KEYS = Set.of(
            "电量", "健康状态", "电压", "温度", "容量", "充电方式",
            "电池状态", "电池技术"
    );

    private static final Set<String> SCREEN_KEYS = Set.of(
            "分辨率", "屏幕计算尺寸", "屏幕尺寸", "屏幕宽度", "屏幕高度", "宽高比", "屏幕类别", "屏幕密度",
            "密度类别", "PPI", "XDPI / YDPI", "状态栏高度", "导航栏高度", "刷新率", "支持刷新率", "当前亮度",
            "屏幕制造商", "屏幕型号", "屏幕超时", "密度", "亮度", "屏幕类型", "像素密度", "亮度模式", "旋转模式", "亮度大小"
    );
    private static final Set<String> CAMERA_KEYS = Set.of(
            "总摄像头数量", "前置数量", "后置数量", "摄像头详细信息",
            "主摄像头"
    );
    public static final Set<String> NET_KEYS = Set.of(
            "WiFi MAC地址", "蓝牙MAC地址", "IP地址", "网络类型",
            "WiFi是否开启", "连接类型", "网关", "移动数据状态", "运营商名字",
            "是否插卡", "卡槽数量", "卡最大订阅数量", "sim国家", "mnc|mcc", "NetName", "HttpAgent"
    );
    public static final Set<String> APPSELF_KEYS = Set.of(
            "应用包名", "应用签名", "应用版本", "安装时间", "是否为系统预装应用",
            "Android ID", "APP路径"
    );

    /**
     * 将扁平指纹列表分组为不同的板块
     */
    public static List<FingerprintSection> groupFingerprints(List<DeviceFingerprint> fingerprints) {
        if (fingerprints == null || fingerprints.isEmpty()) {
            return new ArrayList<>();
        }

        // 创建各个板块的列表
        List<DeviceFingerprint> deviceItems = new ArrayList<>();
        List<DeviceFingerprint> netItems = new ArrayList<>();
        List<DeviceFingerprint> cpuItems = new ArrayList<>();
        List<DeviceFingerprint> memoryItems = new ArrayList<>();
        List<DeviceFingerprint> storageItems = new ArrayList<>();
        List<DeviceFingerprint> batteryItems = new ArrayList<>();
        List<DeviceFingerprint> screenItems = new ArrayList<>();
        List<DeviceFingerprint> cameraItems = new ArrayList<>();
        List<DeviceFingerprint> selfAppItems = new ArrayList<>();
        List<DeviceFingerprint> otherItems = new ArrayList<>();

        // 不再依赖category，直接根据name分类
        for (DeviceFingerprint fp : fingerprints) {
            String name = fp.getName();
            if (DEVICE_KEYS.contains(name)) {
                deviceItems.add(fp);
            } else if (NET_KEYS.contains(name)){
                netItems.add(fp);
            } else if (CPU_KEYS.contains(name)) {
                cpuItems.add(fp);
            } else if (MEMORY_KEYS.contains(name)) {
                memoryItems.add(fp);
            } else if (STORAGE_KEYS.contains(name)) {
                storageItems.add(fp);
            } else if (BATTERY_KEYS.contains(name)) {
                batteryItems.add(fp);
            } else if (SCREEN_KEYS.contains(name)) {
                screenItems.add(fp);
            } else if (CAMERA_KEYS.contains(name)) {
                cameraItems.add(fp);
            } else if (APPSELF_KEYS.contains(name)) {
                selfAppItems.add(fp);
            }
            else {
                otherItems.add(fp);
            }
        }

        // 创建板块对象
        List<FingerprintSection> sections = new ArrayList<>();

        if (!deviceItems.isEmpty()) {
            sections.add(new FingerprintSection("设备信息", deviceItems.size() + " 项", deviceItems));
        }
        if (!cpuItems.isEmpty()) {
            sections.add(new FingerprintSection("CPU信息", cpuItems.size() + " 项", cpuItems));
        }
        if (!memoryItems.isEmpty()) {
            sections.add(new FingerprintSection("内存信息", memoryItems.size() + " 项", memoryItems));
        }
        if (!storageItems.isEmpty()) {
            sections.add(new FingerprintSection("存储信息", storageItems.size() + " 项", storageItems));
        }
        if (!batteryItems.isEmpty()) {
            sections.add(new FingerprintSection("电池信息", batteryItems.size() + " 项", batteryItems));
        }
        if (!screenItems.isEmpty()) {
            sections.add(new FingerprintSection("屏幕信息", screenItems.size() + " 项", screenItems));
        }
        if (!cameraItems.isEmpty()){
            sections.add(new FingerprintSection("摄像头信息", cameraItems.size() + " 项", cameraItems));
        }
        if (!netItems.isEmpty()) {
            sections.add(new FingerprintSection("网络信息", netItems.size() + " 项", netItems));
        }
        if (!selfAppItems.isEmpty()){
            sections.add(new FingerprintSection("APP信息", selfAppItems.size() + " 项", selfAppItems));
        }
        if (!otherItems.isEmpty()) {
            sections.add(new FingerprintSection("系统信息", otherItems.size() + " 项", otherItems));
        }

        return sections;
    }
}