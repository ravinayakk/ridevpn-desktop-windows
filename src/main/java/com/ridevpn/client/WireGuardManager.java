package com.ridevpn.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WireGuardManager {

    private static final String INTERFACE_NAME = "ridevpn";

    private static Path getWireGuardExePath() {
        Path localPath = Paths.get(System.getProperty("user.dir"), "bin", "wireguard.exe");
        if (Files.exists(localPath)) {
            return localPath;
        }
        return Paths.get("C:\\Program Files\\WireGuard\\wireguard.exe");
    }

    public static boolean connect(String configContent) {
        try {
            Path tempConfig = Files.createTempFile(INTERFACE_NAME + "-", ".conf");
            Files.writeString(tempConfig, configContent);

            Path wgExe = getWireGuardExePath();
            ProcessBuilder pb = new ProcessBuilder(
                wgExe.toString(),
                "/installtunnelservice",
                tempConfig.toAbsolutePath().toString()
            );

            Process process = pb.start();
            int exitCode = process.waitFor();
            
            Files.deleteIfExists(tempConfig);
            return exitCode == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean disconnect() {
        try {
            Path wgExe = getWireGuardExePath();
            ProcessBuilder pb = new ProcessBuilder(
                wgExe.toString(),
                "/uninstalltunnelservice",
                INTERFACE_NAME
            );

            Process process = pb.start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}