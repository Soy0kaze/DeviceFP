//
// Created by lfloat on 2026/1/28.
//
#include "PropertyParser.h"
#include <fstream>
#include <iostream>
#include <iomanip>
#include <cstring>
#include <sstream>
#include <cstdint>

#ifdef __ANDROID__
#include <android/log.h>
#define LOG_TAG "PropertyParser"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#else
#define LOGD(...) printf(__VA_ARGS__)
#define LOGE(...) fprintf(stderr, __VA_ARGS__)
#endif

PropertyParser::PropertyParser(PropertyType type)
        : m_type(type)
        , m_androidVersion(0) {
    m_filePath = getPropertyFilePath();
}

std::string PropertyParser::getPropertyFilePath() const {
    switch (m_type) {
        case PropertyType::BUILD:
            return "/dev/__properties__/u:object_r:build_prop:s0";
//        case PropertyType::SYSTEM:
//            return "/dev/__properties__/u:object_r:system_prop:s0";
//        case PropertyType::DEFAULT:
//            return "/dev/__properties__/u:object_r:default_prop:s0";
//        case PropertyType::VENDOR:
//            return "/dev/__properties__/u:object_r:vendor_build_prop:s0";
        default:
            return "/dev/__properties__/u:object_r:build_prop:s0";
    }
}

bool PropertyParser::readPropertyFile() {
    std::ifstream file(m_filePath, std::ios::binary | std::ios::ate);
    if (!file.is_open()) {
        LOGE("无法打开属性文件: %s\n", m_filePath.c_str());
        return false;
    }

    // 获取文件大小
    std::streamsize size = file.tellg();
    file.seekg(0, std::ios::beg);

    // 读取文件数据
    m_fileData.resize(size);
    if (!file.read(reinterpret_cast<char*>(m_fileData.data()), size)) {
        LOGE("读取属性文件失败\n");
        return false;
    }

    LOGD("成功读取属性文件，大小: %zu bytes\n", m_fileData.size());
    return true;
}

bool PropertyParser::parse() {
    if (!readPropertyFile()) {
        LOGE("读取属性文件失败\n");
        return false;
    }

    LOGD("文件大小: %zu bytes\n", m_fileData.size());
    
    // 搜索 "PROP" 魔数在文件中的位置
    size_t propOffset = SIZE_MAX;
    for (size_t i = 0; i < m_fileData.size() - 4; i++) {
        if (m_fileData[i] == 0x50 && m_fileData[i+1] == 0x52 && 
            m_fileData[i+2] == 0x4F && m_fileData[i+3] == 0x50) {
            propOffset = i;
            LOGD("找到 PROP 魔数在偏移: %zu\n", propOffset);
            break;
        }
    }
    
    // 如果找到了 PROP，从该位置读取文件头
    if (propOffset != SIZE_MAX && propOffset + sizeof(PropertyHeader) <= m_fileData.size()) {
        LOGD("从偏移 %zu 读取文件头\n", propOffset);
        memcpy(&m_header, m_fileData.data() + propOffset, sizeof(PropertyHeader));
        
        LOGD("文件头信息:\n");
        LOGD("  Magic: 0x%08X\n", m_header.magic);
        LOGD("  Version: %u\n", m_header.version);
        LOGD("  NumSlots: %u\n", m_header.num_slots);
        LOGD("  TOCOffset: %u (相对偏移)\n", m_header.toc_offset);
        LOGD("  DataSize: %u\n", m_header.data_size);
        
        if (m_header.magic == 0x504F5250) { // "PROP"
            LOGD("检测到Android 7.0-8.0格式 (PROP)\n");
            
            // 检查文件头是否合理
            if (m_header.num_slots == 0 || m_header.num_slots > 100000) {
                LOGD("NumSlots 异常 (%u)，可能文件头结构不对，尝试字符串搜索\n", m_header.num_slots);
                // 回退到字符串搜索
            } else {
                // 保存 propOffset 以便在 parseAndroid7 中使用
                uint32_t dataStart = propOffset + sizeof(PropertyHeader);
                uint32_t actualTocOffset = dataStart + m_header.toc_offset;
                m_header.toc_offset = actualTocOffset; // 临时存储实际偏移
                LOGD("数据区起始: %u, 实际TOC偏移: %u\n", dataStart, actualTocOffset);
                
                if (parseAndroid7(propOffset)) {
                    return true;
                }
                LOGD("parseAndroid7 失败，回退到字符串搜索\n");
            }
        }
    }
    
    // 如果没找到标准格式，尝试字符串搜索
    LOGD("未找到标准文件头，尝试字符串搜索解析\n");
    if (parseByStringSearch()) {
        LOGD("字符串搜索解析成功\n");
        return true;
    }
    
    // 最后尝试TOC表解析
    if (parseTOCTable()) {
        LOGD("TOC表解析成功\n");
        return true;
    }
    
    LOGE("所有解析方式都失败\n");
    return false;
}

bool PropertyParser::parseAndroid7(size_t headerOffset) {
    // Android 7.0-8.0格式解析
    uint32_t actualTocOffset = m_header.toc_offset; // 已经包含完整偏移
    uint32_t dataStart = headerOffset + sizeof(PropertyHeader);
    
    LOGD("parseAndroid7: headerOffset=%zu, dataStart=%u, actualTocOffset=%u, numSlots=%u\n",
         headerOffset, dataStart, actualTocOffset, m_header.num_slots);
    
    if (m_fileData.size() < actualTocOffset + m_header.num_slots * 12) {
        LOGE("文件大小不符合预期: 需要 %u, 实际 %zu\n", 
             actualTocOffset + m_header.num_slots * 12, m_fileData.size());
        return false;
    }

    int foundCount = 0;
    for (uint32_t i = 0; i < m_header.num_slots && i < 10000; i++) { // 限制最大数量
        uint32_t offset = actualTocOffset + i * 12;

        if (offset + 12 > m_fileData.size()) {
            break;
        }

        // 读取TOC条目
        uint32_t name_offset, value_offset, size;
        memcpy(&name_offset, m_fileData.data() + offset, 4);
        memcpy(&value_offset, m_fileData.data() + offset + 4, 4);
        memcpy(&size, m_fileData.data() + offset + 8, 4);

        // 计算实际偏移（相对于数据区开始）
        uint32_t actual_name_offset = dataStart + name_offset;
        uint32_t actual_value_offset = dataStart + value_offset;

        // 读取属性名
        std::string prop_name;
        for (uint32_t j = actual_name_offset;
             j < m_fileData.size() && m_fileData[j] != 0;
             j++) {
            prop_name.push_back(static_cast<char>(m_fileData[j]));
        }

        // 读取属性值
        std::string prop_value;
        for (uint32_t j = actual_value_offset;
             j < m_fileData.size() && m_fileData[j] != 0;
             j++) {
            prop_value.push_back(static_cast<char>(m_fileData[j]));
        }

        if (!prop_name.empty() && prop_name.find("ro.") == 0) {
            m_properties[prop_name] = prop_value;
            foundCount++;
            if (foundCount <= 10) {
                LOGD("找到属性[%d]: %s = %s\n", foundCount, prop_name.c_str(), prop_value.c_str());
            }

            PropertyPair pair;
            pair.key = prop_name;
            pair.value = prop_value;
            pair.offset = actual_name_offset;
            pair.size = size;
            m_detailedProps[prop_name] = pair;
        }
    }

    LOGD("parseAndroid7 完成，找到 %d 个属性\n", foundCount);
    return foundCount > 0;
}

bool PropertyParser::parseAndroid8() {
    // Android 8.1-9.0格式解析
    // 格式类似Android 7，但可能有细微差别
    return parseAndroid7(); // 暂时使用相同解析
}

bool PropertyParser::parseAndroid10() {
    // Android 10+格式解析
    // 注意：Android 10+的格式有较大变化，这里提供基本解析

    // 查找所有字符串
    std::string content;
    for (size_t i = 0; i < m_fileData.size(); i++) {
        if (m_fileData[i] >= 32 && m_fileData[i] <= 126) {
            content.push_back(static_cast<char>(m_fileData[i]));
        } else if (m_fileData[i] == 0) {
            content.push_back(' ');
        }
    }

    // 尝试从字符串中提取属性
    size_t pos = 0;
    while (pos < content.size()) {
        // 查找可能的属性名
        size_t start = content.find("ro.", pos);
        if (start == std::string::npos) break;

        // 查找等号
        size_t eq_pos = content.find('=', start);
        if (eq_pos == std::string::npos) {
            pos = start + 1;
            continue;
        }

        // 提取属性名
        std::string key;
        for (size_t i = start; i < eq_pos; i++) {
            if (content[i] >= 32 && content[i] <= 126) {
                key.push_back(content[i]);
            }
        }

        // 查找属性值结束（遇到不可打印字符或空格）
        std::string value;
        size_t val_start = eq_pos + 1;
        while (val_start < content.size()) {
            char c = content[val_start];
            if (c >= 32 && c <= 126) {
                value.push_back(c);
                val_start++;
            } else {
                break;
            }
        }

        if (!key.empty() && !value.empty()) {
            m_properties[key] = value;
        }

        pos = val_start;
    }

    return !m_properties.empty();
}

bool PropertyParser::parseByStringSearch() {
    LOGD("开始字符串搜索解析（简化版，只搜索需要的属性）\n");
    
    // 只搜索需要的属性
    std::vector<std::string> targetKeys = {
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
    
    int foundCount = 0;
    
    // 逐个搜索每个目标键
    for (const auto& targetKey : targetKeys) {
        // 在二进制数据中搜索这个键
        for (size_t i = 0; i < m_fileData.size() - targetKey.length(); i++) {
            bool match = true;
            // 检查键是否匹配
            for (size_t j = 0; j < targetKey.length(); j++) {
                if (m_fileData[i + j] != static_cast<uint8_t>(targetKey[j])) {
                    match = false;
                    break;
                }
            }
            
            if (match) {
                // 找到键，检查后面是否有等号
                size_t keyEnd = i + targetKey.length();
                if (keyEnd < m_fileData.size() && m_fileData[keyEnd] == '=') {
                    // 提取属性值（到null或不可打印字符）
                    size_t valStart = keyEnd + 1;
                    std::string value;
                    while (valStart < m_fileData.size() && valStart < keyEnd + 200) {
                        char c = static_cast<char>(m_fileData[valStart]);
                        if (c == 0) {
                            break; // 遇到null终止符
                        }
                        if (c >= 32 && c <= 126) {
                            value.push_back(c);
                        } else {
                            // 遇到不可打印字符，也停止
                            break;
                        }
                        valStart++;
                    }
                    
                    if (!value.empty()) {
                        m_properties[targetKey] = value;
                        foundCount++;
                        LOGD("找到属性: %s = %s\n", targetKey.c_str(), value.c_str());
                    }
                }
                // 找到后跳出内层循环，继续下一个键
                break;
            }
        }
    }
    
    LOGD("字符串搜索解析完成，找到 %d/%zu 个目标属性\n", foundCount, targetKeys.size());
    return foundCount > 0;
}

bool PropertyParser::parseTOCTable() {
    LOGD("开始TOC表解析\n");
    
    // 通用TOC表解析
    if (m_header.toc_offset >= m_fileData.size()) {
        LOGD("TOC偏移超出文件大小\n");
        return false;
    }

    // 尝试解析属性名和值
    size_t offset = m_header.toc_offset;
    size_t max_entries = 1000; // 防止无限循环
    int foundCount = 0;

    for (size_t i = 0; i < max_entries && offset < m_fileData.size() - 8; i++) {
        // 读取两个偏移量
        uint32_t offsets[2];
        memcpy(offsets, m_fileData.data() + offset, 8);
        offset += 8;

        // 尝试读取属性名
        std::string prop_name;
        uint32_t name_offset = offsets[0];
        if (name_offset < m_fileData.size()) {
            for (uint32_t j = name_offset;
                 j < m_fileData.size() && m_fileData[j] != 0;
                 j++) {
                prop_name.push_back(static_cast<char>(m_fileData[j]));
            }
        }

        // 尝试读取属性值
        std::string prop_value;
        uint32_t value_offset = offsets[1];
        if (value_offset < m_fileData.size()) {
            for (uint32_t j = value_offset;
                 j < m_fileData.size() && m_fileData[j] != 0;
                 j++) {
                prop_value.push_back(static_cast<char>(m_fileData[j]));
            }
        }

        if (!prop_name.empty() && prop_name.find("ro.") == 0) {
            m_properties[prop_name] = prop_value;
            foundCount++;
            if (foundCount <= 10) {
                LOGD("TOC找到属性: %s = %s\n", prop_name.c_str(), prop_value.c_str());
            }
        }
    }

    LOGD("TOC表解析完成，找到 %d 个属性\n", foundCount);
    return foundCount > 0;
}

const std::map<std::string, std::string>& PropertyParser::getAllProperties() const {
    return m_properties;
}

std::string PropertyParser::getProperty(const std::string& key) const {
    auto it = m_properties.find(key);
    if (it != m_properties.end()) {
        return it->second;
    }
    return "";
}

bool PropertyParser::isPropertyTampered(const std::string& key, const std::string& currentValue) const {
    auto it = m_properties.find(key);
    if (it == m_properties.end()) {
        // 文件中没有这个属性
        return true;
    }

    std::string originalValue = it->second;
    return (originalValue != currentValue);
}

std::string PropertyParser::calculateSHA256(const uint8_t* data, size_t length) const {
    // 简化实现：使用简单的哈希算法（FNV-1a）作为替代
    // 如果需要真正的 SHA256，可以使用 Android 的 libcrypto 或其他库
    uint64_t hash = 14695981039346656037ULL; // FNV offset basis
    
    for (size_t i = 0; i < length; i++) {
        hash ^= static_cast<uint64_t>(data[i]);
        hash *= 1099511628211ULL; // FNV prime
    }
    
    // 转换为十六进制字符串
    std::stringstream ss;
    ss << std::hex << hash;
    return ss.str();
}

std::string PropertyParser::getFileHash() const {
    if (m_fileData.empty()) {
        return "";
    }
    return calculateSHA256(m_fileData.data(), m_fileData.size());
}

const std::vector<uint8_t>& PropertyParser::getRawData() const {
    return m_fileData;
}

void PropertyParser::dumpProperties() const {
    LOGD("===== 设备属性列表 =====\n");
    for (const auto& pair : m_properties) {
        if (pair.first.find("ro.") == 0) {
            LOGD("%s = %s\n", pair.first.c_str(), pair.second.c_str());
        }
    }
    LOGD("=======================\n");
}

std::string PropertyParser::getDeviceModel() const {
    return getProperty("ro.product.model");
}

std::string PropertyParser::getDeviceBrand() const {
    return getProperty("ro.product.brand");
}

std::string PropertyParser::getBuildFingerprint() const {
    return getProperty("ro.build.fingerprint");
}

std::string PropertyParser::getAndroidVersion() const {
    return getProperty("ro.build.version.release");
}

std::string PropertyParser::getBuildID() const {
    return getProperty("ro.build.id");
}

bool PropertyParser::checkForTampering() const {
    // 检查关键属性是否一致
    std::vector<std::string> criticalProps = {
            "ro.build.fingerprint",
            "ro.build.tags",
            "ro.build.type",
            "ro.product.model",
            "ro.product.brand",
            "ro.build.display.id",
            "ro.build.id",
            "ro.build.version.incremental"
    };

    for (const auto& prop : criticalProps) {
        if (m_properties.find(prop) == m_properties.end()) {
            LOGD("关键属性缺失: %s\n", prop.c_str());
            return true;
        }

        std::string value = m_properties.at(prop);
        if (value.empty() || value == "unknown") {
            LOGD("关键属性值异常: %s = %s\n", prop.c_str(), value.c_str());
            return true;
        }
    }

    // 检查构建时间是否合理
    std::string buildDate = getProperty("ro.build.date");
    if (!buildDate.empty()) {
        // 简单的合理性检查：构建时间不应该太老或太新
        // 这里可以根据实际需求添加更复杂的检查
    }

    return false;
}