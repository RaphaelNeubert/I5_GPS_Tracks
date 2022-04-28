package com.example.gps_tracks;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;



public class ListingTracks extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();
    private Button back, sync;
    ProgressBar progressBar;
    String[] testdata = {"Neuer Track 05.11.2022 12:15 Uhr","Neuer Track 22.08.2022 18:25 Uhr",
                         "Runde durch den Park","Neuer Track 20.04.2022 03:43 Uhr",
                         "Neuer Track 20.04.2022 10:14 Uhr"};
    String fileList;
    //String[] instrumentFileList = ListingTracks.this.fileList();
    ListView listView;
    View callingItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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


        // listing
        ArrayAdapter adapter = new ArrayAdapter<String>(this,
                R.layout.activity_list_view, fileList());

        listView = (ListView) findViewById(R.id.listTracks);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // to do: delete if not required
                Context context = getApplicationContext();
                String[] files = context.fileList();
                Log.i(TAG, String.valueOf(files.length));
                for (int i = 0; i < files.length; i++) {
                    Log.i(TAG, "file was found!");
                    Log.i(TAG, files[i]);
                }
                String selectedItem = (String) parent.getItemAtPosition(position);
                callingItem = view;
                Log.i("button:", selectedItem);
                popupMenuExample(selectedItem);
            }
        });
    }

    public void goBackHome() {
        Intent intent = new Intent(ListingTracks.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityIfNeeded(intent, 0);
    }
    private void popupMenuExample(String selectedItem) {
        PopupMenu p = new PopupMenu(this, callingItem);
        p.getMenuInflater().inflate(R.menu.popup_menu_example, p .getMenu());
        p.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {

                File dir = getFilesDir();
                File file = new File(dir, selectedItem);
                switch (String.valueOf(item)) {
                    case "Track löschen":
                        file.delete();
                        finish();
                        overridePendingTransition(10, 10);
                        startActivity(getIntent());
                        overridePendingTransition(10, 10);
                        break;
                    case "Track bearbeiten":
                       File newName=new File(dir,"UPDATED_FILE_"+selectedItem);
                       file.renameTo(newName);
                       //  recreate();
                       finish();
                       overridePendingTransition(10, 10);
                       startActivity(getIntent());
                       overridePendingTransition(10, 10);
                       break;
                    case "Track auf Karte anzeigen":
                        Intent intent = new Intent();
                        intent.putExtra("optionClicked", String.valueOf(item));
                        intent.putExtra("fileName", selectedItem);

                        setResult(Activity.RESULT_OK, intent);
                        finish();
                        break;
                }
                Log.i("menu:", String.valueOf(item));
                return true;
            }
        });
        p.show();
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
                    .url("http://aleksandrpronin.pythonanywhere.com/upload")
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
                //.url("http://ip:5000/download/"+filename)
                .url("http://aleksandrpronin.pythonanywhere.com/download/"+filename)
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

    public void synchronisation() throws IOException {
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
                    if (filesListFromFlask.size() == 0){
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

                for(String el:localeFiles){
                    file = new File(dir, el);
                    uploadFile(file);
                    System.out.println("Locale Files zum upload "+ localeFiles);
                }
            }
        });
    }
}
