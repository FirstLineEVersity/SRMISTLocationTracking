package com.firstline.mylocationtracking;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.provider.Settings;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.conn.util.InetAddressUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import webservice.WebService;

public class MainActivityIST extends AppCompatActivity {
    private KeyStore keyStore;
    private LOCGpsTracker LOCGpsTracker;
    // Defining variable for storing
    // key in android keystore container
    private static final String KEY_NAME = "GEEKSFORGEEKS";
    private Cipher cipher;
    private TextView tvResult;
    private ImageView ivScanResult;
    private FingerprintManager fingerprintManager;
    private FingerprintManager.AuthenticationCallback authenticationCallback;
    private static String strParameters[];
    private static String ResultString = "";
    private String strIpAddress = "";
    private String strimeiNumber = "";
    private long lngEmployeeId = 0;
    private String strGpsCoordinates = "";
    private String strNetId = "";
    private String strPassword = "";
    TelephonyManager telephonyManager;


    @SuppressLint("ServiceCast")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Make to run your application only in portrait mode
        setContentView(R.layout.activity_main_loc);
        LOCStatusColor.SetStatusColor(getWindow(), ContextCompat.getColor(this, R.color.colorPrimary));
        tvResult = findViewById(R.id.tvResult);
        tvResult.setText("Provide your fingerprint");
        ivScanResult = findViewById(R.id.ivScanResult);
        ivScanResult.setImageResource(R.drawable.biometric_resultdisplay);
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);
//        Button button= (Button) findViewById(R.id.button);
        final SharedPreferences loginsession = getApplicationContext().getSharedPreferences("SessionLogin", 0);
        lngEmployeeId = loginsession.getLong("userid", 1);
        strNetId = loginsession.getString("netid", "");
        strPassword = loginsession.getString("pwd", "");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            tvResult.setText("This Android version does not support fingerprint authentication.");
        }
        else {
            if (!fingerprintManager.isHardwareDetected()) {
                // Setting error message if device doesn't have fingerprint sensor
                tvResult.setText("Device does not support fingerprint sensor");
            } else {
                // Checking fingerprint permission
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                    tvResult.setText("Fingerprint authentication is not enabled");
                } else {
                    // Check for at least one registered finger
                    if (!fingerprintManager.hasEnrolledFingerprints()) {
                        tvResult.setText("Register at least one finger");
                    } else {
                        // Checking for screen lock security
                        if (!keyguardManager.isKeyguardSecure()) {
                            tvResult.setText("Screen lock security not enabled");
                        } else {
//                            fingerprintManager.
                            // if everything is enabled and correct then we will generate the encryption key which will be stored on the device
                            generateKey();
                            if (cipherInit()) {
                                FingerprintManager.CryptoObject cryptoObject = new FingerprintManager.CryptoObject(cipher);
                                FingerprintHandler helper = new FingerprintHandler(this);
                                helper.Authentication(fingerprintManager, cryptoObject);
//                            helper.startAuth(fingerprintManager, cryptoObject);
                            }
                        }
                    }
                }

            }
        }
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        // new intent to call an activity that you choose
        Intent intent = new Intent(this, LOCLandingPage.class);
        startActivity(intent);
        // finish the activity picture
        this.finish();
    }
    @Override
    public boolean onKeyDown(int key_code, KeyEvent key_event) {
        if (key_code== KeyEvent.KEYCODE_BACK) {
            super.onKeyDown(key_code, key_event);
            return true;
        }
        return false;
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
        tvResult.setText("Wait authenticate process going on ...........");
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String strIpAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        if (strIpAddress.equals("")){
//            strIpAddress = getMobileIPAddress(true);
        }
        telephonyManager = (TelephonyManager) getSystemService(this.TELEPHONY_SERVICE);
        String strDeviceModel = Build.MODEL;
        String strDeviceBrand = Build.BRAND;
        String imeiNumber = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);
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

    public void getLocation(){  //View view
        tvResult.setText("get GPS current position process...........");
        LatLng latLng;
        String strGPSCoordinates = "";
        String strAltGPSCoordinates = "";
        double ldblRadius = 0.00, ldblGPSCoordinates1 = 0.00, ldblGPSCoordinates2 = 0.00;
        double ldblAltRadius = 0.00, ldblAltGPSCoordinates1 = 0.00, ldblAltGPSCoordinates2 = 0.00;
        final SharedPreferences loginsession = getApplicationContext().getSharedPreferences("SessionLogin", 0);
        strGPSCoordinates = loginsession.getString("gpscoordinate","");
        strAltGPSCoordinates = loginsession.getString("alternategpscoordinate","");
        String[] strColumns = strGPSCoordinates.split(",");
        ldblGPSCoordinates1 = Double.parseDouble(strColumns[0].toString());
        ldblGPSCoordinates2 = Double.parseDouble(strColumns[1].toString());

        strColumns = strAltGPSCoordinates.split(",");
        ldblAltGPSCoordinates1 = Double.parseDouble(strColumns[0].toString());
        ldblAltGPSCoordinates2 = Double.parseDouble(strColumns[1].toString());
        ldblRadius = Double.parseDouble(loginsession.getString("radius","1.00"));
        ldblAltRadius = Double.parseDouble(loginsession.getString("alternateradius","1.00"));

        LOCGpsTracker = new LOCGpsTracker(MainActivityIST.this);
        if (LOCGpsTracker.canGetLocation()){
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
            Location.distanceBetween(ldblGPSCoordinates1, ldblGPSCoordinates2,
                    latitude, longitude,distances);
            float[] altdistances = new float[1];
            Location.distanceBetween(ldblAltGPSCoordinates1, ldblAltGPSCoordinates2,
                    latitude, longitude,altdistances);
            double radiusInMeters = ldblRadius; // * 1000.0; // 80.0*1000.0; //1 KM = 1000 Meter
            double AltradiusInMeters = ldblAltRadius; // * 1000.0; // 80.0*1000.0; //1 KM = 1000 Meter
            if ( distances[0] > radiusInMeters ){
//                tvResult.setText("Not within the range to punch attendance, distance in metre: " + distances[0]);
//                Toast.makeText(getBaseContext(),
//                        "Not within the range to punch attendance, distance in metre: " + distances[0] , // + " radius: " + radiusInMeters,
//                        Toast.LENGTH_LONG).show();
                if (altdistances[0] > AltradiusInMeters){
                    tvResult.setText("Not within the range to punch attendance. (" + altdistances[0] + " metre away)");
//                    Toast.makeText(getBaseContext(),
//                            "Not within the range to punch attendance, distance in metre: " + altdistances[0] , //+ " radius: " + AltradiusInMeters,
//                            Toast.LENGTH_LONG).show();
                }
                else{
                    System.out.println("Inside, distance from center: " + altdistances[0] + " radius: " + AltradiusInMeters);
                    scanButton();
                }
            } else {
                System.out.println("Inside, distance from center: " + distances[0] + " radius: " + radiusInMeters);
                scanButton();
            }
        }else{
            LOCGpsTracker.showSettingsAlert();
        }
    }

    //    @RequiresApi(api = Build.VERSION_CODES.M)
    public void scanButton(){
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        strIpAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        if (strIpAddress.equals("")){
            strIpAddress = getMobileIPAddress(true);
        }
        strimeiNumber = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);
        strParameters = new String[]{"String", "employeeid", String.valueOf(lngEmployeeId),
                "String", "ipaddress",strIpAddress, "String", "gpscoordinates",strGpsCoordinates,
                "String", "deviceUID",strimeiNumber};
        new Thread(new Runnable() {
            public void run() {
                WebService.strParameters = strParameters;
                WebService.METHOD_NAME = "punchDetails";
                AsyncCallSaveWS task = new AsyncCallSaveWS();
                task.execute();
            }
        }).start();
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
                    Toast.makeText(MainActivityIST.this, object.getString("Message"), Toast.LENGTH_LONG).show();
                }
            } catch (JSONException ex) {
                ex.printStackTrace();
                Toast.makeText(MainActivityIST.this, "Error in NETID Verification" + ex.getMessage(), Toast.LENGTH_LONG).show();
                System.out.println("Error in NETID Verification: " + ex.getMessage());
            }
        }
    }

    private class AsyncCallSaveWS extends AsyncTask<Void, Void, Void> {
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
            try{
                JSONObject object = new JSONObject(ResultString.toString());
                try {
                    if (object.getString("Status").equals("Success")) {
                        Toast.makeText(MainActivityIST.this, object.getString("Data"), Toast.LENGTH_LONG).show();
                        tvResult.setText(object.getString("Data"));
                        Intent intent = new Intent(MainActivityIST.this, LOCPunchDetails.class);
                        startActivity(intent);
                    }
                    else{
                        Toast.makeText(MainActivityIST.this, "Error in Punch Details" + object.getString("Data"), Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException ex){
                    ex.printStackTrace();
                    Toast.makeText(MainActivityIST.this, "Error in Punch Details" + ex.getMessage(), Toast.LENGTH_LONG).show();
                    System.out.println("Error in Punch Details:"+ex.getMessage());
                }
            } catch (JSONException ex) {
                ex.printStackTrace();
                Toast.makeText(MainActivityIST.this, "Error in Punch Details" + ex.getMessage(), Toast.LENGTH_LONG).show();
                System.out.println("Error in Punch Details:"+ex.getMessage());
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    protected void generateKey() {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (Exception e) {
            e.printStackTrace();
        }
        KeyGenerator keyGenerator;
        try {
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException("KeyGenerator instance failed", e);
        }
        try {
            keyStore.load(null);
            keyGenerator.init(new
                    KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(
                            KeyProperties.ENCRYPTION_PADDING_PKCS7)
//                    .setUserAuthenticationParameters()
                    .build());
            keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException |
                InvalidAlgorithmParameterException
                | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public boolean cipherInit() {
        try {
            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Cipher failed", e);
        }

        try {
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME,
                    null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Cipher initialization failed", e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    class FingerprintHandler extends FingerprintManager.AuthenticationCallback {

        private Context context;
        // Constructor
        public FingerprintHandler(Context mContext) {
            context = mContext;
        }

        // Fingerprint authentication starts here..
        public void Authentication(FingerprintManager manager, FingerprintManager.CryptoObject cryptoObject) {
            CancellationSignal cancellationSignal = new CancellationSignal();
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.authenticate(cryptoObject, cancellationSignal, 0, this, null);
        }
        public void onAuthenticationError(int errorCode, CharSequence errString) {
            tvResult.setText("ERROR");
            ivScanResult.setImageResource(R.drawable.biometric_failed);
            super.onAuthenticationError(errorCode, errString);
        }

        @Override
        public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
//            tvResult.setText("HELP");
            ivScanResult.setImageResource(R.drawable.biometric_resultdisplay);
            super.onAuthenticationHelp(helpCode, helpString);
        }

        // On authentication failed
        @Override
        public void onAuthenticationFailed() {
            tvResult.setText("FAILED");
            ivScanResult.setImageResource(R.drawable.biometric_failed);
            super.onAuthenticationFailed();
        }

        // On successful authentication
        @Override
        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
            tvResult.setText("SUCCESS");
            ivScanResult.setImageResource(R.drawable.biometric_success);
            super.onAuthenticationSucceeded(result);
//            scanButton();
            try {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ){
                    ActivityCompat.requestPermissions(MainActivityIST.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
                }
                else{
//                    getLocation();
                    authenticateUser();
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
