package pl.marczak.opcuaclient;

import android.util.Log;

import com.prosysopc.ua.ApplicationIdentity;
import com.prosysopc.ua.PkiFileBasedCertificateValidator;
import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.SessionActivationException;
import com.prosysopc.ua.UserIdentity;
import com.prosysopc.ua.client.UaClient;
import com.prosysopc.ua.nodes.UaMethod;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
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

    long time0;

    public void connect(String url) {

        time0 = System.currentTimeMillis();
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
//                    subscriber.onError(syntaxException);
//                    return;
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
                    connector.showResult("Connected to " + uaClient.getServerName());
                    Log.w(TAG, "connected to " + uaClient.getServerName());
                    Log.w(TAG, "time elapsed: " + (System.currentTimeMillis() - time0) + " ms");
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

    static NodeId init(int var0) {
        return new NodeId(0, UnsignedInteger.getFromBits(var0));
    }

    public Observable<Boolean> writeData(final int data) {
        Log.w(TAG, "read available nodes");
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {

                StringBuilder stringBuilder = new StringBuilder();
                long timezero = System.currentTimeMillis();
                int k = 0;
                connector.showProgressBar();
                NodeId[] nodes = new NodeId[12000];
                for (int hk = 0; hk < 12000; hk++) {
                    nodes[hk] = init(hk);
                }
                for (int j = 0; j < 12000; j++) {
                    Log.w(TAG, "current index" + j);
                    NodeId node = nodes[j];
                    try {
                        DataValue dataValue = uaClient.readValue(node);
                        String displayableValue = dataValue.getValue().toString();
                        Log.e(TAG, "next value for node " + j + " : " + displayableValue);
                        stringBuilder.append("nodeId[");
                        stringBuilder.append(j);
                        stringBuilder.append("] = ");
                        stringBuilder.append(displayableValue).append('\n');
                        List<ReferenceDescription> refdesc = uaClient.getAddressSpace().browseMethods(node);
                        List<UaMethod> methods = uaClient.getAddressSpace().getMethods(node);
                        for (ReferenceDescription desc : refdesc) {
                            Log.e(TAG, "reference description: " + desc.getDisplayName().getText());
                        }
                        for (UaMethod m : methods) {
                            Log.e(TAG, "method: " + m.getDisplayName().getText());
                            for (Argument arg : m.getInputArguments()) {
                                Log.e(TAG, "arg in: " + arg.getName());
                            }
                            for (Argument arg : m.getOutputArguments()) {
                                Log.e(TAG, "arg out: " + arg.getName());
                            }
                        }
                    } catch (Exception x) {
                        // Log.e(TAG, "oops! node error: " + x.getMessage());
                        //x.printStackTrace();
                    }
                    if (j % 120 == 0) {
                        ++k;
                        connector.showResult("progress = " + String.valueOf(k) + " %");
                        Log.w(TAG, "now is: " + k + " %");
                    }
                }
                Log.d(TAG, "checking nodes completed in: " + (System.currentTimeMillis() - timezero) + " ms");
                connector.hideProgressBar();
                connector.showResult(stringBuilder.toString());
                subscriber.onNext(false);
                subscriber.onCompleted();
            }
        });
    }

    public static NodeId getNode() {
        return new NodeId(12162, 0);
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
                        connector.onDisconnected();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e(TAG, "Error occurred" + throwable.getMessage());
                        throwable.printStackTrace();
                        connector.showError(new Exception(throwable));
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

    public UiConnector getConnector() {
        return connector;
    }


    public void yetAnotherCalls() {

    }
}
