package fr.raismes;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import fr.raismes.AccesBdd.AccesBdd;
import fr.raismes.Model.Reservation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;

public class AdminController {

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
    private Label noReservationLabel;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    private ObservableList<Reservation> reservationList = FXCollections.observableArrayList();

    private List<String> allRooms = List.of("Salle du second", "Salle Guépin", "Salon d'honneur", "Salle des mariages",
            "Salle mds", "R-LAB");

    private List<String> roomsWithProjector = List.of("Salon d'honneur", "Salle des mariages", "Salle mds");

    public AdminController() {
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

        // Chargez les réservations dès l'initialisation du contrôleur
        loadReservations();
        addDeleteButtonToTable();

        materialComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if ("VIDEO PROJECTEUR".equals(newValue)) {
                updateRoomComboBox();
            } else {
                roomComboBox.setItems(FXCollections.observableArrayList(allRooms));
            }
        });
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

        String formattedDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // Vérifiez les conflits de réservation existants
        if (isMaterialReserved(materialId, formattedDate, time)) {
            showError("Le matériel est déjà réservé pour cette période.");
            return;
        }

        int userId = getCurrentUserId(); // Remplacez par votre méthode pour récupérer l'ID utilisateur

        Reservation newReservation = new Reservation(0, userId, materialId, formattedDate, time, formattedDate, time,
                "En attente", room, materialName);
        boolean success = AccesBdd.insertReservation(newReservation, userId);

        if (success) {
            reservationList.add(newReservation);
            reservationTable.setItems(reservationList);
            reservationTable.refresh();
            errorMessage.setText(""); // Effacer le message d'erreur précédent
            // Mettre à jour l'avertissement si le matériel réservé était un vidéoprojecteur
            if ("VIDEO PROJECTEUR".equals(materialName)) {
                checkMaterialAvailability();
            }
        } else {
            showError("Erreur lors de la réservation.");
        }

        // Réinitialiser les champs après la réservation
        materialComboBox.setValue(null);
        datePicker.setValue(null);
        timeComboBox.setValue(null);
        roomComboBox.setValue(null);
    }

    @FXML
    private void handleLogout() {
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
        ObservableList<Reservation> reservations = AccesBdd.getAllReservations(); // Obtenez toutes les réservations
        reservationList.clear();
        if (reservations != null && !reservations.isEmpty()) {
            reservationList.addAll(reservations);
            reservationTable.setItems(reservationList);
            noReservationLabel.setVisible(false); // Masquer le label s'il y a des réservations
        } else {
            reservationTable.setItems(reservationList);
            noReservationLabel.setVisible(true); // Afficher le label s'il n'y a pas de réservations
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

    private void showError(String message) {
        errorMessage.setText(message);
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void updateRoomComboBox() {
        roomComboBox.setItems(FXCollections.observableArrayList(roomsWithProjector));
    }

    private boolean isMaterialReserved(int materialId, String date, String time) {
        return reservationList.stream().anyMatch(reservation -> reservation.getMaterialId() == materialId
                && reservation.getReservationDate().equals(date) && reservation.getReservationTime().equals(time));
    }

    private void checkMaterialAvailability() {
        Map<String, Long> reservedMaterials = reservationList.stream()
                .collect(Collectors.groupingBy(Reservation::getMaterialName, Collectors.counting()));

        Map<String, Integer> materialAvailability = new HashMap<>();
        materialAvailability.put("ECRAN VIDEO PROJ", 3);
        materialAvailability.put("PC TELETRAVAIL", 3);
        materialAvailability.put("SONO", 1);
        materialAvailability.put("VIDEO PROJECTEUR", 3);
        materialAvailability.put("CAMERA", 1);
        materialAvailability.put("ENCEINTE BLUETOOTH", 2);
        materialAvailability.put("RALLONGE ELECTRIQUE", 3);
        materialAvailability.put("BLOQUE SON", 1);

        reservedMaterials.forEach((materialName, count) -> {
            Integer availableQuantity = materialAvailability.get(materialName);
            if (availableQuantity != null && count >= availableQuantity) {
                showError("Le matériel " + materialName + " n'est plus disponible pour la période sélectionnée.");
            }
        });
    }

    @FXML
    private void handleCreateUser() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Erreur : Nom d'utilisateur ou mot de passe vide.");
            return;
        }

        boolean userCreated = AccesBdd.createUser(username, password);

        if (userCreated) {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setHeaderText(null);
            alert.setContentText("Utilisateur créé avec succès.");
            alert.showAndWait();
        } else {
            showError("Erreur lors de la création de l'utilisateur.");
        }

        // Réinitialiser les champs après la création de l'utilisateur
        usernameField.setText("");
        passwordField.setText("");
    }

    private int getMaterialId(String materialName) {
        switch (materialName) {
            case "ECRAN VIDEO PROJ":
                return 1;
            case "PC TELETRAVAIL":
                return 2;
            case "SONO":
                return 3;
            case "VIDEO PROJECTEUR":
                return 4;
            case "CAMERA":
                return 5;
            case "ENCEINTE BLUETOOTH":
                return 6;
            case "RALLONGE ELECTRIQUE":
                return 7;
            case "BLOQUE SON":
                return 8;
            default:
                return -1;
        }
    }

    private int getCurrentUserId() {

        return 1;
    }
}
