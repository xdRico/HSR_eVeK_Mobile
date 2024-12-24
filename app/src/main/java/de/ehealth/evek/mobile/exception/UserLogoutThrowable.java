package de.ehealth.evek.mobile.exception;

import androidx.annotation.NonNull;

import java.io.Serial;

public class UserLogoutThrowable extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 6497688564588794688L;

    public UserLogoutThrowable(String message) {
        super(message);
    }
    public UserLogoutThrowable(){
        super();
    }

    @NonNull
    @Override
    public String toString(){
      return "User wurde ausgeloggt!";
    }
}
