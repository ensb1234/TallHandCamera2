package com.scl.tallhandcamera2;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.graphics.Color;

import androidx.constraintlayout.widget.ConstraintLayout;

/**
 * @author 作者：涉川良 联系方式：QQ470707134
 * @date 2025/6/21
 * @Description
 */
public class TriTextButton extends ConstraintLayout {

    private TextView text1, text2, text3;
    private TextView[] texts;
    private int currentIndex = 0;
    private AnimatorSet[] animatorSets;
    private float topTextX;
    private float topTextY;
    private float middleTextX;
    private float middleTextY;
    private float bottomTextX;
    private float bottomTextY;
    private final float frontAlpha = 1.0f;
    private final float backAlpha = 0.2f;

    public TriTextButton(Context context) {
        this(context, null);
    }

    public TriTextButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TriTextButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.button_tri_text, this);

        text1 = findViewById(R.id.text1);//man
        text2 = findViewById(R.id.text2);//prt
        text3 = findViewById(R.id.text3);//aut
        texts = new TextView[]{text1, text2, text3};

        this.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                TriTextButton.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                topTextX = text2.getX();
                topTextY = text2.getY();
                middleTextX = text1.getX();
                middleTextY = text1.getY();
                bottomTextX = text3.getX();
                bottomTextY = text3.getY();
                animatorSets = new AnimatorSet[3];
                animatorSets[0] = new AnimatorSet();
                animatorSets[0].playTogether(
                        createAnimator(text1, middleTextX, middleTextY, frontAlpha),
                        createAnimator(text2, topTextX, topTextY, backAlpha),
                        createAnimator(text3, bottomTextX, bottomTextY, backAlpha)
                );
                animatorSets[1] = new AnimatorSet();
                animatorSets[1].playTogether(
                        createAnimator(text2, middleTextX, middleTextY, frontAlpha),
                        createAnimator(text3, topTextX, topTextY, backAlpha),
                        createAnimator(text1, bottomTextX, bottomTextY, backAlpha)
                );
                animatorSets[2] = new AnimatorSet();
                animatorSets[2].playTogether(
                        createAnimator(text3, middleTextX, middleTextY, frontAlpha),
                        createAnimator(text1, topTextX, topTextY, backAlpha),
                        createAnimator(text2, bottomTextX, bottomTextY, backAlpha)
                );
            }
        });

        // 设置点击事件
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                currentIndex = (currentIndex + 1) % texts.length;
                animatorSets[currentIndex].start();
            }
        });
    }

    public String getState(){
        return texts[currentIndex].getText().toString();
    }

    private ObjectAnimator createAnimator(View v, float endX, float endY, float alpha){
        // 创建属性值持有者
        PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat("x", endX);
        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat("y", endY);
        PropertyValuesHolder pvhAlpha = PropertyValuesHolder.ofFloat("alpha", alpha); // 结束时的透明度
        // 创建 ObjectAnimator 并设置目标对象
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(v, pvhX, pvhY, pvhAlpha);
        animator.setDuration(500); // 动画持续时间
        return animator;
    }

    // 提供设置文本的方法
    public void setTexts(String text1, String text2, String text3) {
        this.text1.setText(text1);
        this.text2.setText(text2);
        this.text3.setText(text3);
    }
}
