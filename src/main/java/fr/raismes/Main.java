package fr.raismes;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private static Scene scene;
    private static int currentUserId; // Ajouter une variable pour stocker l'ID de l'utilisateur courant

    @Override
    public void start(Stage stage) {
        try {
            scene = new Scene(loadFXML("ConnexionGestion"));
            stage.setScene(scene);
            stage.setTitle("Gestion du Matériel et des Réunions");
            stage.show();

            scheduleExpiredReservationDeletion();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void setRoot(String fxml) throws IOException {
        System.out.println("Chargement de la vue: " + fxml + ".fxml");
        try {
            scene.setRoot(loadFXML(fxml));
            System.out.println("Vue chargée avec succès: " + fxml + ".fxml");
        } catch (IOException e) {
            System.out.println("Erreur lors du chargement de la vue: " + fxml + ".fxml");
            e.printStackTrace();
            throw e;
        }
    }

    public static int getCurrentUserId() {
        return currentUserId;
    }

    public static void setCurrentUserId(int userId) {
        currentUserId = userId;
    }

    private void scheduleExpiredReservationDeletion() {
        // Le reste du code pour la planification des suppressions des réservations
        // expirées
    }

    public static void main(String[] args) {
        launch();
    }
}
