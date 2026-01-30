package com.kaze.devicefp.model;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

/**
 * 网络信息实体类
 */
public class WifiInfo {
    private boolean wifiEnabled;           // Wi-Fi是否开启
    private String connectionState;        // 连接状态
    private String ipAddress;              // IP地址
    private String gateway;                // 网关
    private boolean simCard;               // 是否插sim
    private String country;                // 设备sim国家
    private String mncMcc;                 // mnc mcc

    public WifiInfo(Context context){
        this.setConnectionState(context);
        this.setWifiEnabled(context);
        this.setGateway();
        this.setIpAddress();
        this.setSimInfo(context);
    }
    public void setSimInfo(Context context){
        String simExist = SimCardUtil.checkSimExist(context);
        if (SimCardUtil.SIM_EXIST.equals(simExist)) {
            this.simCard = true;
            this.country = SimCardUtil.getCachedSimCountryIso(context);
            this.mncMcc = SimCardUtil.getSimMnc(context) + "|" +SimCardUtil.getSimMcc(context);
        } else if (SimCardUtil.SIM_NOT_EXIST.equals(simExist)) {
            this.simCard = false;
        }
    }
    public String getSimCard(){
        if (this.simCard) return "插卡";
        return "没插卡";
    }
    public String getSimCountry(){
        return this.country;
    }
    public String getMncMcc(){
        return this.mncMcc;
    }

    public static final String STATE_WIFI = "WIFI";
    public static final String STATE_4G = "4G";
    public static final String STATE_5G = "5G";
    public static final String STATE_2G = "2G";
    public static final String STATE_3G = "3G";
    public static final String STATE_ETHERNET = "ETHERNET";
    public static final String STATE_NONE = "NONE";
    public static final String STATE_UNKNOWN = "UNKNOWN";

    public void setConnectionState(Context context) {
        if (context == null) {
            this.connectionState = STATE_UNKNOWN;
            return;
        }

        int networkType = NetworkUtils.getNetworkType(context);

        switch (networkType) {
            case NetworkUtils.NETWORK_TYPE_WIFI:
                this.connectionState = STATE_WIFI;
                break;
            case NetworkUtils.NETWORK_TYPE_4G:
                this.connectionState = STATE_4G;
                break;
            case NetworkUtils.NETWORK_TYPE_5G:
                this.connectionState = STATE_5G;
                break;
            case NetworkUtils.NETWORK_TYPE_2G:
                this.connectionState = STATE_2G;
                break;
            case NetworkUtils.NETWORK_TYPE_3G:
                this.connectionState = STATE_3G;
                break;
            case NetworkUtils.NETWORK_TYPE_ETHERNET:
                this.connectionState = STATE_ETHERNET;
                break;
            case NetworkUtils.NETWORK_TYPE_NONE:
                this.connectionState = STATE_NONE;
                break;
            default:
                this.connectionState = STATE_UNKNOWN;
                break;
        }
    }

    public String getConnectionState() {
        return connectionState;
    }

    public void setWifiEnabled(Context context){
        this.wifiEnabled = isWifiConnected(context);
    }
    public boolean getWifiEnabled(){
        return this.wifiEnabled;
    }
    public void setGateway(){
        this.gateway = getDefaultGateway();
    }
    public String getGateway(){
        return this.gateway;
    }
    public void setIpAddress(){
        List<String> allIPv4Addresses = getAllIPv4Addresses();
        StringBuilder ipv4 = new StringBuilder();
        for (int i = 0; i < allIPv4Addresses.size(); i++){
            ipv4.append(allIPv4Addresses.get(i));
        }
        StringBuilder ipv6SB = new StringBuilder();
        for (String allIPv6Address : getAllIPv6Addresses()) {
            ipv6SB.append(allIPv6Address).append("\n");
        }
        this.ipAddress = ipv4 + "\n" + ipv6SB;
    }
    public String getIpInfo(){
        return this.ipAddress;
    }
    public static boolean isWifiConnected(Context context) {
        try {
            ConnectivityManager connManager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (connManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Network network = connManager.getActiveNetwork();
                    if (network != null) {
                        NetworkCapabilities capabilities =
                                connManager.getNetworkCapabilities(network);
                        return capabilities != null &&
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
                    }
                } else {
                    NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    return wifiInfo != null && wifiInfo.isConnected();
                }
            }
        } catch (Exception e) {
            Log.e("WifiChecker", "检查Wi-Fi连接失败", e);
        }
        return false;
    }
    public static List<String> getAllIPv4Addresses() {
        List<String> ipv4List = new ArrayList<>();

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                Enumeration<InetAddress> addresses = ni.getInetAddresses();

                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        ipv4List.add(addr.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        return ipv4List;
    }
    public static List<String> getAllIPv6Addresses() {
        List<String> ipv6List = new ArrayList<>();

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                Enumeration<InetAddress> addresses = ni.getInetAddresses();

                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet6Address) {
                        ipv6List.add(addr.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        return ipv6List;
    }
    public static String getDefaultIPv4() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isUp() && !ni.isLoopback()) {
                    Enumeration<InetAddress> addresses = ni.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                            return addr.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static String getDefaultIPv6() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isUp() && !ni.isLoopback()) {
                    Enumeration<InetAddress> addresses = ni.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (!addr.isLoopbackAddress() && addr instanceof Inet6Address) {
                            return addr.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }
    private static boolean isIPv6(InetAddress addr) {
        return addr.getAddress().length == 16;
    }
    private static String getIPv6Gateway() {
        try {
            List<NetworkInterface> interfaces =
                    Collections.list(NetworkInterface.getNetworkInterfaces());

            for (NetworkInterface ni : interfaces) {
                if (ni.isUp() && !ni.isLoopback()) {
                    List<InetAddress> addresses = Collections.list(ni.getInetAddresses());
                    for (InetAddress addr : addresses) {
                        if (!addr.isLoopbackAddress() && isIPv6(addr)) {
                            // 对于IPv6链路本地地址，网关通常是fe80::1
                            String ip = addr.getHostAddress();
                            if (ip.startsWith("fe80:")) {
                                return "fe80::1%" + ni.getName();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 从 /proc/net/route 读取默认 IPv4 网关（不依赖其他 gateway 方法，避免递归）
     */
    private static String getIPv4Gateway() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("/proc/net/route"));
            String line;
            // 跳过表头
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");
                // 格式: Iface Destination Gateway Flags ...
                // 默认路由的 Destination 为 00000000
                if (parts.length >= 3 && "00000000".equals(parts[1])) {
                    String gatewayHex = parts[2];
                    if (gatewayHex.length() == 8) {
                        // 小端序 hex 转 IPv4 字符串
                        int a = Integer.parseInt(gatewayHex.substring(6, 8), 16) & 0xff;
                        int b = Integer.parseInt(gatewayHex.substring(4, 6), 16) & 0xff;
                        int c = Integer.parseInt(gatewayHex.substring(2, 4), 16) & 0xff;
                        int d = Integer.parseInt(gatewayHex.substring(0, 2), 16) & 0xff;
                        return d + "." + c + "." + b + "." + a;
                    }
                }
            }
        } catch (IOException e) {
            Log.e("WifiInfo", "读取 /proc/net/route 失败", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
        return null;
    }

    public static List<String> getAllGateways() {
        List<String> gateways = new ArrayList<>();

        // 获取 IPv4 网关（直接读系统，不调用 getDefaultGateway）
        String ipv4Gateway = getIPv4Gateway();
        if (ipv4Gateway != null && !ipv4Gateway.isEmpty()) {
            gateways.add("IPv4: " + ipv4Gateway);
        }

        // 尝试获取 IPv6 网关
        String ipv6Gateway = getIPv6Gateway();
        if (ipv6Gateway != null && !ipv6Gateway.isEmpty()) {
            gateways.add("IPv6: " + ipv6Gateway);
        }

        return gateways;
    }

    public static String getDefaultGateway() {
        // 优先返回 IPv4 默认网关
        String ipv4 = getIPv4Gateway();
        if (ipv4 != null && !ipv4.isEmpty()) {
            return ipv4;
        }
        return getIPv6Gateway();
    }
    // 代理检测
    public static String netWorkName() throws SocketException {
        Enumeration obj = NetworkInterface.getNetworkInterfaces();
        Iterator iterator = Collections.list(obj).iterator();
        StringBuilder stringBuilder = new StringBuilder();
        while (iterator.hasNext()) {
            NetworkInterface networkInter = (NetworkInterface) iterator.next();
            stringBuilder.append(networkInter.getName()).append("\n");
        }
        return stringBuilder.toString();
    }
    public static String get_httpAgent(){
        return System.getProperty("http.agent").replace("=", "").replace("&", "");
    }
    public static int simCount(Context context){
        try {
            TelephonyManager telephonyManager0 = (TelephonyManager) context.getSystemService("phone");
            return telephonyManager0 == null ? -1 : telephonyManager0.getPhoneCount();
        }
        catch(Throwable unused_ex) {
            return -2;
        }
    }
    public static int getMaxActiveSubscriptionCount(Context context) {
        try {
            // 获取订阅管理器
            SubscriptionManager subscriptionManager =
                    (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);

            // 检查管理器是否为空
            if (subscriptionManager == null) {
                return -1;  // 服务不可用
            }

            // 获取最大活跃订阅数量
            return subscriptionManager.getActiveSubscriptionInfoCountMax();

        } catch (Exception e) {
            // 捕获所有异常
            return -2;  // 获取失败
        }
    }
    public static int mobileNetStatus(Context context0) {
        try {
            // 反射获取ConnectivityManager的getMobileDataEnabled方法
            Method method0 = ConnectivityManager.class.getDeclaredMethod("getMobileDataEnabled");
            method0.setAccessible(true);  // 设置为可访问（突破private限制）

            // 调用该方法并获取返回值
            return ((Boolean)method0.invoke(((ConnectivityManager)context0.getSystemService("connectivity")))).booleanValue() ? 0 : 1;
        } catch(Throwable unused_ex) {
            return -2;  // 出现异常返回-2
        }
    }

}
