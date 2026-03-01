package client.gui;

import client.ServerConnection;
import client.ServerListener;
import common.Constants;
import common.Message;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Aplicacion principal JavaFX para NetAuction.
 */
public class MainApp extends Application {

    private ServerConnection connection;
    private ServerListener serverListener;
    private ExecutorService listenerExecutor;
    private String sessionToken;
    private String currentUser;
    private Stage primaryStage;
    private BorderPane mainPane;
    private Label statusLabel;
    private VBox notificationPane;
    private Timeline countdownTimeline;
    private String currentDetailAuctionId;
    private boolean sslEnabled = true;
    private static final DateTimeFormatter BID_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM HH:mm:ss").withZone(ZoneId.systemDefault());

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("NetAuction - Sistema de Subastas");

        mainPane = new BorderPane();

        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setStyle("-fx-background-color: #2c3e50;");
        statusLabel = new Label("Desconectado");
        statusLabel.setStyle("-fx-text-fill: white;");
        statusBar.getChildren().add(statusLabel);
        mainPane.setBottom(statusBar);

        notificationPane = new VBox(5);
        notificationPane.setPadding(new Insets(10));
        notificationPane.setStyle("-fx-background-color: #ecf0f1;");
        notificationPane.setPrefWidth(250);
        Label notifTitle = new Label("Notificaciones");
        notifTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        notificationPane.getChildren().add(notifTitle);

        showConnectionView();

        Scene scene = new Scene(mainPane, 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> disconnect());
        primaryStage.show();
    }

    private void showConnectionView() {
        resetAppState(false);
        VBox connectBox = new VBox(15);
        connectBox.setAlignment(Pos.CENTER);
        connectBox.setPadding(new Insets(50));
        connectBox.setMaxWidth(400);

        Label title = new Label("NetAuction");
        title.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #2980b9;");

        TextField hostField = new TextField(Constants.SERVER_HOST);
        hostField.setPromptText("Host del servidor");
        hostField.setMaxWidth(250);

        TextField portField = new TextField(String.valueOf(Constants.SERVER_PORT));
        portField.setPromptText("Puerto");
        portField.setMaxWidth(250);

        CheckBox sslCheck = new CheckBox("Conexion segura obligatoria (SSL/TLS)");
        sslCheck.setSelected(true);
        sslCheck.setDisable(true);

        Button connectBtn = new Button("Conectar");
        connectBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px;");
        connectBtn.setPrefWidth(250);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");

        connectBtn.setOnAction(e -> {
            try {
                String host = hostField.getText().trim();
                int port = Integer.parseInt(portField.getText().trim());
                sslEnabled = true;

                connectToServer(host, port);
                showLoginView();
            } catch (NumberFormatException ex) {
                errorLabel.setText("Puerto invalido");
            } catch (IOException ex) {
                errorLabel.setText("Error de conexion: " + ex.getMessage());
            }
        });

        connectBox.getChildren().addAll(title, hostField, portField, sslCheck, connectBtn, errorLabel);

        StackPane centerPane = new StackPane(connectBox);
        centerPane.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #ecf0f1);");
        mainPane.setCenter(centerPane);
    }

    private void connectToServer(String host, int port) throws IOException {
        connection = new ServerConnection(sslEnabled);
        connection.connect(host, port);

        serverListener = new ServerListener(connection.getReader());
        serverListener.setNotificationCallback(this::handleNotification);
        listenerExecutor = Executors.newSingleThreadExecutor();
        listenerExecutor.submit(serverListener);

        Platform.runLater(() -> statusLabel.setText("Conectado a " + host + ":" + port +
            (sslEnabled ? " (SSL)" : "")));
    }

    public void showLoginView() {
        resetAppState(connection != null && connection.isConnected());
        VBox loginBox = new VBox(15);
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setPadding(new Insets(50));
        loginBox.setMaxWidth(400);

        Label title = new Label("Iniciar Sesion");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TextField userField = new TextField();
        userField.setPromptText("Usuario");
        userField.setMaxWidth(250);

        PasswordField passField = new PasswordField();
        passField.setPromptText("Contrasena");
        passField.setMaxWidth(250);

        Button loginBtn = new Button("Entrar");
        loginBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        loginBtn.setPrefWidth(250);

        Button registerBtn = new Button("Crear cuenta");
        registerBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        registerBtn.setPrefWidth(250);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");

        loginBtn.setOnAction(e -> {
            String user = userField.getText().trim();
            String pass = passField.getText();

            if (user.isEmpty() || pass.isEmpty()) {
                errorLabel.setText("Complete todos los campos");
                return;
            }

            try {
                Message request = new Message(Constants.ACTION_LOGIN)
                    .addData("user", user)
                    .addData("password", pass);
                Message response = sendRequest(request);

                if (response != null && response.isSuccess()) {
                    sessionToken = response.getDataString("token");
                    currentUser = response.getDataString("username");
                    showDashboard();
                } else {
                    errorLabel.setText(response != null ? response.getDataString("message") : "Error de conexion");
                }
            } catch (IOException ex) {
                errorLabel.setText("Error: " + ex.getMessage());
            }
        });

        registerBtn.setOnAction(e -> showRegisterView());

        loginBox.getChildren().addAll(title, userField, passField, loginBtn, registerBtn, errorLabel);

        StackPane centerPane = new StackPane(loginBox);
        centerPane.setStyle("-fx-background-color: #ffffff;");
        mainPane.setCenter(centerPane);
        mainPane.setRight(null);
    }

    public void showRegisterView() {
        VBox registerBox = new VBox(15);
        registerBox.setAlignment(Pos.CENTER);
        registerBox.setPadding(new Insets(50));
        registerBox.setMaxWidth(400);

        Label title = new Label("Crear Cuenta");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TextField userField = new TextField();
        userField.setPromptText("Usuario");
        userField.setMaxWidth(250);

        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        emailField.setMaxWidth(250);

        PasswordField passField = new PasswordField();
        passField.setPromptText("Contrasena");
        passField.setMaxWidth(250);

        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Confirmar contrasena");
        confirmField.setMaxWidth(250);

        Button registerBtn = new Button("Registrar");
        registerBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        registerBtn.setPrefWidth(250);

        Button backBtn = new Button("Volver");
        backBtn.setPrefWidth(250);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");

        Label successLabel = new Label();
        successLabel.setStyle("-fx-text-fill: green;");

        registerBtn.setOnAction(e -> {
            String user = userField.getText().trim();
            String email = emailField.getText().trim();
            String pass = passField.getText();
            String confirm = confirmField.getText();

            if (user.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                errorLabel.setText("Complete todos los campos");
                return;
            }

            if (!pass.equals(confirm)) {
                errorLabel.setText("Las contrasenas no coinciden");
                return;
            }

            try {
                Message request = new Message(Constants.ACTION_REGISTER)
                    .addData("user", user)
                    .addData("password", pass)
                    .addData("email", email);
                Message response = sendRequest(request);

                if (response != null && response.isSuccess()) {
                    successLabel.setText("Registro exitoso. Ya puede iniciar sesion.");
                    errorLabel.setText("");
                } else {
                    errorLabel.setText(response != null ? response.getDataString("message") : "Error");
                    successLabel.setText("");
                }
            } catch (IOException ex) {
                errorLabel.setText("Error: " + ex.getMessage());
            }
        });

        backBtn.setOnAction(e -> showLoginView());

        registerBox.getChildren().addAll(title, userField, emailField, passField, confirmField,
            registerBtn, backBtn, errorLabel, successLabel);

        StackPane centerPane = new StackPane(registerBox);
        centerPane.setStyle("-fx-background-color: #ffffff;");
        mainPane.setCenter(centerPane);
    }

    public void showDashboard() {
        currentDetailAuctionId = null;
        HBox menuBar = new HBox(10);
        menuBar.setPadding(new Insets(10));
        menuBar.setStyle("-fx-background-color: #2c3e50;");
        menuBar.setAlignment(Pos.CENTER_LEFT);

        Label logo = new Label("NetAuction");
        logo.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("Actualizar");
        Button createBtn = new Button("Nueva Subasta");
        Button logoutBtn = new Button("Cerrar Sesion");

        Label userLabel = new Label(currentUser);
        userLabel.setStyle("-fx-text-fill: white;");

        menuBar.getChildren().addAll(logo, spacer, userLabel, refreshBtn, createBtn, logoutBtn);
        mainPane.setTop(menuBar);

        mainPane.setRight(notificationPane);

        refreshAuctionList();

        refreshBtn.setOnAction(e -> refreshAuctionList());
        createBtn.setOnAction(e -> showCreateAuctionDialog());
        logoutBtn.setOnAction(e -> logout());

        Platform.runLater(() -> statusLabel.setText("Conectado como " + currentUser));
    }

    private void refreshAuctionList() {
        try {
            Message request = new Message(Constants.ACTION_LIST_AUCTIONS);
            request.setToken(sessionToken);
            Message response = sendRequest(request);

            if (response != null && response.isSuccess()) {
                VBox auctionList = new VBox(10);
                auctionList.setPadding(new Insets(20));

                Label title = new Label("Subastas Activas");
                title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
                auctionList.getChildren().add(title);

                var auctions = response.getData().getAsJsonArray("auctions");
                List<CountdownTarget> countdownTargets = new ArrayList<>();
                if (auctions != null && auctions.size() > 0) {
                    for (int i = 0; i < auctions.size(); i++) {
                        var auction = auctions.get(i).getAsJsonObject();
                        HBox auctionCard = createAuctionCard(
                            auction.get("id").getAsString(),
                            auction.get("title").getAsString(),
                            auction.get("currentPrice").getAsDouble(),
                            auction.get("remainingTime").getAsString(),
                            auction.get("remainingSeconds").getAsLong(),
                            auction.get("bidCount").getAsInt(),
                            auction.get("seller").getAsString(),
                            countdownTargets
                        );
                        auctionList.getChildren().add(auctionCard);
                    }
                } else {
                    stopCountdown();
                    auctionList.getChildren().add(new Label("No hay subastas activas"));
                }

                ScrollPane scrollPane = new ScrollPane(auctionList);
                scrollPane.setFitToWidth(true);
                mainPane.setCenter(scrollPane);
                startCountdown(countdownTargets, this::refreshAuctionList);
            } else if (response != null) {
                showError(response.getDataString("message"));
            }
        } catch (IOException e) {
            showError("Error actualizando lista: " + e.getMessage());
        }
    }

    private HBox createAuctionCard(String id, String title, double price, String time, long remainingSeconds,
                                   int bids, String seller, List<CountdownTarget> countdownTargets) {
        HBox card = new HBox(15);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");

        VBox info = new VBox(5);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label sellerLabel = new Label("Vendedor: " + seller);
        sellerLabel.setStyle("-fx-text-fill: #7f8c8d;");
        info.getChildren().addAll(titleLabel, sellerLabel);

        VBox priceBox = new VBox(5);
        priceBox.setAlignment(Pos.CENTER);
        Label priceLabel = new Label(String.format("%.2f EUR", price));
        priceLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
        Label bidsLabel = new Label(bids + " pujas");
        priceBox.getChildren().addAll(priceLabel, bidsLabel);

        VBox timeBox = new VBox(5);
        timeBox.setAlignment(Pos.CENTER);
        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #e74c3c;");
        timeBox.getChildren().add(timeLabel);
        countdownTargets.add(new CountdownTarget(timeLabel, remainingSeconds));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button viewBtn = new Button("Ver / Pujar");
        viewBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        viewBtn.setOnAction(e -> showAuctionDetail(id));
        card.setOnMouseClicked(e -> showAuctionDetail(id));

        card.getChildren().addAll(info, spacer, priceBox, timeBox, viewBtn);
        return card;
    }

    private void showAuctionDetail(String auctionId) {
        try {
            Message request = new Message(Constants.ACTION_AUCTION_DETAIL);
            request.setToken(sessionToken);
            request.addData("auctionId", auctionId);
            Message response = sendRequest(request);

            if (response != null && response.isSuccess()) {
                currentDetailAuctionId = auctionId;
                VBox detailBox = new VBox(15);
                detailBox.setPadding(new Insets(20));

                Button backBtn = new Button("<- Volver");
                backBtn.setOnAction(e -> {
                    currentDetailAuctionId = null;
                    refreshAuctionList();
                });

                Label title = new Label(response.getDataString("title"));
                title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

                Label desc = new Label(response.getDataString("description"));
                desc.setWrapText(true);

                GridPane infoGrid = new GridPane();
                infoGrid.setHgap(20);
                infoGrid.setVgap(10);
                infoGrid.add(new Label("Vendedor:"), 0, 0);
                infoGrid.add(new Label(response.getDataString("seller")), 1, 0);
                infoGrid.add(new Label("Precio inicial:"), 0, 1);
                infoGrid.add(new Label(String.format("%.2f EUR", response.getDataDouble("startPrice", 0))), 1, 1);
                infoGrid.add(new Label("Precio actual:"), 0, 2);
                Label currentPrice = new Label(String.format("%.2f EUR", response.getDataDouble("currentPrice", 0)));
                currentPrice.setStyle("-fx-font-weight: bold; -fx-text-fill: #27ae60;");
                infoGrid.add(currentPrice, 1, 2);
                infoGrid.add(new Label("Lider actual:"), 0, 3);
                String winner = response.getDataString("currentWinner");
                infoGrid.add(new Label(winner != null ? winner : "Sin pujas"), 1, 3);
                infoGrid.add(new Label("Tiempo restante:"), 0, 4);
                Label timeLabel = new Label(response.getDataString("remainingTime"));
                timeLabel.setStyle("-fx-text-fill: #e74c3c;");
                infoGrid.add(timeLabel, 1, 4);
                infoGrid.add(new Label("Estado:"), 0, 5);
                infoGrid.add(new Label(response.getDataString("auctionStatus")), 1, 5);
                infoGrid.add(new Label("Numero de pujas:"), 0, 6);
                infoGrid.add(new Label(String.valueOf(response.getDataInt("bidCount", 0))), 1, 6);

                VBox bidHistoryBox = new VBox(8);
                Label historyTitle = new Label("Historial de pujas");
                historyTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
                bidHistoryBox.getChildren().add(historyTitle);

                var recentBids = response.getData().getAsJsonArray("recentBids");
                if (recentBids != null && recentBids.size() > 0) {
                    for (int i = 0; i < recentBids.size(); i++) {
                        var bid = recentBids.get(i).getAsJsonObject();
                        HBox bidRow = new HBox(10);
                        bidRow.setAlignment(Pos.CENTER_LEFT);
                        bidRow.setPadding(new Insets(8));
                        bidRow.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dfe6e9;");

                        Label bidderLabel = new Label(bid.get("bidder").getAsString());
                        bidderLabel.setStyle("-fx-font-weight: bold;");
                        Label amountLabel = new Label(String.format("%.2f EUR", bid.get("amount").getAsDouble()));
                        Label dateLabel = new Label(formatBidTimestamp(bid.get("timestamp").getAsLong()));
                        dateLabel.setStyle("-fx-text-fill: #636e72;");

                        Region bidSpacer = new Region();
                        HBox.setHgrow(bidSpacer, Priority.ALWAYS);
                        bidRow.getChildren().addAll(bidderLabel, bidSpacer, amountLabel, dateLabel);
                        bidHistoryBox.getChildren().add(bidRow);
                    }
                } else {
                    bidHistoryBox.getChildren().add(new Label("Todavia no hay pujas registradas."));
                }

                HBox bidBox = new HBox(10);
                bidBox.setAlignment(Pos.CENTER_LEFT);
                TextField bidField = new TextField();
                bidField.setPromptText("Cantidad");
                bidField.setPrefWidth(150);
                Button bidBtn = new Button("Realizar Puja");
                bidBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white;");
                Label bidStatus = new Label();

                final String aId = auctionId;
                bidBtn.setOnAction(e -> {
                    try {
                        double amount = Double.parseDouble(bidField.getText().trim());
                        Message bidRequest = new Message(Constants.ACTION_BID);
                        bidRequest.setToken(sessionToken);
                        bidRequest.addData("auctionId", aId);
                        bidRequest.addData("amount", amount);
                        Message bidResponse = sendRequest(bidRequest);

                        if (bidResponse != null && bidResponse.isSuccess()) {
                            showAuctionDetail(aId);
                        } else {
                            bidStatus.setText(bidResponse != null ? bidResponse.getDataString("message") : "Error");
                            bidStatus.setStyle("-fx-text-fill: red;");
                        }
                    } catch (NumberFormatException ex) {
                        bidStatus.setText("Cantidad invalida");
                        bidStatus.setStyle("-fx-text-fill: red;");
                    } catch (IOException ex) {
                        bidStatus.setText("Error: " + ex.getMessage());
                        bidStatus.setStyle("-fx-text-fill: red;");
                    }
                });

                bidBox.getChildren().addAll(new Label("Tu puja:"), bidField, new Label("EUR"), bidBtn, bidStatus);

                startCountdown(List.of(new CountdownTarget(timeLabel, response.getDataLong("remainingSeconds", 0))),
                    () -> {
                        currentDetailAuctionId = null;
                        refreshAuctionList();
                    });

                detailBox.getChildren().addAll(backBtn, title, desc, new Separator(), infoGrid,
                    new Separator(), bidHistoryBox, new Separator(), bidBox);

                ScrollPane scrollPane = new ScrollPane(detailBox);
                scrollPane.setFitToWidth(true);
                mainPane.setCenter(scrollPane);
            } else if (response != null) {
                showError(response.getDataString("message"));
            }
        } catch (IOException e) {
            showError("Error cargando detalle: " + e.getMessage());
        }
    }

    private void showCreateAuctionDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Nueva Subasta");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField titleField = new TextField();
        titleField.setPromptText("Titulo");
        TextArea descField = new TextArea();
        descField.setPromptText("Descripcion");
        descField.setPrefRowCount(3);
        TextField priceField = new TextField();
        priceField.setPromptText("Precio inicial");
        TextField durationField = new TextField();
        durationField.setPromptText("Duracion (minutos)");

        grid.add(new Label("Titulo:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Descripcion:"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label("Precio inicial (EUR):"), 0, 2);
        grid.add(priceField, 1, 2);
        grid.add(new Label("Duracion (min):"), 0, 3);
        grid.add(durationField, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                try {
                    Message request = new Message(Constants.ACTION_CREATE_AUCTION);
                    request.setToken(sessionToken);
                    request.addData("title", titleField.getText().trim());
                    request.addData("description", descField.getText().trim());
                    request.addData("startPrice", Double.parseDouble(priceField.getText().trim()));
                    request.addData("durationMinutes", Integer.parseInt(durationField.getText().trim()));
                    Message response = sendRequest(request);

                    if (response != null && response.isSuccess()) {
                        showInfo("Subasta creada correctamente");
                        refreshAuctionList();
                    } else {
                        showError(response != null ? response.getDataString("message") : "Error");
                    }
                } catch (Exception e) {
                    showError("Error: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void handleNotification(Message notification) {
        Platform.runLater(() -> {
            String action = notification.getAction();
            VBox notifCard = new VBox(5);
            notifCard.setPadding(new Insets(10));
            notifCard.setStyle("-fx-background-color: white; -fx-border-color: #3498db; -fx-border-radius: 5;");

            Label typeLabel = new Label();
            Label messageLabel = new Label();
            messageLabel.setWrapText(true);

            switch (action) {
                case Constants.NOTIFY_NEW_BID:
                    typeLabel.setText("Nueva Puja");
                    typeLabel.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                    messageLabel.setText(notification.getDataString("bidder") + " pujo " +
                        notification.getDataDouble("amount", 0) + " EUR en " +
                        notification.getDataString("auctionTitle"));
                    break;
                case Constants.NOTIFY_OUTBID:
                    typeLabel.setText("Superado");
                    typeLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    messageLabel.setText("Has sido superado en " + notification.getDataString("auctionTitle") +
                        ". Nueva puja: " + notification.getDataDouble("newAmount", 0) + " EUR");
                    break;
                case Constants.NOTIFY_AUCTION_CLOSED:
                    typeLabel.setText("Subasta Finalizada");
                    typeLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    if (notification.getDataBoolean("isDesierta", false)) {
                        messageLabel.setText(notification.getDataString("auctionTitle") + " quedo desierta");
                    } else {
                        messageLabel.setText(notification.getDataString("auctionTitle") +
                            " - Ganador: " + notification.getDataString("winner"));
                    }
                    break;
                default:
                    return;
            }

            notifCard.getChildren().addAll(typeLabel, messageLabel);

            if (notificationPane.getChildren().size() > 6) {
                notificationPane.getChildren().remove(1);
            }
            notificationPane.getChildren().add(1, notifCard);
        });
    }

    private void logout() {
        Message response = null;
        try {
            Message request = new Message(Constants.ACTION_LOGOUT);
            request.setToken(sessionToken);
            response = sendRequest(request);
        } catch (IOException e) {
            showError("No se pudo cerrar la sesion: " + e.getMessage());
        }

        if (response != null && !response.isSuccess()) {
            showError(response.getDataString("message"));
        }

        resetAppState(true);
        showLoginView();
    }

    private void disconnect() {
        resetAppState(false);
        if (serverListener != null) {
            serverListener.stop();
        }
        if (listenerExecutor != null) {
            listenerExecutor.shutdown();
            try {
                listenerExecutor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                listenerExecutor.shutdownNow();
            }
        }
        if (connection != null) {
            connection.disconnect();
        }
    }

    private Message sendRequest(Message request) throws IOException {
        ensureConnected();
        if (serverListener != null) {
            serverListener.clearResponses();
        }
        connection.send(request);
        Message response = serverListener != null ? serverListener.getNextResponse(10000) : null;
        if (response == null) {
            throw new IOException("Timeout esperando respuesta del servidor");
        }
        return response;
    }

    private void ensureConnected() throws IOException {
        if (connection == null || !connection.isConnected() || serverListener == null || !serverListener.isRunning()) {
            throw new IOException("La conexion con el servidor no esta disponible");
        }
    }

    private void resetAppState(boolean keepConnection) {
        sessionToken = null;
        currentUser = null;
        currentDetailAuctionId = null;
        stopCountdown();

        if (notificationPane != null) {
            notificationPane.getChildren().setAll(notificationPane.getChildren().get(0));
        }

        if (mainPane != null) {
            mainPane.setTop(null);
            mainPane.setRight(null);
        }

        if (statusLabel != null) {
            String status = keepConnection && connection != null && connection.isConnected()
                ? "Conectado"
                : "Desconectado";
            statusLabel.setText(status);
        }
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showInfo(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Informacion");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void startCountdown(List<CountdownTarget> targets, Runnable onExpire) {
        stopCountdown();

        if (targets == null || targets.isEmpty()) {
            return;
        }

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            boolean expired = false;
            for (CountdownTarget target : targets) {
                if (target.remainingSeconds > 0) {
                    target.remainingSeconds--;
                }

                target.label.setText(formatRemainingTime(target.remainingSeconds));
                if (target.remainingSeconds == 0) {
                    expired = true;
                }
            }

            if (expired) {
                stopCountdown();
                onExpire.run();
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void stopCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
    }

    private String formatRemainingTime(long remainingSeconds) {
        if (remainingSeconds <= 0) {
            return "Finalizada";
        }

        long minutes = remainingSeconds / 60;
        long seconds = remainingSeconds % 60;
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }

    private String formatBidTimestamp(long timestamp) {
        return BID_TIME_FORMATTER.format(Instant.ofEpochMilli(timestamp));
    }

    private static class CountdownTarget {
        private final Label label;
        private long remainingSeconds;

        private CountdownTarget(Label label, long remainingSeconds) {
            this.label = label;
            this.remainingSeconds = Math.max(0, remainingSeconds);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

