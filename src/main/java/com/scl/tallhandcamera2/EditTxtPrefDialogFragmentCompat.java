package com.scl.tallhandcamera2;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

/**
 * @author 作者：涉川良 联系方式：QQ470707134
 * @date 2025/4/25
 * @Description
 */
public class EditTxtPrefDialogFragmentCompat extends androidx.preference.EditTextPreferenceDialogFragmentCompat {

    private EditText mEditText;
    private ObjectAnimator mShakeAnimator;
    private EditTxtPref mPreference;
    private CharSequence mDialogTitle;
    private CharSequence mPositiveButtonText;
    private CharSequence mNegativeButtonText;
    private CharSequence mDialogMessage;
    @NonNull
    public static EditTxtPrefDialogFragmentCompat newInstance(String key) {
        final EditTxtPrefDialogFragmentCompat
                fragment = new EditTxtPrefDialogFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreference = (EditTxtPref) getPreference();
        if (savedInstanceState == null) {
            mDialogTitle = mPreference.getDialogTitle();
            mPositiveButtonText = mPreference.getPositiveButtonText();
            mNegativeButtonText = mPreference.getNegativeButtonText();
            mDialogMessage = mPreference.getDialogMessage();
        } else {
            mDialogTitle = savedInstanceState.getCharSequence("PreferenceDialogFragment.title");
            mPositiveButtonText = savedInstanceState.getCharSequence("PreferenceDialogFragment.positiveText");
            mNegativeButtonText = savedInstanceState.getCharSequence("PreferenceDialogFragment.negativeText");
            mDialogMessage = savedInstanceState.getCharSequence("PreferenceDialogFragment.message");
        }
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);
        mEditText = view.findViewById(android.R.id.edit);
        mShakeAnimator = ObjectAnimator.ofFloat(mEditText, "translationX", 0, 25, -25, 25, -25, 15, -15, 6, -6, 0);
        mShakeAnimator.setDuration(1000);
    }

    @Override
    public void onClick(@NonNull DialogInterface dialog, int which) {
        super.onClick(dialog, which);
    }

    @Override
    public @NonNull
    Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        final AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle(mDialogTitle)
                .setPositiveButton(mPositiveButtonText, null) //侦听器截胡
                .setNegativeButton(mNegativeButtonText, this);

        View contentView = onCreateDialogView(requireContext());
        if (contentView != null) {
            onBindDialogView(contentView);
            builder.setView(contentView);
        } else {
            builder.setMessage(mDialogMessage);
        }

        onPrepareDialogBuilder(builder);

        // Create the dialog
        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                //为了避免点击 positive 按钮后直接关闭 dialog,把点击事件拿出来设置
                ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(!isValidNum(mEditText.getText().toString())){
                            mShakeAnimator.start();
                            return;
                        }
                        EditTxtPrefDialogFragmentCompat.this.onClick(dialog, AlertDialog.BUTTON_POSITIVE);
                        dialog.dismiss();
                    }
                });
            }
        });
        return dialog;
    }

    private Boolean isValidNum(String editText){
        int num = 0;
        try {
            num = Integer.parseInt(editText);
        }catch (NumberFormatException e){
            return true;
        }
        switch (mPreference.getKey()){
            case SettingsFragment.SCANNING_INTERVAL:
                return num >= 10;
            case SettingsFragment.REFERENCE_WIDTH:
            case SettingsFragment.REFERENCE_HEIGHT:
            case SettingsFragment.PROTO_WIDTH:
            case SettingsFragment.PROTO_HEIGHT:
            case SettingsFragment.BLOCK_SIZE:
            case SettingsFragment.C:
            case SettingsFragment.FIRST_STEP_MULTIPLE:
                return num > 0;
            case SettingsFragment.PIC_SIMILAR_TOLERANCE:
                return num > 0 && num < 64;
            default:
                return true;
        }
    }
}
