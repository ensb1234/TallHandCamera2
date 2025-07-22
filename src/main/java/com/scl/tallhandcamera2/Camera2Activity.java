package com.scl.tallhandcamera2;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.scl.tallhandcamera2.databinding.ActivityCamera2Binding;
import com.scl.tallhandcamera2.grader.ChoicesGrader;
import com.scl.tallhandcamera2.grader.TwoRect;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * @author 作者：涉川良 联系方式：QQ470707134
 * @date 2025/4/8
 * @Description
 */
public class Camera2Activity extends AppCompatActivity implements View.OnClickListener{

    private ActivityCamera2Binding binding;
    private final Handler mHideHandler = new Handler(Looper.myLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCamera2Binding.inflate(getLayoutInflater());
        //常亮、无题、全屏、横屏。古法制作
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        //
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        binding.camera2Container.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
        THTools.log("相机任务%d已经创建。现在是%s屏状态。", getTaskId(), THTools.isLandscape(this)?"横":"竖");
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        //接下来做的事情，它们原本是用于动态控制全屏的
        //在活动创建后进行第一步隐藏，先隐藏UI
        mHideHandler.postDelayed(mHidePart1Runnable, 300);
    }

    /**
     * 第一步隐藏，先隐藏UI
     */
    private final Runnable mHidePart1Runnable = new Runnable() {
        @Override
        public void run() {
            // 先隐藏UI
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.hide();
            }
            // 第二步隐藏。一些较旧的设备在UI小部件更新、状态和导航栏的更改之间，需要一个小延迟。
            mHideHandler.postDelayed(mHidePart2Runnable, 300);
        }
    };

    /**
     * 第二步隐藏，隐藏状态栏和导航栏
     */
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // 隐藏状态栏和导航栏
            if (Build.VERSION.SDK_INT >= 30) {
                getWindow().getDecorView().getWindowInsetsController().hide(
                        WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            } else {
                // 请注意，其中一些常量是API 16 （Jelly Bean）和API 19 （KitKat）的新常量。
                // 使用它们是安全的，因为它们是在编译时内联的，在早期的设备上不做任何事情。
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            }
        }
    };

    private final ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            winContentWidth = binding.camera2Container.getWidth();
            winContentHeight = binding.camera2Container.getHeight();
            THTools.log("camera2activity宽%d高%d", winContentWidth, winContentHeight);
            // 移除监听器避免重复调用
            binding.camera2Container.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            initialize();
        }
    };

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            //
        }
    }

    //todo 活动事件 ================================================================

    @Override
    protected void onPause(){
        super.onPause();
        if(camera2View != null){
            camera2View.disableView();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (camera2View != null){
            camera2View.enableView();
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        THEvents.getInstance().removeTHEventListener(updateScoreListener);
        if(camera2View != null){
            camera2View.disableView();
            binding.getRoot().removeAllViews();
            camera2View = null;
        }
        btnFocusRot90.removeAllListeners();
        btnFocusRot0.removeAllListeners();
        binding = null;
        THTools.log("相机任务%d已经摧毁。现在是%s屏状态。", getTaskId(), THTools.isLandscape(this)?"横":"竖");
    }

    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent();
        THTools.log("相机任务%d已经复用。", getTaskId());
    }

    //todo 从这里开始 ================================================================
    public enum BtnMiddleMode {
        MANUAL_IDENTIFY,
        GET_PROTO,
        AUTO_IDENTIFY
    }
    public static final String PROTO_PNG = "proto.png";
    public static final String CAMERA2ACTIVITY_CREATED = "Camera2Activity created";
    public static final String COMMAND_EXIT = "exit";
    public static int winContentWidth;
    public static int winContentHeight;
    public static int paperWidth;
    public static int paperHeight;
    private ObjectAnimator txtTipsRot0;
    private ObjectAnimator txtTipsRot90;
    private boolean txtTipsRotated = true;
    private ConstraintLayout.LayoutParams btnFocusLP;
    private ObjectAnimator btnFocusRot0;
    private ObjectAnimator btnFocusRot90;
    public CvCameraViewBase camera2View;
    private CvCamera2Listener cvCamera2Listener;
    public boolean btnMiddlePressed = false;
    private Bitmap protoBmp;
    public int[][] protoBlackCountHashMaps;
    private THEvents.UpdateScoreListener updateScoreListener;

    private void initialize(){
//        android.graphics.Rect rect = new android.graphics.Rect();
//        getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
//        winContentWidth = rect.width();
//        winContentHeight = rect.height();
        //创建新的SurfaceView
        createSurfaceView(getIntent().getStringExtra(SettingsFragment.CAMERA_API));
        //设置动画们
        setAnimations();
        //获取对焦图标的布局参数
        btnFocusLP = (ConstraintLayout.LayoutParams) binding.btnFocus.getLayoutParams();

        binding.txtTips.setOnClickListener(this);
        binding.btnFocus.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY;
            private boolean draged = false;
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int action = event.getActionMasked();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        // 记录触摸点与控件左上角的偏移量
                        dX = event.getRawX() - view.getX();
                        dY = event.getRawY() - view.getY();
                        // 防止父容器拦截事件
                        view.getParent().requestDisallowInterceptTouchEvent(true);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        draged = true;
                        // 计算新位置
                        float newX = event.getRawX() - dX;
                        float newY = event.getRawY() - dY;
                        // 限制按钮不能移出父容器范围
                        float rightBound = winContentWidth - binding.btnFocus.getWidth();
                        float bottomBound = winContentHeight - binding.btnFocus.getHeight();
                        newX = Math.max(0, Math.min(newX, rightBound));
                        newY = Math.max(0, Math.min(newY, bottomBound));
                        // 更新按钮位置
                        view.setX(newX);
                        view.setY(newY);
                        break;
                    case MotionEvent.ACTION_UP:
                        if(!draged){
                            view.performClick();
                        }
                        // 可选：松手后做些动画或处理
                        view.getParent().requestDisallowInterceptTouchEvent(false);
                        draged = false;
                        break;
                }
                return true; // 表示事件已消费
            }
        });
        binding.btnFocus.setOnClickListener(this);
        binding.btnRight.setOnClickListener(this);
        binding.btnRight.setOnTouchListener(new THTools.OnTouchButtonListener(binding.btnRight));
        binding.btnMiddle.setOnClickListener(this);
        binding.btnMiddle.setOnTouchListener(new THTools.OnTouchButtonListener(binding.btnMiddle));
        binding.correctPaperContainer.initialize();
        binding.testerPaperContainer.initialize();
        binding.ui.setOnClickListener(this);



        updateScoreListener = new THEvents.UpdateScoreListener() {
            @Override
            public void handleEvent(THEvents.UpdateScoreEvent event) {
                int score = binding.testerPaperContainer.getTotalScores();
                binding.txtTips.setText("总分：" + score + "分");
            }
        };
        THEvents.getInstance().addTHEventListener(updateScoreListener);
    }

    public void setTips(List<HashSet<Integer>> testerAnsList){
        int score = binding.testerPaperContainer.addGrids(binding.correctPaperContainer.features, testerAnsList);
        binding.txtTips.setText("总分为：" + score + "分");
    }

    public List<int[][][]> getAnsXYs(){
        return binding.correctPaperContainer.ansXYs;
    }

    public BtnMiddleMode getBtnMiddleMode(){
        switch (binding.btnLeft.getState()){
            case "PRT":
                return BtnMiddleMode.GET_PROTO;
            case "AUT":
                return BtnMiddleMode.AUTO_IDENTIFY;
            default:
                //MAN
                return BtnMiddleMode.MANUAL_IDENTIFY;
        }
    }

    //todo 动画设置 ================================================================

    private void setAnimations(){
        //设置提示文本的旋转动画
        txtTipsRot0 = ObjectAnimator.ofFloat(binding.txtTips, "rotation", -90, 0);
        txtTipsRot0.setDuration(300);
        txtTipsRot90 = ObjectAnimator.ofFloat(binding.txtTips, "rotation", 0, -90);
        txtTipsRot90.setDuration(300);
        //设置对焦图标的对焦动画
        btnFocusRot0 = ObjectAnimator.ofFloat(binding.btnFocus, "rotation", 90, 0);
        btnFocusRot0.setDuration(500);
        btnFocusRot90 = ObjectAnimator.ofFloat(binding.btnFocus, "rotation", 0, 90);
        btnFocusRot90.setDuration(2500);
        //动画侦听器设置
        OnViewAnimationListener onViewAnimationListener = new OnViewAnimationListener();
        btnFocusRot90.addListener(onViewAnimationListener);
        btnFocusRot0.addListener(onViewAnimationListener);
    }

    private class OnViewAnimationListener extends AnimatorListenerAdapter{
        @Override
        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            if(animation == btnFocusRot90){
                btnFocusRot0.start();
            }else if(animation == btnFocusRot0){
                binding.btnFocus.setVisibility(GONE);
            }
        }
    }

    //todo 相机设置 ================================================================

    private void createSurfaceView(String cameraApi){
        ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
        );
        if(cameraApi.equals(CameraData.CAMERA1)){
            //代替品
            camera2View = new CvCameraView(this,
                    getIntent().getStringExtra(SettingsFragment.CAMERA_ID),
                    getIntent().getStringExtra(SettingsFragment.CAMERA_SIZE),
                    getIntent().getStringExtra(SettingsFragment.CAMERA_FPS_RANGE));
        } else {
            camera2View = new CvCamera2View(this,
                    getIntent().getStringExtra(SettingsFragment.CAMERA_ID),
                    getIntent().getStringExtra(SettingsFragment.CAMERA_SIZE),
                    getIntent().getStringExtra(SettingsFragment.CAMERA_FPS_RANGE));
        }
        //camera2View.setMaxFrameSize(1920, 1080);
        //camera2View.setMaxFps(20);
        binding.getRoot().addView(camera2View, 0, layoutParams);
        camera2View.setCameraPermissionGranted();
        camera2View.enableView();
        camera2View.enableFpsMeter();
        camera2View.setOnTouchListener(new View.OnTouchListener() {
            private int originX;
            private int originY;
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        originX = (int) event.getX();
                        originY = (int) event.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                        float mouseX = event.getX();
                        float mouseY = event.getY();
                        if(Math.abs(mouseX - originX) > 100 || Math.abs(mouseY - originY) > 100){
                            //滑动一定距离就触发图标显示开或关
                            THTools.switchVisible(binding.ui);
                            cvCamera2Listener.pausing = binding.ui.getVisibility() == GONE;
                        }else{
                            //否则就触发自动对焦
                            if(!camera2View.focusing){
                                showAndFocus(mouseX, mouseY);
                            }
                        }
                        break;
                }
                return true;
            }
        });
        cvCamera2Listener = new CvCamera2Listener(this);
        handleIntent();
        camera2View.setCvCameraViewListener(cvCamera2Listener);
    }

    private void showAndFocus(float x, float y){
        if(binding.btnFocus.getVisibility() == VISIBLE){
            return;
        }
        //显示对焦图标
        binding.btnFocus.setVisibility(VISIBLE);
        //将对焦图标移动到点按处
        int w = binding.btnFocus.getWidth() >>> 1;
        int h = binding.btnFocus.getHeight() >>> 1;
        btnFocusLP.leftMargin = (int) x - w;
        btnFocusLP.topMargin = (int) y - h;
        binding.btnFocus.setLayoutParams(btnFocusLP);
        //播放图标动画（动画完后，对焦图标自动消失）
        btnFocusRot90.start();
        //令相机在点按处自动对焦
        camera2View.focusOnTouch(x, y, camera2View.getWidth(), camera2View.getHeight());
    }

    //todo 点击事件集合 ================================================================

    @Override
    public void onClick(View v) {
        if(v == binding.btnMiddle){
            if(camera2View.getVisibility() == GONE){
                if(binding.correctPaperContainer.getVisibility() == VISIBLE){
                    binding.correctPaperContainer.setVisibility(GONE);
                }else if(binding.testerPaperContainer.getVisibility() == VISIBLE){
                    binding.testerPaperContainer.setVisibility(GONE);
                }
                camera2View.setVisibility(VISIBLE);
            }else {
                btnMiddlePressed = true;
            }
        }else if(v == binding.btnRight){
            if(camera2View.getVisibility() == GONE){
                //点击切换以下选项：只显示选中的标准答案、显示全部标准答案
            }else{
                //回到主活动并显示设置页面
                Intent intent = new Intent();
                intent.setClass(Camera2Activity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent.putExtra(CAMERA2ACTIVITY_CREATED, true);
                startActivity(intent);
            }
        }else if(v == binding.btnFocus){
            if(binding.testerPaperContainer.getVisibility() == VISIBLE || binding.correctPaperContainer.getVisibility() == VISIBLE){
                //在显示图片时应该在点击后显示ui
                binding.ui.setVisibility(VISIBLE);
                binding.btnFocus.setVisibility(GONE);
            }else{
                //否则应该打开/关闭手电筒 todo...
            }
        }else if(v == binding.txtTips){
            //给提示文本框添加点按旋转功能
            if(txtTipsRotated){
                txtTipsRot0.start();
            }else{
                txtTipsRot90.start();
            }
            txtTipsRotated = !txtTipsRotated;
        }else if(v == binding.ui){
            if(binding.testerPaperContainer.getVisibility() == VISIBLE || binding.correctPaperContainer.getVisibility() == VISIBLE){
                binding.ui.setVisibility(GONE);
                binding.btnFocus.setVisibility(VISIBLE);
            }else{
                showCorrectPaper();
            }
        }
    }

    //todo 功能函数 ================================================================

    /**
     * 处理新传入的intent数据。
     */
    private void handleIntent(){
        if(getIntent().getBooleanExtra(COMMAND_EXIT, false)){
            finish();
            return;
        }
        cvCamera2Listener.setIntent(getIntent());
        //获取适应后的宽高，作为答案网格的依据
        var ar = TwoRect.getAdaptedWH(
                winContentWidth,
                winContentHeight,
                getIntent().getIntExtra(SettingsFragment.PROTO_WIDTH, 180),
                getIntent().getIntExtra(SettingsFragment.PROTO_HEIGHT, 180)
        );
        paperWidth = ar.w;
        paperHeight = ar.h;
        THTools.log("适应后的宽高分别为%d,%d", paperWidth, paperHeight);
        String features = getIntent().getStringExtra(SettingsFragment.FEATURES);
        //设置标准答案网格
        if(features != null && !features.isEmpty()) binding.correctPaperContainer.setFeatures(features);
        protoBmp = THTools.getBitmapFromInternalStorage(this, PROTO_PNG);
        if(protoBmp == null) return;
        Mat protoMat = new Mat();
        Utils.bitmapToMat(protoBmp, protoMat);
        //这里需要将它二值化之后再数黑格，否则会报错
        Mat protoGrayMat = new Mat();
        Imgproc.cvtColor(protoMat, protoGrayMat, Imgproc.COLOR_RGB2GRAY);
        List<int[][][]> xysList = getAnsXYs();
        protoBlackCountHashMaps = new int[xysList.size()][];
        for(int i = 0; i < xysList.size(); i++){
            protoBlackCountHashMaps[i] = ChoicesGrader.blackPixelCount(protoGrayMat, xysList.get(i));
        }
        protoMat.release();
        protoGrayMat.release();
    }

    public void showCorrectPaper(){
        if(protoBmp == null) return;
        //非主线程不能更新ui
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                camera2View.setVisibility(View.GONE);
                binding.correctPaperContainer.setVisibility(View.VISIBLE);
                binding.unfilledPaper.setImageBitmap(protoBmp);
            }
        });
    }

    public void showCorrectPaper(Mat mat){
        List<int[][][]> xysList = getAnsXYs();
        protoBlackCountHashMaps = new int[xysList.size()][];
        for(int i = 0; i < xysList.size(); i++){
            protoBlackCountHashMaps[i] = ChoicesGrader.blackPixelCount(mat, xysList.get(i));
        }

        Bitmap bm = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.RGB_565);
        THTools.log("这个mat的宽有%d列，高有%d行", mat.cols(), mat.rows());
        for(int i = 0; i < 10; i++){
            THTools.log("这个图上随机某一点的像素值为：%s", Arrays.toString(mat.get((int) (Math.random() * mat.rows()), (int) (Math.random() * mat.cols()))));
        }
        Utils.matToBitmap(mat, bm);
        protoBmp = bm;
        THTools.log("这个bmp的宽%d，高%d", bm.getWidth(), bm.getHeight());
        THTools.saveBitmapToInternalStorage(this, bm, PROTO_PNG);
        //这实在是很遗憾。原本我计划要将原型图做成base64便于复制传输，但实际上即便选择了很低的分辨率，这个数据也还是很长。没有办法。
        //THTools.log(bitmapToBase64(bm));

        //非主线程不能更新ui
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                camera2View.setVisibility(View.GONE);
                binding.correctPaperContainer.setVisibility(View.VISIBLE);
                binding.unfilledPaper.setImageBitmap(bm);
            }
        });
    }

    public void showTesterPaper(Mat mat){
        Bitmap bm = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bm);
        //非主线程不能更新ui
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                camera2View.setVisibility(View.GONE);
                binding.testerPaperContainer.setVisibility(View.VISIBLE);
                binding.filledPaper.setImageBitmap(bm);
            }
        });
    }

}