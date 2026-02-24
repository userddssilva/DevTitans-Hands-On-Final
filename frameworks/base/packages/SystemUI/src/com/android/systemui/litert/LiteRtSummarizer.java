package com.android.systemui.litert;

import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class LiteRtSummarizer {
    private static final String TAG = "LiteRtSummarizer";
    private static final int MAX_INPUT_LENGTH = 500;
    private static final int TIMEOUT_SECONDS = 30;

    private final LiteRtManager mManager;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    public interface SummaryCallback {
        void onSummaryReady(String summary);

        void onError(String error);
    }

    public interface StreamCallback {
        void onToken(String textoParcial);

        void onFinished(String textoFinal);

        void onError(Exception e);
    }

    public LiteRtSummarizer(LiteRtManager manager) {
        mManager = manager;
    }

    public void summarizeAsync(String prompt, StreamCallback callback) {
        new Thread(() -> {
            try {
                runBinaryStreaming(prompt, callback);
            } catch (Exception e) {
                Log.e(TAG, "Erro ao resumir", e);
                callback.onError(e);
            }
        }).start();
    }

    public void summarize(String text, SummaryCallback callback) {
        if (!mManager.isReady()) {
            callback.onError("LiteRt não está pronto");
            return;
        }

        // Não resumir textos curtos
        // if (text == null || text.length() < 100) {
        if (text == null || text.length() < 10) {
            callback.onError("Texto muito curto para resumir");
            return;
        }

        // Truncar textos muito longos
        // String input = text.length() > MAX_INPUT_LENGTH
        // ? text.substring(0, MAX_INPUT_LENGTH) + "..."
        // : text;

        String prompt = "Resuma em uma frase curta em português: " + text;

        mExecutor.execute(() -> {
            try {
                // String result = runBinary(prompt);
                String result = runBinaryStreamingToLog(prompt);
                callback.onSummaryReady(result);
            } catch (Exception e) {
                Log.e(TAG, "Erro ao resumir", e);
                callback.onError(e.getMessage());
            }
        });
    }

    private String runBinary(String prompt) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                mManager.getBinaryPath(),
                "--backend=cpu",
                "--model_path=" + mManager.getModelPath(),
                "--input_prompt=" + prompt);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));

        StringBuilder output = new StringBuilder();
        String line;
        boolean capturandoTexto = false;

        while ((line = reader.readLine()) != null) {
            // output.append(line).append("\n");
            // ---------------------------------
            // heurística simples para ignorar logs de infra e pegar só o texto
            if (line.startsWith("input_prompt:")) {
                // depois dessa linha normalmente vem o texto do modelo
                capturandoTexto = true;
                continue;
            }

            if (line.contains("BenchmarkInfo:")
                    || line.contains("INFO:")
                    || line.contains("Init Phas")
                    || line.contains("- Executor initialization:")
                    || line.contains("- Tokenizer initialization:")
                    || line.contains("Total init time:")
                    || line.contains("Time to first token:")
                    || line.contains("Prefill ")
                    || line.contains("Decode ")
                    || line.startsWith("VERBOSE:")
                    || line.startsWith("I0000")
                    || line.startsWith("E0000")
                    || line.startsWith("F0000")
                    || line.startsWith("---")) {
                // linhas de log técnico, não o texto gerado
                continue;
            }

            if (capturandoTexto && !line.trim().isEmpty()) {
                output.append(line).append("\n");
                // aqui é o “streaming” via log
                Log.d(TAG, "RESUMO_PARCIAL: " + output.toString());
            }
        }

        boolean finished = process.waitFor(TIMEOUT_SECONDS,
                java.util.concurrent.TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new Exception("Timeout ao executar LiteRt");
        }

        return output.toString().trim();
    }

    private void runBinaryStreaming(String prompt, StreamCallback callback) throws Exception {
        String fullPrompt = "Resuma em uma frase curta em português as seguinte notificações: " + prompt;
        String[] command = {
                "/system/bin/litert_lm_main",
                "--backend=cpu",
                "--model_path=/data/litert/gemma-3n-E2B-it-int4.litertlm",
                "--input_prompt=" + fullPrompt
        };

        Log.d(TAG, "Executando: " + String.join(" ", command));

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(false)
                .start();

        BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder resumoAcumulado = new StringBuilder();
        String line;
        boolean capturandoTexto = false;

        while ((line = stdout.readLine()) != null) {

            if (line.startsWith("input_prompt:")) {
                capturandoTexto = true;
                continue;
            }

            if (line.contains("BenchmarkInfo:")
                    || line.contains("INFO:")
                    || line.contains("Init Phas")
                    || line.contains("- Executor initialization:")
                    || line.contains("- Tokenizer initialization:")
                    || line.contains("Total init time:")
                    || line.contains("Time to first token:")
                    || line.contains("Prefill ")
                    || line.contains("Decode ")
                    || line.startsWith("VERBOSE:")
                    || line.startsWith("I0000")
                    || line.startsWith("E0000")
                    || line.startsWith("F0000")
                    || line.startsWith("---")
                    || line.startsWith("\n")
                    || line.isBlank()
                    || line.isEmpty()) {
                // linhas de log técnico, não o texto gerado
                continue;
            }

            Log.d(TAG, "STDOUT: " + line);
            if (capturandoTexto && !line.trim().isEmpty()) {
                resumoAcumulado.append(line);
                Log.d(TAG, "RESUMO_PARCIAL: " + resumoAcumulado);
                callback.onToken(resumoAcumulado.toString());
            }
        }

        process.waitFor();
        Log.d(TAG, "RESUMO_FINAL: " + resumoAcumulado);
        callback.onFinished(resumoAcumulado.toString());
    }

    private String runBinaryStreamingToLog(String prompt) throws Exception {
        String[] command = {
                "/system/bin/litert_lm_main",
                "--backend=cpu",
                "--model_path=/data/litert/gemma-3n-E2B-it-int4.litertlm",
                "--input_prompt=" + prompt
        };

        Log.d(TAG, "Executando: " + String.join(" ", command));

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(false) // manter stdout e stderr separados
                .start();

        BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        StringBuilder resumoAcumulado = new StringBuilder();
        String line;

        boolean capturandoTexto = false;

        // Lê STDOUT em tempo real
        while ((line = stdout.readLine()) != null) {

            // heurística simples para ignorar logs de infra e pegar só o texto
            if (line.startsWith("input_prompt:")) {
                // depois dessa linha normalmente vem o texto do modelo
                capturandoTexto = true;
                continue;
            }

            if (line.contains("BenchmarkInfo:")
                    || line.contains("INFO:")
                    || line.contains("Init Phas")
                    || line.contains("- Executor initialization:")
                    || line.contains("- Tokenizer initialization:")
                    || line.contains("Total init time:")
                    || line.contains("Time to first token:")
                    || line.contains("Prefill ")
                    || line.contains("Decode ")
                    || line.startsWith("VERBOSE:")
                    || line.startsWith("I0000")
                    || line.startsWith("E0000")
                    || line.startsWith("F0000")
                    || line.startsWith("---")) {
                // linhas de log técnico, não o texto gerado
                continue;
            }

            if (capturandoTexto && !line.trim().isEmpty()) {
                resumoAcumulado.append(line);
                // aqui é o “streaming” via log
                Log.d(TAG, "RESUMO_PARCIAL: " + resumoAcumulado.toString());
            }
        }

        // Lê STDERR (logs técnicos) – opcional, só pra debug
        while ((line = stderr.readLine()) != null) {
            Log.d(TAG, "STDERR: " + line);
        }

        int exitCode = process.waitFor();
        Log.d(TAG, "Processo litert_lm_main terminou com código: " + exitCode);
        Log.d(TAG, "RESUMO_FINAL: " + resumoAcumulado.toString());
        return resumoAcumulado.toString().trim();
    }
}