package pl.marczak.opcuaclient;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements UiConnector {

    public static final String TAG = MainActivity.class.getSimpleName();
    public static final String name = MainActivity.class.getPackage().getName();
    Button connectButton, readButton, writeButton;
    ProgressBar progressBar;
    TextInputLayout serverLayout;
    TextInputEditText serverEdittext;
    TextView status, content;
    Rx rx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        injectViews();
        rx = new Rx(this);
        connectButton.setOnClickListener(getUaListener());
        readButton.setOnClickListener(new ReadListener(rx));
        writeButton.setOnClickListener(new WriteListener(rx));
    }

    private void injectViews() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        status = (TextView) findViewById(R.id.status);
        content = (TextView) findViewById(R.id.content);
        connectButton = (Button) findViewById(R.id.fab);
        readButton = (Button) findViewById(R.id.fab1);
        writeButton = (Button) findViewById(R.id.fab3);
        readButton.setVisibility(View.GONE);
        writeButton.setVisibility(View.GONE);

        serverLayout = (TextInputLayout) findViewById(R.id.input_layout);
        serverEdittext = (TextInputEditText) findViewById(R.id.input_server_name);
        if (serverEdittext == null) throw new NullPointerException("Nullable view");
        serverEdittext.setText("opc.tcp://192.168.0.15:9099/Matlab");
    }

    View.OnClickListener getUaListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showProgressBar();
                final String server = serverEdittext.getText().toString();
                if (!rx.isConnected()) {
                    Snackbar.make(view, "Connecting to server...", Snackbar.LENGTH_LONG).show();
                    rx.connect(server);
                } else {
                    rx.disconnect();
                }
            }
        };
    }

    @Override
    public void hideProgressBar() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void showProgressBar() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
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

    @Override
    public void showResult(final String dt) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hideProgressBar();
                status.setTextColor(Color.BLUE);
                content.setText(dt);
                readButton.setVisibility(View.VISIBLE);
                writeButton.setVisibility(View.VISIBLE);
                if (rx.isConnected()) connectButton.setText("disconnect");
                else connectButton.setText("connect");
            }
        });
    }

    @Override
    public void onConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                status.setText("Connected");
                connectButton.setText("disconnect");
                readButton.setVisibility(View.VISIBLE);
                writeButton.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                status.setText("Disconnected");
                connectButton.setText("connect");
                readButton.setVisibility(View.GONE);
                writeButton.setVisibility(View.GONE);
            }
        });
    }
}
