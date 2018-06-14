package khushboo.rohit.osmnavi;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.osmdroid.bonuspack.location.POI;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.location.OverpassAPIProvider;
import org.osmdroid.bonuspack.utils.HttpConnection;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.location.places.ui.SupportPlaceAutocompleteFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.vision.face.Landmark;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static java.lang.Math.abs;


public class MainActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final int REQ_CODE_SPEECH_INPUT = 3;
    private static final String OSM_EDITING_URL = "http://api.openstreetmap.org/api/0.6//map?bbox=";
    private LinearLayout myLayout = null;
    MyItemizedOverlay myItemizedOverlay = null;
    SQLiteDatabase db;
    private MediaRecorder myAudioRecorder;
    private String outputFile = null;
    boolean isNavigating = false;
    boolean isPreviewing = false;
    boolean faceHeadDirection = false;
    boolean part1 = false, part2 = false, part3 = false, part4 = false;
    long time_diff = 60 * 1000;
    Handler h = new Handler();
    int delay = 10; //milliseconds
    int instid=0;
    int osmNumInstructions, osmNextInstruction, prefetch_nextInstruction, Instruction_progressTracker;
    Button start, stop;
    Button button, save_button;
    String navigatingDistance;
    TextToSpeech tts;
    int prev_id = 0;
    double headingDirection = 0;
    double currentDegree = 0;
    ArrayList<GeoPoint> landmarks;
    ArrayList<GeoPoint> saved_landmarks;
    ArrayList<GeoPoint> tags;
    ArrayList<GeoPoint> poi_tags;
    ArrayList<String> instructions;
    ArrayList<String> saved_instructions;
    ArrayList<String> tagInstructions;
    ArrayList<Integer> tagCheck;
    ArrayList<Integer> saved_landmarks_check;
    ArrayList<Integer> tagLandmark;
    ArrayList<String> poi_tagInstructions;
    ArrayList<Long> timestamps;
    Long dir_timestamp;
    int pathLength;
    GeoPoint endPoint,startPoint;
    OSRMRoadManager roadManager;
    Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    PlaceAutocompleteFragment endingDestination;
    Place destinationLatLng;
    boolean isSelected = false;
    GPSTracker gps;
    boolean isDebug = false;
    MyApp app;
    long turn_timestamp;
    double current_lat, current_long, previous_bearing;
    GeoPoint previous_location;
    Road road;
    private Compass compass;
    private String TAG = MainActivity.class.getSimpleName();
    private ProgressBar mProgressBar;
    private int mProgressStatus = 0;
    private Handler mHandler = new Handler();
    FragmentManager fm = getSupportFragmentManager();

    int i = 100;
    double final_lat = 0;
    double final_long = 0;
    double init_lat = 0;
    double init_long = 0;
    int x=0,y=0,z=0,w=0;

    float[] ones = new float[1];

    float[] ones1 = new float[1];
    // added by sac
    OverpassAPIProvider2 overpassProvider;
    Map<Long, String> tagdescriptions;
    ArrayList<Long> tst; // timestamp for pre scheduling of future instructions, filling the empty spaces

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // System.out.println("onCreate function called");
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
        IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);//"android.intent.action.MEDIA_BUTTON"
        MediaButtonIntentReceiver r = new MediaButtonIntentReceiver();
        filter.setPriority(10000); //this line sets receiver priority
        registerReceiver(r, filter);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        landmarks = new ArrayList<GeoPoint>();
        instructions = new ArrayList<String>();
        saved_landmarks = new ArrayList<GeoPoint>();
        saved_landmarks_check = new ArrayList<Integer>();
        saved_instructions = new ArrayList<String>();
        tags = new ArrayList<GeoPoint>();
        tagInstructions = new ArrayList<String>();
        tagCheck = new ArrayList<Integer>();
        tagLandmark = new ArrayList<Integer>();
        poi_tags = new ArrayList<GeoPoint>();
        poi_tagInstructions = new ArrayList<String>();
        timestamps = new ArrayList<Long>();
        tst = new ArrayList<>();
//        MapView map = (MapView) findViewById(R.id.map);
//        map.setTileSource(TileSourceFactory.MAPNIK);
//        map.setBuiltInZoomControls(true);
//        map.setMultiTouchControls(true);

//        GeoPoint startPoint = new GeoPoint(28.544837,77.194259);
//        IMapController mapController = map.getController();
//        mapController.setZoom(9);
//        mapController.setCenter(startPoint);
        roadManager = new OSRMRoadManager(this);
//        ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
//        waypoints.add(startPoint);
//        GeoPoint endPoint = new GeoPoint(28.545213,77.192219);
//        waypoints.add(endPoint);
//        roadManager.addRequestOption("routeType=pedestrian");
//        Road road = roadManager.getRoad(waypoints);
//        Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
//        map.getOverlays().add(roadOverlay);
//        for (int i=0; i<road.mNodes.size(); i++){
//            System.out.println(road.mNodes.get(i).mLocation.getLatitude() + ", " + road.mNodes.get(i).mLocation.getLongitude());
//            System.out.println(road.mNodes.get(i).mInstructions);
//        }
//        map.invalidate();

        setContentView(R.layout.activity_main);
        button = (Button) findViewById(R.id.button8);
        save_button = (Button) findViewById(R.id.button_save);
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

//        gps = new GPSTracker(this);
        compass = new Compass(this);
            buildGoogleApiClient();
            app = (MyApp) this.getApplicationContext();
            db = app.myDb;
        db.execSQL("CREATE TABLE IF NOT EXISTS myLocation(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lat INT,long INT,description VARCHAR, timestamp INT, prev_id INT, next_id INT );");
        db.execSQL("CREATE TABLE IF NOT EXISTS myTags(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lat INT,long INT, tag VARCHAR );");
        db.execSQL("CREATE TABLE IF NOT EXISTS locationByTag(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, tag_id INTEGER, location_id INTEGER);");
        db.execSQL("CREATE TABLE IF NOT EXISTS trackData(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lat INT, long INT, type INT, timestamp INT );");
        db.execSQL("CREATE TABLE IF NOT EXISTS routes(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name VARCHAR, distance VARCHAR, start_lat INT, start_lon INT, end_lat INT, end_lon INT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS routebyinstructions(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, routeid INTEGER, lat INT, long INT, description VARCHAR);");
        db.execSQL("CREATE TABLE IF NOT EXISTS tagroutebyinstructions(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, routeid INTEGER, lat INT, long INT, description VARCHAR);");

        endingDestination = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.ending_destination_2);
//        LatLng southWestBound = new LatLng(7.597576, 67.345201);
//        LatLng northEastBound = new LatLng(38.733380, 96.964342);
//        LatLngBounds indiaBounds = new LatLngBounds(southWestBound, northEastBound)
        endingDestination.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(Place place) {
                    isSelected = true;
                    // TODO: Get info about the selected place.
                    destinationLatLng = place;
                    Log.i("endingDestination ", "Place: " + place.getName());
                    tts.speak("Destination successfully set to " + place.getName() + ". Press Middle button to start navigating. Press bottom button for route preview", TextToSpeech.QUEUE_ADD, null);
                }
                @Override
                public void onError(Status status) {
                    // TODO: Handle the error.
                    Log.i("endingDestination ", "An error occurred: " + status);
                    tts.speak("There was an error setting up the destination. Please try again.", TextToSpeech.QUEUE_ADD, null);
                }
        });

        h.postDelayed(new Runnable() {
            public void run() {
                getLocalInfo();
                h.postDelayed(this, delay);
            }
        }, delay);


    }

    public void promptSpeechInputView(View view) {
        // System.out.println("promptSpeechInputView function called");
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
            else{
                startActivityForResult(intent, 5);
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

    @Override
    protected void onStart() {

        super.onStart();
        compass.start();
    }

    @Override
    protected void onDestroy() {
        tts.shutdown();
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // System.out.println("onActivityResult function called");
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                String myDescription = data.getStringExtra("result");
                boolean[] tags = data.getBooleanArrayExtra("tags");
                double lat_float = current_lat;
                double long_float = current_long;
                int lat_int = (int) (lat_float * 10000000);
                int long_int = (int) (long_float * 10000000);


                int new_row_id;
                if (prev_id > 0) {
                    Cursor c = db.rawQuery("SELECT * from myLocation where id = '" + prev_id + "'", null);
                    c.moveToFirst();
                    int next_id;
                    next_id = Integer.parseInt(c.getString(6)); // The next id of prev, if present
                    c.close();

                    // update the next id of new point with this, and update the next id of previous one. If next id was 0, it will remain 0
                    db.execSQL("INSERT INTO myLocation VALUES(NULL, '" + lat_int + "','" +
                            long_int + "','" + myDescription + "','" + System.currentTimeMillis() +
                            "','" + prev_id + "','" + next_id + "');");
                    Cursor c_new = db.rawQuery("SELECT last_insert_rowid()", null);
                    c_new.moveToFirst();
                    new_row_id = Integer.parseInt(c_new.getString(0));
                    c_new.close();
                    db.execSQL("UPDATE myLocation SET next_id = '" + new_row_id + "' WHERE id = '" + prev_id + "'");
                } else {
                    db.execSQL("INSERT INTO myLocation VALUES(NULL, '" + lat_int + "','" +
                            long_int + "','" + myDescription + "','" + System.currentTimeMillis() +
                            "','" + prev_id + "','0');");
                    Cursor c_new = db.rawQuery("SELECT last_insert_rowid()", null);
                    c_new.moveToFirst();
                    new_row_id = Integer.parseInt(c_new.getString(0));
                    c_new.close();
                }
                prev_id = new_row_id;
                for (int i = 0; i < tags.length; i++) {
                    if (tags[i]) {
                        db.execSQL("INSERT INTO locationByTag VALUES(NULL, " + (i + 1) + ", " + prev_id + ");");
                    }
                }
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        } else if (requestCode == 2) {                              // request from
            if (resultCode == RESULT_OK) {
                String routeName = data.getStringExtra("name");
                int start_lat = (int) (startPoint.getLatitude()*10000000);
                int start_lon = (int) (startPoint.getLongitude()*10000000);
                int end_lat = (int) (endPoint.getLatitude()*10000000);
                int end_lon = (int) (endPoint.getLongitude()*10000000);
                System.out.println(start_lat);
                System.out.println(start_lon);
                System.out.println(end_lat);
                System.out.println(end_lon);
                db.execSQL("INSERT INTO routes VALUES(NULL, '" + routeName + "', '" + navigatingDistance +  "','" + start_lat + "','" + start_lon + "','" + end_lat + "','" + end_lon + "');");
                Cursor c_new = db.rawQuery("SELECT last_insert_rowid()", null);
                c_new.moveToFirst();
                int new_row_id = Integer.parseInt(c_new.getString(0));
                c_new.close();
                for (int i = 0; i < instructions.size(); i++) {
                    String instruction = instructions.get(i);
                    double lat_float = landmarks.get(i).getLatitude();
                    int lat_int = (int) (lat_float * 10000000);
                    double long_float = landmarks.get(i).getLongitude();
                    int long_int = (int) (long_float * 10000000);
                    db.execSQL("INSERT INTO routebyinstructions VALUES(NULL, '" + new_row_id + "','" +
                            lat_int + "','" + long_int + "','" + instruction + "');");
                }
                for (int i = 0; i < tagInstructions.size(); i++) {
                    String tagInstruction = tagInstructions.get(i);
                    double lat_float = tags.get(i).getLatitude();
                    int lat_int = (int) (lat_float * 10000000);
                    double long_float = tags.get(i).getLatitude();
                    int long_int = (int) (long_float * 10000000);
                    db.execSQL("INSERT INTO tagroutebyinstructions VALUES(NULL, '" + new_row_id + "','" +
                            lat_int + "','" + long_int + "','" + tagInstruction + "');");
                }
            }
        } else if (requestCode == REQ_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> result = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                endingDestination.setText(result.get(0));
            }
        }else if (requestCode == 5) {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> result = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String poi_search = result.get(0);
                searchPOIfromName(poi_search);
            }
        } else if (requestCode == 4) {
            if (resultCode == RESULT_OK) {
                int selectedRoute = data.getIntExtra("selectedRoute", 0);
                navigatingDistance = data.getStringExtra("selectedRouteDistance");
                String routeName = data.getStringExtra("selectedRouteName");
                Cursor ec = db.rawQuery("SELECT * FROM routes where name = '" + routeName + "';", null);
                ec.moveToFirst();
                double start_lat = (ec.getInt(3)) / 10000000.0;
                double start_lon = (ec.getInt(4)) / 10000000.0;
                double end_lat = (ec.getInt(5)) / 10000000.0;
                double end_lon = (ec.getInt(6)) / 10000000.0;
                startPoint = new GeoPoint(start_lat,start_lon);
                endPoint = new GeoPoint(end_lat, end_lon);
                Cursor c = db.rawQuery("SELECT * FROM routebyinstructions where routeid = " + selectedRoute + ";", null);
                double max_latitude = current_lat;
                double min_latitude = current_lat;
                double max_longitude = current_long;
                double min_longitude = current_long;
                while (c.moveToNext()) {
                    double latitude = Double.parseDouble(c.getString(2)) / 10000000;
                    double longitude = Double.parseDouble(c.getString(3)) / 10000000;
                    String description = c.getString(4);
                    landmarks.add(new GeoPoint(latitude, longitude));
                    instructions.add(description);
                    timestamps.add(new Long(0));
                    tst.add(new Long(0));
                    if (latitude > max_latitude) {
                        max_latitude = latitude;
                    }
                    if (latitude < min_latitude) {
                        min_latitude = latitude;
                    }
                    if (longitude > max_longitude) {
                        max_longitude = longitude;
                    }
                    if (longitude < min_longitude) {
                        min_longitude = longitude;
                    }
                }
                Cursor d = db.rawQuery("SELECT * FROM tagroutebyinstructions where routeid = " + selectedRoute + ";", null);
                double tag_max_latitude = current_lat;
                double tag_min_latitude = current_lat;
                double tag_max_longitude = current_long;
                double tag_min_longitude = current_long;
                while (d.moveToNext()) {
                    double latitude = Double.parseDouble(d.getString(2)) / 10000000;
                    double longitude = Double.parseDouble(d.getString(3)) / 10000000;
                    String description = d.getString(4);
                    tags.add(new GeoPoint(latitude, longitude));
                    tagInstructions.add(description);
                    tagCheck.add(0);
                    if (latitude > max_latitude) {
                        max_latitude = latitude;
                    }
                    if (latitude < min_latitude) {
                        min_latitude = latitude;
                    }
                    if (longitude > max_longitude) {
                        max_longitude = longitude;
                    }
                    if (longitude < min_longitude) {
                        min_longitude = longitude;
                    }
                }
//               Cursor c = db.rawQuery("SELECT * FROM myLocation", null);
                Cursor e = db.rawQuery("SELECT * FROM myLocation", null);
                while (e.moveToNext()) {
                    double latitude = Double.parseDouble(e.getString(1)) / 10000000;
                    double longitude = Double.parseDouble(e.getString(2)) / 10000000;
                    String description = e.getString(3);
                    saved_landmarks.add(new GeoPoint(latitude, longitude));
                    saved_instructions.add(description);
                    saved_landmarks_check.add(0);
                }
                GeoPoint startPoint = new GeoPoint(current_lat, current_long);
                previous_location = startPoint;
                osmNumInstructions = landmarks.size();
                osmNextInstruction = 0;
                prefetch_nextInstruction = 0;
                isNavigating = true;
                changeNavigationLayout();
                turn_timestamp = System.currentTimeMillis();
                tts.speak("Starting Navigation. Your location is " + navigatingDistance + " away.", TextToSpeech.QUEUE_ADD, null);
                headingDirection = getBearingNext();
                System.out.println(headingDirection);
                if (headingDirection < 180 && headingDirection > 10)
                    tts.speak("Turn " + (int) headingDirection + " degree Clockwise.", TextToSpeech.QUEUE_ADD, null);
                else if(headingDirection >= 180 && headingDirection < 350)
                    tts.speak("Turn " + (int) (360 - headingDirection) + " degrees Anti-Clockwise.", TextToSpeech.QUEUE_ADD, null);

            }
        }
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
        start = (Button) findViewById(R.id.button6);
        stop = (Button) findViewById(R.id.button7);
        start.setEnabled(true);
        stop.setEnabled(false);

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

        stop.setEnabled(true);
        start.setEnabled(false);
        Toast.makeText(getApplicationContext(), "Recording started", Toast.LENGTH_LONG).show();
    }

    public void stop_audio(View view) {
        myAudioRecorder.stop();
        myAudioRecorder.release();
        myAudioRecorder = null;

        stop.setEnabled(false);
        start.setEnabled(false);

        Toast.makeText(getApplicationContext(), "Audio recorded successfully", Toast.LENGTH_LONG).show();


    }

    public void onSaveButton(View view) {
        // System.out.println("onSaveButton function called");
        if (isNavigating) {
            Intent i = new Intent(getBaseContext(), SaveRoute.class);
            startActivityForResult(i, 2);
        } else {
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
            startActivityForResult(i, 4);
        }
    }

    public void onStartButton(View view) {
        // System.out.println("onStartButton function called");
        if (!isNavigating) {
//            EditText edit =  (EditText) findViewById(R.id.editText2);
//            String destination = edit.getText().toString();
            if (!isSelected) {
                tts.speak("Destination not set", TextToSpeech.QUEUE_ADD, null);
                Toast.makeText(getApplicationContext(), "Destination not set!", Toast.LENGTH_LONG).show();
                return;
            }

            tts.speak(" Please wait while we search for a suitable route.", TextToSpeech.QUEUE_ADD, null);
            startPoint = new GeoPoint(current_lat, current_long);
            previous_location = startPoint;
            //            GeoPoint endPoint = new GeoPoint(Double.parseDouble(destination.split(",")[0]), Double.parseDouble(destination.split(",")[1]));
            endPoint = new GeoPoint(destinationLatLng.getLatLng().latitude, destinationLatLng.getLatLng().longitude);
            final_lat=destinationLatLng.getLatLng().latitude;
            final_long=destinationLatLng.getLatLng().longitude;
            init_lat=current_lat;
            init_long=current_long;
            ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
            waypoints.add(startPoint);
            waypoints.add(endPoint);
//            roadManager.addRequestOption("routeType=pedestrian");
            roadManager.setService("http://router.project-osrm.org/route/v1/pedestrian/");
            // roadManager.addRequestOption("alternatives=3");
            double max_latitude = current_lat;
            double min_latitude = current_lat;
            double max_longitude = current_long;
            double min_longitude = current_long;
            System.out.println("Starting point is: " + startPoint.toDoubleString());

            // original line earlier
            road = roadManager.getRoad(waypoints);
            // System.out.println("Number of roads found: " + road.length);

            // added check
            boolean roadstatusflag = true;
            if (road.mStatus != Road.STATUS_OK) {
                System.out.println("~~~~~~~THERE WAS ISSUE WITH THE STATUS OF THE ROADS~~~~~~~~");
                Toast.makeText(getApplicationContext(), "Network error!", Toast.LENGTH_LONG).show();
                tts.speak("Network Issue. Please try again!", TextToSpeech.QUEUE_ADD, null);
                roadstatusflag = false;
                return;
            }
            overpassProvider = new OverpassAPIProvider2();
            for (int i = 0; i < road.mNodes.size(); i++) {
                if (road.mNodes.get(i).mInstructions != null && !road.mNodes.get(i).mInstructions.isEmpty()) {
                    double nodelat_curr = road.mNodes.get(i).mLocation.getLatitude();
                    double nodelon_curr = road.mNodes.get(i).mLocation.getLongitude();
                    double nodelat_next = 0;
                    double nodelon_next = 0;
                    double nodelat_prev = 0;
                    double nodelon_prev = 0;
                    if (i != road.mNodes.size() - 1) {
                        nodelat_next = road.mNodes.get(i + 1).mLocation.getLatitude();
                        nodelon_next = road.mNodes.get(i + 1).mLocation.getLongitude();
                    }
                    if (landmarks.size() != 0) {
                        nodelat_prev = landmarks.get(landmarks.size() - 1).getLatitude();
                        nodelon_prev = landmarks.get(landmarks.size() - 1).getLongitude();
                    }
                    GeoPoint loc = road.mNodes.get(i).mLocation;
                    BoundingBox bb = new BoundingBox(loc.getLatitude() + 0.0002, loc.getLongitude() + 0.0002, loc.getLatitude() - 0.0002, loc.getLongitude() - 0.0002);

                    // url for highway type
                    String urlforpoirequest = overpassProvider.urlForPOISearch("\"highway\"", bb, 10, 50);
                    ArrayList<POI> points = overpassProvider.getPOIsFromUrlRoad(urlforpoirequest);
                    if (points == null)
                    {
                        System.out.println("overpass returning nothing");
                        continue;
                    }
                    if (points.size() == 0) System.out.println("Size is: " + points.size());
                    {
                        int ptr = 0;
                        if (points.size() > 1) {
                            while (points != null && ptr < points.size()) {
                                if ((points.get(ptr).mLocation.getLatitude() - nodelat_curr) / (nodelat_next - nodelat_curr) > 0) {
                                    if ((points.get(ptr).mLocation.getLongitude() - nodelon_curr) / (nodelon_next - nodelon_curr) > 0) {
                                        String typeofhighway = points.get(ptr).mType;
                                        String lanecount = points.get(ptr).mUrl;
                                        String typeofsurface = points.get(ptr).mDescription;
                                        String footway = points.get(ptr).mThumbnailPath;
                                        tags.add(points.get(ptr).mLocation);
                                        System.out.println("Tag Location " + points.get(ptr).mLocation);
                                        typeofhighway = typeofhighway.replaceAll("_", " ");
                                        typeofsurface = typeofsurface.replaceAll(",", "");
                                        if (lanecount != null && (typeofsurface != null && !typeofsurface.isEmpty())) {
                                            tagInstructions.add("This is " + typeofhighway + " with " + lanecount + " lanes and " + typeofsurface + " surface");
                                        } else if (lanecount == null && (typeofsurface != null && !typeofsurface.isEmpty())) {
                                            tagInstructions.add("This is " + typeofhighway + " with " + typeofsurface + " surface");
                                        } else if (lanecount != null && (typeofsurface == null || typeofsurface.isEmpty())) {
                                            tagInstructions.add("This is " + typeofhighway + " with " + lanecount + " lanes");
                                        } else if (lanecount == null && (typeofsurface == null || typeofsurface.isEmpty())) {
                                            tagInstructions.add("This is " + typeofhighway);
                                        }
                                        int len = (tagInstructions.size() - 1);
                                        if (footway != null) {
                                            tagInstructions.set(len, tagInstructions.get(len) + ". Presence of footway ; " + footway);
                                        }
                                        System.out.println(tagInstructions.get(len));
                                        tagCheck.add(0);
                                        if (i != 0) {
                                            tagLandmark.add(landmarks.size());
                                        } else if (i == 0) {
                                            tagLandmark.add(-1);
                                        }
                                        break;
                                    }
                                }
                                ptr++;
                            }
                        } else if (points.size() == 1) {
                            while (points != null && ptr < points.size()) {
                                String typeofhighway = points.get(ptr).mType;
                                String lanecount = points.get(ptr).mUrl;
                                String typeofsurface = points.get(ptr).mDescription;
                                String footway = points.get(ptr).mThumbnailPath;
                                tags.add(points.get(ptr).mLocation);
                                System.out.println("Tag Location " + points.get(ptr).mLocation);
                                typeofhighway = typeofhighway.replaceAll("_", " ");
                                if (lanecount != null && typeofsurface != null) {
                                    tagInstructions.add("This is " + typeofhighway + " with " + lanecount + " lanes and " + typeofsurface + " surface");
                                } else if (lanecount == null && typeofsurface != null) {
                                    tagInstructions.add("This is " + typeofhighway + " with " + typeofsurface + " surface");
                                } else if (lanecount != null && typeofsurface == null) {
                                    tagInstructions.add("This is " + typeofhighway + " with " + lanecount + " lanes");
                                } else if (lanecount == null && typeofsurface == null) {
                                    tagInstructions.add("This is " + typeofhighway);
                                }
                                int len = (tagInstructions.size() - 1);
                                if (footway != null) {
                                    tagInstructions.set(len, tagInstructions.get(len) + ". Presence of footway ; " + footway);
                                }
                                System.out.println(tagInstructions.get(len));
                                tagCheck.add(0);
                                if (i != 0) {
                                    tagLandmark.add(landmarks.size());
                                } else if (i == 0) {
                                    tagLandmark.add(-1);
                                }
                                ptr++;
                            }
                        }
                    }

                    if (i != 0)
                    {
                        landmarks.add(new GeoPoint(loc.getLatitude(), loc.getLongitude()));
                        System.out.println("Added Instruction: " + loc.getLatitude() + ", " + loc.getLongitude() + " " + road.mNodes.get(i).mInstructions);
                        instructions.add(removeUnnamed(road.mNodes.get(i).mInstructions));
                        timestamps.add(new Long(0));
                        tst.add(new Long(0));
                        osmNumInstructions = road.mNodes.size();
                        osmNextInstruction = 0;
                        prefetch_nextInstruction = 0;
                        if (loc.getLatitude() > max_latitude) {
                            max_latitude = loc.getLatitude();
                        }
                        if (loc.getLatitude() < min_latitude) {
                            min_latitude = loc.getLatitude();
                        }
                        if (loc.getLongitude() > max_longitude) {
                            max_longitude = loc.getLongitude();
                        }
                        if (loc.getLongitude() < min_longitude) {
                            min_longitude = loc.getLongitude();
                        }
                    }
                }
            }
            // System.exit(0);
            int max_latitude_int = (int) (max_latitude * 10000000);
            int max_longitude_int = (int) (max_longitude * 10000000);
            int min_latitude_int = (int) (min_latitude * 10000000);
            int min_longitude_int = (int) (min_longitude * 10000000);
//            Cursor c = db.rawQuery("SELECT * FROM myLocation WHERE lat BETWEEN " + (min_latitude_int) + " AND " + (max_latitude_int) + " AND long BETWEEN " + (min_longitude_int) + " AND " + (max_longitude_int), null);
            Cursor c = db.rawQuery("SELECT * FROM myLocation", null);
            while (c.moveToNext()) {
                double latitude = Double.parseDouble(c.getString(1)) / 10000000;
                double longitude = Double.parseDouble(c.getString(2)) / 10000000;
                String description = c.getString(3);
                saved_landmarks.add(new GeoPoint(latitude, longitude));
                saved_instructions.add(description);
                saved_landmarks_check.add(0);
                System.out.println(latitude);
                System.out.println(longitude);
                System.out.println(description);
            }

            if (roadstatusflag) {
                changeNavigationLayout();
                isNavigating = true;
                float[] temp = new float[1];
                Location.distanceBetween(current_lat,current_long,landmarks.get(0).getLatitude(),landmarks.get(0).getLongitude(),temp);
                turn_timestamp = System.currentTimeMillis();
                navigatingDistance = distanceToStr(road.mLength);
                tts.speak("Starting Navigation. Your location is " + navigatingDistance + " away.", TextToSpeech.QUEUE_ADD, null);
                headingDirection = getBearingNext();
                System.out.println(headingDirection);
                if (headingDirection < 180 && headingDirection > 10)
                    tts.speak("Turn " + (int) headingDirection + " degree Clockwise.", TextToSpeech.QUEUE_ADD, null);
                else if(headingDirection >= 180 && headingDirection < 350)
                    tts.speak("Turn " + (int) (360 - headingDirection) + " degrees Anti-Clockwise.", TextToSpeech.QUEUE_ADD, null);

            }
        } else {
            landmarks.clear();
            tags.clear();
            instructions.clear();
            tagInstructions.clear();
            tagCheck.clear();
            tagLandmark.clear();
            timestamps.clear();
            tst.clear();
            isNavigating = false;
            faceHeadDirection = false;
            exportDb(view);
            setContentView(R.layout.activity_dummy);
        }
    }

    public void getTagAtLocation(){
        BoundingBox bb = new BoundingBox(current_lat + 0.0002, current_long + 0.0002, current_lat - 0.0002, current_long - 0.0002);

        // url for highway type
        OverpassAPIProvider2 overpassProvider2;
        overpassProvider2 = new OverpassAPIProvider2();
        String urlforpoirequest = overpassProvider2.urlForPOISearch("\"highway\"", bb, 10, 50);
        ArrayList<POI> points2 = overpassProvider2.getPOIsFromUrlRoad(urlforpoirequest);
        if (points2.size() == 0)
        {
            System.out.println("overpass returning nothing");
            tts.speak("No tag present", TextToSpeech.QUEUE_ADD, null);
            return;
        }
        if (points2 != null) System.out.println("Local Tag Size is: " + points2.size());
        int ptr = 0;
        String inst = null;
        int min_dist_ptr = 0;
        float[] dist_min = new float[1];
        float[] check_dist = new float[1];
        Location.distanceBetween(current_lat, current_long, points2.get(0).mLocation.getLatitude(), points2.get(0).mLocation.getLongitude(), dist_min);

        while (points2 != null && ptr < points2.size()) {
            Location.distanceBetween(current_lat, current_long, points2.get(ptr).mLocation.getLatitude(), points2.get(ptr).mLocation.getLongitude(), check_dist);
            System.out.println(check_dist[0]);
            if (check_dist[0] < dist_min[0]) {
                min_dist_ptr = ptr;
            }
            ptr++;
        }
        System.out.println(min_dist_ptr);
//            if ((points2.get(ptr).mLocation.getLatitude() - current_lat) / (landmarks.get(0).getLatitude() - current_lat) > 0) {
//                if ((points2.get(ptr).mLocation.getLongitude() - current_long) / (landmarks.get(0).getLongitude() - current_long) > 0) {
        String typeofhighway = points2.get(min_dist_ptr).mType;
        String lanecount = points2.get(min_dist_ptr).mUrl;
        String typeofsurface = points2.get(min_dist_ptr).mDescription;
        String footway = points2.get(min_dist_ptr).mThumbnailPath;
        typeofhighway = typeofhighway.replaceAll("_", " ");
        if (lanecount != null && typeofsurface != null) {
            tts.speak("This is " + typeofhighway + " with " + lanecount + " lanes and " + typeofsurface + " surface", TextToSpeech.QUEUE_ADD, null);
           // inst = "This is " + typeofhighway + " with " + lanecount + " lanes and " + typeofsurface + " surface";
        } else if (lanecount == null && typeofsurface != null) {
            tts.speak("This is " + typeofhighway + " with " + typeofsurface + " surface", TextToSpeech.QUEUE_ADD, null);
            //inst = "This is " + typeofhighway + " with " + typeofsurface + " surface";
        } else if (lanecount != null && typeofsurface == null) {
            tts.speak("This is " + typeofhighway + " with " + lanecount + " lanes", TextToSpeech.QUEUE_ADD, null);
           // inst = "This is " + typeofhighway + " with " + lanecount + " lanes";
        } else if (lanecount == null && typeofsurface == null) {
            tts.speak("This is " + typeofhighway, TextToSpeech.QUEUE_ADD, null);
            //inst = "This is " + typeofhighway;
        }
        if (footway != null) {
            tts.speak(". Presence of footway ; " + footway, TextToSpeech.QUEUE_ADD, null);
            //inst = inst + ". Presence of footway ; " + footway;
        }
                    //System.out.println(inst);
//                    break;
//                }
//            }
//            ptr++;
//        }
        //tts.speak(inst, TextToSpeech.QUEUE_ADD, null);
    }

    public void showDb() {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < landmarks.size(); i++) {
            buffer.append(i + " : " + instructions.get(i) + "\n");
            buffer.append("Lat: " + landmarks.get(i).getLatitude() + " Long: " + landmarks.get(i).getLongitude());
        }
        for (int i = 0; i < tags.size(); i++) {
            buffer.append(i + " : " + tagInstructions.get(i) + "\n");
            buffer.append("Lat: " + tags.get(i).getLatitude() + " Long: " + tags.get(i).getLongitude());
        }

        Intent i = new Intent(getBaseContext(), ShowDb.class);
        i.putExtra("db", buffer.toString());
        startActivity(i);
//        Toast.makeText(getApplicationContext(),buffer.toString(),Toast.LENGTH_LONG).show();
    }

    public String removeUnnamed(String instruction) {
//        String newinstruction = instruction.replaceAll("unnamed", "this");
        return instruction.replaceAll("(.*)waypoint(.*)", "You have arrived at your destination.");
//        return newinstruction;
    }

    public String distanceToStr(double length) {
        String result;
        if (length >= 100.0) {
            result = this.getString(org.osmdroid.bonuspack.R.string.osmbonuspack_format_distance_kilometers, (int) (length)) + ", ";
        } else if (length >= 1.0) {
            result = this.getString(org.osmdroid.bonuspack.R.string.osmbonuspack_format_distance_kilometers, Math.round(length * 10) / 10.0) + ", ";
        } else {
            result = this.getString(org.osmdroid.bonuspack.R.string.osmbonuspack_format_distance_meters, (int) (length * 1000)) + ", ";
        }
        return result;
    }


    public void changeNavigationLayout(){
        setContentView(R.layout.activity_navigation);
    }

    public void exportDb(View view) {
        try {
            File file = new File(this.getExternalFilesDir(null), "trace" + UUID.randomUUID().toString() + ".txt");
            FileOutputStream fileOutput = new FileOutputStream(file);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutput);
            Cursor c = db.rawQuery("SELECT * from trackData;", null);
            while (c.moveToNext()) {
                outputStreamWriter.write(c.getString(0) + "\t" + c.getString(1) + "\t" + c.getString(2) + "\t" + c.getString(3) + "\t" + c.getString(4) + "\n");
//                Toast.makeText(getApplicationContext(), c.getString(1), Toast.LENGTH_LONG).show();
            }
            outputStreamWriter.flush();
            fileOutput.getFD().sync();
            outputStreamWriter.close();
            MediaScannerConnection.scanFile(
                    this,
                    new String[]{file.getAbsolutePath()},
                    null,
                    null);
            db.execSQL("DELETE from trackData;");
            db.execSQL("UPDATE SQLITE_SEQUENCE SET SEQ=0 WHERE NAME='trackData';");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
        if (isNavigating) {
            try {
                File file = new File(this.getExternalFilesDir(null), "mypoints" + UUID.randomUUID().toString() + ".txt");

                FileOutputStream fileOutput = new FileOutputStream(file);
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutput);
                for (int i = 0; i < landmarks.size(); i++) {
                    outputStreamWriter.write(i + 1 + "\t" + landmarks.get(i).getLatitude() + "\t" + landmarks.get(i).getLongitude() + "\n");
                }
                for (int i = 0; i < tags.size(); i++) {
                    outputStreamWriter.write(i + 1 + "\t" + tags.get(i).getLatitude() + "\t" + tags.get(i).getLongitude() + "\n");
                }
                outputStreamWriter.flush();
                fileOutput.getFD().sync();
                outputStreamWriter.close();
                MediaScannerConnection.scanFile(
                        this,
                        new String[]{file.getAbsolutePath()},
                        null,
                        null);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
        }
        Toast.makeText(getApplicationContext(), "Written to file", Toast.LENGTH_LONG).show();


    }


    //PRINT THE WHOLE DATABASE
    public void showMessage1(View view) {
        for (int i = 0; i < landmarks.size(); i++) {
            System.out.println("Landmarks: " + landmarks.get(i));
            System.out.println("Instructions: " + instructions.get(i));
        }
        for (int i = 0; i < tags.size(); i++) {
            System.out.println("tags: " + tags.get(i));
            System.out.println("Tag Instructions: " + tagInstructions.get(i));
        }

        StringBuffer buffer = new StringBuffer();

        buffer.append("Current Location: " + current_lat + " " + current_long + "\n" + "\n");
        System.out.println("Location Details: \n" + buffer.toString());

        Cursor c = db.rawQuery("SELECT * FROM myLocation", null);
        Cursor cc = db.rawQuery("SELECT * FROM locationByTag", null);

        if ((c.getCount() == 0)&&(cc.getCount() == 0)) {
            System.out.println("No records found");
            Toast.makeText(getApplicationContext(), "No Database Found", Toast.LENGTH_LONG).show();
            return;
        }

        if ((c.getCount() != 0)) buffer.append("L A N D M A R K S : " + "\n");
        while (c.moveToNext()) {
            buffer.append("Latitude: " + c.getString(1) + "\n");
            buffer.append("Longitude: " + c.getString(2) + "\n");
            buffer.append("Description: " + c.getString(3) + "\n");
            buffer.append("Id: " + c.getString(0) + "\n");
            buffer.append("Previous Id: " + c.getString(5) + "\n");
            buffer.append("Next Id: " + c.getString(6) + "\n");
        }

        if ((c.getCount() != 0)) buffer.append("\nT A G S : " + "\n");
        while (cc.moveToNext()) {
            buffer.append("Tag id: ");
            buffer.append(cc.getString(1) + "\nNode id: ");
            buffer.append(cc.getString(2) + "\n");
        }

        Intent i = new Intent(getBaseContext(), ShowDb.class);
        i.putExtra("db", buffer.toString());
        startActivity(i);
//        Toast.makeText(getApplicationContext(),buffer.toString(),Toast.LENGTH_LONG).show();
    }


    // PRINT THE POINTS NEAR TO THE GIVEN POINT
    public void getLocalInfo(View view) {

        Toast.makeText(getApplicationContext(), "Lat: " + current_lat + " Long: " + current_long, Toast.LENGTH_LONG).show();
    }

    public void clearData(View view) {
        db.execSQL("DELETE FROM myLocation;");
        db.execSQL("DELETE FROM myTags;");
        db.execSQL("DELETE FROM locationByTag;");
        db.execSQL("DELETE FROM trackdata;");
        db.execSQL("DELETE FROM routes;");
        db.execSQL("DELETE FROM routebyinstructions;");
        db.execSQL("DELETE FROM tagroutebyinstructions;");
        prev_id = 0;
        Toast.makeText(this, "Data Cleared", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        compass.stop();
    }
    @Override
    protected void onResume() {
        super.onResume();
        compass.start();
    }
    @Override
    protected void onStop() {
        if(mGoogleApiClient!=null){

            if(mGoogleApiClient.isConnected()){

                mGoogleApiClient.disconnect();
            }

        }
        compass.stop();
        super.onStop();
    }

    public double getBearingNext() {

        double lat1 = current_lat * Math.PI / 180;
        double lat2 = landmarks.get(osmNextInstruction).getLatitude() * Math.PI / 180;
        double lon1 = current_long * Math.PI / 180;
        double lon2 = landmarks.get(osmNextInstruction).getLongitude() * Math.PI / 180;
        double dLon = (lon1 - lon2);
        double return_val = 0;
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        double brng = (Math.atan2(y, x)) * 180 / Math.PI;

        // fix negative degrees
        if (brng < 0) {
            brng = 360 - abs(brng);
        }

        for(int i =0; i<5; i++){
            return_val = return_val + (720 - (brng + compass.getAzimuth())) % 360;
        }

        return return_val/5;
    }

    public double getBearingTo(GeoPoint target) {

        double lat1 = current_lat * Math.PI / 180;
        double lat2 = target.getLatitude() * Math.PI / 180;
        double lon1 = current_long * Math.PI / 180;
        double lon2 = target.getLongitude() * Math.PI / 180;
        double dLon = (lon1 - lon2);
        double return_val = 0;
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        double brng = (Math.atan2(y, x)) * 180 / Math.PI;

        // fix negative degrees
        if (brng < 0) {
            brng = 360 - abs(brng);
        }

        for(int i =0; i<5; i++){
            return_val = return_val + (720 - (brng + compass.getAzimuth())) % 360;
        }

        return return_val/5;
    }


    public void getLocalInfo() {
        // System.out.println("getLocalInfo function called");
//        gps.getLocation();
        if (isNavigating) {


            double lat_float = current_lat;
            double long_float = current_long;
            double turn = getBearingNext();

            if (!faceHeadDirection) {
                if ((int) turn < 10 || (int) turn > 350) {
                    tts.speak("You are now facing the correct direction. Proceed straight. ", TextToSpeech.QUEUE_ADD, null);
                    faceHeadDirection = true;
                    getTagAtLocation();
                    dir_timestamp = System.currentTimeMillis();
                    turn_timestamp = System.currentTimeMillis();
                    setParts();
                }
            } else if (osmNextInstruction < landmarks.size()) {
                int i = osmNextInstruction;
                float[] comp1 = new float[1];
                float[] comp2 = new float[1];
                float[] comp3 = new float[1];
                float[] comp4 = new float[1];
                Location.distanceBetween(current_lat, current_long, landmarks.get(i).getLatitude(), landmarks.get(i).getLongitude(), comp1);
                Location.distanceBetween(previous_location.getLatitude(), previous_location.getLongitude(), landmarks.get(i).getLatitude(), landmarks.get(i).getLongitude(), comp2);
                checkWayPart(comp1[0],comp2[0]);

                Location.distanceBetween(current_lat, current_long, final_lat, final_long, comp3);
                Location.distanceBetween(init_lat, init_long, final_lat, final_long, comp4);

                x = (int) comp1[0];
                y = (int) (comp2[0]) - 12;

                z = (int) (comp3[0]);
                w = (int) (comp4[0]);


                if ((isNavigating == true)&&(isDebug == false)) {
                    myLayout = (LinearLayout) findViewById(R.id.myLayout);
                    myLayout.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {

                            int p = (int) event.getX();
                            int q = (int) event.getY();
                            Log.i(TAG, String.valueOf(p) + "tan2");
                            int r = p / 11;
                            if (r - mProgressStatus < 5 && r - mProgressStatus > -5) {
                                tts.speak(String.valueOf(mProgressStatus) + "percent route covered", TextToSpeech.QUEUE_ADD, null);
                            }
                            return false;
                        }
                    });

                    mProgressBar = (ProgressBar) findViewById(R.id.progressbar);
                    mProgressStatus = (int) ((w - z) * 100 / (w + 0.0001));

                    if (mProgressStatus < 0) {
                        mProgressStatus = 0;
                    }
                    mProgressBar.setProgress(mProgressStatus);
                }
                if (Math.abs(dir_timestamp - System.currentTimeMillis()) > 30000) {
                    if (comp1[0] > 20.0) {
                        if(Math.abs(turn_timestamp - System.currentTimeMillis()) > 10000) {
                            if (!((int) turn < 30 || (int) turn > 330)) {
                                if (turn < 180)
                                    tts.speak("You are deviating from the correct direction. Turn " + (int) turn + " degree Clockwise.", TextToSpeech.QUEUE_ADD, null);
                                else
                                    tts.speak("You are deviating from the correct direction. Turn " + (int) (360 - turn) + " degrees Anti-Clockwise.", TextToSpeech.QUEUE_ADD, null);
                                dir_timestamp = System.currentTimeMillis();
                            }
                        }
                    }
                }

                if (abs(tst.get(i) - System.currentTimeMillis()) > 60000) {
                    System.out.println(comp1[0] + " " + comp2[0]);
                    if (comp1[0] > 12.0) {

                        if (x <= y / 5) {
                            tts.speak("In " + x + " meters " + instructions.get(i), TextToSpeech.QUEUE_ADD, null);
                        } else if (x <= 4 * y / 7) {
                            tts.speak("Continue straight for another " + x + " meters", TextToSpeech.QUEUE_ADD, null);
                        }
//                        } else if (x <= 2 * y / 3 + 10) {
//                            tts.speak("You are on the correct path. Next turn will be in " + x + " meters", TextToSpeech.QUEUE_ADD, null);
//                        }
                    } else if (abs(tst.get(i)) <= abs(timestamps.get(i))) {
                        prefetch_nextInstruction = i + 1;
                        previous_location = new GeoPoint(landmarks.get(i).getLatitude(), landmarks.get(i).getLongitude());
                    }
                    tst.set(i, System.currentTimeMillis());
                }
            }
            // comment out till here

            // this below section works
            db.execSQL("INSERT INTO trackData VALUES(NULL, " + (int) (current_lat * 10000000) + ", " + (int) (current_long * 10000000) + ", 1, " + System.currentTimeMillis() + ");");
            for (int i = 0; i < landmarks.size(); i++) {
                if (abs(landmarks.get(i).getLatitude() - lat_float) < 0.0001 && abs(landmarks.get(i).getLongitude() - long_float) < 0.0001) {
                    if (abs(timestamps.get(i) - System.currentTimeMillis()) > 60000) {
                        System.out.println("Lat_diff: " + abs(landmarks.get(i).getLatitude() - lat_float));
                        System.out.println("Long diff: " + abs(landmarks.get(i).getLongitude() - long_float));
                        Toast.makeText(getApplicationContext(), instructions.get(i), Toast.LENGTH_LONG).show();
                        System.out.println("String found: " + instructions.get(i));
                        tts.speak(instructions.get(i), TextToSpeech.QUEUE_ADD, null);
                        timestamps.set(i, System.currentTimeMillis());

                        for (int j = 0; j < tags.size(); j++) {
                            if (tagLandmark.get(j) == i) {
                                if (tagCheck.get(j) == 0) {
                                    System.out.println("Lat_diff: " + abs(tags.get(j).getLatitude() - lat_float));
                                    System.out.println("Long diff: " + abs(tags.get(j).getLongitude() - long_float));
                                    System.out.println("String found: " + tagInstructions.get(j));
                                    tts.speak(tagInstructions.get(j), TextToSpeech.QUEUE_ADD, null);
                                    tagCheck.set(j, 1);
                                }
                            }
                        }
                        if (i < landmarks.size() - 1) {
                            osmNextInstruction = i + 1;
                            setParts();
                            turn_timestamp = System.currentTimeMillis();
                            tst.set(i, System.currentTimeMillis() - 30000);
                            //float[] results = new float[3];
                            //Location.distanceBetween(current_lat, current_long, landmarks.get(osmNextInstruction).getLatitude(), landmarks.get(osmNextInstruction).getLongitude(), results);
                            //String new_instruction = instructions.get(osmNextInstruction).replace("(.*)You have arrived at your destination(.*)", "You will be at your destination");
                            //tts.speak("For " + ((int) results[0]) + "metres, continue straight", TextToSpeech.QUEUE_ADD, null);
                        }
                        if (i == landmarks.size() - 1){
                            double direction = getBearingTo(endPoint);
                            if (direction < 180 && direction > 10)
                                tts.speak(" Your Destination is on the right.", TextToSpeech.QUEUE_ADD, null);
                            else if(direction >= 180 && direction < 350)
                                tts.speak(" Your Destination is on the left.", TextToSpeech.QUEUE_ADD, null);

                        }
                    }
                }
            }
            for (int i = 0; i < saved_landmarks.size(); i++) {
                if (abs(saved_landmarks.get(i).getLatitude() - lat_float) < 0.0001 && abs(saved_landmarks.get(i).getLongitude() - long_float) < 0.0001) {
                    if(saved_landmarks_check.get(i) == 0) {
                        System.out.println("Lat_diff: " + abs(saved_landmarks.get(i).getLatitude() - lat_float));
                        System.out.println("Long diff: " + abs(saved_landmarks.get(i).getLongitude() - long_float));
                        Toast.makeText(getApplicationContext(), saved_instructions.get(i), Toast.LENGTH_LONG).show();
                        System.out.println("String found: " + saved_instructions.get(i));
                        tts.speak(saved_instructions.get(i) + " at current location", TextToSpeech.QUEUE_ADD, null);
                        saved_landmarks_check.set(i, 1);
                    }
                }
            }
        }
    }

    public void checkWayPart(float remaining, float total){
        if (remaining < total/4 && part4){
            tts.speak("In " + (int) remaining + " meters " + instructions.get(osmNextInstruction), TextToSpeech.QUEUE_ADD, null);
            part4 = false;
        }
        else if (remaining < total/2 && part3){
            tts.speak("Continue straight for another " + (int) remaining + " meters", TextToSpeech.QUEUE_ADD, null);
            part3 = false;
        }
        else if (remaining < 3*total/4 && part2){
            tts.speak("Continue straight for another " + (int) remaining + " meters", TextToSpeech.QUEUE_ADD, null);
            part2 = false;
        }
        else if (remaining < 7*total/8 && part1){
            tts.speak("For " + (int) remaining + " meters, continue straight", TextToSpeech.QUEUE_ADD, null);
            part1 = false;
        }
    }
    public void setParts(){
        part1 = true;
        part2 = true;
        part3 = true;
        part4 = true;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000); // Update location every second

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
            current_lat = mLastLocation.getLatitude();
            current_long = mLastLocation.getLongitude();
            Log.i("current_lat_lng","Fetching Current LatLong  " + current_lat + ";" + current_long);
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
        current_lat = location.getLatitude();
        current_long = location.getLongitude();
    }

    public void onDebugButton(View view) {
        Intent i = new Intent(getBaseContext(), Debug.class);
        startActivity(i);
    }

    public void onDebug(View view){
        isDebug = true;
        setContentView(R.layout.debug_activity);
    }

    public void onStopDebug(View view){
        setContentView(R.layout.activity_dummy);
        isDebug = false;
    }


    public void onNextButton(View view) {
        if (isNavigating) {
            if (!faceHeadDirection) {
                double turn = getBearingNext();
                if (turn < 180)
                    tts.speak("Turn " + (int) turn + " degree Clockwise.", TextToSpeech.QUEUE_ADD, null);
                else
                    tts.speak("Turn " + (int) (360 - turn) + " degrees Anti-Clockwise.", TextToSpeech.QUEUE_ADD, null);
                return;
            }
            if (osmNextInstruction < landmarks.size()) {
                float[] results = new float[3];
                System.out.println("current : " + current_lat + " , " + current_long);
                System.out.println("next : " + landmarks.get(osmNextInstruction).getLatitude() + " " + landmarks.get(osmNextInstruction).getLongitude());
                Location.distanceBetween(current_lat, current_long, landmarks.get(osmNextInstruction).getLatitude(), landmarks.get(osmNextInstruction).getLongitude(), results);
                String new_instruction = instructions.get(osmNextInstruction).replace("(.*)You have arrived at your destination(.*)", "You will be at your destination");
                tts.speak("After " + ((int) results[0]) + " meters, " + new_instruction, TextToSpeech.QUEUE_ADD, null);
            }
            else if(osmNextInstruction == landmarks.size() - 1){
                double direction = getBearingTo(endPoint);
                if (direction < 180 && direction > 10)
                    tts.speak(" Your Destination is on the right.", TextToSpeech.QUEUE_ADD, null);
                else if(direction >= 180 && direction < 350)
                    tts.speak(" Your Destination is on the left.", TextToSpeech.QUEUE_ADD, null);
            }
        }
    }
    public void nearbyPOI(View view) {
        //promptSpeechInput();          Uncomment if inputting POIs by voice command
        searchPOIfromName("name");
    }
    public void searchPOIfromName(String POI_type) {
        double lat_float = current_lat;
        double long_float = current_long;
        float[] results = new float[3];
        BoundingBox bb = new BoundingBox(lat_float + 0.0003, long_float + 0.0003, lat_float - 0.0003, long_float - 0.0003);
        System.out.println("Starting to find the POI : " + POI_type);
        String urlforpoirequest = overpassProvider.urlForPOISearch("\"" + POI_type + "\"", bb, 20, 50);
        ArrayList<POI> namePOI = overpassProvider.getPOIsFromUrl(urlforpoirequest);
        if (namePOI.size() == 0) {
            tts.speak("No " + POI_type + " nearby", TextToSpeech.QUEUE_ADD, null);
            System.out.println("overpass returning nothing");
        }
        if (namePOI != null) {
            System.out.println("Size is: " + namePOI.size());
            int ptr = 0;
            while (namePOI != null && ptr < namePOI.size()){
                Log.i("name", "name is : " + namePOI.get(ptr).mType);
                String name = namePOI.get(ptr).mType;
                if ((name != null) && (name.length() > 3)) {
                    poi_tags.add(namePOI.get(ptr).mLocation);
                    System.out.println("For POI Added: " + namePOI.get(ptr).mLocation + " " + name + " " + namePOI.get(ptr).mType);
                    name = name.replaceAll("_", " ");
//                    name = name.replaceAll("residential", "residential area");
//                    name = name.replaceAll("commercial", "commercial area");
//                    name = name.replaceAll("yes", " ");
                    String temp = " " + name + " nearby";
                    poi_tagInstructions.add(temp);
                }
                ptr++;
            }
            for (int i = 0; i < poi_tags.size(); i++) {
                Location.distanceBetween(lat_float, long_float, poi_tags.get(i).getLatitude(), poi_tags.get(i).getLongitude(), results);
                tts.speak(poi_tagInstructions.get(i) + " at a distance of " + ((int) results[0]) + " metres.", TextToSpeech.QUEUE_ADD, null);
            }
        }
        namePOI.clear();
        poi_tags.clear();
        poi_tagInstructions.clear();
    }


    public void setBoundsBias(){
        LatLng southWestBound = new LatLng( current_lat - 0.4, current_long - 0.4);
        LatLng northEastBound = new LatLng(current_lat + 0.4, current_long + 0.4);
        LatLngBounds customBounds = new LatLngBounds(southWestBound, northEastBound);
        endingDestination.setBoundsBias(customBounds);
        AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                .setTypeFilter(AutocompleteFilter.TYPE_FILTER_NONE)
                .build();
        endingDestination.setFilter(typeFilter);
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

    public void aroundMe(View view) {
        String min_lat = "" + (current_lat - 0.0005);
        String max_lat = "" + (current_lat + 0.0005);
        String min_long = "" + (current_long - 0.0005);
        String max_long = "" + (current_long + 0.0005);
        String request_url = OSM_EDITING_URL + min_long + "," + min_lat + "," + max_long + "," +  max_lat;
        String raw_text = requestStringFromUrl(request_url);
        Toast.makeText(getApplicationContext(), raw_text, Toast.LENGTH_LONG).show();
        System.out.println(raw_text);
    }


    public void prePlanning ( View v){
       /* if(isNavigating){
            for(i=0;instructions.get(i)!=null;i++)
                tts.speak(instructions.get(i), TextToSpeech.QUEUE_ADD, null);
                tts.speak("next instruction is",TextToSpeech.QUEUE_ADD, null);
        }*/

        // System.out.println("onStartButton function called");
        if (!isNavigating) {
//            EditText edit =  (EditText) findViewById(R.id.editText2);
//            String destination = edit.getText().toString();
            if (!isSelected) {
                tts.speak("Destination not set", TextToSpeech.QUEUE_ADD, null);
                Toast.makeText(getApplicationContext(), "Destination not set!", Toast.LENGTH_LONG).show();
                return;
            }

            tts.speak(" Please wait while we search for a suitable route.", TextToSpeech.QUEUE_ADD, null);
            GeoPoint startPoint = new GeoPoint(current_lat, current_long);
            previous_location = startPoint;
            //            GeoPoint endPoint = new GeoPoint(Double.parseDouble(destination.split(",")[0]), Double.parseDouble(destination.split(",")[1]));
            GeoPoint endPoint = new GeoPoint(destinationLatLng.getLatLng().latitude, destinationLatLng.getLatLng().longitude);
            ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
            waypoints.add(startPoint);
            waypoints.add(endPoint);
//            roadManager.addRequestOption("routeType=pedestrian");
            roadManager.setService("http://router.project-osrm.org/route/v1/pedestrian/");
            // roadManager.addRequestOption("alternatives=3");
            double max_latitude = current_lat;
            double min_latitude = current_lat;
            double max_longitude = current_long;
            double min_longitude = current_long;
            System.out.println("Starting point is: " + startPoint.toDoubleString());

            // original line earlier
            road = roadManager.getRoad(waypoints);
            // System.out.println("Number of roads found: " + road.length);

            // added check
            boolean roadstatusflag = true;
            if (road.mStatus != Road.STATUS_OK) {
                System.out.println("~~~~~~~THERE WAS ISSUE WITH THE STATUS OF THE ROADS~~~~~~~~");
                Toast.makeText(getApplicationContext(), "Network error!", Toast.LENGTH_LONG).show();
                tts.speak("Network Issue. Please try again!", TextToSpeech.QUEUE_ADD, null);
                roadstatusflag = false;
            }
            overpassProvider = new OverpassAPIProvider2();
            for (int i = 0; i < road.mNodes.size(); i++) {
                if (road.mNodes.get(i).mInstructions != null && !road.mNodes.get(i).mInstructions.isEmpty()) {

                    double nodelat_curr = road.mNodes.get(i).mLocation.getLatitude();
                    double nodelon_curr = road.mNodes.get(i).mLocation.getLongitude();
                    double nodelat_next = 0;
                    double nodelon_next = 0;
                    double nodelat_prev = 0;
                    double nodelon_prev = 0;
                    if (i != road.mNodes.size() - 1) {
                        nodelat_next = road.mNodes.get(i + 1).mLocation.getLatitude();
                        nodelon_next = road.mNodes.get(i + 1).mLocation.getLongitude();
                    }
                    if (landmarks.size() != 0) {
                        nodelat_prev = landmarks.get(landmarks.size() - 1).getLatitude();
                        nodelon_prev = landmarks.get(landmarks.size() - 1).getLongitude();
                    }
                    GeoPoint loc = road.mNodes.get(i).mLocation;
                    BoundingBox bb = new BoundingBox(loc.getLatitude() + 0.0002, loc.getLongitude() + 0.0002, loc.getLatitude() - 0.0002, loc.getLongitude() - 0.0002);

                    // url for highway type
                    String urlforpoirequest = overpassProvider.urlForPOISearch("\"highway\"", bb, 10, 50);
                    ArrayList<POI> points = overpassProvider.getPOIsFromUrlRoad(urlforpoirequest);
                    if (points == null) System.out.println("overpass returning nothing");
                    if (points != null) System.out.println("Size is: " + points.size());
                    {
                        int ptr = 0;
                        if (points.size() > 1) {
                            while (points != null && ptr < points.size()) {
                                if ((points.get(ptr).mLocation.getLatitude() - nodelat_curr) / (nodelat_next - nodelat_curr) > 0) {
                                    if ((points.get(ptr).mLocation.getLongitude() - nodelon_curr) / (nodelon_next - nodelon_curr) > 0) {
                                        String typeofhighway = points.get(ptr).mType;
                                        String lanecount = points.get(ptr).mUrl;
                                        String typeofsurface = points.get(ptr).mDescription;
                                        String footway = points.get(ptr).mThumbnailPath;
                                        tags.add(points.get(ptr).mLocation);
                                        System.out.println("Tag Location " + points.get(ptr).mLocation);
                                        typeofhighway = typeofhighway.replaceAll("_", " ");
                                        if (lanecount != null && (typeofsurface != null && !typeofsurface.isEmpty())) {
                                            tagInstructions.add("This is " + typeofhighway + " with " + lanecount + " lanes and " + typeofsurface + " surface");
                                        } else if (lanecount == null && (typeofsurface != null && !typeofsurface.isEmpty())) {
                                            tagInstructions.add("This is " + typeofhighway + " with " + typeofsurface + " surface");
                                        } else if (lanecount != null && (typeofsurface == null || typeofsurface.isEmpty())) {
                                            tagInstructions.add("This is " + typeofhighway + " with " + lanecount + " lanes");
                                        } else {
                                            tagInstructions.add("This is " + typeofhighway);
                                        }
                                        if (footway != null) {
                                            tagInstructions.set(tagInstructions.size() - 1, tagInstructions.get(tagInstructions.size() - 1) + ". Presence of footway ; " + footway);
                                        }
                                        int len = (tagInstructions.size() - 1);
                                        System.out.println(tagInstructions.get(len));
                                        if (i != 0) {
                                            tagLandmark.add(landmarks.size());
                                        } else if (i == 0) {
                                            tagLandmark.add(-1);
                                        }
                                        tagCheck.add(0);
                                        break;
                                    }
                                }
                                ptr++;
                            }
                        } else if (points.size() == 1) {
                            while (points != null && ptr < points.size()) {
                                String typeofhighway = points.get(ptr).mType;
                                String lanecount = points.get(ptr).mUrl;
                                String typeofsurface = points.get(ptr).mDescription;
                                String footway = points.get(ptr).mThumbnailPath;
                                tags.add(points.get(ptr).mLocation);
                                System.out.println("Tag Location " + points.get(ptr).mLocation);
                                typeofhighway = typeofhighway.replaceAll("_", " ");
                                if (lanecount != null && typeofsurface != null) {
                                    tagInstructions.add("This is " + typeofhighway + " with " + lanecount + " lanes and " + typeofsurface + " surface");
                                } else if (lanecount == null && typeofsurface != null && !typeofsurface.isEmpty()) {
                                    tagInstructions.add("This is " + typeofhighway + " with " + typeofsurface + " surface");
                                } else if (lanecount != null && typeofsurface == null) {
                                    tagInstructions.add("This is " + typeofhighway + " with " + lanecount + " lanes");
                                } else {
                                    tagInstructions.add("This is " + typeofhighway);
                                }
                                if (footway != null) {
                                    tagInstructions.set(tagInstructions.size() - 1, tagInstructions.get(tagInstructions.size() - 1) + ". Presence of footway ; " + footway);
                                }
                                int len = (tagInstructions.size() - 1);
                                System.out.println(tagInstructions.get(len));
                                tagCheck.add(0);
                                if (i != 0) {
                                    tagLandmark.add(landmarks.size());
                                } else if (i == 0) {
                                    tagLandmark.add(-1);
                                }
                                ptr++;
                            }
                        }
                    }

                    if (i != 0)
                    {
                        landmarks.add(new GeoPoint(loc.getLatitude(), loc.getLongitude()));
                        System.out.println("Added Instruction: " + loc.getLatitude() + ", " + loc.getLongitude() + " " + road.mNodes.get(i).mInstructions);
                        instructions.add(removeUnnamed(road.mNodes.get(i).mInstructions));

                        timestamps.add(new Long(0));
                        tst.add(new Long(0));
                        osmNumInstructions = road.mNodes.size();
                        osmNextInstruction = 0;
                        prefetch_nextInstruction = 0;
                        if (loc.getLatitude() > max_latitude) {
                            max_latitude = loc.getLatitude();
                        }
                        if (loc.getLatitude() < min_latitude) {
                            min_latitude = loc.getLatitude();
                        }
                        if (loc.getLongitude() > max_longitude) {
                            max_longitude = loc.getLongitude();
                        }
                        if (loc.getLongitude() < min_longitude) {
                            min_longitude = loc.getLongitude();
                        }
                    }
                }
            }
            // System.exit(0);
            int max_latitude_int = (int) (max_latitude * 10000000);
            int max_longitude_int = (int) (max_longitude * 10000000);
            int min_latitude_int = (int) (min_latitude * 10000000);
            int min_longitude_int = (int) (min_longitude * 10000000);
//            Cursor c = db.rawQuery("SELECT * FROM myLocation WHERE lat BETWEEN " + (min_latitude_int) + " AND " + (max_latitude_int) + " AND long BETWEEN " + (min_longitude_int) + " AND " + (max_longitude_int), null);
//            Cursor c = db.rawQuery("SELECT * FROM myLocation", null);
//            while (c.moveToNext()) {
//                double latitude = Double.parseDouble(c.getString(1)) / 10000000;
//                double longitude = Double.parseDouble(c.getString(2)) / 10000000;
//                String description = c.getString(3);
//                landmarks.add(new GeoPoint(latitude, longitude));
//                instructions.add(description);
//                timestamps.add(new Long(0));
//                tst.add(new Long(0));
//            }

            if (roadstatusflag) {

                setContentView(R.layout.activity_path_review);
                isPreviewing = true;

                navigatingDistance = distanceToStr(road.mLength);
                tts.speak("Starting Route Preview. Your location is " + navigatingDistance + " away.", TextToSpeech.QUEUE_ADD, null);
                headingDirection = getBearingNext();
                System.out.println(headingDirection);
                if (headingDirection < 180 && headingDirection >= 0)
                    tts.speak("heading direction is " + (int) headingDirection + " degree Clockwise.", TextToSpeech.QUEUE_ADD, null);
                else if(headingDirection >= 180 && headingDirection < 360)
                    tts.speak("heading direction is " + (int) (360 - headingDirection) + " degrees Anti-Clockwise.", TextToSpeech.QUEUE_ADD, null);

                for(int i = 0; i< landmarks.size(); i++){
                    System.out.println(landmarks.get(i).getLatitude());
                    System.out.println(landmarks.get(i).getLongitude());
                    System.out.println(instructions.get(i));
                }
            }
        } else {
            landmarks.clear();
            tags.clear();
            instructions.clear();
            tagInstructions.clear();
            tagCheck.clear();
            tagLandmark.clear();
            timestamps.clear();
            tst.clear();
            isNavigating = false;
            faceHeadDirection = false;
            setContentView(R.layout.activity_main);
        }
    }

    public void back(View v){
        setContentView(R.layout.activity_dummy);
        landmarks.clear();
        tags.clear();
        instructions.clear();
        tagInstructions.clear();
        tagCheck.clear();
        tagLandmark.clear();
        timestamps.clear();
        tst.clear();
        isNavigating = false;
        faceHeadDirection = false;
    }

    public void tags(View v){
        try{
            if(instid<landmarks.size()){
                GeoPoint node = landmarks.get(instid);
                double loc_lat = node.getLatitude();
                double loc_lon = node.getLongitude();
                boolean tag_presence = false;
                for (int j = 0; j < tagInstructions.size(); j++) {
                    if (tagLandmark.get(j) == instid - 1) {
                        tag_presence = true;
                        tts.speak(tagInstructions.get(j), TextToSpeech.QUEUE_ADD, null);
                    }
                }
                if (tag_presence == false) {
                    tts.speak(" No tags Present", TextToSpeech.QUEUE_ADD, null);
                }
            }
            else{
                tts.speak(" End of route preview", TextToSpeech.QUEUE_ADD, null);
            }
        }
        catch (ArrayIndexOutOfBoundsException e){

        }
    }

    public void repeat(View v){
        if(instid > 0) {
            instid--;
            tts.speak(instructions.get(instid), TextToSpeech.QUEUE_ADD, null);
            instid++;
        }
        else{
            headingDirection = getBearingNext();
            System.out.println(headingDirection);
            if (headingDirection < 180 && headingDirection >= 0)
                tts.speak("heading direction is " + (int) headingDirection + " degree Clockwise.", TextToSpeech.QUEUE_ADD, null);
            else if(headingDirection >= 180 && headingDirection < 360)
                tts.speak("heading direction is " + (int) (360 - headingDirection) + " degrees Anti-Clockwise.", TextToSpeech.QUEUE_ADD, null);
        }
    }

    public void next (View v){
        if(instid < instructions.size()) {
            tts.speak(instructions.get(instid), TextToSpeech.QUEUE_ADD, null);
            instid++;
        }
        else {
            tts.speak("End of route Preview", TextToSpeech.QUEUE_ADD, null);
        }
    }
    public void previous (View v){
        if(instid<2){
            tts.speak("no previous instructions", TextToSpeech.QUEUE_ADD, null);
        }
        else {
            instid = instid - 2;
            tts.speak(instructions.get(instid), TextToSpeech.QUEUE_ADD, null);
            instid = instid + 1;
        }
    }

}