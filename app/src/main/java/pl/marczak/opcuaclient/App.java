package pl.marczak.opcuaclient;

import android.support.multidex.MultiDexApplication;
import android.util.Log;

import org.apache.log4j.BasicConfigurator;

/**
 * @author Lukasz Marczak
 * @since 16.05.16.
 */
public class App extends MultiDexApplication {
    public static final String TAG = App.class.getSimpleName();
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");
        BasicConfigurator.configure();
    }
}
