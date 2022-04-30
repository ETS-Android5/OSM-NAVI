package khushboo.rohit.osmnavi;

import android.Manifest;
import android.app.Activity;
import android.app.NativeActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.places.api.model.Place;

import org.osmdroid.bonuspack.location.POI;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static java.lang.Math.abs;

public class NavigationActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final int REQ_CODE_SAVE_LANDMARK = 1;
    private static final int REQ_CODE_SAVE_ROUTE = 2;
    TextToSpeech tts;
    Compass compass;
    SQLiteDatabase db;
    MyApp app;
    Handler h = new Handler();
    Handler appHandler = new Handler();
    int delay = 100, delayCheck = 100 ; //milliseconds
    boolean faceHeadDirection = false;
    boolean part1 = false, part2 = false, part3 = false, part4 = false;
    long time_diff = 60 * 1000;
    Button button, save_button;
    String navigatingDistance, routeName;
    int osmNumInstructions, osmNextInstruction, prefetch_nextInstruction, Instruction_progressTracker;
    int prev_id = 0;
    long tracetime = 0;
    int trackdataSerial = 0;
    int currentLandmark = -1, xingCnt = 0;
    String xingStr = "After ";
    double headingDirection = 0;
    boolean orientationCheck = true;
    boolean searchStatus;
    Place destinationLatLng;
    double currentDegree = 0;
    ArrayList<GeoPoint> landmarks;
    ArrayList<GeoPoint> saved_landmarks;
    ArrayList<GeoPoint> tags;
    ArrayList<GeoPoint> poi_tags;
    ArrayList<String> instructions;
    ArrayList<String> saved_instructions;
    ArrayList<String> tagInstructions;
    ArrayList<Integer> tagCheck;
    ArrayList<Integer> landmarkCheck;
    ArrayList<Integer> saved_landmarks_check;
    ArrayList<Integer> tagLandmark;
    ArrayList<String> poi_tagInstructions;
    ArrayList<String> crossings;
    ArrayList<Integer> crossingsLandmark;
    ArrayList<Long> timestamps;
    ArrayList<Long> tst;
    Long dir_timestamp;
    int i = 100;
    int instid=0;
    int navType, selectedRoute;
    String destinationAddress;
    double final_lat = 0;
    double final_long = 0;
    double init_lat = 0;
    double init_long = 0;
    int x=0,y=0,z=0,w=0;
    long starttime = 0;
    long turn_timestamp;
    boolean isPreplanning = false, isSpeaking = false, isConnected=false, isNavigating =false, isLocChanged = false;
    double current_lat, current_long, previous_bearing;
    GeoPoint previous_landmark;
    Road road;
    OverpassAPIProvider2 overpassProvider;
    int pathLength;
    GeoPoint endPoint,startPoint;
    OSRMRoadManager roadManager;
    Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    Runnable localInfoRunnable = new Runnable() {
        public void run() {
            getLocalInfo();
            h.postDelayed(this, delay);
        }
    };
    Runnable appRunnable = new Runnable() {
        public void run() {
            navigationCheck();
            appHandler.postDelayed(this, delayCheck);
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.loading);

        landmarks = new ArrayList<GeoPoint>();
        instructions = new ArrayList<String>();
        saved_landmarks = new ArrayList<GeoPoint>();
        saved_landmarks_check = new ArrayList<Integer>();
        saved_instructions = new ArrayList<String>();
        tags = new ArrayList<GeoPoint>();
        tagInstructions = new ArrayList<String>();
        tagCheck = new ArrayList<Integer>();
        landmarkCheck = new ArrayList<Integer>();
        tagLandmark = new ArrayList<Integer>();
        poi_tags = new ArrayList<GeoPoint>();
        poi_tagInstructions = new ArrayList<String>();
        timestamps = new ArrayList<Long>();
        tst = new ArrayList<Long>();
        roadManager = new OSRMRoadManager(this);
        compass = new Compass(this);
        app = (MyApp) this.getApplicationContext();
        db = app.myDb;
        button = (Button) findViewById(R.id.button8);
        save_button = (Button) findViewById(R.id.button_save);
        compass.start();

        navType = getIntent().getIntExtra("Type",0);
        System.out.println(navType);


        if(navType == MainActivity.NAV_TYPE_LOAD_ROUTE) {
            selectedRoute = getIntent().getIntExtra("selectedRoute",0);
            navigatingDistance = getIntent().getStringExtra("selectedRouteDistance");
            routeName = getIntent().getStringExtra("selectedRouteName");
        }
        else if(navType == MainActivity.NAV_TYPE_NAVIGATE || navType == MainActivity.NAV_TYPE_PREPLAN) {
            final_lat = getIntent().getDoubleExtra("endLat", 0);
            final_long = getIntent().getDoubleExtra("endLon", 0);
            destinationAddress = getIntent().getStringExtra("destination");
        }

        buildGoogleApiClient();
        setTts();
        appHandler.postDelayed(appRunnable, delay);

    }

    @Override
    protected void onStart() {
        if(mGoogleApiClient!=null){
            if(!mGoogleApiClient.isConnected()){
                mGoogleApiClient.connect();
            }
        }
        if(mGoogleApiClient==null){
            buildGoogleApiClient();
        }
        super.onStart();
        compass.start();
    }

    @Override
    protected void onDestroy() {
        tts.shutdown();
        mGoogleApiClient.disconnect();
        super.onDestroy();
        compass.stop();
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
        if(mGoogleApiClient!=null){

            if(mGoogleApiClient.isConnected()){

                mGoogleApiClient.disconnect();
            }

        }
        compass.stop();
        super.onStop();
    }

    public void onSaveButton(View view) {
        if(navType!= MainActivity.NAV_TYPE_LOAD_ROUTE) {
            Intent i = new Intent(getBaseContext(), SuggestRoute.class);
            Address startAdd = getAddress(startPoint.getLatitude(), startPoint.getLongitude());
            String startLoc = getNameFromAddress(startAdd, 3);
            String endLoc = destinationAddress;
            //String startLoc = startAdd.getFeatureName() + ", " + startAdd.getThoroughfare();
            //String endLoc = endAdd.getFeatureName()+ ", " + startAdd.getThoroughfare();
            i.putExtra("startLoc", startLoc);
            i.putExtra("endLoc", endLoc);
            startActivityForResult(i, REQ_CODE_SAVE_ROUTE);
        }
        else{
            tts.speak("This route is already saved", TextToSpeech.QUEUE_ADD, null);
        }
    }

    public void onImageButton(View view) {
        System.out.println("onSaveButton function called");
        Intent i = new Intent(getBaseContext(), ImageActivity.class);
        startActivity(i);
    }

    public void whereAmI(View view){
        Address obj = getAddress(current_lat,current_long);
        String fin_add = getNameFromAddress(obj, 4);
        if(fin_add == null)
            tts.speak("Could Not find location info", TextToSpeech.QUEUE_ADD, null);
        else
            tts.speak("You are at " + fin_add, TextToSpeech.QUEUE_ADD, null);

        System.out.println(fin_add);
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

    public boolean requestRoute() {

        startPoint = new GeoPoint(current_lat, current_long);
        endPoint = new GeoPoint(final_lat, final_long);
        previous_landmark = startPoint;
        init_lat = current_lat;
        init_long = current_long;
        if (navType == MainActivity.NAV_TYPE_PREPLAN) isPreplanning = true;

        ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
        waypoints.add(startPoint);
        waypoints.add(endPoint);
        roadManager.setUserAgent("com.osm-navi.iit");
        roadManager.setService("https://router.project-osrm.org/route/v1/driving/");
        //roadManager.setService("https://api.openrouteservice.org/directions?api_key=5b3ce3597851110001cf6248fb336dc5e50e4501be13e8dbf3b45ac2&coordinates=8.34234,48.23424%7C8.34423,48.26424&profile=foot-walking&preference=shortest&roundabout_exits=true&continue_straight=true&extra_info=surface%7Cwaycategory%7Cwaytype%7Csuitability&optimized=true");// roadManager.addRequestOption("alternatives=3");

        System.out.println("Starting point is: " + startPoint.toDoubleString());
        System.out.println("Destination Point is: " + endPoint.toDoubleString());

        road = roadManager.getRoad(waypoints);
        if (road.mStatus != Road.STATUS_OK) {
            System.out.println("~~~~~~~THERE WAS ISSUE WITH THE STATUS OF THE ROADS~~~~~~~~");
            Toast.makeText(getApplicationContext(), "Network error!", Toast.LENGTH_LONG).show();
            tts.speak("Network Issue. Please try again!", TextToSpeech.QUEUE_ADD, null);
            return false;
        }

        overpassProvider = new OverpassAPIProvider2();
        for (int i = 0; i < road.mNodes.size(); i++) {

            GeoPoint loc = road.mNodes.get(i).mLocation;
            BoundingBox bb = new BoundingBox(loc.getLatitude() + 0.0002, loc.getLongitude() + 0.0002, loc.getLatitude() - 0.0002, loc.getLongitude() - 0.0002);

            if (road.mNodes.get(i).mInstructions != null && !road.mNodes.get(i).mInstructions.isEmpty()) {
                double nodelat_curr = road.mNodes.get(i).mLocation.getLatitude();
                double nodelon_curr = road.mNodes.get(i).mLocation.getLongitude();
                double nodelat_next = 0, nodelon_next = 0, nodelat_prev = 0, nodelon_prev = 0;

                if (i != road.mNodes.size() - 1) {
                    nodelat_next = road.mNodes.get(i + 1).mLocation.getLatitude();
                    nodelon_next = road.mNodes.get(i + 1).mLocation.getLongitude();
                }
                if (landmarks.size() != 0) {
                    nodelat_prev = landmarks.get(landmarks.size() - 1).getLatitude();
                    nodelon_prev = landmarks.get(landmarks.size() - 1).getLongitude();
                }
                // url for highway type
                String urlforpoirequest = overpassProvider.urlForPOISearch("\"highway\"", bb, 10, 50);
                ArrayList<POI> points = overpassProvider.getPOIsFromUrlRoad(urlforpoirequest);
                if (points == null) {
                    System.out.println("overpass returning nothing");
                    continue;
                }

                if (points.size() != 0 && points != null)
                {
                    System.out.println("Size is: " + points.size());

                    POI tagPOI = points.get(0);
                    if (points.size() > 1) {
                        for(int ptr =0; ptr < points.size(); ptr++) {
                            if ((points.get(ptr).mLocation.getLatitude() - nodelat_curr) / (nodelat_next - nodelat_curr) > 0) {
                                if ((points.get(ptr).mLocation.getLongitude() - nodelon_curr) / (nodelon_next - nodelon_curr) > 0) {
                                    tagPOI = points.get(ptr);
                                    break;
                                }
                            }
                        }
                    }
                    String typeofhighway = tagPOI.mType;
                    String lanecount = tagPOI.mUrl;
                    String typeofsurface = tagPOI.mDescription;
                    String footway = tagPOI.mThumbnailPath;
                    tags.add(tagPOI.mLocation);
                    System.out.println("Tag Location " + tagPOI.mLocation);
                    typeofhighway = typeofhighway.replaceAll("_", " ");
                    typeofsurface = typeofsurface.replaceAll(",", "");
                    if (lanecount != null && (typeofsurface != null && !typeofsurface.isEmpty())) {
                        tagInstructions.add("This is " + typeofhighway + " with " + lanecount + " lanes and " + typeofsurface + " surface.");
                    } else if (lanecount == null && (typeofsurface != null && !typeofsurface.isEmpty())) {
                        tagInstructions.add("This is " + typeofhighway + " with " + typeofsurface + " surface.");
                    } else if (lanecount != null && (typeofsurface == null || typeofsurface.isEmpty())) {
                        tagInstructions.add("This is " + typeofhighway + " with " + lanecount + " lanes.");
                    } else if (lanecount == null && (typeofsurface == null || typeofsurface.isEmpty())) {
                        tagInstructions.add("This is " + typeofhighway + ".");
                    }
                    int len = (tagInstructions.size() - 1);
                    if (footway != null) {
                        tagInstructions.set(len, tagInstructions.get(len) + " Presence of " + footway + " footway.");
                    }
                    System.out.println(tagInstructions.get(len));
                    tagCheck.add(0);
                    if (i != 0) {
                        tagLandmark.add(landmarks.size());
                    } else if (i == 0) {
                        tagLandmark.add(-1);
                    }
                }

                if (i != 0) {
                    landmarks.add(new GeoPoint(loc.getLatitude(), loc.getLongitude()));
                    landmarkCheck.add(0);
                    System.out.println("Added Instruction: " + loc.getLatitude() + ", " + loc.getLongitude() + " " + road.mNodes.get(i).mInstructions);
                    instructions.add(removeUnnamed(road.mNodes.get(i).mInstructions));
                    if(isPreplanning) {
                        preplanInstruction();
                        //preplanInstruction2();
                    }
                    timestamps.add(0L);
                    tst.add(0L);
                    osmNumInstructions = road.mNodes.size();
                    osmNextInstruction = 0;
                    prefetch_nextInstruction = 0;
                }
            }

//            else { //********** Block for fetching crossing *************//
//
//                String urlforpoirequest = overpassProvider.urlForPOISearch("\"crossing\"", bb, 10, 50);
//                ArrayList<POI> points = overpassProvider.getPOIsFromUrlNode(urlforpoirequest);
//                System.out.println("Arjun here it was .........  " + points.size());
//                if (points.size() != 0 && points != null) {
//                    System.out.println(points.get(0).mCategory);
//                    xingCnt++;
//                    xingStr = xingStr + getNumberOrdinal(xingCnt) + " " + points.get(0).mCategory;
//                    System.out.println(".............."+ xingStr + "..............");
//                }
//            }
        }

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

        changeNavigationLayout();
        navigatingDistance = distanceToStr(road.mLength);
        if(!isPreplanning) {
            tts.speak("Starting Navigation. Your location is " + navigatingDistance + " away.", TextToSpeech.QUEUE_ADD, null);
        }
        else {
            tts.speak("Starting Route Preview. Your location is " + navigatingDistance + " away.", TextToSpeech.QUEUE_ADD, null);
        }
        headingDirection = getBearingNext();
        System.out.println("Heading Direction" + headingDirection);
        if (headingDirection < 180 && headingDirection > 10){
            tts.speak("Turn " + (int) headingDirection + " degree towards right.", TextToSpeech.QUEUE_ADD, null);
        }
        else if (headingDirection >= 180 && headingDirection < 350){
            tts.speak("Turn " + (int) (360 - headingDirection) + " degrees towards left.", TextToSpeech.QUEUE_ADD, null);
        }

        turn_timestamp = System.currentTimeMillis();
        tst.set(0, turn_timestamp);
        starttime = System.currentTimeMillis();

        if(!isPreplanning) {
            h.postDelayed(localInfoRunnable, delay);
        }

        return true;
    }

    public void setTts(){
        tts = new TextToSpeech(NavigationActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // TODO Auto-generated method stub
                if (status == TextToSpeech.SUCCESS) {
                    isSpeaking = true;
                    tts.speak(" Please wait while we search for a suitable route.", TextToSpeech.QUEUE_ADD, null);
                }
                else
                    Log.e("error", "Initialization Failed!");
            }
        });
    }

    public void navigationCheck(){
        if(!isNavigating) {
            if (isConnected && isSpeaking && isLocChanged) {
                isNavigating=true;
                startNavigation();
            }
        }
    }

    public void startNavigation(){
        appHandler.removeCallbacks(appRunnable);

       if(navType == MainActivity.NAV_TYPE_NAVIGATE || navType == MainActivity.NAV_TYPE_PREPLAN) {
           searchStatus = requestRoute();
       }
       else if (navType == MainActivity.NAV_TYPE_LOAD_ROUTE){
           searchStatus = loadRoute();
       }

        if (!searchStatus) {
            while (tts.isSpeaking()) {
            }
            finish();
        }
    }

    public void onStopButton(View view){
        h.removeCallbacks(localInfoRunnable);
        landmarks.clear();
        tags.clear();
        instructions.clear();
        tagInstructions.clear();
        tagCheck.clear();
        landmarkCheck.clear();
        tagLandmark.clear();
        timestamps.clear();
        tst.clear();
        faceHeadDirection = false;
        exportDb();
        finish();
    }


    public void getLocalInfo() {

        double lat_float = current_lat;
        double long_float = current_long;
        double turn = getBearingNext();

//        System.out.println("turn" + turn);

        if (!faceHeadDirection) {
            if ((int) turn < 10 || (int) turn > 350) {
                tts.speak("You are now facing the correct direction. Proceed straight.", TextToSpeech.QUEUE_ADD, null);
                faceHeadDirection = true;
                getTagAtLocation();
                dir_timestamp = System.currentTimeMillis();
                turn_timestamp = System.currentTimeMillis();
                setParts();
            }
        }
        else if (osmNextInstruction < landmarks.size()) {
            int i = osmNextInstruction;
            float[] comp1 = new float[1];
            float[] comp2 = new float[1];
            Location.distanceBetween(current_lat, current_long, landmarks.get(i).getLatitude(), landmarks.get(i).getLongitude(), comp1);
            Location.distanceBetween(previous_landmark.getLatitude(), previous_landmark.getLongitude(), landmarks.get(i).getLatitude(), landmarks.get(i).getLongitude(), comp2);

            x = (int) comp1[0];  // remaining
            y = (int) comp2[0];  // total

            if(orientationCheck == false) {
                if (((int) turn < 10 || (int) turn > 350)){
                    tts.speak("You are now facing the correct direction. ", TextToSpeech.QUEUE_ADD, null);
                    orientationCheck = true;
                    dir_timestamp = System.currentTimeMillis();
                }
            }
            if (Math.abs(dir_timestamp - System.currentTimeMillis()) > 20000) {
                if (x > 30.0) {
                    if(Math.abs(turn_timestamp - System.currentTimeMillis()) > 10000) {
                        if (!((int) turn < 30 || (int) turn > 330)) {
                            if (turn < 180)
                                tts.speak("You are deviating from the correct direction. Turn " + (int) turn + " degree towards right.", TextToSpeech.QUEUE_ADD, null);
                            else
                                tts.speak("You are deviating from the correct direction. Turn " + (int) (360 - turn) + " degrees towards left.", TextToSpeech.QUEUE_ADD, null);
                            dir_timestamp = System.currentTimeMillis();
                            orientationCheck = false;
                        }
                    }
                }
            }
            if ((orientationCheck == true) && (abs(tst.get(i) - System.currentTimeMillis()) > 15000)) {
                if(checkWayPart(comp1[0], comp2[0]))
                    tst.set(i,System.currentTimeMillis());
            }

            if ((abs(tst.get(i) - System.currentTimeMillis()) > 90000) && (orientationCheck == true)) {
                if (x > 20.0) {
                    if (x < y/2) {
                        tts.speak("In " + x + " meters " + instructions.get(i), TextToSpeech.QUEUE_ADD, null);
                    } else if (x >= y/2) {
                        tts.speak("Continue straight for another " + x + " meters", TextToSpeech.QUEUE_ADD, null);
                    }
                }
                tst.set(i, System.currentTimeMillis());
            }
        }
        //ToDo instead of 'for loop' check for OsmNexTInstruction closeness
        for (int i = 0; i < landmarks.size(); i++) {
            if (abs(landmarks.get(i).getLatitude() - lat_float) < 0.0001 && abs(landmarks.get(i).getLongitude() - long_float) < 0.0001) {
                if (landmarkCheck.get(i) ==  0) {
                    System.out.println("Lat_diff: " + abs(landmarks.get(i).getLatitude() - lat_float));
                    System.out.println("Long diff: " + abs(landmarks.get(i).getLongitude() - long_float));
                    System.out.println("String found: " + instructions.get(i));
                    tts.speak(instructions.get(i), TextToSpeech.QUEUE_ADD, null);
                    timestamps.set(i, System.currentTimeMillis());
                    currentLandmark = i;
                    landmarkCheck.set(i,1);
                    previous_landmark = ((i == 0) ? startPoint : (new GeoPoint(landmarks.get(i).getLatitude(), landmarks.get(i).getLongitude())));

                    if (i < landmarks.size() - 1) {
                        osmNextInstruction = i + 1;
                        setParts();
                        turn_timestamp = System.currentTimeMillis();
                        tst.set(i+1, System.currentTimeMillis() - 30000);
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
        if (currentLandmark != -1) {
            if (Math.abs(System.currentTimeMillis() - timestamps.get(currentLandmark)) > 5000) {
                for (int j = 0; j < tags.size(); j++) {
                    if (tagLandmark.get(j) == currentLandmark) {
                        if (tagCheck.get(j) == 0) {
                            System.out.println("Lat_diff: " + abs(tags.get(j).getLatitude() - lat_float));
                            System.out.println("Long diff: " + abs(tags.get(j).getLongitude() - long_float));
                            System.out.println("String found: " + tagInstructions.get(j));
                            tts.speak(tagInstructions.get(j), TextToSpeech.QUEUE_ADD, null);
                            tagCheck.set(j, 1);
                        }
                    }
                }
            }
        }
        for (int i = 0; i < saved_landmarks.size(); i++) {
            if (abs(saved_landmarks.get(i).getLatitude() - lat_float) < 0.00005 && abs(saved_landmarks.get(i).getLongitude() - long_float) < 0.00005) {
                if(saved_landmarks_check.get(i) == 0) {
                    System.out.println("Lat_diff: " + abs(saved_landmarks.get(i).getLatitude() - lat_float));
                    System.out.println("Long diff: " + abs(saved_landmarks.get(i).getLongitude() - long_float));
                    //Toast.makeText(getApplicationContext(), saved_instructions.get(i), Toast.LENGTH_LONG).show();
                    System.out.println("String found: " + saved_instructions.get(i));
                    tts.speak(saved_instructions.get(i) + " at current location", TextToSpeech.QUEUE_ADD, null);
                    saved_landmarks_check.set(i, 1);
                }
            }
        }
        if (System.currentTimeMillis() - tracetime > 1000) {
            trackdataSerial++;
            long ttime = (System.currentTimeMillis() - starttime)/1000;
            db.execSQL("INSERT INTO trackData VALUES(NULL, " + (current_lat) + ", " + (current_long) + "," + osmNextInstruction + "," + ttime + ");");
            if(trackdataSerial == 10){
                trackdataSerial = 0;
                db.execSQL("INSERT INTO trackData30 VALUES(NULL, " + (current_lat) + ", " + (current_long) + "," + osmNextInstruction + "," + ttime + ");");
            }
            tracetime = System.currentTimeMillis();
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
//            if ((points2.get(ptr).mLocation.getLatitude() - currentLat) / (landmarks.get(0).getLatitude() - currentLat) > 0) {
//                if ((points2.get(ptr).mLocation.getLongitude() - currentLong) / (landmarks.get(0).getLongitude() - currentLong) > 0) {
        String typeofhighway = points2.get(min_dist_ptr).mType;
        String lanecount = points2.get(min_dist_ptr).mUrl;
        String typeofsurface = points2.get(min_dist_ptr).mDescription;
        String footway = points2.get(min_dist_ptr).mThumbnailPath;
        typeofhighway = typeofhighway.replaceAll("_", " ");
        if (lanecount != null && typeofsurface != null) {
            tts.speak("This is " + typeofhighway + " with " + lanecount + " lanes and " + typeofsurface + " surface.", TextToSpeech.QUEUE_ADD, null);
            // inst = "This is " + typeofhighway + " with " + lanecount + " lanes and " + typeofsurface + " surface";
        } else if (lanecount == null && typeofsurface != null) {
            tts.speak("This is " + typeofhighway + " with " + typeofsurface + " surface.", TextToSpeech.QUEUE_ADD, null);
            //inst = "This is " + typeofhighway + " with " + typeofsurface + " surface";
        } else if (lanecount != null && typeofsurface == null) {
            tts.speak("This is " + typeofhighway + " with " + lanecount + " lanes.", TextToSpeech.QUEUE_ADD, null);
            // inst = "This is " + typeofhighway + " with " + lanecount + " lanes";
        } else if (lanecount == null && typeofsurface == null) {
            tts.speak("This is " + typeofhighway + ".", TextToSpeech.QUEUE_ADD, null);
            //inst = "This is " + typeofhighway;
        }
        if (footway != null) {
            tts.speak("Presence of " + footway + " footway.", TextToSpeech.QUEUE_ADD, null);
            //inst = inst + ". Presence of footway ; " + footway;
        }
    }

    public void onSaveLandmark(View view) {
        Intent i = new Intent(getBaseContext(), AddButton.class);
        startActivityForResult(i, REQ_CODE_SAVE_LANDMARK);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_CODE_SAVE_LANDMARK) {
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
        } else if (requestCode == REQ_CODE_SAVE_ROUTE) {                              // request from
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
        }else if (requestCode == 5) {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> result = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String poi_search = result.get(0);
                searchPOIfromName(poi_search);
            }
        }
    }

    public boolean loadRoute(){


        System.out.println("Loading Route");
        Cursor ec = db.rawQuery("SELECT * FROM routes where name = '" + routeName + "';", null);
        ec.moveToFirst();
        double start_lat = (ec.getInt(3)) / 10000000.0;
        double start_lon = (ec.getInt(4)) / 10000000.0;
        double end_lat = (ec.getInt(5)) / 10000000.0;
        double end_lon = (ec.getInt(6)) / 10000000.0;
        startPoint = new GeoPoint(start_lat,start_lon);
        endPoint = new GeoPoint(end_lat, end_lon);
        Cursor c = db.rawQuery("SELECT * FROM routebyinstructions where routeid = " + selectedRoute + ";", null);
        while (c.moveToNext()) {
            double latitude = Double.parseDouble(c.getString(2)) / 10000000;
            double longitude = Double.parseDouble(c.getString(3)) / 10000000;
            String description = c.getString(4);
            landmarks.add(new GeoPoint(latitude, longitude));
            landmarkCheck.add(0);
            instructions.add(description);
            timestamps.add(new Long(0));
            tst.add(new Long(0));
        }
        System.out.println(landmarks.size());
        Cursor d = db.rawQuery("SELECT * FROM tagroutebyinstructions where routeid = " + selectedRoute + ";", null);
        while (d.moveToNext()) {
            double latitude = Double.parseDouble(d.getString(2)) / 10000000;
            double longitude = Double.parseDouble(d.getString(3)) / 10000000;
            String description = d.getString(4);
            tags.add(new GeoPoint(latitude, longitude));
            tagInstructions.add(description);
            tagCheck.add(0);
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

        changeNavigationLayout();

        GeoPoint startPoint = new GeoPoint(current_lat, current_long);
        previous_landmark = startPoint;
        osmNumInstructions = landmarks.size();
        osmNextInstruction = 0;
        prefetch_nextInstruction = 0;

        tts.speak("Starting Navigation. Your location is " + navigatingDistance + " away.", TextToSpeech.QUEUE_ADD, null);
        headingDirection = getBearingNext();
        System.out.println("heading direction" + headingDirection);
        if (headingDirection < 180 && headingDirection > 10)
            tts.speak("Turn " + (int) headingDirection + " degree towards right.", TextToSpeech.QUEUE_ADD, null);
        else if(headingDirection >= 180 && headingDirection < 350)
            tts.speak("Turn " + (int) (360 - headingDirection) + " degrees towards left.", TextToSpeech.QUEUE_ADD, null);

        turn_timestamp = System.currentTimeMillis();
        tst.set(0, turn_timestamp);
        starttime = System.currentTimeMillis();

        h.postDelayed(localInfoRunnable, delay);

        return true;
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

    public void preplanInstruction(){
        int lsize = landmarks.size();
        String insto;
        float[] res = new float[3];
        if (lsize > 1) {
            Location.distanceBetween(landmarks.get(lsize - 2).getLatitude(), landmarks.get(lsize - 2).getLongitude(), landmarks.get(lsize - 1).getLatitude(), landmarks.get(lsize - 1).getLongitude(), res);
        }
        else{
            Location.distanceBetween(current_lat,current_long, landmarks.get(lsize - 1).getLatitude(), landmarks.get(lsize - 1).getLongitude(), res);
        }
        int intres = (int) res[0];
        if (intres < 1000) {
            insto = "after " + intres + " metres " + instructions.get(lsize-1);
        } else {
            intres = (int)intres/100;
            insto = "after " + ((double)(intres/10)) + " kilometres " + instructions.get(lsize-1);
        }
        instructions.set((lsize-1),insto);
    }

    public void preplanInstruction2(){
        if (xingCnt == 0) {
            preplanInstruction();
        }
        else {
            int lsize = landmarks.size();
            String insto;
            insto = xingStr + " " + instructions.get(lsize - 1);
            instructions.set((lsize - 1), insto);
        }
        xingCnt = 0;
        xingStr = "After ";
    }

    public static String getNumberOrdinal(Number number) {
        if (number == null) {
            return null;
        }

        String format = "{0,ordinal}";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return android.icu.text.MessageFormat.format(format, number);
        } else {
            return com.ibm.icu.text.MessageFormat.format(format, number);
        }
    }

    public void changeNavigationLayout(){
        if(isPreplanning) setContentView(R.layout.activity_path_review);
        else setContentView(R.layout.activity_navigation);
    }

    public void exportDb() {
        try {
            File file = new File(this.getExternalFilesDir(null), "trace1" + UUID.randomUUID().toString() + ".txt");
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

        try {
            File file = new File(this.getExternalFilesDir(null), "trace30" + UUID.randomUUID().toString() + ".txt");
            FileOutputStream fileOutput = new FileOutputStream(file);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutput);
            Cursor c = db.rawQuery("SELECT * from trackData30;", null);
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
            db.execSQL("DELETE from trackData30;");
            db.execSQL("UPDATE SQLITE_SEQUENCE SET SEQ=0 WHERE NAME='trackData30';");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
        Toast.makeText(getApplicationContext(), "Written to file", Toast.LENGTH_LONG).show();


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
        //System.out.println(lat1 + ", " + lat2 + ", " + lon1 + ", " + lon2 + ", " + (return_val/5));
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




    public boolean checkWayPart(float remaining, float total){
        boolean status = true;
        if (remaining < total/4 && remaining > total/6 && part4){
            tts.speak("In " + (int) remaining + " meters " + instructions.get(osmNextInstruction), TextToSpeech.QUEUE_ADD, null);
            part4 = false;

        }
        else if (remaining < total/2 && part3){
            tts.speak("Continue straight for another " + (int) remaining + " meters", TextToSpeech.QUEUE_ADD, null);
            part3 = false;
        }
        else if (remaining < 3*total/4 && part2){
            tts.speak("You are on the correct path. Next turn will be in " + (int)remaining + " meters", TextToSpeech.QUEUE_ADD, null);
            part2 = false;
        }
        else if (remaining < 7*total/8 && part1){
            tts.speak("For " + (int) remaining + " meters, continue straight", TextToSpeech.QUEUE_ADD, null);
            part1 = false;
        }
        else
            status = false;
        return status;
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
        mLocationRequest.setInterval(500); // Update location twice every second

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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


        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
            Log.i("current_lat_lng","Fetching Current LatLong  in navigation " + current_lat + ";" + current_long);
        }
        isConnected = true;
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
        isLocChanged =true;
        current_lat = location.getLatitude();
        current_long = location.getLongitude();
        //Log.i("current_lat_lng","Fetching Current LatLong  in navigation/changed " + current_lat + ";" + current_long);

    }

    public void onNextButton(View view) {
        if (true) { //previously if isNavigating
            if (!faceHeadDirection) {
                double turn = getBearingNext();
                if (turn < 180)
                    tts.speak("Turn " + (int) turn + " degree towards right.", TextToSpeech.QUEUE_ADD, null);
                else
                    tts.speak("Turn " + (int) (360 - turn) + " degrees towards left.", TextToSpeech.QUEUE_ADD, null);
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
        overpassProvider = new OverpassAPIProvider2();
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
                if(i<3){
                    Location.distanceBetween(lat_float, long_float, poi_tags.get(i).getLatitude(), poi_tags.get(i).getLongitude(), results);
                    tts.speak(poi_tagInstructions.get(i) + " at a distance of " + ((int) results[0]) + " metres.", TextToSpeech.QUEUE_ADD, null);
                }
            }
        }
        namePOI.clear();
        poi_tags.clear();
        poi_tagInstructions.clear();
    }

    /*
    public void showDb(View view) {
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
    }*/

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
                tts.speak("heading direction is " + (int) headingDirection + " degree towards right.", TextToSpeech.QUEUE_ADD, null);
            else if(headingDirection >= 180 && headingDirection < 360)
                tts.speak("heading direction is " + (int) (360 - headingDirection) + " degrees towards left.", TextToSpeech.QUEUE_ADD, null);
        }
    }


    public void next (View v){
        if(instid < instructions.size()) {
            String insto = makeInstruction(instid);
            tts.speak(insto, TextToSpeech.QUEUE_ADD, null);
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
    public String makeInstruction(int instid){
        String ret = "Walk straight";

        ret = ret + " then " + instructions.get(instid);
        return ret;
    }

}
