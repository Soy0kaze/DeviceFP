package com.kaze.devicefp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.kaze.devicefp.adapter.SectionAdapter;
import com.kaze.devicefp.model.DeviceFingerprint;
import com.kaze.devicefp.model.FingerprintSection;
import com.kaze.devicefp.service.DeviceFingerprintService;
import com.kaze.devicefp.util.SectionGroupHelper;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MaterialButton btnCollect;
    private ProgressBar progressBar;
    private View progressContainer;
    private SectionAdapter sectionAdapter;
    private DeviceFingerprintService fingerprintService;
    
    // 统计信息视图
    private MaterialCardView cardStats;
    private TextView tvTotalCount;
    private TextView tvSuccessCount;
    private TextView tvPendingCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化组件
        initViews();
        
        // 初始化服务
        fingerprintService = new DeviceFingerprintService(this);
        
        // 可折叠板块适配器
        sectionAdapter = new SectionAdapter(this);
        recyclerView.setAdapter(sectionAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // 设置按钮点击事件
        btnCollect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 添加按钮点击动画
                v.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                v.animate()
                                        .scaleX(1.0f)
                                        .scaleY(1.0f)
                                        .setDuration(100)
                                        .start();
                            }
                        })
                        .start();
                
                collectFingerprints();
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 停止CPU频率更新
        if (sectionAdapter != null) {
            sectionAdapter.stopFrequencyUpdate();
        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view);
        btnCollect = findViewById(R.id.btn_collect);
        progressBar = findViewById(R.id.progress_bar);
        progressContainer = findViewById(R.id.progress_container);
        
        // 统计信息视图
        cardStats = findViewById(R.id.card_stats);
        tvTotalCount = findViewById(R.id.tv_total_count);
        tvSuccessCount = findViewById(R.id.tv_success_count);
        tvPendingCount = findViewById(R.id.tv_pending_count);
    }

    /**
     * 收集设备指纹信息
     */
    private void collectFingerprints() {
        android.util.Log.d("MainActivity", "collectFingerprints() called");
        
        // 显示加载进度
        showProgress(true);
        btnCollect.setEnabled(false);
        btnCollect.setText("收集中...");
        
        // 在新线程中执行收集操作（避免阻塞UI）
        new Thread(new Runnable() {
            @Override
            public void run() {
                android.util.Log.d("MainActivity", "Collect thread started");
                
                // 重新加载属性
                fingerprintService.reloadProperties();
                
                // 模拟收集延迟，让动画更流畅
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                android.util.Log.d("MainActivity", "Getting all fingerprints");
                // 获取所有指纹信息
                List<DeviceFingerprint> fingerprints = fingerprintService.getAllFingerprints();
                android.util.Log.d("MainActivity", "Got " + fingerprints.size() + " fingerprints");
                
                // 在主线程中更新UI
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        List<FingerprintSection> sections = SectionGroupHelper.groupFingerprints(fingerprints);
                        sectionAdapter.setSections(sections);
                        sectionAdapter.startFrequencyUpdate();
                        updateStatistics(fingerprints);
                        showProgress(false);
                        btnCollect.setEnabled(true);
                        btnCollect.setText("重新收集");
                        
                        // 显示统计卡片动画
                        if (cardStats.getVisibility() != View.VISIBLE) {
                            showStatsCard();
                        }
                        
                        Toast.makeText(MainActivity.this, 
                                "已收集 " + fingerprints.size() + " 项设备指纹信息", 
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }
    
    /**
     * 显示/隐藏进度条
     */
    private void showProgress(boolean show) {
        if (show) {
            progressContainer.setVisibility(View.VISIBLE);
            progressContainer.setAlpha(0f);
            progressContainer.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start();
        } else {
            progressContainer.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            progressContainer.setVisibility(View.GONE);
                        }
                    })
                    .start();
        }
    }
    
    /**
     * 更新统计信息
     */
    private void updateStatistics(List<DeviceFingerprint> fingerprints) {
        int total = fingerprints.size();
        int success = 0;
        int pending = 0;
        
        for (DeviceFingerprint fp : fingerprints) {
            if (fp.getStatus().contains("已获取")) {
                success++;
            } else {
                pending++;
            }
        }
        
        // 使用动画更新数字
        animateNumber(tvTotalCount, total);
        animateNumber(tvSuccessCount, success);
        animateNumber(tvPendingCount, pending);
    }
    
    /**
     * 数字动画
     */
    private void animateNumber(TextView textView, int targetValue) {
        ValueAnimator animator = ValueAnimator.ofInt(0, targetValue);
        animator.setDuration(800);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                textView.setText(String.valueOf(value));
            }
        });
        animator.start();
    }
    
    /**
     * 显示统计卡片
     */
    private void showStatsCard() {
        cardStats.setVisibility(View.VISIBLE);
        cardStats.setAlpha(0f);
        cardStats.setTranslationY(-20f);
        cardStats.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .start();
    }
}