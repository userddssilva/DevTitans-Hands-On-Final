package com.android.systemui.litert;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class LiteRtNotificationManager {
    private static final String TAG = "LiteRtNotifManager";
    private static final String CHANNEL_ID = "litert_resumo";
    private static final String CHANNEL_NAME = "Resumo de Notificações (LiteRt)";
    private static final int NOTIF_ID = 0x1A7E; // ID fixo para sempre atualizar a mesma notif

    private final Context mContext;
    private final NotificationManager mNm;
    private LiteRtManager mLiteRtManager;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    public LiteRtNotificationManager(Context context, LiteRtManager manager) {
        mContext = context;
        mNm = context.getSystemService(NotificationManager.class);
        mLiteRtManager = manager;
        criarCanal();
    }

    private void criarCanal() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW // LOW = sem som, sem vibração
        );
        channel.setDescription("Resumos gerados por IA local (Gemma)");
        mNm.createNotificationChannel(channel);
    }

    /** Mostra "Resumindo..." e começa o streaming */
    public void iniciarResumo(String appOrigem, String textoOriginal) {
        // Mostra estado inicial
        publicar("Resumindo...", "Aguarde, a IA está processando...", true);

        LiteRtSummarizer summarizer = new LiteRtSummarizer(mLiteRtManager);
        summarizer.summarizeAsync(textoOriginal, new LiteRtSummarizer.StreamCallback() {
            @Override
            public void onToken(String textoParcial) {
                // Atualiza a notificação a cada linha nova
                mMainHandler.post(() ->
                    publicar("Resumo de " + appOrigem, textoParcial, true)
                );
            }

            @Override
            public void onFinished(String textoFinal) {
                // Atualização final: remove o spinner de progresso
                mMainHandler.post(() ->
                    publicar("Resumo de " + appOrigem, textoFinal, false)
                );
                Log.d(TAG, "Resumo concluído para: " + appOrigem);
            }

            @Override
            public void onError(Exception e) {
                mMainHandler.post(() ->
                    publicar("Resumo indisponível", "Não foi possível resumir.", false)
                );
                Log.e(TAG, "Erro no resumo", e);
            }
        });
    }

    private void publicar(String titulo, String texto, boolean emProgresso) {
        Notification.Builder builder = new Notification.Builder(mContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(new Notification.BigTextStyle().bigText(texto))
                .setOnlyAlertOnce(true)   // não toca som a cada update
                .setOngoing(emProgresso); // mantém fixo enquanto processa

        if (emProgresso) {
            builder.setProgress(0, 0, true); // spinner infinito
        }

        mNm.notify(NOTIF_ID, builder.build());
    }
}