package com.example.gps_tracks;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.MapView;

import java.util.ArrayList;
import java.util.List;

import io.ticofab.androidgpxparser.parser.GPXParser;

public class GPSTrack {
    public enum State {READY, RECORDING, EDITING, EMPTY}
    private Context context;
    private GpsMyLocationProvider gpsMyLocationProvider;
    private State state = State.EMPTY;


    GPSTrack(Context context) {
        this.context= context;
    }
    boolean startRecording(MapView map) {
        Log.i("GPSTrack","Recording has been started.");
        state = State.RECORDING;
        int i = 0;
        Polyline path = new Polyline();
        map.getOverlayManager().add(path);

        gpsMyLocationProvider = new GpsMyLocationProvider(context);
        gpsMyLocationProvider.setLocationUpdateMinDistance(5.0f);
        gpsMyLocationProvider.startLocationProvider(new IMyLocationConsumer() {
            @Override
            public void onLocationChanged(Location location, IMyLocationProvider source) {
                String position = "Location changed to: " + new GeoPoint(location).toDoubleString();
                Log.i("onLocationChanged: ", position);
                path.addPoint(new GeoPoint(location));
                //save current path on disk
                if (i%10 == 0) {
                    List<GeoPoint> points = path.getActualPoints();
                    //TODO save List on disk as GPX
                }
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
