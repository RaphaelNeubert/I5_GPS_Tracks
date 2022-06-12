package com.example.gps_tracks;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

public class MarkerBubble extends InfoWindow {
    private GPSTrack gpsTrack;
    private View view;

    public MarkerBubble(int layoutResId, MapView mapView, GPSTrack gpsTrack, View view) {
        super(layoutResId, mapView);
        this.gpsTrack = gpsTrack;
        this.view = view;
    }
    public void setTitle(String title) {
        TextView txtTitle = (TextView) mView.findViewById(R.id.bubble_title);
        txtTitle.setText(title);

    }

    public void onOpen(Object arg0) {
        LinearLayout layout = (LinearLayout) mView.findViewById(R.id.bubble_layout);
        layout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Override Marker's onClick behaviour here
                close();
            }
        });
    }
    public void onClose() {
        gpsTrack.setBubbleOpen(false);
        ImageButton editButton = (ImageButton) view.findViewById(R.id.edit_special_point);
        editButton.setVisibility(View.INVISIBLE);
    }
}
