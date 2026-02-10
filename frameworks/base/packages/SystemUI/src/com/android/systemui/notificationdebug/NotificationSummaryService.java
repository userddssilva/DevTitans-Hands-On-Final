/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.systemui.notificationdebug;

import android.util.Log;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Serviço mockado para resumir notificações.
 * Este serviço simula uma chamada de API externa para processar e resumir notificações.
 */
public class NotificationSummaryService {
    private static final String TAG = "NotificationSummaryService";
    private static final boolean DEBUG = true;
    
    private final Executor mExecutor;

    public NotificationSummaryService() {
        mExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * Resumir a lista de notificações e retornar uma resposta resumida.
     * Este método simula uma chamada assíncrona a um serviço externo.
     */
    public CompletableFuture<NotificationSummaryResponse> summarizeNotifications(
            NotificationSummaryRequest request) {
        
        if (DEBUG) {
            Log.d(TAG, "summarizeNotifications: " + request.getAppCount() + " apps, " 
                    + request.getTotalNotificationCount() + " notifications");
        }

        CompletableFuture<NotificationSummaryResponse> future = new CompletableFuture<>();
        
        mExecutor.execute(() -> {
            try {
                // Simular processamento assíncrono
                Thread.sleep(100);
                
                // Gerar resumo das notificações
                String summaryText = generateSummary(request);
                String title = "Resumo de Notificações";
                
                NotificationSummaryResponse response = new NotificationSummaryResponse(
                        title,
                        summaryText,
                        request.getTotalNotificationCount(),
                        request.getAppCount()
                );
                
                if (DEBUG) {
                    Log.d(TAG, "Summary generated: " + summaryText);
                }
                
                future.complete(response);
            } catch (InterruptedException e) {
                Log.e(TAG, "Error summarizing notifications", e);
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }

    private String generateSummary(NotificationSummaryRequest request) {
        StringBuilder summary = new StringBuilder();
        summary.append("Total de notificações: ").append(request.getTotalNotificationCount())
               .append("\nTotal de apps: ").append(request.getAppCount())
               .append("\n\nDetalhes por app:\n");
        
        List<AppNotificationData> apps = request.getAppNotifications();
        for (AppNotificationData app : apps) {
            summary.append("- ").append(app.getPackageName())
                   .append(": ").append(app.getNotificationCount())
                   .append(" notificação(ões)\n");
        }
        
        return summary.toString();
    }
}

