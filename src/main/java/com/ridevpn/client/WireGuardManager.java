package com.ridevpn.client;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WireGuardManager {

    // wireguard names the tunnel after the config file, make it match the file name
    private static final String INTERFACE_NAME = "ridevpn";

    // The Windows service WireGuard creates for the tunnel
    private static final String SERVICE_NAME = "WireGuardTunnel$" + INTERFACE_NAME;

    private static Path getWireGuardExePath() {
        Path localPath = Paths.get(System.getProperty("user.dir"), "bin", "wireguard.exe");
        if (Files.exists(localPath)) {
            return localPath;
        }
        return Paths.get("C:\\Program Files\\WireGuard\\wireguard.exe");
    }

    // %ProgramData%\RideVPN\ridevpn.conf
    private static Path getConfigPath() {
        String programData = System.getenv().getOrDefault("ProgramData", "C:\\ProgramData");
        return Paths.get(programData, "RideVPN", INTERFACE_NAME + ".conf");
    }

    public static boolean connect(String configContent) {
        try {
            // Remove tunnel if it exists from a previous time the user connected
            uninstallTunnel();

            Path configPath = getConfigPath();
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, configContent);

            // Keep the config file: the tunnel service reads it on start/restart.
            boolean installed = run(
                getWireGuardExePath().toString(),
                "/installtunnelservice",
                configPath.toAbsolutePath().toString()
            ) == 0;
            if (!installed) {
                return false;
            }

            // Set the wg service to manual so it doesn't automatically come on if the users pc reboots
            run("sc.exe", "config", SERVICE_NAME, "start=", "demand");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean disconnect() {
        try {
            boolean ok = uninstallTunnel();
            if (ok) {
                // the config file keeps the private key, so delete it on disconnect
                Files.deleteIfExists(getConfigPath());
            }
            return ok;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // True if the tunnel service exists and is running right now
    public static boolean isRunning() {
        try {
            ProcessBuilder pb = new ProcessBuilder("sc.exe", "query", SERVICE_NAME);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            process.waitFor();
            return output.contains("RUNNING");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // this gets called when the app opens. if a tunnel is left installed but not running,
    // remove it and its config file.
    public static void cleanupIfNotRunning() {
        if (!isRunning()) {
            disconnect();
        }
    }

    private static boolean uninstallTunnel() throws Exception {
        return run(
            getWireGuardExePath().toString(),
            "/uninstalltunnelservice",
            INTERFACE_NAME
        ) == 0;
    }

    private static int run(String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        return pb.start().waitFor();
    }
}
