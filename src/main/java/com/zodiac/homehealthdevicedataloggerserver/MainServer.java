package com.zodiac.homehealthdevicedataloggerserver;

import com.zodiac.homehealthdevicedataloggerserver.Data.ClientInfo;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.stage.Stage;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainServer extends Application {
	private TextArea logArea;
	private Button startButton;
	private Button stopButton;
	private Button clearButton;
	private TableView<ClientInfo> clientTable;
	private Label statusLabel;

	private ServerSocket serverSocket;
	private ExecutorService threadPool; // For managing client threads
	private boolean isServerRunning = false;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("Server Dashboard");

		// Log Area
		logArea = new TextArea();
		logArea.setEditable(false);
		logArea.setPrefHeight(300);

		// Start and Stop Buttons
		startButton = new Button("Start Server");
		stopButton = new Button("Stop Server");
		stopButton.setDisable(true); // Disable Stop button initially

		// Clear Button
		clearButton = new Button("Clear Logs");
		clearButton.setOnAction(e -> clearLogs());

		// Client Management Table
		clientTable = new TableView<>();
		clientTable.getColumns().add(createClientColumn("Client ID", "clientId"));
		clientTable.getColumns().add(createClientColumn("Status", "status"));

		// Status Label
		statusLabel = new Label("Server Status: Inactive");

		// Layout
		VBox controlPanel = new VBox(10, startButton, stopButton, clearButton, statusLabel);
		VBox logPanel = new VBox(10, new Label("Server Logs:"), logArea);
		HBox mainLayout = new HBox(20, controlPanel, logPanel);
		mainLayout.setSpacing(20);
		VBox vBox = new VBox(mainLayout, new Label("Connected Clients:"), clientTable);
		vBox.setSpacing(20);

		// Scene
		Scene scene = new Scene(vBox, 800, 600);
		primaryStage.setScene(scene);
		primaryStage.show();

		// Button Handlers
		startButton.setOnAction(e -> {
			try {
				startServer();
			} catch (UnknownHostException ex) {
				throw new RuntimeException(ex);
			}
		});
		stopButton.setOnAction(e -> stopServer());
	}

	private TableColumn<ClientInfo, String> createClientColumn(String columnName, String property) {
		TableColumn<ClientInfo, String> column = new TableColumn<>(columnName);
		column.setCellValueFactory(data -> new SimpleStringProperty(
				property.equals("clientId") ? data.getValue().getClientId() : data.getValue().getStatus()
		));
		return column;
	}

	private void startServer() throws UnknownHostException {
		if (isServerRunning) {
			updateLog("Server is already running!");
			return;
		}

		statusLabel.setText("Server Status: Active");
		startButton.setDisable(true);
		stopButton.setDisable(false);
		updateLog("Starting server...");
		System.out.println(InetAddress.getLocalHost());

		// Initialize thread pool and server socket
		threadPool = Executors.newFixedThreadPool(10); // Adjust thread pool size as needed
		try {
			serverSocket = new ServerSocket(8100);
			updateLog("Server listening on port 8100...");
			isServerRunning = true;

			// Listen for client connections in a separate thread
			new Thread(() -> {
				try {
					while (isServerRunning) {
						Socket clientSocket = serverSocket.accept();
						String clientId = clientSocket.getInetAddress().toString();
						updateLog("Client connected: " + clientId);

						// Update client table
						updateClientTable(clientId, "Connected");

						// Handle client in a new thread
						ClientHandler clientHandler = new ClientHandler(clientSocket, this);
						threadPool.execute(clientHandler);
					}
				} catch (IOException e) {
					if (isServerRunning) {
						updateLog("Error accepting connection: " + e.getMessage());
					}
				}
			}).start();

		} catch (IOException e) {
			updateLog("Error starting server: " + e.getMessage());
		}
	}

	private void stopServer() {
		if (!isServerRunning) {
			updateLog("Server is not running!");
			return;
		}

		try {
			statusLabel.setText("Server Status: Inactive");
			startButton.setDisable(false);
			stopButton.setDisable(true);
			updateLog("Stopping server...");

			isServerRunning = false;

			// Close server socket
			if (serverSocket != null && !serverSocket.isClosed()) {
				serverSocket.close();
			}

			// Shut down thread pool
			if (threadPool != null && !threadPool.isShutdown()) {
				threadPool.shutdown();
			}

			updateLog("Server stopped.");
		} catch (IOException e) {
			updateLog("Error stopping server: " + e.getMessage());
		}
	}

	private void clearLogs() {
		logArea.clear();
	}

	public void updateLog(String logMessage) {
		logArea.appendText(logMessage + "\n");
	}

	public void updateClientTable(String clientId, String status) {
		ClientInfo clientInfo = new ClientInfo(clientId, status);
		clientTable.getItems().add(clientInfo);
	}

	public void removeClientFromTable(String clientId) {
		clientTable.getItems().removeIf(client -> client.getClientId().equals(clientId));
	}
}
