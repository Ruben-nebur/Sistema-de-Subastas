package client.gui;

import client.ServerConnection;
import client.ServerListener;
import common.Constants;
import common.Message;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
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
    private boolean sslEnabled = true;

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
                if (auctions != null && auctions.size() > 0) {
                    for (int i = 0; i < auctions.size(); i++) {
                        var auction = auctions.get(i).getAsJsonObject();
                        HBox auctionCard = createAuctionCard(
                            auction.get("id").getAsString(),
                            auction.get("title").getAsString(),
                            auction.get("currentPrice").getAsDouble(),
                            auction.get("remainingTime").getAsString(),
                            auction.get("bidCount").getAsInt(),
                            auction.get("seller").getAsString()
                        );
                        auctionList.getChildren().add(auctionCard);
                    }
                } else {
                    auctionList.getChildren().add(new Label("No hay subastas activas"));
                }

                ScrollPane scrollPane = new ScrollPane(auctionList);
                scrollPane.setFitToWidth(true);
                mainPane.setCenter(scrollPane);
            }
        } catch (IOException e) {
            showError("Error actualizando lista: " + e.getMessage());
        }
    }

    private HBox createAuctionCard(String id, String title, double price, String time, int bids, String seller) {
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

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button viewBtn = new Button("Ver / Pujar");
        viewBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        viewBtn.setOnAction(e -> showAuctionDetail(id));

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
                VBox detailBox = new VBox(15);
                detailBox.setPadding(new Insets(20));

                Button backBtn = new Button("<- Volver");
                backBtn.setOnAction(e -> refreshAuctionList());

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
                            bidStatus.setText("Puja realizada");
                            bidStatus.setStyle("-fx-text-fill: green;");
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

                detailBox.getChildren().addAll(backBtn, title, desc, new Separator(), infoGrid, new Separator(), bidBox);

                ScrollPane scrollPane = new ScrollPane(detailBox);
                scrollPane.setFitToWidth(true);
                mainPane.setCenter(scrollPane);
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

    public static void main(String[] args) {
        launch(args);
    }
}

