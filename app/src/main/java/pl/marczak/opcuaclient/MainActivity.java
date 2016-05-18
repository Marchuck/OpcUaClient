package pl.marczak.opcuaclient;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.opcfoundation.ua.builtintypes.DateTime;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    public static final String name = MainActivity.class.getPackage().getName();
    FloatingActionButton fab, fab1, fab3;
    ProgressBar progressBar;
    TextInputLayout serverLayout;
    TextInputEditText serverEdittext;
    TextView status;
    Rx rx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        injectViews();
        prepareFab(Color.BLACK, fab3);
        prepareFab(Color.BLUE, fab1);
        fab.setOnClickListener(getUaListener());
        fab1.setOnClickListener(new ReadListener(rx));
        fab3.setOnClickListener(new WriteListener(rx));
    }

    private void prepareFab(int Color, FloatingActionButton fab3) {
        fab3.setRippleColor(Color);
        fab3.setBackgroundColor(Color);
        fab3.setBackgroundTintList(ColorStateList.valueOf(Color));
    }

    private void injectViews() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        //int colorAccent = getResources().getColor(R.color.colorAccent);

//        if (Build.VERSION.SDK_INT >= 21)
//            progressBar.setProgressTintList(ColorStateList.valueOf(colorAccent));
//        else
//            progressBar.getProgressDrawable().setColorFilter(colorAccent, android.graphics.PorterDuff.Mode.SRC_IN);

        status = (TextView) findViewById(R.id.status);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab1 = (FloatingActionButton) findViewById(R.id.fab1);
        fab3 = (FloatingActionButton) findViewById(R.id.fab3);

        fab1.setVisibility(View.GONE);
        fab3.setVisibility(View.GONE);

        serverLayout = (TextInputLayout) findViewById(R.id.input_layout);
        serverEdittext = (TextInputEditText) findViewById(R.id.input_server_name);
        if (serverEdittext == null) throw new NullPointerException("Nullable view");
        serverEdittext.setText("opc.tcp://192.168.0.15:9099/Matlab");
        rx = new Rx(new UiConnector() {
            @Override
            public void showError(Exception ex) {
                MainActivity.this.showError(ex);
            }

            @Override
            public void onConnected() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        status.setAlpha(0f);
                        status.setText("Connected");
                        status.animate().alpha(1f).setDuration(300).start();
                        fab1.setVisibility(View.VISIBLE);
                        fab3.setVisibility(View.VISIBLE);

                        fab1.setAlpha(0f);
                        fab3.setAlpha(0f);

                        fab1.animate().alpha(1f).setDuration(300).start();
                        fab3.animate().alpha(1f).setDuration(300).start();
                    }
                });
            }

            @Override
            public void showResult(DateTime dt) {
                MainActivity.this.showResult(dt);
            }
        });
    }

    private View.OnClickListener getUaListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Connecting to server...", Snackbar.LENGTH_LONG).show();
                showProgressBar();
                final String server = serverEdittext.getText().toString();
                rx.connect(server);
            }
        };
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
    }

    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }

    public void showError(final Exception ex) {
        Log.e(TAG, "showError: " + ex.getMessage());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hideProgressBar();
                status.setTextColor(Color.RED);
                status.setText(ex.toString());
            }
        });
    }

    public void showResult(final DateTime dt) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hideProgressBar();
                status.setTextColor(Color.BLUE);
                status.setText("Connected " + dt);
                fab1.setVisibility(View.VISIBLE);
                fab3.setVisibility(View.VISIBLE);

            }
        });
    }
}
