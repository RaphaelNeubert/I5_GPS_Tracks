package com.example.gps_tracks;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.core.util.Pair;

import java.util.List;

/**
 * Adapter needed to display track names given as {@literal Pair<String, Long>}
 */
public class FileListAdapter extends ArrayAdapter<Pair<String, Long>> {
    public FileListAdapter(Context context, List<Pair<String, Long>> objects) {
        super(context, 0, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.activity_list_view, parent, false);
        }

        ((TextView) convertView.findViewById(R.id.label))
                .setText(getItem(position).first);

        return convertView;
    }
}
