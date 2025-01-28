package fr.raismes;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import fr.raismes.AccesBdd.AccesBdd;
import fr.raismes.Logger.Logger;
import fr.raismes.Model.Reservation;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;

public class HomeController {
    @FXML
    private ComboBox<String> materialComboBox;

    @FXML
    private DatePicker datePicker;

    @FXML
    private ComboBox<String> timeComboBox;

    @FXML
    private ComboBox<String> roomComboBox;

    @FXML
    private TableView<Reservation> reservationTable;

    @FXML
    private TableColumn<Reservation, Integer> materialColumn;

    @FXML
    private TableColumn<Reservation, String> dateColumn;

    @FXML
    private TableColumn<Reservation, String> timeColumn;

    @FXML
    private TableColumn<Reservation, String> roomColumn;

    @FXML
    private TableColumn<Reservation, String> materialNameColumn;

    @FXML
    private TableColumn<Reservation, Void> deleteColumn;

    @FXML
    private Button logoutButton;

    @FXML
    private Label errorMessage;

    @FXML
    private Label warningMessage;

    @FXML
    private Label noReservationLabel;

    private ObservableList<Reservation> reservationList = FXCollections.observableArrayList();

    private List<String> allRooms = List.of("Salle du second", "Salle Guépin", "Salon d'honneur", "Salle des mariages",
            "Salle mds", "R-LAB");

    private List<String> roomsWithProjector = List.of("Salon d'honneur", "Salle des mariages", "Salle mds");

    public HomeController() {
    }

    @FXML
    public void initialize() {
        materialColumn.setCellValueFactory(new PropertyValueFactory<>("materialId"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("reservationDate"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("reservationTime"));
        roomColumn.setCellValueFactory(new PropertyValueFactory<>("room"));
        materialNameColumn.setCellValueFactory(new PropertyValueFactory<>("materialName"));

        materialColumn.prefWidthProperty().bind(reservationTable.widthProperty().multiply(0.2));
        dateColumn.prefWidthProperty().bind(reservationTable.widthProperty().multiply(0.2));
        timeColumn.prefWidthProperty().bind(reservationTable.widthProperty().multiply(0.2));
        roomColumn.prefWidthProperty().bind(reservationTable.widthProperty().multiply(0.2));
        materialNameColumn.prefWidthProperty().bind(reservationTable.widthProperty().multiply(0.2));

        materialComboBox.setItems(FXCollections.observableArrayList("ECRAN VIDEO PROJ", "PC TELETRAVAIL", "SONO",
                "VIDEO PROJECTEUR", "CAMERA", "ENCEINTE BLUETOOTH", "RALLONGE ELECTRIQUE", "BLOQUE SON"));
        roomComboBox.setItems(FXCollections.observableArrayList(allRooms));
        timeComboBox.setItems(
                FXCollections.observableArrayList("07:00", "08:00", "09:00", "10:00", "11:00", "12:00", "13:00",
                        "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00"));

        // Charger les réservations uniquement si l'utilisateur est connecté
        int userId = Auth.getCurrentUserId();
        if (userId != -1) {
            loadReservations();
            addDeleteButtonToTable();
        } else {
            System.err.println("Utilisateur non connecté.");
        }

        materialComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if ("VIDEO PROJECTEUR".equals(newValue)) {
                updateRoomComboBox();
            } else {
                roomComboBox.setItems(FXCollections.observableArrayList(allRooms));
            }
        });
    }

    private void startAutomaticDeletionTask() {
        // Tâche qui vérifie les réservations toutes les heures
        Runnable task = () -> {
            while (true) {
                try {
                    Thread.sleep(3600000); // Attente d'une heure (3600 secondes * 1000 millisecondes)
                    Platform.runLater(() -> {
                        LocalDateTime now = LocalDateTime.now();
                        List<Reservation> reservationsToRemove = new ArrayList<>();

                        for (Reservation reservation : reservationList) {
                            LocalDateTime reservationDateTime = LocalDateTime.of(
                                    LocalDate.parse(reservation.getReservationDate()),
                                    LocalTime.parse(reservation.getReservationTime()));

                            if (now.isAfter(reservationDateTime.plusHours(24))) {
                                // Si plus de 24 heures se sont écoulées, ajouter à la liste des réservations à
                                // supprimer
                                reservationsToRemove.add(reservation);
                            }
                        }

                        // Supprimer de la liste observable
                        reservationList.removeAll(reservationsToRemove);

                        // Supprimer de la base de données
                        reservationsToRemove.forEach(reservation -> {
                            boolean success = AccesBdd.deleteReservation(reservation.getId());
                            if (!success) {
                                Logger.log("Échec de la suppression automatique de la réservation : "
                                        + reservation.getId());
                            }
                        });

                        // Rafraîchir la table après les suppressions
                        reservationTable.refresh();
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        // Démarrage de la tâche dans un nouveau thread
        new Thread(task).start();
    }

    @FXML
    private void handleReserve() {
        String materialName = materialComboBox.getValue();
        LocalDate date = datePicker.getValue();
        String time = timeComboBox.getValue();
        String room = roomComboBox.getValue();

        int materialId = getMaterialId(materialName);
        if (materialId == -1) {
            showError("Erreur : ID de matériel invalide pour " + materialName);
            return;
        }

        int userId = Auth.getCurrentUserId(); // Récupérer l'ID de l'utilisateur connecté
        Logger.log("Tentative de réservation pour l'utilisateur avec userId : " + userId); // Ajout du log

        if (date == null || time == null || room == null) {
            showError("Erreur : Date, heure ou salle non sélectionnée");
            return;
        }

        LocalDateTime selectedDateTime = LocalDateTime.of(date, LocalTime.parse(time));
        LocalDateTime now = LocalDateTime.now();

        // Vérification pour interdire la réservation pour une date/heure passée
        if (selectedDateTime.isBefore(now)) {
            showError("Impossible de réserver pour une date/heure passée.");
            return;
        }

        // Vérification pour interdire la réservation sous 24 heures
        LocalDateTime minDateTimeAllowed = now.plusHours(24);
        if (selectedDateTime.isBefore(minDateTimeAllowed)) {
            showError("Vous ne pouvez pas réserver pour une date/heure inférieure à 24 heures.");
            return;
        }

        // Vérification pour interdire la réservation plus de 3 semaines à l'avance
        LocalDateTime maxDateTimeAllowed = now.plusWeeks(3); // Date limite pour la réservation (3 semaines à partir de
                                                             // maintenant)

        if (selectedDateTime.isAfter(maxDateTimeAllowed)) {
            showError("Vous ne pouvez pas réserver pour une date supérieure à 3 semaines à partir d'aujourd'hui.");
            return;
        }

        String formattedDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // Vérifiez les conflits de réservation existants
        if (isMaterialReserved(materialId, formattedDate, time)) {
            showError("Le matériel est déjà réservé pour cette période.");
            return;
        }

        Reservation newReservation = new Reservation(0, userId, materialId, formattedDate, time, formattedDate, time,
                "En attente", room, materialName);
        boolean success = AccesBdd.insertReservation(newReservation, userId);

        if (success) {
            Logger.log("Réservation créée avec succès pour l'utilisateur avec userId : " + userId); // Ajout du log
            reservationList.add(newReservation);
            reservationTable.setItems(reservationList);
            reservationTable.refresh();
            errorMessage.setText(""); // Effacer le message d'erreur précédent
            // Mettre à jour l'avertissement si le matériel réservé était un vidéoprojecteur
            if ("VIDEO PROJECTEUR".equals(materialName)) {
                checkMaterialAvailability();
            }
        } else {
            Logger.log("Échec de la création de réservation pour l'utilisateur avec userId : " + userId); // Ajout du
                                                                                                          // log
            showError("Erreur lors de la réservation.");
        }

        // Réinitialiser les champs après la réservation
        materialComboBox.setValue(null);
        datePicker.setValue(null);
        timeComboBox.setValue(null);
        roomComboBox.setValue(null);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur de réservation");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private int getMaterialId(String materialName) {
        Map<String, Integer> materialIds = new HashMap<>();
        materialIds.put("ECRAN VIDEO PROJ", 1);
        materialIds.put("PC TELETRAVAIL", 2);
        materialIds.put("SONO", 3);
        materialIds.put("VIDEO PROJECTEUR", 4);
        materialIds.put("CAMERA", 5);
        materialIds.put("ENCEINTE BLUETOOTH", 6);
        materialIds.put("RALLONGE ELECTRIQUE", 7);
        materialIds.put("BLOQUE SON", 8);

        if (materialIds.containsKey(materialName)) {
            return materialIds.get(materialName);
        } else {
            System.err.println("Material not found: " + materialName);
            return -1;
        }
    }

    private boolean isMaterialReserved(int materialId, String date, String time) {
        for (Reservation reservation : reservationList) {
            if (reservation.getMaterialId() == materialId && reservation.getReservationDate().equals(date)
                    && reservation.getReservationTime().equals(time)) {
                return true;
            }
        }
        return false;
    }

    @FXML
    private void handleLogout() {
        Auth.logout(); // Déconnexion de l'utilisateur
        Stage stage = (Stage) logoutButton.getScene().getWindow();
        stage.close();
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/path/to/your/Login.fxml"));
            Parent root = fxmlLoader.load();
            Stage loginStage = new Stage();
            loginStage.setTitle("Login");
            loginStage.setScene(new Scene(root));
            loginStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadReservations() {
        ObservableList<Reservation> reservations = AccesBdd.getReservations(Auth.getCurrentUserId());
        reservationList.clear();
        if (reservations != null && !reservations.isEmpty()) {
            reservationList.addAll(reservations);
            reservationTable.setItems(reservationList);
            noReservationLabel.setVisible(false); // Masquer le label si des réservations existent
        } else {
            noReservationLabel.setVisible(true); // Afficher le label si aucune réservation n'existe
        }
    }

    private void addDeleteButtonToTable() {
        Callback<TableColumn<Reservation, Void>, TableCell<Reservation, Void>> cellFactory = new Callback<TableColumn<Reservation, Void>, TableCell<Reservation, Void>>() {
            @Override
            public TableCell<Reservation, Void> call(final TableColumn<Reservation, Void> param) {
                final TableCell<Reservation, Void> cell = new TableCell<Reservation, Void>() {
                    private final Button btn = new Button("Supprimer");

                    {
                        btn.setOnAction((ActionEvent event) -> {
                            Reservation reservation = getTableView().getItems().get(getIndex());
                            boolean success = AccesBdd.deleteReservation(reservation.getId());
                            if (success) {
                                getTableView().getItems().remove(reservation);
                                reservationTable.refresh();
                            } else {
                                showError("Erreur lors de la suppression de la réservation.");
                            }
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
                return cell;
            }
        };

        deleteColumn.setCellFactory(cellFactory);
    }

    private void handleDeleteReservation(Reservation reservation) {
        boolean success = AccesBdd.deleteReservation(reservation.getReservationId());
        if (success) {
            Logger.log("Réservation supprimée avec succès pour l'utilisateur avec userId : " + Auth.getCurrentUserId());
            reservationList.remove(reservation);
            reservationTable.refresh();
            if (reservationList.isEmpty()) {
                noReservationLabel.setVisible(true); // Afficher le label si aucune réservation n'existe
            }
            // Mettre à jour l'avertissement si le matériel supprimé était un
            // vidéoprojecteur
            if ("VIDEO PROJECTEUR".equals(reservation.getMaterialName())) {
                checkMaterialAvailability();
            }
        } else {
            Logger.log("Échec de la suppression de la réservation pour l'utilisateur avec userId : "
                    + Auth.getCurrentUserId());
            showError("Erreur lors de la suppression de la réservation.");
        }
    }

    private void checkMaterialAvailability() {
        Map<String, Long> materialCount = reservationList.stream()
                .filter(reservation -> "VIDEO PROJECTEUR".equals(reservation.getMaterialName()))
                .collect(Collectors.groupingBy(Reservation::getRoom, Collectors.counting()));

        List<String> unavailableRooms = materialCount.entrySet().stream()
                .filter(entry -> entry.getValue() >= 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (!unavailableRooms.isEmpty()) {
            warningMessage.setText("Attention : Les vidéoprojecteurs sont déjà réservés pour : "
                    + String.join(", ", unavailableRooms));
        } else {
            warningMessage.setText("");
        }
    }

    private void updateRoomComboBox() {
        List<String> availableRooms = allRooms.stream()
                .filter(room -> !roomsWithProjector.contains(room) || isRoomAvailable(room))
                .collect(Collectors.toList());
        roomComboBox.setItems(FXCollections.observableArrayList(availableRooms));
    }

    private boolean isRoomAvailable(String room) {
        return reservationList.stream()
                .filter(reservation -> "VIDEO PROJECTEUR".equals(reservation.getMaterialName()))
                .noneMatch(reservation -> reservation.getRoom().equals(room));
    }
}
