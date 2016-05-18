package pl.marczak.opcuaclient;

import android.util.Log;
import android.view.View;

import org.opcfoundation.ua.core.CallResponse;

import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * @author Lukasz Marczak
 * @since 17.05.16.
 */
public class WriteListener implements View.OnClickListener {
    public static final String TAG = WriteListener.class.getSimpleName();
    Rx rx;

    public WriteListener(Rx x) {
        rx = x;
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick: ");
        rx.writeData(12).subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<Boolean>() {
                    @Override
                    public void onCompleted() {
                        Log.i(TAG, "onCompleted writing: ");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError: " + e.getMessage());
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        Log.d(TAG, "onNext: " + aBoolean);
                    }
                });
        /*rx.callCommand().subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<CallResponse>() {
                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "onCompleted: ");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError: " + e.getMessage());
                        e.getMessage();
                    }

                    @Override
                    public void onNext(CallResponse callResponse) {
                        Log.d(TAG, "onNext: " + callResponse);
                    }
                });*/
    }
}
