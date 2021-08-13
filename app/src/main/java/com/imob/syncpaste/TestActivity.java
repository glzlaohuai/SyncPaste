package com.imob.syncpaste;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import androidx.appcompat.app.AppCompatActivity;

public class TestActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "TestActivity";


    public static final String SERVICE_TYPE = "_syncpaste._tcp";

    private boolean isConnected = false;
    private boolean isConnecting = false;

    private NsdManager nsdManager;

    private boolean registered;
    private boolean isRegisting;

    private boolean isUnregisting;

    private Set<Socket> sockets = new HashSet<>();

    private ServerSocket serverSocket;
    private int port;

    private ListView listView;

    private List<NsdServiceInfo> foundServices = new LinkedList<>();

    private boolean isDiscovering;
    private boolean isHandlingDiscovery;

    private boolean isHandlingStopDiscovery;

    private ArrayAdapter<NsdServiceInfo> adapter;


    private NsdManager.DiscoveryListener listener = new NsdManager.DiscoveryListener() {
        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            isHandlingDiscovery = false;
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            isHandlingStopDiscovery = false;
            Log.i(TAG, "onStopDiscoveryFailed: ");
        }

        @Override
        public void onDiscoveryStarted(String serviceType) {
            isDiscovering = true;
            isHandlingDiscovery = false;
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            isDiscovering = false;
            isHandlingStopDiscovery = false;
            Log.i(TAG, "onDiscoveryStopped: ");

            foundServices.clear();
            notifyDataSetChanged();
        }

        @Override
        public void onServiceFound(NsdServiceInfo serviceInfo) {
            Log.i(TAG, "onServiceFound: " + serviceInfo.getHost() + ", " + serviceInfo.getPort());
            addService(serviceInfo);
            notifyDataSetChanged();
        }

        @Override
        public void onServiceLost(NsdServiceInfo serviceInfo) {
            Log.i(TAG, "onServiceLost: " + serviceInfo);
            removeService(serviceInfo);
            notifyDataSetChanged();
        }
    };

    private NsdServiceInfo isServiceExistsInSet(String name) {
        if (name == null) return null;
        for (NsdServiceInfo serviceInfo : foundServices) {
            if (serviceInfo.getServiceName().equals(name)) {
                return serviceInfo;
            }
        }
        return null;
    }


    private void addService(NsdServiceInfo nsdServiceInfo) {
        if (nsdServiceInfo != null) {
            if (isServiceExistsInSet(nsdServiceInfo.getServiceName()) == null) {
                foundServices.add(nsdServiceInfo);
            }
        }
    }

    private void removeService(NsdServiceInfo nsdServiceInfo) {
        if (nsdServiceInfo != null) {
            if ((nsdServiceInfo = isServiceExistsInSet(nsdServiceInfo.getServiceName())) != null) {
                foundServices.remove(nsdServiceInfo);
            }
        }
    }

    private NsdManager.RegistrationListener registrationListener = new NsdManager.RegistrationListener() {
        @Override
        public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            isRegisting = false;
            registered = false;
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            isUnregisting = false;
        }

        @Override
        public void onServiceRegistered(NsdServiceInfo serviceInfo) {
            registered = true;
            isRegisting = false;
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
            registered = false;
            isUnregisting = false;
        }
    };


    private void notifyDataSetChanged() {
        Log.i(TAG, "notifyDataSetChanged: " + foundServices.size());

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.clear();
                adapter.addAll(foundServices);
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);

        findViewById(R.id.register).setOnClickListener(this);
        findViewById(R.id.discover).setOnClickListener(this);
        findViewById(R.id.unregister).setOnClickListener(this);
        findViewById(R.id.stopDiscover).setOnClickListener(this);

        listView = findViewById(R.id.list);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i(TAG, "onItemClick: " + position);
                connectTo(foundServices.get(position));
            }
        });
    }


    private void connectTo(NsdServiceInfo serviceInfo) {
        if (serviceInfo == null || isConnected || isConnecting) return;
        isConnecting = true;

        if (serviceInfo.getPort() == 0 || serviceInfo.getHost() == null) {
            //need to resolve service info
            getDetailedInfoThenConnect(serviceInfo);
        } else {
            doRealConnect(serviceInfo);
        }
    }


    private void doRealConnect(NsdServiceInfo serviceInfo) {
        try {
            Socket socket = new Socket(serviceInfo.getHost(), serviceInfo.getPort());
            isConnected = true;
            Log.i(TAG, "doRealConnect: success, " + serviceInfo.getServiceName());
        } catch (IOException e) {
            e.printStackTrace();
        }

        isConnecting = false;
    }


    private void getDetailedInfoThenConnect(NsdServiceInfo serviceInfo) {
        if (serviceInfo != null) {
            nsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {
                @Override
                public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                    isConnecting = false;
                }

                @Override
                public void onServiceResolved(NsdServiceInfo serviceInfo) {
                    Log.i(TAG, "onServiceResolved: " + serviceInfo.toString());
                    doRealConnect(serviceInfo);
                }
            });
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.register:
                doRegister();
                break;
            case R.id.discover:
                doDiscovery();
                break;

            case R.id.unregister:
                doUnregister();
                break;
            case R.id.stopDiscover:
                stopDiscovery();
                break;
        }
    }

    private void stopDiscovery() {
        if (isHandlingDiscovery || isHandlingStopDiscovery || !isDiscovering) return;
        Log.i(TAG, "stopDiscovery: ");
        isHandlingStopDiscovery = true;
        nsdManager.stopServiceDiscovery(listener);
    }

    private void doUnregister() {
        if (isUnregisting || !registered || isRegisting) return;

        isUnregisting = true;
        nsdManager.unregisterService(registrationListener);
    }


    private void initServerSocket() throws IOException {
        serverSocket = new ServerSocket(0);
        int localPort = serverSocket.getLocalPort();
        this.port = localPort;
    }


    private void startListenAcceptedSockets() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (serverSocket != null) {
                        try {
                            Socket socket = serverSocket.accept();
                            handleSocket(socket);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }


    private void handleSocket(Socket socket) {
        sockets.add(socket);
        Log.i(TAG, "handleSocket: got a new connected socket: " + socket);
    }

    private void doRegister() {
        if (!registered && !isRegisting && !isUnregisting) {

            isRegisting = true;
            try {
                initServerSocket();
                startListenAcceptedSockets();
            } catch (IOException e) {
                e.printStackTrace();
                isRegisting = false;
                return;
            }

            NsdServiceInfo info = new NsdServiceInfo();
            info.setServiceType(SERVICE_TYPE);
            info.setServiceName("SyncPaste - " + Build.BRAND + " # " + UUID.randomUUID().hashCode());
            info.setPort(port);
            Log.i(TAG, "doRegister: " + info);

            nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, registrationListener);
        }
    }

    private void doDiscovery() {

        if (isDiscovering || isHandlingDiscovery) return;
        isHandlingDiscovery = true;
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener);

    }
}