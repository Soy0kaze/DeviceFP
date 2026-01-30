package com.kaze.devicefp.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class FileHelper {
    public static String readFileAsString(String filePath) {
        File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {
            System.err.println("文件不存在或不是文件: " + filePath);
            return null;
        }

        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
                content.append(System.lineSeparator()); // 保持换行符
            }
            return content.toString();
        } catch (IOException e) {
            System.err.println("读取文件失败: " + filePath);
            e.printStackTrace();
            return null;
        }
    }
}
