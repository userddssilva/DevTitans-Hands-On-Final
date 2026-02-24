package com.android.systemui.litert;

import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class NotificationSummaryHook {
    private static final String TAG = "NotificationSummaryHook";

    // Apps que você quer resumir (adicione os pacotes que quiser)
    private static final String[] TARGET_PACKAGES = {
        "com.whatsapp",
        "org.telegram.messenger",
        "com.instagram.android",
        "br.edu.ufam.testenotification"
    };

    private final LiteRtSummarizer mSummarizer;
    private LiteRtNotificationManager mLiteRtNotificationManager;


    public NotificationSummaryHook(Context context) {
        LiteRtManager manager = LiteRtManager.getInstance(context);
        mSummarizer = new LiteRtSummarizer(manager);
        mLiteRtNotificationManager = new LiteRtNotificationManager(context, manager);
    }

    public void onNotificationPosted(StatusBarNotification sbn,
            SummaryReadyListener listener) {
        
        String appOrigem = sbn.getPackageName();
        if (!isTargetPackage(appOrigem)) return;

        String text = extractText(sbn);
        if (text == null || text.isEmpty()) return;

        Log.d(TAG, "Resumindo notificação de: " + appOrigem);
        Log.d(TAG, "Texto original: " + text);
        
        // mSummarizer.summarize(text, new LiteRtSummarizer.SummaryCallback() {
        //     @Override
        //     public void onSummaryReady(String summary) {
        //         Log.d(TAG, "Resumo pronto: " + summary);
        //         listener.onSummaryReady(sbn.getKey(), summary);
        //     }

        //     @Override
        //     public void onError(String error) {
        //         Log.w(TAG, "Erro ao resumir: " + error);
        //     }
        // });

        mLiteRtNotificationManager.iniciarResumo(appOrigem, text);
    }

    private boolean isTargetPackage(String packageName) {
        for (String pkg : TARGET_PACKAGES) {
            if (pkg.equals(packageName)) return true;
        }
        return false;
    }

    private String extractText(StatusBarNotification sbn) {
        Bundle extras = sbn.getNotification().extras;
        if (extras == null) return null;

        CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);

        // Prefere o texto longo se disponível
        if (bigText != null && bigText.length() > 0) return bigText.toString();
        if (text != null && text.length() > 0) return text.toString();
        return null;
    }

    public interface SummaryReadyListener {
        void onSummaryReady(String notificationKey, String summary);
    }
}