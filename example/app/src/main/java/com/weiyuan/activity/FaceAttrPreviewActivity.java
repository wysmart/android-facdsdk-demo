package com.weiyuan.activity;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import com.weiyuan.R;
import com.weiyuan.model.DrawInfo;
import com.weiyuan.util.ConfigUtil;
import com.weiyuan.util.DrawHelper;
import com.weiyuan.util.camera.CameraHelper;
import com.weiyuan.util.camera.CameraListener;
import com.weiyuan.util.face.RecognizeColor;
import com.weiyuan.widget.FaceRectView;
import com.wyuansmart.mars.shared.algorithm.FaceEngine;
import com.wyuansmart.mars.shared.algorithm.dto.AgeInfo;
import com.wyuansmart.mars.shared.algorithm.dto.DetectMode;
import com.wyuansmart.mars.shared.algorithm.dto.ErrorInfo;
import com.wyuansmart.mars.shared.algorithm.dto.Face3DAngle;
import com.wyuansmart.mars.shared.algorithm.dto.FaceInfo;
import com.wyuansmart.mars.shared.algorithm.dto.GenderInfo;
import com.wyuansmart.mars.shared.algorithm.dto.LivenessInfo;

import java.util.ArrayList;
import java.util.List;

public class FaceAttrPreviewActivity extends BaseActivity implements ViewTreeObserver.OnGlobalLayoutListener {
    private static final String TAG = "FaceAttrPreviewActivity";
    private CameraHelper cameraHelper;
    private DrawHelper drawHelper;
    private Camera.Size previewSize;
    private Integer rgbCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private FaceEngine faceEngine;
    private int sdkCode = -1;
    //private int processMask = FaceEngine.SDK_FACE_DETECT | FaceEngine.SDK_AGE | FaceEngine.SDK_GENDER | FaceEngine.SDK_FACE3DANGLE | FaceEngine.SDK_LIVENESS;
    private int processMask = FaceEngine.SDK_FACE_DETECT  | FaceEngine.SDK_AGE | FaceEngine.SDK_GENDER | FaceEngine.SDK_LIVENESS;

    List<FaceInfo> lastFaceInfoList = new ArrayList<>();
    List<AgeInfo> lastAgeInfoList = new ArrayList<>();
    List<GenderInfo> lastGenderInfoList = new ArrayList<>();
    List<Face3DAngle> lastFace3DAngleList = new ArrayList<>();
    List<LivenessInfo> lastFaceLivenessInfoList = new ArrayList<>();
    /**
     * 相机预览显示的控件，可为SurfaceView或TextureView
     */
    private View previewView;
    private FaceRectView faceRectView;

    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    /**
     * 所需的所有权限信息
     */
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_attr_preview);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WindowManager.LayoutParams attributes = getWindow().getAttributes();
            attributes.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            getWindow().setAttributes(attributes);
        }

        // Activity启动后就锁定为启动时的方向
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        previewView = findViewById(R.id.texture_preview);
        faceRectView = findViewById(R.id.face_rect_view);

        //在布局结束后才做初始化操作
        previewView.getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    private boolean initEngine() {
        faceEngine = new FaceEngine();
        sdkCode = faceEngine.init(DetectMode.SDK_DETECT_MODE_VIDEO, ConfigUtil.getFtOrient(this), 320, processMask );
        faceEngine.setDebug(0, 0, 0, 0, 0);
        Log.i(TAG, "initEngine:  init: " + sdkCode);
        if (sdkCode != ErrorInfo.OK) {
            showToast( getString(R.string.init_failed, sdkCode));
            return false;
        }
        return true;
    }

    private void unInitEngine() {

        if (sdkCode == 0 && faceEngine != null) {
            sdkCode = faceEngine.unInit();
            faceEngine = null;
            Log.i(TAG, "unInitEngine: " + sdkCode);
        }
    }

    @Override
    protected void onDestroy() {
        if (cameraHelper != null) {
            cameraHelper.release();
            cameraHelper = null;
        }
        unInitEngine();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (cameraHelper != null) {
                cameraHelper.release();
                cameraHelper = null;
            }
            unInitEngine();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initCamera() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        CameraListener cameraListener = new CameraListener() {
            @Override
            public void onCameraOpened(Camera camera, int cameraId, int displayOrientation, boolean isMirror) {
                Log.i(TAG, "onCameraOpened: " + cameraId + "  " + displayOrientation + " " + isMirror);
                previewSize = camera.getParameters().getPreviewSize();
                drawHelper = new DrawHelper(previewSize.width, previewSize.height, previewView.getWidth(), previewView.getHeight(), displayOrientation
                        , cameraId, isMirror, false, false);
            }


            @Override
            public void onPreview(byte[] nv21, Camera camera) {

                if (faceRectView != null) {
                    faceRectView.clearFaceInfo();
                }
                List<FaceInfo> faceInfoList = new ArrayList<>();
                int code = faceEngine.detectFaces(nv21, previewSize.width, previewSize.height, ConfigUtil.getPictureFormat(), faceInfoList);
                if (code != ErrorInfo.OK) {
                    Log.e(TAG, "detect face fail, code:" + code  + ", width:" + previewSize.width + ", height:" + previewSize.height + ", data length:" + nv21.length);
                    return;
                }
                Log.i(TAG, "face count:" + faceInfoList.size());
                for (FaceInfo face : faceInfoList) {
                    Log.i(TAG, "face info:" + face.toString());
                }

                boolean faceChange = isFaceChange(faceInfoList);
                List<AgeInfo> ageInfoList = new ArrayList<>();
                List<GenderInfo> genderInfoList = new ArrayList<>();
                List<Face3DAngle> face3DAngleList = new ArrayList<>();
                List<LivenessInfo> faceLivenessInfoList = new ArrayList<>();

                if (faceChange) {
                    ageInfoList = getAgeInfo();
                    genderInfoList = getGenderInfo();
                    face3DAngleList = getPoseInfo();
                    faceLivenessInfoList = getLivenessInfo();
                    lastFaceInfoList = faceInfoList;
                } else {
                    ageInfoList = lastAgeInfoList;
                    genderInfoList = lastGenderInfoList;
                    face3DAngleList = lastFace3DAngleList;
                    //faceLivenessInfoList = lastFaceLivenessInfoList;
                    faceLivenessInfoList = getLivenessInfo();
                }

                if (faceRectView != null && drawHelper != null) {
                    List<DrawInfo> drawInfoList = new ArrayList<>();
                    for (int i = 0; i < faceInfoList.size(); i++) {
                        if (faceLivenessInfoList.size() == 0) {
                            drawInfoList.add(new DrawInfo(drawHelper.adjustRect(faceInfoList.get(i).getRect()), 0, 0, -1, faceInfoList.get(i).getFaceId(), RecognizeColor.COLOR_UNKNOWN, null));
                            continue;
                        }
                        if (faceLivenessInfoList.get(i).getLiveness() == LivenessInfo.NOT_ALIVE) {
                            drawInfoList.add(new DrawInfo(drawHelper.adjustRect(faceInfoList.get(i).getRect()), genderInfoList.get(i).getGender(), ageInfoList.get(i).getAge(), faceLivenessInfoList.get(i).getLiveness(), faceInfoList.get(i).getFaceId(), RecognizeColor.COLOR_FAILED, null));
                            //drawInfoList.add(new DrawInfo(drawHelper.adjustRect(faceInfoList.get(i).getRect()), 0, 0, faceLivenessInfoList.get(i).getLiveness(), faceInfoList.get(i).getFaceId(), RecognizeColor.COLOR_FAILED, null));
                        }else if (faceLivenessInfoList.get(i).getLiveness() == LivenessInfo.ALIVE){
                            drawInfoList.add(new DrawInfo(drawHelper.adjustRect(faceInfoList.get(i).getRect()), genderInfoList.get(i).getGender(), ageInfoList.get(i).getAge(), faceLivenessInfoList.get(i).getLiveness(),  faceInfoList.get(i).getFaceId(), RecognizeColor.COLOR_SUCCESS, null));
                            //drawInfoList.add(new DrawInfo(drawHelper.adjustRect(faceInfoList.get(i).getRect()), 0, 0, faceLivenessInfoList.get(i).getLiveness(), faceInfoList.get(i).getFaceId(), RecognizeColor.COLOR_SUCCESS, null));
                        } else {
                            drawInfoList.add(new DrawInfo(drawHelper.adjustRect(faceInfoList.get(i).getRect()), genderInfoList.get(i).getGender(), ageInfoList.get(i).getAge(), faceLivenessInfoList.get(i).getLiveness(),  faceInfoList.get(i).getFaceId(), RecognizeColor.COLOR_UNKNOWN, null));
                        }
                    }
                    drawHelper.draw(faceRectView, drawInfoList);
                }
            }

            @Override
            public void onCameraClosed() {
                Log.i(TAG, "onCameraClosed: ");
            }

            @Override
            public void onCameraError(Exception e) {
                Log.i(TAG, "onCameraError: " + e.getMessage());
            }

            @Override
            public void onCameraConfigurationChanged(int cameraID, int displayOrientation) {
                if (drawHelper != null) {
                    drawHelper.setCameraDisplayOrientation(displayOrientation);
                }
                Log.i(TAG, "onCameraConfigurationChanged: " + cameraID + "  " + displayOrientation);
            }
        };
        cameraHelper = new CameraHelper.Builder()
                .previewViewSize(new Point(previewView.getMeasuredWidth(), previewView.getMeasuredHeight()))
                .rotation(getWindowManager().getDefaultDisplay().getRotation())
                .specificCameraId(rgbCameraId != null ? rgbCameraId : Camera.CameraInfo.CAMERA_FACING_FRONT)
                .isMirror(false)
                .previewOn(previewView)
                .cameraListener(cameraListener)
                .build();
        cameraHelper.init();
        cameraHelper.start();
    }

    @Override
    void afterRequestPermission(int requestCode, boolean isAllGranted) {
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            if (isAllGranted) {
                boolean ret = initEngine();
                if (ret) {
                    initCamera();
                }
            } else {
                showToast(getString( R.string.permission_denied));
            }
        }
    }

    /**
     * 在{@link #previewView}第一次布局完成后，去除该监听，并且进行引擎和相机的初始化
     */
    @Override
    public void onGlobalLayout() {
        previewView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
        } else {
            boolean ret = initEngine();
            if (ret) {
                initCamera();
            }
        }
    }

    /**
     * 切换相机。注意：若切换相机发现检测不到人脸，则极有可能是检测角度导致的，需要销毁引擎重新创建或者在设置界面修改配置的检测角度
     *
     * @param view
     */
    public void switchCamera(View view) {
        if (cameraHelper != null) {
            boolean success = cameraHelper.switchCamera();
            if (!success) {
                showToast(getString(R.string.switch_camera_failed));
            } else {
                showLongToast(getString(R.string.notice_change_detect_degree));
            }
        }
    }

    // 人脸是否发生了改变
    private boolean isFaceChange(List<FaceInfo> faces) {
        if (faces.size() != lastFaceInfoList.size()) {
            return true;
        }

        for (int i = 0; i < faces.size(); ++i) {
            boolean isFind = false;
            if (faces.get(i).getFaceId() == lastFaceInfoList.get(i).getFaceId()) {
                isFind = true;
                break;
            }

            // 如果没有找到，则直接返回
            if (!isFind) {
                return true;
            }
        }

        return  false;
    }

    // 获取年龄

    private List<AgeInfo> getAgeInfo() {
        List<AgeInfo> ageInfos = new ArrayList<>();
        faceEngine.getAge(ageInfos);
        lastAgeInfoList = ageInfos;
        return ageInfos;
    }

    // 获取性别
    private List<GenderInfo> getGenderInfo() {
        List<GenderInfo> genderInfos = new ArrayList<>();
        faceEngine.getGender(genderInfos);
        lastGenderInfoList = genderInfos;
        return genderInfos;
    }

    // 获取活体
    private List<LivenessInfo> getLivenessInfo() {
        List<LivenessInfo> livenessInfos = new ArrayList<>();
        faceEngine.getLiveness(livenessInfos);
        lastFaceLivenessInfoList = livenessInfos;
        return livenessInfos;
    }

    // 获取角度
    private List<Face3DAngle> getPoseInfo() {
        List<Face3DAngle> poseInfos = new ArrayList<>();
        faceEngine.getFace3DAngle(poseInfos);
        lastFace3DAngleList = poseInfos;
        return poseInfos;
    }
}
