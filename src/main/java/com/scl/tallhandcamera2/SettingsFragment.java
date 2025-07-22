package com.scl.tallhandcamera2;

import static com.scl.tallhandcamera2.THTools.checkFloatString;
import static com.scl.tallhandcamera2.THTools.checkIntString;
import static com.scl.tallhandcamera2.THTools.checkString;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

import com.scl.tallhandcamera2.grader.TwoRect;
import com.scl.tallhandcamera2.throom.AnsDetailsDatabase;

/**
 * @author 作者：涉川良 联系方式：QQ470707134
 * @date 2025/4/12
 * @Description
 */
public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private ListPreference cameraApi;
    private ListPreference cameraId;
    private ListPreference cameraSize;
    private ListPreference cameraFpsRange;
    private Preference startBtn;
    private EditTxtPref features;
    private SortableListPref editing;
    private Preference answers;
    private EditTxtPref scoreSet;
    private EditTxtPref referenceWidth;
    private EditTxtPref referenceHeight;
    private EditTxtPref protoWidth;
    private EditTxtPref protoHeight;
    private EditTxtPref protoX;
    private EditTxtPref protoY;

    public static SharedPreferences sp;
    public static SharedPreferences.Editor ed;
    private boolean reuse = true;
    private CameraData cameraData;
    private ChoicesGridFeature[] cgfs;
    private int currentCgfIndex;
    private ThApplication app;
    private AnsDetailsDatabase db;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        //读取sharedPreference数据。但未读入这个preference
        THTools.log("现在读取已经存储过的设置数据：");
        sp = PreferenceManager.getDefaultSharedPreferences(requireContext());
        ed = sp.edit();
        var preferences = sp.getAll();
        for(var preference : preferences.entrySet()){
            THTools.log("%s -> %s", preference.getKey(), preference.getValue());
        }
        //读取sharedPreference和相关的xml文件。
        setPreferencesFromResource(R.xml.preferences, rootKey);
        //已从sharedPreference中读取。
        //cameraApi
        cameraApi = findPreference(CAMERA_API);
        cameraId = findPreference(CAMERA_ID);
        cameraSize = findPreference(CAMERA_SIZE);
        cameraFpsRange = findPreference(CAMERA_FPS_RANGE);
        startBtn = findPreference(START_BTN);
        features = findPreference(FEATURES);
        editing = findPreference(EDITING);
        answers = findPreference(ANSWERS);
        scoreSet = findPreference(SCORE_SET);
        referenceWidth = findPreference(REFERENCE_WIDTH);
        referenceHeight = findPreference(REFERENCE_HEIGHT);
        protoWidth = findPreference(PROTO_WIDTH);
        protoHeight = findPreference(PROTO_HEIGHT);
        protoX = findPreference(PROTO_X);
        protoY = findPreference(PROTO_Y);
        //cameraData设置
        resetCameraData();
        if(!finishCameraData()){
            THTools.log("这个id设置出了问题。");
        }
        //设置各项目的变化侦听器。
        setListeners(getPreferenceScreen());
        setCorrectPapers();
        app = (ThApplication) requireActivity().getApplication();
        db = AnsDetailsDatabase.getInstance(requireActivity());
    }

    public void setCorrectPapers(){
        String featuresStr = sp.getString(FEATURES, "");
        if(!featuresStr.isEmpty()){
            String[] feature_strs = featuresStr.split("\\r?\\n|\\r");
            cgfs = new ChoicesGridFeature[feature_strs.length];
            String[] vals = new String[feature_strs.length];
            String[] entries = new String[feature_strs.length];
            for(int i = 0; i < feature_strs.length; i++){
                cgfs[i] = new ChoicesGridFeature(feature_strs[i]);
                entries[i] = cgfs[i].toString();
                vals[i] = i + "";
            }
            features.setText(String.join("\n",entries));
            editing.setEnabled(true);
            editing.setEntries(entries);
            editing.setEntryValues(vals);
            if(currentCgfIndex < cgfs.length){
                editing.setValue(vals[currentCgfIndex]);
                editing.setSummary(entries[currentCgfIndex]);
                answers.setEnabled(true);
                answers.setSummary(cgfs[currentCgfIndex].answersStr);
                scoreSet.setEnabled(true);
                scoreSet.setText(cgfs[currentCgfIndex].choiceScoreSetExs.scoreSetsString);
            }else{
                editing.setValue(null);
                answers.setEnabled(false);
                scoreSet.setEnabled(false);
            }
        }else{
            features.setText("");
            editing.setEnabled(false);
            answers.setEnabled(false);
            scoreSet.setEnabled(false);
        }
    }

    /**
     * 递归遍历 Preference 并为每个 Preference 添加 OnPreferenceChangeListener
     */
    private void setListeners(PreferenceGroup preferenceGroup) {
        if (preferenceGroup == null) return;
        // 遍历 PreferenceGroup 中的所有子项
        for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
            Preference preference = preferenceGroup.getPreference(i);
            // 如果当前 Preference 是一个 PreferenceGroup（如 PreferenceCategory），递归处理
            if (preference instanceof PreferenceGroup) {
                setListeners((PreferenceGroup) preference);
            } else {
                // 为非 PreferenceGroup 的 Preference 添加监听器
                preference.setOnPreferenceClickListener(this);
                preference.setOnPreferenceChangeListener(this);
                if(preference instanceof EditTextPreference){
                    switch (preference.getKey()){
                        case IMPORT_SCHEME:
                        case REFERENCE_HASH:
                        case FEATURES:
                        case SCORE_SET:
                            break;
                        case PROTO_X:
                        case PROTO_Y:
                            //有符号整数
                            ((EditTextPreference) preference).setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
                                @Override
                                public void onBindEditText(@NonNull EditText editText) {
                                    editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                                }
                            });
                            break;
                        case C:
                        case FIRST_STEP_MULTIPLE:
                            //无符号浮点数
                            ((EditTextPreference) preference).setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
                                @Override
                                public void onBindEditText(@NonNull EditText editText) {
                                    editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                                }
                            });
                            break;
                        default:
                            //无符号整数
                            ((EditTextPreference) preference).setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
                                @Override
                                public void onBindEditText(@NonNull EditText editText) {
                                    editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                                }
                            });
                            break;
                    }
                }

            }
        }
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        if (preference instanceof EditTxtPref) {
            final EditTxtPrefDialogFragmentCompat f = EditTxtPrefDialogFragmentCompat.newInstance(preference.getKey());
            f.setTargetFragment(this, 0);
            f.show(getParentFragmentManager(), "androidx.preference.PreferenceFragment.DIALOG");
            return;
        } else if (preference instanceof SortableListPref) {
            final SortableListPrefDialogFragmentCompat f = SortableListPrefDialogFragmentCompat.newInstance(preference.getKey());
            f.setTargetFragment(this, 0);
            f.show(getParentFragmentManager(), "androidx.preference.PreferenceFragment.DIALOG");
            return;
        }
        super.onDisplayPreferenceDialog(preference);
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        switch (preference.getKey()){
            case START_BTN:
                if (cameraApi.getValue() == null || cameraApi.getValue().isEmpty()
                    || cameraId.getValue() == null || cameraId.getValue().isEmpty()
                    || cameraSize.getValue() == null || cameraSize.getValue().isEmpty()
                    || cameraFpsRange.getValue() == null || cameraFpsRange.getValue().isEmpty()
                ) break;
                updateCamera2Activity(reuse);
                break;
            case EXIT_BTN:
                if(requireActivity().getIntent().getBooleanExtra(Camera2Activity.CAMERA2ACTIVITY_CREATED, false)){
                    //此时后台还有Camera2Activity任务，必须结束它
                    Intent intent = new Intent();
                    intent.setClass(requireActivity(), Camera2Activity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    intent.putExtra(Camera2Activity.COMMAND_EXIT, true);
                    startActivity(intent);
                }
                //无论后台有没有活动，先直接结束当前活动
                requireActivity().finish();
                break;
            case EXPORT_SCHEME:
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(null, String.format(
                    "%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s",
                        sp.getString(SCANNING_INTERVAL, "10"),
                        sp.getString(REFERENCE_WIDTH, "180"),
                        sp.getString(REFERENCE_HEIGHT, "180"),
                        sp.getString(PROTO_WIDTH, "180"),
                        sp.getString(PROTO_HEIGHT, "180"),
                        sp.getString(PROTO_X, "0"),
                        sp.getString(PROTO_Y, "0"),
                        sp.getString(SCANNING_MODE, "CALC_AREA"),
                        sp.getString(BLOCK_SIZE, "37"),
                        sp.getString(C, "7"),
                        sp.getString(PIC_SIMILAR_TOLERANCE, "10"),
                        sp.getString(REFERENCE_HASH, "0123456789ABCDEF"),
                        sp.getString(BLACK_BLOCK_REASONING_MODE, "ADJACENT_DIFF"),
                        sp.getString(ADJACENT_DIFF_STARTING_INDEX, "3"),
                        sp.getString(NO_USE_STEP_MEANING, "1"),
                        sp.getString(FIRST_STEP_MULTIPLE, "1.2"),
                        sp.getString(FEATURES, "")
                ));
                clipboard.setPrimaryClip(clip);
                break;
            case RESET_CURRENT_SCHEME:
                ed.putString(SCANNING_INTERVAL, "");
                ed.putString(REFERENCE_WIDTH, "");
                ed.putString(REFERENCE_HEIGHT, "");
                ed.putString(PROTO_WIDTH, "");
                ed.putString(PROTO_HEIGHT, "");
                ed.putString(PROTO_X, "");
                ed.putString(PROTO_Y, "");
                ed.putString(SCANNING_MODE, "CALC_AREA");
                ed.putString(BLOCK_SIZE, "");
                ed.putString(C, "");
                ed.putString(PIC_SIMILAR_TOLERANCE, "");
                ed.putString(REFERENCE_HASH, "0123456789ABCDEF");
                ed.putString(BLACK_BLOCK_REASONING_MODE, "ADJACENT_DIFF");
                ed.putString(ADJACENT_DIFF_STARTING_INDEX, "");
                ed.putString(NO_USE_STEP_MEANING, "");
                ed.putString(FIRST_STEP_MULTIPLE, "");
                ed.putString(FEATURES, "");
                ed.apply();
                Intent currentIntent = requireActivity().getIntent();
                requireActivity().finish();
                requireActivity().overridePendingTransition(0, 0); // 去掉退出动画
                requireActivity().startActivity(currentIntent);
                requireActivity().overridePendingTransition(0, 0); // 去掉进入动画
                return false;
            case INSTRUCTION:
                Intent intent2Ins = new Intent();
                intent2Ins.setClass(requireActivity(), TextActivity.class);
                intent2Ins.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent2Ins.putExtra(TextActivity.HTML_FILE_NAME, TextActivity.HTML_INSTRUCTION);
                startActivity(intent2Ins);
                break;
            case ANSWERING_STATUS:
                Intent intent2Sta = new Intent();
                intent2Sta.setClass(requireActivity(), TextActivity.class);
                intent2Sta.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent2Sta.putExtra(TextActivity.HTML_FILE_NAME, TextActivity.HTML_ANSWERING_STATUS);
                intent2Sta.putExtra(FEATURES, sp.getString(FEATURES, ""));
                startActivity(intent2Sta);
                break;
            case ROTATE_BTN:
                TwoRect.RotatedTwoRect rotatedTwoRect = TwoRect.rotate(
                        referenceWidth.getText(),
                        referenceHeight.getText(),
                        protoWidth.getText(),
                        protoHeight.getText(),
                        protoX.getText(),
                        protoY.getText()
                );
                if(rotatedTwoRect != null){
                    referenceWidth.setText(rotatedTwoRect.rw);
                    referenceHeight.setText(rotatedTwoRect.rh);
                    protoWidth.setText(rotatedTwoRect.pw);
                    protoHeight.setText(rotatedTwoRect.ph);
                    protoX.setText(rotatedTwoRect.px);
                    protoY.setText(rotatedTwoRect.py);
                }
                break;
            case ANSWERS:
                cgfs[currentCgfIndex].answersRotate();
                String[] feature_strs = new String[cgfs.length];
                for(int i = 0; i < cgfs.length; i++){
                    feature_strs[i] = cgfs[i].toString();
                }
                ed.putString(FEATURES, String.join("\n", feature_strs));
                ed.apply();
                //将记录答题状况的数据表清空
                app.getAnsExecutor().execute(()->{
                    db.ansDetailsDao().deleteAll();
                });
                setCorrectPapers();
                return false;
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        switch (preference.getKey()){
            case IMPORT_SCHEME:
                String[] schemeStrs = newValue.toString().split("[;；]");
                try{
                    int i = 0;
                    ed.putString(SCANNING_INTERVAL, checkIntString(schemeStrs[i++], 10, num -> num >= 10));
                    ed.putString(REFERENCE_WIDTH,   checkIntString(schemeStrs[i++], 180, num -> num > 0));
                    ed.putString(REFERENCE_HEIGHT,  checkIntString(schemeStrs[i++], 180, num -> num > 0));
                    ed.putString(PROTO_WIDTH,       checkIntString(schemeStrs[i++], 180, num -> num > 0));
                    ed.putString(PROTO_HEIGHT,      checkIntString(schemeStrs[i++], 180, num -> num > 0));
                    ed.putString(PROTO_X,       checkIntString(schemeStrs[i++], 0, num -> true));
                    ed.putString(PROTO_Y,       checkIntString(schemeStrs[i++], 0, num -> true));
                    ed.putString(SCANNING_MODE, checkString(schemeStrs[i++], "CALC_AREA", "CALC_AREA|COUNT_CONTOURS"));
                    ed.putString(BLOCK_SIZE,    checkIntString(schemeStrs[i++], 37, num -> num > 0));
                    ed.putString(C,           checkFloatString(schemeStrs[i++], 7.0f, num -> num > 0));
                    ed.putString(PIC_SIMILAR_TOLERANCE, checkIntString(schemeStrs[i++], 10, num -> num > 0 && num < 64));
                    ed.putString(REFERENCE_HASH,        checkString(schemeStrs[i++], "0123456789ABCDEF", "[0-9A-F]{16}"));
                    ed.putString(BLACK_BLOCK_REASONING_MODE,   checkString(schemeStrs[i++], "ADJACENT_DIFF", "ADJACENT_DIFF|COMPARE"));
                    ed.putString(ADJACENT_DIFF_STARTING_INDEX, checkIntString(schemeStrs[i++], 3, num -> num >= 0));
                    ed.putString(NO_USE_STEP_MEANING, checkIntString(schemeStrs[i++], 1, num -> num >= 0));
                    ed.putString(FIRST_STEP_MULTIPLE, checkFloatString(schemeStrs[i++], 1.2f, num -> num > 0));
                    ed.putString(FEATURES, schemeStrs[i++]);
                } catch (Exception e) {
                    THTools.log("导入的字串长度似乎不对。但是，前面部分可读的数据可能已经录入。");
                }
                ed.apply();
                //将记录答题状况的数据表清空
                app.getAnsExecutor().execute(()->{
                    db.ansDetailsDao().deleteAll();
                });
                Intent intent = requireActivity().getIntent();
                requireActivity().finish();
                requireActivity().overridePendingTransition(0, 0); // 去掉退出动画
                requireActivity().startActivity(intent);
                requireActivity().overridePendingTransition(0, 0); // 去掉进入动画
                return false;
            case CAMERA_API:
                reuse = false;
                //换了api必须重置cameraData
                cameraApi.setValue(newValue.toString());//先设置，不然resetCameraData会出错
                resetCameraData();
                return false;
            case CAMERA_ID:
                reuse = false;
                //换了id重置size和fps
                cameraId.setValue(newValue.toString());//先设置，不然finishCameraData会出错
                if(!finishCameraData()){
                    THTools.log("这个id设置出了问题。");
                }
                return false;
            case CAMERA_SIZE:
                reuse = false;
                if(isValueValid(cameraFpsRange)) startBtn.setEnabled(true);
                break;
            case CAMERA_FPS_RANGE:
                reuse = false;
                if(isValueValid(cameraSize)) startBtn.setEnabled(true);
                break;
            case FEATURES:
                ed.putString(FEATURES, newValue.toString());
                ed.apply();
                //将记录答题状况的数据表清空
                app.getAnsExecutor().execute(()->{
                    db.ansDetailsDao().deleteAll();
                });
                setCorrectPapers();
                return false;
            case EDITING:
                currentCgfIndex = Integer.parseInt(newValue.toString());
                editing.setValue(editing.getEntryValues()[currentCgfIndex].toString());
                editing.setSummary(editing.getEntries()[currentCgfIndex]);
                answers.setEnabled(true);
                answers.setSummary(cgfs[currentCgfIndex].answersStr);
                scoreSet.setEnabled(true);
                scoreSet.setText(cgfs[currentCgfIndex].choiceScoreSetExs.scoreSetsString);
                return false;
            case SCORE_SET:
                cgfs[currentCgfIndex].setScoreSets(newValue.toString());
                String[] feature_strs = new String[cgfs.length];
                for(int i = 0; i < cgfs.length; i++){
                    feature_strs[i] = cgfs[i].toString();
                }
                ed.putString(FEATURES, String.join("\n", feature_strs));
                ed.apply();
                //将记录答题状况的数据表清空
                app.getAnsExecutor().execute(()->{
                    db.ansDetailsDao().deleteAll();
                });
                setCorrectPapers();
                return false;
            default:
                break;
        }
        return true;
    }

    private void resetCameraData(){
        if(cameraApi.getValue() == null || cameraApi.getValue().isEmpty()){
            //应该不可能到达
            cameraApi.setValue(cameraApi.getEntryValues()[1].toString());
        }
        //重建cameraData
        if(cameraApi.getValue().equals(CameraData.CAMERA1)){
            cameraData = new CameraData();
        }else if(cameraApi.getValue().equals(CameraData.CAMERA2)){
            cameraData = new CameraData(getContext());
        }
        //确定cameraId的列表内容，并使cameraId可用，以供用户选择
        cameraId.setEnabled(true);
        cameraSize.setEnabled(false);
        cameraFpsRange.setEnabled(false);
        startBtn.setEnabled(false);
        cameraId.setEntries(cameraData.cameraIdEntries);
        cameraId.setEntryValues(cameraData.cameraIdValues);
        if(isValueValid(cameraId)){
            finishCameraData();
        }else{
            cameraId.setValue(null);
        }
    }

    private boolean finishCameraData(){
        if(cameraId.getValue() == null || cameraId.getValue().isEmpty()){
            return false;
        }
        if(cameraApi.getValue().equals(CameraData.CAMERA1)){
            if(!cameraData.getCameraSizesAndFpsRanges(Integer.parseInt(cameraId.getValue()))){
                return false;
            }
        }else if(cameraApi.getValue().equals(CameraData.CAMERA2)){
            if(!cameraData.getCameraSizesAndFpsRanges(getContext(), cameraId.getValue())){
                return false;
            }
        }
        //到这里就意味着正常获取了cameraData的剩余部分
        cameraSize.setEnabled(true);
        cameraSize.setEntries(cameraData.cameraSizes);
        cameraSize.setEntryValues(cameraData.cameraSizes);
        boolean sizeValueValid = isValueValid(cameraSize);
        if(!sizeValueValid){
            cameraSize.setValue(null);
        }
        cameraFpsRange.setEnabled(true);
        cameraFpsRange.setEntries(cameraData.cameraFpsRanges);
        cameraFpsRange.setEntryValues(cameraData.cameraFpsRanges);
        boolean fpsValueValid = isValueValid(cameraFpsRange);
        if(!fpsValueValid){
            cameraFpsRange.setValue(null);
        }
        if(sizeValueValid && fpsValueValid) startBtn.setEnabled(true);
        return true;
    }

    public boolean isValueValid(ListPreference listPreference) {
        // 获取当前值
        String currentValue = listPreference.getValue();
        // 如果当前值为空，直接返回 false
        if (currentValue == null || currentValue.isEmpty()) {
            return false;
        }
        // 获取 entryValues 数组
        CharSequence[] entryValues = listPreference.getEntryValues();
        // 遍历 entryValues，检查当前值是否存在
        for (CharSequence entryValue : entryValues) {
            if (entryValue.equals(currentValue)) {
                return true; // 当前值有效
            }
        }
        return false; // 当前值无效
    }

    /**
     * 如果已有相机活动，则摧毁后重建；如果没有相机活动，则直接新建；也可指定复用相机活动。
     * @param reuse 指示是否复用相机活动
     */
    private void updateCamera2Activity(boolean reuse){
        Intent intent = new Intent();
        intent.setClass(requireActivity(), Camera2Activity.class);
        if(reuse){
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        }else{
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        intentPutExtras(intent);
        startActivity(intent);
        requireActivity().finish();
    }

    private void intentPutExtra(Intent intent, String key, int defValue){
        int value;
        try{
            value = Integer.parseInt(sp.getString(key, defValue + ""));
        }catch (NumberFormatException e){
            value = defValue;
        }
        intent.putExtra(key, value);
    }
    private void intentPutExtra(Intent intent, String key, String defValue){
        intent.putExtra(key, sp.getString(key, defValue));
    }
    private void intentPutExtra(Intent intent, String key, float defValue){
        float value;
        try{
            value = Float.parseFloat(sp.getString(key, defValue + ""));
        }catch (NumberFormatException e){
            value = defValue;
        }
        intent.putExtra(key, value);
    }

    private void intentPutExtras(Intent intent){
        intentPutExtra(intent, CAMERA_API, CameraData.CAMERA2);
        intentPutExtra(intent, CAMERA_ID, "");//
        intentPutExtra(intent, CAMERA_SIZE, "");//
        intentPutExtra(intent, CAMERA_FPS_RANGE, "");//
        intentPutExtra(intent, SCANNING_INTERVAL, 20);
        intentPutExtra(intent, REFERENCE_WIDTH, 180);
        intentPutExtra(intent, REFERENCE_HEIGHT, 180);
        intentPutExtra(intent, PROTO_WIDTH, 180);
        intentPutExtra(intent, PROTO_HEIGHT, 180);
        intentPutExtra(intent, PROTO_X, 0);
        intentPutExtra(intent, PROTO_Y, 0);
        intentPutExtra(intent, SCANNING_MODE, "CALC_AREA");
        intentPutExtra(intent, BLOCK_SIZE, 37);
        intentPutExtra(intent, C, 7.0f);
        intentPutExtra(intent, PIC_SIMILAR_TOLERANCE, 10);
        intentPutExtra(intent, REFERENCE_HASH, "0123456789ABCDEF");
        intentPutExtra(intent, BLACK_BLOCK_REASONING_MODE, "ADJACENT_DIFF");
        intentPutExtra(intent, ADJACENT_DIFF_STARTING_INDEX, 3);
        intentPutExtra(intent, NO_USE_STEP_MEANING, 1);
        intentPutExtra(intent, FIRST_STEP_MULTIPLE, 1.2f);
        intentPutExtra(intent, FEATURES, "");
    }

    public static final String IMPORT_SCHEME = "import_scheme";
    public static final String EXPORT_SCHEME = "export_scheme";
    public static final String RESET_CURRENT_SCHEME = "reset_current_scheme";
    public static final String INSTRUCTION = "instruction";
    public static final String ANSWERING_STATUS = "answering_status";
    public static final String START_BTN = "start_btn";
    public static final String EXIT_BTN = "exit_btn";
    public static final String CAMERA_API = "camera_api";
    public static final String CAMERA_ID = "camera_id";
    public static final String CAMERA_SIZE = "camera_size";
    public static final String CAMERA_FPS_RANGE = "camera_fps_range";
    public static final String SCANNING_INTERVAL = "scanning_interval";
    public static final String REFERENCE_WIDTH = "reference_width";
    public static final String REFERENCE_HEIGHT = "reference_height";
    public static final String PROTO_WIDTH = "proto_width";
    public static final String PROTO_HEIGHT = "proto_height";
    public static final String PROTO_X = "proto_x";
    public static final String PROTO_Y = "proto_y";
    public static final String ROTATE_BTN = "rotate_btn";
    public static final String SCANNING_MODE = "scanning_mode";
    public static final String BLOCK_SIZE = "block_size";
    public static final String C = "c";
    public static final String PIC_SIMILAR_TOLERANCE = "pic_similar_tolerance";
    public static final String REFERENCE_HASH = "reference_hash";
    public static final String BLACK_BLOCK_REASONING_MODE = "black_block_reasoning_mode";
    public static final String ADJACENT_DIFF_STARTING_INDEX = "adjacent_diff_starting_index";
    public static final String NO_USE_STEP_MEANING = "no_use_step_meaning";
    public static final String FIRST_STEP_MULTIPLE = "first_step_multiple";
    public static final String FEATURES = "features";
    public static final String EDITING = "editing";
    public static final String ANSWERS = "answers";
    public static final String SCORE_SET = "score_set";
}
