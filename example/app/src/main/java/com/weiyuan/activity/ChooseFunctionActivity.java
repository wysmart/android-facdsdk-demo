package com.weiyuan.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;

import com.weiyuan.R;
import com.weiyuan.fragment.ChooseDetectDegreeDialog;
import com.weiyuan.util.ConfigUtil;
import com.wyuansmart.mars.shared.algorithm.FaceEngine;
import com.wyuansmart.mars.shared.algorithm.dto.Activation;
import com.wyuansmart.mars.shared.algorithm.dto.Authority;
import com.wyuansmart.mars.shared.algorithm.dto.VersionInfo;

public class ChooseFunctionActivity extends BaseActivity {
    private static final String TAG = "ChooseFunctionActivity";
    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    /**
     * 所需的所有权限信息
     */
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.READ_PHONE_STATE
    };

    // 修改配置项的对话框
    ChooseDetectDegreeDialog chooseDetectDegreeDialog;
    Authority authority = new Authority();      // 验证key的结果

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_function);
        ApplicationInfo applicationInfo = getApplicationInfo();
        Log.i(TAG, "onCreate: " + applicationInfo.nativeLibraryDir);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        VersionInfo versionInfo = new VersionInfo();
        int code = FaceEngine.getVersion(versionInfo);
        Log.i(TAG, "onCreate: getVersion, code is: " + code + ", versionInfo is: " + versionInfo);

        // 检查key是否有效
        FaceEngine.verifyKey(getApplicationContext(), ConfigUtil.key, authority);
        if (authority.getValid() == 0) {
            Log.e(TAG, "key is invalid, reason:" + authority.getInvalidReason());
            showToast(authority.getInvalidReason());
            return;
        }


        // 检查是否有权限，获取激活码需要read phone stat权限
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
            return;
        }

        // 获取激活码
        getActivationCode();
    }

    /**
     * 打开相机，显示年龄性别
     *
     * @param view
     */
    public void jumpToPreviewActivity(View view) {
        checkLibraryAndJump(com.weiyuan.activity.FaceAttrPreviewActivity.class);
    }

    /**
     * 处理单张图片，显示图片中所有人脸的信息，并且一一比对相似度
     *
     * @param view
     */
    public void jumpToSingleImageActivity(View view) {
        checkLibraryAndJump(com.weiyuan.activity.SingleImageActivity.class);
    }

    /**
     * 选择一张主照，显示主照中人脸的详细信息，然后选择图片和主照进行比对
     *
     * @param view
     */
    public void jumpToMultiImageActivity(View view) {
        checkLibraryAndJump(com.weiyuan.activity.MultiImageActivity.class);
    }

    /**
     * 打开相机，RGB活体检测，人脸注册，人脸识别
     *
     * @param view
     */
    public void jumpToFaceRecognizeActivity(View view) {
        checkLibraryAndJump(RegisterAndRecognizeActivity.class);
    }

    /**
     * 批量注册和删除功能
     *
     * @param view
     */
    public void jumpToBatchRegisterActivity(View view) {
        checkLibraryAndJump(FaceManageActivity.class);
    }

    @Override
    void afterRequestPermission(int requestCode, boolean isAllGranted) {
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            if (isAllGranted) {
                getActivationCode();
            } else {
                showToast(getString(R.string.permission_denied));
            }
        }
    }

    void checkLibraryAndJump(Class activityClass) {
        startActivity(new Intent(this, activityClass));
    }


    public void chooseDetectDegree(View view) {
        if (chooseDetectDegreeDialog == null) {
            chooseDetectDegreeDialog = new ChooseDetectDegreeDialog();
        }
        if (chooseDetectDegreeDialog.isAdded()) {
            chooseDetectDegreeDialog.dismiss();
        }
        chooseDetectDegreeDialog.show(getSupportFragmentManager(), ChooseDetectDegreeDialog.class.getSimpleName());
    }

    // 获取激活码
    private boolean getActivationCode() {

        // key是否需要激活
        if (authority.getActivation() == 1) {

            // 获取激活码
            String activationCode = ConfigUtil.getActivationCode(getApplicationContext());
            if (null == activationCode || activationCode.isEmpty()) {
                Log.e(TAG, "key is invalid, reason:" + authority.getInvalidReason());
                showToast(getString(R.string.active_failed));
                return false;
            } else {
                Activation activation = new Activation();
                FaceEngine.activationKey(activationCode, activation);

                // 判断激活是否成功
                if (activation.getValid() == 0) {
                    Log.e(TAG, "activation key fail, reason:" + activation.getInvalidReason());
                    showToast(activation.getInvalidReason());
                    return false;
                }
            }
        }
        return true;
    }
}
