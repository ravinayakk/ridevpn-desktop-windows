package com.ridevpn.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static final String BACKEND_API_URL = "http://35.153.199.173:8080/api/config";

    private static final String RED = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: red;";
    private static final String GREEN = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: green;";

    private boolean isConnected = false;

    private Label statusLabel;
    private Button actionButton;
    private TextField usernameField;
    private PasswordField passwordField;

    // result of asking the server for a config
    private record FetchResult(String config, String error) {}

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("RideVPN Desktop Client");

        statusLabel = new Label("Status: Disconnected");
        statusLabel.setStyle(RED);

        usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(220);

        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(220);

        actionButton = new Button("Connect VPN");
        actionButton.setStyle("-fx-font-size: 14px; -fx-padding: 10px 20px;");

        // pressing enter in the password box clicks Connect
        passwordField.setOnAction(e -> actionButton.fire());

        actionButton.setOnAction(e -> {
            if (!isConnected) {
                connect();
            } else {
                disconnect();
            }
        });

        VBox layout = new VBox(15, statusLabel, usernameField, passwordField, actionButton);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));

        Scene scene = new Scene(layout, 350, 280);
        primaryStage.setScene(scene);
        primaryStage.show();

        // check WG state on startup, match the UI to the real tunnel state.
        // if a tunnel is left over from a previous session but isn't running, clean it up.
        actionButton.setDisable(true);
        new Thread(() -> {
            boolean running = WireGuardManager.isRunning();
            if (!running) {
                WireGuardManager.cleanupIfNotRunning();
            }
            isConnected = running;
            Platform.runLater(() -> {
                if (running) {
                    showConnected();
                }
                actionButton.setDisable(false);
            });
        }).start();
    }

    private void connect() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Enter your username and password");
            return;
        }

        actionButton.setDisable(true);
        statusLabel.setText("Status: Requesting Config...");

        new Thread(() -> {
            FetchResult result = fetchConfigFromBackend(username, password);
            if (result.error() != null) {
                Platform.runLater(() -> {
                    statusLabel.setText(result.error());
                    actionButton.setDisable(false);
                });
                return;
            }

            if (WireGuardManager.connect(result.config())) {
                isConnected = true;
                Platform.runLater(this::showConnected);
            } else {
                Platform.runLater(() -> {
                    statusLabel.setText("Status: Connection Failed (Run as Admin)");
                    actionButton.setDisable(false);
                });
            }
        }).start();
    }

    private void disconnect() {
        actionButton.setDisable(true);
        statusLabel.setText("Status: Disconnecting...");

        new Thread(() -> {
            if (WireGuardManager.disconnect()) {
                isConnected = false;
                Platform.runLater(this::showDisconnected);
            } else {
                Platform.runLater(() -> {
                    statusLabel.setText("Status: Disconnect Failed (Run as Admin)");
                    actionButton.setDisable(false);
                });
            }
        }).start();
    }

    private void showConnected() {
        statusLabel.setText("Status: Connected");
        statusLabel.setStyle(GREEN);
        actionButton.setText("Disconnect VPN");
        actionButton.setDisable(false);
        // Login boxes aren't needed while connected
        usernameField.setVisible(false);
        usernameField.setManaged(false);
        passwordField.setVisible(false);
        passwordField.setManaged(false);
    }

    private void showDisconnected() {
        statusLabel.setText("Status: Disconnected");
        statusLabel.setStyle(RED);
        actionButton.setText("Connect VPN");
        actionButton.setDisable(false);
        usernameField.setVisible(true);
        usernameField.setManaged(true);
        passwordField.setVisible(true);
        passwordField.setManaged(true);
    }

    private FetchResult fetchConfigFromBackend(String username, String password) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            // HTTP Basic auth: "Basic " + base64("username:password")
            String credentials = Base64.getEncoder()
                    .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BACKEND_API_URL))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Basic " + credentials)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code == 200) {
                return new FetchResult(response.body(), null);
            }
            if (code == 401) {
                return new FetchResult(null, "Wrong username or password");
            }
            return new FetchResult(null, "Server error (" + code + ")");
        } catch (Exception ex) {
            ex.printStackTrace();
            return new FetchResult(null, "Can't reach the server");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
