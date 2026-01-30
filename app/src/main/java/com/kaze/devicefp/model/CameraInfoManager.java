package com.kaze.devicefp.model;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraInfoManager  {
    private static final String TAG = "CameraManagerHelper";

    public enum CameraFacing {
        FRONT,      // 前置摄像头
        BACK,       // 后置摄像头
        EXTERNAL,   // 外置摄像头
        UNKNOWN     // 未知
    }

    public enum CameraType {
        SINGLE,     // 单摄
        WIDE,       // 广角
        TELEPHOTO,  // 长焦
        ULTRA_WIDE, // 超广角
        DEPTH,      // 景深/ToF
        MACRO,      // 微距
        MONOCHROME, // 黑白
        UNKNOWN     // 未知
    }

    public static class CameraInfo {
        private String cameraId;
        private CameraFacing facing;
        private CameraType type;
        private String typeDescription; // 摄像头类型描述
        private int sensorOrientation;  // 传感器方向
        private boolean hasFlash;       // 是否有闪光灯
        private List<Size> previewSizes; // 支持的预览尺寸
        private List<Size> photoSizes;   // 支持的拍照尺寸
        private List<Size> videoSizes;   // 支持录像尺寸
        private Range<Integer>[] aeCompensationRange; // 曝光补偿范围
        private float maxDigitalZoom;   // 最大数字变焦
        private boolean supportAutoFocus; // 是否支持自动对焦
        private float physicalFocalLength; // 物理焦距
        private String hardwareLevel;   // 硬件支持级别
        private Map<String, Object> additionalInfo; // 额外信息

        public CameraInfo() {
            previewSizes = new ArrayList<>();
            photoSizes = new ArrayList<>();
            videoSizes = new ArrayList<>();
            additionalInfo = new HashMap<>();
        }

        // Getters and Setters
        public String getCameraId() { return cameraId; }
        public void setCameraId(String cameraId) { this.cameraId = cameraId; }

        public CameraFacing getFacing() { return facing; }
        public void setFacing(CameraFacing facing) { this.facing = facing; }

        public CameraType getType() { return type; }
        public void setType(CameraType type) { this.type = type; }

        public String getTypeDescription() { return typeDescription; }
        public void setTypeDescription(String typeDescription) { this.typeDescription = typeDescription; }

        public int getSensorOrientation() { return sensorOrientation; }
        public void setSensorOrientation(int sensorOrientation) { this.sensorOrientation = sensorOrientation; }

        public boolean hasFlash() { return hasFlash; }
        public void setHasFlash(boolean hasFlash) { this.hasFlash = hasFlash; }

        public List<Size> getPreviewSizes() { return previewSizes; }
        public void setPreviewSizes(List<Size> previewSizes) { this.previewSizes = previewSizes; }

        public List<Size> getPhotoSizes() { return photoSizes; }
        public void setPhotoSizes(List<Size> photoSizes) { this.photoSizes = photoSizes; }

        public List<Size> getVideoSizes() { return videoSizes; }
        public void setVideoSizes(List<Size> videoSizes) { this.videoSizes = videoSizes; }

        public Range<Integer>[] getAeCompensationRange() { return aeCompensationRange; }
        public void setAeCompensationRange(Range<Integer>[] aeCompensationRange) {
            this.aeCompensationRange = aeCompensationRange;
        }

        public float getMaxDigitalZoom() { return maxDigitalZoom; }
        public void setMaxDigitalZoom(float maxDigitalZoom) { this.maxDigitalZoom = maxDigitalZoom; }

        public boolean isSupportAutoFocus() { return supportAutoFocus; }
        public void setSupportAutoFocus(boolean supportAutoFocus) { this.supportAutoFocus = supportAutoFocus; }

        public float getPhysicalFocalLength() { return physicalFocalLength; }
        public void setPhysicalFocalLength(float physicalFocalLength) { this.physicalFocalLength = physicalFocalLength; }

        public String getHardwareLevel() { return hardwareLevel; }
        public void setHardwareLevel(String hardwareLevel) { this.hardwareLevel = hardwareLevel; }

        public Map<String, Object> getAdditionalInfo() { return additionalInfo; }
        public void addAdditionalInfo(String key, Object value) { additionalInfo.put(key, value); }

        @Override
        public String toString() {
            return String.format("Camera ID: %s\n" +
                            "Facing: %s\n" +
                            "Type: %s (%s)\n" +
                            "Orientation: %d°\n" +
                            "Flash: %s\n" +
                            "Auto Focus: %s\n" +
                            "Focal Length: %.1fmm\n" +
                            "Max Digital Zoom: %.1fx\n" +
                            "Hardware Level: %s\n" +
                            "Preview Sizes: %d\n" +
                            "Photo Sizes: %d\n" +
                            "Video Sizes: %d",
                    cameraId, facing, type, typeDescription, sensorOrientation,
                    hasFlash ? "Yes" : "No",
                    supportAutoFocus ? "Yes" : "No",
                    physicalFocalLength, maxDigitalZoom, hardwareLevel,
                    previewSizes.size(), photoSizes.size(), videoSizes.size());
        }
    }

    private Context context;
    private CameraManager cameraManager;
    private List<CameraInfo> allCameraInfos;
    private Map<CameraFacing, List<CameraInfo>> camerasByFacing;
    private Map<CameraType, List<CameraInfo>> camerasByType;

    public CameraInfoManager (Context context) {
        this.context = context.getApplicationContext();
        this.allCameraInfos = new ArrayList<>();
        this.camerasByFacing = new HashMap<>();
        this.camerasByType = new HashMap<>();
        initialize();
    }

    private void initialize() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.w(TAG, "Camera2 API requires API level 21+");
            return;
        }

        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        if (cameraManager == null) {
            Log.e(TAG, "CameraManager is null");
            return;
        }

        refreshCameraInfo();
    }

    /**
     * 刷新摄像头信息
     */
    public void refreshCameraInfo() {
        allCameraInfos.clear();
        camerasByFacing.clear();
        camerasByType.clear();

        try {
            String[] cameraIdList = cameraManager.getCameraIdList();

            for (String cameraId : cameraIdList) {
                CameraInfo cameraInfo = getCameraInfo(cameraId);
                if (cameraInfo != null) {
                    allCameraInfos.add(cameraInfo);

                    // 按朝向分组
                    camerasByFacing.computeIfAbsent(cameraInfo.getFacing(), k -> new ArrayList<>())
                            .add(cameraInfo);

                    // 按类型分组
                    camerasByType.computeIfAbsent(cameraInfo.getType(), k -> new ArrayList<>())
                            .add(cameraInfo);
                }
            }

            Log.i(TAG, "Found " + allCameraInfos.size() + " cameras total");

        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access exception", e);
        } catch (SecurityException e) {
            Log.e(TAG, "No camera permission", e);
        }
    }

    /**
     * 获取指定摄像头的详细信息
     */
    private CameraInfo getCameraInfo(String cameraId) throws CameraAccessException {
        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
        CameraInfo cameraInfo = new CameraInfo();
        cameraInfo.setCameraId(cameraId);

        // 获取摄像头朝向
        Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
        cameraInfo.setFacing(convertFacing(facing));

        // 获取传感器方向
        Integer orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        cameraInfo.setSensorOrientation(orientation != null ? orientation : 0);

        // 判断是否有闪光灯
        Boolean flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        cameraInfo.setHasFlash(flashAvailable != null && flashAvailable);

        // 获取硬件支持级别
        Integer hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        cameraInfo.setHardwareLevel(convertHardwareLevel(hardwareLevel));

        // 获取自动对焦能力
        int[] afModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        cameraInfo.setSupportAutoFocus(afModes != null && afModes.length > 0);

        // 获取支持的输出格式和尺寸
        StreamConfigurationMap streamConfigMap = characteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (streamConfigMap != null) {
            // 获取支持的预览尺寸（通常使用ImageFormat.YUV_420_888格式）
            Size[] previewSizes = streamConfigMap.getOutputSizes(android.graphics.ImageFormat.YUV_420_888);
            if (previewSizes != null) {
                cameraInfo.setPreviewSizes(Arrays.asList(previewSizes));
            }

            // 获取支持的拍照尺寸（JPEG格式）
            Size[] photoSizes = streamConfigMap.getOutputSizes(android.graphics.ImageFormat.JPEG);
            if (photoSizes != null) {
                cameraInfo.setPhotoSizes(Arrays.asList(photoSizes));
            }

            // 获取支持的视频尺寸（通常使用MediaRecorder类）
            Size[] videoSizes = streamConfigMap.getOutputSizes(MediaRecorder.class);
            if (videoSizes != null) {
                cameraInfo.setVideoSizes(Arrays.asList(videoSizes));
            }
        }

        // 获取曝光补偿范围
        cameraInfo.setAeCompensationRange(
                new Range[]{characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)});

        // 获取最大数字变焦
        Float maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        cameraInfo.setMaxDigitalZoom(maxZoom != null ? maxZoom : 1.0f);

        // 获取物理焦距
        float[] focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        if (focalLengths != null && focalLengths.length > 0) {
            cameraInfo.setPhysicalFocalLength(focalLengths[0]);
        }

        // 判断摄像头类型
        determineCameraType(cameraInfo, characteristics);

        return cameraInfo;
    }

    /**
     * 判断摄像头类型（基于焦距和位置）
     */
    private void determineCameraType(CameraInfo cameraInfo, CameraCharacteristics characteristics) {
        float[] focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        if (focalLengths == null || focalLengths.length == 0) {
            cameraInfo.setType(CameraType.UNKNOWN);
            cameraInfo.setTypeDescription("Unknown");
            return;
        }

        float mainFocalLength = focalLengths[0];

        // 根据焦距范围判断摄像头类型
        if (mainFocalLength < 2.0f) {
            cameraInfo.setType(CameraType.ULTRA_WIDE);
            cameraInfo.setTypeDescription(String.format("Ultra Wide (%.1fmm)", mainFocalLength));
        } else if (mainFocalLength >= 2.0f && mainFocalLength < 3.5f) {
            cameraInfo.setType(CameraType.WIDE);
            cameraInfo.setTypeDescription(String.format("Wide (%.1fmm)", mainFocalLength));
        } else if (mainFocalLength >= 3.5f && mainFocalLength < 6.0f) {
            cameraInfo.setType(CameraType.TELEPHOTO);
            cameraInfo.setTypeDescription(String.format("Telephoto (%.1fmm)", mainFocalLength));
        } else if (mainFocalLength >= 6.0f) {
            cameraInfo.setType(CameraType.TELEPHOTO);
            cameraInfo.setTypeDescription(String.format("Super Telephoto (%.1fmm)", mainFocalLength));
        } else {
            cameraInfo.setType(CameraType.SINGLE);
            cameraInfo.setTypeDescription(String.format("Standard (%.1fmm)", mainFocalLength));
        }

        // 检查是否有深度传感器
        int[] capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
        if (capabilities != null) {
            for (int capability : capabilities) {
                if (capability == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT) {
                    cameraInfo.setType(CameraType.DEPTH);
                    cameraInfo.setTypeDescription("Depth/ToF Sensor");
                    break;
                }
            }
        }
    }

    private CameraFacing convertFacing(Integer facing) {
        if (facing == null) {
            return CameraFacing.UNKNOWN;
        }
        switch (facing) {
            case CameraCharacteristics.LENS_FACING_FRONT:
                return CameraFacing.FRONT;
            case CameraCharacteristics.LENS_FACING_BACK:
                return CameraFacing.BACK;
            case CameraCharacteristics.LENS_FACING_EXTERNAL:
                return CameraFacing.EXTERNAL;
            default:
                return CameraFacing.UNKNOWN;
        }
    }

    private String convertHardwareLevel(Integer hardwareLevel) {
        if (hardwareLevel == null) {
            return "UNKNOWN";
        }
        switch (hardwareLevel) {
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY:
                return "LEGACY";
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED:
                return "LIMITED";
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL:
                return "FULL";
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3:
                return "LEVEL_3";
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL:
                return "EXTERNAL";
            default:
                return "UNKNOWN";
        }
    }

    // 公共方法

    /**
     * 获取所有摄像头信息
     */
    public List<CameraInfo> getAllCameras() {
        return new ArrayList<>(allCameraInfos);
    }

    /**
     * 获取摄像头总数
     */
    public int getCameraCount() {
        return allCameraInfos.size();
    }

    /**
     * 获取前置摄像头数量
     */
    public int getFrontCameraCount() {
        List<CameraInfo> frontCameras = camerasByFacing.get(CameraFacing.FRONT);
        return frontCameras != null ? frontCameras.size() : 0;
    }

    /**
     * 获取后置摄像头数量
     */
    public int getBackCameraCount() {
        List<CameraInfo> backCameras = camerasByFacing.get(CameraFacing.BACK);
        return backCameras != null ? backCameras.size() : 0;
    }

    /**
     * 获取外置摄像头数量
     */
    public int getExternalCameraCount() {
        List<CameraInfo> externalCameras = camerasByFacing.get(CameraFacing.EXTERNAL);
        return externalCameras != null ? externalCameras.size() : 0;
    }

    /**
     * 获取指定朝向的摄像头列表
     */
    public List<CameraInfo> getCamerasByFacing(CameraFacing facing) {
        List<CameraInfo> result = camerasByFacing.get(facing);
        return result != null ? new ArrayList<>(result) : new ArrayList<>();
    }

    /**
     * 获取指定类型的摄像头列表
     */
    public List<CameraInfo> getCamerasByType(CameraType type) {
        List<CameraInfo> result = camerasByType.get(type);
        return result != null ? new ArrayList<>(result) : new ArrayList<>();
    }

    /**
     * 获取主摄像头（通常是最广角的后置摄像头）
     */
    public CameraInfo getMainCamera() {
        List<CameraInfo> backCameras = getCamerasByFacing(CameraFacing.BACK);
        if (backCameras.isEmpty()) {
            return null;
        }

        // 寻找焦距最短的后置摄像头作为主摄
        CameraInfo mainCamera = null;
        float minFocalLength = Float.MAX_VALUE;

        for (CameraInfo camera : backCameras) {
            if (camera.getPhysicalFocalLength() < minFocalLength) {
                minFocalLength = camera.getPhysicalFocalLength();
                mainCamera = camera;
            }
        }

        return mainCamera;
    }

    /**
     * 获取前置主摄像头
     */
    public CameraInfo getFrontMainCamera() {
        List<CameraInfo> frontCameras = getCamerasByFacing(CameraFacing.FRONT);
        if (frontCameras.isEmpty()) {
            return null;
        }
        return frontCameras.get(0);
    }

    /**
     * 获取摄像头详细信息报告
     */
    public String getCameraReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Camera Report ===\n");
        report.append("Total Cameras: ").append(getCameraCount()).append("\n");
        report.append("Front Cameras: ").append(getFrontCameraCount()).append("\n");
        report.append("Back Cameras: ").append(getBackCameraCount()).append("\n");
        report.append("External Cameras: ").append(getExternalCameraCount()).append("\n\n");

        for (CameraInfo camera : allCameraInfos) {
            report.append("Camera ").append(camera.getCameraId()).append(":\n");
            report.append(camera.toString()).append("\n\n");
        }

        return report.toString();
    }

    /**
     * 检查是否支持特定功能
     */
    public boolean isFeatureSupported(String cameraId, String feature) {
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);

            switch (feature) {
                case "RAW":
                    int[] capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                    if (capabilities != null) {
                        for (int capability : capabilities) {
                            if (capability == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) {
                                return true;
                            }
                        }
                    }
                    return false;

                case "MANUAL_EXPOSURE":
                    Range<Long> exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
                    return exposureRange != null;

                case "MANUAL_FOCUS":
                    Float minFocus = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
                    return minFocus != null && minFocus > 0;

                default:
                    return false;
            }
        } catch (CameraAccessException e) {
            return false;
        }
    }
}

