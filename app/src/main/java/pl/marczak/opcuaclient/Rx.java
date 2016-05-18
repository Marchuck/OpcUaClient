package pl.marczak.opcuaclient;

import android.util.Log;

import com.prosysopc.ua.ApplicationIdentity;
import com.prosysopc.ua.PkiFileBasedCertificateValidator;
import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.SessionActivationException;
import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.UserIdentity;
import com.prosysopc.ua.client.AddressSpaceException;
import com.prosysopc.ua.client.UaClient;
import com.prosysopc.ua.nodes.UaMethod;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.Variant;
import org.opcfoundation.ua.core.ApplicationDescription;
import org.opcfoundation.ua.core.ApplicationType;
import org.opcfoundation.ua.core.Argument;
import org.opcfoundation.ua.core.CallMethodRequest;
import org.opcfoundation.ua.core.CallMethodResult;
import org.opcfoundation.ua.core.CallResponse;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.ReadResponse;
import org.opcfoundation.ua.core.ReadValueId;
import org.opcfoundation.ua.core.ReferenceDescription;
import org.opcfoundation.ua.core.TimestampsToReturn;
import org.opcfoundation.ua.core.WriteResponse;
import org.opcfoundation.ua.core.WriteValue;
import org.opcfoundation.ua.transport.security.SecurityMode;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * @author Lukasz Marczak
 * @since 16.05.16.
 */
public class Rx {
    public static final String TAG = Rx.class.getSimpleName();
    private UiConnector connector;
    private String name = "Android:Client";

    public Rx(UiConnector connector) {
        this.connector = connector;
    }

    public UaClient uaClient;

    public void connect(String url) {
        connectToServer(url).flatMap(new Func1<UaClient, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(UaClient uaClient) {
                return readDate(uaClient);
            }
        }).flatMap(new Func1<Boolean, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(Boolean aBoolean) {
                return Observable.just(true);
            }
        }).timeout(10, TimeUnit.MINUTES)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Boolean>() {
                    @Override
                    public void onCompleted() {
                        Log.i(TAG, "onCompleted: ");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError: " + e.getMessage());
                        connector.showError(new Exception(e));
                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        Log.d(TAG, "onNext: ");
                    }
                });
    }

    public rx.Observable<UaClient> connectToServer(final String url) {
        return rx.Observable.create(new rx.Observable.OnSubscribe<UaClient>() {
            @Override
            public void call(Subscriber<? super UaClient> subscriber) {
                try {
                    uaClient = new UaClient(url);
                } catch (URISyntaxException syntaxException) {
                    connector.showError(syntaxException);
                    Log.e(TAG, "connectInThread: failed: " + syntaxException.getMessage());
                    subscriber.onError(syntaxException);
                    return;
                }
                // Create and set certificate validator
                PkiFileBasedCertificateValidator validator = new PkiFileBasedCertificateValidator("/sdcard/PKI/CA");
                uaClient.setCertificateValidator(validator);

                // Create application description
                ApplicationDescription appDescription = new ApplicationDescription();
                appDescription.setApplicationName(new LocalizedText(name, Locale.ENGLISH));
                appDescription.setApplicationUri("urn:localhost:UA:" + name);
                appDescription.setProductUri("urn:prosysopc.com:UA:" + name);
                appDescription.setApplicationType(ApplicationType.Client);

                // Create and set application identity
                ApplicationIdentity identity = new ApplicationIdentity();
                identity.setApplicationDescription(appDescription);
                identity.setOrganisation("Prosys");
                uaClient.setApplicationIdentity(identity);

                // Set locale
                uaClient.setLocale(Locale.ENGLISH);

                // Set default timeout to 60 seconds
                uaClient.setTimeout(60000);

                // Set security mode to NONE (others not currently supported on Android)
                uaClient.setSecurityMode(SecurityMode.NONE);

                // Set anonymous user identity
                try {
                    uaClient.setUserIdentity(new UserIdentity());
                    Log.i(TAG, "set user identity: success");
                } catch (SessionActivationException sessionException) {
                    subscriber.onError(sessionException);
                    Log.e(TAG, "connectInThread: failed: " + sessionException.getMessage());
                    return;
                }

                try {
                    uaClient.connect();
                    Log.w(TAG, "connected to " + uaClient.getServerName());
                    Log.w(TAG, "session name: " + uaClient.getSessionName());
                    Log.w(TAG, "host name: " + uaClient.getHost());
                    Log.w(TAG, "session node id null?: " + uaClient.getSession().getSessionId().isNullNodeId());
                    Log.w(TAG, "session id : " + uaClient.getSession().getSessionId().toString());
                    Log.w(TAG, "session id value: " + uaClient.getSession().getSessionId().getValue());
                    //        Log.w(TAG, "getAuthenticationToken: "+uaClient.getSession().getAuthenticationToken().toString() );
                } catch (Exception e) {
                    subscriber.onError(e);
                    Log.e(TAG, "error: " + e.getMessage());
                    return;
                }
                subscriber.onNext(uaClient);
                subscriber.onCompleted();
            }
        });
    }

    public rx.Observable<Boolean> readDate(final UaClient uaClient) {
        return rx.Observable.create(new rx.Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                DataValue dv;
                DateTime time;
                try {
                    dv = uaClient.readValue(Identifiers.Server_ServerStatus_CurrentTime);
                    time = (DateTime) dv.getValue().getValue();
                } catch (Exception e) {
                    Log.e(TAG, "error: " + e.getMessage());
                    time = null;
                    subscriber.onNext(false);
                    connector.showError(e);
                    subscriber.onCompleted();
                    return;
                }
                subscriber.onNext(true);
                connector.showResult("Connected\n" + time.toString());
                subscriber.onCompleted();
            }
        });
    }

    public Observable<Boolean> writeData(final int data) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {

                NodeId node = uaClient.getSession().getSessionId();
                try {

                    List<ReferenceDescription> refdesc = uaClient.getAddressSpace().browseMethods(node);
                    if (refdesc.isEmpty()) Log.e(TAG, "empty refdesc list");
                    for (ReferenceDescription desc : refdesc) {
                        Log.d(TAG, "reference description: " + desc.getDisplayName().getText());
                    }
                    List<UaMethod> methods = uaClient.getAddressSpace().getMethods(node);
                    if (methods.isEmpty()) Log.e(TAG, "empty methods list");
                    for (UaMethod m : methods) {
                        Log.d(TAG, "method: " + m.getDisplayName().getText());
                        for (Argument arg : m.getInputArguments()) {
                            Log.d(TAG, "arg in: " + arg.getName());
                        }
                        for (Argument arg : m.getOutputArguments()) {
                            Log.d(TAG, "arg out: " + arg.getName());
                        }
                    }

                } catch (Exception x) {
                    Log.e(TAG, "exception:" + x.getMessage());
                    x.printStackTrace();
                }
                boolean response = false;
                subscriber.onNext(response);
                subscriber.onCompleted();
            }
        });
    }


    public Observable<CallResponse> callCommand() {
        return Observable.create(new Observable.OnSubscribe<CallResponse>() {
            @Override
            public void call(Subscriber<? super CallResponse> subscriber) {
                Variant[] variants = new Variant[]{
                        new Variant(null)
                };
                NodeId nodeId = new NodeId(85, 85);
                CallMethodRequest re = new CallMethodRequest(new NodeId(85, 85), new NodeId(85, 85), variants);
                CallResponse callResponse = null;
                try {
                    callResponse = uaClient.call(re);
                } catch (ServiceException c) {
                    subscriber.onError(c);
                }
                if (callResponse != null) {
                    for (CallMethodResult result : callResponse.getResults()) {
                        Log.d(TAG, "received results: " + result.toString());
                    }
                }
                subscriber.onNext(callResponse);
                subscriber.onCompleted();
            }
        });
    }

    public Observable<ReadResponse> read() {
        Log.d(TAG, "read: ");
        return Observable.create(new Observable.OnSubscribe<ReadResponse>() {
            @Override
            public void call(Subscriber<? super ReadResponse> subscriber) {
                ReadValueId readValue = new ReadValueId();
                ReadResponse response;
                try {
                    response = uaClient.read((double) (System.currentTimeMillis()), TimestampsToReturn.Server, readValue);
                } catch (ServiceException x) {
                    subscriber.onError(x);
                    return;
                }
                subscriber.onNext(response);
                subscriber.onCompleted();
            }
        });
    }

    public boolean isConnected() {
        return uaClient != null && uaClient.isConnected();
    }

    public void disconnect() {
        disconnectFromServer().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        connector.showResult("Disconnected from server");
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {

                    }
                });
    }

    private rx.Observable<Boolean> disconnectFromServer() {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                uaClient.disconnect();
                subscriber.onNext(true);
                subscriber.onCompleted();
            }
        });
    }
}
