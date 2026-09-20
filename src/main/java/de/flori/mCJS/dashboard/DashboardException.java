package de.flori.mCJS.dashboard;

public class DashboardException extends RuntimeException {
    private final int status;

    public DashboardException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
