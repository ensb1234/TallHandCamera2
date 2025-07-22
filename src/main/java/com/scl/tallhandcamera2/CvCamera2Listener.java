package com.scl.tallhandcamera2;

import android.content.Intent;

import com.scl.tallhandcamera2.grader.ChoicesGrader;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

/**
 * @author 作者：涉川良 联系方式：QQ470707134
 * @date 2025/4/9
 * @Description
 */
public class CvCamera2Listener implements CameraBridgeViewBase.CvCameraViewListener2 {
    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }
    //todo...从这里开始。

    public boolean pausing = false;
    private final ChoicesGrader choicesGrader;
    public CvCamera2Listener(Camera2Activity ca){
        choicesGrader = new ChoicesGrader(ca);
    }

    public void setIntent(Intent intent){
        choicesGrader.init(intent);
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //var frame = (CvCamera2View.JavaCamera2Frame) ((CameraBridgeViewBase.RotatedCameraFrame) inputFrame).mFrame;
        if(pausing){
            return inputFrame.rgba();
        }
        Mat rgba = choicesGrader.grade(inputFrame);
        return rgba;
    }

}
