package com.kaze.devicefp.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.kaze.devicefp.R;
import com.kaze.devicefp.model.CpuInfoReader;
import java.util.List;

/**
 * CPU频率表格视图
 * 显示所有CPU核心的频率信息表格
 */
public class CpuFrequencyTableView extends View {
    
    private static final String TAG = "CpuFrequencyTableView";
    
    private List<CpuInfoReader.CpuFrequencyData> frequencyDataList;
    private Paint headerPaint;
    private Paint cellPaint;
    private Paint textPaint;
    private Paint borderPaint;
    
    private float cellPadding = 6f; // 减少单元格内边距
    private float headerHeight = 40f; // 减小表头高度
    private float rowHeight = 36f; // 减小行高
    private float baseColumnWidth = 85f; // 基础列宽
    private float borderWidth = 1f;
    private float[] columnWidths; // 动态列宽数组
    
    private String[] rowHeaders = {"最大频率", "最小频率", "实时频率"};
    
    public CpuFrequencyTableView(Context context) {
        super(context);
        init();
    }
    
    public CpuFrequencyTableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public CpuFrequencyTableView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        // 表头画笔
        headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(ContextCompat.getColor(getContext(), R.color.primary_blue_light));
        headerPaint.setStyle(Paint.Style.FILL);
        
        // 单元格背景画笔
        cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cellPaint.setColor(ContextCompat.getColor(getContext(), R.color.card_background));
        cellPaint.setStyle(Paint.Style.FILL);
        
        // 文本画笔
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        // 边框画笔
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(ContextCompat.getColor(getContext(), R.color.gray_300));
        borderPaint.setStrokeWidth(borderWidth);
        borderPaint.setStyle(Paint.Style.STROKE);
    }
    
    /**
     * 设置频率数据并刷新视图
     */
    public void setFrequencyData(List<CpuInfoReader.CpuFrequencyData> dataList) {
        this.frequencyDataList = dataList;
        // 重置列宽数组，让onMeasure重新计算
        columnWidths = null;
        requestLayout(); // 请求重新测量和布局
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
        // 计算高度：表头 + 数据行 + 最小padding
        int height = (int)(headerHeight + rowHeaders.length * rowHeight + 2);
        
        int width;
        if (frequencyDataList != null && !frequencyDataList.isEmpty()) {
            int coreCount = frequencyDataList.size();
            int mode = MeasureSpec.getMode(widthMeasureSpec);
            int availableWidth = MeasureSpec.getSize(widthMeasureSpec);
            
            // 计算基础宽度（行标题列 + CPU列）
            int baseWidth = (int)(baseColumnWidth * (coreCount + 1));
            
            // 如果可用宽度大于基础宽度，让表格占满可用宽度
            if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
                if (availableWidth > baseWidth) {
                    // 表格可以占满可用宽度，动态分配列宽
                    width = availableWidth;
                    calculateColumnWidths(coreCount, availableWidth);
                } else {
                    // 内容宽度超过可用宽度，使用基础宽度（支持横向滚动）
                    width = baseWidth;
                    calculateColumnWidths(coreCount, baseWidth);
                }
            } else {
                // UNSPECIFIED模式，使用基础宽度
                width = baseWidth;
                calculateColumnWidths(coreCount, baseWidth);
            }
        } else {
            width = MeasureSpec.getSize(widthMeasureSpec);
        }
        
        setMeasuredDimension(width, height);
    }
    
    /**
     * 计算每列的宽度，让表格占满可用宽度
     */
    private void calculateColumnWidths(int coreCount, int totalWidth) {
        if (coreCount <= 0) {
            return;
        }
        
        columnWidths = new float[coreCount + 1]; // +1 是行标题列
        
        // 行标题列使用固定宽度
        columnWidths[0] = baseColumnWidth;
        
        // 剩余宽度分配给CPU列
        float remainingWidth = totalWidth - baseColumnWidth;
        float cpuColumnWidth = remainingWidth / coreCount;
        
        // 确保每列最小宽度
        if (cpuColumnWidth < baseColumnWidth * 0.7f) {
            // 如果计算出的列宽太小，使用基础列宽，让表格可以横向滚动
            for (int i = 1; i <= coreCount; i++) {
                columnWidths[i] = baseColumnWidth;
            }
        } else {
            // 使用计算出的列宽，让表格占满宽度
            for (int i = 1; i <= coreCount; i++) {
                columnWidths[i] = cpuColumnWidth;
            }
        }
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (frequencyDataList == null || frequencyDataList.isEmpty()) {
            drawEmptyState(canvas);
            return;
        }
        
        int coreCount = frequencyDataList.size();
        if (coreCount == 0) {
            return;
        }
        
        // 绘制表格
        drawTable(canvas, coreCount);
    }
    
    /**
     * 绘制表格
     */
    private void drawTable(Canvas canvas, int coreCount) {
        float currentX = 0;
        float currentY = 0; // 从顶部开始，无额外padding
        
        // 绘制表头（第一行）
        drawHeaderRow(canvas, currentX, currentY, coreCount);
        currentY += headerHeight;
        
        // 绘制数据行
        for (int row = 0; row < rowHeaders.length; row++) {
            drawDataRow(canvas, currentX, currentY, coreCount, row);
            currentY += rowHeight;
        }
    }
    
    /**
     * 绘制表头行
     */
    private void drawHeaderRow(Canvas canvas, float startX, float startY, int coreCount) {
        float x = startX;
        float y = startY;
        
        // 获取列宽数组，如果未初始化则使用基础列宽
        if (columnWidths == null || columnWidths.length == 0) {
            calculateColumnWidths(coreCount, getWidth());
        }
        
        // 绘制左上角单元格（行标题列）
        float rowHeaderWidth = columnWidths != null && columnWidths.length > 0 ? columnWidths[0] : baseColumnWidth;
        Rect headerCell = new Rect((int)x, (int)y, (int)(x + rowHeaderWidth), (int)(y + headerHeight));
        canvas.drawRect(headerCell, headerPaint);
        drawBorder(canvas, headerCell);
        
        x += rowHeaderWidth;
        
        // 绘制CPU列标题
        textPaint.setTextSize(20f); // 减小字体
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        for (int i = 0; i < coreCount; i++) {
            float colWidth = columnWidths != null && columnWidths.length > i + 1 ? columnWidths[i + 1] : baseColumnWidth;
            Rect cell = new Rect((int)x, (int)y, (int)(x + colWidth), (int)(y + headerHeight));
            canvas.drawRect(cell, headerPaint);
            drawBorder(canvas, cell);
            
            // 绘制文本 - 水平和垂直居中
            String text = "CPU" + i;
            float centerX = x + colWidth / 2;
            float centerY = y + headerHeight / 2;
            // 垂直居中：baseline = centerY - (descent + ascent) / 2
            float textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2;
            
            canvas.drawText(text, centerX, textY, textPaint);
            
            x += colWidth;
        }
        
        textPaint.setFakeBoldText(false);
    }
    
    /**
     * 绘制数据行
     */
    private void drawDataRow(Canvas canvas, float startX, float startY, int coreCount, int rowIndex) {
        float x = startX;
        float y = startY;
        
        // 获取列宽数组，如果未初始化则使用基础列宽
        if (columnWidths == null || columnWidths.length == 0) {
            calculateColumnWidths(coreCount, getWidth());
        }
        
        // 绘制行标题
        float rowHeaderWidth = columnWidths != null && columnWidths.length > 0 ? columnWidths[0] : baseColumnWidth;
        Rect rowHeaderCell = new Rect((int)x, (int)y, (int)(x + rowHeaderWidth), (int)(y + rowHeight));
        canvas.drawRect(rowHeaderCell, headerPaint);
        drawBorder(canvas, rowHeaderCell);
        
        // 绘制行标题文本 - 水平和垂直居中
        textPaint.setTextSize(18f); // 减小字体
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        String rowHeader = rowHeaders[rowIndex];
        float centerX = x + rowHeaderWidth / 2;
        float centerY = y + rowHeight / 2;
        float textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2;
        canvas.drawText(rowHeader, centerX, textY, textPaint);
        textPaint.setFakeBoldText(false);
        
        x += rowHeaderWidth;
        
        // 绘制数据单元格
        textPaint.setTextSize(16f); // 减小字体
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        
        for (int i = 0; i < coreCount; i++) {
            CpuInfoReader.CpuFrequencyData data = frequencyDataList.get(i);
            
            float colWidth = columnWidths != null && columnWidths.length > i + 1 ? columnWidths[i + 1] : baseColumnWidth;
            Rect cell = new Rect((int)x, (int)y, (int)(x + colWidth), (int)(y + rowHeight));
            canvas.drawRect(cell, cellPaint);
            drawBorder(canvas, cell);
            
            // 根据行类型绘制不同的数据
            String cellText = "";
            if (rowIndex == 0) {
                // 最大频率
                if (data != null && data.isValid()) {
                    cellText = formatFrequency(data.maxFreq);
                } else {
                    cellText = "-";
                }
            } else if (rowIndex == 1) {
                // 最小频率
                if (data != null && data.isValid()) {
                    cellText = formatFrequency(data.minFreq);
                } else {
                    cellText = "-";
                }
            } else if (rowIndex == 2) {
                // 实时频率（动态更新）
                if (data != null && data.isValid()) {
                    cellText = formatFrequency(data.curFreq);
                    // 实时频率用不同颜色突出显示
                    textPaint.setColor(ContextCompat.getColor(getContext(), R.color.primary_blue));
                } else {
                    cellText = "-";
                    textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
                }
            }
            
            // 绘制单元格文本 - 水平和垂直居中
            float centerXCell = x + colWidth / 2;
            float centerYCell = y + rowHeight / 2;
            float textYCell = centerYCell - (textPaint.descent() + textPaint.ascent()) / 2;
            
            // 检查文字宽度，如果太宽则缩小字体
            float textWidth = textPaint.measureText(cellText);
            float availableWidth = colWidth - cellPadding * 2;
            if (textWidth > availableWidth) {
                float newSize = textPaint.getTextSize() * availableWidth / textWidth;
                textPaint.setTextSize(Math.max(11f, newSize));
            }
            
            canvas.drawText(cellText, centerXCell, textYCell, textPaint);
            
            // 恢复字体大小和颜色
            textPaint.setTextSize(16f);
            if (rowIndex == 2) {
                textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
            }
            
            x += colWidth;
        }
    }
    
    /**
     * 绘制边框
     */
    private void drawBorder(Canvas canvas, Rect rect) {
        // 绘制四条边
        canvas.drawLine(rect.left, rect.top, rect.right, rect.top, borderPaint);
        canvas.drawLine(rect.left, rect.bottom, rect.right, rect.bottom, borderPaint);
        canvas.drawLine(rect.left, rect.top, rect.left, rect.bottom, borderPaint);
        canvas.drawLine(rect.right, rect.top, rect.right, rect.bottom, borderPaint);
    }
    
    /**
     * 绘制空状态
     */
    private void drawEmptyState(Canvas canvas) {
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        textPaint.setTextSize(28f);
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
