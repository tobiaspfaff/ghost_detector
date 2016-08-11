package com.club.ghost.ghostfinder;

import android.app.Application;
import android.os.RemoteException;
import android.util.Log;

import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.service.RunningAverageRssiFilter;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;

interface BeaconDiscoverCallback {
    void discoverPing(final int bitmask);
}

interface BeaconRangeCallback {
    void rangePing(double distance, double rssi);
}

public class BeaconEnabledApp extends Application implements BeaconConsumer {
    private BeaconManager beaconManager;
    private BackgroundPowerSaver backgroundPowerSaver;
    private BeaconRangeCallback rangeCallback = null;
    private BeaconDiscoverCallback discoverCallback = null;
    private int beaconHash = 0;
    private int scanID = 0;
    private static final int majorID = 12;

    public void setDiscoverCallback(BeaconDiscoverCallback cb) {
        discoverCallback = cb;
    }
    public void setRangeCallback(BeaconRangeCallback cb) {
        rangeCallback = cb;
    }
    public void setRangeScanID(int scanID) {
        this.scanID = scanID;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.setForegroundScanPeriod(200);
        beaconManager.setForegroundBetweenScanPeriod(0);
        beaconManager.setRssiFilterImplClass(RunningAverageRssiFilter.class);
        RunningAverageRssiFilter.setSampleExpirationMilliseconds(10000l);
        beaconManager.setDistanceModelUpdateUrl(null);
        beaconManager.bind(this);
        backgroundPowerSaver = new BackgroundPowerSaver(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                // build bitmask with found beacons, and issue power callbacks
                int hash = 0;
                for (Beacon beacon : beacons) {
                    int idMain = beacon.getId2().toInt();
                    int idSec = beacon.getId3().toInt();
                    if (idMain == majorID) {
                        hash += 1 << (idSec - 1);
                        if (rangeCallback != null && scanID == idSec)
                            rangeCallback.rangePing(beacon.getDistance(), beacon.getRssi());
                    }
                }
                if (hash != beaconHash && discoverCallback != null) {
                    discoverCallback.discoverPing(hash);
                }
                beaconHash = hash;
            }
        });
        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {    }
    }
}
