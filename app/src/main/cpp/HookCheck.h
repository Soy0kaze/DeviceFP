//
// HookCheck.h - Hook/Frida/Xposed 检测
//

#ifndef DEVICEFP_HOOKCHECK_H
#define DEVICEFP_HOOKCHECK_H

#include <jni.h>

/**
 * Hook 检测器：检测 Frida、Xposed、虚拟机等 Hook 环境
 *
 * 使用方式（从 JNI 传入 env）：
 *   HookCheck checker;
 *   bool hooked = checker.isHooked(env);
 */
class HookCheck {
public:
    HookCheck() = default;
    virtual ~HookCheck() = default;

    /**
     * 执行 Hook 检测（主入口）
     * @param env JNI 环境，供 checkXposedFridaFiles 等使用
     * @return true 表示检测到 Hook 环境，false 表示未检测到
     */
    bool isHooked(JNIEnv* env);

protected:
    /** 检测进程列表中是否包含 Frida 相关进程 */
    virtual bool checkFridaProcess();
    /** 检测 Frida 默认端口 27042 是否开放 */
    virtual bool checkFridaPort();
    /** 检测 /proc/self/maps 中是否包含 frida 或 gadget */
    virtual bool checkFridaInMaps();
    /** 检测虚拟机相关文件是否存在 */
    virtual bool checkVMFiles();
    /** 检测 Xposed/Frida 相关文件及类是否加载（需 env） */
    virtual bool checkXposedFridaFiles(JNIEnv* env);
    /** 辅助：检查指定类是否已加载 */
    virtual bool checkClassLoaded(JNIEnv* env, const char* className);
};

#endif // DEVICEFP_HOOKCHECK_H
