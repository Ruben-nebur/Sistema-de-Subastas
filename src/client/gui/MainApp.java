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
 * Aplicación principal JavaFX para el cliente NetAuction.
 * Gestiona la navegación entre vistas y la conexión con el servidor.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public class MainApp extends Application {

    /** Conexión con el servidor */
    private ServerConnection connection;

    /** Listener para mensajes del servidor */
    private ServerListener serverListener;

    /** Executor para el listener */
    private ExecutorService listenerExecutor;

    /** Token de sesión actual */
    private String sessionToken;

    /** Username del usuario actual */
    private String currentUser;

    /** Rol del usuario actual */
    private String currentRole;

    /** Stage principal */
    private Stage primaryStage;

    /** Panel de contenido principal */
    private BorderPane mainPane;

    /** Barra de estado */
    private Label statusLabel;

    /** Panel de notificaciones */
    private VBox notificationPane;

    /** SSL habilitado */
    private boolean sslEnabled = false;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("NetAuction - Sistema de Subastas");

        // Crear layout principal
        mainPane = new BorderPane();

        // Barra de estado inferior
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setStyle("-fx-background-color: #2c3e50;");
        statusLabel = new Label("Desconectado");
        statusLabel.setStyle("-fx-text-fill: white;");
        statusBar.getChildren().add(statusLabel);
        mainPane.setBottom(statusBar);

        // Panel de notificaciones (derecha)
        notificationPane = new VBox(5);
        notificationPane.setPadding(new Insets(10));
        notificationPane.setStyle("-fx-background-color: #ecf0f1;");
        notificationPane.setPrefWidth(250);
        Label notifTitle = new Label("Notificaciones");
        notifTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        notificationPane.getChildren().add(notifTitle);

        // Mostrar vista de conexión inicial
        showConnectionView();

        Scene scene = new Scene(mainPane, 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> disconnect());
        primaryStage.show();
    }

    /**
     * Muestra la vista de conexión al servidor.
     */
    private void showConnectionView() {
        VBox connectBox = new VBox(15);
        connectBox.setAlignment(Pos.CENTER);
        connectBox.setPadding(new Insets(50));
        connectBox.setMaxWidth(400);

        Label title = new Label("NetAuction");
        title.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #2980b9;");

        Label subtitle = new Label("Sistema de Subastas en Tiempo Real");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        TextField hostField = new TextField(Constants.SERVER_HOST);
        hostField.setPromptText("Host del servidor");
        hostField.setMaxWidth(250);

        TextField portField = new TextField(String.valueOf(Constants.SERVER_PORT));
        portField.setPromptText("Puerto");
        portField.setMaxWidth(250);

        CheckBox sslCheck = new CheckBox("Usar conexión segura (SSL/TLS)");

        Button connectBtn = new Button("Conectar");
        connectBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px;");
        connectBtn.setPrefWidth(250);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");

        connectBtn.setOnAction(e -> {
            try {
                String host = hostField.getText().trim();
                int port = Integer.parseInt(portField.getText().trim());
                sslEnabled = sslCheck.isSelected();

                connectToServer(host, port);
                showLoginView();
            } catch (NumberFormatException ex) {
                errorLabel.setText("Puerto inválido");
            } catch (IOException ex) {
                errorLabel.setText("Error de conexión: " + ex.getMessage());
            }
        });

        connectBox.getChildren().addAll(title, subtitle,
            new Separator(), hostField, portField, sslCheck, connectBtn, errorLabel);

        StackPane centerPane = new StackPane(connectBox);
        centerPane.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #ecf0f1);");
        mainPane.setCenter(centerPane);
    }

    /**
     * Conecta al servidor.
     */
    private void connectToServer(String host, int port) throws IOException {
        connection = new ServerConnection(sslEnabled);
        connection.connect(host, port);

        // Iniciar listener
        serverListener = new ServerListener(connection.getReader());
        serverListener.setNotificationCallback(this::handleNotification);
        listenerExecutor = Executors.newSingleThreadExecutor();
        listenerExecutor.submit(serverListener);

        Platform.runLater(() -> statusLabel.setText("Conectado a " + host + ":" + port +
            (sslEnabled ? " (SSL)" : "")));
    }

    /**
     * Muestra la vista de login.
     */
    public void showLoginView() {
        VBox loginBox = new VBox(15);
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setPadding(new Insets(50));
        loginBox.setMaxWidth(400);

        Label title = new Label("Iniciar Sesión");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TextField userField = new TextField();
        userField.setPromptText("Usuario");
        userField.setMaxWidth(250);

        PasswordField passField = new PasswordField();
        passField.setPromptText("Contraseña");
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
                Message request = new Message(Constants.ACTION_LOGIN);
                request.addData("user", user);
                request.addData("password", pass);

                connection.send(request);
                Message response = serverListener.getNextResponse(10000);

                if (response != null && response.isSuccess()) {
                    sessionToken = response.getDataString("token");
                    currentUser = response.getDataString("username");
                    currentRole = response.getDataString("role");
                    showDashboard();
                } else {
                    errorLabel.setText(response != null ? response.getDataString("message") : "Error de conexión");
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

    /**
     * Muestra la vista de registro.
     */
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
        passField.setPromptText("Contraseña");
        passField.setMaxWidth(250);

        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Confirmar contraseña");
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
                errorLabel.setText("Las contraseñas no coinciden");
                return;
            }

            try {
                Message request = new Message(Constants.ACTION_REGISTER);
                request.addData("user", user);
                request.addData("password", pass);
                request.addData("email", email);

                connection.send(request);
                Message response = serverListener.getNextResponse(10000);

                if (response != null && response.isSuccess()) {
                    successLabel.setText("¡Registro exitoso! Ahora puede iniciar sesión.");
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

    /**
     * Muestra el dashboard principal.
     */
    public void showDashboard() {
        // Menú superior
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
        Button historyBtn = new Button("Mi Historial");
        Button logoutBtn = new Button("Cerrar Sesión");

        Label userLabel = new Label(currentUser + " (" + currentRole + ")");
        userLabel.setStyle("-fx-text-fill: white;");

        menuBar.getChildren().addAll(logo, spacer, userLabel, refreshBtn, createBtn, historyBtn, logoutBtn);
        mainPane.setTop(menuBar);

        // Panel de notificaciones
        mainPane.setRight(notificationPane);

        // Lista de subastas
        refreshAuctionList();

        // Eventos
        refreshBtn.setOnAction(e -> refreshAuctionList());
        createBtn.setOnAction(e -> showCreateAuctionDialog());
        historyBtn.setOnAction(e -> showHistoryView());
        logoutBtn.setOnAction(e -> logout());

        Platform.runLater(() -> statusLabel.setText("Conectado como " + currentUser));
    }

    /**
     * Actualiza la lista de subastas.
     */
    private void refreshAuctionList() {
        try {
            Message request = new Message(Constants.ACTION_LIST_AUCTIONS);
            request.setToken(sessionToken);

            connection.send(request);
            Message response = serverListener.getNextResponse(10000);

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

    /**
     * Crea una tarjeta de subasta.
     */
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
        Label priceLabel = new Label(String.format("%.2f €", price));
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

    /**
     * Muestra el detalle de una subasta.
     */
    private void showAuctionDetail(String auctionId) {
        try {
            Message request = new Message(Constants.ACTION_AUCTION_DETAIL);
            request.setToken(sessionToken);
            request.addData("auctionId", auctionId);

            connection.send(request);
            Message response = serverListener.getNextResponse(10000);

            if (response != null && response.isSuccess()) {
                VBox detailBox = new VBox(15);
                detailBox.setPadding(new Insets(20));

                Button backBtn = new Button("← Volver");
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
                infoGrid.add(new Label(String.format("%.2f €", response.getDataDouble("startPrice", 0))), 1, 1);
                infoGrid.add(new Label("Precio actual:"), 0, 2);
                Label currentPrice = new Label(String.format("%.2f €", response.getDataDouble("currentPrice", 0)));
                currentPrice.setStyle("-fx-font-weight: bold; -fx-text-fill: #27ae60;");
                infoGrid.add(currentPrice, 1, 2);
                infoGrid.add(new Label("Líder actual:"), 0, 3);
                String winner = response.getDataString("currentWinner");
                infoGrid.add(new Label(winner != null ? winner : "Sin pujas"), 1, 3);
                infoGrid.add(new Label("Tiempo restante:"), 0, 4);
                Label timeLabel = new Label(response.getDataString("remainingTime"));
                timeLabel.setStyle("-fx-text-fill: #e74c3c;");
                infoGrid.add(timeLabel, 1, 4);

                // Formulario de puja
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

                        connection.send(bidRequest);
                        Message bidResponse = serverListener.getNextResponse(10000);

                        if (bidResponse != null && bidResponse.isSuccess()) {
                            bidStatus.setText("¡Puja realizada!");
                            bidStatus.setStyle("-fx-text-fill: green;");
                            showAuctionDetail(aId); // Refrescar
                        } else {
                            bidStatus.setText(bidResponse != null ? bidResponse.getDataString("message") : "Error");
                            bidStatus.setStyle("-fx-text-fill: red;");
                        }
                    } catch (NumberFormatException ex) {
                        bidStatus.setText("Cantidad inválida");
                        bidStatus.setStyle("-fx-text-fill: red;");
                    } catch (IOException ex) {
                        bidStatus.setText("Error: " + ex.getMessage());
                        bidStatus.setStyle("-fx-text-fill: red;");
                    }
                });

                bidBox.getChildren().addAll(new Label("Tu puja:"), bidField, new Label("€"), bidBtn, bidStatus);

                detailBox.getChildren().addAll(backBtn, title, desc, new Separator(), infoGrid, new Separator(), bidBox);

                ScrollPane scrollPane = new ScrollPane(detailBox);
                scrollPane.setFitToWidth(true);
                mainPane.setCenter(scrollPane);
            }
        } catch (IOException e) {
            showError("Error cargando detalle: " + e.getMessage());
        }
    }

    /**
     * Muestra el diálogo para crear subasta.
     */
    private void showCreateAuctionDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Nueva Subasta");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField titleField = new TextField();
        titleField.setPromptText("Título");
        TextArea descField = new TextArea();
        descField.setPromptText("Descripción");
        descField.setPrefRowCount(3);
        TextField priceField = new TextField();
        priceField.setPromptText("Precio inicial");
        TextField durationField = new TextField();
        durationField.setPromptText("Duración (minutos)");

        grid.add(new Label("Título:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Descripción:"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label("Precio inicial (€):"), 0, 2);
        grid.add(priceField, 1, 2);
        grid.add(new Label("Duración (min):"), 0, 3);
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

                    connection.send(request);
                    Message response = serverListener.getNextResponse(10000);

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

    /**
     * Muestra la vista de historial.
     */
    private void showHistoryView() {
        try {
            Message request = new Message(Constants.ACTION_MY_HISTORY);
            request.setToken(sessionToken);

            connection.send(request);
            Message response = serverListener.getNextResponse(10000);

            if (response != null && response.isSuccess()) {
                VBox historyBox = new VBox(15);
                historyBox.setPadding(new Insets(20));

                Button backBtn = new Button("← Volver");
                backBtn.setOnAction(e -> refreshAuctionList());

                Label title = new Label("Mi Historial");
                title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

                Label stats = new Label(String.format(
                    "Subastas creadas: %d | Donde he pujado: %d | Ganadas: %d",
                    response.getDataInt("myAuctionsCount", 0),
                    response.getDataInt("biddedCount", 0),
                    response.getDataInt("wonCount", 0)
                ));

                historyBox.getChildren().addAll(backBtn, title, stats);

                ScrollPane scrollPane = new ScrollPane(historyBox);
                scrollPane.setFitToWidth(true);
                mainPane.setCenter(scrollPane);
            }
        } catch (IOException e) {
            showError("Error cargando historial: " + e.getMessage());
        }
    }

    /**
     * Maneja una notificación del servidor.
     */
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
                    messageLabel.setText(notification.getDataString("bidder") + " pujó " +
                        notification.getDataDouble("amount", 0) + "€ en " +
                        notification.getDataString("auctionTitle"));
                    break;

                case Constants.NOTIFY_OUTBID:
                    typeLabel.setText("¡Superado!");
                    typeLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    messageLabel.setText("Has sido superado en " + notification.getDataString("auctionTitle") +
                        ". Nueva puja: " + notification.getDataDouble("newAmount", 0) + "€");
                    break;

                case Constants.NOTIFY_AUCTION_CLOSED:
                    typeLabel.setText("Subasta Finalizada");
                    typeLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    if (notification.getDataBoolean("isDesierta", false)) {
                        messageLabel.setText(notification.getDataString("auctionTitle") + " quedó desierta");
                    } else {
                        messageLabel.setText(notification.getDataString("auctionTitle") +
                            " - Ganador: " + notification.getDataString("winner"));
                    }
                    break;
            }

            notifCard.getChildren().addAll(typeLabel, messageLabel);

            // Añadir al panel (mantener últimas 5)
            if (notificationPane.getChildren().size() > 6) {
                notificationPane.getChildren().remove(1);
            }
            notificationPane.getChildren().add(1, notifCard);
        });
    }

    /**
     * Cierra sesión.
     */
    private void logout() {
        try {
            Message request = new Message(Constants.ACTION_LOGOUT);
            request.setToken(sessionToken);
            connection.send(request);
        } catch (IOException e) {
            // Ignorar
        }

        sessionToken = null;
        currentUser = null;
        currentRole = null;
        showLoginView();
    }

    /**
     * Desconecta del servidor.
     */
    private void disconnect() {
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

    /**
     * Muestra un mensaje de error.
     */
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Muestra un mensaje de información.
     */
    private void showInfo(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Información");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Punto de entrada principal.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
