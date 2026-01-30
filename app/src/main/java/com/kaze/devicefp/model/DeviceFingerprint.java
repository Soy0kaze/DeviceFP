package com.kaze.devicefp.model;

/**
 * 设备指纹数据模型
 */
public class DeviceFingerprint {
    private String category;  // 分类（如：设备信息、系统信息等）
    private String name;      // 属性名称
    private String value;     // 属性值
    private String status;    // 状态（如：已获取、未获取、需要权限等）

    public DeviceFingerprint(String category, String name, String value, String status) {
        this.category = category;
        this.name = name;
        this.value = value;
        this.status = status;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
