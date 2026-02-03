#include <jni.h>
#include <string>
#include "PropertyParser.h"
#include "RootCheck.h"
#include "HookCheck.h"

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_kaze_devicefp_model_SettingsSettings_checkHookStatus(JNIEnv *env, jclass clazz) {
    if (!env) return JNI_FALSE;
    HookCheck checker;
    return checker.isHooked(env) ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_kaze_devicefp_model_SettingsSettings_checkRootStatus(JNIEnv *env, jclass clazz, jobject context) {
    if (!env || !context) return JNI_FALSE;
    RootCheck checker;
    return checker.isRooted(env, context) ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_kaze_devicefp_model_SettingsSettings_checkVpnStatus(JNIEnv *env, jclass clazz, jobject activity) {
    jboolean result = JNI_FALSE;

    // 1. 首先检查系统代理设置
    jclass uriClass = env->FindClass("java/net/URI");
    jmethodID createMethod = env->GetStaticMethodID(uriClass, "create", "(Ljava/lang/String;)Ljava/net/URI;");
    jstring uriString = env->NewStringUTF("https://api.wavespb.com/");
    jobject uriObj = env->CallStaticObjectMethod(uriClass, createMethod, uriString);

    jclass proxySelectorClass = env->FindClass("java/net/ProxySelector");
    jmethodID getDefaultMethod = env->GetStaticMethodID(proxySelectorClass, "getDefault", "()Ljava/net/ProxySelector;");
    jobject proxySelector = env->CallStaticObjectMethod(proxySelectorClass, getDefaultMethod);

    jmethodID selectMethod = env->GetMethodID(proxySelectorClass, "select", "(Ljava/net/URI;)Ljava/util/List;");
    jobject proxyList = env->CallObjectMethod(proxySelector, selectMethod, uriObj);

    jclass listClass = env->FindClass("java/util/List");
    jmethodID sizeMethod = env->GetMethodID(listClass, "size", "()I");
    jint listSize = env->CallIntMethod(proxyList, sizeMethod);

    // 可选：打印代理列表（对应Java中的System.out.println）
    jmethodID toStringMethod = env->GetMethodID(listClass, "toString", "()Ljava/lang/String;");
    jstring listStr = (jstring)env->CallObjectMethod(proxyList, toStringMethod);
    const char* listStrChars = env->GetStringUTFChars(listStr, NULL);
    env->ReleaseStringUTFChars(listStr, listStrChars);

    if (listSize > 0) {
        // 有代理设置，检查是否包含":"（代理地址:端口格式）
        jmethodID getMethod = env->GetMethodID(listClass, "get", "(I)Ljava/lang/Object;");
        jobject firstProxy = env->CallObjectMethod(proxyList, getMethod, 0);

        jclass proxyClass = env->FindClass("java/net/Proxy");
        jmethodID proxyToStringMethod = env->GetMethodID(proxyClass, "toString", "()Ljava/lang/String;");
        jstring proxyString = (jstring)env->CallObjectMethod(firstProxy, proxyToStringMethod);

        const char* proxyStr = env->GetStringUTFChars(proxyString, NULL);
        bool hasProxy = (strstr(proxyStr, ":") != NULL);
        env->ReleaseStringUTFChars(proxyString, proxyStr);

        if (hasProxy) {
            // 找到代理，返回true
            result = JNI_TRUE;

            // 清理本地引用
            env->DeleteLocalRef(uriClass);
            env->DeleteLocalRef(uriString);
            env->DeleteLocalRef(uriObj);
            env->DeleteLocalRef(proxySelectorClass);
            env->DeleteLocalRef(proxySelector);
            env->DeleteLocalRef(listClass);
            env->DeleteLocalRef(proxyList);
            env->DeleteLocalRef(proxyClass);
            env->DeleteLocalRef(firstProxy);

            return result;
        }

        // 有代理列表但没有有效代理（不包含":"），继续检查VPN
        env->DeleteLocalRef(firstProxy);
        env->DeleteLocalRef(proxyClass);
    }

    // 2. 没有代理或代理无效，检查VPN连接
    // 这部分对应原Java代码中的g0()方法
    try {
        // 获取ConnectivityManager
        jclass contextClass = env->FindClass("android/content/Context");
        jfieldID connectivityServiceField = env->GetStaticFieldID(contextClass, "CONNECTIVITY_SERVICE", "Ljava/lang/String;");
        jstring serviceName = (jstring)env->GetStaticObjectField(contextClass, connectivityServiceField);

        jmethodID getSystemServiceMethod = env->GetMethodID(
                env->GetObjectClass(activity),
                "getSystemService",
                "(Ljava/lang/String;)Ljava/lang/Object;"
        );

        jobject connectivityManagerObj = env->CallObjectMethod(activity, getSystemServiceMethod, serviceName);

        if (connectivityManagerObj != NULL) {
            jclass connectivityManagerClass = env->FindClass("android/net/ConnectivityManager");
            jmethodID getActiveNetworkMethod = env->GetMethodID(connectivityManagerClass, "getActiveNetwork", "()Landroid/net/Network;");
            jobject network = env->CallObjectMethod(connectivityManagerObj, getActiveNetworkMethod);

            if (network != NULL) {
                jmethodID getNetworkCapabilitiesMethod = env->GetMethodID(
                        connectivityManagerClass,
                        "getNetworkCapabilities",
                        "(Landroid/net/Network;)Landroid/net/NetworkCapabilities;"
                );

                jobject networkCapabilities = env->CallObjectMethod(connectivityManagerObj, getNetworkCapabilitiesMethod, network);

                if (networkCapabilities != NULL) {
                    jclass networkCapabilitiesClass = env->FindClass("android/net/NetworkCapabilities");
                    jmethodID hasTransportMethod = env->GetMethodID(networkCapabilitiesClass, "hasTransport", "(I)Z");
                    // TRANSPORT_VPN = 4
                    jboolean hasVpn = env->CallBooleanMethod(networkCapabilities, hasTransportMethod, 4);

                    result = hasVpn ? JNI_TRUE : JNI_FALSE;

                    env->DeleteLocalRef(networkCapabilitiesClass);
                    env->DeleteLocalRef(networkCapabilities);
                }

                env->DeleteLocalRef(network);
            }

            env->DeleteLocalRef(connectivityManagerClass);
            env->DeleteLocalRef(connectivityManagerObj);
        }

        env->DeleteLocalRef(contextClass);
        env->DeleteLocalRef(serviceName);

    } catch (...) {
        // 捕获任何异常，返回false
        result = JNI_FALSE;
    }

    // 清理所有本地引用
    env->DeleteLocalRef(uriClass);
    env->DeleteLocalRef(uriString);
    env->DeleteLocalRef(uriObj);
    env->DeleteLocalRef(proxySelectorClass);
    env->DeleteLocalRef(proxySelector);
    env->DeleteLocalRef(listClass);
    env->DeleteLocalRef(proxyList);
    env->DeleteLocalRef(listStr);

    return result;
}