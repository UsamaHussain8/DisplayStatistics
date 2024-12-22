package org.statsapp.statsapp;

import java.io.IOException;
import java.lang.StringBuilder;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.scene.Scene;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StatsWindow extends AnchorPane
{
    @FXML private TableView statsTable;
    @FXML private TextArea statsArea;

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
                    ids[i] = Integer.parseInt(selectedItems.get(i).get(0)); // Assume getId() fetches the primary key (SR_NO)
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

    public void calculateStats(int[] ids) {
        // Retrieve selected rows from the TableView
        ObservableList<ObservableList<String>> selectedItems = statsTable.getSelectionModel().getSelectedItems();

        if (selectedItems.isEmpty()) {
            statsArea.setText("No rows selected.");
            return;
        }

        int numIds = ids.length;

        // Collect the IDs of the selected rows
//        StringBuilder queryPlaceholders = new StringBuilder();
//        for (int i = 0; i < selectedItems.size(); i++) {
//            queryPlaceholders.append("?");
//            if (i < selectedItems.size() - 1) {
//                queryPlaceholders.append(", ");
//            }
//        }
        String queryPlaceholders = String.join(", ", Collections.nCopies(numIds, "?"));

        // Construct the SQL query
//        String query = "SELECT Role, COUNT(*) AS Num_Players FROM Player WHERE ID IN (" +
//                queryPlaceholders + ") GROUP BY Role";
        String query = "SELECT Role, COUNT(*) AS Num_Players, " +
                "(SELECT COUNT(DISTINCT Role) FROM Player WHERE ID IN (" + queryPlaceholders + ")) AS DistinctRoleCount " +
                "FROM Player " +
                "WHERE ID IN (" + queryPlaceholders + ") " +
                "GROUP BY Role;";

        // Execute the query
        try (PreparedStatement preparedStatement = sqlConn.prepareStatement(query)) {
            // Set the parameters for the placeholders
            for (int i = 0; i < numIds; i++) {
                preparedStatement.setInt(i + 1, ids[i]); // For the first occurrence
                preparedStatement.setInt(i + 1 + numIds, ids[i]); // For the second occurrence
            }

            // Execute the query and process the results
            ResultSet resultSet = preparedStatement.executeQuery();
            StringBuilder statsResult = new StringBuilder();

            statsResult.append("Total Roles: ");
            while (resultSet.next()) {
                String role = resultSet.getString("Role");
                int count = resultSet.getInt("Num_Players");
                int numRoles = resultSet.getInt("DistinctRoleCount");
                statsResult.append(numRoles).append("\n").append("Role: ").append(role).append(", Count: ").append(count).append("\n");
            }

            // Display the results in the TextArea
            statsArea.setText(statsResult.toString());
        }
        catch (SQLException e) {
            e.printStackTrace();
            statsArea.setText("Error fetching statistics: " + e.getMessage());
        }
    }

}
