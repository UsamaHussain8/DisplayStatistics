package org.statsapp.statsapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class HelloApplication extends Application {
    private StatsWindow statsWindow;
    @Override
    public void start(Stage stage) throws IOException, SQLException, ClassNotFoundException {
//        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
//        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
//        stage.setTitle("Hello!");
//        stage.setScene(scene);
//        stage.show();
        statsWindow = new StatsWindow();
    }

    public static void main(String[] args) {
        launch();
    }
}