package com.android.systemui.litert;

import android.content.Context;
import android.util.Log;

public class LiteRtManager {
    private static final String TAG = "LiteRtManager";
    private static final String MODEL_PATH = "/data/litert/gemma-3n-E2B-it-int4.litertlm";
    private static final String BINARY_PATH = "/system/bin/litert_lm_main";

    private static LiteRtManager sInstance;
    private final Context mContext;
    private boolean mReady = false;

    private LiteRtManager(Context context) {
        mContext = context.getApplicationContext();
        checkReady();
    }

    public static synchronized LiteRtManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new LiteRtManager(context);
        }
        return sInstance;
    }

    private void checkReady() {
        mReady = new java.io.File(BINARY_PATH).exists()
               && new java.io.File(MODEL_PATH).exists();
        Log.d(TAG, "LiteRtManager ready: " + mReady);
    }

    public boolean isReady() {
        return mReady;
    }

    public String getModelPath() {
        return MODEL_PATH;
    }

    public String getBinaryPath() {
        return BINARY_PATH;
    }
}