package pl.marczak.opcuaclient;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

/**
 * Project "OpcUaClient"
 * <p>
 * Created by Lukasz Marczak
 * on 05.01.2017.
 */

public class ConnectButton extends Button implements View.OnClickListener {
    View.OnClickListener listener;
    boolean currentState;

    public ConnectButton(Context context) {
        super(context);
        init();
    }

    public ConnectButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ConnectButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ConnectButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    void init() {
        setOnClickListener(this);
    }

    void setBoundView(View v) {
        boundView = v;
        boundView.setVisibility(GONE);
    }

    View boundView;

    @Override
    public void onClick(View view) {
        currentState = !currentState;
        if (currentState) {
            setText("DISCONNECT");
            if (boundView != null) boundView.setVisibility(VISIBLE);

        } else {
            setText("CONNECT");
            if (boundView != null) boundView.setVisibility(GONE);
        }
        if (listener != null) {
            listener.onClick(view);
        }
    }

    public void onClickedAction(View.OnClickListener listener) {
        this.listener = listener;
    }
}
