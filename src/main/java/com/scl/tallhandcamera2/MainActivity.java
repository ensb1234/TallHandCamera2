package com.scl.tallhandcamera2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.opencv.android.OpenCVLoader;

/**
 * @author 作者：涉川良 联系方式：QQ470707134
 * @date 2025/4/8
 * @Description
 */
public class MainActivity extends AppCompatActivity {
    private SettingsFragment settings;
    private static boolean inited = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        if(inited){
            initialize();
            THTools.log("主页任务%d已经重建。现在是%s屏状态。", getTaskId(), THTools.isLandscape(this)?"横":"竖");
            return;
        }
        //opencv初始化
        if (OpenCVLoader.initLocal()) {
            Log.i(THTools.TAG, "OpenCV loaded successfully");
        } else {
            Log.e(THTools.TAG, "OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
        }

        //请求相机权限
        if (allPermissionsGranted()) {
            //如果权限已经全部通过那就初始化
            initialize();
        } else {
            //否则就申请权限，回调函数参看下面的onRequestPermissionsResult
            ActivityCompat.requestPermissions(this, Configuration.REQUIRED_PERMISSIONS,
                    Configuration.REQUEST_CODE_PERMISSIONS);
        }
        THTools.log("主页任务%d已经创建。现在是%s屏状态。", getTaskId(), THTools.isLandscape(this)?"横":"竖");
        inited = true;
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        THTools.log("主页任务%d已经摧毁。现在是%s屏状态。", getTaskId(), THTools.isLandscape(this)?"横":"竖");
    }

    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        THTools.log("主页任务%d已经复用。现在是%s屏状态。", getTaskId(), THTools.isLandscape(this)?"横":"竖");
        if(settings != null) settings.setCorrectPapers();
    }

    /**
     * 判断是否已经通过全部权限
     * @return 通过与否
     */
    private boolean allPermissionsGranted() {
        for (String permission : Configuration.REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Configuration.REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                initialize();
            } else {
                Toast.makeText(this, "用户拒绝授权，已自动退出", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private static class Configuration {
        public static final int REQUEST_CODE_PERMISSIONS = 10;
        public static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    }

    //todo

    private void initialize(){
        //设置页面初始化
        settings = new SettingsFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, settings)
                .commit();
    }
}