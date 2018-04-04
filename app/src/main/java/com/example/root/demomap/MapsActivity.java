package com.example.root.demomap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.InputStream;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.android.gms.location.Geofence.NEVER_EXPIRE;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    private GoogleMap mMap;
    private Location location;
    private GPSTracker gps;
    private double my_longitude = 0.0;
    private double my_latitude = 0.0;
    private double altitude;
    static Thread t;
    static double result;
    private TextView tv_latitude;
    private Button btn_getlatitude, btn_clockin;
    double mainLatitude;
    private boolean statusOfButton = false;
    private GeofencingClient mgeofencingClient;
    List<Geofence> geofence=new ArrayList<>();
    PendingIntent mGeofencePendingIntent;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        tv_latitude = findViewById(R.id.tv_latitude);
        btn_getlatitude = findViewById(R.id.btn_getlatitude);
        btn_clockin = findViewById(R.id.btn_clockin);
        btn_getlatitude.setOnClickListener(this);
        btn_clockin.setOnClickListener(this);
        mgeofencingClient= LocationServices.getGeofencingClient(this);


//        mgeofencingClient.removeGeofences(getGeofencePendingIntent())
//                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
//                    @Override
//                    public void onSuccess(Void aVoid) {
//                        // Geofences removed
//                        // ...
//                    }
//                })
//                .addOnFailureListener(this, new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        // Failed to remove geofences
//                        // ...
//                    }
//                });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
            } else {
                setCurrentMarker(mMap);

            }
        } else {
            setCurrentMarker(mMap);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0) {
            if (grantResults.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    setCurrentMarker(mMap);
                } else {
                    Toast.makeText(MapsActivity.this, "Please give permission to set current marker", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void setCurrentMarker(GoogleMap map) {
        if (mMap == null)
            return;
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            return;
        }
        location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        gps = new GPSTracker(MapsActivity.this);
        if (gps.canGetLocation) {
            my_longitude = gps.getLongitude();
            my_latitude = gps.getLatitude();
            LatLng currentlatlang = new LatLng(my_latitude, my_longitude);
//            ProgressMessages progressMessages=new ProgressMessages(my_latitude,my_longitude);
//            Double latitude=progressMessages.doNetworkStuff();
//            GetAltitude getAltitude = new GetAltitude(my_latitude, my_longitude);
//            getAltitude.execute();
            setGeofence();

            mMap.addMarker(new MarkerOptions().position(currentlatlang).title("Marker in Current Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentlatlang));
            CameraUpdate zoom = CameraUpdateFactory.zoomTo(16);
            mMap.animateCamera(zoom);
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            mgeofencingClient.addGeofences(getGeofencingRequest(),getGeofencePendingIntent())
                    .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(getApplicationContext(),"GeoFence Added",Toast.LENGTH_SHORT).show();
                            Intent intent=new Intent(MapsActivity.this,GeofenceTransitionsIntentService.class);
                            startService(intent);
                        }
                    })
                    .addOnFailureListener(this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getApplicationContext(),"GeoFence not Added",Toast.LENGTH_SHORT).show();
                        }
                    });



        }

    }

    private double getAltitude(Double longitude, Double latitude) {
        double result = Double.NaN;
        HttpClient httpClient = new DefaultHttpClient();
        HttpContext localContext = new BasicHttpContext();
        String url = "http://maps.googleapis.com/maps/api/elevation/"
                + "xml?locations=" + String.valueOf(latitude)
                + "," + String.valueOf(longitude)
                + "&sensor=true";
        HttpGet httpGet = new HttpGet(url);
        try {
            HttpResponse response = httpClient.execute(httpGet, localContext);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                int r = -1;
                StringBuffer respStr = new StringBuffer();
                while ((r = instream.read()) != -1)
                    respStr.append((char) r);
                String tagOpen = "<elevation>";
                String tagClose = "</elevation>";
                if (respStr.indexOf(tagOpen) != -1) {
                    int start = respStr.indexOf(tagOpen) + tagOpen.length();
                    int end = respStr.indexOf(tagClose);
                    String value = respStr.substring(start, end);
                    result = (double) (Double.parseDouble(value) * 3.2808399);


                }
                instream.close();
            }
        } catch (ClientProtocolException e) {
        } catch (IOException e) {
        }

        return result;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_getlatitude:
                Appdata appdata=new Appdata();

                Double mainreaule=appdata.getLatitude();
                tv_latitude.setText(""+mainreaule);
                if(mainreaule!=null) {
                    mainLatitude = round(mainreaule, 1);

                    String[] arr = String.valueOf(mainLatitude).split("\\.");
                    int[] intArr = new int[2];
                    intArr[0] = Integer.parseInt(arr[0]);
                    intArr[1] = Integer.parseInt(arr[1]);

                    if (intArr[0] == 9 && intArr[1] <= 9) {
                        // Toast.makeText(getApplicationContext(),""+mainLatitude,Toast.LENGTH_SHORT).show();
                        if (!tv_latitude.getText().toString().equals("NAN")) {
                            btn_clockin.setEnabled(true);
                            // Toast.makeText(getApplicationContext(),""+mainLatitude,Toast.LENGTH_SHORT).show();
                            btn_clockin.setBackgroundColor(getResources().getColor(R.color.newbutton));
                            statusOfButton = true;
                        }
                    }
                }
                setCurrentMarker(mMap);
                break;
            case R.id.btn_clockin:

                if (!statusOfButton) {
                    Toast.makeText(getApplicationContext(), "Clocked out", Toast.LENGTH_LONG).show();
                    btn_clockin.setEnabled(false);
                    btn_clockin.setBackgroundColor(getResources().getColor(R.color.button));

                }
                if (statusOfButton) {
                    Toast.makeText(getApplicationContext(), "Clocked IN", Toast.LENGTH_LONG).show();
                    btn_clockin.setBackgroundColor(getResources().getColor(R.color.redButton));
                    btn_clockin.setText("Clock Out");
                    statusOfButton = false;
                }
                break;
        }
    }

    class GetAltitude extends AsyncTask<Void, Void, Double> {
        Double latitude, longitude;

        public GetAltitude(Double latitude, Double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        @Override
        protected Double doInBackground(Void... voids) {
            try {

                // call you method here
                Double altutide = getAltitude(longitude, latitude);
                Appdata appdata=new Appdata();
                appdata.setLatitude(altutide);

                return altutide;

            } catch (Exception ex) {
                // handle the exception here
                Log.e("EXCEPTION",""+ex);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Double aDouble) {
            super.onPostExecute(aDouble);
        }
    }

    public void setGeofence()
    {
        geofence.add(new Geofence.Builder()
                .setRequestId("ajit")
                .setCircularRegion(22.624026, 88.450101,800)
                .setExpirationDuration(10*1000)
                .setLoiteringDelay(5000)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build());
        CircleOptions circleOptions = new CircleOptions()
                .center( new LatLng(22.624026, 88.450101) )
                .radius(800)
                .fillColor(0x40ff0000)
                .strokeColor(Color.TRANSPARENT)
                .strokeWidth(2);
        Circle circle = mMap.addCircle(circleOptions);



    }

    public GeofencingRequest getGeofencingRequest()
    {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofence);
        return builder.build();
    }
    private PendingIntent getGeofencePendingIntent() {
        // Reuse t PendingIntent if we already have it.

        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        mGeofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

}


