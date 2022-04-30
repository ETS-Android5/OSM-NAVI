package khushboo.rohit.osmnavi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.ContactsContract;
import android.speech.RecognizerIntent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.net.URLEncoder;
import java.util.ArrayList;

public class shareLocActivity extends AppCompatActivity {

    private static final int REQ_CODE_PICK_CONTACT = 5;
    double currentLat, currentLong;
    ToDoCursorAdapter todoAdapter;
    MyApp app;
    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        currentLat = getIntent().getDoubleExtra("currentLat", 0);
        currentLong = getIntent().getDoubleExtra("currentLong",0);


        app = (MyApp) this.getApplicationContext();
        db = app.myDb;
        db.execSQL("CREATE TABLE IF NOT EXISTS myContacts(_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name VARCHAR, phone VARCHAR );");

        setContentView(R.layout.activity_share_loc);
        Cursor todoCursor = db.rawQuery("SELECT * FROM myContacts", null);
        ListView lvItems = (ListView) findViewById(R.id.conlist_view);
        lvItems.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                System.out.println(position);
                sendloc(position);
            }
        });

        todoAdapter = new ToDoCursorAdapter(this, todoCursor);
        lvItems.setAdapter(todoAdapter);
        viewList();
    }


    public void viewList(){ /*

        Cursor c = db.rawQuery("SELECT * FROM myContacts", null);
        StringBuffer buffer = new StringBuffer();
        TextView tv2 = (TextView) findViewById(R.id.textViewshareLoc);

        if (c.getCount() == 0) {
            System.out.println("No Contacts found");
            tv2.setText("No Contacts Added");
            Toast.makeText(getApplicationContext(), "No Database Found", Toast.LENGTH_LONG).show();
            return;
        }

        if ((c.getCount() != 0)) buffer.append("S A V E D   C O N T A C T S : " + "\n" + "\n");
        while (c.moveToNext()) {
            buffer.append("Name: " + c.getString(1) + "\n");
            buffer.append("Phone No.: " + c.getString(2) + "\n" + "\n");
        }
        tv2.setText(buffer);*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_PICK_CONTACT) {
            if (resultCode == RESULT_OK) {
                Cursor cursor = null;
                try {

                    String phoneNo = null;
                    String name = null;
                    Uri uri = data.getData();
                    cursor = getContentResolver().query(uri, null, null, null, null);
                    cursor.moveToFirst();
                    int phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    phoneNo = cursor.getString(phoneIndex);
                    phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    name = cursor.getString(phoneIndex);

                    db.execSQL("INSERT INTO myContacts VALUES(NULL, '" + name + "','" +
                            phoneNo + "');");

                    Cursor todoCursor = db.rawQuery("SELECT * FROM myContacts", null);
                    todoAdapter.changeCursor(todoCursor);
                    viewList();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

            } if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            } else {
                Log.e("Add Contact", "Failed to pick contact");
            }
        }
    }

    public void add(View view){

        Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(contactPickerIntent, REQ_CODE_PICK_CONTACT);
    }

    public void sendloc(int pos){

        Context context = this;
        PackageManager packageManager = context.getPackageManager();
        Intent i = new Intent(Intent.ACTION_VIEW);
        Cursor todoCursor = db.rawQuery("SELECT * FROM myContacts", null);
        todoCursor.moveToPosition(pos);
        String phone = todoCursor.getString(2);

        try {
            String locurl = "Hi, I am sharing my current location with you through NAVI App!" +"\n" +"\n" + "http://maps.google.com/maps?saddr=" + currentLat + "," + currentLong;
            String url = "https://api.whatsapp.com/send?phone="+ phone +"&text=" + URLEncoder.encode(locurl, "UTF-8");
            i.setPackage("com.whatsapp");
            i.setData(Uri.parse(url));
            if (i.resolveActivity(packageManager) != null) {
               context.startActivity(i);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }


    public void backto(View view){
        super.onBackPressed();
    }

    public void clearcon(View view){
        db.execSQL("DELETE FROM myContacts;");
        Toast.makeText(this, "Contacts Cleared", Toast.LENGTH_SHORT).show();
        Cursor todoCursor = db.rawQuery("SELECT * FROM myContacts", null);
        todoAdapter.changeCursor(todoCursor);
        viewList();
    }


}
