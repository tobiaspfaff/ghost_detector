package com.club.ghost.ghostfinder;

import android.util.Log;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.content.Intent;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import android.bluetooth.BluetoothAdapter;

public class Selector extends AppCompatActivity implements BeaconDiscoverCallback {
    private final static int REQUEST_ENABLE_BT = 1;

    ImageBarAdapter adapter;
    ListView list;
    BeaconEnabledApp app = null;
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selector);

        adapter = new ImageBarAdapter(this);
        list = (ListView) findViewById(R.id.listView);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // Call RangeFinder with selected beacon
                if (i >= 0 && i < adapter.getCount()) {
                    int idx = adapter.getID(i);
                    app.setRangeScanID(idx);

                    // switch to ranging
                    Intent intent = new Intent(getApplicationContext(), RangeFinder.class);
                    intent.putExtra("name", adapter.getName(idx));
                    intent.putExtra("file", adapter.getFile(idx));
                    startActivity(intent);
                }
            }
        });

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            startBluetooth();
        }
    }

    private void startBluetooth() {
        app = (BeaconEnabledApp)getApplication();
        app.setDiscoverCallback(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                startBluetooth();
            } else {
                this.finishAffinity();
            }
        }
    }

    @Override
    public void discoverPing(final int bitmask) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int mask = bitmask;
                ArrayList<Integer> list = new ArrayList<>();
                for (int id=1; mask != 0; id++) {
                    if ((mask & 1) == 1)
                        list.add(id);
                    mask /= 2;
                }
                adapter.rebuild(list);
            }
        });
    }
}
