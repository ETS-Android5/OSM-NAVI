package khushboo.rohit.osmnavi;

/**
 * Created by hp pc on 11-04-2018.
 */

import android.util.Log;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import org.osmdroid.bonuspack.kml.KmlFolder;
import org.osmdroid.bonuspack.kml.KmlGeometry;
import org.osmdroid.bonuspack.kml.KmlLineString;
import org.osmdroid.bonuspack.kml.KmlMultiGeometry;
import org.osmdroid.bonuspack.kml.KmlPlacemark;
import org.osmdroid.bonuspack.kml.KmlPoint;
import org.osmdroid.bonuspack.kml.KmlPolygon;
import org.osmdroid.bonuspack.location.POI;
import org.osmdroid.bonuspack.utils.BonusPackHelper;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;

public class OverpassAPIProvider2 {
    public static final String OVERPASS_API_DE_SERVICE = "https://overpass-api.de/api/interpreter";
    public static final String OVERPASS_API_SERVICE = "https://api.openstreetmap.fr/oapi/interpreter";
    protected String mService;

    public OverpassAPIProvider2() {
        this.setService("https://overpass-api.de/api/interpreter");
    }

    public void setService(String serviceUrl) {
        this.mService = serviceUrl;
    }

    public String urlForPOISearch(String tag, BoundingBox bb, int limit, int timeout) {
        StringBuilder s = new StringBuilder();
        s.append(this.mService + "?data=");
        String sBB = "(" + bb.getLatSouth() + "," + bb.getLonWest() + "," + bb.getLatNorth() + "," + bb.getLonEast() + ")";
        String data = "[out:json][timeout:" + timeout + "];(node[" + tag + "]" + sBB + ";way[" + tag + "]" + sBB + ";relation[" + tag + "]" + sBB + ";);out qt center " + limit + " tags;";
        Log.d("BONUSPACK", "data=" + data);
        s.append(URLEncoder.encode(data));
        return s.toString();
    }

    protected GeoPoint geoPointFromJson(JsonObject jLatLon) {
        double lat = jLatLon.get("lat").getAsDouble();
        double lon = jLatLon.get("lon").getAsDouble();
        return new GeoPoint(lat, lon);
    }

    protected String tagValueFromJson(String key, JsonObject jTags) {
        JsonElement jTag = jTags.get(key);
        return jTag == null?null:jTag.getAsString();
    }

    protected String tagValueFromJsonNotNull(String key, JsonObject jTags) {
        String v = this.tagValueFromJson(key, jTags);
        return v != null?"," + v:"";
    }

    public ArrayList<POI> getPOIsFromUrl(String url) {
        Log.d("BONUSPACK", "OverpassAPIProvider:getPOIsFromUrl:" + url);
        String jString = BonusPackHelper.requestStringFromUrl(url);
        if(jString == null) {
            Log.e("BONUSPACK", "OverpassAPIProvider: request failed.");
            return null;
        } else {
            try {
                JsonParser parser = new JsonParser();
                JsonElement json = parser.parse(jString);
                JsonObject jResult = json.getAsJsonObject();
                JsonArray jElements = jResult.get("elements").getAsJsonArray();
                ArrayList<POI> pois = new ArrayList(jElements.size());
                Iterator var8 = jElements.iterator();

                while(var8.hasNext()) {
                    JsonElement j = (JsonElement)var8.next();
                    JsonObject jo = j.getAsJsonObject();
                    POI poi = new POI(POI.POI_SERVICE_OVERPASS_API);
                    poi.mId = jo.get("id").getAsLong();
                    poi.mCategory = jo.get("type").getAsString();
                    JsonObject jCenter;
                    if(jo.has("tags")) {
                        jCenter = jo.get("tags").getAsJsonObject();
                        poi.mType = this.tagValueFromJson("name", jCenter);
                        poi.mDescription = this.tagValueFromJsonNotNull("amenity", jCenter);

                        poi.mUrl = this.tagValueFromJson("website", jCenter);
                        if(poi.mUrl != null && !poi.mUrl.startsWith("http://") && !poi.mUrl.startsWith("https://")) {
                            poi.mUrl = "http://" + poi.mUrl;
                        }
                    }

                    if("node".equals(poi.mCategory)) {
                        poi.mLocation = this.geoPointFromJson(jo);
                    } else if(jo.has("center")) {
                        jCenter = jo.get("center").getAsJsonObject();
                        poi.mLocation = this.geoPointFromJson(jCenter);
                    }

                    if(poi.mLocation != null) {
                        pois.add(poi);
                    }
                }

                return pois;
            } catch (JsonSyntaxException var13) {
                Log.e("BONUSPACK", "OverpassAPIProvider: parsing error.");
                return null;
            }
        }
    }

    public String urlForTagSearchKml(String tag, BoundingBox bb, int limit, int timeout) {
        StringBuilder s = new StringBuilder();
        s.append(this.mService + "?data=");
        String sBB = "(" + bb.getLatSouth() + "," + bb.getLonWest() + "," + bb.getLatNorth() + "," + bb.getLonEast() + ")";
        String data = "[out:json][timeout:" + timeout + "];(node[" + tag + "]" + sBB + ";way[" + tag + "]" + sBB + ";);out qt geom tags " + limit + ";relation[" + tag + "]" + sBB + ";out qt geom body " + limit + ";";
        Log.d("BONUSPACK", "data=" + data);
        s.append(URLEncoder.encode(data));
        return s.toString();
    }

    protected boolean isAnArea(ArrayList<GeoPoint> coords) {
        return coords != null && coords.size() >= 3 && ((GeoPoint)coords.get(0)).equals(coords.get(coords.size() - 1));
    }

    protected ArrayList<GeoPoint> parseGeometry(JsonObject jo) {
        JsonArray jGeometry = jo.get("geometry").getAsJsonArray();
        ArrayList<GeoPoint> coords = new ArrayList(jGeometry.size());
        Iterator var4 = jGeometry.iterator();

        while(var4.hasNext()) {
            JsonElement j = (JsonElement)var4.next();
            JsonObject jLatLon = j.getAsJsonObject();
            GeoPoint p = this.geoPointFromJson(jLatLon);
            coords.add(p);
        }

        return coords;
    }

    protected KmlMultiGeometry buildMultiGeometry(JsonArray jMembers) {
        KmlMultiGeometry geometry = new KmlMultiGeometry();
        Iterator var3 = jMembers.iterator();

        while(var3.hasNext()) {
            JsonElement j = (JsonElement)var3.next();
            JsonObject jMember = j.getAsJsonObject();
            KmlGeometry item = this.buildGeometry(jMember);
            geometry.addItem(item);
        }

        return geometry;
    }

    protected KmlGeometry buildGeometry(JsonObject jo) {
        String type = jo.get("type").getAsString();
        Object geometry;
        if("node".equals(type)) {
            geometry = new KmlPoint(this.geoPointFromJson(jo));
        } else if("way".equals(type)) {
            ArrayList<GeoPoint> coords = this.parseGeometry(jo);
            if(this.isAnArea(coords)) {
                geometry = new KmlPolygon();
                ((KmlGeometry)geometry).mCoordinates = coords;
            } else {
                geometry = new KmlLineString();
                ((KmlGeometry)geometry).mCoordinates = coords;
            }
        } else {
            JsonArray jMembers = jo.get("members").getAsJsonArray();
            geometry = this.buildMultiGeometry(jMembers);
        }

        return (KmlGeometry)geometry;
    }

    public boolean addInKmlFolder(KmlFolder kmlFolder, String url) {
        Log.d("BONUSPACK", "OverpassAPIProvider:addInKmlFolder:" + url);
        String jString = BonusPackHelper.requestStringFromUrl(url);
        if(jString == null) {
            Log.e("BONUSPACK", "OverpassAPIProvider: request failed.");
            return false;
        } else {
            try {
                JsonParser parser = new JsonParser();
                JsonElement json = parser.parse(jString);
                JsonObject jResult = json.getAsJsonObject();
                JsonArray jElements = jResult.get("elements").getAsJsonArray();

                KmlPlacemark placemark;
                for(Iterator var8 = jElements.iterator(); var8.hasNext(); kmlFolder.add(placemark)) {
                    JsonElement j = (JsonElement)var8.next();
                    JsonObject jo = j.getAsJsonObject();
                    placemark = new KmlPlacemark();
                    placemark.mGeometry = this.buildGeometry(jo);
                    placemark.mId = jo.get("id").getAsString();
                    if(jo.has("tags")) {
                        JsonObject jTags = jo.get("tags").getAsJsonObject();
                        if(jTags.has("name")) {
                            placemark.mName = jTags.get("name").getAsString();
                        }

                        Set<Entry<String, JsonElement>> entrySet = jTags.entrySet();
                        Iterator var14 = entrySet.iterator();

                        while(var14.hasNext()) {
                            Entry<String, JsonElement> entry = (Entry)var14.next();
                            String key = (String)entry.getKey();
                            String value = ((JsonElement)entry.getValue()).getAsString();
                            placemark.setExtendedData(key, value);
                        }
                    }
                }

                return true;
            } catch (JsonSyntaxException var18) {
                Log.e("BONUSPACK", "OverpassAPIProvider: parsing error.");
                return false;
            }
        }
    }
    public ArrayList<POI> getPOIsFromUrlRoad(String url) {
        Log.d("BONUSPACK", "OverpassAPIProvider:getPOIsFromUrl:" + url);
        String jString = BonusPackHelper.requestStringFromUrl(url);
        if(jString == null) {
            Log.e("BONUSPACK", "OverpassAPIProvider: request failed.");
            return null;
        } else {
            try {
                JsonParser parser = new JsonParser();
                JsonElement json = parser.parse(jString);
                JsonObject jResult = json.getAsJsonObject();
                JsonArray jElements = jResult.get("elements").getAsJsonArray();
                ArrayList<POI> pois = new ArrayList(jElements.size());
                Iterator var8 = jElements.iterator();

                while(var8.hasNext()) {
                    JsonElement j = (JsonElement)var8.next();
                    JsonObject jo = j.getAsJsonObject();
                    POI poi = new POI(POI.POI_SERVICE_OVERPASS_API);
                    poi.mId = jo.get("id").getAsLong();
                    poi.mCategory = jo.get("type").getAsString();
                    JsonObject jCenter;
                    if(jo.has("tags")) {
                        jCenter = jo.get("tags").getAsJsonObject();
                        poi.mType = this.tagValueFromJson("name", jCenter);                    // name of road
                        if(poi.mType == null) {
                            poi.mType = this.tagValueFromJson("highway", jCenter); // type of road if name unspecified
                            poi.mType = "a " + poi.mType + " road";
                        }
                        poi.mThumbnailPath = this.tagValueFromJson("footway", jCenter);
                        poi.mDescription = this.tagValueFromJsonNotNull("surface", jCenter);   // surface type
                        poi.mUrl = this.tagValueFromJson("lanes", jCenter);                    // no. of lanes
                    }

                    if("node".equals(poi.mCategory)) {
                        poi.mLocation = this.geoPointFromJson(jo);
                    } else if(jo.has("center")) {
                        jCenter = jo.get("center").getAsJsonObject();
                        poi.mLocation = this.geoPointFromJson(jCenter);
                    }

                    if(poi.mLocation != null) {
                        pois.add(poi);
                    }
                }

                return pois;
            } catch (JsonSyntaxException var13) {
                Log.e("BONUSPACK", "OverpassAPIProvider: parsing error.");
                return null;
            }
        }
    }

    public ArrayList<POI> getPOIsFromUrlNode(String url) {
        Log.d("BONUSPACK", "OverpassAPIProvider:getPOIsFromUrl:" + url);
        String jString = BonusPackHelper.requestStringFromUrl(url);
        if(jString == null) {
            Log.e("BONUSPACK", "OverpassAPIProvider: request failed.");
            return null;
        } else {
            try {
                JsonParser parser = new JsonParser();
                JsonElement json = parser.parse(jString);
                JsonObject jResult = json.getAsJsonObject();
                JsonArray jElements = jResult.get("elements").getAsJsonArray();
                ArrayList<POI> pois = new ArrayList(jElements.size());
                Iterator var8 = jElements.iterator();

                while(var8.hasNext()) {
                    JsonElement j = (JsonElement)var8.next();
                    JsonObject jo = j.getAsJsonObject();
                    POI poi = new POI(POI.POI_SERVICE_OVERPASS_API);
                    poi.mId = jo.get("id").getAsLong();
                    if(jo.get("crossing")!= null) {
                        poi.mCategory = jo.get("crossing").getAsString();   // type of element
                    }
                                                   // type of element
                    JsonObject jCenter;
                    if("node".equals(poi.mCategory)) {
                        if(jo.has("tags")) {
                            jCenter = jo.get("tags").getAsJsonObject();
                            poi.mType = this.tagValueFromJson("highway", jCenter); // type of node
                            poi.mDescription = this.tagValueFromJsonNotNull("crossing", jCenter);   // type of crossing
                        }
                        poi.mLocation = this.geoPointFromJson(jo);
                    } else if(jo.has("center")) {
                        jCenter = jo.get("center").getAsJsonObject();
                        poi.mLocation = this.geoPointFromJson(jCenter);
                    }

                    if(poi.mLocation != null) {
                        pois.add(poi);
                    }
                }

                return pois;
            } catch (JsonSyntaxException var13) {
                Log.e("BONUSPACK", "OverpassAPIProvider: parsing error.");
                return null;
            }
        }
    }
}
