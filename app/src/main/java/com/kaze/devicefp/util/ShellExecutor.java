package com.kaze.devicefp.util;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Android 普通权限命令执行工具类
 * 无需 root 权限
 */
public class ShellExecutor {

    private static final String TAG = "ShellExecutor";

    /**
     * 执行命令并返回结果
     * @param command 要执行的命令
     * @return 执行结果，如果失败返回 null 或错误信息
     */
    public static String execute(String command) {
        return executeCommand(command);
    }

    /**
     * 执行命令并返回结果（完整版）
     * @param command 要执行的命令
     * @return 执行结果字符串
     */
    private static String executeCommand(String command) {
        Process process = null;
        StringBuilder output = new StringBuilder();

        try {
            // 创建进程
            process = Runtime.getRuntime().exec(command);

            // 读取标准输出
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // 读取错误输出
            InputStream errorStream = process.getErrorStream();
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));

            StringBuilder errorOutput = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }

            // 等待进程结束
            int exitCode = process.waitFor();

            // 如果有错误输出，添加到结果中
            if (errorOutput.length() > 0) {
                output.append("\n[Error Output]\n").append(errorOutput.toString());
            }

            // 添加退出码
            output.insert(0, "Exit Code: " + exitCode + "\n");

            Log.d(TAG, "Command executed: " + command);

        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "Failed to execute command: " + command, e);
            return "Error: " + e.getMessage();
        } finally {
            // 清理资源
            if (process != null) {
                process.destroy();
            }
        }

        return output.toString();
    }

    /**
     * 执行命令并只返回成功输出（忽略错误信息）
     * @param command 要执行的命令
     * @return 标准输出内容
     */
    public static String executeSuccessOnly(String command) {
        Process process = null;
        StringBuilder output = new StringBuilder();

        try {
            process = Runtime.getRuntime().exec(command);

            // 读取标准输出
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // 忽略错误输出
            process.waitFor();

            // 移除最后一个换行符
            if (output.length() > 0) {
                output.setLength(output.length() - 1);
            }

        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "Failed to execute command: " + command, e);
            return "";
        } finally {
            if (process != null) {
                process.destroy();
            }
        }

        return output.toString();
    }

    /**
     * 执行命令并返回第一行结果
     * @param command 要执行的命令
     * @return 第一行输出，如果失败返回空字符串
     */
    public static String executeGetFirstLine(String command) {
        String result = executeSuccessOnly(command);
        if (result != null && result.contains("\n")) {
            return result.split("\n")[0];
        }
        return result;
    }

    /**
     * 执行多条命令
     * @param commands 命令数组
     * @return 每条命令的结果数组
     */
    public static String[] executeMultiple(String[] commands) {
        String[] results = new String[commands.length];

        for (int i = 0; i < commands.length; i++) {
            results[i] = execute(commands[i]);
        }

        return results;
    }

    /**
     * 检查命令是否存在并可用
     * @param command 要检查的命令
     * @return true 如果命令可用
     */
    public static boolean isCommandAvailable(String command) {
        try {
            Process process = Runtime.getRuntime().exec("which " + command);
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 异步执行命令（回调方式）
     * @param command 要执行的命令
     * @param callback 结果回调
     */
    public static void executeAsync(final String command, final ExecuteCallback callback) {
        new Thread(() -> {
            try {
                String result = execute(command);
                if (callback != null) {
                    callback.onResult(result);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        }).start();
    }

    /**
     * 执行结果回调接口
     */
    public interface ExecuteCallback {
        void onResult(String result);
        void onError(Exception e);
    }
}
