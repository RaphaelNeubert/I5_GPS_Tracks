package com.example.gps_tracks;

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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

    private Button back;
    String[] testdata = {"Neuer Track 05.11.2022 12:15 Uhr","Neuer Track 22.08.2022 18:25 Uhr",
                         "Runde durch den Park","Neuer Track 20.04.2022 03:43 Uhr",
                         "Neuer Track 20.04.2022 10:14 Uhr"};

    //String[] instrumentFileList = ListingTracks.this.fileList();
    ListView listView;
    View callingItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
        // new File
        String filename = "myfile.txt";
        String string = "Hello world!";
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(string.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        */

        File dir = getFilesDir();
        File file = new File(dir, "myfile.txt");

        uploadFile("http://192.168.178.46:5000/upload",file);
        try {
            downloadFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

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

        //listing
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
                }
                Log.i("menu:", String.valueOf(item));
                return true;
            }
        });
        p.show();
    }

    public static Boolean uploadFile(String serverURL, File file) {
        OkHttpClient okHttpClient = new OkHttpClient();

        try {
            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getName(),
                            RequestBody.create(MediaType.parse("text/csv"), file))
                    .build();

            Request request = new Request.Builder()
                    .url(serverURL)
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

    public void downloadFile() throws IOException {

        OkHttpClient client = new OkHttpClient();


        Request request = new Request.Builder()
                .url("http://192.168.178.46:5000/download/myfile.txt")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
            }

            @Override
            public void onResponse(final Call call, Response response) throws IOException {
                Log.d("TAG",response.body().string());
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to download file: " + response);
                }
                FileOutputStream outputStream;
                try {
                    outputStream = openFileOutput("tmp.txt", Context.MODE_PRIVATE);
                    outputStream.write(response.body().bytes());
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
