package org.statsapp.statsapp;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.StringBuilder;

import dk.dma.ais.binary.SixbitException;
import dk.dma.ais.bus.AisBusSocket;
import dk.dma.ais.json_decoder_helpers.Decoder;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessageException;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.packet.AisPacketParser;
import dk.dma.ais.packet.AisPacketReader;
import dk.dma.ais.reader.AisReader;
import dk.dma.ais.reader.AisReaders;
import dk.dma.ais.reader.AisStreamReader;
import dk.dma.ais.reader.AisUdpReader;
import dk.dma.ais.sentence.CommentBlock;
import dk.dma.ais.sentence.SentenceException;
import dk.dma.ais.sentence.Vdm;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleBooleanProperty;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.function.Consumer;

public class StatsWindow extends AnchorPane {
    @FXML private TableView statsTable;
    @FXML private TextArea statsArea;
    @FXML private ScrollPane statsScrollPane;
    @FXML private Accordion statsAccordion;
    @FXML private Button showStatsBtn;
    @FXML private Button insertBtn;
    @FXML private Button updateBtn;

    public static Connection mySqlConn;
    private Node originalContent;
    private ChartsController chartsController;
    private PlayersController playersController;
    private ObservableList<ObservableList<String>> selectedItems;
    private Map<String, Integer> nationalitiesData = new HashMap<>();
    private Map<String, Integer> rolesData = new HashMap<>();
    private Map<String, Integer> yearsData = new HashMap<>();

    public static ArrayList<String> columnNames = new ArrayList<>();

    @FXML
    private void initialize() {
        statsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        insertBtn.setOnAction(event -> {showInsertForm();});

        updateBtn.setOnAction(event -> {
            if(selectedItems.size() != 0) {
                Stage stage = new Stage();
                stage.setTitle("Update Players");
                stage.setScene(playersController.scene);
                stage.show();

                int updateResult = playersController.displayPlayers(selectedItems);
//                if(updateResult == 1)
//                    stage.close();
            }
            else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Invalid");
                alert.setContentText("No items selected");
                alert.showAndWait();
            }
        });

        showStatsBtn.setOnAction(event -> {
            if (chartsController != null) {
                // Load the ChartsWindow.fxml
                chartsController.setChartData(nationalitiesData, rolesData, yearsData);
                // Create a new stage
                Stage stage = new Stage();
                stage.setTitle("Charts");
                stage.setScene(chartsController.scene);
                stage.show();
            }
        });

        try {
            populateTableView();

            selectedItems = statsTable.getSelectionModel().getSelectedItems();

            statsTable.getSelectionModel().getSelectedItems().addListener((ListChangeListener<ObservableList<String>>) change -> {
                int[] ids = new int[selectedItems.size()];
                for (int i = 0; i < selectedItems.size(); i++) {
                    ids[i] = Integer.parseInt(selectedItems.get(i).get(0));
                }
                calculateStats(ids);
            });
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public StatsWindow() throws SQLException, ClassNotFoundException, SentenceException, AisMessageException, SixbitException, InterruptedException, IOException {
        //originalContent = this.getChildren().get(0);
        mySqlConn = establishConnection();
        chartsController = new ChartsController();
        playersController = new PlayersController();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("stats-window.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to load fxml file" + e.getMessage(), e);
        }
        if (!this.getChildren().isEmpty()) {
            originalContent = this.getChildren().get(0);
        }

        String aisSentence = "!AIVDM,1,1,,A,181:Jqh02c1Qra`E46I<@9n@059l,0*30";
        Vdm vdm = new Vdm();
        try {
            vdm.parse(aisSentence);
        }
        catch(SentenceException ex) {
            throw new SentenceException(ex.getMessage());
        }

        UDPReceiver.receiveAisMessage();

        AisReader reader = AisReaders.createUdpReader(4001);
        reader.registerHandler(new Consumer<AisMessage>() {
            @Override
            public void accept(AisMessage aisMessage) {
                System.out.println("message id: " + aisMessage.getMsgId());
            }
        });
        // Register error handler if available
        try {
            reader.registerPacketHandler(packet -> {
                System.out.println("Raw packet received: " + packet);
            });
            System.out.println("Packet handler registered successfully");
        } catch (Exception e) {
            System.out.println("Could not register packet handler: " + e.getMessage());
        }

        System.out.println("Starting AIS reader on port 4001...");
        reader.start();
        System.out.println("AIS reader started, waiting for messages...");
        System.out.println("Reader status: " + reader.getStatus());

        // Add a timeout mechanism instead of blocking indefinitely
        Thread readerThread = new Thread(() -> {
            try {
                reader.join();
            } catch (InterruptedException e) {
                System.out.println("Reader thread interrupted");
            }
        });

        readerThread.start();

        // Wait for a reasonable time and check status
        Thread.sleep(5000);
        System.out.println("After 5 seconds - Reader status: " + reader.getStatus());

        // Keep the main thread alive
        while (readerThread.isAlive()) {
            Thread.sleep(1000);
            System.out.println("Still waiting... Reader alive: " + reader.getStatus());
        }
        String aisSentence1 = "!AIVDM,2,1,9,B,53nFBv01SJ<thHp6220H4heHTf2222222222221?50:454o<`9QSlUDp,0*09";
        String aisSentence2 = "!AIVDM,2,2,9,B,888888888888880,2*2E";

        AisPacket packet = AisPacket.from(aisSentence1);
        System.out.println("Packet read AIS message as: " + packet.getAisMessage());
        Vdm vdm1 = new Vdm();
        vdm1.parse(aisSentence1);
        vdm1.parse(aisSentence2);

//        AisMessage aisMessage1 = AisMessage.getInstance(vdm1);
//        System.out.println(aisMessage1);

//        String aisSentenceForJson = "!AIVDM,1,1,,A,181:Jqh02c1Qra`E46I<@9n@059l,0*30";
//        Decoder decoder = new Decoder(aisSentenceForJson);
//        String json = decoder.decode(true);
//        System.out.println(json);
    }

    private Connection establishConnection() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/players";
        String username = "root";
        String password = "SqlDatabase<3";
        Connection mySqlConnection = null;
        try {
            // Load the JDBC driver (optional in recent versions)
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Create connection
            mySqlConnection = DriverManager.getConnection(url, username, password);
            System.out.println("Connected to MySQL successfully!");


        } catch (ClassNotFoundException e) {
            System.out.println("MySQL JDBC driver not found.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("Connection failed.");
            e.printStackTrace();
        }

        return mySqlConnection;
    }

    private ResultSet getData() {
        ResultSet players;
        try {
            mySqlConn = establishConnection();
            Statement getPlayersStatement = mySqlConn.createStatement();
            players = getPlayersStatement.executeQuery("SELECT * FROM players.player");
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return players;
    }

    private void populateTableView() throws SQLException, ClassNotFoundException {
        ResultSet players = getData();

        ResultSetMetaData metaData = players.getMetaData();
        int columnCount = metaData.getColumnCount();

        // Clear existing columns in TableView
        statsTable.getColumns().clear();

        // Dynamically create TableColumns
        for (int i = 1; i <= columnCount; i++) {
            final int columnIndex = i;
            TableColumn<ObservableList<String>, String> column = new TableColumn<>(metaData.getColumnLabel(i));
            column.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(cellData.getValue().get(columnIndex - 1)));
            statsTable.getColumns().add(column);
        }

        // Clear existing data
        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();

        // Populate data rows dynamically
        while (players.next()) {
            ObservableList<String> row = FXCollections.observableArrayList();
            for (int i = 1; i <= columnCount; i++) {
                row.add(players.getString(i));      // Fetch data for each column
            }
            data.add(row);
        }

        // Bind data to TableView
        statsTable.setItems(data);

        return;
    }

    public ResultSet calculateStats(int[] ids) {
        // Retrieve selected rows from the TableView
        ObservableList<ObservableList<String>> selectedItems = statsTable.getSelectionModel().getSelectedItems();

        if (selectedItems.isEmpty()) {
            StringBuilder error = new StringBuilder("No players selected");
            return null;
        }

        int numIds = ids.length;
        String queryPlaceholders = String.join(", ", Collections.nCopies(numIds, "?"));

        String query = "SELECT 'Nationality' AS Category, Nationality AS Value, COUNT(*) AS Count" +
        "FROM (" +
                "SELECT * FROM players.player WHERE ID IN (" + queryPlaceholders + ")" +
        ") AS SelectedPlayers" +
        "GROUP BY Nationality" +
        "UNION ALL" +
        "SELECT 'Role' AS Category, Role AS Value, COUNT(*) AS Count" +
        "FROM (" +
                "SELECT * FROM players.player WHERE ID IN (" + queryPlaceholders + ")" +
        ") AS SelectedPlayers" +
        "GROUP BY Role" +
        "UNION ALL" +
        "SELECT 'Debut Year' AS Category, CAST(YEAR(Debut) AS CHAR) AS Value, COUNT(*) AS Count" +
        "FROM (" +
                "SELECT * FROM players.player WHERE ID IN (" + queryPlaceholders + ")" +
        ") AS SelectedPlayers" +
        "GROUP BY YEAR(Debut);";

        String distinctQuery = "SELECT 'Nationality' AS Category, COUNT(DISTINCT Nationality) AS Count" +
                "FROM (" +
                "    SELECT * FROM players.player WHERE ID IN (" + queryPlaceholders + ")" +
                ") AS SelectedPlayers" +
                "UNION ALL" +
                "SELECT 'Role' AS Category, COUNT(DISTINCT Role) AS Count" +
                "FROM (" +
                "    SELECT * FROM players.player WHERE ID IN (" + queryPlaceholders + ")" +
                ") AS SelectedPlayers" +
                "UNION ALL" +
                "SELECT 'Debut Year' AS Category, COUNT(DISTINCT YEAR(Debut)) AS Count" +
                "FROM (" +
                "    SELECT * FROM players.player WHERE ID IN (" + queryPlaceholders + ")" +
                ") AS SelectedPlayers;";

        ResultSet resultSet = null;
        try (PreparedStatement preparedStatement = mySqlConn.prepareStatement(query);
             PreparedStatement distinctStatement = mySqlConn.prepareStatement(distinctQuery)) {
            // Set the parameters for the placeholders
            for (int i = 0; i < numIds; i++) {
                // Set the parameters for the first occurrence
                preparedStatement.setInt(i + 1, ids[i]);
                distinctStatement.setInt(i + 1, ids[i]);
            }

            // Execute the query and process the results
            resultSet = preparedStatement.executeQuery();
            ResultSet distinctResultSet = distinctStatement.executeQuery();
            displayStats(resultSet, distinctResultSet);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }

        return resultSet;
    }

    public void displayStats(ResultSet statsResult, ResultSet distinctResultSet) throws SQLException {
        Map<String, Integer> distinctCounts = new HashMap<>();
        while (distinctResultSet.next()) {
            String category = distinctResultSet.getString("Category");
            int count = distinctResultSet.getInt("Count");
            distinctCounts.put(category, count);
        }

        Map<String, List<String>> categoryValues = new LinkedHashMap<>();

        while (statsResult.next()) {
            String category = statsResult.getString("Category");
            String value = statsResult.getString("Value");
            int count = statsResult.getInt("Count");
            categoryValues.putIfAbsent(category, new ArrayList<>());
            categoryValues.get(category).add(value + " (" + count + ")");

            switch (category) {
                case "Nationality":
                    nationalitiesData.put(value, count);
                    break;
                case "Role":
                    rolesData.put(value, count);
                    break;
                case "Debut Year":
                    yearsData.put(value, count);
                    break;
            }
        }

        // Update or create TitledPanes
        statsAccordion.getPanes().clear();
        for (Map.Entry<String, List<String>> entry : categoryValues.entrySet()) {
            String category = entry.getKey();
            List<String> values = entry.getValue();
            int distinctCount = distinctCounts.getOrDefault(category, 0);

            VBox content = new VBox();
            content.setSpacing(10);
            for (String value : values) {
                CheckBox checkBox = new CheckBox(value);
                content.getChildren().add(checkBox);
            }

            TitledPane titledPane = new TitledPane(category + " (" + distinctCount + ")", content);
            statsAccordion.getPanes().add(titledPane);
        }
        chartsController.setChartData(nationalitiesData, rolesData, yearsData);
    }

    private void showInsertForm() {
        VBox formLayout = new VBox(10);
        formLayout.setPadding(new Insets(20));
        formLayout.setAlignment(Pos.TOP_LEFT);

        // Add a title or instruction
        Label formTitle = new Label("Insert New Record");
        formTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
        formLayout.getChildren().add(formTitle);

        // Create a GridPane for better alignment of fields
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10); // Horizontal spacing between label and field
        gridPane.setVgap(15); // Vertical spacing between rows
        //gridPane.setAlignment(Pos.CENTER); // Center the grid in the form

        // Set column constraints
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setHgrow(Priority.NEVER); // Label column doesn't grow
        labelColumn.setPrefWidth(100); // Set a preferred width for labels

        ColumnConstraints textFieldColumn = new ColumnConstraints();
        textFieldColumn.setHgrow(Priority.ALWAYS); // TextField column takes remaining space
        textFieldColumn.setFillWidth(true);

        gridPane.getColumnConstraints().addAll(labelColumn, textFieldColumn);

        int row = 0;

        // Create a map to store TextField references for each column
        Map<String, TextField> fieldInputs = new LinkedHashMap<>();

        // Fetch field names dynamically from the database
        try (Statement statement = mySqlConn.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT FROM players.player LIMIT 1;")) {

            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                columnNames.add(columnName);

                // Skip the ID column (auto-increment field)
                if (!columnName.equalsIgnoreCase("ID")) {
                    // Create a label and text field for each column
                    Label fieldLabel = new Label(columnName + ":");
                    TextField textField = new TextField();
                    fieldInputs.put(columnName, textField);

                    // Make the TextField take the remaining width
                    GridPane.setHgrow(textField, Priority.ALWAYS);
                    textField.setMaxWidth(Double.MAX_VALUE);

                    // Add label and text field to GridPane
                    //GridPane.setHalignment(fieldLabel, HPos.RIGHT); // Align labels to the right
                    gridPane.add(fieldLabel, 0, row);
                    gridPane.add(textField, 1, row);
                    row++;
                }
            }
        }

        catch (SQLException e) {
            e.printStackTrace();
        }

        // Add the gridPane to the form layout
        formLayout.getChildren().add(gridPane);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(formLayout);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // Add the ScrollPane to the scene (or layout)
        VBox layout = new VBox(scrollPane);
        layout.setPadding(new Insets(10));
        layout.setAlignment(Pos.TOP_LEFT);

        // Add Save and Back buttons
        Button saveButton = new Button("Save");
        Button backButton = new Button("Back");
        saveButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14;");
        backButton.setStyle("-fx-background-color: #9d9393; -fx-text-fill: #0e0e0e; -fx-font-size: 14;");
        saveButton.setOnAction(event -> {
            try {
                saveRecord(fieldInputs);
            }
            catch (SQLException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }); // Save record using field inputs
        backButton.setOnAction(event -> goBackToOriginalPage()); // Go back to the original page

        HBox buttonRow = new HBox(10, saveButton, backButton);
        buttonRow.setAlignment(Pos.CENTER_LEFT);
        formLayout.getChildren().add(buttonRow);

        // Replace the original content with the form layout
        this.getChildren().clear(); // Clear original content
        this.getChildren().add(formLayout);
    }

    private void saveRecord(Map<String, TextField> fieldInputs) throws SQLException, ClassNotFoundException {
        StringBuilder columnNames = new StringBuilder();
        StringBuilder values = new StringBuilder();

        fieldInputs.forEach((columnName, textField) -> {
            columnNames.append(columnName).append(", ");
            values.append("'").append(textField.getText()).append("', ");
        });

        // Remove trailing commas
        columnNames.setLength(columnNames.length() - 2);
        values.setLength(values.length() - 2);

        String insertQuery = "INSERT INTO Player (" + columnNames + ") VALUES (" + values + ")";
        try (Statement statement = mySqlConn.createStatement()) {
            statement.executeUpdate(insertQuery);
            System.out.println("Record inserted successfully!");
            populateTableView();
            goBackToOriginalPage(); // Return to the original layout after saving
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void goBackToOriginalPage() {
        this.getChildren().clear(); // Clear form layout
        this.getChildren().add(originalContent); // Add the original content back
    }

}

