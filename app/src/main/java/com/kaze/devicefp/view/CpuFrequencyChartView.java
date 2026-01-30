package com.kaze.devicefp.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.kaze.devicefp.R;
import com.kaze.devicefp.model.CpuInfoReader;
import java.util.List;

/**
 * CPU频率柱状图视图
 * 显示所有CPU核心的实时频率柱状图
 */
public class CpuFrequencyChartView extends View {
    
    private static final String TAG = "CpuFrequencyChartView";
    
    private List<CpuInfoReader.CpuFrequencyData> frequencyDataList;
    private Paint barPaint;
    private Paint textPaint;
    private Paint axisPaint;
    private Paint labelPaint;
    
    private float paddingLeft = 60f;
    private float paddingRight = 20f;
    private float paddingTop = 40f;
    private float paddingBottom = 40f;
    private float barWidth;
    private float barSpacing;
    
    public CpuFrequencyChartView(Context context) {
        super(context);
        init();
    }
    
    public CpuFrequencyChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public CpuFrequencyChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        // 柱状图画笔
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(ContextCompat.getColor(getContext(), R.color.primary_blue));
        barPaint.setStyle(Paint.Style.FILL);
        
        // 文本画笔
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        textPaint.setTextSize(24f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        // 坐标轴画笔
        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(ContextCompat.getColor(getContext(), R.color.gray_400));
        axisPaint.setStrokeWidth(2f);
        
        // 标签画笔
        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        labelPaint.setTextSize(20f);
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }
    
    /**
     * 设置频率数据并刷新视图
     */
    public void setFrequencyData(List<CpuInfoReader.CpuFrequencyData> dataList) {
        this.frequencyDataList = dataList;
        invalidate(); // 触发重绘
    }
    
    /**
     * 更新频率数据（用于动态刷新）
     */
    public void updateFrequencyData() {
        CpuInfoReader reader = new CpuInfoReader();
        List<CpuInfoReader.CpuFrequencyData> newData = reader.getAllCpuFrequencyData();
        setFrequencyData(newData);
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        // 固定高度，使用dp转px
        float density = getContext().getResources().getDisplayMetrics().density;
        int height = (int)(350 * density); // 350dp
        
        setMeasuredDimension(width, height);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (frequencyDataList == null || frequencyDataList.isEmpty()) {
            drawEmptyState(canvas);
            return;
        }
        
        float chartWidth = getWidth() - paddingLeft - paddingRight;
        float chartHeight = getHeight() - paddingTop - paddingBottom;
        
        int coreCount = frequencyDataList.size();
        if (coreCount == 0) {
            return;
        }
        
        // 计算柱状图宽度和间距
        barSpacing = 20f;
        barWidth = (chartWidth - (coreCount - 1) * barSpacing) / coreCount;
        barWidth = Math.min(barWidth, 60f); // 最大宽度限制
        
        // 找到全局最大和最小频率
        long globalMaxFreq = 0;
        long globalMinFreq = Long.MAX_VALUE;
        
        for (CpuInfoReader.CpuFrequencyData data : frequencyDataList) {
            if (data.isValid()) {
                globalMaxFreq = Math.max(globalMaxFreq, data.maxFreq);
                globalMinFreq = Math.min(globalMinFreq, data.minFreq);
            }
        }
        
        if (globalMaxFreq <= 0 || globalMinFreq == Long.MAX_VALUE) {
            drawEmptyState(canvas);
            return;
        }
        
        // 绘制Y轴标签（最大和最小频率）
        labelPaint.setTextAlign(Paint.Align.RIGHT);
        labelPaint.setTextSize(18f);
        canvas.drawText(formatFrequency(globalMaxFreq), paddingLeft - 10, paddingTop + 20, labelPaint);
        canvas.drawText(formatFrequency(globalMinFreq), paddingLeft - 10, getHeight() - paddingBottom + 20, labelPaint);
        
        // 绘制坐标轴
        float axisY = getHeight() - paddingBottom;
        canvas.drawLine(paddingLeft, paddingTop, paddingLeft, axisY, axisPaint);
        canvas.drawLine(paddingLeft, axisY, getWidth() - paddingRight, axisY, axisPaint);
        
        // 绘制每个CPU核心的柱状图
        float startX = paddingLeft + barSpacing / 2;
        
        for (int i = 0; i < coreCount; i++) {
            CpuInfoReader.CpuFrequencyData data = frequencyDataList.get(i);
            
            if (!data.isValid()) {
                // 跳过无效数据，但保留位置
                startX += barWidth + barSpacing;
                continue;
            }
            
            float x = startX;
            
            // 计算柱状图高度（基于当前频率在最大和最小频率之间的比例）
            float normalizedFreq = (float)(data.curFreq - globalMinFreq) / (globalMaxFreq - globalMinFreq);
            float barHeight = normalizedFreq * chartHeight;
            barHeight = Math.max(barHeight, 5f); // 最小高度5px，确保可见
            
            // 绘制柱状图
            float barTop = axisY - barHeight;
            RectF barRect = new RectF(x, barTop, x + barWidth, axisY);
            canvas.drawRect(barRect, barPaint);
            
            // 绘制当前频率文本（在柱状图上方）
            if (barHeight > 30) {
                textPaint.setTextSize(16f);
                textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary));
                String freqText = formatFrequency(data.curFreq);
                canvas.drawText(freqText, x + barWidth / 2, barTop - 5, textPaint);
            }
            
            // 绘制CPU标签（在X轴下方）
            labelPaint.setTextAlign(Paint.Align.CENTER);
            labelPaint.setTextSize(18f);
            labelPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
            canvas.drawText("CPU" + i, x + barWidth / 2, axisY + 30, labelPaint);
            
            startX += barWidth + barSpacing;
        }
    }
    
    /**
     * 绘制空状态
     */
    private void drawEmptyState(Canvas canvas) {
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        canvas.drawText("暂无CPU频率数据", getWidth() / 2, getHeight() / 2, textPaint);
    }
    
    /**
     * 格式化频率
     */
    private String formatFrequency(long freqKhz) {
        if (freqKhz >= 1000000) {
            double freqGhz = freqKhz / 1000000.0;
            return String.format("%.2fG", freqGhz);
        } else if (freqKhz >= 1000) {
            double freqMhz = freqKhz / 1000.0;
            return String.format("%.0fM", freqMhz);
        } else {
            return freqKhz + "K";
        }
    }
}
