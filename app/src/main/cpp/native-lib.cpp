#include <jni.h>
#include <string>
#include "PropertyParser.h"

extern "C" {

// 为 com.kaze.devicefp.service.DeviceFingerprintService 提供的方法
JNIEXPORT jstring JNICALL
Java_com_kaze_devicefp_service_DeviceFingerprintService_baseDeviceInfo(
        JNIEnv* env,
        jclass /* clazz */) {

    PropertyParser parser(PropertyType::BUILD);
    if (!parser.parse()) {
        return env->NewStringUTF("");
    }

    // 获取所有属性并格式化为 JSON 格式的字符串
    auto props = parser.getAllProperties();
    std::string result = "{";
    bool first = true;
    for (const auto& pair : props) {
        if (!first) {
            result += ",";
        }
        first = false;
        result += "\"" + pair.first + "\":\"" + pair.second + "\"";
    }
    result += "}";

    return env->NewStringUTF(result.c_str());
}

// 获取特定属性值
JNIEXPORT jstring JNICALL
Java_com_kaze_devicefp_service_DeviceFingerprintService_getProperty(
        JNIEnv* env,
        jclass /* clazz */,
        jstring key) {

    PropertyParser parser(PropertyType::BUILD);
    if (!parser.parse()) {
        return env->NewStringUTF("");
    }

    const char* keyStr = env->GetStringUTFChars(key, nullptr);
    std::string value = parser.getProperty(std::string(keyStr));
    env->ReleaseStringUTFChars(key, keyStr);

    return env->NewStringUTF(value.c_str());
}

// 获取所有属性（返回键值对数组）- 简化版，只返回需要的属性
JNIEXPORT jobjectArray JNICALL
Java_com_kaze_devicefp_service_DeviceFingerprintService_getAllProperties(
        JNIEnv* env,
        jclass /* clazz */) {

    PropertyParser parser(PropertyType::BUILD);
    if (!parser.parse()) {
        return nullptr;
    }

    // 只获取需要的属性
    std::vector<std::string> keys = {
        "ro.product.brand",
        "ro.product.model",
        "ro.product.manufacturer",
        "ro.product.name",
        "ro.product.device",
        "ro.hardware",
        "ro.build.version.release",
        "ro.build.version.sdk",
        "ro.build.version.incremental"
    };

    // 计算实际找到的属性数量
    int foundCount = 0;
    for (const auto& key : keys) {
        std::string value = parser.getProperty(key);
        if (!value.empty()) {
            foundCount++;
        }
    }

    if (foundCount == 0) {
        return nullptr;
    }

    // 创建数组：键值对，所以是 foundCount * 2
    jobjectArray result = env->NewObjectArray(
            foundCount * 2,
            env->FindClass("java/lang/String"),
            env->NewStringUTF(""));

    int index = 0;
    for (const auto& key : keys) {
        std::string value = parser.getProperty(key);
        if (!value.empty()) {
            env->SetObjectArrayElement(
                    result,
                    index++,
                    env->NewStringUTF(key.c_str()));
            env->SetObjectArrayElement(
                    result,
                    index++,
                    env->NewStringUTF(value.c_str()));
        }
    }

    return result;
}

// 兼容旧代码的方法（如果需要保留）
JNIEXPORT jboolean JNICALL
Java_com_example_antitamper_PropertyChecker_checkTampering(
        JNIEnv* env,
        jobject /* this */) {

    PropertyParser parser(PropertyType::BUILD);
    if (!parser.parse()) {
        return JNI_TRUE; // 解析失败也认为是篡改
    }

    // 检查关键属性
    return parser.checkForTampering() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_example_antitamper_PropertyChecker_getDeviceInfo(
        JNIEnv* env,
        jobject /* this */) {

    PropertyParser parser(PropertyType::BUILD);
    if (!parser.parse()) {
        return env->NewStringUTF("");
    }

    std::string info = "Model: " + parser.getDeviceModel() + "\n";
    info += "Brand: " + parser.getDeviceBrand() + "\n";
    info += "Android: " + parser.getAndroidVersion() + "\n";
    info += "Fingerprint: " + parser.getBuildFingerprint();

    return env->NewStringUTF(info.c_str());
}

}