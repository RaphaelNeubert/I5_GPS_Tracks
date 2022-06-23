package com.example.gps_tracks;

import android.widget.LinearLayout;
import android.widget.TextView;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

public class MarkerBubble extends InfoWindow {

    public MarkerBubble(int layoutResId, MapView mapView) {
        super(layoutResId, mapView);
    }
    public void setTitle(String title) {
        TextView txtTitle = (TextView) mView.findViewById(R.id.bubble_title);
        txtTitle.setText(title);
    }

    public void onOpen(Object arg0) {
        LinearLayout layout = (LinearLayout) mView.findViewById(R.id.bubble_layout);
    }
    public void onClose() {
    }
}
