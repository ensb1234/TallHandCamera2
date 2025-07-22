package com.scl.tallhandcamera2.grader;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.scl.tallhandcamera2.R;
import com.scl.tallhandcamera2.SettingsFragment;
import com.scl.tallhandcamera2.ThApplication;
import com.scl.tallhandcamera2.throom.AnsDetailsDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author 作者：涉川良 联系方式：QQ470707134
 * @date 2025/4/12
 * @Description
 */
public class CorrectPaperContainer extends ConstraintLayout {
    public CorrectPaperContainer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * 用这个去实例化，不要new！
     * @param parent
     * @return
     */
    public static CorrectPaperContainer createInstanceIn(ConstraintLayout parent){
        //通过布局膨胀，加载xml布局
        LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(R.layout.activity_camera2, parent, true).findViewById(R.id.correctPaperContainer);
    }

    private ImageView unfilledPaper;
    private ConstraintLayout correctAnswersContainer;
    private boolean inited = false;
    public List<String> features = new ArrayList<>();
    public List<int[][][]> ansXYs = new ArrayList<>();
    private ThApplication app;
    private AnsDetailsDatabase db;
    public void initialize(){
        unfilledPaper = findViewById(R.id.unfilledPaper);
        correctAnswersContainer = findViewById(R.id.correctAnswersContainer);
        correctAnswersContainer.setOnTouchListener(new TouchAnswersListener());
        app = (ThApplication) getContext().getApplicationContext();
        db = AnsDetailsDatabase.getInstance(getContext());
    }

    /**
     * 用于初始化。
     * @param features_str 存储于应用记忆中的所有答案网格特征码
     */
    public void setFeatures(String features_str){
        features = Arrays.asList(features_str.split("\\r?\\n|\\r"));
        ansXYs = new ArrayList<>();
        for(int i = 0; i < features.size(); i++){
            ansXYs.add(ChoicesGridView.getXYPoints(features.get(i)));
        }
        inited = false;
    }

    @Override
    public void setVisibility(int visibility) {
        if(visibility == GONE) {
            //消失时应生成特征码
            features = new ArrayList<>();
            ansXYs = new ArrayList<>();
            int childCount = correctAnswersContainer.getChildCount();
            for (int i = 0; i < childCount; i++) {
                ChoicesGridView child = (ChoicesGridView) correctAnswersContainer.getChildAt(i);
                features.add(child.toFeatureCode());
                ansXYs.add(child.getXYPoints());
            }
            SettingsFragment.ed.putString(SettingsFragment.FEATURES, String.join("\n", features));
            SettingsFragment.ed.apply();
            //将记录答题状况的数据表清空
            app.getAnsExecutor().execute(()->{
                db.ansDetailsDao().deleteAll();
            });
        }else if(visibility == VISIBLE){
            if(!inited){
                correctAnswersContainer.removeAllViews();
                for(int i = 0; i < features.size(); i++){
                    ChoicesGridView cg = new ChoicesGridView(getContext(), correctAnswersContainer, features.get(i));
                    correctAnswersContainer.addView(cg);
                }
                inited = true;
            }
        }
        super.setVisibility(visibility);
    }

    private class TouchAnswersListener implements View.OnTouchListener{
        private float originX;//记录触摸的起始坐标
        private float originY;
        private ChoicesGridView drawingCg;

        @Override
        public boolean onTouch(View v, MotionEvent event){
            switch(event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    originX = event.getX();//记录起始触摸的坐标
                    originY = event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if(drawingCg == null){
                        //一开始绘制时，须先创建答案网格实例。
                        drawingCg = new ChoicesGridView(v.getContext(), correctAnswersContainer);
                        //添加到容器。
                        correctAnswersContainer.addView(drawingCg);
                    }
                    move(drawingCg, (int) originX, (int) originY, (int) event.getX(), (int) event.getY());
                    break;
                case MotionEvent.ACTION_UP:
                    //如果没有拖动，就不绘制，无事发生
                    if(drawingCg == null){
                        break;
                    }
                    //如果不足180宽高，则自动重置到180
                    float w = event.getX() - originX;
                    float h = event.getY() - originY;
                    if(Math.abs(w) < 180){
                        w = Math.signum(w) * 180;
                    }
                    if(Math.abs(h) < 180){
                        h = Math.signum(h) * 180;
                    }
                    if(w == 0) w = 180;
                    if(h == 0) h = 180;
                    move(drawingCg, (int) originX, (int) originY, (int) (originX + w), (int) (originY + h));
                    if(drawingCg.getWidth() < 180 || drawingCg.getHeight() < 180){
                        correctAnswersContainer.removeView(drawingCg);
                    }
                    //触摸结束，记录手指离开屏幕的状态。
                    drawingCg = null;
                    break;
            }
            return true;
        }

        private void move(ChoicesGridView cg, int l, int t, int r, int b){
            //满足从起始点开始往任意方向拖动，都能绘制出来
            int w = r - l;
            int h = b - t;
            if(w < 0){
                w = -w;
                r = l;
                l -= w;
            }
            if(h < 0){
                h = -h;
                b = t;
                t -= h;
            }
            cg.layout(l, t, r, b);
        }
    }

    /**
     * 判断参照物cg0与给定的矩形是否重叠。
     */
    private boolean isOverlap(ChoicesGridView cg0, int l, int t, int r, int b){
        int l0 = (int) cg0.getX();
        int t0 = (int) cg0.getY();
        int r0 = cg0.getWidth() + l0;
        int b0 = cg0.getHeight() + t0;
        return r > l0 && b > t0 && l < r0 && t < b0;
    }

    public boolean allowLayout(ChoicesGridView cg, int l, int t, int r, int b){
        for (int i = 0; i < correctAnswersContainer.getChildCount(); i++) {
            ChoicesGridView cg0 = (ChoicesGridView) correctAnswersContainer.getChildAt(i);
            if(cg0 != cg){
                if(isOverlap(cg0, l, t, r, b)){
                    return true;
                }
            }
        }
        return false;
    }
}
