package fr.raismes;

public class Auth {
    private static int currentUserId = -1;

    public static void login(int userId) {
        currentUserId = userId;
    }

    public static void logout() {
        currentUserId = -1;
    }

    public static int getCurrentUserId() {
        return currentUserId;
    }
}
