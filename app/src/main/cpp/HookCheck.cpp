//
// HookCheck.cpp - Hook/Frida/Xposed 检测实现
//

#include "HookCheck.h"
#include <sys/stat.h>
#include <unistd.h>
#include <string>
#include <cstring>
#include <jni.h>
#include <fstream>
#include <sstream>
#include <vector>
#include <array>
#include <memory>
#include <cstdio>
#include <algorithm>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

bool HookCheck::isHooked(JNIEnv* env) {
    if (checkFridaProcess()) return true;
    if (checkFridaPort()) return true;
    if (checkFridaInMaps()) return true;
    if (checkVMFiles()) return true;
    if (env && checkXposedFridaFiles(env)) return true;
    return false;
}

bool HookCheck::checkFridaProcess() {
    // 对应 Java 中的 r() 方法
    // 检测进程列表中是否包含 Frida 相关进程

    bool found = false;
    try {
        std::array<char, 512> buffer;
        std::unique_ptr<FILE, decltype(&pclose)> pipe(popen("ps", "r"), pclose);

        if (!pipe) {
            return false;
        }

        while (fgets(buffer.data(), static_cast<int>(buffer.size()), pipe.get()) != nullptr) {
            std::string line(buffer.data());

            // 将行转换为小写进行匹配
            std::transform(line.begin(), line.end(), line.begin(), ::tolower);

            // 检查是否包含 frida 相关字符串
            const char* frida_patterns[] = {"frida-server", "frida-agent", "frida"};

            for (int i = 0; i < 3; i++) {
                if (line.find(frida_patterns[i]) != std::string::npos) {
                    found = true;
                    break;
                }
            }

            if (found) {
                break;
            }
        }
    }
    catch (...) {
        // 异常处理
        return false;
    }

    return found;
}

bool HookCheck::checkFridaPort() {
    // 对应 Java 中的 q() 方法
    // 检测 127.0.0.1:27042 端口是否开放

    int sockfd = -1;
    struct sockaddr_in server_addr;

    try {
        // 创建 socket
        sockfd = socket(AF_INET, SOCK_STREAM, 0);
        if (sockfd < 0) {
            return false;
        }

        // 设置超时
        struct timeval timeout;
        timeout.tv_sec = 0;
        timeout.tv_usec = 100000; // 100ms
        setsockopt(sockfd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
        setsockopt(sockfd, SOL_SOCKET, SO_SNDTIMEO, &timeout, sizeof(timeout));

        // 设置地址信息
        memset(&server_addr, 0, sizeof(server_addr));
        server_addr.sin_family = AF_INET;
        server_addr.sin_port = htons(27042); // Frida 默认端口
        inet_pton(AF_INET, "127.0.0.1", &server_addr.sin_addr);

        // 尝试连接
        int result = connect(sockfd, (struct sockaddr*)&server_addr, sizeof(server_addr));

        // 关闭 socket
        if (sockfd >= 0) {
            close(sockfd);
        }

        // 连接成功
        if (result == 0) {
            return true;
        }
    }
    catch (...) {
        // 异常处理
        if (sockfd >= 0) {
            close(sockfd);
        }
        return false;
    }

    return false;
}

bool HookCheck::checkFridaInMaps() {
    // 对应 Java 中的 p() 方法
    // 检测 /proc/self/maps 中是否包含 frida 或 gadget

    bool found = false;

    try {
        std::ifstream mapsFile("/proc/self/maps");
        if (!mapsFile.is_open()) {
            return false;
        }

        std::string line;
        while (std::getline(mapsFile, line)) {
            // 检查是否包含 frida 或 gadget
            if (line.find("frida") != std::string::npos ||
                line.find("gadget") != std::string::npos) {
                found = true;
                break;
            }
        }

        mapsFile.close();
    }
    catch (...) {
        // 异常处理
        return false;
    }

    return found;
}

bool HookCheck::checkVMFiles() {
    // 对应 Java 中的 u() 方法
    // 检测虚拟机相关文件是否存在

    const char* vm_files[] = {
            "/system/bin/androVM-prop",
            "/system/bin/microvirt-prop",
            "/system/lib/libc_malloc_debug_qemu.so",
            "/system/bin/nox-prop",
            "/system/bin/microvirt-prop64"
    };
    const int n = static_cast<int>(sizeof(vm_files) / sizeof(vm_files[0]));
    for (int i = 0; i < n; i++) {
        if (access(vm_files[i], F_OK) == 0) return true;
    }

    return false;
}
bool HookCheck::checkXposedFridaFiles(JNIEnv* env) {
    if (!env) return false;
    // 第一部分：检查 Xposed/Frida 相关文件是否存在
    const char* suspicious_files[] = {
            "/system/lib/libsubstrate.so",
            "/system/lib/libxposed_art.so",
            "/system/lib/libfrida-gadget.so",
            "/data/local/tmp/frida-server",
            "/system/xbin/su"
    };
    const int n = static_cast<int>(sizeof(suspicious_files) / sizeof(suspicious_files[0]));
    for (int i = 0; i < n; i++) {
        if (access(suspicious_files[i], F_OK) == 0) return true;
    }
    // 第二部分：检查 XposedBridge / frida 相关类是否已加载
    return checkClassLoaded(env, "de.robv.android.xposed.XposedBridge") ||
           checkClassLoaded(env, "frida.Server");
}

// 辅助方法：检查类是否已加载（FindClass 要求 JNI 格式：package/Class，即 . 改为 /）
bool HookCheck::checkClassLoaded(JNIEnv* env, const char* className) {
    if (!env || !className) return false;
    std::string jniName(className);
    for (size_t i = 0; i < jniName.size(); i++) {
        if (jniName[i] == '.') jniName[i] = '/';
    }
    jclass cls = env->FindClass(jniName.c_str());
    if (cls != nullptr) {
        env->DeleteLocalRef(cls);
        return true;
    }
    if (env->ExceptionCheck()) env->ExceptionClear();
    return false;
}