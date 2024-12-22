package org.statsapp.statsapp;

import java.io.IOException;
import java.lang.StringBuilder;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
    public StatsWindow() throws SQLException, ClassNotFoundException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("StatsWindow.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        String url = "jdbc:sqlserver://localhost:1433;user=admin;password=test;databaseName=Players;encrypt=False"; // DESKTOP-MIQ3N8K
        ResultSet players = getData(url);
        if (players.next())
        {
            System.out.println(players.getString("Name"));
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
}
