package co.edu.unal.biketrainer.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Random;

import co.edu.unal.biketrainer.model.Route;
import timber.log.Timber;


public class Utils {

    public static final String PRIMARY_ROUTE_BUNDLE_KEY = "myPrimaryRouteBundleKey";

    /**
     * <p>
     * Returns the Mapbox access token set in the app resources.
     * </p>
     * It will first search for a token in the Mapbox object. If not found it
     * will then attempt to load the access token from the
     * {@code res/values/dev.xml} development file.
     *
     * @param context The {@link Context} of the {@link android.app.Activity} or {@link android.app.Fragment}.
     * @return The Mapbox access token or null if not found.
     */
    public static String getMapboxAccessToken(@NonNull Context context) {
        try {
            // Read out AndroidManifest
            String token = Mapbox.getAccessToken();
            if (token == null || token.isEmpty()) {
                throw new IllegalArgumentException();
            }
            return token;
        } catch (Exception exception) {
            // Use fallback on string resource, used for development
            int tokenResId = context.getResources()
                    .getIdentifier("mapbox_access_token", "string", context.getPackageName());
            return tokenResId != 0 ? context.getString(tokenResId) : null;
        }
    }

    /**
     * Demonstrates converting any Drawable to an Icon, for use as a marker icon.
     */
    public static Icon drawableToIcon(@NonNull Context context, @DrawableRes int id) {
        Drawable vectorDrawable = ResourcesCompat.getDrawable(context.getResources(), id, context.getTheme());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return IconFactory.getInstance(context).fromBitmap(bitmap);
    }

    public static LatLng getRandomLatLng(double[] bbox) {
        Random random = new Random();

        double randomLat = bbox[1] + (bbox[3] - bbox[1]) * random.nextDouble();
        double randomLon = bbox[0] + (bbox[2] - bbox[0]) * random.nextDouble();

        LatLng latLng = new LatLng(randomLat, randomLon);
//        MapboxLogger.INSTANCE.d(new Message("getRandomLatLng: " + latLng.toString()));
        return latLng;
    }

    /**
     * Used by the example activities to get a DirectionsRoute from a bundle.
     *
     * @param bundle to get the DirectionsRoute from
     * @return a DirectionsRoute or null
     */
    public static DirectionsRoute getRouteFromBundle(Bundle bundle) {
        try {
            if (bundle.containsKey(PRIMARY_ROUTE_BUNDLE_KEY)) {
                String routeAsJson = bundle.getString(PRIMARY_ROUTE_BUNDLE_KEY);
                return DirectionsRoute.fromJson(routeAsJson);
            }
        } catch (Exception ex) {
            Timber.i(ex);
        }
        return null;
    }

    public static Location getLocationFromJson(JsonObject json) {
        Location location = new Location(json.get("provider").getAsString());
        location.setAltitude(json.get("altitude").getAsDouble());
        location.setLongitude
                (json.get("longitude").getAsDouble());
        location.setLatitude
                (json.get("latitude").getAsDouble());
        location.setTime
                (json.get("time").getAsLong());
        location.setAccuracy
                (json.get("accuracy").getAsFloat());
        location.setBearing
                (json.get("bearing").getAsFloat());
        location.setSpeed
                (json.get("speed").getAsFloat());
        return location;
    }

    public static Route getRouteFromDocumentSnap(DocumentSnapshot documentSnapshot) {
        Route route = null;
        if (documentSnapshot != null) {
            route = new Route();
            route.setLevel(documentSnapshot.getData().get("level").toString());
            route.setId(documentSnapshot.getId());
            route.setAverage_duration(new Long(documentSnapshot.getData().get("average_duration").toString()));
            route.setComments(documentSnapshot.getData().get("comments").toString());
            route.setCreated_at((Timestamp) documentSnapshot.getData().get("created_at"));
            route.setName(documentSnapshot.getData().get("name").toString());
            route.setCreated_by(documentSnapshot.getData().get("created_by").toString());
            route.setDestination(getLocationFromJson((JsonObject) new Gson().toJsonTree(documentSnapshot.getData().get("destination"))));
            route.setOrigin(getLocationFromJson((JsonObject) new Gson().toJsonTree(documentSnapshot.getData().get("origin"))));
            ArrayList<Location> routeRoute = new ArrayList<>();
            JsonArray locations = (JsonArray) new Gson().toJsonTree(documentSnapshot.getData().get("route"));
            for (JsonElement location : locations) {
                routeRoute.add(Utils.getLocationFromJson(location.getAsJsonObject()));
            }
            route.setDistance(new Float(documentSnapshot.getData().get("distance").toString()));
            route.setRoute(routeRoute);
            route.setSecurity(new Float(documentSnapshot.getData().get("security").toString()));
            route.setVisitors(new Integer(documentSnapshot.getData().get("visitors").toString()));
        }
        return route;
    }

    @Nullable
    public static Route getRouteFromJson(@NotNull JsonObject json) {
        Route route = new Route();
        route = new Route();

        route.setLevel(json.get("level").toString());
        route.setId(json.get("id").toString());
        route.setAverage_duration(new Long(json.get("average_duration").toString()));
        route.setComments(json.get("comments").toString());
        route.setCreated_at(new Timestamp(json.get("created_at").getAsJsonObject().get("seconds").getAsLong(), json.get("created_at").getAsJsonObject().get("nanoseconds").getAsInt()));
        route.setName(json.get("name").toString());
        route.setCreated_by(json.get("created_by").toString());
        route.setDestination(getLocationFromJson((JsonObject) new Gson().toJsonTree(json.get("destination"))));
        route.setOrigin(getLocationFromJson((JsonObject) new Gson().toJsonTree(json.get("origin"))));
        ArrayList<Location> routeRoute = new ArrayList<>();
        JsonArray locations = (JsonArray) new Gson().toJsonTree(json.get("route"));
        for (JsonElement location : locations) {
            routeRoute.add(Utils.getLocationFromJson(location.getAsJsonObject()));
        }
        route.setDistance(new Float(json.get("distance").toString()));
        route.setRoute(routeRoute);
        route.setSecurity(new Float(json.get("security").toString()));
        route.setVisitors(new Integer(json.get("visitors").toString()));
        return route;
    }
}