package com.imob.syncpaste;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import java.io.File;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.Nullable;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    private ServerSocket serverSocket;

    private ExecutorService executorService = Executors.newCachedThreadPool();

    private File file;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        findViewById(R.id.create_server).setOnClickListener(this);
        findViewById(R.id.monitor_incoming).setOnClickListener(this);
        findViewById(R.id.stop_server).setOnClickListener(this);
        findViewById(R.id.connect_to_server).setOnClickListener(this);
        findViewById(R.id.log_server_info).setOnClickListener(this);
        findViewById(R.id.write_to_server).setOnClickListener(this);
        findViewById(R.id.write_to_clients).setOnClickListener(this);
        findViewById(R.id.write_to_clients_file).setOnClickListener(this);
        findViewById(R.id.write_to_server_file).setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {

    }
}
