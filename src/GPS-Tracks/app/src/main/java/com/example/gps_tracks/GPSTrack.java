package com.example.gps_tracks;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;

import io.ticofab.androidgpxparser.parser.GPXParser;

public class GPSTrack {
    public enum State {READY, RECORDING, EDITING, EMPTY}
    Context context;
    private GpsMyLocationProvider gpsMyLocationProvider;
    private State state = State.EMPTY;


    GPSTrack(Context context) {
        this.context= context;
    }
    boolean startRecording() {
        Log.i("GPSTrack","Recording has been started.");
        state = State.RECORDING;
        gpsMyLocationProvider = new GpsMyLocationProvider(context);
        gpsMyLocationProvider.setLocationUpdateMinDistance(5.0f);
        gpsMyLocationProvider.startLocationProvider(new IMyLocationConsumer() {
            @Override
            public void onLocationChanged(Location location, IMyLocationProvider source) {
                String position = "Location changed to: " + new GeoPoint(location).toDoubleString();
                Log.i("onLocationChanged: ", position);
            }
        });
        return true;
    }
    boolean endRecording() {
        state = State.READY;
        Log.i("GPSTrack","Recording has been stopped.");
        gpsMyLocationProvider.destroy();
        return true;
    }
    State getState() {
        return state;
    }
    void setState(State state) {
        this.state = state;
    }
}
