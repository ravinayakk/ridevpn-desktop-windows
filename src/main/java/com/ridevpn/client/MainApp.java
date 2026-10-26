package com.ridevpn.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static final String BACKEND_API_URL = "http://23.20.22.249:8080/api/v1/vpn/config";
    private boolean isConnected = false;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("RideVPN Desktop Client");

        Label statusLabel = new Label("Status: Disconnected");
        statusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: red;");

        Button actionButton = new Button("Connect VPN");
        actionButton.setStyle("-fx-font-size: 14px; -fx-padding: 10px 20px;");

        actionButton.setOnAction(e -> {
            actionButton.setDisable(true);
            if (!isConnected) {
                statusLabel.setText("Status: Requesting Config...");
                new Thread(() -> {
                    String conf = fetchConfigFromBackend();
                    if (conf != null && WireGuardManager.connect(conf)) {
                        isConnected = true;
                        javafx.application.Platform.runLater(() -> {
                            statusLabel.setText("Status: Connected");
                            statusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: green;");
                            actionButton.setText("Disconnect VPN");
                            actionButton.setDisable(false);
                        });
                    } else {
                        javafx.application.Platform.runLater(() -> {
                            statusLabel.setText("Status: Connection Failed (Run as Admin)");
                            actionButton.setDisable(false);
                        });
                    }
                }).start();
            } else {
                statusLabel.setText("Status: Disconnecting...");
                new Thread(() -> {
                    if (WireGuardManager.disconnect()) {
                        isConnected = false;
                        javafx.application.Platform.runLater(() -> {
                            statusLabel.setText("Status: Disconnected");
                            statusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: red;");
                            actionButton.setText("Connect VPN");
                            actionButton.setDisable(false);
                        });
                    }
                }).start();
            }
        });

        VBox layout = new VBox(20, statusLabel, actionButton);
        layout.setAlignment(Pos.CENTER);

        Scene scene = new Scene(layout, 350, 220);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private String fetchConfigFromBackend() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BACKEND_API_URL))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        launch(args);
    }
}