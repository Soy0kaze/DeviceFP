//
// RootCheck.cpp - Root 检测 OOP 实现（框架）
//

#include "RootCheck.h"
#include <sys/stat.h>
#include <unistd.h>
#include <string>
#include <sys/system_properties.h>
#include <cstring>
#include <jni.h>
#include <fstream>
#include <sstream>
#include <vector>
#include <array>
#include <memory>
#include <cstdio>

bool RootCheck::isRooted(JNIEnv* env, jobject context) {
    if (checkSuBinary()) return true;
    if (checkRootPaths()) return true;
    if (env && context && checkRootManagementApps(env, context)) return true;
    if (checkBuildTags()) return true;
    if (env && context && checkMisc(env, context)) return true;
    return false;
}

bool RootCheck::checkSuBinary() {
    // 常见的root二进制文件和路径数组
    const char* suPaths[] = {
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
            "/system/bin/.ext/.su",
            "/system/usr/we-need-root/su-backup",
            "/system/xbin/mu",
            "/system/su",
            "/system/bin/busybox",
            "/system/xbin/busybox",
            "/data/local/bin/busybox",
            "/system/etc/selinux/selinux_policy",
            "/data/data/com.superuser.supersu"
    };

    const int pathCount = sizeof(suPaths) / sizeof(suPaths[0]);

    // 遍历所有路径检查文件是否存在
    for (int i = 0; i < pathCount; ++i) {
        struct stat fileStat;
        if (stat(suPaths[i], &fileStat) == 0) {
            if (S_ISREG(fileStat.st_mode) || S_ISLNK(fileStat.st_mode)) {
                if (access(suPaths[i], X_OK) == 0) {
                    return true;
                }
            }
        }
    }

    FILE* pipe = popen("/system/xbin/which su 2>/dev/null", "r");
    if (pipe) {
        char buffer[128];
        if (fgets(buffer, sizeof(buffer), pipe) != nullptr) {
            pclose(pipe);
            return true;
        }
        pclose(pipe);
    }

    // 不使用 system("su -c ...")：会触发 Root 授权弹窗或阻塞，影响体验且不可靠
    return false;
}

bool RootCheck::checkRootPaths() {
    // 检测 Magisk、SuperSU 等常见 Root 相关路径/文件
    const char* rootPaths[] = {
            "/magisk", "/sbin/.magisk", "/dev/.magisk",
            "/cache/magisk", "/data/adb/magisk",
            "/system/app/Superuser.apk", "/system/xbin/daemonsu"
    };
    const int n = sizeof(rootPaths) / sizeof(rootPaths[0]);
    for (int i = 0; i < n; ++i) {
        struct stat st;
        if (stat(rootPaths[i], &st) == 0) return true;
    }
    return false;
}

bool RootCheck::checkRootManagementApps(JNIEnv* env, jobject context) {
    if (!env || !context) return false;
    // Root管理应用包名列表
    const char* rootApps[] = {
            "eu.chainfire.supersu",            // SuperSU
            "com.topjohnwu.magisk",            // Magisk
            "com.noshufou.android.su",         // Superuser
            "com.noshufou.android.su.elite",   // Superuser Elite
            "com.koushikdutta.superuser",      // Superuser (Koush)
            "com.thirdparty.superuser",        // 第三方Superuser
            "com.yellowes.su",                 // Superuser (其他版本)
            "com.kingroot.kinguser",           // KingRoot
            "com.kingo.root",                  // Kingo Root
            "com.smedialink.oneclickroot",     // 一键Root
            "com.zhiqupk.root.global",         // 其他Root应用
            "com.alephzain.framaroot",         // Framaroot
            "com.koushikdutta.rommanager",     // ROM Manager
            "com.koushikdutta.rommanager.license",
            "com.dimonvideo.luckypatcher",     // Lucky Patcher
            "com.chelpus.lackypatch",
            "com.ramdroid.appquarantine",      // 应用隔离
            "com.ramdroid.appquarantinepro",
            "com.android.vending.billing.InAppBillingService.COIN",
            "com.android.vending.billing.InAppBillingService.LUCK",
            "com.chelpus.luckypatcher",        // Lucky Patcher
            "com.blackmartalpha",              // BlackMart
            "org.blackmart.market",
            "com.allinone.free",
            "com.repodroid.app",
            "org.creeplays.hack",
            "com.baseappfull.fwd",
            "com.zmapp",
            "com.dv.marketmod.installer",
            "org.mobilism.android",
            "com.android.wp.net.log",
            "com.android.camera.update",
            "cc.madkite.freedom",
            "com.solohsu.android.edxp.manager",   // EdXposed
            "org.meowcat.edxposed.manager",
            "com.xmodgame",                       // 游戏修改器
            "com.cih.game_cih",
            "com.charles.lpoqasert",
            "catch_.me_.if_.you_.can_",
            "com.devadvance.rootcloak",           // Root隐藏工具
            "com.devadvance.rootcloakplus",
            "de.robv.android.xposed.installer",   // Xposed
            "com.saurik.substrate",               // Substrate
            "com.zachspong.temprootremovejb",
            "com.amphoras.hidemyroot",            // Hide My Root
            "com.amphoras.hidemyrootadfree",
            "com.formyhm.hiderootPremium",
            "com.formyhm.hideroot"
    };

    // 获取PackageManager类和方法
    jclass contextClass = env->GetObjectClass(context);
    if (!contextClass) return false;
    jmethodID getPackageManagerMethod = env->GetMethodID(contextClass, "getPackageManager", "()Landroid/content/pm/PackageManager;");
    env->DeleteLocalRef(contextClass);
    if (!getPackageManagerMethod) return false;

    jobject packageManager = env->CallObjectMethod(context, getPackageManagerMethod);
    if (!packageManager) return false;

    jclass packageManagerClass = env->GetObjectClass(packageManager);
    if (!packageManagerClass) {
        env->DeleteLocalRef(packageManager);
        return false;
    }
    jmethodID getPackageInfoMethod = env->GetMethodID(packageManagerClass, "getPackageInfo", "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");
    if (!getPackageInfoMethod) {
        env->DeleteLocalRef(packageManagerClass);
        env->DeleteLocalRef(packageManager);
        return false;
    }

    int rootAppsCount = sizeof(rootApps) / sizeof(rootApps[0]);
    for (int i = 0; i < rootAppsCount; i++) {
        jstring packageName = env->NewStringUTF(rootApps[i]);
        if (!packageName) continue;

        jobject packageInfo = env->CallObjectMethod(packageManager, getPackageInfoMethod, packageName, 0x80);
        env->DeleteLocalRef(packageName);

        if (!env->ExceptionCheck()) {
            if (packageInfo) env->DeleteLocalRef(packageInfo);
            env->DeleteLocalRef(packageManagerClass);
            env->DeleteLocalRef(packageManager);
            return true;
        }
        env->ExceptionClear();
    }

    env->DeleteLocalRef(packageManagerClass);
    env->DeleteLocalRef(packageManager);
    return false;
}

bool RootCheck::checkBuildTags() {
    char value[92];
    value[0] = '\0';
    __system_property_get("ro.debuggable", value);
    if (strcmp(value, "1") == 0) return true;
    value[0] = '\0';
    __system_property_get("ro.secure", value);
    if (strcmp(value, "0") == 0) return true;
    return false;
}

bool RootCheck::checkMisc(JNIEnv* env, jobject context) {
    // 检测1: 检查 /system 分区是否为可读写挂载（Root 后常见）
    std::array<char, 256> buffer;
    std::unique_ptr<FILE, decltype(&pclose)> mountPipe(popen("mount", "r"), pclose);

    if (mountPipe) {
        while (fgets(buffer.data(), static_cast<int>(buffer.size()), mountPipe.get())) {
            std::string line(buffer.data());
            // 匹配 "/system " 或 "/system" 作为挂载点，且包含 "rw"（避免误匹配 /system/xxx 的 rw）
            size_t sysPos = line.find("/system");
            if (sysPos != std::string::npos) {
                char next = (sysPos + 7 < line.size()) ? line[sysPos + 7] : '\0';
                if (next == ' ' || next == '\t' || next == '\0') {
                    if (line.find("rw") != std::string::npos) return true;
                }
            }
        }
    }

    // 检测2: 检查 SELinux 状态（enforce=0 表示 permissive，常见于 Root 环境）
    std::ifstream selinuxFile("/sys/fs/selinux/enforce");
    if (selinuxFile) {
        std::string enforce;
        if (std::getline(selinuxFile, enforce)) {
            size_t start = enforce.find_first_not_of(" \t\r\n");
            if (start != std::string::npos) {
                size_t end = enforce.find_last_not_of(" \t\r\n");
                if (end == std::string::npos) end = start;
                std::string trimmed = enforce.substr(start, end - start + 1);
                if (trimmed == "0") return true;
            }
        }
    }

    // 检测3: 检查内核版本是否包含 root
    std::unique_ptr<FILE, decltype(&pclose)> unamePipe(popen("uname -r", "r"), pclose);
    if (unamePipe) {
        while (fgets(buffer.data(), static_cast<int>(buffer.size()), unamePipe.get())) {
            std::string kernelVer(buffer.data());
            if (kernelVer.find("root") != std::string::npos) {
                return true;  // Root 检测通过
            }
        }
    }

    return false;  // 所有检测都未通过
}
