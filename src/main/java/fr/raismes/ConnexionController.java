package fr.raismes;

import java.io.IOException;

import fr.raismes.AccesBdd.AccesBdd;
import fr.raismes.Model.Reservation;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class ConnexionController {

    @FXML
    private TextField tflogin;

    @FXML
    private PasswordField tfmdp;

    @FXML
    private Label messageErreur;

    @FXML
    private void btnBddClick(ActionEvent event) throws IOException {
        String login = tflogin.getText();
        String mdp = tfmdp.getText();

        // Appeler la méthode checkLoginAndGetRole de la classe AccesBdd
        String role = AccesBdd.checkLoginAndGetRole(login, mdp);

        if (role != null) {
            // Obtenir l'ID de l'utilisateur à partir de la base de données
            int userId = AccesBdd.getUserIdFromDatabase(login);

            // Stocker l'ID de l'utilisateur connecté dans Auth
            Auth.login(userId);

            // Charger les réservations de l'utilisateur spécifique
            ObservableList<Reservation> userReservations = null;

            // Vérifier le rôle de l'utilisateur et obtenir les réservations appropriées
            if ("Agent".equalsIgnoreCase(role.trim())) {
                userReservations = AccesBdd.getReservations(userId);
            } else if ("gerant".equalsIgnoreCase(role.trim())) {
                userReservations = AccesBdd.getAllReservations();
            }

            // Afficher un message de connexion réussie
            messageErreur.setText("Connexion réussie en tant que " + role + " !");

            // Rediriger vers la vue appropriée en fonction du rôle
            if ("Agent".equalsIgnoreCase(role.trim())) {
                Main.setRoot("Accueilagent");
            } else if ("gerant".equalsIgnoreCase(role.trim())) {
                Main.setRoot("Admin");
            } else {
                messageErreur.setText("Rôle non reconnu : " + role);
                System.out.println("Rôle non reconnu : " + role);
            }
        } else {
            // Afficher un message d'erreur si l'authentification échoue
            messageErreur.setText("Identifiant ou mot de passe incorrect !");
            System.out.println("Identifiant ou mot de passe incorrect");
        }
    }
}
