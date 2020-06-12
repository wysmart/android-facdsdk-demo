package com.weiyuan.fragment;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.weiyuan.R;
import com.weiyuan.util.ConfigUtil;
import com.wyuansmart.mars.shared.algorithm.dto.RotateDegree;

public class ChooseDetectDegreeDialog extends DialogFragment implements View.OnClickListener {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.dialog_choose_detect_degree, container);
        initView(dialogView);
        return dialogView;
    }

    private void initView(View dialogView) {
        ImageView ivClose = dialogView.findViewById(R.id.iv_close);
        ivClose.setOnClickListener(this);
        //设置视频模式下的人脸优先检测方向
        RadioGroup radioGroupFtOrient = dialogView.findViewById(R.id.radio_group_ft_orient);
        RadioButton rbOrient0 = dialogView.findViewById(R.id.rb_orient_0);
        RadioButton rbOrient90 = dialogView.findViewById(R.id.rb_orient_90);
        RadioButton rbOrient180 = dialogView.findViewById(R.id.rb_orient_180);
        RadioButton rbOrient270 = dialogView.findViewById(R.id.rb_orient_270);
        switch (ConfigUtil.getFtOrient(getActivity())) {
            case RotateDegree.SDK_Rotate_90:
                rbOrient90.setChecked(true);
                break;
            case RotateDegree.SDK_Rotate_180:
                rbOrient180.setChecked(true);
                break;
            case RotateDegree.SDK_Rotate_270:
                rbOrient270.setChecked(true);
                break;
            case RotateDegree.SDK_Rotate_0:
            default:
                rbOrient0.setChecked(true);
                break;
        }
        radioGroupFtOrient.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rb_orient_90:
                        ConfigUtil.setFtOrient(getActivity(), RotateDegree.SDK_Rotate_90);
                        break;
                    case R.id.rb_orient_180:
                        ConfigUtil.setFtOrient(getActivity(), RotateDegree.SDK_Rotate_180);
                        break;
                    case R.id.rb_orient_270:
                        ConfigUtil.setFtOrient(getActivity(), RotateDegree.SDK_Rotate_270);
                        break;
                    case R.id.rb_orient_0:
                    default:
                        ConfigUtil.setFtOrient(getActivity(), RotateDegree.SDK_Rotate_0);
                        break;
                }
                dismiss();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog().getWindow();
        if (window != null){
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Override
    public void onClick(View view) {
        dismiss();
    }
}
