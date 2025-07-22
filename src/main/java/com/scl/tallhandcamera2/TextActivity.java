package com.scl.tallhandcamera2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;
import com.scl.tallhandcamera2.databinding.ActivityTextBinding;
import com.scl.tallhandcamera2.throom.AnsAnalysis;
import com.scl.tallhandcamera2.throom.AnsDetailsDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TextActivity extends AppCompatActivity {

    public static final String HTML_FILE_NAME = "html_file_name";
    public static final String HTML_INSTRUCTION = "instruction";
    public static final String HTML_ANSWERING_STATUS = "answering_status";
    private static final String ANSWERS_26 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private ActivityTextBinding binding;
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTextBinding.inflate(getLayoutInflater());

        EdgeToEdge.enable(this);
        //有binding时用下面这句会导致按钮点击侦听的设置无效
        //setContentView(R.layout.activity_text);
        setContentView(binding.getRoot());
        //顺带下面这句也要改
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(TextActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                TextActivity.this.finish();
            }
        });

        WebSettings myWebSettings = binding.webview.getSettings();
        myWebSettings.setJavaScriptEnabled(true); // 支持JavaScript
        myWebSettings.setUseWideViewPort(true); // 将图片调整到适合WebView大小
        myWebSettings.setBuiltInZoomControls(true);// 启用内置缩放控件
        myWebSettings.setDisplayZoomControls(false);// 隐藏原生的缩放按钮
        myWebSettings.setTextZoom(100); // 设置文本缩放比例，默认值是 100
        myWebSettings.setUseWideViewPort(true);// 使用宽视图（即缩放至屏幕宽度）
        myWebSettings.setLoadWithOverviewMode(true); // 缩放至屏幕大小
        myWebSettings.setDomStorageEnabled(true); // 设置DOM缓存
        myWebSettings.setCacheMode(WebSettings.LOAD_NO_CACHE); // 禁用缓存
        myWebSettings.setAllowFileAccess(true); // 允许访问文件
        myWebSettings.setAllowContentAccess(true); // 允许访问内容
        myWebSettings.setAllowUniversalAccessFromFileURLs(true); // 允许跨域访问
        // 加载本地HTML文件
        String filePath = String.format("file:///android_asset/%s.html", getIntent().getStringExtra(HTML_FILE_NAME));
        binding.webview.addJavascriptInterface(new WebAppInterface(), "Android");
        binding.webview.loadUrl(filePath);
    }

    class WebAppInterface {
        @JavascriptInterface
        public void getRoomData() {
            var db = AnsDetailsDatabase.getInstance(TextActivity.this);
            ((ThApplication)getApplication()).getAnsExecutor().execute(() -> {
                        List<AnsAnalysis> ansAnalysisList = db.ansDetailsDao().analysisForAllAns();
                        String json = new Gson().toJson(ansAnalysisList);
                        runOnUiThread(() -> {
                            binding.webview.evaluateJavascript("handleData(" + json + ")", null);
                        });
                    }
            );
        }
    }

}