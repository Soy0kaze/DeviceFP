package com.kaze.devicefp.model;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SIM卡工具类
 * 功能：判断SIM卡状态、获取国家代码、获取MCC/MNC等
 */
public class SimCardUtil {

    // 日志标签
    private static final String TAG = "SimCardUtil";

    // SIM卡存在状态常量
    public static final String SIM_EXIST = "exist";
    public static final String SIM_NOT_EXIST = "noexist";
    public static final String SIM_UNKNOWN = "unknown";

    // 缓存配置
    private static final String CACHE_KEY_COUNTRY = "getSimCountryIso_";
    private static final String CACHE_KEY_SIM_STATE = "getSimState_";
    private static final long DEFAULT_CACHE_DURATION = 60 * 1000; // 60秒缓存

    // 是否启用缓存
    private static boolean sCacheEnabled = true;
    private static final ConcurrentHashMap<String, CachedValue> sCache = new ConcurrentHashMap<>();

    /**
     * 缓存值对象
     */
    private static class CachedValue {
        private final Object value;
        private final long expireTime;

        CachedValue(Object value, long cacheDuration) {
            this.value = value;
            this.expireTime = System.currentTimeMillis() + cacheDuration;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }

        Object getValue() {
            return value;
        }
    }

    /**
     * 判断SIM卡是否存在
     * 原方法逻辑：simState != 0 && simState != 1 则存在
     * SIM状态说明：
     * 0 = SIM_STATE_UNKNOWN (未知状态)
     * 1 = SIM_STATE_ABSENT (无SIM卡)
     * 2 = SIM_STATE_PIN_REQUIRED (需要PIN)
     * 3 = SIM_STATE_PUK_REQUIRED (需要PUK)
     * 4 = SIM_STATE_NETWORK_LOCKED (网络锁)
     * 5 = SIM_STATE_READY (就绪)
     * 6 = SIM_STATE_NOT_READY (未就绪)
     * 7 = SIM_STATE_PERM_DISABLED (永久禁用)
     * 8 = SIM_STATE_CARD_IO_ERROR (卡IO错误)
     * 9 = SIM_STATE_CARD_RESTRICTED (卡受限)
     *
     * @param context 上下文
     * @return "exist": SIM卡存在, "noexist": SIM卡不存在, "unknown": 未知状态
     */
    public static String checkSimExist(Context context) {
        if (context == null) {
            Log.w(TAG, "Context is null, cannot check SIM existence");
            return SIM_UNKNOWN;
        }

        String cacheKey = CACHE_KEY_SIM_STATE;

        // 如果缓存启用，尝试从缓存获取
        if (sCacheEnabled) {
            CachedValue cached = sCache.get(cacheKey);
            if (cached != null && !cached.isExpired()) {
                return (String) cached.getValue();
            }
        }

        String result = SIM_UNKNOWN;
        try {
            TelephonyManager telephonyManager =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            if (telephonyManager != null) {
                int simState = telephonyManager.getSimState();
                Log.d(TAG, "SIM state: " + simState);

                // 原逻辑：simState != 0 && simState != 1 则存在
                if (simState != TelephonyManager.SIM_STATE_UNKNOWN &&
                        simState != TelephonyManager.SIM_STATE_ABSENT) {
                    result = SIM_EXIST;
                } else {
                    result = SIM_NOT_EXIST;
                }
            } else {
                result = SIM_UNKNOWN;
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied: READ_PHONE_STATE", e);
            result = SIM_UNKNOWN;
        } catch (Exception e) {
            Log.e(TAG, "Error checking SIM existence", e);
            result = SIM_UNKNOWN;
        }

        // 存入缓存
        if (sCacheEnabled) {
            sCache.put(cacheKey, new CachedValue(result, DEFAULT_CACHE_DURATION));
        }

        return result;
    }

    /**
     * 获取详细的SIM卡状态
     * @param context 上下文
     * @return SIM状态描述字符串
     */
    public static String getSimStateDescription(Context context) {
        if (context == null) {
            return "UNKNOWN";
        }

        try {
            TelephonyManager telephonyManager =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            if (telephonyManager != null) {
                int simState = telephonyManager.getSimState();

                switch (simState) {
                    case TelephonyManager.SIM_STATE_UNKNOWN:
                        return "SIM_STATE_UNKNOWN";
                    case TelephonyManager.SIM_STATE_ABSENT:
                        return "SIM_STATE_ABSENT";
                    case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                        return "SIM_STATE_PIN_REQUIRED";
                    case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                        return "SIM_STATE_PUK_REQUIRED";
                    case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                        return "SIM_STATE_NETWORK_LOCKED";
                    case TelephonyManager.SIM_STATE_READY:
                        return "SIM_STATE_READY";
                    case TelephonyManager.SIM_STATE_NOT_READY:
                        return "SIM_STATE_NOT_READY";
                    case TelephonyManager.SIM_STATE_PERM_DISABLED:
                        return "SIM_STATE_PERM_DISABLED";
                    case TelephonyManager.SIM_STATE_CARD_IO_ERROR:
                        return "SIM_STATE_CARD_IO_ERROR";
                    case TelephonyManager.SIM_STATE_CARD_RESTRICTED:
                        return "SIM_STATE_CARD_RESTRICTED";
                    default:
                        return "SIM_STATE_UNKNOWN_" + simState;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting SIM state description", e);
        }

        return "UNKNOWN";
    }

    /**
     * 获取SIM卡国家代码（带缓存）
     * @param context 上下文
     * @return ISO国家代码（如"CN", "US"），获取失败返回空字符串
     */
    public static String getCachedSimCountryIso(Context context) {
        if (context == null) {
            return "";
        }

        // 如果不启用缓存，直接获取
        if (!sCacheEnabled) {
            return getSimCountryIsoDirectly(context);
        }

        String cacheKey = CACHE_KEY_COUNTRY;
        CachedValue cached = sCache.get(cacheKey);

        // 检查缓存是否存在且未过期
        if (cached != null && !cached.isExpired()) {
            return (String) cached.getValue();
        }

        // 缓存未命中或已过期，重新获取
        String countryCode = getSimCountryIsoDirectly(context);

        // 存入缓存（即使是空值也缓存，避免频繁调用）
        sCache.put(cacheKey, new CachedValue(countryCode, DEFAULT_CACHE_DURATION));

        return countryCode;
    }

    /**
     * 直接获取SIM卡国家代码（无缓存）
     */
    private static String getSimCountryIsoDirectly(Context context) {
        try {
            TelephonyManager telephonyManager =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            if (telephonyManager != null) {
                String countryCode = telephonyManager.getSimCountryIso();
                return countryCode != null ? countryCode.toUpperCase(Locale.US) : "";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting SIM country code", e);
        }
        return "";
    }

    /**
     * 获取SIM卡运营商代码（MCC+MNC）
     * MCC: Mobile Country Code (移动国家代码, 3位)
     * MNC: Mobile Network Code (移动网络代码, 2-3位)
     *
     * @param context 上下文
     * @return 运营商代码字符串（如"46000"），获取失败返回空字符串
     */
    public static String getSimOperator(Context context) {
        if (context == null) {
            return "";
        }

        try {
            TelephonyManager telephonyManager =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            if (telephonyManager != null) {
                String operator = telephonyManager.getSimOperator();
                return operator != null ? operator : "";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting SIM operator", e);
        }
        return "";
    }

    /**
     * 获取网络运营商代码（MCC+MNC）
     * @param context 上下文
     * @return 网络运营商代码，获取失败返回空字符串
     */
    public static String getNetworkOperator(Context context) {
        if (context == null) {
            return "";
        }

        try {
            TelephonyManager telephonyManager =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            if (telephonyManager != null) {
                String operator = telephonyManager.getNetworkOperator();
                return operator != null ? operator : "";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting network operator", e);
        }
        return "";
    }

    /**
     * 获取SIM卡MCC（移动国家代码）
     * @param context 上下文
     * @return MCC字符串（如"460"），获取失败返回空字符串
     */
    public static String getSimMcc(Context context) {
        String operator = getSimOperator(context);
        if (!TextUtils.isEmpty(operator) && operator.length() >= 3) {
            return operator.substring(0, 3);
        }
        return "";
    }

    /**
     * 获取SIM卡MNC（移动网络代码）
     * @param context 上下文
     * @return MNC字符串（如"00"），获取失败返回空字符串
     */
    public static String getSimMnc(Context context) {
        String operator = getSimOperator(context);
        if (!TextUtils.isEmpty(operator) && operator.length() > 3) {
            return operator.substring(3);
        }
        return "";
    }

    /**
     * 获取网络MCC
     * @param context 上下文
     * @return MCC字符串，获取失败返回空字符串
     */
    public static String getNetworkMcc(Context context) {
        String operator = getNetworkOperator(context);
        if (!TextUtils.isEmpty(operator) && operator.length() >= 3) {
            return operator.substring(0, 3);
        }
        return "";
    }

    /**
     * 获取网络MNC
     * @param context 上下文
     * @return MNC字符串，获取失败返回空字符串
     */
    public static String getNetworkMnc(Context context) {
        String operator = getNetworkOperator(context);
        if (!TextUtils.isEmpty(operator) && operator.length() > 3) {
            return operator.substring(3);
        }
        return "";
    }

    /**
     * 根据MCC获取国家信息
     * @param mcc 移动国家代码
     * @return 国家名称，未知返回空字符串
     */
    public static String getCountryByMcc(String mcc) {
        if (TextUtils.isEmpty(mcc)) {
            return "";
        }

        switch (mcc) {
            case "460": // 中国
                return "CN";
            case "310": // 美国
            case "311":
            case "312":
            case "313":
            case "314":
            case "315":
            case "316":
                return "US";
            case "440": // 日本
            case "441":
                return "JP";
            case "450": // 韩国
                return "KR";
            case "262": // 德国
                return "DE";
            case "208": // 法国
                return "FR";
            case "234": // 英国
                return "GB";
            case "255": // 乌克兰
                return "UA";
            case "250": // 俄罗斯
                return "RU";
            case "404": // 印度
            case "405":
            case "406":
                return "IN";
            default:
                return "";
        }
    }

    /**
     * 获取运营商名称
     * @param context 上下文
     * @return 运营商名称（如"中国移动"），获取失败返回空字符串
     */
    public static String getSimOperatorName(Context context) {
        if (context == null) {
            return "";
        }

        try {
            TelephonyManager telephonyManager =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            if (telephonyManager != null) {
                String operatorName = telephonyManager.getSimOperatorName();
                return operatorName != null ? operatorName.trim() : "";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting SIM operator name", e);
        }
        return "";
    }

    /**
     * 获取完整的SIM卡信息
     * @param context 上下文
     * @return SIM卡信息对象
     */
    public static SimInfo getSimInfo(Context context) {
        SimInfo info = new SimInfo();

        if (context == null) {
            return info;
        }

        info.simExist = checkSimExist(context);
        info.simState = getSimStateDescription(context);
        info.countryCode = getCachedSimCountryIso(context);
        info.simOperator = getSimOperator(context);
        info.simMcc = getSimMcc(context);
        info.simMnc = getSimMnc(context);
        info.networkOperator = getNetworkOperator(context);
        info.networkMcc = getNetworkMcc(context);
        info.networkMnc = getNetworkMnc(context);
        info.operatorName = getSimOperatorName(context);

        // 如果从MCC能获取到国家代码，且当前国家代码为空，则使用MCC推断
        if (TextUtils.isEmpty(info.countryCode) && !TextUtils.isEmpty(info.simMcc)) {
            info.countryCode = getCountryByMcc(info.simMcc);
        }

        return info;
    }

    /**
     * SIM卡信息对象
     */
    public static class SimInfo {
        public String simExist = SIM_UNKNOWN;
        public String simState = "UNKNOWN";
        public String countryCode = "";
        public String simOperator = "";
        public String simMcc = "";
        public String simMnc = "";
        public String networkOperator = "";
        public String networkMcc = "";
        public String networkMnc = "";
        public String operatorName = "";

        @Override
        public String toString() {
            return String.format(Locale.US,
                    "SimInfo{" +
                            "simExist='%s', " +
                            "simState='%s', " +
                            "countryCode='%s', " +
                            "simOperator='%s', " +
                            "simMcc='%s', " +
                            "simMnc='%s', " +
                            "networkOperator='%s', " +
                            "networkMcc='%s', " +
                            "networkMnc='%s', " +
                            "operatorName='%s'" +
                            "}",
                    simExist, simState, countryCode, simOperator, simMcc, simMnc,
                    networkOperator, networkMcc, networkMnc, operatorName);
        }
    }

    /**
     * 清空所有缓存
     */
    public static void clearCache() {
        sCache.clear();
        Log.d(TAG, "Cache cleared");
    }

    /**
     * 设置缓存开关
     * @param enabled true启用缓存，false禁用缓存
     */
    public static void setCacheEnabled(boolean enabled) {
        sCacheEnabled = enabled;
        if (!enabled) {
            clearCache();
        }
        Log.d(TAG, "Cache enabled: " + enabled);
    }

    /**
     * 判断当前是否在中国大陆
     * @param context 上下文
     * @return true:在中国大陆，false:不在
     */
    public static boolean isInMainlandChina(Context context) {
        String countryCode = getCachedSimCountryIso(context);
        String simMcc = getSimMcc(context);

        // 通过国家代码判断
        if ("CN".equalsIgnoreCase(countryCode)) {
            return true;
        }

        // 通过MCC判断（中国大陆MCC: 460）
        if ("460".equals(simMcc)) {
            return true;
        }

        return false;
    }

    /**
     * 获取运营商类型（中国）
     * @param context 上下文
     * @return 1:中国移动, 2:中国联通, 3:中国电信, 0:未知
     */
    public static int getChinaOperatorType(Context context) {
        String mnc = getSimMnc(context);
        if (TextUtils.isEmpty(mnc)) {
            mnc = getNetworkMnc(context);
        }

        if (TextUtils.isEmpty(mnc)) {
            return 0;
        }

        // 中国移动: 00, 02, 04, 07, 08
        if (mnc.equals("00") || mnc.equals("02") || mnc.equals("04") ||
                mnc.equals("07") || mnc.equals("08")) {
            return 1;
        }

        // 中国联通: 01, 06, 09
        if (mnc.equals("01") || mnc.equals("06") || mnc.equals("09")) {
            return 2;
        }

        // 中国电信: 03, 05, 11
        if (mnc.equals("03") || mnc.equals("05") || mnc.equals("11")) {
            return 3;
        }

        return 0;
    }

    /**
     * 获取运营商名称（中国）
     * @param context 上下文
     * @return 运营商名称
     */
    public static String getChinaOperatorName(Context context) {
        int type = getChinaOperatorType(context);
        switch (type) {
            case 1:
                return "中国移动";
            case 2:
                return "中国联通";
            case 3:
                return "中国电信";
            default:
                return "未知运营商";
        }
    }
}

