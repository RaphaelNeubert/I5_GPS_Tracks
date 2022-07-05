package com.example.gps_tracks;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Client-Server component. Allows you to exchange data with the server and compile lists of files.
 */
public class SyncManager {
    public static final String TAG = MainActivity.class.getSimpleName();
    private static String serverAddresse  = "http://141.56.137.84:5000/";
    SharedPreferences.Editor editor;
    SharedPreferences mSettings;
    ListingTracks listingTracks;
    Context context;

    ProgressBar progressBar;

    /**
     *  Initialise the objects required for operation.
     */
    public SyncManager(ListingTracks listingTracks, Context context, ProgressBar progressBar) {
        this.context = context;
        mSettings = context.getSharedPreferences("del_files", Context.MODE_MULTI_PROCESS);
        editor = mSettings.edit();
        this.listingTracks = listingTracks;
        this.progressBar = progressBar;

    }

    /**
     * Creates a list of deleted files. After sending a request to the server to delete files, this list is cleared.
     */
    public static void ondelList(String selectedItem, Context context) {
        SharedPreferences.Editor editor;
        SharedPreferences mSettings;
        mSettings = context.getSharedPreferences("del_files", Context.MODE_MULTI_PROCESS);
        editor = mSettings.edit();
        editor.putString(selectedItem, selectedItem);
        editor.commit();
    }

    /**
     * Allows GPS tracks to be uploaded to the server using POST requests.
     */
    public static Boolean uploadFile(File file) {
        OkHttpClient okHttpClient = new OkHttpClient();
        // deploy on http://pythonanywhere.com

        try {
            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getName(),
                            RequestBody.create(MediaType.parse("text/csv"), file))
                    .build();

            Request request = new Request.Builder()
                    .url(serverAddresse+"upload")
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

    /**
     * Enables GPS tracks to be downloaded from the server using GET requests.
     */
    public void downloadFile(String filename) throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                //.url("http://aleksandrpronin.pythonanywhere.com/download/"+filename)
                .url(serverAddresse+"download/"+filename)
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

    /**
     * Allows GPS tracks to be deleted from the server using GET requests. 
     */
    public static Boolean delFile(String filename) {
        OkHttpClient okHttpClient = new OkHttpClient();
        try {
            Request request = new Request.Builder()
                    //.url("http://ip:5000/download/"+filename)
                    //.url("http://aleksandrpronin.pythonanywhere.com/delete/"+filename)
                    .url(serverAddresse+"delete/"+filename)
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


    /**
     * reads files-lists and runs REST methods.
     */
    public void synchronisation() throws IOException {

        // the map containing the items to be deleted
        Map<String, String> ret = (Map<String, String>) mSettings.getAll();
        editor.clear();
        Collection<String> toDelete = ret.values();
        String filePath = context.getFilesDir().getParent()+"/shared_prefs/del_files.xml";
        File deletePrefFile = new File(filePath);
        System.out.println("delList: "+toDelete);

        // delete
        for(String el:toDelete){
            delFile(el);
            System.out.println("Values "+ el);
        }

        progressBar.setVisibility(ProgressBar.VISIBLE);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(serverAddresse+"liste")
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


                // download
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

    /**
     * Enables GPS tracks to be loaded from the GET requests to download GPS tracks from the server.
     */
    public String[] downloadDelList() throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(serverAddresse+"dellist")
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

