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
                popupMenuExample();
            }
        });
    }

    public void goBackHome() {
        Intent intent = new Intent(ListingTracks.this, MainActivity.class);
        startActivity(intent);
    }
    private void popupMenuExample() {
        PopupMenu p = new PopupMenu(this, callingItem);
        p.getMenuInflater().inflate(R.menu.popup_menu_example, p .getMenu());
        p.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                Log.i("menu:", "test" );
                return true;
            }
        });
        p.show();
    }
}