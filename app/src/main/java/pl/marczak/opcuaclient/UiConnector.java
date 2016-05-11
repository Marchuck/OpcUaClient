package pl.marczak.opcuaclient;

import org.opcfoundation.ua.builtintypes.DateTime;

/**
 * @author Lukasz Marczak
 * @since 11.05.16.
 */
public interface UiConnector {
    void showError(Exception ex);

    void showResult(DateTime dt);
}

