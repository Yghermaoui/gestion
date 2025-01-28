package fr.raismes.AccesBdd;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import fr.raismes.Logger.Logger;
import fr.raismes.Model.Reservation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class AccesBdd {

  private static final String DB_URL = "jdbc:mysql://localhost:3307/gestionmaterielreunion";
  private static final String DB_USER = "root";
  private static final String DB_PASSWORD = "youcef";

  private static Connection getConnection() {
    try {
      return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    } catch (SQLException ex) {
      Logger.log("Connexion à la base de données échouée : " + ex.getMessage());
      return null;
    }
  }

  public static ObservableList<Reservation> getReservations(int userId) {
    Connection conn = getConnection();
    if (conn == null) {
      return null;
    }

    ObservableList<Reservation> reservations = FXCollections.observableArrayList();
    try {
      String sql = "SELECT r.id, r.user_id, r.material_id, r.reservation_date, r.reservation_time, " +
          "r.return_date, r.return_time, r.status, r.room, m.name as material_name " +
          "FROM reservations r " +
          "JOIN materials m ON r.material_id = m.id " +
          "WHERE r.user_id = ?";
      PreparedStatement statement = conn.prepareStatement(sql);
      statement.setInt(1, userId);
      ResultSet resultSet = statement.executeQuery();

      while (resultSet.next()) {
        int id = resultSet.getInt("id");
        int materialId = resultSet.getInt("material_id");
        String reservationDate = resultSet.getString("reservation_date");
        String reservationTime = resultSet.getString("reservation_time");
        String returnDate = resultSet.getString("return_date");
        String returnTime = resultSet.getString("return_time");
        String status = resultSet.getString("status");
        String materialName = resultSet.getString("material_name");
        String room = resultSet.getString("room");

        Reservation reservation = new Reservation(id, userId, materialId, reservationDate, reservationTime,
            returnDate, returnTime, status, room, materialName);
        reservations.add(reservation);
      }
    } catch (SQLException ex) {
      Logger.log("Récupération des réservations échouée : " + ex.getMessage());
      return null;
    } finally {
      try {
        conn.close();
      } catch (SQLException ex) {
        Logger.log("Échec de la fermeture de la connexion : " + ex.getMessage());
      }
    }
    Logger.log("Récupération des réservations pour l'utilisateur " + userId + " réussie");
    return reservations;
  }

  public static ObservableList<Reservation> getAllReservations() {
    Connection conn = getConnection();
    if (conn == null) {
      return null;
    }

    ObservableList<Reservation> reservations = FXCollections.observableArrayList();
    try {
      String sql = "SELECT r.id, r.user_id, r.material_id, r.reservation_date, r.reservation_time, " +
          "r.return_date, r.return_time, r.status, r.room, m.name as material_name " +
          "FROM reservations r " +
          "JOIN materials m ON r.material_id = m.id";
      PreparedStatement statement = conn.prepareStatement(sql);
      ResultSet resultSet = statement.executeQuery();

      while (resultSet.next()) {
        int id = resultSet.getInt("id");
        int userId = resultSet.getInt("user_id");
        int materialId = resultSet.getInt("material_id");
        String reservationDate = resultSet.getString("reservation_date");
        String reservationTime = resultSet.getString("reservation_time");
        String returnDate = resultSet.getString("return_date");
        String returnTime = resultSet.getString("return_time");
        String status = resultSet.getString("status");
        String materialName = resultSet.getString("material_name");
        String room = resultSet.getString("room");

        Reservation reservation = new Reservation(id, userId, materialId, reservationDate,
            reservationTime, returnDate, returnTime, status, room, materialName);
        reservations.add(reservation);
      }
    } catch (SQLException ex) {
      Logger.log("Récupération de toutes les réservations échouée : " + ex.getMessage());
      return null;
    } finally {
      try {
        conn.close();
      } catch (SQLException ex) {
        Logger.log("Échec de la fermeture de la connexion : " + ex.getMessage());
      }
    }
    Logger.log("Récupération de toutes les réservations réussie");
    return reservations;
  }

  public static int getUserIdFromDatabase(String username) {
    Connection conn = getConnection();
    if (conn == null) {
      return -1; // Valeur par défaut si la connexion échoue
    }

    try {
      String sql = "SELECT id FROM users WHERE username = ?";
      PreparedStatement preparedStatement = conn.prepareStatement(sql);
      preparedStatement.setString(1, username);
      ResultSet resultSet = preparedStatement.executeQuery();

      if (resultSet.next()) {
        int userId = resultSet.getInt("id");
        Logger.log("ID de l'utilisateur récupéré avec succès : " + userId);
        return userId;
      } else {
        Logger.log("Aucun utilisateur trouvé avec le nom d'utilisateur spécifié");
        return -1; // Retourne une valeur par défaut si aucun utilisateur n'est trouvé
      }
    } catch (SQLException ex) {
      Logger.log("Échec de la récupération de l'ID de l'utilisateur : " + ex.getMessage());
      return -1; // Retourne une valeur par défaut en cas d'erreur SQL
    } finally {
      try {
        conn.close();
      } catch (SQLException ex) {
        Logger.log("Échec de la fermeture de la connexion : " + ex.getMessage());
      }
    }
  }

  public static String checkLoginAndGetRole(String username, String password) {
    Connection conn = getConnection();
    if (conn == null) {
      return null;
    }

    try {
      String sql = "SELECT role FROM users WHERE username = ? AND password = ?";
      PreparedStatement preparedStatement = conn.prepareStatement(sql);
      preparedStatement.setString(1, username);
      preparedStatement.setString(2, password);
      ResultSet resultSet = preparedStatement.executeQuery();

      if (resultSet.next()) {
        String role = resultSet.getString("role");
        Logger.log("Utilisateur authentifié avec succès  : " + role);
        return role;
      }
    } catch (SQLException ex) {
      Logger.log("Échec de l'authentification de l'utilisateur : " + ex.getMessage());
    } finally {
      try {
        conn.close();
      } catch (SQLException ex) {
        Logger.log("Échec de la fermeture de la connexion : " + ex.getMessage());
      }
    }
    return null;
  }

  public static boolean createUser(String username, String password) {
    Connection conn = getConnection();
    if (conn == null) {
      return false;
    }

    try {
      String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, 'agent')";
      PreparedStatement preparedStatement = conn.prepareStatement(sql);
      preparedStatement.setString(1, username);
      preparedStatement.setString(2, password);
      int rowsAffected = preparedStatement.executeUpdate();

      if (rowsAffected > 0) {
        Logger.log("Utilisateur créé avec succès : " + username);
        return true;
      } else {
        Logger.log("Échec de la création de l'utilisateur : aucune ligne affectée");
      }
    } catch (SQLException ex) {
      Logger.log("Échec de la création de l'utilisateur : " + ex.getMessage());
    } finally {
      try {
        conn.close();
      } catch (SQLException ex) {
        Logger.log("Échec de la fermeture de la connexion : " + ex.getMessage());
      }
    }
    return false;
  }

  public static boolean insertReservation(Reservation reservation, int userId) {
    Connection conn = getConnection();
    if (conn == null) {
      Logger.log("Échec de la connexion à la base de données.");
      return false;
    }

    try {
      // Vérifier la quantité de matériel disponible
      if (!isMaterialAvailable(reservation.getMaterialId())) {
        Logger.log("Pas de matériel disponible pour l'ID : " + reservation.getMaterialId());
        return false; // Indique qu'il n'y a pas de matériel disponible
      }

      conn.setAutoCommit(false); // Début de la transaction

      String sql = "INSERT INTO reservations (user_id, material_id, reservation_date, reservation_time, return_date, return_time, status, room, material_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
      PreparedStatement preparedStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      preparedStatement.setInt(1, userId);
      preparedStatement.setInt(2, reservation.getMaterialId());
      preparedStatement.setString(3, reservation.getReservationDate());
      preparedStatement.setString(4, reservation.getReservationTime());
      preparedStatement.setString(5, reservation.getReturnDate());
      preparedStatement.setString(6, reservation.getReturnTime());
      preparedStatement.setString(7, reservation.getStatus());
      preparedStatement.setString(8, reservation.getRoom());
      preparedStatement.setString(9, reservation.getMaterialName());

      int rowsAffected = preparedStatement.executeUpdate();

      if (rowsAffected > 0) {
        Logger.log("Réservation insérée avec succès");

        // Mettre à jour la quantité de matériel
        boolean updated = updateMaterialQuantity(conn, reservation.getMaterialId(), -1);
        if (updated) {
          Logger.log("Quantité de matériel mise à jour avec succès");
          conn.commit(); // Valider la transaction
          return true;
        } else {
          Logger.log("Échec de la mise à jour de la quantité de matériel");
          conn.rollback(); // Annuler la transaction en cas d'échec
        }
      } else {
        Logger.log("Échec de l'insertion de la réservation : aucune ligne affectée");
      }
    } catch (SQLException ex) {
      Logger.log("Échec de l'insertion de la réservation : " + ex.getMessage());
      try {
        conn.rollback(); // Annuler la transaction en cas d'exception
      } catch (SQLException e) {
        Logger.log("Échec de l'annulation de la transaction : " + e.getMessage());
      }
    } finally {
      try {
        conn.setAutoCommit(true); // Réactiver l'auto-commit
        conn.close();
      } catch (SQLException ex) {
        Logger.log("Échec de la fermeture de la connexion : " + ex.getMessage());
      }
    }
    return false;
  }

  public static boolean updateMaterialQuantity(Connection conn, int materialId, int quantityChange) {
    try {
      String sql = "UPDATE materials SET quantity = quantity + ? WHERE id = ?";
      PreparedStatement preparedStatement = conn.prepareStatement(sql);
      preparedStatement.setInt(1, quantityChange);
      preparedStatement.setInt(2, materialId);

      Logger.log("Tentative de mise à jour de la quantité de matériel avec les valeurs suivantes :");
      Logger.log("material_id: " + materialId);
      Logger.log("quantity_change: " + quantityChange);

      int affectedRows = preparedStatement.executeUpdate();
      if (affectedRows > 0) {
        Logger.log("Quantité de matériel mise à jour avec succès");
        return true;
      } else {
        Logger.log("Aucun matériel trouvé avec l'ID spécifié");
        return false;
      }
    } catch (SQLException ex) {
      Logger.log("Échec de la mise à jour de la quantité de matériel : " + ex.getMessage());
      return false;
    }
  }

  public static boolean deleteReservation(int reservationId) {
    Connection conn = getConnection();
    if (conn == null) {
      Logger.log("Échec de la connexion à la base de données pour la suppression de la réservation.");
      return false;
    }

    try {
      conn.setAutoCommit(false); // Début de la transaction

      // Récupérer la réservation supprimée
      Reservation deletedReservation = getReservationById(reservationId);
      if (deletedReservation == null) {
        Logger.log("Impossible de trouver la réservation supprimée pour mettre à jour la quantité de matériel.");
        return false;
      }

      String sql = "DELETE FROM reservations WHERE id = ?";
      PreparedStatement preparedStatement = conn.prepareStatement(sql);
      preparedStatement.setInt(1, reservationId);

      Logger.log("Tentative de suppression de la réservation avec l'ID : " + reservationId);

      int affectedRows = preparedStatement.executeUpdate();
      if (affectedRows > 0) {
        Logger.log("Réservation supprimée avec succès");

        // Augmenter la quantité de matériel
        boolean updated = updateMaterialQuantity(conn, deletedReservation.getMaterialId(), 1);
        if (updated) {
          Logger.log("Quantité de matériel augmentée avec succès");
          conn.commit(); // Valider la transaction
          return true;
        } else {
          Logger.log("Échec de l'augmentation de la quantité de matériel");
          conn.rollback(); // Annuler la transaction en cas d'échec
          return false;
        }
      } else {
        Logger.log("Aucune réservation trouvée avec l'ID spécifié");
        return false;
      }
    } catch (SQLException ex) {
      Logger.log("Échec de la suppression de la réservation : " + ex.getMessage());
      try {
        conn.rollback(); // Annuler la transaction en cas d'exception
      } catch (SQLException e) {
        Logger.log("Échec de l'annulation de la transaction : " + e.getMessage());
      }
      return false;
    } finally {
      try {
        conn.setAutoCommit(true); // Réactiver l'auto-commit
        conn.close();
      } catch (SQLException ex) {
        Logger.log("Échec de la fermeture de la connexion : " + ex.getMessage());
      }
    }
  }

  public static boolean isMaterialAvailable(int materialId) {
    Connection conn = getConnection();
    if (conn == null) {
      Logger.log("Échec de la connexion à la base de données pour vérifier la disponibilité du matériel.");
      return false;
    }

    try {
      String sql = "SELECT quantity FROM materials WHERE id = ?";
      PreparedStatement preparedStatement = conn.prepareStatement(sql);
      preparedStatement.setInt(1, materialId);
      ResultSet resultSet = preparedStatement.executeQuery();

      if (resultSet.next()) {
        int quantity = resultSet.getInt("quantity");
        return quantity > 0;
      } else {
        Logger.log("Aucun matériel trouvé avec l'ID spécifié");
        return false;
      }
    } catch (SQLException ex) {
      Logger.log("Échec de la vérification de la disponibilité du matériel : " + ex.getMessage());
      return false;
    } finally {
      try {
        conn.close();
      } catch (SQLException ex) {
        Logger.log("Échec de la fermeture de la connexion : " + ex.getMessage());
      }
    }
  }

  public static boolean deleteExpiredReservations() {
    Connection conn = getConnection();
    if (conn == null) {
      Logger.log("Échec de la connexion à la base de données pour la suppression des réservations expirées.");
      return false;
    }

    try {
      conn.setAutoCommit(false); // Début de la transaction

      LocalDateTime now = LocalDateTime.now();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      String formattedDateTime = now.format(formatter);

      String sql = "DELETE FROM reservations WHERE return_date < ?";
      PreparedStatement preparedStatement = conn.prepareStatement(sql);
      preparedStatement.setString(1, formattedDateTime);

      int rowsAffected = preparedStatement.executeUpdate();

      conn.commit(); // Valider la transaction

      Logger.log(rowsAffected + " réservations expirées ont été supprimées avec succès.");

      return true;
    } catch (SQLException ex) {
      Logger.log("Échec de la suppression des réservations expirées : " + ex.getMessage());
      try {
        conn.rollback(); // Annuler la transaction en cas d'exception
      } catch (SQLException e) {
        Logger.log("Échec de l'annulation de la transaction : " + e.getMessage());
      }
      return false;
    } finally {
      try {
        conn.setAutoCommit(true); // Réactiver l'auto-commit
        conn.close();
      } catch (SQLException ex) {
        Logger.log("Échec de la fermeture de la connexion : " + ex.getMessage());
      }
    }
  }

  public static boolean updateReservation(Reservation reservation, int userId) {
    Connection conn = getConnection();
    if (conn == null) {
      Logger.log("Échec de la connexion à la base de données.");
      return false;
    }

    try {
      conn.setAutoCommit(false); // Début de la transaction

      String sql = "UPDATE reservations SET user_id = ?, reservation_date = ?, reservation_time = ?, return_date = ?, return_time = ?, status = ?, room = ?, material_name = ? WHERE id = ?";
      PreparedStatement preparedStatement = conn.prepareStatement(sql);
      preparedStatement.setInt(1, userId);
      preparedStatement.setString(2, reservation.getReservationDate());
      preparedStatement.setString(3, reservation.getReservationTime());
      preparedStatement.setString(4, reservation.getReturnDate());
      preparedStatement.setString(5, reservation.getReturnTime());
      preparedStatement.setString(6, reservation.getStatus());
      preparedStatement.setString(7, reservation.getRoom());
      preparedStatement.setString(8, reservation.getMaterialName());
      preparedStatement.setInt(9, reservation.getId());

      int rowsAffected = preparedStatement.executeUpdate();

      if (rowsAffected > 0) {
        Logger.log("Réservation mise à jour avec succès");
        conn.commit(); // Valider la transaction
        return true;
      } else {
        Logger.log("Échec de la mise à jour de la réservation : aucune ligne affectée");
      }
    } catch (SQLException ex) {
      Logger.log("Échec de la mise à jour de la réservation : " + ex.getMessage());
      try {
        conn.rollback(); // Annuler la transaction en cas d'exception
      } catch (SQLException e) {
        Logger.log("Échec de l'annulation de la transaction : " + e.getMessage());
      }
    } finally {
      try {
        conn.setAutoCommit(true); // Réactiver l'auto-commit
        conn.close();
      } catch (SQLException ex) {
        Logger.log("Échec de la fermeture de la connexion : " + ex.getMessage());
      }
    }
    return false;
  }

  public static Reservation getReservationById(int reservationId) {
    Connection conn = getConnection();
    if (conn == null) {
      Logger.log("Échec de la connexion à la base de données pour récupérer la réservation par ID.");
      return null;
    }

    try {
      String sql = "SELECT r.id, r.user_id, r.material_id, r.reservation_date, r.reservation_time, " +
          "r.return_date, r.return_time, r.status, r.room, m.name as material_name " +
          "FROM reservations r " +
          "JOIN materials m ON r.material_id = m.id " +
          "WHERE r.id = ?";
      PreparedStatement preparedStatement = conn.prepareStatement(sql);
      preparedStatement.setInt(1, reservationId);
      ResultSet resultSet = preparedStatement.executeQuery();

      if (resultSet.next()) {
        int userId = resultSet.getInt("user_id");
        int materialId = resultSet.getInt("material_id");
        String reservationDate = resultSet.getString("reservation_date");
        String reservationTime = resultSet.getString("reservation_time");
        String returnDate = resultSet.getString("return_date");
        String returnTime = resultSet.getString("return_time");
        String status = resultSet.getString("status");
        String materialName = resultSet.getString("material_name");
        String room = resultSet.getString("room");

        return new Reservation(reservationId, userId, materialId, reservationDate, reservationTime,
            returnDate, returnTime, status, room, materialName);
      } else {
        Logger.log("Aucune réservation trouvée avec l'ID spécifié");
        return null;
      }
    } catch (SQLException ex) {
      Logger.log("Échec de la récupération de la réservation par ID : " + ex.getMessage());
      return null;
    } finally {
      try {
        conn.close();
      } catch (SQLException ex) {
        Logger.log("Échec de la fermeture de la connexion : " + ex.getMessage());
      }
    }
  }
}
