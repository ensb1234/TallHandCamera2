package com.scl.tallhandcamera2.grader;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;

import java.util.Arrays;

public class TwoRect {

    //点位图：
    //0,0——w,0
    // |    |
    //0,h——w,h
    //0——3
    //|  |
    //1——2
    /**
     * 这个数组用于记录参照图矩形的四个角坐标。<br/>
     * 四个角的点位图如下：<br/>
     * 0,0——w,0<br/>
     * 0,h——w,h<br/>
     * 四个角对应的数组元素下标：<br/>
     * 0——3<br/>
     * 1——2<br/>
     */
    public Point[] fourCornerPointsOfReference = new Point[4];
    public Point[] fourCornerPointsOfReferenceInTwoRect = new Point[4];
    public int rx;
    public int ry;
    public int rh;
    public int rw;
    public int rh0;
    public int rw0;
    public int px;
    public int py;
    public int ph;
    public int pw;
    public Rect protoRect;
    public int cx;
    public int cy;
    public int ch;
    public int cw;

    public TwoRect(int container_w, int container_h, int reference_w, int reference_h, int proto_x, int proto_y, int proto_w, int proto_h){
        //找出包含两个矩形的最小外接矩形。
        int[] xs = {0, reference_w, proto_x, proto_x + proto_w};
        int[] ys = {0, reference_h, proto_y, proto_y + proto_h};
        Arrays.sort(xs);
        Arrays.sort(ys);
        cx = xs[0];
        cy = ys[0];
        cw = xs[3] - cx;
        ch = ys[3] - cy;
        //计算出外接矩形缩放后在容器中的最大尺寸
        AdaptedRect ar = getAdaptedWH(container_w, container_h, cw, ch);
        cw = ar.w;
        ch = ar.h;
        int ratio = ar.ratio;
        //将包含两个小矩形的外接矩形左上角移动到坐标轴0点处，计算出两个小矩形的坐标及尺寸，以及外接矩形的尺寸。
        rx = -cx * ratio;
        ry = -cy * ratio;
        px = (proto_x - cx) * ratio;
        py = (proto_y - cy) * ratio;
        cx = 0;
        cy = 0;
        rw = reference_w * ratio;
        rh = reference_h * ratio;
        pw = proto_w * ratio;
        ph = proto_h * ratio;
        protoRect = new Rect(px, py, pw, ph);
        fourCornerPointsOfReferenceInTwoRect[0] = new Point(rx, ry);
        fourCornerPointsOfReferenceInTwoRect[1] = new Point(rx, ry + rh);
        fourCornerPointsOfReferenceInTwoRect[2] = new Point(rx + rw, ry + rh);
        fourCornerPointsOfReferenceInTwoRect[3] = new Point(rx + rw, ry);
        //计算出参照图单独占据容器时的尺寸
        ar = getAdaptedWH(container_w, container_h, reference_w, reference_h);
        rw0 = ar.w;
        rh0 = ar.h;
        fourCornerPointsOfReference[0] = new Point(0, 0);
        fourCornerPointsOfReference[1] = new Point(0, rh0);
        fourCornerPointsOfReference[2] = new Point(rw0, rh0);
        fourCornerPointsOfReference[3] = new Point(rw0, 0);
    }

    public MatOfPoint2f getFourCornerPointsOfReference(int[] order){
        Point[] res = new Point[]{
                fourCornerPointsOfReference[order[0]],
                fourCornerPointsOfReference[order[1]],
                fourCornerPointsOfReference[order[2]],
                fourCornerPointsOfReference[order[3]]
        };
        return new MatOfPoint2f(res);
    }

    public MatOfPoint2f getFourCornerPointsOfReferenceInTwoRect(int[] order){
        Point[] res = new Point[]{
                fourCornerPointsOfReferenceInTwoRect[order[0]],
                fourCornerPointsOfReferenceInTwoRect[order[1]],
                fourCornerPointsOfReferenceInTwoRect[order[2]],
                fourCornerPointsOfReferenceInTwoRect[order[3]]
        };
        return new MatOfPoint2f(res);
    }

    public static AdaptedRect getAdaptedWH(int container_w, int container_h, int origin_w, int origin_h){
        int res_w, res_h, ratio;
        res_w = container_h * origin_w / origin_h;
        res_h = container_h;
        if(res_w > container_w){
            res_h = container_w * origin_h / origin_w;
            res_w = container_w;
        }
        ratio = res_w / origin_w;
        return new AdaptedRect(res_w, res_h, ratio);
    }

    public static class AdaptedRect{
        public int w;
        public int h;
        public int ratio;
        public AdaptedRect(int new_w, int new_h, int new_ratio){
            w = new_w;
            h = new_h;
            ratio = new_ratio;
        }
    }

    public static class RotatedTwoRect{
        public String rw;
        public String rh;
        public String pw;
        public String ph;
        public String px;
        public String py;
    }

    public static RotatedTwoRect rotate(String rw, String rh, String pw, String ph, String px, String py){
        RotatedTwoRect res = new RotatedTwoRect();
        try{
            res.rw = rh;
            res.rh = rw;
            res.pw = ph;
            res.ph = pw;
            res.px = py;
            res.py = String.valueOf(Integer.parseInt(rw) - Integer.parseInt(px) - Integer.parseInt(pw));//rw - px - pw;
        } catch (NumberFormatException e) {
            return null;
        }
        return res;
    }
}
