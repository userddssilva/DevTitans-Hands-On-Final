package br.edu.ufam.icomp.devtitans;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Scanner;

public class LiteRt {
    public static final String TAG = "DevTITANS.LiteRt";

    // Defaults (ajuste se quiser)
    private String deviceFolder = "/data/local/tmp";
    private String backend = "gpu";

    private String binPath = deviceFolder + "/litert_lm_main";
    private String modelPath = deviceFolder + "/model.litertlm";
    private String ldLibraryPath = deviceFolder; // onde estão as .so do prebuilt/android_arm64

    public static void main(String[] args) {
        new LiteRt().runLoop();
    }

    private void runLoop() {
        System.out.println("=== DevTITANS LiteRt (Java -> litert_lm_main) ===");
        System.out.println("Digite um prompt e pressione ENTER.");
        System.out.println("Comandos:");
        System.out.println("  /exit");
        System.out.println("  /backend cpu|gpu");
        System.out.println("  /model /caminho/no/device/model.litertlm");
        System.out.println("  /bin   /caminho/no/device/litert_lm_main");
        System.out.println("  /ld    /caminho/no/device (LD_LIBRARY_PATH)");
        System.out.println();

        // Checagens iniciais (só avisos)
        sanityCheck();

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                String line;
                try {
                    line = scanner.nextLine();
                } catch (Exception e) {
                    break; // EOF
                }

                if (line == null) break;
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.equalsIgnoreCase("/exit") || line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("sair")) {
                    break;
                }

                if (handleCommand(line)) {
                    continue;
                }

                // Executa o modelo com esse prompt
                int exit = runModelOnce(line);

                if (exit != 0) {
                    System.out.println("[LiteRt] litert_lm_main saiu com exitCode=" + exit);
                    System.out.println("[LiteRt] Dica: confira se as .so estão em " + ldLibraryPath + " e se o backend/model path estão corretos.");
                }
            }
        }

        System.out.println("Encerrado.");
    }

    private boolean handleCommand(String line) {
        // /backend gpu
        if (line.startsWith("/backend ")) {
            String b = line.substring("/backend ".length()).trim();
            if (!b.equalsIgnoreCase("cpu") && !b.equalsIgnoreCase("gpu")) {
                System.out.println("Uso: /backend cpu|gpu");
                return true;
            }
            backend = b.toLowerCase();
            System.out.println("[LiteRt] backend=" + backend);
            Log.v(TAG, "backend=" + backend);
            return true;
        }

        // /model /data/local/tmp/model.litertlm
        if (line.startsWith("/model ")) {
            modelPath = line.substring("/model ".length()).trim();
            System.out.println("[LiteRt] modelPath=" + modelPath);
            Log.v(TAG, "modelPath=" + modelPath);
            return true;
        }

        // /bin /data/local/tmp/litert_lm_main
        if (line.startsWith("/bin ")) {
            binPath = line.substring("/bin ".length()).trim();
            System.out.println("[LiteRt] binPath=" + binPath);
            Log.v(TAG, "binPath=" + binPath);
            return true;
        }

        // /ld /data/local/tmp
        if (line.startsWith("/ld ")) {
            ldLibraryPath = line.substring("/ld ".length()).trim();
            System.out.println("[LiteRt] LD_LIBRARY_PATH=" + ldLibraryPath);
            Log.v(TAG, "LD_LIBRARY_PATH=" + ldLibraryPath);
            return true;
        }

        return false;
    }

    private void sanityCheck() {
        if (!new File(binPath).exists()) {
            System.out.println("[LiteRt] Aviso: binário não encontrado em " + binPath);
            System.out.println("[LiteRt] Verifique: adb shell ls -l " + binPath);
        }
        if (!new File(modelPath).exists()) {
            System.out.println("[LiteRt] Aviso: modelo não encontrado em " + modelPath);
            System.out.println("[LiteRt] Verifique: adb shell ls -l " + modelPath);
        }
        if ("gpu".equalsIgnoreCase(backend)) {
            // Não dá pra listar *.so facilmente aqui sem shell, mas avisamos o caminho.
            System.out.println("[LiteRt] GPU: usando LD_LIBRARY_PATH=" + ldLibraryPath);
        }
    }

    private int runModelOnce(String prompt) {
        ProcessBuilder pb = new ProcessBuilder(
                binPath,
                "--backend=" + backend,
                "--model_path=" + modelPath,
                "--input_prompt=" + prompt
        );

        // Junta stderr em stdout (pra você ver erros de linker, etc.)
        pb.redirectErrorStream(true);

        // Env igual ao seu comando no shell
        Map<String, String> env = pb.environment();
        if ("gpu".equalsIgnoreCase(backend)) {
            env.put("LD_LIBRARY_PATH", ldLibraryPath);
        }

        Log.v(TAG, "Exec: " + String.join(" ", pb.command()));

        Process p = null;
        try {
            p = pb.start();

            // Imprime toda a saída do modelo
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String outLine;
                while ((outLine = br.readLine()) != null) {
                    System.out.println(outLine);
                    Log.v(TAG, outLine);
                }
            }

            return p.waitFor();
        } catch (IOException e) {
            System.out.println("[LiteRt] IOException ao iniciar o modelo: " + e.getMessage());
            Log.e(TAG, "IOException ao iniciar", e);
            return 127;
        } catch (InterruptedException e) {
            System.out.println("[LiteRt] Interrompido.");
            Log.w(TAG, "Interrompido", e);
            return 130;
        } finally {
            if (p != null) {
                try { p.destroy(); } catch (Exception ignored) {}
            }
        }
    }
}
