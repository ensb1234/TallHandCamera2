package com.scl.tallhandcamera2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;

import org.opencv.BuildConfig;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * This class is an implementation of the Bridge View between OpenCV and Java Camera.
 * This class relays on the functionality available in base class and only implements
 * required functions:
 * connectCamera - opens Java camera and sets the PreviewCallback to be delivered.
 * disconnectCamera - closes the camera and stops preview.
 * When frame is delivered via callback from Camera - it processed via OpenCV to be
 * converted to RGBA32 and then passed to the external callback for modifications if required.
 */
public class CvCameraView extends CvCameraViewBase implements PreviewCallback {

    private static final int MAGIC_TEXTURE_ID = 10;
    private static final String TAG = "CvCameraView";

    private byte mBuffer[];
    private Mat[] mFrameChain;
    private int mChainIdx = 0;
    private Thread mThread;
    private boolean mStopThread;

    protected Camera mCamera;
    protected RotatedCameraFrame[] mCameraFrame;
    private SurfaceTexture mSurfaceTexture;
    private int mPreviewFormat = ImageFormat.NV21;

    public static class JavaCameraSizeAccessor implements ListItemAccessor {

        @Override
        public int getWidth(Object obj) {
            Camera.Size size = (Camera.Size) obj;
            return size.width;
        }

        @Override
        public int getHeight(Object obj) {
            Camera.Size size = (Camera.Size) obj;
            return size.height;
        }
    }

    public CvCameraView(Context context, int cameraId) {
        super(context, cameraId);
    }

    private boolean mCameraCustomized = false;
    private int mCameraId;
    private Size mCameraSize;
    private int[] mCameraFpsRange;

    public CvCameraView(Context context, String cameraId, String cameraSize, String cameraFpsRange){
        super(context, -1);
        mCameraCustomized = true;
        mCameraId = THTools.matchInt(cameraId, "(.+)");
        preSetWidth = THTools.matchInt(cameraSize, "([0-9]+) x");
        preSetHeight = THTools.matchInt(cameraSize, "x ([0-9]+)");
        this.setMaxFrameSize(preSetWidth, preSetHeight);
        mCameraFpsRange = new int[]{
                THTools.matchInt(cameraFpsRange, "\\[([^,]+),"),
                THTools.matchInt(cameraFpsRange, ", ([^\\]]+)\\]")};
    }

    public CvCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected boolean initializeCamera(int width, int height) {
        Log.d(TAG, "Initialize java camera");
        boolean result = true;
        synchronized (this) {
            mCamera = null;
            int cameraId = -1;

            getCameraIDs();

            if (mCameraIndex == CAMERA_ID_ANY) {
                boolean connected = false;
                if(mCameraCustomized){
                    //改造
                    try {
                        mCamera = Camera.open(mCameraId);
                        connected = true;
                        cameraId = mCameraId;
                    } catch (RuntimeException err){
                        //原版代码
                        for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
                            Log.d(TAG, "Trying to open camera with new open(" + Integer.valueOf(camIdx) + ")");
                            try {
                                mCamera = Camera.open(camIdx);
                                connected = true;
                                cameraId = camIdx;
                            } catch (RuntimeException e) {
                                Log.e(TAG, "Camera #" + camIdx + "failed to open: " + e.getLocalizedMessage());
                            }
                            if (connected) break;
                        }
                    }
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    int localCameraIndex = mCameraIndex;
                    if (mCameraIndex == CAMERA_ID_BACK) {
                        Log.i(TAG, "Trying to open back camera");
                        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                        for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
                            Camera.getCameraInfo( camIdx, cameraInfo );
                            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                                localCameraIndex = camIdx;
                                break;
                            }
                        }
                    } else if (mCameraIndex == CAMERA_ID_FRONT) {
                        Log.i(TAG, "Trying to open front camera");
                        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                        for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
                            Camera.getCameraInfo( camIdx, cameraInfo );
                            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                                localCameraIndex = camIdx;
                                break;
                            }
                        }
                    }
                    if (localCameraIndex == CAMERA_ID_BACK) {
                        Log.e(TAG, "Back camera not found!");
                    } else if (localCameraIndex == CAMERA_ID_FRONT) {
                        Log.e(TAG, "Front camera not found!");
                    } else {
                        Log.d(TAG, "Trying to open camera with new open(" + Integer.valueOf(localCameraIndex) + ")");
                        try {
                            mCamera = Camera.open(localCameraIndex);
                            cameraId = localCameraIndex;
                        } catch (RuntimeException e) {
                            Log.e(TAG, "Camera #" + localCameraIndex + "failed to open: " + e.getLocalizedMessage());
                        }
                    }
                }
            }

            if (mCamera == null)
                return false;

            android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(cameraId, info);
            int frameRotation = getFrameRotation(
                    info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT,
                    info.orientation);
            /* Now set camera parameters */
            try {
                Camera.Parameters params = mCamera.getParameters();
                Log.d(TAG, "getSupportedPreviewSizes()");
                List<android.hardware.Camera.Size> sizes = params.getSupportedPreviewSizes();

                if (sizes != null) {
                    //列出支持的尺寸
                    THTools.log("相机%d支持的尺寸如下：（宽 x 高）", mCameraIndex);
                    for (android.hardware.Camera.Size size : sizes) {
                        THTools.log("%d x %d", size.width, size.height);
                    }
                    /* Select the size that fits surface considering maximum size allowed */
                    Size frameSize = calculateCameraFrameSize(sizes, new CvCameraView.JavaCameraSizeAccessor(), width, height);

                    /* Image format NV21 causes issues in the Android emulators */
                    if (Build.FINGERPRINT.startsWith("generic")
                            || Build.FINGERPRINT.startsWith("unknown")
                            || Build.MODEL.contains("google_sdk")
                            || Build.MODEL.contains("Emulator")
                            || Build.MODEL.contains("Android SDK built for x86")
                            || Build.MANUFACTURER.contains("Genymotion")
                            || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                            || "google_sdk".equals(Build.PRODUCT))
                        params.setPreviewFormat(ImageFormat.YV12);  // "generic" or "android" = android emulator
                    else
                        params.setPreviewFormat(ImageFormat.NV21);

                    mPreviewFormat = params.getPreviewFormat();

                    Log.d(TAG, "Set preview size to " + Integer.valueOf((int)frameSize.width) + "x" + Integer.valueOf((int)frameSize.height));
                    params.setPreviewSize((int)frameSize.width, (int)frameSize.height);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && !android.os.Build.MODEL.equals("GT-I9100"))
                        params.setRecordingHint(true);

                    List<String> FocusModes = params.getSupportedFocusModes();
                    if (FocusModes != null && FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                    {
                        //持续自动对焦（适合录像）
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    }

                    // 设置帧率范围
                    if(mCameraCustomized){
                        params.setPreviewFpsRange(mCameraFpsRange[0], mCameraFpsRange[1]);
                    }else{
                        if(mMaxFps > 0){
                            int[] fpsRange = findFpsRangeMax(mMaxFps * 1000);
                            if(fpsRange != null){
                                params.setPreviewFpsRange(fpsRange[0], fpsRange[1]);
                            }
                        }
                    }

                    mCamera.setParameters(params);
                    params = mCamera.getParameters();

                    int rawFrameWidth = params.getPreviewSize().width;
                    int rawFrameHeight = params.getPreviewSize().height;

                    if (frameRotation % 180 == 0) {
                        mFrameWidth = params.getPreviewSize().width;
                        mFrameHeight = params.getPreviewSize().height;
                    } else {
                        mFrameWidth = params.getPreviewSize().height;
                        mFrameHeight = params.getPreviewSize().width;
                    }

                    //发送事件
                    THEvents.getInstance().dispatchEvent(mFrameWidth, mFrameHeight);

                    if ((getLayoutParams().width == LayoutParams.MATCH_PARENT) && (getLayoutParams().height == LayoutParams.MATCH_PARENT))
                        mScale = Math.min(((float)height)/mFrameHeight, ((float)width)/mFrameWidth);
                    else
                        mScale = 0;

                    if (mFpsMeter != null) {
                        mFpsMeter.setResolution(mFrameWidth, mFrameHeight);
                    }

                    int size = mFrameWidth * mFrameHeight;
                    size  = size * ImageFormat.getBitsPerPixel(params.getPreviewFormat()) / 8;
                    mBuffer = new byte[size];

                    mCamera.addCallbackBuffer(mBuffer);
                    mCamera.setPreviewCallbackWithBuffer(this);

                    mFrameChain = new Mat[2];
                    mFrameChain[0] = new Mat(rawFrameHeight + (rawFrameHeight/2), rawFrameWidth, CvType.CV_8UC1);
                    mFrameChain[1] = new Mat(rawFrameHeight + (rawFrameHeight/2), rawFrameWidth, CvType.CV_8UC1);

                    AllocateCache();

                    mCameraFrame = new RotatedCameraFrame[2];
                    mCameraFrame[0] = new RotatedCameraFrame(new CvCameraView.JavaCameraFrame(mFrameChain[0], rawFrameWidth, rawFrameHeight), frameRotation);
                    mCameraFrame[1] = new RotatedCameraFrame(new CvCameraView.JavaCameraFrame(mFrameChain[1], rawFrameWidth, rawFrameHeight), frameRotation);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        mSurfaceTexture = new SurfaceTexture(MAGIC_TEXTURE_ID);
                        mCamera.setPreviewTexture(mSurfaceTexture);
                    } else
                        mCamera.setPreviewDisplay(null);

                    /* Finally we are ready to start the preview */
                    Log.d(TAG, "startPreview");
                    mCamera.startPreview();
                }
                else
                    result = false;
            } catch (Exception e) {
                result = false;
                e.printStackTrace();
            }
        }

        return result;
    }

    protected void releaseCamera() {
        synchronized (this) {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);

                mCamera.release();
            }
            mCamera = null;
            if (mFrameChain != null) {
                mFrameChain[0].release();
                mFrameChain[1].release();
            }
            if (mCameraFrame != null) {
                mCameraFrame[0].mFrame.release();
                mCameraFrame[0].release();
                mCameraFrame[1].mFrame.release();
                mCameraFrame[1].release();
            }
        }
    }

    private boolean mCameraFrameReady = false;

    @Override
    protected boolean connectCamera(int width, int height) {

        /* 1. We need to instantiate camera
         * 2. We need to start thread which will be getting frames
         */
        /* First step - initialize camera connection */
        Log.d(TAG, "Connecting to camera");
        if (!initializeCamera(width, height))
            return false;

        mCameraFrameReady = false;

        /* now we can start update thread */
        Log.d(TAG, "Starting processing thread");
        mStopThread = false;
        mThread = new Thread(new CvCameraView.CameraWorker());
        mThread.start();

        return true;
    }

    @Override
    protected void disconnectCamera() {
        /* 1. We need to stop thread which updating the frames
         * 2. Stop camera and release it
         */
        Log.d(TAG, "Disconnecting from camera");
        try {
            mStopThread = true;
            Log.d(TAG, "Notify thread");
            synchronized (this) {
                this.notify();
            }
            Log.d(TAG, "Waiting for thread");
            if (mThread != null)
                mThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mThread =  null;
        }

        /* Now release camera */
        releaseCamera();

        mCameraFrameReady = false;
    }

    @Override
    public void onPreviewFrame(byte[] frame, Camera arg1) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Preview Frame received. Frame size: " + frame.length);
        synchronized (this) {
            mFrameChain[mChainIdx].put(0, 0, frame);
            mCameraFrameReady = true;
            this.notify();
        }
        if (mCamera != null)
            mCamera.addCallbackBuffer(mBuffer);
    }

    private class JavaCameraFrame implements CvCameraViewFrame {
        @Override
        public Mat gray() {
            return mYuvFrameData.submat(0, mHeight, 0, mWidth);
        }

        @Override
        public Mat rgba() {
            if (mPreviewFormat == ImageFormat.NV21)
                Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2RGBA_NV21, 4);
            else if (mPreviewFormat == ImageFormat.YV12)
                Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2RGB_I420, 4);  // COLOR_YUV2RGBA_YV12 produces inverted colors
            else
                throw new IllegalArgumentException("Preview Format can be NV21 or YV12");

            return mRgba;
        }

        public JavaCameraFrame(Mat Yuv420sp, int width, int height) {
            super();
            mWidth = width;
            mHeight = height;
            mYuvFrameData = Yuv420sp;
            mRgba = new Mat();
        }

        @Override
        public void release() {
            mRgba.release();
        }

        private Mat mYuvFrameData;
        private Mat mRgba;
        private int mWidth;
        private int mHeight;
    };

    private class CameraWorker implements Runnable {

        @Override
        public void run() {
            do {
                boolean hasFrame = false;
                synchronized (CvCameraView.this) {
                    try {
                        while (!mCameraFrameReady && !mStopThread) {
                            CvCameraView.this.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (mCameraFrameReady)
                    {
                        mChainIdx = 1 - mChainIdx;
                        mCameraFrameReady = false;
                        hasFrame = true;
                    }
                }

                if (!mStopThread && hasFrame) {
                    if (!mFrameChain[1 - mChainIdx].empty())
                        deliverAndDrawFrame(mCameraFrame[1 - mChainIdx]);
                }
            } while (!mStopThread);
            Log.d(TAG, "Finish processing thread");
        }
    }

    /**
     * 检测并打印支持的帧率范围，并按要求排序
     * @return 符合条件的第一个帧率范围（最大帧率 <= maxFps）
     */
    private int[] findFpsRangeMax(int maxFps) {
        // 获取相机参数
        Camera.Parameters parameters = mCamera.getParameters();
        // 获取支持的帧率范围
        List<int[]> supportedFpsRanges = parameters.getSupportedPreviewFpsRange();
        if (supportedFpsRanges != null && !supportedFpsRanges.isEmpty()) {
            // 对帧率范围进行排序：按最大帧率降序，次要排序按最小帧率降序
            Collections.sort(supportedFpsRanges, new Comparator<int[]>() {
                @Override
                public int compare(int[] range1, int[] range2) {
                    // 按最大帧率降序排序
                    if (range1[1] != range2[1]) {
                        return Integer.compare(range2[1], range1[1]); // 降序
                    }
                    // 如果最大帧率相同，按最小帧率降序排序
                    return Integer.compare(range2[0], range1[0]); // 降序
                }
            });
            // 打印排序后的帧率范围
            THTools.log("支持的帧率范围（已排序）：");
            for (int[] range : supportedFpsRanges) {
                THTools.log(" - %d,%d", range[0], range[1]);
            }
            // 挑选出最大帧率 <= maxFps 的第一个帧率范围
            for (int[] range : supportedFpsRanges) {
                if (range[1] <= maxFps) {
                    THTools.log("符合条件的帧率范围（最大帧率 <= %d）：%d,%d", maxFps, range[0], range[1]);
                    return range;
                }
            }
            THTools.log("未找到符合条件的帧率范围（最大帧率 <= %d）！", maxFps);
        }else{
            THTools.log("无法获取支持的帧率范围！");
        }
        return null;
    }

    /**
     * 获取cameraID列表。
     */
    public void getCameraIDs() {
        // 获取摄像头数量
        int numberOfCameras = Camera.getNumberOfCameras();
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
        }
    }

    Camera.AutoFocusCallback mAutoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            // 对焦完成切换回持续对焦
            Camera.Parameters params = mCamera.getParameters();
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            mCamera.setParameters(params);
            focusing = false;
        }
    };

    @Override
    public boolean focusOnTouch(float x, float y, int width, int height) {
        if(!focusing){
            focusing = true;
        }else {
            return false;
        }
        Camera.Parameters params = mCamera.getParameters();
        if (params.getMaxNumFocusAreas() > 0) {
            List<Camera.Area> focusAreas = new ArrayList<>();
            int rectLeft = clamp((int) (((x / width) * 2000) - 1000), -1000, 1000);
            int rectTop = clamp((int) (((y / height) * 2000) - 1000), -1000, 1000);
            Rect areaRect = new Rect(rectLeft, rectTop, rectLeft + 200, rectTop + 200);
            focusAreas.add(new Camera.Area(areaRect, 1000));
            params.setFocusAreas(focusAreas);
            params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
            //改为单次对焦（原先默认是适合录像的持续自动对焦）
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            try {
                mCamera.setParameters(params);
                mCamera.autoFocus(mAutoFocusCallback);
                return true;
            } catch (Exception e) {
                return false;
            }
        } else {
            return false;
        }
    }
}

