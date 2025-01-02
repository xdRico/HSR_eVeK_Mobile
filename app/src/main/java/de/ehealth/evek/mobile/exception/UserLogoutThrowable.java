package de.ehealth.evek.mobile.exception;

import androidx.annotation.NonNull;

import java.io.Serial;

/**
 * Class thrown, when a User has been logged out
 */
public class UserLogoutThrowable extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 6497688564588794688L;

    /**
     * Constructor of UserLogoutThrowable <br>
     * Thrown, when a User has been logged out
     *
     * @param message - a message or the reason of the logout
     */
    public UserLogoutThrowable(String message) {
        super(message);
    }

    /**
     * Constructor of UserLogoutThrowable <br>
     * Thrown, when a User has been logged out
     */
    public UserLogoutThrowable(){
        super();
    }

    @NonNull
    @Override
    public String toString(){
      return "User wurde ausgeloggt!";
    }
}
