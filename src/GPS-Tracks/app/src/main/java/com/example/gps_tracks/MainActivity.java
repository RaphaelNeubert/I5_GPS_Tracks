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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;


import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.appcompat.app.AlertDialog;

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
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity{
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map = null;
    private MyLocationNewOverlay locationOverlay;
    IMapController mapController;
    private ImageButton toPosButton;
    private GPSTrack gpsTrack;
    ActivityResultLauncher<Intent> listingTracksResultLauncher;
    private boolean cameraToTrack = false;
    private boolean recording = false;

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

        mapController = map.getController();
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


        //Switch between defaultrecord and startrecord Button
        ImageButton recstart = (ImageButton) findViewById(R.id.record);
        recstart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recording) {
                    //stop recording

                    //save dialog
                    AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
                    View mView = getLayoutInflater().inflate(R.layout.save_track, null);
                    mBuilder.setView(mView);
                    AlertDialog dialog = mBuilder.create();
                    final EditText sv = (EditText) mView.findViewById(R.id.saveinput);
                    Button mok = (Button) mView.findViewById(R.id.saving);
                    Button mab = (Button) mView.findViewById(R.id.abb);
                    Button mdel = (Button) mView.findViewById(R.id.del);

                    mab.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.cancel();
                        }
                    });

                    mdel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            gpsTrack.endRecording();
                            recstart.setImageResource(R.drawable.button_63x63);
                            recording = false;
                            dialog.dismiss();
                        }
                    });

                    mok.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (!sv.getText().toString().isEmpty()) {
                                gpsTrack.setFileName(sv.getText().toString());
                            }
                                gpsTrack.endRecording();
                                gpsTrack.hide(map);
                                gpsTrack = null;
                                Toast.makeText(MainActivity.this,
                                        "Speichern erfolgreich",
                                        Toast.LENGTH_SHORT).show();
                                //change back icon
                                recstart.setImageResource(R.drawable.button_63x63);
                                recording = false;
                                dialog.dismiss();
                        }
                    });
                    dialog.show();

                } else {
                    //start recording
                    recording = true;
                    gpsTrack = new GPSTrack(getApplicationContext(), map);
                    gpsTrack.startRecording();
                    gpsTrack.setState(GPSTrack.State.RECORDING);
                    gpsTrack.display(map);
                    //change icon
                    recstart.setImageResource(R.drawable.button_rec);
                }
            }
        });



        MapListener mapListener = new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                if (locationOverlay.isFollowLocationEnabled() == false) {
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
                                    if(gpsTrack != null)
                                        gpsTrack.hide(map);

                                    loadTrack(data.getStringExtra("fileName"));
                                    cameraToTrack = true;
                                    break;
                                case "Track bearbeiten":
                                    loadTrack(data.getStringExtra("fileName"));
                                    cameraToTrack = true;
                                    gpsTrack.startEditing(map);
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
        if (cameraToTrack) {
            //move camera to track
            locationOverlay.disableFollowLocation();
            map.getController().animateTo(gpsTrack.getStartPoint());
            cameraToTrack = false;
        }

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

    public void loadTrack(String fileName) {
        gpsTrack = new GPSTrack(getApplicationContext(), map);
        gpsTrack.loadGPX(fileName);
        gpsTrack.display(map);
        ImageButton deselectButton = findViewById(R.id.deselect);
        deselectButton.setVisibility(View.VISIBLE);
        //move camera to track
        locationOverlay.disableFollowLocation();
        mapController.setCenter(gpsTrack.getStartPoint());
    }

    /**
    * This function will hide a GPSTrack that is shown on the map
    * If you call it from a Button in xml, you don't have to care about view
    * @param view android.view.View
    * @return void
    */
    public void deselect(View view) {
        gpsTrack.hide(map);
        gpsTrack = null;
        //hide button
        ImageButton deselectButton = findViewById(R.id.deselect);
        deselectButton.setVisibility(View.INVISIBLE);
    }
}
