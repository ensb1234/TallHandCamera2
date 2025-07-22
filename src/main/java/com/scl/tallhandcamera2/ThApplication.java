package com.scl.tallhandcamera2;

import android.app.Application;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author 作者：涉川良 联系方式：QQ470707134
 * @date 2025/7/20
 * @Description
 */
public class ThApplication extends Application {

    private ExecutorService AnsExecutor;

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化全局资源
        AnsExecutor = Executors.newSingleThreadExecutor();
    }

    public ExecutorService getAnsExecutor() {
        return AnsExecutor;
    }

    // 可选：在应用终止时关闭线程池（有些设备不会调用）
    @Override
    public void onTerminate() {
        if (AnsExecutor != null) {
            AnsExecutor.shutdown();
        }
        super.onTerminate();
    }
}
