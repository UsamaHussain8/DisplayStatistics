package org.statsapp.statsapp;

import java.io.IOException;
import java.lang.StringBuilder;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Scene;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.DriverManager;
import java.util.*;

public class StatsWindow extends AnchorPane
{
    @FXML private TableView statsTable;
    @FXML private TextArea statsArea;
    @FXML private ScrollPane statsScrollPane;
    @FXML private Accordion statsAccordion;

    private Connection sqlConn;

    @FXML
    private void initialize() {
        statsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
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
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("stats-window.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to load fxml file" + e.getMessage(), e);
        }
    }

    private Connection establishConnection(String url) throws SQLException
    {
        Connection sqlServerConnection = DriverManager.getConnection(url);

        return sqlServerConnection;
    }

    private ResultSet getData(String url) {
        ResultSet players;
        try {
            sqlConn = establishConnection(url);
            Statement getPlayersStatement = sqlConn.createStatement();
            players = getPlayersStatement.executeQuery("SELECT * FROM Players.dbo.player");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return players;
    }

    private void populateTableView() throws SQLException, ClassNotFoundException {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        String url = "jdbc:sqlserver://localhost:1433;user=admin;password=test;databaseName=Players;encrypt=False"; // DESKTOP-MIQ3N8K

        ResultSet players = getData(url);

        var metaData = players.getMetaData();
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
            statsArea.setText("No rows selected.");
            return null;
        }

        int numIds = ids.length;
        String queryPlaceholders = String.join(", ", Collections.nCopies(numIds, "?"));

        String query =
                "WITH SelectedPlayers AS ( " +
                        "    SELECT * FROM Player WHERE ID IN (" + queryPlaceholders + ") " +
                        ") " +
                        "SELECT 'Nationality' AS Category, CAST(Nationality AS VARCHAR) AS Value, COUNT(*) AS Count " +
                        "FROM SelectedPlayers " +
                        "GROUP BY Nationality " +
                        "UNION ALL " +
                        "SELECT 'Role' AS Category, CAST(Role AS VARCHAR) AS Value, COUNT(*) AS Count " +
                        "FROM SelectedPlayers " +
                        "GROUP BY Role " +
                        "UNION ALL " +
                        "SELECT 'Debut Year' AS Category, CAST(YEAR(Debut) AS VARCHAR) AS Value, COUNT(*) AS Count " +
                        "FROM SelectedPlayers " +
                        "GROUP BY YEAR(Debut) " +
                        "UNION ALL " +
                        "SELECT 'Nationality' AS Category, 'Nationalities' AS Value, COUNT(DISTINCT Nationality) AS Count " +
                        "FROM SelectedPlayers " +
                        "UNION ALL " +
                        "SELECT 'Roles' AS Category, 'Roles' AS Value, COUNT(DISTINCT Role) AS Count " +
                        "FROM SelectedPlayers " +
                        "UNION ALL " +
                        "SELECT 'Years' AS Category, 'Years' AS Value, COUNT(DISTINCT YEAR(Debut)) AS Count " +
                        "FROM SelectedPlayers;";

        ResultSet resultSet = null;
        try (PreparedStatement preparedStatement = sqlConn.prepareStatement(query)) {
            // Set the parameters for the placeholders
            for (int i = 0; i < numIds; i++) {
                // Set the parameters for the first occurrence
                preparedStatement.setInt(i + 1, ids[i]);
            }

            // Execute the query and process the results
            resultSet = preparedStatement.executeQuery();
            displayStats(resultSet);
        }

        catch (SQLException e) {
            e.printStackTrace();
            statsArea.setText("Error fetching statistics: " + e.getMessage());
        }

        return resultSet;
    }

    public void displayStats(ResultSet statsResult) throws SQLException {
        Map<String, List<String>> categoryValues = new LinkedHashMap<>();
        Map<String, Integer> distinctCounts = new HashMap<>();
        while (statsResult.next()) {
            String category = statsResult.getString("Category");
            String value = statsResult.getString("Value");
            int count = statsResult.getInt("Count");
            categoryValues.putIfAbsent(category, new ArrayList<>());
            if (!value.equalsIgnoreCase(category)) {
                categoryValues.get(category).add(value + " (" + count + ")");
            }
            else {
                distinctCounts.put(category, count);
            }
        }

        // Update or create TitledPanes
        for (Map.Entry<String, List<String>> entry : categoryValues.entrySet()) {
            String category = entry.getKey();
            List<String> values = entry.getValue();
            int distinctCount = distinctCounts.getOrDefault(category, 0);

            // Find existing TitledPane or create a new one
            TitledPane titledPane = statsAccordion.getPanes()
                    .stream()
                    .filter(tp -> tp.getText().startsWith(category))
                    .findFirst()
                    .orElse(null);

            if (titledPane == null) {
                // Create a new TitledPane
                VBox content = new VBox();
                content.setSpacing(10);
                titledPane = new TitledPane(category + " (" + distinctCount + ")", content);
                statsAccordion.getPanes().add(titledPane);
            }

            // Update content of the TitledPane
            VBox content = (VBox) titledPane.getContent();
            content.getChildren().clear();
            for (String value : values) {
                CheckBox checkBox = new CheckBox(value);
                content.getChildren().add(checkBox);
            }

            // Update title with the new count
            titledPane.setText(category + " (" + distinctCount + ")");
        }

            // Add to Accordion
            //statsAccordion.getPanes().add(titledPane);
    }
}

