//
// RootCheck.h - Root 检测 OOP 框架
// 请在 RootCheck.cpp 中实现各 checkXxx() 方法的具体逻辑。
//

#ifndef DEVICEFP_ROOTCHECK_H
#define DEVICEFP_ROOTCHECK_H

#include <jni.h>

/**
 * Root 检测器（面向对象框架）
 *
 * 使用方式（需从 JNI 传入 env 与 context）：
 *   RootCheck checker;
 *   bool rooted = checker.isRooted(env, context);
 *
 * checkRootManagementApps / checkMisc 需要 Context 以调用 Java 层 API。
 */
class RootCheck {
public:
    RootCheck() = default;
    virtual ~RootCheck() = default;

    /**
     * 执行 Root 检测（主入口）
     * @param env JNI 环境，供 checkRootManagementApps / checkMisc 使用
     * @param context Android Context，用于查询已安装应用等
     * @return true 表示设备可能已 Root，false 表示未检测到 Root
     */
    bool isRooted(JNIEnv* env, jobject context);

protected:
    /**
     * 检测 su 等 Root 相关二进制是否存在（如 /system/bin/su, /system/xbin/su）
     * @return true 表示检测到
     */
    virtual bool checkSuBinary();

    /**
     * 检测常见 Root 相关路径/文件是否存在（如 Magisk、SuperSU 相关路径）
     * @return true 表示检测到
     */
    virtual bool checkRootPaths();

    /**
     * 检测是否安装 Root 管理类应用（需 env/context 调用 PackageManager）
     * @return true 表示检测到
     */
    virtual bool checkRootManagementApps(JNIEnv* env, jobject context);

    /**
     * 检测 build.prop 等构建属性中的 Root 特征（如 test-keys, ro.debuggable）
     * @return true 表示检测到
     */
    virtual bool checkBuildTags();

    /**
     * 其他自定义检测项（可选用 env/context 调用 Java 层）
     * @return true 表示检测到
     */
    virtual bool checkMisc(JNIEnv* env, jobject context);
};

#endif // DEVICEFP_ROOTCHECK_H
