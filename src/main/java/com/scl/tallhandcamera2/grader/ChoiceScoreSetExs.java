package com.scl.tallhandcamera2.grader;

import static com.scl.tallhandcamera2.THTools.ceilDiv;
import static com.scl.tallhandcamera2.THTools.matchInt;
import static com.scl.tallhandcamera2.THTools.matchInteger;
import static com.scl.tallhandcamera2.THTools.matchStr;

import androidx.annotation.NonNull;

/**
 * @author 作者：涉川良 联系方式：QQ470707134
 * @date 2025/5/24
 * @Description
 * 选择题给分时，按照以下三个数量给分：<br>
 * 【对选】对了的选项，考生已选；<br>
 * 【漏选】对了的选项，考生未选；<br>
 * 【错选】错了的选项，考生已选。<br>
 * 给分表达式的完整格式：【生效题量q】【:】【扣分下限min】【~】【得分上限max】【得分式】【扣分式】<br>
 * 解释：总共有q道题使用该设定，按得分式和扣分式计算分数。得分式和扣分式都可以忽略。<br>
 * 上下限如果忽略不写，代表无限（~必须写）。得分范围为0到max，扣分范围为-min到0。min前面不可以添加-号！<br>
 * 【得分式】有如下三种：<br>
 *      1.经典式：【+或-】【单元分c】对一个答案+c分，或漏一个答案-c分<br>
 *      2.二阶式：【%】【半对分h】针对所谓的半对（即有对选、有漏选、无错选），给h分<br>
 *      3.多阶式：【/】【第1阶】【/】【第2阶】【/】【第3阶】【/】【第4阶】【/】...【/】【第n阶】<br>
 *      每一阶都用“单元阶式”描述，允许不按顺序写出单元阶式。<br>
 *      以上给出的n代表“阶”数，每一阶的阶数就等于正确选项的总个数。<br>
 *      单元阶式格式：【c1】【c2】【c3】...【cn】<br>
 *      每个单元阶式一定有n个对选给分c，每个c都是一个数字，<br>
 *      其位数d与max相同（如果省略了max，则默认d=1），不足d位则前置补0到足够d位。<br>
 *      例如：10:0~4/4/24/124/1234代表接下来十道题都使用该设定。<br>
 *      最高给分4分，最低0分，有错选一律0分，其中：<br>
 *      第一阶：该题只有1个正确选项，选对一个得4分；
 *      第二阶：该题只有2个正确选项，选对一个得2分，选对两个得4分；<br>
 *      第三阶：该题只有3个正确选项，选对一个得1分，选对两个得2分，选对三个得4分；<br>
 *      第四阶：该题共有4个正确选项，选对一个得1分，选对两个得2分，选对三个得3分，选对四个得4分。<br>
 *      多阶式可以囊括几乎所有评分形式。然而，由于写法繁琐，应该仅在多选题或不定项选择题中使用它。<br>
 * 【扣分式】有如下几种：<br>
 * （注：扣分有可能无视0分下限，但不能低于-min。倒扣分常见于某些竞赛题）<br>
 *      1.有错无视：【*】没有这个符号时，有错选会将该题重置为0分再进行扣分。有*则取消该功能。<br>
 *      *可与以下任一类型共用，写在其前面或后面，或单独使用。<br>
 *      据此，得分一定是从0开始计算得分，而扣分则在无*时从0开始计算，有*时则在得分后开始计算扣分。<br>
 *      2.有错终扣：【@】【倒扣分d】代表无论有几个错选，直接扣d分。<br>
 *      3.错一扣一：【#】【倒扣分d】代表每错选一个，就扣d分。<br>
 */
public class ChoiceScoreSetExs {
    private enum DeductionMode{
        IGNORE,
        ALL,
        EVERY
    }

    public String scoreSetsString;
    private int questionsCount;
    private int scoreSetsCount;
    private ScoreSetFx[] scoreSetFxs;

    /**
     * 只给出题量的构造函数。用于首次初始化网格时，构建默认给分表达式，以不至于为空
     * @param questionsCount
     */
    public ChoiceScoreSetExs(int questionsCount){
        this.questionsCount = questionsCount;
        this.scoreSetsString = questionsCount + ":0~2";
    }

    /**
     * 有给分表达式、有题量的构造函数。用于第二次及之后初始化网格时。必先进行给分表达式的格式化。
     * @param scoreSetExsStr
     * @param questionsCount
     */
    public ChoiceScoreSetExs(String scoreSetExsStr, int questionsCount){
        this.questionsCount = questionsCount;
        this.scoreSetsString = formatScoreSets(scoreSetExsStr);
    }

    /**
     * 分析给分表达式并构建给分函数。
     * 注意：一次性地使用该方法之后，方可利用getScore计分
     */
    public void analyzeScoreSets(){
        String[] scoreSets = scoreSetsString.split(",");
        scoreSetFxs = new ScoreSetFx[scoreSetsCount];
        int i = 0;
        int effectiveCount = 0;
        String logo;
        for(String scoreSet : scoreSets){
            effectiveCount += matchInt(scoreSet, "(\\d+):");
            logo = matchStr(scoreSet, "([+\\-%/])");
            ScoreSetFx scoreSetFx;
            switch (logo){
                case "+":
                case "-":
                    scoreSetFx = new ClassicFx(scoreSet);
                    break;
                case "%":
                    scoreSetFx = new Order2Fx(scoreSet);
                    break;
                case "/":
                    scoreSetFx = new OrderXFx(scoreSet);
                    break;
                default:
                    //""，即短格式
                    scoreSetFx = new ShortFx(scoreSet);
                    break;
            }
            for (;i < effectiveCount; i++){
                scoreSetFxs[i] = scoreSetFx;
            }
        }
    }

    /**
     * 计算某一题的最终得分。
     * @param quesNum 从0开始的题号
     * @param bingoCount 对选数量
     * @param missingCount 漏选数量
     * @param wrongCount 错选数量
     * @return 最终得分
     */
    public int getScore(int quesNum, int bingoCount, int missingCount, int wrongCount){
        return scoreSetFxs[quesNum].getScore(bingoCount, missingCount, wrongCount);
    }

    /**
     * 将单个给分表达式格式化。
     * @param input 传入的给分表达式字符串，只有:后面的部分（不包括:）
     * @return 格式化后的字符串。
     */
    private String formatScoreSet(@NonNull String input){
        //找出头部的给分上下限
        Integer min = matchInteger(input, "(\\d+)[~～]");
        Integer max = matchInteger(input, "[~～](\\d+)");
        String head = (min == null ? "" : min.toString()) + "~" + (max == null ? "" : max.toString());
        //找出尾部的扣分式，将其格式化并提取
        String tail = matchStr(input, "([@#]\\d*)");
        //在tail中：@、#带数字的将被记录；不带数字的@、#也被记录；其他情况，都将记录为空字符串。
        //如果是不带数字的@、#，则自动加上0
        if(tail.equals("@") || tail.equals("#")) tail += "0";
        //寻找x并记录
        if(input.contains("*")){
            tail = "*" + tail;
        }
        //找出得分式。首先寻找经典式或二阶式。
        String body = matchStr(input, "([+\\-%]\\d+)");
        if(!body.isEmpty()){
            return head + body + tail;
        }
        //寻找多阶式。
        body = matchStr(input, "((/\\d+)+)");
        if(!body.isEmpty()){
            int digits;
            if(max == null){
                digits = 1;
            }else{
                digits = max.toString().length();
            }
            //分割body为单元阶式数组
            String[] cellsStrs = body.substring(1).split("/");
            //找出最长的单元阶式长度
            int longest = 0;
            for(String cellsStr : cellsStrs){
                if(cellsStr.length() > longest){
                    longest = cellsStr.length();
                }
            }
            //得出最高阶数
            int n = ceilDiv(longest, digits);
            //接下来对单元阶式进行排序去重和位数校正。需新建一个数组用于储存结果数据。
            String[] cellsStrings = new String[n + 1];
            int i;
            for(String cellsStr : cellsStrs){
                if(cellsStr.isEmpty()) continue;
                i = ceilDiv(cellsStr.length(), digits);
                cellsStrings[i] = String.format("%" + i * digits + "s", cellsStr).replaceAll(" ", "0");
            }
            //此时cellsStrings是一个离散的数组（即存在null值）。
            body = "/" + String.join("/", cellsStrings).replaceAll("null/", "");
            return head + body + tail;
        }
        //这个是短格式。短格式不能没有max。
        return (min==null?"":min.toString())+"~"+(max==null?"2":max.toString())+tail;
    }

    /**
     * 对一系列给分表达式进行格式化。
     * @param input 用逗号分隔的多个给分表达式连接而成的字符串。
     * @return
     */
    public String formatScoreSets(@NonNull String input){
        //获取多个给分表达式组成的数组。
        String[] scoreSetStrs = input.replaceAll("[^0-9:：~～+\\-%/*@#,，\\r\\n]+", "").split("[,，\\r\\n]");
        int effectiveCount;
        scoreSetsCount = 0;
        String tmp;
        StringBuilder res = new StringBuilder();
        for(String scoreSetStr : scoreSetStrs){
            effectiveCount = matchInt(scoreSetStr, "(\\d+)[:：]");
            tmp = matchStr(scoreSetStr, "[:：](.+)");
            res.append(effectiveCount).append(":").append(formatScoreSet(tmp)).append(",");
            scoreSetsCount += effectiveCount;
        }
        if(scoreSetsCount < questionsCount){
            res.append(questionsCount - scoreSetsCount).append(":0~2");
            scoreSetsCount = questionsCount;
        }else{
            //给分表达式可以比题目多。多出的表达式是不会被用到的。
            res.deleteCharAt(res.length() - 1);//把逗号删了
        }
        return res.toString();
    }

    private abstract class ScoreSetFx{
        protected Integer minScore;
        protected Integer maxScore;
        protected int deductionScore = 0;
        protected boolean wrongTake0 = true;
        protected DeductionMode deductionMode = DeductionMode.IGNORE;
        public ScoreSetFx(String scoreSet){
            minScore = matchInteger(scoreSet, "(\\d+)~");
            maxScore = matchInteger(scoreSet, "~(\\d+)");
            Integer d;
            do{
                d = matchInteger(scoreSet, "@(\\d+)");
                if(d != null){
                    deductionMode = DeductionMode.ALL;
                    deductionScore = d;
                    break;
                }
                d = matchInteger(scoreSet, "#(\\d+)");
                if(d != null) {
                    deductionMode = DeductionMode.EVERY;
                    deductionScore = d;
                    break;
                }
                //deductionMode = DeductionMode.IGNORE;
                //deductionScore = 0;
            }while(false);
            wrongTake0 = !scoreSet.contains("*");
        }

        public int deduction(int wrongCount, int resultScore){
            if(wrongCount <= 0){
                return resultScore;
            }
            if(wrongTake0){
                resultScore = 0;
            }
            switch (deductionMode){
                case ALL:
                    return resultScore - deductionScore;
                case EVERY:
                    return resultScore - deductionScore * wrongCount;
                default:
                    //IGNORE
                    return resultScore;
            }
        }

        public abstract int getScore(int bingoCount, int missingCount, int wrongCount);
    }

    private class ClassicFx extends ScoreSetFx{
        private final int cellScore;
        public ClassicFx(String scoreSet){
            super(scoreSet);
            cellScore = matchInt(scoreSet, "([+\\-]\\d+)");
        }

        @Override
        public int getScore(int bingoCount, int missingCount, int wrongCount) {
            int res = 0;
            if(cellScore > 0){
                //小题分是正数，即对一得cellScore
                res = bingoCount * cellScore;
                if(res > maxScore){
                    res = maxScore;
                }
            }else{
                //小题分为负数（0虽然包括在内但写了0是不报错的），即漏一扣cellScore
                res = maxScore + (missingCount * cellScore);
                if(res < 0){
                    res = 0;
                }
            }
            return deduction(wrongCount, res);
        }
    }

    private class Order2Fx extends ScoreSetFx{
        private final int halfScore;
        public Order2Fx(String scoreSet){
            super(scoreSet);
            halfScore = matchInt(scoreSet, "\\%(\\d+)");
        }

        @Override
        public int getScore(int bingoCount, int missingCount, int wrongCount) {
            int res = 0;
            if(bingoCount > 0){
                if(missingCount <= 0){
                    res = maxScore;
                }else{
                    res = halfScore;
                }
            }
            return deduction(wrongCount, res);
        }
    }

    private class OrderXFx extends ScoreSetFx{
        //cellScores的格式：第0个元素为第一阶；第1个元素为第二阶；第2个元素为第三阶；依此类推。
        private final int[][] cellScores;
        public OrderXFx(String scoreSet){
            super(scoreSet);
            //获取多个单元阶式
            String[] cellsStrs = matchStr(scoreSet, "((/\\d+)+)").substring(1).split("/");
            //计算单元分位数
            int digits;
            if(maxScore == null){
                digits = 1;
            }else{
                digits = maxScore.toString().length();
            }
            //找出最大阶数，用于确认cellScores长度
            int n = cellsStrs[cellsStrs.length - 1].length() / digits;
            cellScores = new int[n][n];
            //遍历所有单元阶式，提取单元分
            int start;
            String cell;
            for(String cellsStr : cellsStrs){
                //先确定阶数
                n = cellsStr.length() / digits;
                for(int i = 0; i < n; i++){
                    //每次取digits个字符
                    start = i * digits;
                    cell = cellsStr.substring(start, start + digits);
                    cellScores[n - 1][i] = Integer.parseInt(cell);
                }
            }
        }

        @Override
        public int getScore(int bingoCount, int missingCount, int wrongCount) {
            //求阶
            int n = bingoCount + missingCount;
            //取出分数
            int res = cellScores[n - 1][bingoCount - 1];
            return deduction(wrongCount, res);
        }
    }

    private class ShortFx extends ScoreSetFx{

        public ShortFx(String scoreSet) {
            super(scoreSet);
        }

        @Override
        public int getScore(int bingoCount, int missingCount, int wrongCount) {
            int res = 0;
            if(missingCount <= 0 && bingoCount > 0) {
                res = maxScore;
            }
            return deduction(wrongCount, res);
        }
    }
}
