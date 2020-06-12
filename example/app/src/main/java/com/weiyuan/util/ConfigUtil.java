package com.weiyuan.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.util.Log;

import com.wyuansmart.mars.shared.algorithm.FaceEngine;
import com.wyuansmart.mars.shared.algorithm.dto.RotateDegree;
import com.wyuansmart.mars.shared.algorithm.dto.Response;
import com.wyuansmart.mars.shared.algorithm.imageutil.SdkImageFormat;

public class ConfigUtil {
    private static final String APP_NAME = "FaceSdkExample";
    private static final String TRACKED_FACE_COUNT = "trackedFaceCount";
    private static final String FT_ORIENT = "ftOrientPriority";
    public static final String key = "Rn8OjrcNDmArL9ByBKGcAQJDT5oLFrTQFwJtaItflrty4eCLMTBSW1greawl5VzbZ0379F4tO0yyQi2nqmhYKd5l/pn+i1vyQ2R2XeT3kH1ztKC2LBBU4Ihz2pRt5dEYDg0TYEloGqXjXuLyKEeWaVgCxWn89BijEOCzAua3Rao8fkT+4kauiWUiRMSswIXPGRm2lxPH7xyPvTaYVPdOD8wQV23tXoESi0IzfUyqc8+kf1I1OTzt5g0VKDGt3pRuHqdVcvvsdNn7pTO3RAV21QdRHAkbqZM3s5akxsYUrFI5WFx/WyECfbU0qvmcciODBzB8iVP6rAcxU2plxaY6+Q==";
    public static final String ACTIVATION_CODE = "ActivationCode";
    private static String activationCode;
    private static int pictureFormat = SdkImageFormat.NV21;
    private static String TAG = "ConfigUtil";
    public static boolean setTrackedFaceCount(Context context, int trackedFaceCount) {
        if (context == null) {
            return false;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.edit()
                .putInt(TRACKED_FACE_COUNT, trackedFaceCount)
                .commit();
    }

    // 图像格式
    public static void setPictureFormat(int format) {
        Log.i(TAG, "set picture format:" + format);
        switch(format) {
            case ImageFormat.NV21:
                pictureFormat = SdkImageFormat.NV21;
                break;
            case ImageFormat.YV12:
                pictureFormat = SdkImageFormat.YV12;
                break;
            case ImageFormat.YUV_420_888:
                pictureFormat = SdkImageFormat.I420;
                break;
            case ImageFormat.YUY2:
                pictureFormat = SdkImageFormat.YUY2;
                break;
        }
    }
    public static int getPictureFormat() {
        return pictureFormat;
    }

    public static int getTrackedFaceCount(Context context) {
        if (context == null) {
            return 0;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(TRACKED_FACE_COUNT, 0);
    }

    // 获取激活码
    public static String getActivationCode(Context context) {

        // 激活码为空的话，先去shared perference里面找
        if (activationCode == null || activationCode.isEmpty()) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE);
            activationCode = sharedPreferences.getString(ACTIVATION_CODE, "");
        }

        // 如果shared perference里面没有找到，去网上激活
        if (activationCode == null || activationCode.isEmpty()) {
            Response response = new Response();
            FaceEngine.getActivationCode(response);
            if (response.isSuccessful()) {
                activationCode = response.getResponse();

                // 保存到shared perference中
                SharedPreferences sharedPreferences = context.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE);
                sharedPreferences.edit().putString(ACTIVATION_CODE, activationCode).commit();
            }
        }

        return activationCode;
    }

    public static boolean setFtOrient(Context context, int ftOrient) {
        if (context == null) {
            return false;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.edit()
                .putInt(FT_ORIENT, ftOrient)
                .commit();
    }

    public static int getFtOrient(Context context) {
        if (context == null) {
            return RotateDegree.SDK_Rotate_270;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(FT_ORIENT, RotateDegree.SDK_Rotate_270);
    }
}
