package com.example.gps_tracks;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import org.alternativevision.gpx.GPXParser;
import org.alternativevision.gpx.beans.*;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;


public class GPSTrack {
    public enum State {READY, RECORDING, EDITING, EMPTY}
    private Context context;
    private GpsMyLocationProvider gpsMyLocationProvider;
    private State state = State.EMPTY;
    private GeoPoint startPoint;
    private Polyline path;
    private String fileName;
    private ArrayList<Marker> markerList;
    private MapView map;


    GPSTrack(Context context, MapView map) {
        this.context = context;
        this.map = map;
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
    boolean startRecording() {
        Log.i("GPSTrack","Recording has been started.");
        state = State.RECORDING;
        int i = 0;
        path = new Polyline();

        gpsMyLocationProvider = new GpsMyLocationProvider(context);
        gpsMyLocationProvider.setLocationUpdateMinDistance(5.0f);
        gpsMyLocationProvider.startLocationProvider(new IMyLocationConsumer() {
            @Override
            public void onLocationChanged(Location location, IMyLocationProvider source) {
                String position = "Location changed to: " + new GeoPoint(location).toDoubleString();
                Log.i("onLocationChanged: ", position);
                path.addPoint(new GeoPoint(location));
                if (startPoint == null) startPoint = new GeoPoint(location);
            }
        });
        return true;
    }
    boolean endRecording() {
        state = State.READY;
        saveAsGPX();
        Log.i("GPSTrack","Recording has been stopped.");
        gpsMyLocationProvider.destroy();
        return true;
    }
    private void saveAsGPX() {
        GPX gpx = new GPX();
        Route route = new Route();
        List<GeoPoint> pointList = path.getActualPoints();
        ListIterator listIterator = pointList.listIterator();

        while(listIterator.hasNext()) {
            Waypoint waypoint = new Waypoint();
            GeoPoint point = (GeoPoint) listIterator.next();
            waypoint.setLatitude(point.getLatitude());
            waypoint.setLongitude(point.getLongitude());

            route.addRoutePoint(waypoint);
        }
        gpx.addRoute(route);

        GPXParser p = new GPXParser();
        try {
            File file;
            String path = context.getFilesDir().toString();
            String id = ((Long)System.currentTimeMillis()).toString();
            if (this.fileName != null) {
                file = new File(path+'/'+this.fileName+id+".gpx");
                //out = new FileOutputStream(this.fileName);
            }
            else {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy hh:mm");
                String fileName = "New Track " + simpleDateFormat.format(new Date());
                file = new File(path+'/'+fileName+id+".gpx");
                //out = new FileOutputStream(fileName);
            }
            Log.i("GPSTrack",file.getName());
            FileOutputStream out = new FileOutputStream(file);
            p.writeGPX(gpx, out);
            out.close();
        } catch (Exception e) {
            //TODO display warning popup
            Log.e("GPSTrack", "Failed to save File", e);
        }
    }
    /**
    * Loads the GPSTrack of a given file into the Polyline path of the calling instance
    * The String filename should have the extension .gpx
    * @param fileName name of the gpx-file to load
    * @return void
    */
    void loadGPX(String fileName) {
        this.fileName = fileName;
        path = new Polyline();
        try {
            String dir = context.getFilesDir().toString();
            File file = new File(context.getFilesDir(),fileName);
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
                    Waypoint wp = (Waypoint) listIterator.next();
                    GeoPoint geo = new GeoPoint(wp.getLatitude(), wp.getLongitude());
                    this.path.addPoint(geo);
                    //set startPoint if not set
                    if (startPoint == null) startPoint = geo;
                }
            }

            fis.close();
        } catch (Exception e) {
            //TODO display warning popup
            Log.e("GPSTrack", "Failed to load File", e);
        }
    }
    public void startEditing(MapView map){
        state = State.EDITING;
        markerList = new ArrayList<Marker>();
        List<GeoPoint> points = path.getActualPoints();
        ListIterator listIterator = points.listIterator();

        while(listIterator.hasNext()) {
            Marker m = new Marker(map);
            m.setPosition((GeoPoint) listIterator.next());
            m.setId(String.valueOf(listIterator.nextIndex()-1));
            m.setIcon(context.getResources().getDrawable(R.drawable.edit));
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
                public void onMarkerDragEnd(Marker marker) {

                }

                @Override
                public void onMarkerDragStart(Marker marker) {

                }
            });
            markerList.add(m);
            map.getOverlayManager().add(m);
        }
    }
    public State getState() {
        return state;
    }
    public void setState(State state) {
        this.state = state;
    }
    public String getFileName() {
        return fileName;
    }
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    public GeoPoint getStartPoint(){
        return startPoint;
    }
}
