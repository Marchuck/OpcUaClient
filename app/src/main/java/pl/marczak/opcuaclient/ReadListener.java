package pl.marczak.opcuaclient;

import android.util.Log;
import android.view.View;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.core.ReadResponse;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * @author Lukasz Marczak
 * @since 17.05.16.
 */
public class ReadListener implements View.OnClickListener {
    public static final String TAG = ReadListener.class.getSimpleName();
    Rx rx;

    public ReadListener(Rx x) {
        rx = x;
    }

    @Override
    public void onClick(View v) {
        Log.i(TAG, "onClick: ");
        if (rx != null) rx.read().map(new Func1<ReadResponse, String>() {
            @Override
            public String call(ReadResponse readResponse) {
                Log.d(TAG, "xmlEncodeId: " + readResponse.getXmlEncodeId().toString());
                Log.d(TAG, "binaryEncodeId: " + readResponse.getBinaryEncodeId().toString());
                Log.d(TAG, "responseHeader: " + readResponse.getResponseHeader().toString());
                Log.d(TAG, "typeId: " + readResponse.getTypeId().toString());

                DataValue re = readResponse.getResults()[0];
                for (DataValue dv : readResponse.getResults()) {
                    Log.e(TAG, "next value: " + dv.getValue().toString());
                }
                return re.getValue().toString();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "onCompleted: ");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError: " + e.getMessage());
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(String readResponse) {
                        Log.i(TAG, "onNext: " + readResponse);
                    }
                });
    }
}
