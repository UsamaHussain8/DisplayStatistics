package org.statsapp.statsapp;

import java.io.IOException;
import java.lang.StringBuilder;

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
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.*;
import java.util.*;

public class StatsWindow extends AnchorPane {
    @FXML private TableView statsTable;
    @FXML private TextArea statsArea;
    @FXML private ScrollPane statsScrollPane;
    @FXML private Accordion statsAccordion;
    @FXML private Button showStatsBtn;
    @FXML private Button insertBtn;

    private Connection sqlConn;
    private Node originalContent;
    private ChartsController chartsController;
    private Map<String, Integer> nationalitiesData = new HashMap<>();
    private Map<String, Integer> rolesData = new HashMap<>();
    private Map<String, Integer> yearsData = new HashMap<>();

    @FXML
    private void initialize() {
        statsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        insertBtn.setOnAction(event -> {showInsertForm();});

        showStatsBtn.setOnAction(event -> {
            // Load the ChartsWindow.fxml
            chartsController.setChartData(nationalitiesData, rolesData, yearsData);
            // Create a new stage
            Stage stage = new Stage();
            stage.setTitle("Charts");
            stage.setScene(new Scene(chartsController));
            stage.show();
        });

        try {
            populateTableView();

            ObservableList<ObservableList<String>> selectedItems = statsTable.getSelectionModel().getSelectedItems();

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

    public StatsWindow() throws SQLException, ClassNotFoundException {
        //originalContent = this.getChildren().get(0);
        chartsController = new ChartsController();
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
    }

    private Connection establishConnection(String url) throws SQLException {
        Connection sqlServerConnection = DriverManager.getConnection(url);

        return sqlServerConnection;
    }

    private ResultSet getData(String url) {
        ResultSet players;
        try {
            sqlConn = establishConnection(url);
            Statement getPlayersStatement = sqlConn.createStatement();
            players = getPlayersStatement.executeQuery("SELECT * FROM Players.dbo.player");
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return players;
    }

    private void populateTableView() throws SQLException, ClassNotFoundException {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        String url = "jdbc:sqlserver://localhost:1433;user=admin;password=test;databaseName=Players;encrypt=False";

        ResultSet players = getData(url);

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

        String query = "WITH SelectedPlayers AS ( " +
                " SELECT * FROM Player WHERE ID IN (" + queryPlaceholders + ")) " +
                "SELECT 'Nationality' AS Category, Nationality AS Value, COUNT(*) AS Count " +
                "FROM SelectedPlayers " +
                "GROUP BY Nationality " +
                "UNION ALL " +
                "SELECT 'Role' AS Category, Role AS Value, COUNT(*) AS Count " +
                "FROM SelectedPlayers " +
                "GROUP BY Role " +
                "UNION ALL " +
                "SELECT 'Debut Year' AS Category, CAST(YEAR(Debut) AS VARCHAR) AS Value, COUNT(*) AS Count " +
                "FROM SelectedPlayers " +
                "GROUP BY YEAR(Debut);";

        String distinctQuery = "WITH SelectedPlayers AS ( " +
                " SELECT * FROM Player WHERE ID IN (" + queryPlaceholders + ")) " +
                "SELECT 'Nationality' AS Category, COUNT(DISTINCT Nationality) AS Count " +
                "FROM SelectedPlayers " +
                "UNION ALL " +
                "SELECT 'Role' AS Category, COUNT(DISTINCT Role) AS Count " +
                "FROM SelectedPlayers " +
                "UNION ALL " +
                "SELECT 'Debut Year' AS Category, COUNT(DISTINCT YEAR(Debut)) AS Count " +
                "FROM SelectedPlayers;";

        ResultSet resultSet = null;
        try (PreparedStatement preparedStatement = sqlConn.prepareStatement(query);
             PreparedStatement distinctStatement = sqlConn.prepareStatement(distinctQuery)) {
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

//    private void showInsertForm() {
//        VBox formLayout = new VBox(10);
//        formLayout.setPadding(new Insets(20));
//        formLayout.setAlignment(Pos.CENTER_LEFT);
//
//        // Add a title or instruction
//        Label formTitle = new Label("Insert New Record");
//        formTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
//        formLayout.getChildren().add(formTitle);
//
//        // Create a map to store TextField references for each column
//        Map<String, TextField> fieldInputs = new LinkedHashMap<>();
//
//        // Fetch field names dynamically from the database
//        try (Statement statement = sqlConn.createStatement();
//             ResultSet resultSet = statement.executeQuery("SELECT TOP(1) * FROM Player")) {
//
//            ResultSetMetaData metaData = resultSet.getMetaData();
//            int columnCount = metaData.getColumnCount();
//
////            for (int i = 1; i <= columnCount; i++) {
////                String columnName = metaData.getColumnName(i);
////                Label fieldLabel = new Label(columnName + ":");
////                TextField textField = new TextField();
////                fieldInputs.put(columnName, textField);
////
////                HBox fieldRow = new HBox(10, fieldLabel, textField);
////                formLayout.getChildren().add(fieldRow);
////            }
//            for (String column : databaseColumns) { // Assume databaseColumns contains the column names
//                if (!column.equalsIgnoreCase("ID")) { // Skip the ID field
//                    // Create a label and a text field for each column
//                    Label label = new Label(column + ":");
//                    TextField textField = new TextField();
//                    textField.setId(column); // Set the ID for later retrieval
//                    formLayout.getChildren().addAll(label, textField);
//                }
//            }
//        }
//        catch (SQLException e) {
//            e.printStackTrace();
//        }
//
//        // Add Save and Back buttons
//        Button saveButton = new Button("Save");
//        Button backButton = new Button("Back");
//
//        saveButton.setOnAction(event -> saveRecord(fieldInputs));
//        backButton.setOnAction(event -> goBackToOriginalPage());
//
//        HBox buttonRow = new HBox(10, saveButton, backButton);
//        buttonRow.setAlignment(Pos.CENTER);
//        formLayout.getChildren().add(buttonRow);
//
//        // Replace the original content with the form layout
//        this.getChildren().clear(); // Clear original content
//        this.getChildren().add(formLayout);
//    }

    private void showInsertForm() {
        VBox formLayout = new VBox(10);
        formLayout.setPadding(new Insets(20));
        formLayout.setAlignment(Pos.CENTER_LEFT);

        // Add a title or instruction
        Label formTitle = new Label("Insert New Record");
        formTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
        formLayout.getChildren().add(formTitle);

        // Create a GridPane for better alignment of fields
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10); // Horizontal spacing between label and field
        gridPane.setVgap(15); // Vertical spacing between rows
        gridPane.setAlignment(Pos.CENTER); // Center the grid in the form

        int row = 0;

        // Create a map to store TextField references for each column
        Map<String, TextField> fieldInputs = new LinkedHashMap<>();

        // Fetch field names dynamically from the database
        try (Statement statement = sqlConn.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT TOP(1) * FROM Player")) {

            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);

                // Skip the ID column (auto-increment field)
                if (!columnName.equalsIgnoreCase("ID")) {
                    // Create a label and text field for each column
                    Label fieldLabel = new Label(columnName + ":");
                    TextField textField = new TextField();
                    fieldInputs.put(columnName, textField);

//                    // Add label and text field to a horizontal row
//                    HBox fieldRow = new HBox(10, fieldLabel, textField);
//                    fieldRow.setAlignment(Pos.CENTER_LEFT);
//                    formLayout.getChildren().add(fieldRow);
                    // Add label and text field to GridPane
                    GridPane.setHalignment(fieldLabel, HPos.RIGHT); // Align labels to the right
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

        // Add Save and Back buttons
        Button saveButton = new Button("Save");
        Button backButton = new Button("Back");
        saveButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14;");
        backButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 14;");
        saveButton.setOnAction(event -> {
            try {
                saveRecord(fieldInputs);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }); // Save record using field inputs
        backButton.setOnAction(event -> goBackToOriginalPage()); // Go back to the original page

        HBox buttonRow = new HBox(10, saveButton, backButton);
        buttonRow.setAlignment(Pos.CENTER);
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
        try (Statement statement = sqlConn.createStatement()) {
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

