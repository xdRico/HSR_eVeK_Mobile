package de.ehealth.evek.mobile.exception;


import androidx.annotation.NonNull;

import java.io.Serial;

import de.ehealth.evek.api.type.UserRole;

/**
 * Exception class thrown, when the User with his role is not allowed to perform an action
 *
 * @extends IllegalAccessException
 */
public class NoValidUserRoleException extends IllegalAccessException {
    @Serial
    private static final long serialVersionUID = 6495388157592725452L;
    private final UserRole userRole;
    private final String action;

    /**
     * Constructor for NoValidUserRoleException <br>
     * Thrown, when the User with his role is not allowed to perform an action
     *
     * @param userRole - the userRole of the User performing the action
     * @param action - the action tried to be performed, as String
     */
    public NoValidUserRoleException(UserRole userRole, String action) {
        super(String.format("%s is not a valid role for action: %s!", userRole, action));
        this.userRole = userRole;
        this.action = action;
    }

    @NonNull
    @Override
    public String toString(){
        return String.format("%s is not a valid role for action: %s!", userRole, action);
    }
}
