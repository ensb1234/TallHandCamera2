package com.scl.tallhandcamera2;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author 作者：涉川良 联系方式：QQ470707134
 * @date 2025/4/25
 * @Description
 */
public class EditTxtPref extends androidx.preference.EditTextPreference{

    public EditTxtPref(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public EditTxtPref(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public EditTxtPref(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTxtPref(@NonNull Context context) {
        super(context);
    }

}

