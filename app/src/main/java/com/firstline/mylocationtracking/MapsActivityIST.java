package com.firstline.mylocationtracking;


        import android.Manifest;
        import android.content.Intent;
        import android.content.SharedPreferences;
        import android.content.pm.ActivityInfo;
        import android.content.pm.PackageManager;
        import androidx.core.app.ActivityCompat;
        import androidx.appcompat.app.AppCompatActivity;
        import android.location.Location;
        import android.net.wifi.WifiManager;
        import android.os.AsyncTask;
        import android.os.Build;
        import android.os.Bundle;
        import android.provider.Settings;
        import android.telephony.TelephonyManager;
        import android.text.format.Formatter;
        import android.widget.TextView;
        import android.widget.Toast;
        import com.google.android.gms.maps.model.LatLng;

//        import org.apache.http.conn.util.InetAddressUtils;
        import org.json.JSONArray;
        import org.json.JSONException;
        import org.json.JSONObject;

        import java.net.Inet4Address;
        import java.net.InetAddress;
        import java.net.NetworkInterface;
        import java.util.Collections;
        import java.util.List;
        import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.conn.util.InetAddressUtils;

        import webservice.WebService;

public class MapsActivityIST extends AppCompatActivity {

    private LOCGpsTracker LOCGpsTracker;
    private LatLng latLng;
    private String strGPSCoordinates = "";
    private String strAltGPSCoordinates = "";
    private double ldblRadius = 0.00, ldblGPSCoordinates1 = 0.00, ldblGPSCoordinates2 = 0.00;
    private double ldblAltRadius = 0.00, ldblAltGPSCoordinates1 = 0.00, ldblAltGPSCoordinates2 = 0.00;
    private TextView tvResult;
    TelephonyManager telephonyManager;
    private String strNetId = "";
    private String strPassword = "";
    private String strGpsCoordinates = "";
    private static String strParameters[];
    private static String ResultString = "";
//    final SharedPreferences loginsession = getApplicationContext().getSharedPreferences("SessionLogin", 0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Make to run your application only in portrait mode
        setContentView(R.layout.landingpage_loc);
        tvResult = findViewById(R.id.tvInformation);
        tvResult.setText("Wait authenticate process going on ...........");
        final SharedPreferences loginsession = getApplicationContext().getSharedPreferences("SessionLogin", 0);

        try {
            strNetId = loginsession.getString("netid", "");
            strPassword = loginsession.getString("pwd", "");
            strGPSCoordinates = loginsession.getString("gpscoordinate", "");
            strAltGPSCoordinates = loginsession.getString("alternategpscoordinate", "");
            String[] strColumns = strGPSCoordinates.split(",");
            ldblGPSCoordinates1 = Double.parseDouble(strColumns[0].toString());
            ldblGPSCoordinates2 = Double.parseDouble(strColumns[1].toString());

            strColumns = strAltGPSCoordinates.split(",");
            ldblAltGPSCoordinates1 = Double.parseDouble(strColumns[0].toString());
            ldblAltGPSCoordinates2 = Double.parseDouble(strColumns[1].toString());
            ldblRadius = Double.parseDouble(loginsession.getString("radius", "1.00"));
            ldblAltRadius = Double.parseDouble(loginsession.getString("alternateradius", "1.00"));
        }catch(Exception e){
            authenticateUser();
        }

        try {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PackageManager.PERMISSION_GRANTED);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PackageManager.PERMISSION_GRANTED);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PackageManager.PERMISSION_GRANTED);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, PackageManager.PERMISSION_GRANTED);

            if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ){
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }
            else{
                getLocation();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static String getMobileIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if(!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        if (!addr.isLoopbackAddress()) {
                            String sAddr = addr.getHostAddress().toUpperCase();
                            boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                            if (useIPv4) {
                                if (isIPv4)
                                    return sAddr;
                            } else {
                                if (!isIPv4) {
                                    // drop ip6 port suffix
                                    int delim = sAddr.indexOf('%');
                                    return delim < 0 ? sAddr : sAddr.substring(0, delim);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }

    public void authenticateUser(){
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String strIpAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        if (strIpAddress.equals("")){
            strIpAddress = getMobileIPAddress(true);
        }
        telephonyManager = (TelephonyManager) getSystemService(this.TELEPHONY_SERVICE);
        String strDeviceModel = Build.MODEL;
        String strDeviceBrand = Build.BRAND;
        String imeiNumber = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);
        try {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ){
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }
            else{
                LOCGpsTracker = new LOCGpsTracker(MapsActivityIST.this);
                if (LOCGpsTracker.canGetLocation()) {
                    double latitude = LOCGpsTracker.getLatitude();
                    double longitude = LOCGpsTracker.getLongitude();
                    strGpsCoordinates = latitude + "," + longitude;
                }
                LOCGpsTracker.stopUsingGPS();
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        strParameters = new String[]{"String", "netid", strNetId, "String", "password", strPassword,
                "String","mobilemodel",strDeviceModel + " " + strDeviceBrand,
                "String","ipaddress",strIpAddress,"String","gpscoordinates",strGpsCoordinates,
                "String","deviceUID",imeiNumber};
        new Thread(new Runnable() {
            public void run() {
                WebService.strParameters = strParameters;
                WebService.METHOD_NAME = "AuthenticateUser";
                AsyncCallWS task = new AsyncCallWS();
                task.execute();
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, LOCLandingPage.class);
        startActivity(intent);
        // finish the activity picture
        this.finish();
    }

    public void getLocation(){  //View view
        final SharedPreferences loginsession = getApplicationContext().getSharedPreferences("SessionLogin", 0);
        strGPSCoordinates = loginsession.getString("gpscoordinate", "");
        strAltGPSCoordinates = loginsession.getString("alternategpscoordinate", "");
        String[] strColumns = strGPSCoordinates.split(",");
        ldblGPSCoordinates1 = Double.parseDouble(strColumns[0].toString());
        ldblGPSCoordinates2 = Double.parseDouble(strColumns[1].toString());

        strColumns = strAltGPSCoordinates.split(",");
        ldblAltGPSCoordinates1 = Double.parseDouble(strColumns[0].toString());
        ldblAltGPSCoordinates2 = Double.parseDouble(strColumns[1].toString());
        ldblRadius = Double.parseDouble(loginsession.getString("radius", "1.00"));
        ldblAltRadius = Double.parseDouble(loginsession.getString("alternateradius", "1.00"));

        LOCGpsTracker = new LOCGpsTracker(MapsActivityIST.this);
        if (LOCGpsTracker.canGetLocation()) {
            double latitude = LOCGpsTracker.getLatitude();
            double longitude = LOCGpsTracker.getLongitude();
            LOCGpsTracker.stopUsingGPS();
//            SharedPreferences loginsession = getApplicationContext().getSharedPreferences("SessionLogin", 0);
            SharedPreferences.Editor ed = loginsession.edit();
            ed.putString("gpscoordinatescurrposition",latitude + "," + longitude);
            ed.commit();

            LatLng srm = new LatLng(ldblGPSCoordinates1, ldblGPSCoordinates2);
            LatLng srmAlt = new LatLng(ldblAltGPSCoordinates1, ldblAltGPSCoordinates2);
            float[] distances = new float[1];
//                    Location.distanceBetween(12.99170, 80.20518, // Nandanam Arts college
            Location.distanceBetween(ldblGPSCoordinates1, ldblGPSCoordinates2,
                    latitude, longitude,distances);
            float[] altdistances = new float[1];
//                    Location.distanceBetween(12.99170, 80.20518, // Nandanam Arts college
            Location.distanceBetween(ldblAltGPSCoordinates1, ldblAltGPSCoordinates2,
                    latitude, longitude,altdistances);
            double radiusInMeters = ldblRadius; // * 1000.0; // 80.0*1000.0; //1 KM = 1000 Meter
            double AltradiusInMeters = ldblAltRadius; // * 1000.0; // 80.0*1000.0; //1 KM = 1000 Meter
            if ( distances[0] > radiusInMeters ){
//                System.out.println("Outside, distance from center: " + distances[0] + " radius: " + radiusInMeters);
//                Toast.makeText(getBaseContext(),
//                        "Not within the range to punch attendance, distance in metre: " + distances[0], // + " radius: " + radiusInMeters,
//                        Toast.LENGTH_LONG).show();
                if (altdistances[0] > AltradiusInMeters){
//                    System.out.println("Outside, distance from center: " + altdistances[0] + " radius: " + AltradiusInMeters);
//                    tvResult.setText("Not within the range to punch attendance, distance in metre: " + altdistances[0]);
//                    Toast.makeText(getBaseContext(),"Not within the range to punch attendance, distance in metre: " + altdistances[0] , //+ " radius: " + AltradiusInMeters,
//                            Toast.LENGTH_LONG).show();
                    final Intent data = new Intent(getApplicationContext(), LOCLandingPage.class);
                    String text = "Not within the range to punch attendance. (" + altdistances[0] + " metre away)";
                    data.putExtra("ReturnMessage",text);
                    startActivity(data);
//---close the activity---
                    finish();
                }
                else{
                    System.out.println("Inside, distance from center: " + altdistances[0] + " radius: " + AltradiusInMeters);
                    Intent intent = new Intent(getApplicationContext(), MainActivityIST.class);
                    startActivity(intent);
                }
            } else {
                System.out.println("Inside, distance from center: " + distances[0] + " radius: " + radiusInMeters);
                Intent intent = new Intent(getApplicationContext(), MainActivityIST.class);
                startActivity(intent);
            }
        }else{
            LOCGpsTracker.showSettingsAlert();
        }
    }

    private class AsyncCallWS extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute(){
//            Log.i(TAG, "onPreExecute");
        }

        @Override
        protected Void doInBackground(Void... params) {
//            Log.i(TAG, "doInBackground");
            if(android.os.Debug.isDebuggerConnected())
                android.os.Debug.waitForDebugger();
            ResultString = WebService.invokeWSIST();
            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            SharedPreferences loginsession = getApplicationContext().getSharedPreferences("SessionLogin", 0);
            SharedPreferences.Editor ed = loginsession.edit();
            try{
                JSONObject object = new JSONObject(ResultString.toString());
                if (object.getString("Status").equals("Success")) {
                    JSONArray data = (JSONArray) object.get("Data");
                    JSONObject datainner = data.getJSONObject(0);
                    ed.putLong("userid", datainner.getLong("employeeid"));
                    ed.putString("employeename", datainner.getString("employeename"));
                    ed.putString("department", datainner.getString("divisionname"));
                    ed.putString("designation", datainner.getString("designation"));
                    ed.putString("gpscoordinate", datainner.getString("gpscoordinate"));
                    ed.putString("radius", datainner.getString("radius"));
                    ed.putString("alternategpscoordinate", datainner.getString("alternategpscoordinate"));
                    ed.putString("alternateradius", datainner.getString("alternateradius"));
                    ed.putString("netid",strNetId);
                    ed.putString("pwd",strPassword);
                    ed.commit();
//                    SqlliteController sc = new SqlliteController(MainActivity.this);
//                    sc.insertLoginStaffDetails(datainner.getLong("employeeid"),datainner.getString("employeename"),
//                            datainner.getString("divisionname"),datainner.getString("designation"),
//                            editTextUsername,editTextPassword);
                    getLocation();
////                    Toast.makeText(LoginActivity.this, "Successfully Logged In", Toast.LENGTH_LONG).show();
//                    Intent intent = new Intent(LoginActivity.this, LandingPage.class);
//                    startActivity(intent);
                }
                else{
                    Toast.makeText(MapsActivityIST.this, object.getString("Message"), Toast.LENGTH_LONG).show();
                }
            } catch (JSONException ex) {
                ex.printStackTrace();
                Toast.makeText(MapsActivityIST.this, "Error in NETID Verification" + ex.getMessage(), Toast.LENGTH_LONG).show();
                System.out.println("Error in NETID Verification: " + ex.getMessage());
            }
        }
    }
}
