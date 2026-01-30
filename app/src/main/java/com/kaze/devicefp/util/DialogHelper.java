package com.kaze.devicefp.util;

import android.content.Context;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kaze.devicefp.R;

/**
 * 弹窗工具类。所有通过此类创建的 AlertDialog 会默认使用应用图标（paojie）。
 */
public final class DialogHelper {

    private DialogHelper() {}

    /**
     * 创建带有应用默认图标的 MaterialAlertDialogBuilder。
     * 之后可链式调用 setTitle、setMessage、setPositiveButton 等。
     *
     * @param context 上下文，一般为 Activity
     * @return 已设置图标的 Builder
     */
    public static MaterialAlertDialogBuilder newAlertDialog(Context context) {
        return new MaterialAlertDialogBuilder(context)
                .setIcon(R.drawable.paojie);
    }
}
