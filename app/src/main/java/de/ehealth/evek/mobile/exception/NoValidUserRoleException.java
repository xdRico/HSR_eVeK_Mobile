package de.ehealth.evek.mobile.exception;


import androidx.annotation.NonNull;

import java.io.Serial;

import de.ehealth.evek.api.type.UserRole;

public class NoValidUserRoleException extends IllegalAccessError {
    @Serial
    private static final long serialVersionUID = 6495388157592725452L;
    private final UserRole userRole;
    private final String action;
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
