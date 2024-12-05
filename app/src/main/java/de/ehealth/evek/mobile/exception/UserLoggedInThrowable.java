package de.ehealth.evek.mobile.exception;

import androidx.annotation.NonNull;

import java.io.Serial;

import de.ehealth.evek.api.entity.User;

public class UserLoggedInThrowable extends Throwable {
    @Serial
    private static final long serialVersionUID = 6497388564582682356L;
    private final User loginUser;

    public UserLoggedInThrowable(User loginUser){
        this.loginUser = loginUser;
    }

    public User getUser(){
        return loginUser;
    }
    @NonNull
    @Override
    public String toString(){
        return String.format("User %s has been successfully logged in!", loginUser.id().value());
    }

}
