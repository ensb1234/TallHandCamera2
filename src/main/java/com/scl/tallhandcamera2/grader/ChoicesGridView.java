package com.scl.tallhandcamera2.grader;

import static com.scl.tallhandcamera2.THTools.matchInt;
import static com.scl.tallhandcamera2.THTools.matchStr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.scl.tallhandcamera2.Camera2Activity;
import com.scl.tallhandcamera2.R;
import com.scl.tallhandcamera2.THEvents;
import com.scl.tallhandcamera2.throom.AnsDetails;

import java.util.Arrays;
import java.util.HashSet;

/**
 * @author 作者：涉川良 联系方式：QQ470707134
 * @date 2025/5/4
 * @Description
 */
public class ChoicesGridView extends View implements View.OnTouchListener {

// todo 主要是UI ================================================================

    private ConstraintLayout.LayoutParams layoutParams;
    private final Paint paintLineGrids = new Paint();//网格画笔
    private final Paint paintFillAns = new Paint();//答案填充
    private final Paint paintLineBorder = new Paint();//外框
    private final Paint paintFillInside = new Paint();//默认底色填充
    private final Paint paintLineTesterIncorrect = new Paint();//考生答案外框（错误答案）
    private final Paint paintLineTesterCorrect = new Paint();//考生答案外框（正确答案）
    private static final int ICON_DIAMETER = 60;
    private Drawable da_add;
    private Drawable da_sub;
    private Drawable da_mul;
    private Drawable da_move;
    private Drawable da_ud;
    private Drawable da_lr;
    private Drawable da_rd;
    private Drawable da_switch;
    private Drawable da_l;
    private Drawable da_r;
    private Drawable da_u;
    private Drawable da_d;
    private Drawable da_l1;
    private Drawable da_r1;
    private Drawable da_u1;
    private Drawable da_d1;
    private Drawable da_zheng;
    private Drawable da_zheng1;
    private Drawable[] iconsCoarseTuning;//粗调图标数组
    private Drawable[] iconsFineTuning;//微调图标数组
    private final int[][] iconsPositions = new int[9][4];//含9个图标的left值、top值、right值、bottom值的二维数组
    public void init(){
        //线条画笔
        paintLineGrids.setStyle(Paint.Style.STROKE);
        paintLineGrids.setColor(0xa00068b7);
        paintLineGrids.setStrokeWidth(5.0f);
        //答案填充
        paintFillAns.setStyle(Paint.Style.FILL);
        paintFillAns.setColor(0x600068b7);
        //外框线条
        paintLineBorder.setStyle(Paint.Style.STROKE);
        paintLineBorder.setColor(0xa00068b7);
        paintLineBorder.setStrokeWidth(10.0f);
        //默认填充
        paintFillInside.setStyle(Paint.Style.FILL);
        paintFillInside.setColor(0x400068b7);
        //考生答案格子填充
        paintLineTesterIncorrect.setStyle(Paint.Style.STROKE);
        paintLineTesterIncorrect.setColor(0xffff0000);
        paintLineTesterIncorrect.setStrokeWidth(5.0f);
        paintLineTesterCorrect.setStyle(Paint.Style.STROKE);
        paintLineTesterCorrect.setColor(0xff60bf60);
        paintLineTesterCorrect.setStrokeWidth(5.0f);
        //图标资源引用
        da_add = ContextCompat.getDrawable(getContext(), R.drawable.add);
        da_sub = ContextCompat.getDrawable(getContext(), R.drawable.sub);
        da_mul = ContextCompat.getDrawable(getContext(), R.drawable.mul);
        da_move = ContextCompat.getDrawable(getContext(), R.drawable.move);
        da_ud = ContextCompat.getDrawable(getContext(), R.drawable.ud);
        da_lr = ContextCompat.getDrawable(getContext(), R.drawable.lr);
        da_rd = ContextCompat.getDrawable(getContext(), R.drawable.rd);
        da_switch = ContextCompat.getDrawable(getContext(), R.drawable.resource_switch);
        da_l = ContextCompat.getDrawable(getContext(), R.drawable.l);
        da_r = ContextCompat.getDrawable(getContext(), R.drawable.r);
        da_u = ContextCompat.getDrawable(getContext(), R.drawable.u);
        da_d = ContextCompat.getDrawable(getContext(), R.drawable.d);
        da_l1 = ContextCompat.getDrawable(getContext(), R.drawable.l1);
        da_r1 = ContextCompat.getDrawable(getContext(), R.drawable.r1);
        da_u1 = ContextCompat.getDrawable(getContext(), R.drawable.u1);
        da_d1 = ContextCompat.getDrawable(getContext(), R.drawable.d1);
        da_zheng = ContextCompat.getDrawable(getContext(), R.drawable.zheng);
        da_zheng1 = ContextCompat.getDrawable(getContext(), R.drawable.zheng1);
        iconsCoarseTuning = new Drawable[]{da_switch, da_sub, da_add, da_sub, da_move, da_lr, da_add, da_ud, da_rd};
        iconsFineTuning = new Drawable[]{da_switch, da_u, da_l1, da_l, da_move, da_r1, da_u1, da_d1, landScape?da_zheng:da_zheng1};
        setOnTouchListener(this);
    }

    @Override
    public void layout(int l, int t, int r, int b){
        //if(!grandParent.allowLayout(this, l, t, r, b)) return;
        super.layout(l, t, r, b);
        layoutParams.leftMargin = l;
        layoutParams.topMargin = t;
        layoutParams.width = r - l;
        layoutParams.height = b - t;
        setLayoutParams(layoutParams);
    }

    /**
     * 计算九个图标的位置。
     * 图标对应的数组元素：
     * 0 1 2
     * 3 4 5
     * 6 7 8
     * @param w view的宽度
     * @param h view的高度
     * @param d 图标直径
     */
    private void getIconsPosition(int w, int h, int d){
        iconsPositions[0][0] = iconsPositions[3][0] = iconsPositions[6][0] = 0;
        iconsPositions[0][2] = iconsPositions[3][2] = iconsPositions[6][2] = d;
        iconsPositions[1][0] = iconsPositions[4][0] = iconsPositions[7][0] = (w - d)/2;
        iconsPositions[1][2] = iconsPositions[4][2] = iconsPositions[7][2] = (w + d)/2;
        iconsPositions[2][0] = iconsPositions[5][0] = iconsPositions[8][0] = w - d;
        iconsPositions[2][2] = iconsPositions[5][2] = iconsPositions[8][2] = w;

        iconsPositions[0][1] = iconsPositions[1][1] = iconsPositions[2][1] = 0;
        iconsPositions[0][3] = iconsPositions[1][3] = iconsPositions[2][3] = d;
        iconsPositions[3][1] = iconsPositions[4][1] = iconsPositions[5][1] = (h - d)/2;
        iconsPositions[3][3] = iconsPositions[4][3] = iconsPositions[5][3] = (h + d)/2;
        iconsPositions[6][1] = iconsPositions[7][1] = iconsPositions[8][1] = h - d;
        iconsPositions[6][3] = iconsPositions[7][3] = iconsPositions[8][3] = h;
    }

    /**
     * 根据九图标位置数组，对比后得知点击处位于哪个图标区域内。
     * @param x 点击处x坐标
     * @param y 点击处y坐标
     * @return 点击处所在的图标序号。如果不在图标区域内，则返回-100
     */
    private int checkFingerPosition(float x, float y){
        for(int i = 0; i < 9; i++){
            if(x>=iconsPositions[i][0] && x<=iconsPositions[i][2] && y>=iconsPositions[i][1] && y<=iconsPositions[i][3]){
                return i;
            }
        }
        return -100;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas){
        super.onDraw(canvas);
        //onDraw调用时机：
        //添加这个view到窗口时；view尺寸、位置发生改变时；手动调用invalidate()或postInvalidate();

        //当这个答案网格是扫描学生答案后显示的、也即只有一次性的功能时，应该只需要绘制一次。
        //但该应用测试时，系统会自动重绘随机一到数次，由于使用的是透明蓝色，叠加后肉眼可见不同。推测有可能是因为硬件加速？
        //以下是防止重绘的解决方法。这个问题我只能这样解决。
//        if(testerMode > 1){
//            //return;
//        }else if(testerMode == 1){
//            testerMode++;
//        }
        //获取该view尺寸
        int w = getWidth();
        int h = getHeight();
        //获取每行每列间距
        float colspacing = (float) w / colsCount;
        float rowspacing = (float) h / rowsCount;
        //绘制网格
        for(float i = colspacing; i < w; i += colspacing){
            canvas.drawLine(i, 0, i, h, paintLineGrids);
        }
        for(float i = rowspacing; i < h; i += rowspacing){
            canvas.drawLine(0, i, w, i, paintLineGrids);
        }
        //绘制外框和默认底色
        canvas.drawRect(0,0,w,h, paintLineBorder);
        canvas.drawRect(0,0,w,h, paintFillInside);
        //填充答案所在方框
        float left, top;
        for(int answer : answers){
            left = colspacing * (answer % colsCount);
            top = rowspacing * ((int) Math.floor((double) answer / colsCount));
            canvas.drawRect(left, top, left + colspacing, top + rowspacing, paintFillAns);
        }
        //if(testerMode == 2){
        if(testerMode){
            //考生答案模式，只能进入这里一次，函数入口时是1，这里是2
            float right, bottom;
            for(int answer : testerAnswers){
                left = colspacing * (answer % colsCount);
                top = rowspacing * ((int) Math.floor((double) answer / colsCount));
                right = left + colspacing;
                bottom = top + rowspacing;
                if(answers.contains(answer)){
                    //如果该答案是正确答案，则绘制为绿色；否则绘制为红色。
                    canvas.drawRect(left, top, right, bottom, paintLineTesterCorrect);
                    canvas.drawLine(left, top, right, bottom, paintLineTesterCorrect);
                }else{
                    canvas.drawRect(left, top, right, bottom, paintLineTesterIncorrect);
                    canvas.drawLine(left, top, right, bottom, paintLineTesterIncorrect);
                }
            }
        }
        //绘制九图标
        switch (currentMode){
            case COARSE_TUNING:
                getIconsPosition(w, h, ICON_DIAMETER);
                for(int i = 0; i < 9; i++){
                    iconsCoarseTuning[i].setBounds(iconsPositions[i][0], iconsPositions[i][1], iconsPositions[i][2], iconsPositions[i][3]);
                    iconsCoarseTuning[i].draw(canvas);
                }
                break;
            case FINE_TUNING:
                getIconsPosition(w, h, ICON_DIAMETER);
                for(int i = 0; i < 9; i++){
                    iconsFineTuning[i].setBounds(iconsPositions[i][0], iconsPositions[i][1], iconsPositions[i][2], iconsPositions[i][3]);
                    iconsFineTuning[i].draw(canvas);
                }
                break;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //if(testerMode > 0){
        if(testerMode){
            if(event.getAction() == MotionEvent.ACTION_UP){
                //计算点击处所在格子的在第几行第几列（0开始）
                int res_x = (int) Math.floor(event.getX() / getWidth() * colsCount);
                int res_y = (int) Math.floor(event.getY() / getHeight() * rowsCount);
                //存储点击处所在格子序号为答案，左上角第一个序号为0
                res_x = res_y * colsCount + res_x;//借用res_x来存储这个序号
                if(!testerAnswers.add(res_x)){
                    testerAnswers.remove(res_x);
                }
                //更新绘制，绘制时会将已点击处的格子染色（见onDraw）
                invalidate();
                THEvents.getInstance().dispatchUpdateScoreEvent();
            }
            return true;
        }
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                switch (currentMode){
                    case COARSE_TUNING:
                        pressingBtn = checkFingerPosition(event.getX(), event.getY());
                        break;
                    case FINE_TUNING:
                        pressingBtn = checkFingerPosition(event.getX(), event.getY()) + 10;
                        break;
                    default:
                        //EDIT
                        pressingBtn = 9;
                }
                //触摸事件开始，存储触摸起始点坐标
                startingTouchX = event.getX();
                startingTouchY = event.getY();
                //也储存一开始view的四个边距
                originLeft = getLeft();
                originRight = getRight();
                originTop = getTop();
                originBottom = getBottom();
                break;
            case MotionEvent.ACTION_MOVE:
                //存储手指在横纵坐标上的位移分量
                int displacementX = (int) (event.getX() - startingTouchX);
                int displacementY = (int) (event.getY() - startingTouchY);
                switch (pressingBtn){
                    //粗调模式
                    case 4:
                        //任意移动
                        move(displacementX, displacementY);
                        break;
                    case 5:
                        //右边线调整
                        drag(displacementX, 0);
                        break;
                    case 7:
                        //下边线调整
                        drag(0, displacementY);
                        break;
                    case 8:
                        //右、下边线一起调整
                        drag(displacementX, displacementY);
                        break;
                    //编辑模式
                    case 9:
                        //编辑模式下，移动手指超过一定范围就触发模式切换
                        if(Math.abs(displacementX) > 100 || Math.abs(displacementY) > 100){
                            currentMode = GridMode.COARSE_TUNING;
                            invalidate();
                            pressingBtn = -100;
                        }
                        break;
                }
                break;
            case MotionEvent.ACTION_UP:
                switch (pressingBtn){
                    //粗调模式
                    case 0:
                        //左上角手指离开，是切换模式命令
                        currentMode = GridMode.FINE_TUNING;
                        invalidate();
                        break;
                    case 1:
                        //减少列数
                        if(colsCount > 1){
                            colsCount--;
                            invalidate();
                        }
                        break;
                    case 2:
                        //增加列数
                        if(colsCount <= 26){
                            colsCount++;
                            invalidate();
                        }
                        break;
                    case 3:
                        //减少行数
                        if(rowsCount > 1){
                            rowsCount--;
                            invalidate();
                        }
                        break;
                    case 6:
                        //增加行数
                        if(rowsCount <= 26){
                            rowsCount++;
                            invalidate();
                        }
                        break;
                    //EDIT 编辑模式
                    case 9:
                        //计算点击处所在格子的在第几行第几列（0开始）
                        int res_x = (int) Math.floor(event.getX() / getWidth() * colsCount);
                        int res_y = (int) Math.floor(event.getY() / getHeight() * rowsCount);
                        //存储点击处所在格子序号为答案，左上角第一个序号为0
                        res_x = res_y * colsCount + res_x;//借用res_x来存储这个序号
                        if(!answers.add(res_x)){
                            answers.remove(res_x);
                        }
                        //更新绘制，绘制时会将已点击处的格子染色（见onDraw）
                        invalidate();
                        break;
                    //微调模式
                    case 10:
                        //左上角手指离开，是切换模式命令
                        currentMode = GridMode.EDIT;
                        invalidate();
                        break;
                    case 11:
                        //向上微移
                        move(0, -1);
                        break;
                    case 12:
                        //右边线微向左，变窄
                        drag(-1, 0);
                        break;
                    case 13:
                        //向左微移
                        move(-1, 0);
                        break;
                    case 14:
                        //中间，表示切换微调的move模式
                        if(fineTuningMove){
                            fineTuningMove = false;
                            iconsFineTuning[5] = da_r1;
                            iconsFineTuning[7] = da_d1;
                            iconsFineTuning[8] = landScape?da_zheng:da_zheng1;
                        }else{
                            fineTuningMove = true;
                            iconsFineTuning[5] = da_r;
                            iconsFineTuning[7] = da_d;
                            iconsFineTuning[8] = da_mul;
                        }
                        invalidate();
                        break;
                    case 15:
                        if(fineTuningMove){
                            //向右微移
                            move(1, 0);
                        }else{
                            //右边线微向右，加宽
                            drag(1, 0);
                        }
                        break;
                    case 16:
                        //下边线微向上，变窄
                        drag(0, -1);
                        break;
                    case 17:
                        if(fineTuningMove){
                            //向下微移
                            move(0, 1);
                        }else{
                            //下边线微向下，加高
                            drag(0, 1);
                        }
                        break;
                    case 18:
                        //右下角关闭或切换横竖屏
                        if(!fineTuningMove){
                            landScape = !landScape;
                            iconsFineTuning[8] = landScape?da_zheng:da_zheng1;
                            invalidate();
                        }else{
                            ((ViewGroup) getParent()).removeView(this);
                        }
                        break;
                }
                pressingBtn = -1;
                break;
        }
        return true;
    }

//todo 主要用于绘制标准答案时 ================================================================

    private static final int PRECISION = 10000;
    public int rowsCount = 4;//横屏行数，默认为3
    public int colsCount = 3;//横屏列数，默认为4
    private boolean landScape = false;
    public HashSet<Integer> answers = new HashSet<>(Arrays.asList(9, 7, 5, 2));//答案数组。记录的是答案所在格子的序号。横屏左上角第一个网格序号为0，其右边为1、2、3...换行继续，依此类推。
    public HashSet<Integer> testerAnswers;
    private boolean testerMode = false;
    private static final String ANSWERS_26 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private ChoiceScoreSetExs choiceScoreSetExs;

    public enum GridMode {
        COARSE_TUNING,
        FINE_TUNING,
        EDIT
    }//粗调，微调，编辑模式
    public GridMode currentMode = GridMode.EDIT;//这个视图目前的功能模式。默认为编辑
    private boolean fineTuningMove = false;//微调模式下，是否为四向微移模式？默认否。
    private float startingTouchX;//手指触摸该视图时，触摸起点的x坐标。
    private float startingTouchY;//同上y坐标
    private int originLeft;//手指触摸该视图时，最初的左边距。
    private int originRight;
    private int originTop;
    private int originBottom;//以上类推
    private int maxRight;//最右
    private int maxBottom;//最底
    private int pressingBtn = -1;//0-8粗调；10-18微调；9编辑
    //private CorrectPaperContainer grandParent;

    ///该构造函数对应的是手指从头开始绘制该视图。
    public ChoicesGridView(Context context, ViewGroup container) {
        super(context);
        init();
        //储存父视图的尺寸
        getMaxRB();
        //
        layoutParams = new ConstraintLayout.LayoutParams(180, 180);
        layoutParams.leftToLeft = container.getId();
        layoutParams.topToTop = container.getId();
        setLayoutParams(layoutParams);
        choiceScoreSetExs = new ChoiceScoreSetExs(rowsCount);
    }

    private void getMaxRB(){
        this.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // 移除监听器防止重复调用
                ChoicesGridView.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                // 获取父容器的宽高
                ChoicesGridView.this.maxRight = ChoicesGridView.this.getParent() != null ? ((View) ChoicesGridView.this.getParent()).getWidth() : 0;
                ChoicesGridView.this.maxBottom = ChoicesGridView.this.getParent() != null ? ((View) ChoicesGridView.this.getParent()).getHeight() : 0;
            }
        });
    }

    /**
     * 移动该view
     * @param dx 移动的x轴距离
     * @param dy 移动的y轴距离
     */
    private void move(int dx, int dy){
        int nl = getLeft() + dx;
        int nr = getRight() + dx;
        int nt = getTop() + dy;
        int nb = getBottom() +dy;
        if(nl < 0 || nr > maxRight){
            nl -= dx;
            nr -= dx;
        }
        if(nt < 0 || nb > maxBottom){
            nt -= dy;
            nb -= dy;
        }
        layout(nl, nt, nr, nb);
    }

    /**
     * 拖动改变view的大小（仅右、下边线）
     * @param dx 右边线位置改变量
     * @param dy 下边线位置改变量
     */
    private void drag(int dx, int dy){
        int l = originLeft;
        int t = originTop;
        int nr = originRight + dx;
        int nb = originBottom + dy;
        if(nr > maxRight){
            nr = maxRight;
        }else if(nr - l < 180){
            nr = l + 180;
        }
        if(nb > maxBottom){
            nb = maxBottom;
        }else if(nb - t < 180){
            nb = t + 180;
        }
        layout(l, t, nr, nb);
    }

    private int num2ratiox(int num){
        return (num * PRECISION) / Camera2Activity.paperWidth;
    }

    private int num2ratioy(int num){
        return (num * PRECISION) / Camera2Activity.paperHeight;
    }

    /**
     * 生成对应特征码。一般是父容器在隐藏时，调用所有这种子视图的这个函数。<br/>
     * 《特征码说明书》<br/>
     * xy坐标，代表该答案表格的位置<br/>
     * hw高宽，代表该答案表格的大小尺寸<br/>
     * rc行列，代表该答案表格的行数列数<br/>
     * LP横竖屏，代表该答案是匹配横屏或竖屏，L横屏，P竖屏<br/>
     * 答案，用字母A到Z表示。答案表格每行第一个格子对应字母A，第二个格子对应B，依此类推。每题答案之间用半角逗号隔开。<br/>
     * 分数设定，格式为：<br/>
     * 【题序】:【总分】【+-】【s】【ox】<br/>
     * 首先是【题序】，代表这个设定到哪一题结束（包括这一题），首题题序为1，后面依此类推。<br/>
     * 因此，分数设定可以有多个，不同设定的题序应该不同。如果相同，后面的设定会覆盖前面的设定。<br/>
     * 第一个设定的范围是从第一题开始，到这个设定的题序结束。<br/>
     * 第二个及以后的设定中，该设定的范围是从上一个题序的下一题开始，到这个设定的题序结束。<br/>
     * 不同的设定之间用半角逗号隔开。<br/>
     * 【总分】代表该题总分。本题扣分值最大不能超过这个数字。<br/>
     * 当本题为单选时，【总分】之后的符号可以省略。<br/>
     * 【+-】可能为+或-；【s】是一个数字，代表给分或扣分。<br/>
     * 【+-】连同【s】代表逐个答案的给分细则。<br/>
     * 如果是+s，代表“对一得s”；<br/>
     * 如果是-s，代表“漏一扣s”。<br/>
     * 【ox】可能为o或x（小写字母）；<br/>
     * 其中o代表“错一无视”，x代表“错一全扣”。省略这一项时，默认为错一全扣。<br/>
     * @return 特征码。可以根据特征码重绘该视图。<br/>
     */
    public String toFeatureCode(){
        String[] answer_strs = answers2Strs(answers);

        if(choiceScoreSetExs == null){
            choiceScoreSetExs = new ChoiceScoreSetExs(landScape?rowsCount:colsCount);
        }
        @SuppressLint("DefaultLocale") String res = String.format("x%dy%d_w%dh%d_r%dc%d_%s:%s_%s",
                num2ratiox((int)getX()),num2ratioy((int)getY()),num2ratiox(getWidth()),num2ratioy(getHeight()),
                rowsCount,colsCount,landScape ? "L":"P",String.join(",", answer_strs), choiceScoreSetExs.scoreSetsString);
        return res;
    }

    /**
     * 将 由格子序号组成的所有题目答案 转化为字面可见的字符串数组。
     * @param answer_nums 可能是标准答案，也可能是客户给出的考生答案
     * @return 字面可见的字符串答案数组。每一题对应一个数组元素。
     */
    private String[] answers2Strs(HashSet<Integer> answer_nums){
        int ques_count = landScape?rowsCount:colsCount;
        int answers_count = rowsCount * colsCount;
        String[] answer_strs = new String[ques_count];
        Arrays.fill(answer_strs, "");
        if(landScape){
            for(int i : answer_nums){
                if(i >= answers_count) continue;
                answer_strs[(int)(i/colsCount)] += ANSWERS_26.charAt(i%colsCount);
            }
        }else{
            int new_num;
            for(int i : answer_nums){
                if(i >= answers_count) continue;
                new_num = rowsCount - (int)(i/colsCount) - 1 + (i%colsCount) * rowsCount;
                answer_strs[(int)(new_num/rowsCount)] += ANSWERS_26.charAt(new_num%rowsCount);
            }
        }
        return answer_strs;
    }


//todo 主要用于显示考生答案时 ================================================================

    ///该构造函数对应的是已有特征码的情况下，自动绘制本视图。
    public ChoicesGridView(Context context, ViewGroup container, String feature){
        super(context);

        //储存父视图的尺寸
        getMaxRB();
        //解析
        String[] strings = feature.split("_");
        if(strings.length != 5){
            strings = ("x0y0_w180h180_r3c4_P:A,B,CD_3@0:2").split("_");
        }
        //首先分析xy坐标（位置）
        int x = matchInt(strings[0], "x(\\d+)");
        int y = matchInt(strings[0], "y(\\d+)");
        if(x < 0) x = 0;
        if(y < 0) y = 0;
        x = ratio2Numx(x);
        y = ratio2Numy(y);
        //然后分析wh宽高（尺寸）
        int w = matchInt(strings[1], "w(\\d+)");
        int h = matchInt(strings[1], "h(\\d+)");
        w = ratio2Numx(w);
        h = ratio2Numy(h);
        if(w < 180) w = 180;
        if(h < 180) h = 180;
        //应用以上指示的位置和尺寸到格式中。
        super.layout(x, y, x+w, y+h);
        layoutParams = new ConstraintLayout.LayoutParams(w, h);
        layoutParams.leftToLeft = container.getId();
        layoutParams.topToTop = container.getId();
        layoutParams.leftMargin = x;
        layoutParams.topMargin = y;
        setLayoutParams(layoutParams);
        //再而是rc行列数
        rowsCount = matchInt(strings[2], "r(\\d+)");
        colsCount = matchInt(strings[2], "c(\\d+)");
        if(rowsCount < 1 || rowsCount > 26) rowsCount = 1;
        if(colsCount < 1 || colsCount > 26) colsCount = 1;
        //接着是各题答案，半角冒号前是横屏L竖屏P，冒号后每题答案用半角逗号隔开
        landScape = (strings[3].charAt(0) == 'L');
        //读取出由每题的答案组成的数组
        String[] questions_answers = matchStr(strings[3], ":(.+)").split(",");
        //将答案（ABCD...）转化为数字（格子序号），注意，当数组长度小于题量时，后面的答案会填充null
        answers = strs2answers(Arrays.copyOf(questions_answers, (landScape?rowsCount:colsCount)));
        //最后是各题给分设定
        choiceScoreSetExs = new ChoiceScoreSetExs(strings[4], landScape?rowsCount:colsCount);
        choiceScoreSetExs.analyzeScoreSets();
        init();
    }

    private HashSet<Integer> strs2answers(String[] strs){
        HashSet<Integer> res = new HashSet<>();
        String str;
        if(landScape){
            //横屏
            for(int i = 0; i < strs.length; i++){
                str = strs[i];
                if(str == null) continue;
                for(int j = str.length() - 1; j >= 0; j--){
                    //横屏模式下，i即题号等于行号，故存储的格子序号应该等于行号乘以列数（即每行格子数）再加列号（即选项转换的数字）
                    res.add(i * colsCount + ANSWERS_26.indexOf(str.charAt(j)));
                }
            }
        }else{
            //竖屏（在前面的横屏条件下，顺时针转动九十度获得的竖屏，此时前置摄像头在屏幕上侧）
            int tmp;
            for(int i = 0; i < strs.length; i++){
                str = strs[i];
                if(str == null) continue;
                for(int j = str.length() - 1; j >= 0; j--){
                    //竖屏模式下，i即题号等于横屏列号，格子序号计算公式如下
                    tmp = rowsCount - ANSWERS_26.indexOf(str.charAt(j)) - 1;
                    if(tmp < 0){
                        continue;
                    }
                    res.add(tmp * colsCount + i);
                }
            }
        }
        return res;
    }

    public AnsDetails[] getAnsDetails(long studentId, int questionNumStartIndex){
        String[] strings = answers2Strs(testerAnswers);
        AnsDetails[] ansDetails = new AnsDetails[strings.length];
        for(int i = strings.length - 1; i >= 0; i--){
            ansDetails[i] = new AnsDetails(studentId, questionNumStartIndex + i, strings[i]);
        }
        return ansDetails;
    }

    private static int ratio2Numx(int ratio){
        return (ratio * Camera2Activity.paperWidth) / PRECISION;
    }

    private static int ratio2Numy(int ratio){
        return (ratio * Camera2Activity.paperHeight) / PRECISION;
    }

    /**
     * 获取这个网格视图的得分。这个方法合理只用于显示考生答案时，与传进来的标准答案对比。
     * @return 计算结果。
     */
    public int getTotalScore(){
        int score = 0;
        //首先转换考生答案和标准答案为字符串数组模式。
        int ques_count = landScape?rowsCount:colsCount;
        //【注意】该方法在使用时，这个网格并非保存标准答案，而是保存了考生答案。
        String[] tester_answer_strs = answers2Strs(testerAnswers);
        String[] standard_answer_strs = answers2Strs(answers);
        String sta,tes;
        int bingo_count,wrong_count,missing_count;
        for(int i = 0; i < ques_count; i++){
            sta = standard_answer_strs[i];
            tes = tester_answer_strs[i];
            bingo_count = wrong_count = missing_count = 0;
            for(int j = tes.length() - 1; j >= 0; j--){
                if(sta.indexOf(tes.charAt(j)) > -1){
                    //在标准答案中发现考生的选择。说明考生选对了。
                    bingo_count++;
                }else{
                    //找不到考生的选择，说明考生的选择错误。
                    wrong_count++;
                }
            }
            for(int j = sta.length() - 1; j >= 0; j--){
                if(tes.indexOf(sta.charAt(j)) == -1){
                    //考生的答案中，找不到标准答案的选择，说明考生漏选。
                    missing_count++;
                }
            }
            //计算出正确、错误、漏选个数之后，对照给分设定进行给分。
            score += choiceScoreSetExs.getScore(i, bingo_count, missing_count, wrong_count);
        }
        //按照不同的分值设定，逐题对比计算得分。
        return score;
    }

    public void Convert2TesterMode(HashSet<Integer> tester_answers){
        testerAnswers = tester_answers;
        //testerMode = 1;
        testerMode = true;
    }

    /**
     * 获取网格当中所有格子的左上角坐标数组。
     * @return 数组元素各层级为【行】【列】【x或y】
     */
    public int[][][] getXYPoints(){
        return getXYPoints((int)getX(),(int)getY(),getHeight(),getWidth(),rowsCount,colsCount);
    }

    public static int[][][] getXYPoints(String feature){
        String[] strings = feature.split("_");
        if(strings.length != 5){
            strings = ("x0y0_h180w180_r3c4_P").split("_");
        }
        //首先分析xy坐标（位置）
        int x = matchInt(strings[0], "x(\\d+)");
        int y = matchInt(strings[0], "y(\\d+)");
        if(x < 0) x = 0;
        if(y < 0) y = 0;
        x = ratio2Numx(x);
        y = ratio2Numy(y);
        //然后分析hw高和宽（尺寸）
        int h = matchInt(strings[1], "h(\\d+)");
        int w = matchInt(strings[1], "w(\\d+)");
        w = ratio2Numx(w);
        h = ratio2Numy(h);
        if(w < 180) w = 180;
        if(h < 180) h = 180;
        //再而是rc行列数
        int r = matchInt(strings[2], "r(\\d+)");
        int c = matchInt(strings[2], "c(\\d+)");
        //最后输出
        return getXYPoints(x, y, h, w, r, c);
    }

    /**
     * 获取网格当中所有格子的左上角坐标数组。
     * @param x 网格最左上角x坐标
     * @param y 网格最左上角y坐标
     * @param w 网格总宽度
     * @param h 网格总高度
     * @param r 网格总行数
     * @param c 网格总列数
     * @return 坐标数组元素各层级为【行】【列】【x或y】
     */
    private static int[][][] getXYPoints(int x, int y, int h, int w, int r, int c){
        int[][][] res = new int[r][c][2];
        float colspacing = (float) w / c;
        float rowspacing = (float) h / r;
        for(int row = 0; row < r; row++){
            for(int col = 0; col < c; col++){
                res[row][col][0] = (int) (x + col * colspacing);
                res[row][col][1] = (int) (y + row * rowspacing);
            }
        }
        return res;
    }

}
