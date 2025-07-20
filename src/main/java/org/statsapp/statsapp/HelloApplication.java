package org.statsapp.statsapp;

import dk.dma.ais.binary.SixbitException;
import dk.dma.ais.message.AisMessageException;
import dk.dma.ais.sentence.SentenceException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class HelloApplication extends Application {
    private StatsWindow statsWindow;
    @Override
    public void start(Stage stage) throws IOException, SQLException, ClassNotFoundException, SentenceException, AisMessageException, SixbitException, InterruptedException {
        statsWindow = new StatsWindow();
        Scene scene = new Scene(statsWindow, 800, 400);
        stage.setScene(scene);
        stage.setTitle("Stats App");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}