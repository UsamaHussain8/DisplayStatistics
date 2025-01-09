package org.statsapp.statsapp;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

public class PlayersController extends AnchorPane {
    @FXML private AnchorPane mainPane;
    @FXML private GridPane playersGridPane;

    @FXML private TextField nameField;
    @FXML private TextField nationalityField;
    @FXML private TextField clubField;
    @FXML private TextField goalsField;
    @FXML private TextField assistsField;
    @FXML private TextField roleField;
    @FXML private DatePicker debutField;

    @FXML private Button saveUpdateBtn;

    public Scene scene;
    private int id;

    @FXML
    private void initialize() {
        saveUpdateBtn.setOnAction(event -> {
            StringBuilder query = new StringBuilder();
            query.append("UPDATE player SET ");
            query.append(nameField.getUserData().toString() + " = " + "'" + nameField.getText() + "'" + ", ");
            query.append(nationalityField.getUserData().toString() + " = " + "'" + nationalityField.getText() + "'" + ", ");
            query.append(clubField.getUserData().toString() + " = " + "'" + clubField.getText() + "'" + ", ");
            query.append(goalsField.getUserData().toString() + " = " + Integer.parseInt(goalsField.getText()) + ", ");
            query.append(assistsField.getUserData().toString() + " = " + Integer.parseInt(assistsField.getText()) + ", ");
            query.append(roleField.getUserData().toString() + " = " + "'" + roleField.getText() + "'" + ", ");
            query.append(debutField.getUserData().toString() + " = " + "'" + String.valueOf(debutField.getValue().toString()) + "'" + " ");
            query.append(" WHERE ID = 6;");

            try {
                Statement statement = StatsWindow.sqlConn.createStatement();
                int result = statement.executeUpdate(query.toString());
                if (result == 1) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setContentText("Record has been updated successfully");
                    alert.showAndWait();

                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }) ;
    }

    public PlayersController() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("players.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        scene = new Scene(this);

        try {
            fxmlLoader.load();
        }
        catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public int displayPlayers(ObservableList<ObservableList<String>> selectedItems) {
        AtomicInteger i = new AtomicInteger(1);
        playersGridPane.getChildren().stream().forEach(child -> {
            if (child instanceof TextField && child.getUserData() != null) {
                TextField field = (TextField) child;
                field.setText(selectedItems.get(0).get(i.getAndIncrement()));
                }
            else if (child instanceof DatePicker && child.getUserData() != null) {
                DatePicker datePicker = (DatePicker) child;
                datePicker.setValue(LocalDate.parse(selectedItems.get(0).get(i.getAndIncrement())));
            }
        });

        return 1;
    }
}
