package com.scl.tallhandcamera2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.util.Range;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * @author 作者：涉川良 联系方式：QQ470707134
 * @date 2025/4/20
 * @Description
 */
public class CameraData {
    public String[] cameraIdEntries;
    public String[] cameraIdValues;
    public String[] cameraSizes;
    public String[] cameraFpsRanges;
    public static final String CAMERA1 = "camera1";
    public static final String CAMERA2 = "camera2";
    public CameraData(){
        // 没有参数，是camera1
        // 先获取相机id和前/后置标记
        THTools.log("新建cameraData，使用了camera1");
        // 获取摄像头数量
        int numberOfCameras = Camera.getNumberOfCameras();
        cameraIdEntries = new String[numberOfCameras];
        cameraIdValues = new String[numberOfCameras];
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            // 摄像头 ID 就是索引值 i
            THTools.log("camera id: %d", i);
            // 判断摄像头方向
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                THTools.log("上面这个是后置摄像头。");
            } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                THTools.log("上面这个是前置摄像头。");
            }
            // 试试是否可用
            boolean available = true;
            try{
                Camera camera = Camera.open(i);
                camera.release();
            } catch (RuntimeException e){
                THTools.log("ID为%d的摄像头不可用。", i);
                available = false;
            }
            cameraIdEntries[i] = cameraInfoToString(i, info.facing, available);
            cameraIdValues[i] = i + "";
        }
    }

    public CameraData(Context context){
        // 有参数，是camera2
        // 先获取相机id和前/后置标记
        THTools.log("新建cameraData，使用了camera2");
        // 获取 CameraManager 实例
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            // 获取摄像头 ID 列表
            String[] cameraIdList = cameraManager.getCameraIdList();
            cameraIdEntries = new String[cameraIdList.length];
            cameraIdValues = new String[cameraIdList.length];
            for (int i = 0; i < cameraIdList.length; i++) {
                THTools.log("Camera ID: %s", cameraIdList[i]);
                // 获取摄像头特性
                CameraCharacteristics characteristics;
                boolean available = true;
                Integer lensFacing = null;
                try {
                    characteristics = cameraManager.getCameraCharacteristics(cameraIdList[i]);
                    // 判断摄像头方向
                    lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (lensFacing != null) {
                        if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                            THTools.log("上面这个是后置摄像头。");
                        } else if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                            THTools.log("上面这个是前置摄像头。");
                        }
                    }
                    //尝试打开摄像头
                    cameraManager.openCamera(cameraIdList[i], new CameraDevice.StateCallback() {
                        @Override
                        public void onOpened(@NonNull CameraDevice camera) {
                            camera.close();
                        }
                        @Override
                        public void onDisconnected(@NonNull CameraDevice camera) {
                            camera.close();
                        }
                        @Override
                        public void onError(@NonNull CameraDevice camera, int error) {
                            camera.close();
                        }
                    }, null);
                }catch (IllegalArgumentException | CameraAccessException | SecurityException e){
                    //IllegalArgumentException：指定摄像头id不对，比如不在列表内
                    //CameraAccessException：设备策略禁用指定摄像头、正在被更高优先级的相机api客户端使用指定摄像头、已达到最大资源无法打开指定摄像头
                    //SecurityException：没有访问指定摄像头的权限
                    available = false;
                }
                cameraIdEntries[i] = cameraInfoToString(cameraIdList[i], lensFacing, available);
                cameraIdValues[i] = cameraIdList[i];
            }
        } catch (CameraAccessException e) {
            THTools.log(e.toString());
        }
    }

    public static final String FRONT = "前置";
    public static final String BACK = "后置";
    public static final String UNKNOWN = "未知";
    public static final String AVAILABLE = "可用";
    public static final String UNAVAILABLE = "不可用";

    @SuppressLint("DefaultLocale")
    private String cameraInfoToString(int cameraID, int cameraFacing, boolean available){
        //camera1
        if(cameraFacing == 0){
            return String.format("#%d,%s,%s", cameraID, BACK, available?AVAILABLE:UNAVAILABLE);
        }else if(cameraFacing == 1){
            return String.format("#%d,%s,%s", cameraID, FRONT, available?AVAILABLE:UNAVAILABLE);
        }else{
            return String.format("#%d,%s,%s", cameraID, UNKNOWN, available?AVAILABLE:UNAVAILABLE);
        }
    }

    private String cameraInfoToString(String cameraID, Integer cameraFacing, boolean available){
        //camera2
        if(cameraFacing == 0){
            return String.format("#%s,%s,%s", cameraID, FRONT, available?AVAILABLE:UNAVAILABLE);
        }else if(cameraFacing == 1){
            return String.format("#%s,%s,%s", cameraID, BACK, available?AVAILABLE:UNAVAILABLE);
        }else{
            return String.format("#%s,%s,%s", cameraID, UNKNOWN, available?AVAILABLE:UNAVAILABLE);
        }
    }

    @SuppressLint("DefaultLocale")
    public boolean getCameraSizesAndFpsRanges(int cameraID){
        //camera1
        try{
            Camera camera = Camera.open(cameraID);
            Camera.Parameters parameters = camera.getParameters();
            //获取支持的尺寸列表
            List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
            if(sizes != null){
                THTools.log("相机%d支持的尺寸如下：（宽 x 高）", cameraID);
                cameraSizes = new String[sizes.size()];
                for (int i = 0; i < sizes.size(); i++){
                    cameraSizes[i] = sizes.get(i).width + " x " + sizes.get(i).height;
                    THTools.log(cameraSizes[i]);
                }
            }
            //获取支持的帧率范围
            List<int[]> supportedFpsRanges = parameters.getSupportedPreviewFpsRange();
            if (supportedFpsRanges != null && !supportedFpsRanges.isEmpty()) {
                THTools.log("支持的帧率范围：");
                cameraFpsRanges = new String[supportedFpsRanges.size()];
                for(int i = 0; i < supportedFpsRanges.size(); i++){
                    cameraFpsRanges[i] = String.format("[%d, %d]", supportedFpsRanges.get(i)[0], supportedFpsRanges.get(i)[1]);
                    THTools.log(cameraFpsRanges[i]);
                }
            }
            camera.release();
            return true;
        }catch (Exception e){
            THTools.log(e.toString());
            return false;
        }
    }

    @SuppressLint("DefaultLocale")
    public boolean getCameraSizesAndFpsRanges(Context context, String cameraID){
        //camera2
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraID);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            //获取支持的尺寸列表
            android.util.Size[] sizes = map.getOutputSizes(ImageReader.class);
            THTools.log("相机%s支持的尺寸如下：（宽 x 高）", cameraID);
            cameraSizes = new String[sizes.length];
            for (int i = 0; i < sizes.length; i++){
                cameraSizes[i] = sizes[i].getWidth() + " x " + sizes[i].getHeight();
                THTools.log(cameraSizes[i]);
            }
            //获取支持的帧率范围
            Range<Integer>[] fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            if (fpsRanges == null || fpsRanges.length == 0) {
                THTools.log("无法获取支持的帧率范围！");
                return false;
            }
            THTools.log("支持的帧率范围：");
            cameraFpsRanges = new String[fpsRanges.length];
            for(int i = 0; i < fpsRanges.length; i++){
                //cameraFpsRanges[i] = fpsRanges[i].toString();
                cameraFpsRanges[i] = String.format("[%d, %d]", fpsRanges[i].getLower(), fpsRanges[i].getUpper());
                THTools.log(cameraFpsRanges[i]);
            }
            return true;
        } catch (Exception e) {
            THTools.log(e.toString());
            return false;
        }
    }

}
