package org.statsapp.statsapp;

import java.io.IOException;
import java.lang.StringBuilder;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.scene.Scene;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.DriverManager;

public class StatsWindow extends AnchorPane
{
    @FXML
    private TableView statsTable;
    private Connection sqlConn;

    @FXML
    private void initialize() {
        statsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        System.out.println("Initialize StatsWindow");
        try {
            populateTableView();
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
        System.out.println("In populateTableView");

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

}
