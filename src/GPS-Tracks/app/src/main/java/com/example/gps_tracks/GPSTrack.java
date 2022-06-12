package com.example.gps_tracks;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.alternativevision.gpx.GPXParser;
import org.alternativevision.gpx.beans.*;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.zip.Inflater;


public class GPSTrack extends BroadcastReceiver{

    public enum State {READY, RECORDING, EDITING, EMPTY, CONTREC}
    private Context context;
    private Intent recIntent;
    private GpsMyLocationProvider gpsMyLocationProvider;
    private State state = State.EMPTY;
    private GeoPoint startPoint;

    private Polyline path;
    private String trackName;
    private long id;
    private ArrayList<Marker> markerList;
    private MapView map;
    private Hashtable<Integer,String> specialPoints;
    private boolean bubbleOpen = false;
    private View view;
    private Context test;

    GPSTrack(Context context, MapView map, View view, Context test) {
        this.test = test;
        this.map = map;
        this.view = view;
        this.test = test;
        specialPoints = new Hashtable<>();
        specialPoints.put(0,"haha");
    }
    void display(MapView map) {
        if (path != null) {
            map.getOverlayManager().add(path);
        }
    }
    void hide(MapView map) {
        if (path != null) {
            map.getOverlayManager().remove(path);
            if (markerList != null) {
                ListIterator listIterator = markerList.listIterator();
                while(listIterator.hasNext()) {
                    map.getOverlayManager().remove(listIterator.next());
                }
            }
        }
    }
    @Override
    public void onReceive(Context test, Intent intent) {
        if (state != State.RECORDING && state != State.CONTREC) {
            //abort if we don't want to receive Location data
            return;
        }
        Log.i("onReceive", intent.getStringExtra("gpsPos"));
        GeoPoint point = GeoPoint.fromDoubleString(intent.getStringExtra("gpsPos"),',');
        path.addPoint(point);
        if (startPoint == null) startPoint = point;

    }
    boolean startRecording() {
        Log.i("GPSTrack","Recording has been started.");
        int i = 0;

        if(path == null) {
            state = State.RECORDING;
            path = new Polyline();
        }
        else
            state = State.CONTREC;


        recIntent = new Intent(test, RecordingService.class);
        test.startForegroundService(recIntent);

        return true;
    }
    boolean endRecording() {
        if(state == State.RECORDING) {
            test.stopService(recIntent);
        }
        saveAsGPX();
        state = State.READY;
        Log.i("GPSTrack","Recording has been stopped.");
        return true;
    }
    private void saveAsGPX() {
        GPX gpx = new GPX();
        Route route = generateRoute();
        gpx.addRoute(route);

        GPXParser p = new GPXParser();
        try {
            File file;
            String path = test.getFilesDir().toString();
            long newId = (Long)System.currentTimeMillis();
            if (this.id != 0) { //delete old track
                String oldIdString = '-'+ String.format("%020d",this.id);
                file = new File(path+'/'+this.trackName +oldIdString+".gpx");
                file.delete();
            }

            String idString = '-'+ String.format("%020d",newId);
            if (this.trackName != null) {
                file = new File(path+'/'+this.trackName +idString+".gpx");
                //out = new FileOutputStream(this.fileName);
            }
            else {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy hh:mm");
                String fileName = "New Track " + simpleDateFormat.format(new Date());
                file = new File(path+'/'+fileName+idString+".gpx");
                //out = new FileOutputStream(fileName);
            }
            Log.i("GPSTrack",file.getName());
            FileOutputStream out = new FileOutputStream(file);
            p.writeGPX(gpx, out);
            out.close();
        } catch (Exception e) {
            warning();
            Log.e("GPSTrack", "Failed to save File", e);
        }
    }

    @NonNull
    private Route generateRoute() {
        Route route = new Route();
        List<GeoPoint> pointList = path.getActualPoints();
        ListIterator listIterator = pointList.listIterator();

        while(listIterator.hasNext()) {
            int i = listIterator.nextIndex();
            Log.i("route i:", String.valueOf(i));
            Waypoint waypoint = new Waypoint();
            GeoPoint point = (GeoPoint) listIterator.next();
            waypoint.setLatitude(point.getLatitude());
            waypoint.setLongitude(point.getLongitude());
            if (specialPoints.containsKey(i)) {
                String type = specialPoints.get(i);
                waypoint.setType(type);
            }
            else {
                waypoint.setType("default");
            }

            route.addRoutePoint(waypoint);
        }
        return route;
    }

    /**
    * Loads the GPSTrack of a given file into the Polyline path of the calling instance
    * The String filename should have the extension .gpx
    * @param fileName name of the gpx-file to load
    * @return void
    */
    void loadGPX(String fileName) {
        int n = fileName.length();
        this.trackName = fileName.substring(0, n-4-21); //remove extension and id
        this.id = Long.parseLong(fileName.substring(n-4-20, n-4));
        path = new Polyline();
        try {
            String dir = test.getFilesDir().toString();
            File file = new File(test.getFilesDir(),fileName);
            FileInputStream fis = new FileInputStream(file);
            GPXParser parser = new GPXParser();
            GPX gpx = parser.parseGPX(fis);
            HashSet<Route> routes = gpx.getRoutes();
            Iterator routeIterator = routes.iterator();

            while(routeIterator.hasNext()) {
                Route route = (Route) routeIterator.next();
                List<Waypoint> listWaypoints = route.getRoutePoints();
                ListIterator listIterator = listWaypoints.listIterator();

                while(listIterator.hasNext()) {
                    int i = listIterator.nextIndex();
                    Waypoint wp = (Waypoint) listIterator.next();
                    GeoPoint geo = new GeoPoint(wp.getLatitude(), wp.getLongitude());
                    this.path.addPoint(geo);

                    //store special points of interests in Hashtable
                    String type = wp.getType();
                    if (type != "default") {
                        specialPoints.put(i, type);
                    }
                    //set startPoint if not set
                    if (startPoint == null) startPoint = geo;
                }
            }

            fis.close();
        } catch (Exception e) {
            warning();
            Log.e("GPSTrack", "Failed to load File", e);
        }
    }
    public void startEditing(MapView map){
        state = State.EDITING;
        markerList = new ArrayList<Marker>();
        List<GeoPoint> points = path.getActualPoints();
        ListIterator listIterator = points.listIterator();

        while(listIterator.hasNext()) {
            int i = listIterator.nextIndex();
            Marker m = new Marker(map);
            m.setId(String.valueOf(i));
            m.setPosition((GeoPoint) listIterator.next());
            m.setIcon(test.getResources().getDrawable(R.drawable.edit));
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            m.setDraggable(true);
            m.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
                @Override
                public void onMarkerDrag(Marker marker) {
                    List<GeoPoint> geoPointList = path.getActualPoints();
                    int index = Integer.parseInt(marker.getId());
                    geoPointList.set(index, marker.getPosition());

                    map.getOverlayManager().remove(path);
                    path = new Polyline();
                    path.setPoints(geoPointList);
                    map.getOverlayManager().add(path);
                }
                @Override
                public void onMarkerDragEnd(Marker marker) {}
                @Override
                public void onMarkerDragStart(Marker marker) {}
            });
            MarkerBubble markerBubble = new MarkerBubble(R.layout.waypoint_bubble,map,this,view);
            if (specialPoints.containsKey(i))
                markerBubble.setTitle(specialPoints.get(i));
            else
                markerBubble.setTitle("default");

            m.setInfoWindow(markerBubble);
            m.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker, MapView mapView) {
                    if (!bubbleOpen) {
                        bubbleOpen = true;
                        m.showInfoWindow();
                        ImageButton editButton = (ImageButton) view.findViewById(R.id.edit_special_point);
                        editButton.setVisibility(View.VISIBLE);
                        editButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v){
                                showSpecialPointDialog();
                            }
                        });
                    }
                    return true;
                }
            });
            markerList.add(m);
            map.getOverlayManager().add(m);
        }
    }
    private void showSpecialPointDialog() {
        final Dialog dia = new Dialog(test);
        dia.setContentView(R.layout.rename_track);

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(test);
        LayoutInflater inflater = LayoutInflater.from(test);
        View mView = inflater.inflate(R.layout.edit_special_point, null);
        final EditText rn = (EditText) mView.findViewById(R.id.spi_edit_text);
        String OldFileName = rn.getText().toString();
        rn.setText(OldFileName);
        Button mok = (Button) mView.findViewById(R.id.spi_ok);
        Button mab = (Button) mView.findViewById(R.id.spi_ab);

        mab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
            }
        });
        mok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

        mBuilder.setView(mView);
        AlertDialog dialog = mBuilder.create();
        dialog.show();
    }
    public void warning() {
        Toast.makeText(test.getApplicationContext(),
                "Error: Failed to load file", Toast.LENGTH_SHORT).show();
    }
    public State getState() {
        return state;
    }
    public Polyline getPath() {
        return path;
    }
    public void setPath(Polyline path) {
        this.path = path;
    }
    public void setState(State state) {
        this.state = state;
    }
    public String getTrackName() {
        return trackName;
    }
    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }
    public GeoPoint getStartPoint(){
        return startPoint;
    }
    public void setStartPoint(GeoPoint point){
        this.startPoint = point;
    }
    public void setBubbleOpen(boolean bubbleOpen) {
        this.bubbleOpen = bubbleOpen;
    }
}
