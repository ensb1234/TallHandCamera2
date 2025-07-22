package com.scl.tallhandcamera2.grader;

import android.content.Intent;

import com.scl.tallhandcamera2.Camera2Activity;
import com.scl.tallhandcamera2.SettingsFragment;
import com.scl.tallhandcamera2.THEvents;
import com.scl.tallhandcamera2.THTools;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author 作者：涉川良 联系方式：QQ470707134
 * @date 2025/4/12
 * @Description
 */
public class ChoicesGrader {

    public enum ScanningMode {
        COUNT_CONTOURS, //数轮廓法
        CALC_AREA //面积法
    }
    public enum BlackBlockReasoningMode{
        ADJACENT_DIFF,
        COMPARE
    }
    private final Scalar SCALAR_GREEN = new Scalar(0, 255, 0);


    private Camera2Activity ca;
    private int currentFrame = 20;
    private int scanningInterval = 20;
    private int referenceWidth = 180;
    private int referenceHeight = 180;
    private int protoWidth = 180;
    private int protoHeight = 180;
    private int protoX = 0;
    private int protoY = 0;
    private ScanningMode scanningMode = ScanningMode.CALC_AREA;
    private int blockSize = 37;
    private float C = 7.0f;
    private int picSimilarTolerance = 10;
    private long referenceHash = 0;
    private BlackBlockReasoningMode blackBlockReasoningMode = BlackBlockReasoningMode.ADJACENT_DIFF;
    private int adjacentDiffStartingIndex = 3;
    private int noUseStepMeaning = 1;
    private float firstStepMultiple = 1.2f;

    private MatOfPoint contour2;
    private int frameWidth;
    private int frameHeight;
    private TwoRect twoRect;

    public ChoicesGrader(Camera2Activity ca){
        this.ca = ca;
    }

    /**
     * 从intent中获取数据并初始化这个阅卷器。
     * @param intent 从CvCamera2Listener传来的intent
     */
    public void init(Intent intent){
        scanningInterval = intent.getIntExtra(SettingsFragment.SCANNING_INTERVAL, 20);
        currentFrame = scanningInterval;
        referenceWidth = intent.getIntExtra(SettingsFragment.REFERENCE_WIDTH, 180);
        referenceHeight = intent.getIntExtra(SettingsFragment.REFERENCE_HEIGHT, 180);
        protoWidth = intent.getIntExtra(SettingsFragment.PROTO_WIDTH, 180);
        protoHeight = intent.getIntExtra(SettingsFragment.PROTO_HEIGHT, 180);
        protoX = intent.getIntExtra(SettingsFragment.PROTO_X, 0);
        protoY = intent.getIntExtra(SettingsFragment.PROTO_Y, 0);
        scanningMode = ScanningMode.valueOf(intent.getStringExtra(SettingsFragment.SCANNING_MODE));
        blockSize = intent.getIntExtra(SettingsFragment.BLOCK_SIZE, 37);
        C = intent.getFloatExtra(SettingsFragment.C, 7.0f);
        picSimilarTolerance = intent.getIntExtra(SettingsFragment.PIC_SIMILAR_TOLERANCE, 10);
        referenceHash = Long.parseUnsignedLong(intent.getStringExtra(SettingsFragment.REFERENCE_HASH), 16);
        blackBlockReasoningMode = BlackBlockReasoningMode.valueOf(intent.getStringExtra(SettingsFragment.BLACK_BLOCK_REASONING_MODE));
        adjacentDiffStartingIndex = intent.getIntExtra(SettingsFragment.ADJACENT_DIFF_STARTING_INDEX, 3);
        noUseStepMeaning = intent.getIntExtra(SettingsFragment.NO_USE_STEP_MEANING, 1);
        firstStepMultiple = intent.getFloatExtra(SettingsFragment.FIRST_STEP_MULTIPLE, 1.2f);

        THEvents.getInstance().addTHEventListener(new THEvents.CameraSizeConfirmListener() {
            @Override
            public void handleEvent(THEvents.CameraSizeConfirmEvent event) {
                THEvents.getInstance().removeTHEventListener(this);
                frameWidth = event.getWidth();
                frameHeight = event.getHeight();
                twoRect = new TwoRect(
                        frameWidth, frameHeight,
                        referenceWidth, referenceHeight,
                        protoX, protoY,
                        protoWidth, protoHeight);
            }
        });
    }

    public Mat grade(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        //每20帧即每秒扫描一次，降低资源消耗。
        Mat rgba = inputFrame.rgba();
        while (currentFrame >= 20){
            currentFrame = 0;
            //当前帧画面二值化
            Mat gray = inputFrame.gray();
            Mat threshold = new Mat();
            Imgproc.adaptiveThreshold(gray, threshold, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, blockSize, C);
            gray.release();
            //找出所有轮廓
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(threshold,contours,hierarchy,Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);
            //全黑屏时只有一个轮廓，为防止继续运行下去导致闪退，应该中止解析。
            if(contours.size() <= 1){
                threshold.release();
                hierarchy.release();
                for(MatOfPoint contour : contours){
                    contour.release();
                }
                contours.clear();
                break;
            }
            //找出参照图轮廓id。在相机画面中，第一大的轮廓应该是全画面的最外周，第二大的轮廓就是我们的目标。
            int referenceId = 0;
            if(scanningMode == ScanningMode.COUNT_CONTOURS){
                //数轮廓法：搜索包含子轮廓第二多的大轮廓。
                HashMap<Integer,Integer> contoursChildCounts = getChildCountOfContours(hierarchy);
                List<Map.Entry<Integer,Integer>> list = THTools.HashMapSort(contoursChildCounts);
                if(list.size() < 3){
                    threshold.release();
                    hierarchy.release();
                    for(MatOfPoint contour : contours){
                        contour.release();
                    }
                    contours.clear();
                    break;
                }
                referenceId = list.get(1).getKey();
            }else if(scanningMode == ScanningMode.CALC_AREA){
                //面积法：搜索面积第二大的轮廓。
                HashMap<Integer,Double> contoursChildCounts = getAreaOfContours(contours);
                List<Map.Entry<Integer,Double>> list = THTools.HashMapSort(contoursChildCounts);
                if(list.size() < 3){
                    threshold.release();
                    hierarchy.release();
                    for(MatOfPoint contour : contours){
                        contour.release();
                    }
                    contours.clear();
                    break;
                }
                referenceId = list.get(1).getKey();
            }
            //先清空contour2。这个变量用于存储应当显示的轮廓范围（在非扫描帧时）。
            if(contour2 != null){
                contour2.release();
            }
            //获取参照图轮廓。
            contour2 = contours.get(referenceId);
            //为参照图轮廓拟合四边形
            double girth = 0.0d;
            MatOfPoint2f contour2f = new MatOfPoint2f(contour2.toArray());
            MatOfPoint2f approxCurve = new MatOfPoint2f();
            double p = 0.02;
            do{
                girth = Imgproc.arcLength(contour2f,true) * p;
                approxCurve.release();
                approxCurve = new MatOfPoint2f();
                Imgproc.approxPolyDP(contour2f,approxCurve,girth,true);
                p *= 1.5;
            }while(approxCurve.toArray().length > 4);
            //把参照图轮廓转换为四角轮廓。这里不需要担心后面的release问题。
            //因为原来的contour2存储的对象位于contours当中，而最后会遍历删除contours中的所有内容。
            contour2 = new MatOfPoint(approxCurve.toArray());
            if(approxCurve.toArray().length == 4){
                THTools.log("开始扫描...");
                //如果拟合出四边形了，就尝试扫描答案
                if(ca.getBtnMiddleMode() == Camera2Activity.BtnMiddleMode.GET_PROTO && ca.btnMiddlePressed){
                    //如果按下了拍照按钮，这里将其记录为参照图。
                    //将按钮状态重置为未按下。
                    ca.btnMiddlePressed = false;
                    //对应已扫描的轮廓四角到即将摆正的参照图四角。
                    int[] newPointsPos = guessFourCorners(approxCurve);
                    //截取参照图。
                    Mat referenceMat = setYesAndCutReference(threshold, approxCurve, newPointsPos);
                    //记录参照图hash
                    referenceHash = aHash(referenceMat);
                    SettingsFragment.ed.putString(SettingsFragment.REFERENCE_HASH, String.format("%016X", referenceHash));
                    SettingsFragment.ed.apply();
                    //截取原型图（答案所在区域）
                    Mat protoMat = setYesAndCutProto(threshold, approxCurve, newPointsPos);
                    //显示原型图，即答案区域
                    ca.showCorrectPaper(protoMat);
                    //打完收工
                    referenceMat.release();
                    protoMat.release();
                }else if((ca.getBtnMiddleMode() == Camera2Activity.BtnMiddleMode.AUTO_IDENTIFY || (ca.getBtnMiddleMode() == Camera2Activity.BtnMiddleMode.MANUAL_IDENTIFY && ca.btnMiddlePressed)) && referenceHash != 0){
                    //如果正在扫描，就与拍下的参照图比较，得出相似度
                    //重置按钮状态。
                    ca.btnMiddlePressed = false;
                    //对应已扫描的轮廓四角到即将摆正的参照图四角。
                    int[] newPointsPos = guessFourCorners(approxCurve);
                    //截取参照图。
                    Mat referenceMat = setYesAndCutReference(threshold, approxCurve, newPointsPos);
                    //比较参照图hash
                    long hash = aHash(referenceMat) ^ referenceHash;
                    //计算汉明距离
                    int hammingDistance = Long.bitCount(hash);
                    //比较汉明距离与相似图容忍度。
                    if(hammingDistance < picSimilarTolerance){
                        //证明两个图片属于相似范围内
                        //截取原型图（答案所在区域）
                        Mat filledMat = setYesAndCutProto(threshold, approxCurve, newPointsPos);
                        //显示原型图，即答案区域
                        ca.showTesterPaper(filledMat);
                        ca.setTips(findOutBlackGrids(filledMat, ca.getAnsXYs()));
                        filledMat.release();
                    }
                    referenceMat.release();
                }
            }
            threshold.release();
            hierarchy.release();
            for(MatOfPoint contour : contours){
                contour.release();
            }
            contours.clear();
            contour2f.release();
            approxCurve.release();
            break;
        }
        currentFrame++;
        if(contour2 != null){
            List<MatOfPoint> contours2 = new ArrayList<>();
            contours2.add(contour2);
            Imgproc.drawContours(rgba,contours2,0,SCALAR_GREEN,12);
        }
        return rgba;
    }

    /**
     * 用于获取包含所有轮廓的子轮廓个数的键值对。
     * @param hierarchy 从findContours方法获取的Mat，一般的Mat不能用在这里。
     * @return 键为轮廓id；值为该轮廓的子轮廓总个数。
     */
    private HashMap<Integer,Integer> getChildCountOfContours(Mat hierarchy){
        //hierarchy相当于多列一行的数组。
        //存储所有轮廓对应的父轮廓
        int length = hierarchy.cols();
        int[] parents = new int[length];
        for(int id = 0; id < length; id++){
            parents[id] = (int) hierarchy.get(0, id)[3];
        }
        //遍历所有轮廓，并上溯所有祖轮廓，每一代父轮廓都记录+1
        //最终得到的hashmap中，键为轮廓id，值为该轮廓的子孙轮廓数量合计。
        HashMap<Integer, Integer> relative = new HashMap<>();
        int pid = 0;
        int count = 0;
        for(int id = 0; id < length; id++){
            pid = parents[id];
            while(pid > -1) {
                if(null == relative.get(pid)){
                    relative.put(pid, 1);
                }else{
                    count = relative.get(pid) + 1;
                    relative.put(pid, count);
                }
                pid = parents[pid];
            }
        }
        return relative;
    }

    /**
     * 用于获取最大面积的轮廓。
     * @param contours 从findContours方法获取的所有轮廓数据。
     * @return 键为轮廓id；值为该轮廓的面积。
     */
    private HashMap<Integer, Double> getAreaOfContours(List<MatOfPoint> contours){
        HashMap<Integer,Double> contourAreas = new HashMap<>();
        double S = 0;
        for(int i = contours.size() - 1; i >= 0; i--){
            S = Imgproc.contourArea(contours.get(i));
            contourAreas.put(i,S);
        }
        return contourAreas;
    }

    /**
     * 均值哈希法前置技能。其实就是个缩小8x8再二值化的过程。由于刚好64个1或0的像素，故最后存储为一个64位整数。
     * @param thresholdmat 要序列化的图片。
     * @return 代表该图片的一个hash值。用于对比含1量得知图片相似度。
     */
    private long aHash(Mat thresholdmat){
        Mat dst = new Mat();
        Imgproc.resize(thresholdmat, dst, new Size(8, 8), 0, 0, Imgproc.INTER_AREA);
        //求均值
        double average = Core.mean(thresholdmat).val[0];
        long hash = 0;
        for(int row = 0; row < 8; row++){
            for(int col = 0; col < 8; col++){
                if(dst.get(row, col)[0] > average){
                    hash = (hash << 1) + 1;
                }else{
                    hash = (hash << 1);
                }
            }
        }
        dst.release();
        return hash;
    }

    /**
     * 猜测结果点位与给出轮廓四个角点的对应关系（不要拍太歪一般可以成功）
     * @param approxCurve 给出的轮廓。必须为四边形的四个角。
     * @return 结果四个角的点位。
     */
    private static int[] guessFourCorners(MatOfPoint2f approxCurve){
        //点位图：（建议拍照时，画面平行于拍摄对象，且尽量正视）
        //0,0——w,0
        // |    |
        //0,h——w,h
        //0——3
        //|  |
        //1——2
        //猜测结果点位与给出轮廓四个角点的对应关系（不要拍太歪一般可以成功）
        Point[] arr = approxCurve.toArray();
        double[] xs = {arr[0].x, arr[1].x, arr[2].x, arr[3].x};
        double[] ys = {arr[0].y, arr[1].y, arr[2].y, arr[3].y};
        //sort是升序
        Arrays.sort(xs);
        Arrays.sort(ys);
        int[] res = new int[4];
        for(int i = 0; i < 4; i++){
            if(Arrays.binarySearch(xs, arr[i].x) < 2){
                //该点的x坐标靠前，是方框的左边，可能是左上点或左下点
                if(Arrays.binarySearch(ys, arr[i].y) < 2){
                    //该点的y坐标靠上，是方框的上边，可能是左上点或右上点
                    //综上为左上点
                    res[i] = 0;
                }else{
                    res[i] = 1;
                }
            }else{
                if(Arrays.binarySearch(ys, arr[i].y) < 2){
                    res[i] = 3;
                }else{
                    res[i] = 2;
                }
            }
        }
        return res;
    }

    /**
     * 平面转换，截取相应位置并缩放到新尺寸
     * @param originMat
     * @param approxCurve
     * @param newPoints
     * @param newW
     * @param newH
     * @return
     */
    private Mat setYesAndCut(Mat originMat, MatOfPoint2f approxCurve, MatOfPoint2f newPoints, int newW, int newH){
        //平面转换并缩放到新尺寸
        Mat m = Imgproc.getPerspectiveTransform(approxCurve, newPoints);
        Mat res0 = new Mat();
        Imgproc.warpPerspective(originMat, res0, m, new Size(newW, newH));
        //再次二值化
        Mat res = new Mat();
        Imgproc.adaptiveThreshold(res0, res, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, blockSize, C);
        //打完收工
        res0.release();
        m.release();
        return res;
    }

    /**
     * 猜测四边形轮廓的各个角点对应的实际位置，将图片摆正并剪切到适合于容器的大小。
     * @param originMat 来自摄像头拍下来的一帧源图像，并且已经二值化。
     * @param approxCurve 已经扫描出来的四边形轮廓。
     * @return 摆正剪切后的图片。
     */
    private Mat setYesAndCutReference(Mat originMat, MatOfPoint2f approxCurve, int[] newPointsPosition){
        MatOfPoint2f newPoints = twoRect.getFourCornerPointsOfReference(newPointsPosition);
        Mat res = setYesAndCut(originMat, approxCurve, newPoints, twoRect.rw0, twoRect.rh0);
        newPoints.release();
        return res;
    }

    private Mat setYesAndCutProto(Mat originMat, MatOfPoint2f approxCurve, int[] newPointsPosition){
        //先按模板轮廓摆正，并截取twoRect范围中的内容
        MatOfPoint2f newPoints = twoRect.getFourCornerPointsOfReferenceInTwoRect(newPointsPosition);
        Mat circumRect = setYesAndCut(originMat, approxCurve, newPoints, twoRect.cw, twoRect.ch);
        Mat protoRect = circumRect.submat(twoRect.protoRect);
        Mat res0 = protoRect.clone();
        int newW,newH;
        newW = Camera2Activity.winContentHeight * twoRect.pw / twoRect.ph;
        newH = Camera2Activity.winContentHeight;
        if(newW > Camera2Activity.winContentWidth){
            newH = Camera2Activity.winContentWidth * twoRect.ph / twoRect.pw;
            newW = Camera2Activity.winContentWidth;
        }
        Mat res = new Mat();
        Imgproc.resize(res0, res, new Size(newW, newH));
        newPoints.release();
        circumRect.release();
        protoRect.release();
        res0.release();
        return res;
    }

    /**
     * 在二值化图片中，扫描xys定义的网格，找出已经涂黑的格子。
     * @param mat 源图片。已经二值化过。
     * @param xysList 要扫描的所有网格的左上角坐标。
     * @return 包含各个答案网格的数组，每个元素就是该网格中被涂黑的格子组成的数组。第一个格子序号为0，右边为1、2、3...等，换行继续，与GreatGridView.answers相同设定
     */
    private List<HashSet<Integer>> findOutBlackGrids(Mat mat, List<int[][][]> xysList){
        if (Objects.requireNonNull(blackBlockReasoningMode) == BlackBlockReasoningMode.COMPARE) {
            return compare(mat, xysList);
        }//ADJACENT_DIFF
        return adjacentDiff(mat, xysList);
    }

    private List<HashSet<Integer>> compare(Mat mat, List<int[][][]> xysList){
        int j, cellsCount;
        List<HashSet<Integer>> res = new ArrayList<>();

        for(int i = 0; i < xysList.size(); i++){
            int[] testerBlackCountHashMap = blackPixelCount(mat, xysList.get(i));
            int[] protoBlackCountHashMap = ca.protoBlackCountHashMaps[i];
            cellsCount = testerBlackCountHashMap.length;
            if(cellsCount != protoBlackCountHashMap.length){
                THTools.log("这不对吧！");
                return res;
            }
            HashSet<Integer> answers = new HashSet<>();
            for(j = 0; j < cellsCount; j++){
                if(testerBlackCountHashMap[j] > protoBlackCountHashMap[j] * firstStepMultiple){
                    answers.add(j);
                }
            }
            res.add(answers);
        }
        return res;
    }

    private List<HashSet<Integer>> adjacentDiff(Mat mat, List<int[][][]> xysList){
        int w, h, rowsCount, colsCount, num, j;
        List<HashSet<Integer>> res = new ArrayList<>();
        for(int i = 0; i < xysList.size(); i++){
            HashMap<Integer, Integer> hashMapBlackCount = new HashMap<>();//用于储存每个格子的黑点数（黑色像素总计数）。第一个整数为格子序号，第二个为黑点数。
            int[][][] xys = xysList.get(i);
            rowsCount = xys.length;
            //计算出网格的大小。
            w = xys[0][1][0] - xys[0][0][0];
            h = xys[1][0][1] - xys[0][0][1];
            num = 0;//正在扫描的格子序号
            for(int r = 0; r < rowsCount; r++){
                int[][] grids = xys[r];
                colsCount = grids.length;
                for(int c = 0; c < colsCount; c++){
                    //定义格子方框
                    Rect roi = new Rect(grids[c][0], grids[c][1], w, h);
                    //截取方框内容
                    Mat roiMat = mat.submat(roi);
                    //计算黑色像素个数并存储
                    hashMapBlackCount.put(num, (int)roiMat.total() - Core.countNonZero(roiMat));
                    //更新正在扫描的格子序号
                    num++;
                    roiMat.release();
                }
            }
            //这里使用自识别（即不借助空题照片，而是自行判断涂黑的格子）
            //以下这套自识别的方法，我称之为“升序第3号起始首个较大邻差划分法”，简称邻差法。
            List<Map.Entry<Integer,Integer>> listBlackCount = THTools.HashMapSort(hashMapBlackCount);//降序
            Collections.reverse(listBlackCount);//升序
            //此时num刚好是最大的格子序号+1，即格子总数，或hashMap/list的长度
            //默认3号起始，忽略前三个格子。主要是防止考生用力擦除之后导致黑色像素少而误判。这个数字可以设计为答案网格自定义属性。
            //同理，邻差倍数也可以设计为自定义。此处默认为1.2
            for(j = adjacentDiffStartingIndex; j < num; j++){
                if((listBlackCount.get(j).getValue() > listBlackCount.get(j - 1).getValue() * firstStepMultiple)
                        && (listBlackCount.get(j).getValue() > noUseStepMeaning)) {
                    break;
                }
            }
            //使用subList方法获取涂黑的格子
            List<Map.Entry<Integer, Integer>> subList = listBlackCount.subList(j, num);
            //提取出黑格子序号并成为一个HashSet
            HashSet<Integer> keysSet = new HashSet<>();
            for (Map.Entry<Integer, Integer> entry : subList) {
                keysSet.add(entry.getKey());
            }
            res.add(keysSet);
        }
        return res;
    }

    public static int[] blackPixelCount(Mat mat, int[][][] xys){
        int w, h, rowsCount, colsCount, num;
        rowsCount = xys.length;
        colsCount = xys[0].length;
        int[] res = new int[rowsCount * colsCount];
        //计算出网格的大小。
        w = xys[0][1][0] - xys[0][0][0];
        h = xys[1][0][1] - xys[0][0][1];
        num = 0;//正在扫描的格子序号
        for(int r = 0; r < rowsCount; r++){
            int[][] grids = xys[r];
            //colsCount = grids.length;//网格每一行格子数都是相同的。
            for(int c = 0; c < colsCount; c++){
                //定义格子方框
                Rect roi = new Rect(grids[c][0], grids[c][1], w, h);
                //截取方框内容
                Mat roiMat = mat.submat(roi);
                //计算黑色像素个数并存储
                res[num] = (int)roiMat.total() - Core.countNonZero(roiMat);
                //更新正在扫描的格子序号
                num++;
                roiMat.release();
            }
        }
        return res;
    }
}
