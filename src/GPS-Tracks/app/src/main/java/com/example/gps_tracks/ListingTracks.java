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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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


public class ListingTracks extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();
    private Button back, sync;
    ProgressBar progressBar;
    ListView listView;
    View callingItem;
    SharedPreferences.Editor editor;
    SharedPreferences mSettings;

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


        sync = (Button)findViewById(R.id.sync);
        sync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    synchronisation();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        //listing
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
        ArrayAdapter adapter = new FileListAdapter(this, files );

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
                popupMenuExample(fileName);
            }
        });
    }

    public void goBackHome() {
        Intent intent = new Intent(ListingTracks.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityIfNeeded(intent, 0);
    }

    private void popupMenuExample(String fileName) {
        PopupMenu p = new PopupMenu(this, callingItem);
        p.getMenuInflater().inflate(R.menu.popup_menu_example, p .getMenu());
        p.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {

                File dir = getFilesDir();
                File file = new File(dir, fileName);

                if(String.valueOf(item).equals(getString(R.string.details)))
                {

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
                    renameFile(file);
                }

                else if(String.valueOf(item).equals(getString(R.string.delete)))
                {
                    ondelList(fileName);
                    file.delete();
                    finish();
                    overridePendingTransition(10, 10);
                    startActivity(getIntent());
                    overridePendingTransition(10, 10);
                }


                Log.i("menu:", String.valueOf(item));
                return true;
            }
        });
        p.show();
    }
    
    private void ondelList(String selectedItem) {
        editor.putString(selectedItem, selectedItem);
        editor.commit();
    }

    private void renameFile(File file) {
        final Dialog dia = new Dialog(ListingTracks.this);
        dia.setContentView(R.layout.rename_track);

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(ListingTracks.this);
        View mView = getLayoutInflater().inflate(R.layout.rename_track, null);
        final EditText rn = (EditText) mView.findViewById(R.id.reninput);
        String OldFileName = rn.getText().toString();
        rn.setText(OldFileName);
        Button mok = (Button) mView.findViewById(R.id.ok);
        Button mab = (Button) mView.findViewById(R.id.ab);

        mab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                overridePendingTransition(10, 10);
                startActivity(getIntent());
                overridePendingTransition(10, 10);
                finish();
            }
        });

        mok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long newId = (Long)System.currentTimeMillis();
                String idString = '-'+ String.format("%020d",newId);

                String newFilename = rn.getText().toString()+idString+".gpx";
                if(newFilename.length()>4+14 && newFilename.length() <= 64){
                    file.renameTo(new File(getFilesDir(),newFilename));
                    Toast.makeText(ListingTracks.this,
                            getString(R.string.renameSuccess),
                            Toast.LENGTH_SHORT).show();
                    dia.dismiss();
                    overridePendingTransition(10, 10);
                    startActivity(getIntent());
                    overridePendingTransition(10, 10);
                } else {
                    Toast.makeText(ListingTracks.this,
                           getString(R.string.renameWarning),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        mBuilder.setView(mView);
        AlertDialog dialog = mBuilder.create();
        dialog.show();
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
                    outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
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
                    .url("http://aleksandrpronin.pythonanywhere.com/delete/"+filename)
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
        Collection<String> values =ret.values();
        String filePath = getApplicationContext().getFilesDir().getParent()+"/shared_prefs/del_files.xml";
        File deletePrefFile = new File(filePath);
        System.out.println("delList: "+values);

        // del
        for(String el:values){
            delFile(el);
            System.out.println("Values "+ el);
        }

        progressBar.setVisibility(ProgressBar.VISIBLE);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://aleksandrpronin.pythonanywhere.com/liste")
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
                Context context = getApplicationContext();
                String[] fileList = context.fileList();

                List<String> filesListFromFlask = new ArrayList<>(Arrays.asList(arrayFromFlask));
                System.out.println("Files From Flask: "+ filesListFromFlask);

                for (int i = 0; i < fileList.length; i++) {
                    filesListFromFlask.remove(fileList[i]);
                    //listFormated=filesListFromFlask[i]
                    if (i ==(fileList.length -1 )){
                        startActivity(getIntent());
                        overridePendingTransition(10, 10);
                    }
                }

                for(String el:filesListFromFlask){
                    System.out.println("Download from Flask: "+ filesListFromFlask);
                    downloadFile(el);
                }

                // upload
                File dir = getFilesDir();
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
            }
        });
    }

    public String[] downloadDelList() throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://aleksandrpronin.pythonanywhere.com/dellist")
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

