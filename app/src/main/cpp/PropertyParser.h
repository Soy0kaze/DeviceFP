//
// Created by lfloat on 2026/1/28.
//

#ifndef DEVICEFP_PROPERTYPARSER_H
#define DEVICEFP_PROPERTYPARSER_H


#include <string>
#include <vector>
#include <map>
#include <cstdint>

// 属性类型枚举
enum class PropertyType {
    BUILD,      // build.prop
    SYSTEM,     // system properties
    DEFAULT,    // default properties
    VENDOR      // vendor properties
};

// 属性键值对
struct PropertyPair {
    std::string key;
    std::string value;
    uint32_t offset;    // 在文件中的偏移量
    uint32_t size;      // 属性大小
};

// 属性文件头结构
struct PropertyHeader {
    uint32_t magic;     // 魔数
    uint32_t version;   // 版本
    uint32_t num_slots; // 槽位数量
    uint32_t unused;
    uint32_t toc_offset; // TOC偏移
    uint32_t data_size;  // 数据大小
};

class PropertyParser {
public:
    // 构造函数
    explicit PropertyParser(PropertyType type = PropertyType::BUILD);

    // 解析属性文件
    bool parse();

    // 获取所有属性
    const std::map<std::string, std::string>& getAllProperties() const;

    // 获取特定属性
    std::string getProperty(const std::string& key) const;

    // 检查属性是否被篡改
    bool isPropertyTampered(const std::string& key, const std::string& currentValue) const;

    // 获取属性文件哈希
    std::string getFileHash() const;

    // 获取原始二进制数据（用于高级分析）
    const std::vector<uint8_t>& getRawData() const;

    // 打印所有属性
    void dumpProperties() const;

    // 获取关键设备信息
    std::string getDeviceModel() const;
    std::string getDeviceBrand() const;
    std::string getBuildFingerprint() const;
    std::string getAndroidVersion() const;
    std::string getBuildID() const;

    // 检查是否是常见篡改模式
    bool checkForTampering() const;

private:
    // 获取属性文件路径
    std::string getPropertyFilePath() const;

    // 读取文件到内存
    bool readPropertyFile();

    // 解析不同版本的属性格式
    bool parseAndroid7(size_t headerOffset = 0);
    bool parseAndroid8();
    bool parseAndroid9();
    bool parseAndroid10();
    bool parseAndroid11();

    // 解析TOC表
    bool parseTOCTable();

    // 解析属性数据
    bool parsePropertyData();
    
    // 通过字符串搜索解析（最通用的方法）
    bool parseByStringSearch();

    // 计算SHA256哈希
    std::string calculateSHA256(const uint8_t* data, size_t length) const;

private:
    PropertyType m_type;                 // 属性类型
    std::string m_filePath;              // 属性文件路径
    std::vector<uint8_t> m_fileData;     // 文件原始数据
    PropertyHeader m_header;             // 文件头
    std::map<std::string, std::string> m_properties;  // 解析出的属性
    std::map<std::string, PropertyPair> m_detailedProps; // 详细属性信息
    uint32_t m_androidVersion;           // Android版本（通过属性推断）
};

#endif //DEVICEFP_PROPERTYPARSER_H
