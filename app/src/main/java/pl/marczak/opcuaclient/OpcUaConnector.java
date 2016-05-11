package pl.marczak.opcuaclient;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.prosysopc.ua.ApplicationIdentity;
import com.prosysopc.ua.PkiFileBasedCertificateValidator;
import com.prosysopc.ua.SessionActivationException;
import com.prosysopc.ua.UserIdentity;
import com.prosysopc.ua.client.UaClient;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.core.ApplicationDescription;
import org.opcfoundation.ua.core.ApplicationType;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.transport.security.SecurityMode;

import java.net.URISyntaxException;
import java.util.Locale;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * @author Lukasz Marczak
 * @since 11.05.16.
 */
public class OpcUaConnector {
    private String name;

    @Nullable
    private UiConnector uiConnector;

    public void setUiConnector(@Nullable UiConnector connector) {
        this.uiConnector = connector;
    }

    public OpcUaConnector(String name) {
        this.name = name;
    }

    public Observable<Boolean> connect() {
        return connect("opc.tcp://10.0.2.2:52520/OPCUA/SampleConsoleServer");
    }

    public Observable<Boolean> connect(String serverUri) {
        return createClient(serverUri, name).flatMap(new Func1<UaClient, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(@NonNull UaClient uaClient) {
                try {
                    uaClient.connect();
                } catch (Exception e) {
                    if (uiConnector != null) uiConnector.showError(e);
                    return Observable.just(false);
                }

                DataValue dv;
                DateTime time;
                try {
                    dv = uaClient.readValue(Identifiers.Server_ServerStatus_CurrentTime);
                    time = (DateTime) dv.getValue().getValue();
                } catch (Exception e) {
                    if (uiConnector != null) uiConnector.showError(e);
                    return Observable.just(false);
                }
                if (uiConnector != null) uiConnector.showResult(time);

                uaClient.disconnect();

                return Observable.just(true);
            }
        });
    }


    public static Observable<UaClient> createClient(final String serverUri, final String applicationName) {
        return Observable.create(new Observable.OnSubscribe<UaClient>() {
            @Override
            public void call(Subscriber<? super UaClient> subscriber) {
                // Create the UaClient
                UaClient myClient;
                try {
                    myClient = new UaClient(serverUri);
                } catch (URISyntaxException syntaxException) {
                    subscriber.onError(syntaxException);
                    return;
                }
                // Create and set certificate validator
                PkiFileBasedCertificateValidator validator = new PkiFileBasedCertificateValidator("/sdcard/PKI/CA");
                myClient.setCertificateValidator(validator);

                // Create application description
                ApplicationDescription appDescription = new ApplicationDescription();
                appDescription.setApplicationName(new LocalizedText(applicationName, Locale.ENGLISH));
                appDescription.setApplicationUri("urn:localhost:UA:" + applicationName);
                appDescription.setProductUri("urn:prosysopc.com:UA:" + applicationName);
                appDescription.setApplicationType(ApplicationType.Client);

                // Create and set application identity
                ApplicationIdentity identity = new ApplicationIdentity();
                identity.setApplicationDescription(appDescription);
                identity.setOrganisation("Prosys");
                myClient.setApplicationIdentity(identity);

                // Set locale
                myClient.setLocale(Locale.ENGLISH);

                // Set default timeout to 60 seconds
                myClient.setTimeout(60000);

                // Set security mode to NONE (others not currently supported on Android)
                myClient.setSecurityMode(SecurityMode.NONE);

                // Set anonymous user identity
                try {
                    myClient.setUserIdentity(new UserIdentity());
                } catch (SessionActivationException sessionException) {
                    subscriber.onError(sessionException);
                    return;
                }
                subscriber.onNext(myClient);
                subscriber.onCompleted();
            }
        });
    }
}
