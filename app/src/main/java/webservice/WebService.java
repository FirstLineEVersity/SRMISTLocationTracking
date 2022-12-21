package webservice;

import android.util.Log;

import org.apache.http.params.HttpConnectionParams;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.util.ArrayList;
import java.util.Date;

import static android.content.ContentValues.TAG;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by fipl on 09-12-2016.
 */

public class WebService {

    //Namespace of the Webservice - can be found in WSDL
    private static String NAMESPACE = "http://ws.fipl.com/";
    //Webservice URL - WSDL File location
    //For SRM IST live server database
    private static String URL = "https://uatserver.srmist.edu.in/srmistEmployeeAndroid/EmployeeAndroid?wsdl";//Make sure you changed IP address
    private static String URL1 = "https://firstlineinfotech.com/srmistEmployeeAndroid/EmployeeAndroid?wsdl";//Make sure you changed IP address

//    private static String URL = "http://192.168.1.50/srmistEmployeeAndroid/EmployeeAndroid?wsdl";//Make sure you changed IP address


    //SOAP Action URI again Namespace + Web method name
    private static String SOAP_ACTION = "http://ws.fipl.com/";
    private static String ResultString = "";

    private static byte[] image;
    public static String METHOD_NAME = "";
    public static String strParameters[];
    public static String timeDiff = "";


    public static String invokeWS() {
        // No change in Server side. we will use same https URL from Mobile to server request.

// Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {

            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        });
// Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);
            String strBody = "";
            if (strParameters != null) {

                for (int i = 0; i <= strParameters.length - 1; i = i + 3) {
                    strBody += "<" + strParameters[i + 1] + ">" + strParameters[i + 2] + "</" + strParameters[i + 1] + ">";
                    Log.e("Params:" ,strBody);

                }
            }
            EncryptDecrypt ED = new EncryptDecrypt();
            String strEncryptedData = ED.getEncryptedData(strBody);
            PropertyInfo piInfo = new PropertyInfo();
            piInfo.setType(String.class);
            piInfo.setName("EncryptedData");
            piInfo.setValue(strEncryptedData);
            request.addProperty(piInfo);


            SoapSerializationEnvelope soapEnvelope = new SoapSerializationEnvelope(
                    SoapEnvelope.VER10);
            soapEnvelope.dotNet = false;
            soapEnvelope.setOutputSoapObject(request);
            HttpTransportSE transport = new HttpTransportSE(URL, 100000);
            transport.debug = true;
            System.setProperty("http.keepAlive", "false");
            Date d = new Date();
            transport.call("\"" + SOAP_ACTION + METHOD_NAME + "\"", soapEnvelope);
            if (soapEnvelope.bodyIn instanceof SoapFault) {
                //this is the actual part that will call the webservice
                String str = ((SoapFault) soapEnvelope.bodyIn).faultstring;
            } else {
                // Get the SoapResult from the envelope body.
                SoapObject result = (SoapObject) soapEnvelope.bodyIn;
                ResultString = result.getProperty(0).toString();
                ResultString = ED.getDecryptedData(ResultString);
                Date d1 = new Date();
                timeDiff = d.toString()+" : "+d1.toString();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.e("Method:" ,METHOD_NAME);
        Log.e("TIME DIFF:" ,timeDiff+" : "+ResultString);

        return ResultString;
    }

    public static String invokeWSTEST(){
        //Object result;
        //Initialize soap request + add parameters
        SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);
        Log.i("Method: ",METHOD_NAME);
        //Use this to add parameters
        for (int i=0; i<=strParameters.length-1; i=i+3){
            PropertyInfo piInfo = new PropertyInfo();
            if (strParameters[i]=="String"){
                piInfo.setType(String.class);
                piInfo.setName(strParameters[i + 1]);
                piInfo.setValue(strParameters[i + 2]);
            } else if (strParameters[i]=="int"){
                piInfo.setType(int.class);
                piInfo.setName(strParameters[i + 1]);
                piInfo.setValue(strParameters[i + 2]);
            } else if (strParameters[i]=="Long"){
                piInfo.setType(Long.class);
                piInfo.setName(strParameters[i + 1]);
                piInfo.setValue(Long.parseLong(strParameters[i + 2]));
            } else if (strParameters[i]=="float"){
                piInfo.setType(float.class);
                piInfo.setName(strParameters[i + 1]);
                piInfo.setValue(Long.parseLong(strParameters[i + 2]));
            }
            request.addProperty(piInfo);
        }
        //Declare the version of the SOAP request
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        envelope.dotNet = false;
        try {
            HttpTransportSE androidHttpTransport = new HttpTransportSE(URL,100000); //,100000

//            HttpConnectionParams.setKeepAlive(true);
//            HttpConnectionParams.setSoKeepalive(HttpParams params, enableKeepalive="true");
            System.setProperty("http.keepAlive", "false");
            //this is the actual part that will call the webservice
            //androidHttpTransport.setXmlVersionTag("?xml version=\"1.0\" encoding=\"utf-8\"?>");
            System.out.println("Method Name:"+METHOD_NAME);
            Log.i("Method: ",METHOD_NAME);

            androidHttpTransport.call("\""+SOAP_ACTION+METHOD_NAME+"\"", envelope);
            if (envelope.bodyIn instanceof SoapFault) {
                //this is the actual part that will call the webservice
                String str= ((SoapFault) envelope.bodyIn).faultstring;
                Log.i("", str);
                ResultString = str;
            }else {
                // Get the SoapResult from the envelope body.
                SoapObject result = (SoapObject) envelope.bodyIn;
                ResultString = result.getProperty(0).toString();
            }
            // Get the SoapResult from the envelope body.
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
            Log.i("Error: ",e.getMessage());

            e.printStackTrace();
        }
        Log.i("ResultString: ",ResultString);

        return ResultString;
    }


    public static String invokeWSIST(){
        //Object result;
        //Initialize soap request + add parameters
        SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);
        //Use this to add parameters
        for (int i=0; i<=strParameters.length-1; i=i+3){
            PropertyInfo piInfo = new PropertyInfo();
            if (strParameters[i]=="String"){
                piInfo.setType(String.class);
                piInfo.setName(strParameters[i + 1]);
                piInfo.setValue(strParameters[i + 2]);
            } else if (strParameters[i]=="int"){
                piInfo.setType(int.class);
                piInfo.setName(strParameters[i + 1]);
                piInfo.setValue(strParameters[i + 2]);
            } else if (strParameters[i]=="Long"){
                piInfo.setType(Long.class);
                piInfo.setName(strParameters[i + 1]);
                piInfo.setValue(Long.parseLong(strParameters[i + 2]));
            } else if (strParameters[i]=="float"){
                piInfo.setType(float.class);
                piInfo.setName(strParameters[i + 1]);
                piInfo.setValue(Long.parseLong(strParameters[i + 2]));
            }
            request.addProperty(piInfo);
        }
        //Declare the version of the SOAP request
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        envelope.dotNet = false;
        try {
            HttpTransportSE androidHttpTransport = new HttpTransportSE(URL1,100000); //,100000

//            HttpConnectionParams.setKeepAlive(true);
//            HttpConnectionParams.setSoKeepalive(HttpParams params, enableKeepalive="true");
            System.setProperty("http.keepAlive", "false");
            //this is the actual part that will call the webservice
            //androidHttpTransport.setXmlVersionTag("?xml version=\"1.0\" encoding=\"utf-8\"?>");
            System.out.println("Method Name:"+METHOD_NAME);
            androidHttpTransport.call("\""+SOAP_ACTION+METHOD_NAME+"\"", envelope);
            if (envelope.bodyIn instanceof SoapFault) {
                //this is the actual part that will call the webservice
                String str= ((SoapFault) envelope.bodyIn).faultstring;
                Log.i("", str);
            }else {
                // Get the SoapResult from the envelope body.
                SoapObject result = (SoapObject) envelope.bodyIn;
                ResultString = result.getProperty(0).toString();
            }
            // Get the SoapResult from the envelope body.
        } catch (Exception e) {
            ResultString = e.getMessage();
            Log.e(TAG, "Error: " + e.getMessage());
            e.printStackTrace();
        }
        return ResultString;
    }

    public static ArrayList invokeWSArray(){
        //Object result;
        //Initialize soap request + add parameters
        ArrayList<String> arrlist = new ArrayList<String>();
        SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);
        //Use this to add parameters
        for (int i=0; i<=strParameters.length-1; i=i+3){
            PropertyInfo piInfo = new PropertyInfo();
            if (strParameters[i]=="String"){
                piInfo.setType(String.class);
                piInfo.setName(strParameters[i + 1]);
                piInfo.setValue(strParameters[i + 2]);
            }
            else{
                piInfo.setType(Long.class);
                piInfo.setName(strParameters[i + 1]);
                piInfo.setValue(Long.parseLong(strParameters[i + 2]));
            }
            request.addProperty(piInfo);
        }
        //Declare the version of the SOAP request
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        envelope.dotNet = false;
        try {
            HttpTransportSE androidHttpTransport = new HttpTransportSE(URL,100000); //,100000
            System.out.println("Method Name:"+METHOD_NAME);

            androidHttpTransport.call("\""+SOAP_ACTION+METHOD_NAME+"\"", envelope);
            if (envelope.bodyIn instanceof SoapFault) {
                //this is the actual part that will call the webservice
                String str= ((SoapFault) envelope.bodyIn).faultstring;
                Log.i("", str);
            }else {
                // Get the SoapResult from the envelope body.
                SoapObject result = (SoapObject) envelope.bodyIn;
                ResultString = result.getProperty(0).toString();
                java.util.StringTokenizer st = new java.util.StringTokenizer(ResultString,",");
                while (st.hasMoreTokens()){
                    arrlist.add(st.nextToken());
                }
            }
            // Get the SoapResult from the envelope body.
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
            e.printStackTrace();
        }
        return arrlist;
    }

    public static ArrayList invokeWSArrayInner(){
        //Object result;
        //Initialize soap request + add parameters
        ArrayList<String> arrlist = new ArrayList<String>();
        SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);
        //Use this to add parameters
        for (int i=0; i<=strParameters.length-1; i=i+3){
            PropertyInfo piInfo = new PropertyInfo();
            if (strParameters[i]=="String"){
                piInfo.setType(String.class);
                piInfo.setName(strParameters[i + 1]);
                piInfo.setValue(strParameters[i + 2]);
            }
            else{
                piInfo.setType(Long.class);
                piInfo.setName(strParameters[i + 1]);
                piInfo.setValue(Long.parseLong(strParameters[i + 2]));
            }
            request.addProperty(piInfo);
        }
        //Declare the version of the SOAP request
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        envelope.dotNet = false;
        try {
            HttpTransportSE androidHttpTransport = new HttpTransportSE(URL,100000); //,100000
            System.out.println("Method Name:"+METHOD_NAME);

            androidHttpTransport.call("\""+SOAP_ACTION+METHOD_NAME+"\"", envelope);
            if (envelope.bodyIn instanceof SoapFault) {
                //this is the actual part that will call the webservice
                String str= ((SoapFault) envelope.bodyIn).faultstring;
                Log.i("", str);
            }else {
                // Get the SoapResult from the envelope body.
                SoapObject result = (SoapObject) envelope.bodyIn;
                ResultString = result.getProperty(0).toString();
                java.util.StringTokenizer st = new java.util.StringTokenizer(ResultString,"#");
                while (st.hasMoreTokens()){
                    arrlist.add(st.nextToken());
                }
            }
            // Get the SoapResult from the envelope body.
        } catch (Exception e) {
//            System.out.println(e);
            Log.e(TAG, "Error: " + e.getMessage());
            e.printStackTrace();
        }
        return arrlist;
    }
}

