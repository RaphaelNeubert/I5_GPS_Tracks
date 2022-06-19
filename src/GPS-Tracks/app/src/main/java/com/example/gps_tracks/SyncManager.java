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
import java.util.Map;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class SyncManager {
    public static final String TAG = MainActivity.class.getSimpleName();
    SharedPreferences.Editor editor;
    SharedPreferences mSettings;
    ListingTracks listingTracks;
    Context context;
    ProgressBar progressBar;

    public SyncManager(ListingTracks listingTracks, Context context, ProgressBar progressBar) {
        this.context = context;
        mSettings = context.getSharedPreferences("del_files", Context.MODE_MULTI_PROCESS);
        editor = mSettings.edit();
        this.listingTracks = listingTracks;
        this.progressBar = progressBar;

    }

    public static void ondelList(String selectedItem, Context context) {
        SharedPreferences.Editor editor;
        SharedPreferences mSettings;
        mSettings = context.getSharedPreferences("del_files", Context.MODE_MULTI_PROCESS);
        editor = mSettings.edit();
        editor.putString(selectedItem, selectedItem);
        editor.commit();
    }

    public static Boolean uploadFile(File file) {
        OkHttpClient okHttpClient = new OkHttpClient();
        // deploy on http://pythonanywhere.com

        try {
            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getName(),
                            RequestBody.create(MediaType.parse("text/csv"), file))
                    .build();

            Request request = new Request.Builder()
                    .url("http://141.56.137.84:5000/upload")
                    .post(requestBody)
                    .build();

            okHttpClient.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(final Call call, final IOException e) {
                    // Handle the error
                    System.out.println("Error => " + e);
                }

                @Override
                public void onResponse(final Call call, final Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        // Handle the error
                    }
                    // Upload successful
                }
            });
            return true;
        } catch (Exception ex) {
            // Handle the error
        }
        return false;
    }

    public void downloadFile(String filename) throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                //.url("http://aleksandrpronin.pythonanywhere.com/download/"+filename)
                .url("http://141.56.137.84:5000/download/"+filename)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
            }

            @Override
            public void onResponse(final Call call, Response response) throws IOException {
                //Log.d("TAG",response.body().string());
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to download file: " + response);
                }
                FileOutputStream outputStream;
                try {
                    outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
                    outputStream.write(response.body().bytes());
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static Boolean delFile(String filename) {
        OkHttpClient okHttpClient = new OkHttpClient();
        try {
            Request request = new Request.Builder()
                    //.url("http://ip:5000/download/"+filename)
                    //.url("http://aleksandrpronin.pythonanywhere.com/delete/"+filename)
                    .url("http://141.56.137.84:5000/delete/"+filename)
                    .build();

            okHttpClient.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(final Call call, final IOException e) {
                    // Handle the error
                    System.out.println("Error => " + e);
                }

                @Override
                public void onResponse(final Call call, final Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        // Handle the error
                    }
                    // Upload successful
                }
            });
            return true;
        } catch (Exception ex) {
            // Handle the error
        }
        return false;
    }


    public void synchronisation() throws IOException {

        // the map containing the items to be deleted
        Map<String, String> ret = (Map<String, String>) mSettings.getAll();
        editor.clear();
        Collection<String> toDelete = ret.values();
        String filePath = context.getFilesDir().getParent()+"/shared_prefs/del_files.xml";
        File deletePrefFile = new File(filePath);
        System.out.println("delList: "+toDelete);

        // del
        for(String el:toDelete){
            delFile(el);
            System.out.println("Values "+ el);
        }

        progressBar.setVisibility(ProgressBar.VISIBLE);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://141.56.137.84:5000/liste")
                .build();

         client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
            }

            @Override
            public void onResponse(final Call call, Response response) throws IOException {
                String responseData = response.body().string();
                String listFormated=responseData.replace("'", "").replace(", ",",").replace("[","").replace("]","");
                String[] arrayFromFlask = listFormated.split(",");


                // сdownload
                String[] fileList = context.fileList();

                List<String> filesListFromFlask = new ArrayList<>(Arrays.asList(arrayFromFlask));
                System.out.println("Files From Flask: "+ filesListFromFlask);

                for (int i = 0; i < fileList.length; i++) {
                    Log.i("local", new ArrayList<>(Arrays.asList(fileList)).toString());
                    Log.i("server", filesListFromFlask.toString());
                    filesListFromFlask.remove(fileList[i]);
                }

                for(String el:filesListFromFlask){
                    System.out.println("Download from Flask: "+ filesListFromFlask);
                    if (!toDelete.contains(el))
                        downloadFile(el);
                }

                // upload
                File dir = context.getFilesDir();
                File file;
                List<String> localeFiles = new ArrayList<>(Arrays.asList(fileList));
                localeFiles.remove("osmdroid");
                System.out.println("Files Local: "+ localeFiles);

                for (int i = 0; i < arrayFromFlask.length; i++) {
                    localeFiles.remove(arrayFromFlask[i]);
                }

                /* Files deleted from the server will not be re-downloaded to the server.
                 ** The entire list of deleted files is stored in a list.
                 */
                String[] delListFromFlask = downloadDelList();
                for (int i = 0; i < delListFromFlask.length; i++) {
                    localeFiles.remove(delListFromFlask[i]);
                }

                for(String el:localeFiles){
                    file = new File(dir, el);
                    uploadFile(file);
                    System.out.println("Locale Files zum upload "+ localeFiles);
                }
                //okhttp callbacks run in background and can't access the UI. In order to access the UI
                //we need to run something in the UI Thread.
                listingTracks.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Handle UI here
                        progressBar.setVisibility(View.INVISIBLE);
                        listingTracks.refreshListing();
                    }
                });
            }
        });
    }

    public String[] downloadDelList() throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://141.56.137.84:5000/dellist")
                .build();
        Response response = client.newCall(request).execute();
        String responseData = response.body().string();
        String listFormated=responseData.replace("'", "").replace(", ",",").replace("[","").replace("]","");
        String[] arrayFromFlask = listFormated.split(",");
        List<String> filesListFromFlask = new ArrayList<>(Arrays.asList(arrayFromFlask));
        System.out.println("Alle Dateien, die vom Server gelöscht wurden: "+ filesListFromFlask);
        return arrayFromFlask;
    }
}

