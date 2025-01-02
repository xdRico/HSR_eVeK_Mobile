package de.ehealth.evek.mobile.exception;

import androidx.annotation.NonNull;

import java.io.Serial;

import de.ehealth.evek.api.entity.User;

/**
 * Class thrown, when a user has been successfully logged in
 */
public class UserLoggedInThrowable extends Throwable {
    @Serial
    private static final long serialVersionUID = 6497388564582682356L;
    private final User loginUser;

    /**
     * Constructor of UserLoggedInThrowable <br>
     * Thrown, when a user has been successfully logged in
     *
     * @param loginUser - the user that has been logged in successfully
     */
    public UserLoggedInThrowable(User loginUser){
        this.loginUser = loginUser;
    }

    /**
     * Method to get the user that has been logged in successfully
     *
     * @return User - the user that was logged in
     */
    public User getUser(){
        return loginUser;
    }

    @NonNull
    @Override
    public String toString(){
        return String.format("User %s has been successfully logged in!", loginUser.id().value());
    }

}
