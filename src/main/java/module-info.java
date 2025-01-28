module fr.raismes {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires javafx.base;
    requires javafx.graphics;

    opens fr.raismes to javafx.fxml;
    opens fr.raismes.Model; // Ouvre le package fr.raismes.Model pour permettre l'accès depuis JavaFX

    exports fr.raismes;
}
