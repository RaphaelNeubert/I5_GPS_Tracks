package com.example.gps_tracks;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
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

/**
 * Homescreen and controlling instance for most actions.
 */
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
    private View view;

    /**
     * Will be called when the App is started.
     * @param savedInstanceState
     */
    @Override
        public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //handle permissions
        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
        });

        //will hide the title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // hide the title bar
        getSupportActionBar().hide();

        //load/initialize the osmdroid configuration
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        //inflate and create the map
        view = getLayoutInflater().inflate(R.layout.activity_main, null);
        setContentView(view);

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
        setupRecButton();
        setupSaveButton();


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

                            if (option.equals(getString(R.string.showTrack))) {
                                if (gpsTrack != null)
                                    gpsTrack.hide();
                                ImageButton recstart = (ImageButton) findViewById(R.id.record);
                                recstart.setVisibility(View.INVISIBLE);
                                ImageButton listButton = findViewById(R.id.listButton);
                                listButton.setVisibility(View.INVISIBLE);

                                loadTrack(data.getStringExtra("fileName"));
                                ImageButton deselectButton = findViewById(R.id.deselect);
                                deselectButton.setVisibility(View.VISIBLE);
                                cameraToTrack = true;
                            }
                            else if (option.equals(getString(R.string.edit))) {
                                if (gpsTrack != null)
                                    gpsTrack.hide();

                                ImageButton listButton = findViewById(R.id.listButton);
                                listButton.setVisibility(View.INVISIBLE);

                                ImageButton saveButton = (ImageButton) findViewById(R.id.save);
                                ImageButton recstart = (ImageButton) findViewById(R.id.record);
                                recstart.setVisibility(View.INVISIBLE);
                                saveButton.setVisibility(View.VISIBLE);
                                loadTrack(data.getStringExtra("fileName"));
                                cameraToTrack = true;
                                gpsTrack.startEditing(map);
                            }
                            else if (option.equals(getString(R.string.contRec))) {
                                if (gpsTrack != null)
                                    gpsTrack.hide();

                                ImageButton listButton = findViewById(R.id.listButton);
                                listButton.setVisibility(View.INVISIBLE);


                                loadTrack(data.getStringExtra("fileName"));
                                if (gpsTrack != null) {
                                    cameraToTrack = true;
                                    //continue recording
                                    recording = true;
                                    gpsTrack.startRecording();

                                    IntentFilter intentFilter = new IntentFilter("Location");
                                    registerReceiver(gpsTrack, intentFilter);

                                    //change icon
                                    ImageButton recstart = (ImageButton) findViewById(R.id.record);
                                    recstart.setImageResource(R.drawable.button_rec);
                                }
                            }
                        }
                    }
                });

    }

    private void setupSaveButton() {
        ImageButton saveButton = (ImageButton) findViewById(R.id.save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSaveDialog();
            }
        });
    }

    private void setupRecButton() {
        ImageButton recstart = (ImageButton) findViewById(R.id.record);
        recstart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recording) {
                    //stop recording
                    showSaveDialog();
                    //show listButton
                    ImageButton listButton = findViewById(R.id.listButton);
                    listButton.setVisibility(View.VISIBLE);

                } else {
                    //start recording
                    recording = true;
                    gpsTrack = new GPSTrack(MainActivity.this, map, view);
                    gpsTrack.startRecording();
                    //gpsTrack.setState(GPSTrack.State.RECORDING);

                    IntentFilter intentFilter = new IntentFilter("Location");
                    registerReceiver(gpsTrack, intentFilter);

                    gpsTrack.display();
                    //change icon
                    recstart.setImageResource(R.drawable.button_rec);
                    //hide listButton
                    ImageButton listButton = findViewById(R.id.listButton);
                    listButton.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    private void showSaveDialog() {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.save_track, null);
        mBuilder.setView(mView);
        AlertDialog dialog = mBuilder.create();
        final EditText sv = (EditText) mView.findViewById(R.id.saveinput);
        if (gpsTrack.getState() == GPSTrack.State.RECORDING)
            sv.setVisibility(View.VISIBLE);
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
                ImageButton recstart = (ImageButton) findViewById(R.id.record);
                if (gpsTrack.getState() == GPSTrack.State.EDITING) {
                    ImageButton saveButton = (ImageButton) findViewById(R.id.save);
                    saveButton.setVisibility(View.INVISIBLE);
                    recstart.setVisibility(View.VISIBLE);
                }
                recstart.setImageResource(R.drawable.button_63x63);
                recording = false;
                gpsTrack.hide();
                gpsTrack = null;

                // In case spi wasn't deselected before saving
                ImageButton deselect = findViewById(R.id.deselect_point);
                ImageButton spi_button = findViewById(R.id.edit_special_point);
                spi_button.setVisibility(View.INVISIBLE);
                deselect.setVisibility(View.INVISIBLE);
                //show listButton
                ImageButton listButton = findViewById(R.id.listButton);
                listButton.setVisibility(View.VISIBLE);
                dialog.dismiss();
            }
        });

        mok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageButton recstart = (ImageButton) findViewById(R.id.record);
                if (gpsTrack.getState() == GPSTrack.State.RECORDING) {
                    if (!sv.getText().toString().isEmpty()) {
                        gpsTrack.setTrackName(sv.getText().toString());
                    }
                    unregisterReceiver(gpsTrack); //stop receiving GPS data
                }
                //swap rec and save buttons visibility
                if (gpsTrack.getState() == GPSTrack.State.EDITING) {
                    ImageButton saveButton = (ImageButton) findViewById(R.id.save);
                    saveButton.setVisibility(View.INVISIBLE);
                    recstart.setVisibility(View.VISIBLE);
                }
                //change back icon
                recstart.setImageResource(R.drawable.button_63x63);
                gpsTrack.endRecording();
                recording = false;
                gpsTrack.hide();
                gpsTrack = null;

                Toast.makeText(MainActivity.this,
                        getString(R.string.saveSucess),
                        Toast.LENGTH_SHORT).show();
                // In case spi wasn't deselected before saving
                ImageButton deselect = findViewById(R.id.deselect_point);
                ImageButton spi_button = findViewById(R.id.edit_special_point);
                spi_button.setVisibility(View.INVISIBLE);
                deselect.setVisibility(View.INVISIBLE);
                //show listButton
                ImageButton listButton = findViewById(R.id.listButton);
                listButton.setVisibility(View.VISIBLE);
                dialog.dismiss();
            }
        });
        dialog.show();
    }


    /**
     * Will be called by the Android-System when the App gets maximized after being minimized.
     */
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

    /**
     * Will be called by the Android-System when the App gets minimized.
     */
    @Override
    public void onPause() {
        super.onPause();
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    /**
     * Will be called after Permissions have been granted or denied.
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i("requestCode", String.valueOf(requestCode));
        switch (requestCode) {
            case REQUEST_PERMISSIONS_REQUEST_CODE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                }  else {
                    //permission was denied
                    Toast.makeText(MainActivity.this,
                            getString(R.string.permissionWarning),
                            Toast.LENGTH_SHORT).show();
                }
        }
    }

    /**
     * Used to ask the user for permissions.
     * @param permissions
     */
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

    /**
     * starts ListingTrack activity.
     * @param view
     */
    public void openListTracks(View view) {
        Intent intent = new Intent(MainActivity.this, ListingTracks.class);
        listingTracksResultLauncher.launch(intent);
    }

    /**
     * Will load and display a Track. Will also move camera to start point of Track.
     * @param fileName name of gpx-File
     */
    public void loadTrack(String fileName) {
        gpsTrack = new GPSTrack(MainActivity.this, map, view);
        gpsTrack.loadGPX(fileName);
        gpsTrack.display();
        //move camera to track
        locationOverlay.disableFollowLocation();
        mapController.setCenter(gpsTrack.getStartPoint());
    }

    /**
    * This function will hide an active GPSTrack that is shown on the map.
    * Will also change Visibility of UI-Elements
    * @param view android.view.View
    */
    public void deselect(View view) {
        gpsTrack.hide();
        gpsTrack = null;
        //hide button
        ImageButton deselectButton = findViewById(R.id.deselect);
        deselectButton.setVisibility(View.INVISIBLE);
        //show listButton
        ImageButton listButton = findViewById(R.id.listButton);
        listButton.setVisibility(View.VISIBLE);
        ImageButton recButton = findViewById(R.id.record);
        recButton.setVisibility(View.VISIBLE);
    }
}
