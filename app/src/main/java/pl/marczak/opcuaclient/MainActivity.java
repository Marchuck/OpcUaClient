package pl.marczak.opcuaclient;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.opcfoundation.ua.builtintypes.DateTime;

import rx.functions.Action1;
import rx.functions.Func1;

public class MainActivity extends AppCompatActivity implements UiConnector {

    public static final String TAG = MainActivity.class.getSimpleName();
    private FloatingActionButton fab;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        injectViews();

        fab.setOnClickListener(getUaListener());
    }
    private void injectViews() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        fab = (FloatingActionButton) findViewById(R.id.fab);
    }

    private View.OnClickListener getUaListener(){
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Connecting to server...", Snackbar.LENGTH_LONG).show();
                showProgressBar();
                OpcUaConnector uaConnector = new OpcUaConnector(getPackageName());
                uaConnector.setUiConnector(MainActivity.this);
                uaConnector
                        .connect()
                        .filter(onlySuccess())
                        .subscribe(new Action1<Boolean>() {
                            @Override
                            public void call(Boolean aBoolean) {
                                Log.i(TAG, "Done");
                                hideProgressBar();
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                showError(new Exception(throwable));
                            }
                        });
            }
        };
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
    }
    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private Func1<? super Boolean, Boolean> onlySuccess() {
        return new Func1<Boolean, Boolean>() {
            @Override
            public Boolean call(Boolean aBoolean) {
                return aBoolean;
            }
        };
    }

    @Override
    public void showError(final Exception ex) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hideProgressBar();
                Toast.makeText(MainActivity.this, ex.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void showResult(final DateTime dt) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "time elapsed: " + dt.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
