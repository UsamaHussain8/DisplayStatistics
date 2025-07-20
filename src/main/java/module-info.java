module org.statsapp.statsapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;
    requires transitive ais.lib.messages;
    requires transitive ais.lib.communication;
    requires transitive ais.lib.json;

    opens org.statsapp.statsapp to javafx.fxml;
    exports org.statsapp.statsapp;
}