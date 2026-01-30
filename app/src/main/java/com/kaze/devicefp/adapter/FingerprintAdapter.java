package com.kaze.devicefp.adapter;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.kaze.devicefp.R;
import com.kaze.devicefp.model.BatteryInfo;
import com.kaze.devicefp.model.BatteryInfoManager;
import com.kaze.devicefp.model.CpuInfoReader;
import com.kaze.devicefp.model.DeviceFingerprint;
import com.kaze.devicefp.view.CpuFrequencyTableView;
import java.util.ArrayList;
import java.util.List;

/**
 * 设备指纹信息列表适配器
 */
public class FingerprintAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final int VIEW_TYPE_NORMAL = 0;
    private static final int VIEW_TYPE_CPU_CHART = 1;
    
    private List<DeviceFingerprint> fingerprints;
    private int lastPosition = -1;
    private Handler updateHandler;
    private Runnable updateRunnable;
    private boolean isUpdating = false;
    private TableViewHolder tableViewHolder; // 保存TableViewHolder引用用于更新
    private Context context;
    /** 在可折叠板块内使用时隐藏分类标题 */
    private boolean hideCategoryHeader;

    public FingerprintAdapter(Context context) {
        this(context, false);
    }

    public FingerprintAdapter(Context context, boolean hideCategoryHeader) {
        this.fingerprints = new ArrayList<>();
        this.updateHandler = new Handler(Looper.getMainLooper());
        this.context = context != null ? context.getApplicationContext() : null;
        this.hideCategoryHeader = hideCategoryHeader;
    }

    public void setHideCategoryHeader(boolean hide) {
        this.hideCategoryHeader = hide;
    }
    
    public void setFingerprints(List<DeviceFingerprint> fingerprints) {
        this.fingerprints = fingerprints;
        notifyDataSetChanged();
        startFrequencyUpdate();
    }
    
    @Override
    public int getItemViewType(int position) {
        DeviceFingerprint fingerprint = fingerprints.get(position);
        if ("CPU频率".equals(fingerprint.getName())) {
            return VIEW_TYPE_CPU_CHART;
        }
        return VIEW_TYPE_NORMAL;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_CPU_CHART) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_fingerprint_table, parent, false);
            return new TableViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_fingerprint, parent, false);
            return new ViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DeviceFingerprint fingerprint = fingerprints.get(position);
        
        if (holder instanceof TableViewHolder) {
            // CPU频率表格视图
            bindTableViewHolder((TableViewHolder) holder, fingerprint, position);
        } else if (holder instanceof ViewHolder) {
            // 普通视图
            bindNormalViewHolder((ViewHolder) holder, fingerprint, position);
        }
    }
    
    private void bindTableViewHolder(TableViewHolder holder, DeviceFingerprint fingerprint, int position) {
        holder.nameText.setText(fingerprint.getName());
        holder.statusText.setText(fingerprint.getStatus());
        setStatusStyle(holder.statusText, fingerprint.getStatus());
        
        // 保存引用用于后续更新
        tableViewHolder = holder;
        
        // 初始化并更新表格数据
        updateTableData(holder.tableView);
        
        // 添加进入动画
        setAnimation(holder.itemView, position);
    }
    
    /**
     * 更新表格数据
     */
    private void updateTableData(CpuFrequencyTableView tableView) {
        CpuInfoReader reader = new CpuInfoReader();
        List<CpuInfoReader.CpuFrequencyData> frequencyData = reader.getAllCpuFrequencyData();
        tableView.setFrequencyData(frequencyData);
    }
    
    private void bindNormalViewHolder(ViewHolder holder, DeviceFingerprint fingerprint, int position) {
        // 判断是否需要显示分类标题（板块内展示时隐藏；长文本布局无 category_header）
        if (holder.categoryHeader != null) {
            if (hideCategoryHeader) {
                holder.categoryHeader.setVisibility(View.GONE);
            } else if (position == 0 || !fingerprint.getCategory().equals(fingerprints.get(position - 1).getCategory())) {
                holder.categoryHeader.setVisibility(View.VISIBLE);
                holder.categoryHeader.setText(fingerprint.getCategory());
            } else {
                holder.categoryHeader.setVisibility(View.GONE);
            }
        }

        holder.nameText.setText(fingerprint.getName());
        holder.valueText.setText(fingerprint.getValue());
        holder.statusText.setText(fingerprint.getStatus());

        // 设置文本显示：内存信息、IP地址、硬件功能、摄像头详细信息等长文本允许全部展示不截断
        holder.valueText.setSingleLine(false);
        String name = fingerprint.getName();
        if ("OpenGL".equals(name) || "KeyStoreAttestation".equals(name) || "Attestation".equals(name) || "硬件列表".equals(name) || "传感器".equals(name) || "应用签名".equals(name) || "服务列表".equals(name) || "物理输入设备".equals(name) || "DexClassLoader的路径列表".equals(name) || "设备模式信息".equals(name) || "输入法列表".equals(name) || "已安装辅助服务列表".equals(name) || "系统文件哈希".equals(name) || "铃声大小".equals(name) || "内存信息".equals(name) || "IP地址".equals(name) || "硬件功能".equals(name) || "摄像头详细信息".equals(name)) {
            holder.valueText.setMaxLines(Integer.MAX_VALUE);
            holder.valueText.setEllipsize(null);
        } else {
            holder.valueText.setMaxLines(3);
            holder.valueText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        }

        setStatusStyle(holder.statusText, fingerprint.getStatus());
        setAnimation(holder.itemView, position);
    }
    
    private void setStatusStyle(TextView statusText, String status) {
        int textColor = Color.WHITE;
        int backgroundRes;
        
        if (status.contains("已获取")) {
            backgroundRes = R.drawable.status_success;
        } else if (status.contains("需要权限")) {
            backgroundRes = R.drawable.status_warning;
        } else if (status.contains("未获取")) {
            backgroundRes = R.drawable.status_background;
            textColor = ContextCompat.getColor(statusText.getContext(), R.color.text_secondary);
        } else {
            backgroundRes = R.drawable.status_info;
        }
        
        statusText.setBackgroundResource(backgroundRes);
        statusText.setTextColor(textColor);
    }
    
    /**
     * 开始定时更新CPU频率（供 SectionAdapter 等外部调用）
     */
    public void startFrequencyUpdate() {
        // 先停止之前的更新
        stopFrequencyUpdate();
        
        isUpdating = true;
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                // 1. 更新 CPU 频率
                if (tableViewHolder != null && tableViewHolder.tableView != null) {
                    updateTableData(tableViewHolder.tableView);
                } else {
                    for (int i = 0; i < fingerprints.size(); i++) {
                        if ("CPU频率".equals(fingerprints.get(i).getName())) {
                            notifyItemChanged(i);
                            break;
                        }
                    }
                }
                
                // 2. 每 500ms 更新电压和温度
                if (context != null && fingerprints != null && !fingerprints.isEmpty()) {
                    try {
                        BatteryInfo batteryInfo = BatteryInfoManager.getBatteryInfo(context);
                        if (batteryInfo != null) {
                            String voltageStr = String.format("%.2fV", batteryInfo.getVoltageVolts());
                            String temperatureStr = String.format("%.1f°C", batteryInfo.getTemperatureCelsius());
                            for (int i = 0; i < fingerprints.size(); i++) {
                                DeviceFingerprint fp = fingerprints.get(i);
                                if ("电压".equals(fp.getName())) {
                                    fp.setValue(voltageStr);
                                    notifyItemChanged(i);
                                } else if ("温度".equals(fp.getName())) {
                                    fp.setValue(temperatureStr);
                                    notifyItemChanged(i);
                                }
                            }
                        }
                    } catch (Exception ignored) { }
                }
                
                if (isUpdating) {
                    updateHandler.postDelayed(this, 500);
                }
            }
        };
        updateHandler.postDelayed(updateRunnable, 500);
    }
    
    /**
     * 停止更新
     */
    public void stopFrequencyUpdate() {
        isUpdating = false;
        if (updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
    }
    
    private void setAnimation(View viewToAnimate, int position) {
        // 如果位置大于最后显示的位置，则添加动画
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(viewToAnimate.getContext(), 
                    android.R.anim.fade_in);
            animation.setDuration(300);
            animation.setStartOffset(position * 30); // 错开动画时间
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }
    
    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.itemView.clearAnimation();
    }
    
    @Override
    public int getItemCount() {
        return fingerprints.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView categoryHeader;
        TextView nameText;
        TextView valueText;
        TextView statusText;
        View iconIndicator;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryHeader = itemView.findViewById(R.id.category_header);
            nameText = itemView.findViewById(R.id.name_text);
            valueText = itemView.findViewById(R.id.value_text);
            statusText = itemView.findViewById(R.id.status_text);
            iconIndicator = itemView.findViewById(R.id.icon_indicator);
        }
    }
    
    static class TableViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView statusText;
        CpuFrequencyTableView tableView;
        View iconIndicator;
        
        TableViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.name_text);
            statusText = itemView.findViewById(R.id.status_text);
            tableView = itemView.findViewById(R.id.table_view);
            iconIndicator = itemView.findViewById(R.id.icon_indicator);
        }
    }
}
