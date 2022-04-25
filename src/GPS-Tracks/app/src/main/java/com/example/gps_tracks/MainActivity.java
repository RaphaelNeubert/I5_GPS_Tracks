package com.example.gps_tracks;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity{
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map = null;
    private MyLocationNewOverlay locationOverlay;
    private ImageButton toPosButton, recstart;
    boolean press = false;
    private GPSTrack gpsTrack;
    ActivityResultLauncher<Intent> listingTracksResultLauncher;

    @Override
        public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //handle permissions
        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });

        //will hide the title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // hide the title bar
        getSupportActionBar().hide();

        //load/initialize the osmdroid configuration
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        //inflate and create the map
        setContentView(R.layout.activity_main);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);

        IMapController mapController = map.getController();
        mapController.setZoom(15.5);
        mapController.setCenter(new GeoPoint(51.0374,13.7638));

        locationOverlay = new MyLocationNewOverlay(map);
        locationOverlay.enableFollowLocation();
        map.getOverlays().add(locationOverlay);

        toPosButton = (ImageButton) findViewById(R.id.posButton);
        toPosButton.setVisibility(View.INVISIBLE);
        toPosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                locationOverlay.enableFollowLocation();
                toPosButton.setVisibility(View.INVISIBLE);
            }
        });

        MapListener mapListener = new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                if (locationOverlay.isFollowLocationEnabled() == false && map.isAnimating() == false) {
                    toPosButton.setVisibility(View.VISIBLE);
                }
                return false;
            }
            @Override
            public boolean onZoom(ZoomEvent event) {
                return false;
            }
        };
        map.addMapListener(mapListener);
        Marker m = new Marker(map);
        m.setPosition(new GeoPoint(51.051899,13.768021));
        m.setIcon(getResources().getDrawable(R.drawable.ic_location_map_marker));
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        map.getOverlays().add(m);

        // implements communication to ListingTracks activity
        listingTracksResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            String option = data.getStringExtra("optionClicked");

                            switch(option) {
                                case "Track auf Karte anzeigen":
                                    gpsTrack = new GPSTrack(getApplicationContext());
                                    gpsTrack.loadGPX(data.getStringExtra("fileName"));
                                    gpsTrack.display(map);
                                    break;
                            }
                        }
                    }
                });


    }

    @Override
    public void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            permissionsToRequest.add(permissions[i]);
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    public void openListTracks(View view) {
        Intent intent = new Intent(MainActivity.this, ListingTracks.class);
        listingTracksResultLauncher.launch(intent);
    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        Log.i("test","itWorks");
//    }

    public void startRecording(View view) {
        if (gpsTrack == null || gpsTrack.getState() == GPSTrack.State.EMPTY) {
            gpsTrack = new GPSTrack(getApplicationContext());
            gpsTrack.startRecording();
            gpsTrack.setState(GPSTrack.State.RECORDING);
            gpsTrack.display(map);
            //change icon
            ImageButton recButton = (ImageButton) findViewById(R.id.record);
            recButton.setImageResource(R.drawable.button_rec);

        }
        else {
            gpsTrack.endRecording();
            gpsTrack.setState(GPSTrack.State.READY);
            gpsTrack.hide(map);
            //change back icon
            ImageButton recButton = (ImageButton) findViewById(R.id.record);
            recButton.setImageResource(R.drawable.button_63x63);
        }
    }

    public void setGpsTrack(GPSTrack gpsTrack) {
        this.gpsTrack = gpsTrack;
    }
}