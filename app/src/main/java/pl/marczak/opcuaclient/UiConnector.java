package pl.marczak.opcuaclient;

/**
 * @author Lukasz Marczak
 * @since 11.05.16.
 */
public interface UiConnector {
    void showError(Exception ex);

    void onConnected();

    void showResult(String message);
}

