package de.ehealth.evek.mobile.network;

import de.ehealth.evek.mobile.exception.UserLoggedInThrowable;
import de.ehealth.evek.mobile.exception.UserLogoutThrowable;

/**
 * Class used for Listeners listening for changes at Login <br>
 * Must be added to DataHandler as Listener!
 */
public interface IsLoggedInListener {

    /**
     * Called when Login state changes
     *
     * @param isLoggedIn - throwable representing the login State (generally {@link UserLoggedInThrowable} or {@link UserLogoutThrowable})
     */
    void onLoginStateChanged(Throwable isLoggedIn);
}
