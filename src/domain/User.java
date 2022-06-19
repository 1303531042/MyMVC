package domain;

public class User {
    private String uName;
    private int uID;
    private boolean isUsed;
    private Atm atm;

    @Override
    public String toString() {
        return "User{" +
                "uName='" + uName + '\'' +
                ", uID=" + uID +
                ", isUsed=" + isUsed +
                ", atm=" + atm +
                '}';
    }

    public User() {
    }
}
