package com.scl.tallhandcamera2;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.content.res.Resources;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.io.ByteArrayOutputStream;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 作者：涉川良 联系方式：QQ470707134
 * @date 2025/4/8
 * @Description
 */
public class THTools {

    public static final String TAG = "Tall!Hand!";
    public static void switchVisible(View... views){
        for(var view : views){
            if(view.getVisibility() == GONE){
                view.setVisibility(VISIBLE);
            }else if(view.getVisibility() == VISIBLE){
                view.setVisibility(GONE);
            }
        }
    }

    /**
     * 给HashMap排序用。
     * @param hashmap 要排序的对象。
     * @return 降序后的list。get(0)为值最大的键值对。
     * @param <K> 键的类型。没有限制。
     * @param <V> 值的类型。必须为可比较的数字类型。
     */
    public static <K, V extends Comparable<? super V>> List<Map.Entry<K, V>> HashMapSort(HashMap<K, V> hashmap){
        List<Map.Entry<K, V>> list = new ArrayList<>(hashmap.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });
        return list;
    }

    /**
     * 用于将dp值转换为px
     * @param dp 要转换的dp值
     * @return 输出的px值。int格式。
     */
    public static int dp2px(int dp){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    }

    public static int ceilDiv(int a, int b){
        //return (a + b - 1) / b;
        return a / b + (a % b != 0 ? 1 : 0);
    }

    public static boolean isLandscape(Activity activity) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        return width > height; // 如果宽度大于高度，则为横屏
    }

    /**
     * 按给出的含一个捕获组的正则表达式，输出给定字符串中符合要求的数字。
     * @param str 源字符串。
     * @param regex 正则表达式。
     * @return 符合要求的数字。
     */
    public static int matchInt(String str, String regex){
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

    public static Integer matchInteger(String str, String regex){
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    /**
     * 按给出的含一个捕获组的正则表达式，输出给定字符串中符合要求的字符串。
     * @param str 源字符串。
     * @param regex 正则表达式。
     * @return 符合要求的字符串。
     */
    public static String matchStr(String str, String regex){
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    public interface CheckInt{
        boolean check(int num);
    }

    public static String checkIntString(String string, int defValue, CheckInt callBack){
        if(string == null) return defValue + "";
        int num;
        try {
            num = Integer.parseInt(string);
        }catch (Exception e){
            return defValue + "";
        }
        if(callBack.check(num)){
            return string;
        }else{
            return defValue + "";
        }
    }

    public interface CheckFloat{
        boolean check(float num);
    }

    public static String checkFloatString(String string, float defValue, CheckFloat callBack){
        if(string == null) return defValue + "";
        float num;
        try {
            num = Float.parseFloat(string);
        }catch (Exception e){
            return defValue + "";
        }
        if(callBack.check(num)){
            return string;
        }else{
            return defValue + "";
        }
    }

    public static String checkString(String string, String defValue, String regex){
        if(string.matches(regex)){
            return string;
        }else{
            return defValue;
        }
    }

    public static void log(String format, Object... args){
        Log.d(TAG, String.format(format, args));
    }


    public static class OnTouchButtonListener implements View.OnTouchListener{
        private final ObjectAnimator btnScaleDown;
        private final ObjectAnimator btnScaleUp;
        public OnTouchButtonListener(View v){
            super();
            btnScaleDown = ObjectAnimator.ofPropertyValuesHolder(
                    v,
                    PropertyValuesHolder.ofFloat("scaleX", 0.9f),
                    PropertyValuesHolder.ofFloat("scaleY", 0.9f)
            );
            btnScaleDown.setDuration(100);
            btnScaleUp = ObjectAnimator.ofPropertyValuesHolder(
                    v,
                    PropertyValuesHolder.ofFloat("scaleX", 1.0f),
                    PropertyValuesHolder.ofFloat("scaleY", 1.0f)
            );
            btnScaleUp.setDuration(100);
        }
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    btnScaleDown.start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    btnScaleUp.start();
                    break;
            }
            return false;
        }
    }

    public static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        // 将Bitmap压缩为PNG格式并写入ByteArrayOutputStream
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] bitmapData = byteArrayOutputStream.toByteArray();
        THTools.log("这个byte数组有%d这么大", bitmapData.length);
        String encodedImage = Base64.encodeToString(bitmapData, Base64.DEFAULT);
        return encodedImage;
    }

    public static void saveBitmapToInternalStorage(Activity activity, Bitmap bitmap, String filename) {
        try {
            // 使用openFileOutput方法创建或替换文件
            FileOutputStream fos = activity.openFileOutput(filename, MODE_PRIVATE);
            // 将Bitmap压缩成PNG格式，并写入到文件输出流
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            // 关闭输出流
            fos.close();
        } catch (Exception e) {
            THTools.log(e.toString());
        }
    }

    public static Bitmap getBitmapFromInternalStorage(Activity activity, String filename) {
        Bitmap bitmap = null;
        try {
            // 打开文件输入流
            FileInputStream fis = activity.openFileInput(filename);
            // 将输入流解码为bitmap
            bitmap = BitmapFactory.decodeStream(fis);
            // 关闭输入流
            fis.close();
        } catch (Exception e) {
            THTools.log(e.toString());
        }
        return bitmap;
    }
}
