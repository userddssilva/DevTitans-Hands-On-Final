package br.edu.ufam.icomp.devtitans;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

public class LiteRt {

    public static final String TAG = "DevTITANS.LiteRt";

    /* ===== Paths oficiais no device ===== */

    private static final String DEFAULT_BIN =
            "/system/bin/litert_lm_main";

    private static final String DEFAULT_MODEL =
            "/data/litert/gemma-3n-E2B-it-int4.litertlm";

    private static final String DEFAULT_BACKEND = "gpu";

    /* ===== Estado configurável ===== */

    private String binPath = DEFAULT_BIN;
    private String modelPath = DEFAULT_MODEL;
    private String backend = DEFAULT_BACKEND;

    public static void main(String[] args) {
        new LiteRt().runLoop();
    }

    private void runLoop() {
        System.out.println("=== DevTITANS LiteRt ===");
        System.out.println("Java -> /vendor/bin/litert_lm_main");
        System.out.println();
        System.out.println("Comandos:");
        System.out.println("  /exit");
        System.out.println("  /backend cpu|gpu");
        System.out.println("  /model /caminho/model.litertlm  (override)");
        System.out.println("  /bin   /caminho/litert_lm_main  (override)");
        System.out.println();

        sanityCheck();

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                String line;

                try {
                    line = scanner.nextLine();
                } catch (Exception e) {
                    break;
                }

                if (line == null) break;
                line = line.trim();
                if (line.isEmpty()) continue;

                if (isExit(line)) {
                    break;
                }

                if (handleCommand(line)) {
                    continue;
                }

                int exitCode = runModelOnce(line);

                if (exitCode != 0) {
                    System.out.println("[LiteRt] Erro: exitCode=" + exitCode);
                    System.out.println("[LiteRt] Verifique SELinux, modelo e backend.");
                }
            }
        }

        System.out.println("Encerrado.");
    }

    private boolean isExit(String line) {
        return line.equalsIgnoreCase("/exit")
                || line.equalsIgnoreCase("exit")
                || line.equalsIgnoreCase("sair");
    }

    private boolean handleCommand(String line) {

        if (line.startsWith("/backend ")) {
            String b = line.substring(9).trim().toLowerCase();
            if (!b.equals("cpu") && !b.equals("gpu")) {
                System.out.println("Uso: /backend cpu|gpu");
                return true;
            }
            backend = b;
            logInfo("backend", backend);
            return true;
        }

        if (line.startsWith("/model ")) {
            modelPath = line.substring(7).trim();
            logInfo("modelPath", modelPath);
            return true;
        }

        if (line.startsWith("/bin ")) {
            binPath = line.substring(5).trim();
            logInfo("binPath", binPath);
            return true;
        }

        return false;
    }

    private void sanityCheck() {
        checkExists("Binário", binPath);
        checkExists("Modelo", modelPath);

        System.out.println("[LiteRt] backend=" + backend);
        System.out.println("[LiteRt] ✓ Verificação de inicialização concluída");
    }

    private void checkExists(String label, String path) {
        File f = new File(path);
        if (!f.exists()) {
            System.out.println("[LiteRt] ❌ ERRO: " + label + " não encontrado:");
            System.out.println("         " + path);
            if (label.equals("Binário")) {
                System.out.println("[LiteRt] Verifique se o app foi instalado corretamente");
            }
        } else {
            System.out.println("[LiteRt] ✓ " + label + " encontrado: " + path);
        }
    }

    private int runModelOnce(String prompt) {

        // Verificação de permissões do binário
        File binFile = new File(binPath);
        if (!binFile.canExecute()) {
            System.out.println("[LiteRt] ERRO: Binário não tem permissão de execução");
            System.out.println("  " + binPath);
            Log.e(TAG, "Binário sem permissão de execução: " + binPath);
            return 126;
        }

        ProcessBuilder pb = new ProcessBuilder(
                binPath,
                "--backend=" + backend,
                "--model_path=" + modelPath,
                "--input_prompt=" + prompt
        );

        // Não redireciona stderr para stdout - captura separadamente
        Map<String, String> env = pb.environment();
        env.put("ANDROID_LOG_TAGS", TAG + ":V");
        
        // Preserva e expande LD_LIBRARY_PATH do sistema
        String existingLD = env.get("LD_LIBRARY_PATH");
        String newLD = "/system/lib64:/vendor/lib64";
        if (existingLD != null && !existingLD.isEmpty()) {
            newLD = newLD + ":" + existingLD;
        }
        env.put("LD_LIBRARY_PATH", newLD);
        
        // Garante que outras variáveis importantes estão presentes
        if (!env.containsKey("PATH")) {
            env.put("PATH", "/system/bin:/system/xbin:/vendor/bin");
        }

        String cmdStr = String.join(" ", pb.command());
        System.out.println("[LiteRt] Exec: " + cmdStr);
        Log.v(TAG, "Exec: " + cmdStr);

        try {
            final Process p = pb.start();

            // Captura stdout
            Thread stdoutThread = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(p.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        System.out.println("[STDOUT] " + line);
                        Log.v(TAG, "[STDOUT] " + line);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Erro lendo stdout", e);
                }
            });

            // Captura stderr
            Thread stderrThread = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(p.getErrorStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        System.out.println("[STDERR] " + line);
                        Log.e(TAG, "[STDERR] " + line);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Erro lendo stderr", e);
                }
            });

            stdoutThread.start();
            stderrThread.start();

            int exitCode = p.waitFor();

            stdoutThread.join(5000);
            stderrThread.join(5000);

            System.out.println("[LiteRt] Código de saída: " + exitCode);
            Log.v(TAG, "Exit code: " + exitCode);

            return exitCode;

        } catch (IOException e) {
            System.out.println("[LiteRt] Erro de IO ao executar: " + e.getMessage());
            Log.e(TAG, "Falha ao executar", e);
            return 127;

        } catch (InterruptedException e) {
            System.out.println("[LiteRt] Execução interrompida");
            Log.w(TAG, "Interrompido", e);
            return 130;
        }
    }

    private void logInfo(String key, String value) {
        System.out.println("[LiteRt] " + key + "=" + value);
        Log.v(TAG, key + "=" + value);
    }
}