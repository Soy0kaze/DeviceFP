package com.kaze.devicefp.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kaze.devicefp.R;
import com.kaze.devicefp.model.FingerprintSection;

import java.util.ArrayList;
import java.util.List;

/**
 * 可折叠板块列表适配器
 * 每个 item 为 CardView：标题栏 + 可展开/收起的内容区域，带平滑动画
 */
public class SectionAdapter extends RecyclerView.Adapter<SectionAdapter.SectionViewHolder> {

    private static final int ANIM_DURATION_MS = 300;
    private static final String ICON_EXPANDED = "∧";
    private static final String ICON_COLLAPSED = "∨";

    private final List<FingerprintSection> sections = new ArrayList<>();
    private final Context context;
    private final List<FingerprintAdapter> cpuInnerAdapters = new ArrayList<>();

    public SectionAdapter(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
    }

    public void setSections(List<FingerprintSection> newSections) {
        cpuInnerAdapters.clear();
        sections.clear();
        if (newSections != null) {
            sections.addAll(newSections);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_section_card, parent, false);
        return new SectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SectionViewHolder holder, int position) {
        FingerprintSection section = sections.get(position);
        holder.sectionTitle.setText(section.getTitle());
        holder.sectionSummary.setText(section.getSummary());
        holder.sectionSummary.setVisibility(section.getSummary().isEmpty() ? View.GONE : View.VISIBLE);

        boolean expanded = section.isExpanded();
        holder.sectionExpandIcon.setText(expanded ? ICON_EXPANDED : ICON_COLLAPSED);
        holder.contentContainer.setVisibility(expanded ? View.VISIBLE : View.GONE);
        if (expanded) {
            holder.contentContainer.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        } else {
            holder.contentContainer.getLayoutParams().height = 0;
        }

        // 内层列表：使用 FingerprintAdapter，隐藏分类标题
        if (holder.innerAdapter == null) {
            holder.innerAdapter = new FingerprintAdapter(context, true);
            holder.contentList.setLayoutManager(new LinearLayoutManager(context));
            holder.contentList.setAdapter(holder.innerAdapter);
            holder.contentList.setNestedScrollingEnabled(false);
        }
        cpuInnerAdapters.remove(holder.innerAdapter);
        holder.innerAdapter.setFingerprints(section.getItems());
        if ("CPU信息".equals(section.getTitle())) {
            cpuInnerAdapters.add(holder.innerAdapter);
        }

        holder.header.setOnClickListener(v -> {
            section.toggleExpanded();
            boolean nowExpanded = section.isExpanded();
            holder.sectionExpandIcon.setText(nowExpanded ? ICON_EXPANDED : ICON_COLLAPSED);
            animateExpandCollapse(holder.contentContainer, nowExpanded);
        });
    }

    private void animateExpandCollapse(final View contentContainer, boolean expand) {
        if (expand) {
            contentContainer.setVisibility(View.VISIBLE);
            ViewGroup.LayoutParams lp = contentContainer.getLayoutParams();
            lp.height = 0;
            contentContainer.setLayoutParams(lp);
            contentContainer.requestLayout();

            contentContainer.post(() -> {
                int parentWidth = contentContainer.getWidth() > 0
                        ? contentContainer.getWidth()
                        : ((View) contentContainer.getParent()).getWidth();
                if (parentWidth <= 0) {
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    contentContainer.setLayoutParams(lp);
                    return;
                }
                contentContainer.measure(
                        View.MeasureSpec.makeMeasureSpec(parentWidth, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                final int targetHeight = contentContainer.getMeasuredHeight();
                if (targetHeight <= 0) {
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    contentContainer.setLayoutParams(lp);
                    return;
                }
                ValueAnimator anim = ValueAnimator.ofInt(0, targetHeight);
                anim.setDuration(ANIM_DURATION_MS);
                anim.addUpdateListener(animation -> {
                    lp.height = (int) animation.getAnimatedValue();
                    contentContainer.setLayoutParams(lp);
                });
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        contentContainer.setLayoutParams(lp);
                        contentContainer.requestLayout();
                    }
                });
                anim.start();
            });
        } else {
            final int startHeight = contentContainer.getHeight();
            ValueAnimator anim = ValueAnimator.ofInt(startHeight, 0);
            anim.setDuration(ANIM_DURATION_MS);
            anim.addUpdateListener(animation -> {
                contentContainer.getLayoutParams().height = (int) animation.getAnimatedValue();
                contentContainer.requestLayout();
            });
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    contentContainer.setVisibility(View.GONE);
                    contentContainer.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    contentContainer.requestLayout();
                }
            });
            anim.start();
        }
    }

    @Override
    public int getItemCount() {
        return sections.size();
    }

    @Override
    public void onViewRecycled(@NonNull SectionViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.innerAdapter != null) {
            cpuInnerAdapters.remove(holder.innerAdapter);
        }
    }

    public void startFrequencyUpdate() {
        for (FingerprintAdapter a : cpuInnerAdapters) {
            a.startFrequencyUpdate();
        }
    }

    public void stopFrequencyUpdate() {
        for (FingerprintAdapter a : cpuInnerAdapters) {
            a.stopFrequencyUpdate();
        }
    }

    static class SectionViewHolder extends RecyclerView.ViewHolder {
        View header;
        TextView sectionTitle;
        TextView sectionSummary;
        TextView sectionExpandIcon;
        LinearLayout contentContainer;
        RecyclerView contentList;
        FingerprintAdapter innerAdapter;

        SectionViewHolder(@NonNull View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.section_header);
            sectionTitle = itemView.findViewById(R.id.section_title);
            sectionSummary = itemView.findViewById(R.id.section_summary);
            sectionExpandIcon = itemView.findViewById(R.id.section_expand_icon);
            contentContainer = itemView.findViewById(R.id.section_content_container);
            contentList = itemView.findViewById(R.id.section_content_list);
        }
    }
}
