package com.example.gps_tracks;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;


import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;

/**
 *  Foreground Service used to continue getting waypoints even if the MainActivity is paused (standby, minimized).
 *  Uses Broadcast to send Waypoints to {@link GPSTrack} instances.
 */
public class RecordingService extends Service {
    private GpsMyLocationProvider gpsMyLocationProvider;

    public RecordingService() {
        Log.i("RecordingService", "RecordingService was created");

    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("RecordingService", "RecordingService was started");
        // If the notification supports a direct reply action, use
        // PendingIntent.FLAG_MUTABLE instead.
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE);

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("channel1", "test", NotificationManager.IMPORTANCE_DEFAULT));
        Notification notification =
                new Notification.Builder(this, "channel1")
                        .setContentTitle("test")
                        .setContentText("test")
                        .setContentIntent(pendingIntent)
                        .build();



        gpsMyLocationProvider = new GpsMyLocationProvider(getApplicationContext());
        gpsMyLocationProvider.setLocationUpdateMinDistance(5.0f);
        gpsMyLocationProvider.startLocationProvider(new IMyLocationConsumer() {
                @Override
                public void onLocationChanged(Location location, IMyLocationProvider source) {
                    String position = "Location changed to: " + new GeoPoint(location).toDoubleString();
                    Log.i("RecordingService: ", position);
                    Intent intent = new Intent("Location");
                    intent.putExtra("gpsPos", new GeoPoint(location).toDoubleString());
                    sendBroadcast(intent);
                }
            });

        // Notification ID cannot be 0.
        startForeground(5, notification);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}