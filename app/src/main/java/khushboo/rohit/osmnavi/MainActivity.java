package khushboo.rohit.osmnavi;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.ContactsContract;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import org.osmdroid.bonuspack.utils.HttpConnection;
import org.osmdroid.util.GeoPoint;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static java.lang.Math.abs;


public class MainActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final int REQ_CODE_SPEECH_INPUT = 3;
    private static final int REQ_CODE_LOAD_ROUTE = 4;
    public static final int NAV_TYPE_LOAD_ROUTE = 2;
    public static final int NAV_TYPE_NAVIGATE = 0;
    public static final int NAV_TYPE_PREPLAN = 1;
    private static final String API_KEY = "AIzaSyBsq96XJwY6_aCw6uB-00R0EfPIJ0CWs0k";
    private static final String OSM_EDITING_URL = "https://api.openstreetmap.org/api/0.6//map?bbox=";
    private static final int RESULT_PICK_CONTACT = 1;
    private static final int REQ_CODE_PICK_CONTACT = 5;
    private String TAG = MainActivity.class.getSimpleName();
    private LinearLayout myLayout = null;
    MyItemizedOverlay myItemizedOverlay = null;

    SQLiteDatabase db;
    MyApp app;

    private MediaRecorder myAudioRecorder;
    private String outputFile = null;
    boolean isNavigating = false;
    boolean isPreviewing = false;
    boolean isSelected = false;
    boolean isDebug = false;

    Button startVoiceRecording,stopVoiceRecording;
    Button startButton, saveButton;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    Location mLastLocation;
    AutocompleteSupportFragment endingDestination;
    TextToSpeech tts;

    Place destinationLatLng;
    View endingDestinationView;
    double currentLat, currentLong;



    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (PackageManager.PERMISSION_GRANTED !=
                checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1339);
        }
        if (PackageManager.PERMISSION_GRANTED !=
                checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1340);
        }
        if (PackageManager.PERMISSION_GRANTED !=
                checkSelfPermission(Manifest.permission.RECORD_AUDIO)) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1341);
        }

        IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
        MediaButtonIntentReceiver r = new MediaButtonIntentReceiver();
        filter.setPriority(10000); //this line sets receiver priority
        registerReceiver(r, filter);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.activity_main);
        startButton = (Button) findViewById(R.id.button8);
        saveButton = (Button) findViewById(R.id.button_save);

        tts = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // TODO Auto-generated method stub
                if (status == TextToSpeech.SUCCESS) {
//                    int result=tts.setLanguage(Locale.US);
//                    if(result==TextToSpeech.LANG_MISSING_DATA ||
//                            result==TextToSpeech.LANG_NOT_SUPPORTED){
//                        Log.e("error", "This Language is not supported");
//                    }
                    if (!app.hasRefreshed) {
                        System.out.println("Starting app");
                        tts.speak("Welcome to OSM Navi.", TextToSpeech.QUEUE_ADD, null);
                        app.hasRefreshed = true;
                    }
//                    else{
//                        ConvertTextToSpeech();
//                    }
                } else
                    Log.e("error", "Initialization Failed!");
            }
        });

        buildGoogleApiClient();

        app = (MyApp) this.getApplicationContext();
        db = app.myDb;
        db.execSQL("CREATE TABLE IF NOT EXISTS myLocation(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lat INT,long INT,description VARCHAR, timestamp INT, prev_id INT, next_id INT );");
        db.execSQL("CREATE TABLE IF NOT EXISTS myTags(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lat INT,long INT, tag VARCHAR );");
        db.execSQL("CREATE TABLE IF NOT EXISTS locationByTag(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, tag_id INTEGER, location_id INTEGER);");
        db.execSQL("CREATE TABLE IF NOT EXISTS trackData(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lat DOUBLE, long DOUBLE, type INT, timestamp INT );");
        db.execSQL("CREATE TABLE IF NOT EXISTS trackData30(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lat DOUBLE, long DOUBLE, type INT, timestamp INT );");
        db.execSQL("CREATE TABLE IF NOT EXISTS routes(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name VARCHAR, distance VARCHAR, start_lat INT, start_lon INT, end_lat INT, end_lon INT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS routebyinstructions(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, routeid INTEGER, lat INT, long INT, description VARCHAR);");
        db.execSQL("CREATE TABLE IF NOT EXISTS tagroutebyinstructions(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, routeid INTEGER, lat INT, long INT, description VARCHAR);");


        String apiKey;
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), API_KEY);
            PlacesClient placesClient = Places.createClient(this);
        }

        endingDestination = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.ending_destination_2);
        endingDestination.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));
        endingDestinationView = endingDestination.getView();
        endingDestination.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(Place place) {
                    isSelected = true;
                    // TODO: Get info about the selected place.
                    destinationLatLng = place;
                    Log.i("endingDestination ", "Place: " + place.getName());
                    tts.speak("Destination successfully set to " + place.getName(), TextToSpeech.QUEUE_ADD, null);
                }
                @Override
                public void onError(Status status) {
                    // TODO: Handle the error.
                    Log.i("endingDestination ", "An error occurred: " + status);
                    tts.speak("There was an error setting up the destination. Please try again.", TextToSpeech.QUEUE_ADD, null);
                }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mGoogleApiClient!=null){
            if(!mGoogleApiClient.isConnected()){
                mGoogleApiClient.connect();
            }
        }
        if(mGoogleApiClient==null){
            buildGoogleApiClient();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGoogleApiClient.disconnect();

    }
    @Override
    protected void onResume() {
        super.onResume();
        if(mGoogleApiClient!=null){
            if(!mGoogleApiClient.isConnected()){
                mGoogleApiClient.connect();
            }
        }
        if(mGoogleApiClient==null){
            buildGoogleApiClient();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mGoogleApiClient!=null){

            if(mGoogleApiClient.isConnected()){

                mGoogleApiClient.disconnect();
            }

        }
        while(tts.isSpeaking()){}
        tts.stop();

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> result = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                endingDestination.setText(result.get(0));
            }
        } else if (requestCode == REQ_CODE_LOAD_ROUTE) {
            if (resultCode == RESULT_OK) {
                int selectedRoute = data.getIntExtra("selectedRoute", 0);
                String navigatingDistance = data.getStringExtra("selectedRouteDistance");
                String routeName = data.getStringExtra("selectedRouteName");
                Intent intentNA = new Intent(getBaseContext(), NavigationActivity.class);
                intentNA.putExtra("Type", NAV_TYPE_LOAD_ROUTE);
                intentNA.putExtra("selectedRoute", selectedRoute);
                intentNA.putExtra("selectedRouteDistance", navigatingDistance);
                intentNA.putExtra("selectedRouteName", routeName);
                isNavigating = true;
                startActivity(intentNA);
            }
        }
    }

    public void promptSpeechInputView(View view) {
        promptSpeechInput();
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            if(!isNavigating) {
                startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
            }
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }


    public void changeLayout(View view) {
        Intent i = new Intent(getBaseContext(), AddButton.class);
        startActivityForResult(i, 1);
    }

    synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    public void setAudio(View view) {
        System.out.println(Environment.getExternalStorageDirectory().getAbsolutePath());
        outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.3gp";
        ;

        myAudioRecorder = new MediaRecorder();
        myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);

        myAudioRecorder.setOutputFile(outputFile);

        setContentView(R.layout.record_view);
        startVoiceRecording = (Button) findViewById(R.id.button6);
        stopVoiceRecording = (Button) findViewById(R.id.button7);
        startVoiceRecording.setEnabled(true);
        stopVoiceRecording.setEnabled(false);

    }

    public void start_audio(View view) {
        try {
            myAudioRecorder.prepare();
            myAudioRecorder.start();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (java.io.IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        stopVoiceRecording.setEnabled(true);
        startVoiceRecording.setEnabled(false);
        Toast.makeText(getApplicationContext(), "Recording started", Toast.LENGTH_LONG).show();
    }

    public void stop_audio(View view) {
        myAudioRecorder.stop();
        myAudioRecorder.release();
        myAudioRecorder = null;

        stopVoiceRecording.setEnabled(false);
        startVoiceRecording.setEnabled(false);

        Toast.makeText(getApplicationContext(), "Audio recorded successfully", Toast.LENGTH_LONG).show();


    }


    public void onImageButton(View view) {
        Intent i = new Intent(getBaseContext(), ImageActivity.class);
        startActivity(i);
    }

    public void whereAmI(View view){
        Address obj = getAddress(currentLat, currentLong);
        String fin_add = getNameFromAddress(obj, 4);
        if(fin_add == null)
            tts.speak("Could Not find location info", TextToSpeech.QUEUE_ADD, null);
        else
            tts.speak("You are at " + fin_add, TextToSpeech.QUEUE_ADD, null);

        System.out.println(fin_add);
    }

    public void shareloc(View view){

        Intent myIntent = new Intent(MainActivity.this, shareLocActivity.class);
        myIntent.putExtra("currentLat", currentLat);
        myIntent.putExtra("currentLong", currentLong);
        MainActivity.this.startActivity(myIntent);
    }

    public void onspoi(View view){

        Intent myIntent = new Intent(MainActivity.this, SearchPOI.class);
        myIntent.putExtra("currentLat", currentLat);
        myIntent.putExtra("currentLong", currentLong);
        MainActivity.this.startActivity(myIntent);
    }

    public String getNameFromAddress(Address obj, int ex){
        String add = "";
        String fin_add = "";
        for (int i = 0; i<= obj.getMaxAddressLineIndex(); i++){
            add = add + obj.getAddressLine(i);
            add = add + ", ";
        }
        int delimitCnt=0;
        for (int j = 0; j < add.length(); j++){
            if(add.charAt(j) == ',')
                delimitCnt++;
            if (delimitCnt == ex)
                break;
            fin_add = fin_add + add.charAt(j);
        }
        return fin_add;
    }

    public Address getAddress(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        Address obj = null;
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng,  1);
            obj = addresses.get(0);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return obj;
    }

    public void onStartButton(View view) {
        if (!isSelected) {
            tts.speak("Destination not set", TextToSpeech.QUEUE_ADD, null);
            Toast.makeText(getApplicationContext(), "Destination not set!", Toast.LENGTH_LONG).show();
            return;
        }
        else {
            double final_lat = destinationLatLng.getLatLng().latitude;
            double final_long = destinationLatLng.getLatLng().longitude;
            CharSequence destinationAddress = destinationLatLng.getName();

            Intent intentNA = new Intent(getBaseContext(), NavigationActivity.class);
            intentNA.putExtra("Type", NAV_TYPE_NAVIGATE);
            intentNA.putExtra("startLat", currentLat);
            intentNA.putExtra("startLon", currentLong);
            intentNA.putExtra("endLat", final_lat);
            intentNA.putExtra("endLon", final_long);
            intentNA.putExtra("destination", destinationAddress);

            isNavigating = true;
            startActivity(intentNA);
        }
    }

    public void onPrePlanButton(View view) {
        if (!isSelected) {
            tts.speak("Destination not set", TextToSpeech.QUEUE_ADD, null);
            Toast.makeText(getApplicationContext(), "Destination not set!", Toast.LENGTH_LONG).show();
            return;
        }
        else {

            double final_lat = destinationLatLng.getLatLng().latitude;
            double final_long = destinationLatLng.getLatLng().longitude;
            CharSequence destinationAddress = destinationLatLng.getName();

            Intent intentNA = new Intent(getBaseContext(), NavigationActivity.class);
            intentNA.putExtra("Type", NAV_TYPE_PREPLAN);
            intentNA.putExtra("startLat", currentLat);
            intentNA.putExtra("startLon", currentLong);
            intentNA.putExtra("endLat", final_lat);
            intentNA.putExtra("endLon", final_long);
            intentNA.putExtra("destination", destinationAddress);

            isNavigating = true;
            startActivity(intentNA);
        }
    }

    public void onLoadRouteButton(View view) {

        Intent i = new Intent(getBaseContext(), ShowRoutes.class);
        ArrayList<Integer> route_ids;
        route_ids = new ArrayList<Integer>();
        ArrayList<String> route_names;
        route_names = new ArrayList<String>();
        ArrayList<String> route_distances;
        route_distances = new ArrayList<String>();
        Cursor c = db.rawQuery("SELECT * FROM routes;", null);
        while (c.moveToNext()) {
            route_ids.add(Integer.parseInt(c.getString(0)));
            route_names.add(c.getString(1));
            route_distances.add(c.getString(2));
        }
        i.putExtra("route_ids", route_ids);
        i.putExtra("route_names", route_names);
        i.putExtra("route_distances", route_distances);
        startActivityForResult(i, REQ_CODE_LOAD_ROUTE);

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(500); // Update location twice every second

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            currentLat = mLastLocation.getLatitude();
            currentLong = mLastLocation.getLongitude();
            Log.i("current_lat_lng","Fetching Current LatLong  " + currentLat + ";" + currentLong);
        }

        setBoundsBias();
        Log.i("current_lat_lng","Connected!");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        buildGoogleApiClient();
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLat = location.getLatitude();
        currentLong = location.getLongitude();
    }



    public void onDebug(View view){
        Intent myIntent = new Intent(MainActivity.this, debugAcitivity.class);
        myIntent.putExtra("currentLat", currentLat);
        myIntent.putExtra("currentLong", currentLong);
        MainActivity.this.startActivity(myIntent);
    }

    public void setBoundsBias(){
        /*LatLng southWestBound = new LatLng( currentLat - 0.1, currentLong - 0.1);
        LatLng northEastBound = new LatLng(currentLat + 0.1, currentLong + 0.1);
        LatLngBounds customBounds = new LatLngBounds(southWestBound, northEastBound);
        endingDestination.setLocationBias(customBounds);
        AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                .setTypeFilter(AutocompleteFilter.TYPE_FILTER_NONE)
                .build();
        endingDestination.setFilter(typeFilter);*/
    }


    public static String requestStringFromUrl(String url, String userAgent) {
        HttpConnection connection = new HttpConnection();
        if (userAgent != null)
            connection.setUserAgent(userAgent);
        connection.doGet(url);
        String result = connection.getContentAsString();
        connection.close();
        return result;
    }

    /** sends an http request, and returns the whole content result in a String.
     * @param url
     * @return the whole content, or null if any issue.
     */
    public static String requestStringFromUrl(String url) {
        return requestStringFromUrl(url, null);
    }
}