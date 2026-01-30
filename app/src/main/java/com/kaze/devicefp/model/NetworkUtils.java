package com.kaze.devicefp.model;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

public class NetworkUtils {

    // 网络类型常量
    public static final int NETWORK_TYPE_NONE = 0;
    public static final int NETWORK_TYPE_2G = 1;
    public static final int NETWORK_TYPE_3G = 2;
    public static final int NETWORK_TYPE_4G = 3;
    public static final int NETWORK_TYPE_5G = 4;
    public static final int NETWORK_TYPE_WIFI = 5;
    public static final int NETWORK_TYPE_ETHERNET = 6;
    public static final int NETWORK_TYPE_LTE_CA = 19;

    /**
     * 获取当前网络类型
     * @param context 上下文
     * @return 网络类型常量
     */
    public static int getNetworkType(Context context) {
        if (context == null) {
            return NETWORK_TYPE_NONE;
        }

        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return NETWORK_TYPE_NONE;
        }

        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isAvailable()) {
            return NETWORK_TYPE_NONE;
        }

        // 检查WIFI
        NetworkInfo wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (isNetworkConnected(wifiInfo)) {
            return NETWORK_TYPE_WIFI;
        }

        // 检查移动网络
        NetworkInfo mobileInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (isNetworkConnected(mobileInfo)) {
            return getMobileNetworkType(mobileInfo);
        }

        // 检查以太网
        NetworkInfo ethernetInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
        if (isNetworkConnected(ethernetInfo)) {
            return NETWORK_TYPE_ETHERNET;
        }

        return NETWORK_TYPE_NONE;
    }

    /**
     * 检查网络是否已连接
     */
    private static boolean isNetworkConnected(NetworkInfo networkInfo) {
        return networkInfo != null &&
                networkInfo.getState() != null &&
                networkInfo.getState() == NetworkInfo.State.CONNECTED;
    }

    /**
     * 获取移动网络类型 (2G/3G/4G/5G)
     */
    private static int getMobileNetworkType(NetworkInfo networkInfo) {
        if (networkInfo == null) {
            return NETWORK_TYPE_NONE;
        }

        switch (networkInfo.getSubtype()) {
            // 2G网络
            case TelephonyManager.NETWORK_TYPE_GPRS:    // 1
            case TelephonyManager.NETWORK_TYPE_EDGE:    // 2
            case TelephonyManager.NETWORK_TYPE_CDMA:    // 4
            case TelephonyManager.NETWORK_TYPE_1xRTT:   // 7
            case TelephonyManager.NETWORK_TYPE_IDEN:    // 11
            case TelephonyManager.NETWORK_TYPE_GSM:     // 16
                return NETWORK_TYPE_2G;

            // 3G网络
            case TelephonyManager.NETWORK_TYPE_UMTS:    // 3
            case TelephonyManager.NETWORK_TYPE_EVDO_0:  // 5
            case TelephonyManager.NETWORK_TYPE_EVDO_A:  // 6
            case TelephonyManager.NETWORK_TYPE_HSDPA:   // 8
            case TelephonyManager.NETWORK_TYPE_HSUPA:   // 9
            case TelephonyManager.NETWORK_TYPE_HSPA:    // 10
            case TelephonyManager.NETWORK_TYPE_EVDO_B:  // 12
            case TelephonyManager.NETWORK_TYPE_EHRPD:   // 14
            case TelephonyManager.NETWORK_TYPE_HSPAP:   // 15
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:// 17
                return NETWORK_TYPE_3G;

            // 4G网络
            case TelephonyManager.NETWORK_TYPE_LTE:     // 13
            case NETWORK_TYPE_LTE_CA:  // 19 (原代码中的18，实际应该是19)
                return NETWORK_TYPE_4G;

            // 5G网络
            case TelephonyManager.NETWORK_TYPE_NR:      // 20
                return NETWORK_TYPE_5G;

            default:
                return NETWORK_TYPE_NONE;
        }
    }

    /**
     * 获取网络类型名称
     */
    public static String getNetworkTypeName(int networkType) {
        switch (networkType) {
            case NETWORK_TYPE_NONE:
                return "无网络";
            case NETWORK_TYPE_2G:
                return "2G";
            case NETWORK_TYPE_3G:
                return "3G";
            case NETWORK_TYPE_4G:
                return "4G";
            case NETWORK_TYPE_5G:
                return "5G";
            case NETWORK_TYPE_WIFI:
                return "WiFi";
            case NETWORK_TYPE_ETHERNET:
                return "以太网";
            default:
                return "未知";
        }
    }
}

