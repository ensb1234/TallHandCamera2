package com.scl.tallhandcamera2;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;

/**
 * @author 作者：涉川良 联系方式：QQ470707134
 * @date 2025/5/14
 * @Description
 */
public class SortableListPref extends ListPreference {
    public SortableListPref(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public SortableListPref(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SortableListPref(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SortableListPref(@NonNull Context context) {
        super(context);
    }
}
