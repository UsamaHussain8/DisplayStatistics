module org.statsapp.statsapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;

    opens org.statsapp.statsapp to javafx.fxml;
    exports org.statsapp.statsapp;
}