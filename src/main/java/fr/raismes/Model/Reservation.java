package fr.raismes.Model;

public class Reservation {
    private int id;
    private int userId;
    private int materialId;
    private String reservationDate;
    private String reservationTime;
    private String returnDate;
    private String returnTime;
    private String status;
    private String room;
    private String materialName; // Champ pour le nom du matériel

    public Reservation(int id, int userId, int materialId, String reservationDate, String reservationTime,
            String returnDate, String returnTime, String status, String room, String materialName) {
        this.id = id;
        this.userId = userId;
        this.materialId = materialId;
        this.reservationDate = reservationDate;
        this.reservationTime = reservationTime;
        this.returnDate = returnDate;
        this.returnTime = returnTime;
        this.status = status;
        this.room = room;
        this.materialName = materialName;
    }

    // Getters and setters for all fields including materialName

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getMaterialId() {
        return materialId;
    }

    public void setMaterialId(int materialId) {
        this.materialId = materialId;
    }

    public String getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(String reservationDate) {
        this.reservationDate = reservationDate;
    }

    public String getReservationTime() {
        return reservationTime;
    }

    public void setReservationTime(String reservationTime) {
        this.reservationTime = reservationTime;
    }

    public String getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(String returnDate) {
        this.returnDate = returnDate;
    }

    public String getReturnTime() {
        return returnTime;
    }

    public void setReturnTime(String returnTime) {
        this.returnTime = returnTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    // Ajouter les méthodes getEndDate() et getEndTime()

    public String getEndDate() {
        return returnDate; // Renvoie la date de retour comme date de fin
    }

    public String getEndTime() {
        return returnTime; // Renvoie l'heure de retour comme heure de fin
    }

    public int getReservationId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getReservationId'");
    }
}
