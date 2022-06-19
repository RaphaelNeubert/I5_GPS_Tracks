package com.example.gps_tracks;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.util.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.util.Map;
import java.util.TimeZone;


public class ListingTracks extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();
    private Button back, sync;
    ProgressBar progressBar;
    ListView listView;
    View callingItem;
    SharedPreferences.Editor editor;
    SharedPreferences mSettings;
    FileListAdapter adapter;
    SyncManager syncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mSettings = getSharedPreferences("del_files", Context.MODE_MULTI_PROCESS);
        editor = mSettings.edit();
        super.onCreate(savedInstanceState);
        //will hide the title
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // hide the title bars
        getSupportActionBar().hide();

        setContentView(R.layout.activity_listing_tracks);

        back = (Button) findViewById(R.id.backToHome);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                goBackHome();
            }
        });

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        syncManager = new SyncManager(this, getApplicationContext(),progressBar);

        sync = (Button)findViewById(R.id.sync);
        sync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    syncManager.synchronisation();
                    refreshListing();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        refreshListing();
    }
    public void refreshListing() {
        String[] tmpFiles = fileList();
        List<Pair<String, Long>> files = new ArrayList<>();
        for (int i=0; i< tmpFiles.length; i++) {
            if (tmpFiles[i].endsWith(".gpx")) {
                int n = tmpFiles[i].length();

                String name = tmpFiles[i].substring(0, n-4-21); //remove extension and id
                long id = Long.parseLong(tmpFiles[i].substring(n-4-20, n-4));
                files.add(new Pair<String,Long>(name, id));
            }
        }
        adapter = new FileListAdapter(this, files );

        listView = (ListView) findViewById(R.id.listTracks);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Pair<String, Long> selectedItem = (Pair) parent.getItemAtPosition(position);
                callingItem = view;
                Log.i("button:", selectedItem.first);
                String fileName = selectedItem.first;
                //reappend id
                fileName += '-'+String.format("%020d", selectedItem.second);
                //reappend file extension
                fileName+= ".gpx";
                popupMenuExample(fileName, selectedItem);
            }
        });
    }

    public void goBackHome() {
        Intent intent = new Intent(ListingTracks.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityIfNeeded(intent, 0);
    }

    private void popupMenuExample(String fileName, Pair<String, Long> selectedItem) {
        PopupMenu p = new PopupMenu(this, callingItem);
        p.getMenuInflater().inflate(R.menu.popup_menu_example, p .getMenu());
        p.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {

                File dir = getFilesDir();
                File file = new File(dir, fileName);

                if(String.valueOf(item).equals(getString(R.string.details)))
                {
                    // map and view are not needed to get the distance
                    GPSTrack track = new GPSTrack(getApplicationContext(),null,null);
                    track.loadGPX(fileName);
                    int distance = (int) Math.round(track.getDistance());
                    Date date = new Date(selectedItem.second);
                    DateFormat formatter = new SimpleDateFormat("dd.MM.YYYY, HH:mm:ss");
                    formatter.setTimeZone(TimeZone.getTimeZone("CET"));
                    String timeStr = formatter.format(date);
                    String distStr = getString(R.string.dist) +' '+ String.valueOf(distance) + "m";
                    String finalStr = timeStr + '\n' + distStr;
                    Toast.makeText(ListingTracks.this,
                        finalStr, Toast.LENGTH_SHORT).show();
                }
                else if(String.valueOf(item).equals(getString(R.string.showTrack)))
                {
                    Intent intent = new Intent();
                    intent.putExtra("optionClicked", String.valueOf(item));
                    intent.putExtra("fileName", fileName);

                    setResult(Activity.RESULT_OK, intent);
                    finish();
                }

                else if(String.valueOf(item).equals(getString(R.string.edit)))
                {
                    Intent intent = new Intent();
                    intent.putExtra("optionClicked", String.valueOf(item));
                    intent.putExtra("fileName", fileName);

                    setResult(Activity.RESULT_OK, intent);
                    finish();

                }

                else if(String.valueOf(item).equals(getString(R.string.contRec)))
                {

                }

                else if(String.valueOf(item).equals(getString(R.string.rename)))
                {
                    // delete from server
                    syncManager.ondelList(fileName);
                    syncManager.delFile(fileName);
                    renameFile(file, selectedItem);
                }

                else if(String.valueOf(item).equals(getString(R.string.delete)))
                {
                    syncManager.ondelList(fileName);
                    file.delete();
                    adapter.remove(selectedItem);
                }


                Log.i("menu:", String.valueOf(item));
                return true;
            }
        });
        p.show();
    }
    
    private void renameFile(File file, Pair<String, Long> selectedItem) {
        final Dialog dia = new Dialog(ListingTracks.this);
        dia.setContentView(R.layout.rename_track);

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(ListingTracks.this);
        View mView = getLayoutInflater().inflate(R.layout.rename_track, null);
        final EditText rn = (EditText) mView.findViewById(R.id.reninput);
        String OldFileName = rn.getText().toString();
        rn.setText(OldFileName);

        Button mok = (Button) mView.findViewById(R.id.ok);
        Button mab = (Button) mView.findViewById(R.id.ab);

        mBuilder.setView(mView);
        AlertDialog dialog = mBuilder.create();

        mab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                dialog.cancel();
            }
        });

        mok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long newId = (Long)System.currentTimeMillis();
                String idString = '-'+ String.format("%020d",newId);

                String newName = rn.getText().toString();
                String newFilename = newName+idString+".gpx";
                if(newFilename.length()>4+14 && newFilename.length() <= 64){
                    file.renameTo(new File(getFilesDir(),newFilename));
                    int pos = adapter.getPosition(selectedItem);
                    //update UI
                    adapter.remove(selectedItem);
                    adapter.insert(new Pair<>(newName,newId), pos);
                    Toast.makeText(ListingTracks.this,
                            getString(R.string.renameSuccess),
                            Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(ListingTracks.this,
                           getString(R.string.renameWarning),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

}

