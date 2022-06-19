package domain;

import java.security.PrivateKey;

public class Atm {
    private float balance;
    private String password;

    public Atm() {
    }

    @Override
    public String toString() {
        return "Atm{" +
                "balance=" + balance +
                ", password='" + password + '\'' +
                '}';
    }
}
