package com.scl.tallhandcamera2;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import org.opencv.android.CameraBridgeViewBase;

/**
 * @author 作者：涉川良 联系方式：QQ470707134
 * @date 2025/4/11
 * @Description
 */
public class CvCameraViewBase extends CameraBridgeViewBase {
    public CvCameraViewBase(Context context, int cameraId) {
        super(context, cameraId);
    }

    public CvCameraViewBase(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected boolean connectCamera(int width, int height) {
        return false;
    }

    protected void disconnectCamera() {

    }

    //todo...

    protected int mMaxFps = 0;
    public int preSetWidth;
    public int preSetHeight;
    public boolean focusing = false;
    public void setMaxFps(int maxFps){
        mMaxFps = maxFps;
    }

    protected int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    /**
     * 自动对焦
     * @param x
     * @param y
     * @param width
     * @param height
     * @return 指示是否对焦成功。
     */
    public boolean focusOnTouch(float x, float y, int width, int height){
        return true;
    }

}
