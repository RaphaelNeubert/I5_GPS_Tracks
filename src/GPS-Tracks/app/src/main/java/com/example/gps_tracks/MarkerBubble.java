package com.example.gps_tracks;

import android.widget.LinearLayout;
import android.widget.TextView;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

/**
 * MarkerBubbles are InfoWindows that can be assigned to a {@link org.osmdroid.views.overlay.Marker}
 * Once assigned, they can be opened by clicking the {@link org.osmdroid.views.overlay.Marker}
 */
public class MarkerBubble extends InfoWindow {

    public MarkerBubble(int layoutResId, MapView mapView) {
        super(layoutResId, mapView);
    }

    /**
     * Changes the title text inside the Bubble
     * @param title string that will be shown inside the Bubble
     */
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
