package com.scl.tallhandcamera2;

import static com.scl.tallhandcamera2.THTools.matchInt;
import static com.scl.tallhandcamera2.THTools.matchStr;

import androidx.annotation.NonNull;

import com.scl.tallhandcamera2.grader.ChoiceScoreSetExs;

import java.util.Arrays;
import java.util.HashSet;

/**
 * @author 作者：涉川良 联系方式：QQ470707134
 * @date 2025/5/20
 * @Description
 */
public class ChoicesGridFeature {
    private static final String ANSWERS_26 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private boolean landScape;
    private int rowsCount;
    private int colsCount;
    private final HashSet<Integer> answers;
    public ChoiceScoreSetExs choiceScoreSetExs;
    private final String xywhrc;
    public String answersStr;

    public ChoicesGridFeature(String feature){
        //解析
        String[] strings = feature.split("_");
        if(strings.length != 5){
            strings = ("x0y0_w180h180_r3c4_P:A,B,CD_2:2+2x,3:4-2x").split("_");
        }
        xywhrc = String.format("%s_%s_%s", strings[0], strings[1], strings[2]);
        //再而是rc行列数
        rowsCount = matchInt(strings[2], "r(\\d+)");
        colsCount = matchInt(strings[2], "c(\\d+)");
        if(rowsCount < 1 || rowsCount > 26) rowsCount = 1;
        if(colsCount < 1 || colsCount > 26) colsCount = 1;
        //接着是各题答案，半角冒号前是横屏L竖屏P，冒号后每题答案用半角逗号隔开
        landScape = (strings[3].charAt(0) == 'L');
        //读取出由每题的答案组成的数组
        answersStr = matchStr(strings[3], ":(.+)");
        String[] questions_answers = answersStr.split(",");
        //将答案（ABCD...）转化为数字（格子序号），注意，当数组长度小于题量时，后面的答案会填充null
        answers = strs2answers(Arrays.copyOf(questions_answers, (landScape?rowsCount:colsCount)));
        choiceScoreSetExs = new ChoiceScoreSetExs(strings[4], landScape?rowsCount:colsCount);
    }

    /**
     * 将 由格子序号组成的所有题目答案 转化为字面可见的字符串数组。
     * @param answer_nums 可能是标准答案，也可能是客户给出的考生答案
     * @return 字面可见的字符串答案数组。每一题对应一个数组元素。
     */
    private String[] answers2Strs(HashSet<Integer> answer_nums){
        int ques_count = landScape?rowsCount:colsCount;
        String[] answer_strs = new String[ques_count];
        Arrays.fill(answer_strs, "");
        if(landScape){
            for(int i : answer_nums){
                answer_strs[(int)(i/colsCount)] += ANSWERS_26.charAt(i%colsCount);
            }
        }else{
            int new_num;
            for(int i : answer_nums){
                new_num = rowsCount - (int)(i/colsCount) - 1 + (i%colsCount) * rowsCount;
                answer_strs[(int)(new_num/rowsCount)] += ANSWERS_26.charAt(new_num%rowsCount);
            }
        }
        return answer_strs;
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

    @NonNull
    @Override
    public String toString(){
        return String.format("%s_%s:%s_%s",
                xywhrc,
                landScape?"L":"P",
                answersStr,
                choiceScoreSetExs.scoreSetsString);
    }

    public void answersRotate(){
        landScape = !landScape;
        answersStr = String.join(",", answers2Strs(answers));
    }

    public void setScoreSets(String score_sets_str){
        choiceScoreSetExs = new ChoiceScoreSetExs(score_sets_str, landScape?rowsCount:colsCount);
    }
}
