package com.kaze.devicefp.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 可折叠板块数据模型
 * 包含板块标题、摘要、详细项列表及展开状态
 */
public class FingerprintSection {
    private final String title;
    private final String summary;
    private final List<DeviceFingerprint> items;
    private boolean expanded;

    public FingerprintSection(String title, String summary, List<DeviceFingerprint> items) {
        this.title = title;
        this.summary = summary != null ? summary : "";
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<DeviceFingerprint>();
        this.expanded = false;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public List<DeviceFingerprint> getItems() {
        return items;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public void toggleExpanded() {
        this.expanded = !this.expanded;
    }
}
