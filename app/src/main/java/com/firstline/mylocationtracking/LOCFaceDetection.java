package com.firstline.mylocationtracking;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;
import java.io.File;
import java.io.FileNotFoundException;

public class LOCFaceDetection extends AppCompatActivity {
    private static final String LOG_TAG = "FACE API";
    private static final int PHOTO_REQUEST = 10;
    private TextView scanResults;
    private ImageView imageView;
    private Uri imageUri;
    private FaceDetector detector;
    private static final int REQUEST_WRITE_PERMISSION = 20;
    private static final String SAVED_INSTANCE_URI = "uri";
    private static final String SAVED_INSTANCE_BITMAP = "bitmap";
    private static final String SAVED_INSTANCE_RESULT = "result";
    Bitmap editedBitmap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_loc);
        Button button = (Button) findViewById(R.id.button);
        scanResults = (TextView) findViewById(R.id.results);
        imageView = (ImageView) findViewById(R.id.scannedResults);
        if (savedInstanceState != null) {
            editedBitmap = savedInstanceState.getParcelable(SAVED_INSTANCE_BITMAP);
            if (savedInstanceState.getString(SAVED_INSTANCE_URI) != null) {
                imageUri = Uri.parse(savedInstanceState.getString(SAVED_INSTANCE_URI));
            }
            imageView.setImageBitmap(editedBitmap);
            scanResults.setText(savedInstanceState.getString(SAVED_INSTANCE_RESULT));
        }
        detector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(LOCFaceDetection.this, new
                        String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);
            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_WRITE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePicture();
                } else {
                    Toast.makeText(LOCFaceDetection.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode,data);
        if (requestCode == PHOTO_REQUEST && resultCode == RESULT_OK) {
            launchMediaScanIntent();
            try {
                scanFaces();
            } catch (Exception e) {
                Toast.makeText(this, "Failed to load Image", Toast.LENGTH_SHORT).show();
                Log.e(LOG_TAG, e.toString());
            }
        }
    }
    private void scanFaces() throws Exception {
        Bitmap bitmap = decodeBitmapUri(this, imageUri);
        if (detector.isOperational() && bitmap != null) {
            editedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap
                    .getHeight(), bitmap.getConfig());
            float scale = getResources().getDisplayMetrics().density;
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.rgb(255, 61, 61));
            paint.setTextSize((int) (14 * scale));
            paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3f);
            Canvas canvas = new Canvas(editedBitmap);
            canvas.drawBitmap(bitmap, 0, 0, paint);
            Frame frame = new Frame.Builder().setBitmap(editedBitmap).build();
            SparseArray<Face> faces = detector.detect(frame);
            scanResults.setText(null);
            for (int index = 0; index < faces.size(); ++index) {
                Face face = faces.valueAt(index);
                canvas.drawRect(
                        face.getPosition().x,
                        face.getPosition().y,
                        face.getPosition().x + face.getWidth(),
                        face.getPosition().y + face.getHeight(), paint);
                scanResults.setText(scanResults.getText() + "Face " + (index + 1) + "\n");
                scanResults.setText(scanResults.getText() + "Smile probability:" + "\n");
                scanResults.setText(scanResults.getText() + String.valueOf(face.getIsSmilingProbability()) + "\n");
                scanResults.setText(scanResults.getText() + "Left Eye Open Probability: " + "\n");
                scanResults.setText(scanResults.getText() + String.valueOf(face.getIsLeftEyeOpenProbability()) + "\n");
                scanResults.setText(scanResults.getText() + "Right Eye Open Probability: " + "\n");
                scanResults.setText(scanResults.getText() + String.valueOf(face.getIsRightEyeOpenProbability()) + "\n");
                scanResults.setText(scanResults.getText() + "---------" + "\n");
                for (Landmark landmark : face.getLandmarks()) {
                    int cx = (int) (landmark.getPosition().x);
                    int cy = (int) (landmark.getPosition().y);
                    canvas.drawCircle(cx, cy, 5, paint);
                }
            }
            if (faces.size() == 0) {
                scanResults.setText("Scan Failed: Found nothing to scan");
            } else {
                imageView.setImageBitmap(editedBitmap);
                scanResults.setText(scanResults.getText() + "No of Faces Detected: " + "\n");
                scanResults.setText(scanResults.getText() + String.valueOf(faces.size()) + "\n");
                scanResults.setText(scanResults.getText() + "---------" + "\n");
            }
        } else {
            scanResults.setText("Could not set up the detector!");
        }
    }
    private void takePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = new File(Environment.getExternalStorageDirectory(), "picture.jpg");
        imageUri = FileProvider.getUriForFile(LOCFaceDetection.this,
                BuildConfig.APPLICATION_ID + ".provider", photo);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, PHOTO_REQUEST);
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (imageUri != null) {
            outState.putParcelable(SAVED_INSTANCE_BITMAP, editedBitmap);
            outState.putString(SAVED_INSTANCE_URI, imageUri.toString());
            outState.putString(SAVED_INSTANCE_RESULT, scanResults.getText().toString());
        }
        super.onSaveInstanceState(outState);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        detector.release();
    }
    private void launchMediaScanIntent() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(imageUri);
        this.sendBroadcast(mediaScanIntent);
    }
    private Bitmap decodeBitmapUri(Context ctx, Uri uri) throws FileNotFoundException {
        int targetW = 600;
        int targetH = 600;
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(ctx.getContentResolver().openInputStream(uri), null, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        return BitmapFactory.decodeStream(ctx.getContentResolver()
                .openInputStream(uri), null, bmOptions);
    }
}
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.graphics.Canvas;
//import android.graphics.Paint;
//import android.graphics.RectF;
//import android.graphics.drawable.BitmapDrawable;
//import android.hardware.camera2.params.Face;
//import android.media.FaceDetector;
//import android.os.Bundle;
//import android.util.SparseArray;
//import android.view.View;
//import android.widget.Button;
//import android.widget.ImageView;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.google.android.gms.vision.Frame;
//
//public class FaceDetection extends AppCompatActivity {
//    ImageView imgFace;
//    Button btnProgress;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState)
//    {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.facerecognition);
//
//        imgFace = (ImageView) findViewById(R.id.imgFace);
//        btnProgress = (Button) findViewById(R.id.btnProgress);
//
//        Bitmap myBitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.sample1);
//        imgFace.setImageBitmap(myBitmap);
//
//        Paint rectPaint = new Paint();
//        rectPaint.setStrokeWidth(5);
//        rectPaint.setColor(android.R.color.holo_red_dark);
//        rectPaint.setStyle(Paint.Style.STROKE);
//
//        Bitmap tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(),myBitmap.getHeight(),Bitmap.Config.RGB_565);
//        Canvas canvas = new Canvas(tempBitmap);
//        canvas.drawBitmap(myBitmap,0,0,null);
//        btnProgress.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View v){
//                FaceDetector facedetector = new FaceDetector.Builder(getApplicationContext())
//                        .setTrackingEnabled(false)
//                        .setLandmarkType(FaceDetector.ALL_LANDMAKS)
//                        .setMode(FaceDetector.FAST_MODE)
//                        .build();
//                if (!facedetector.isOperational()){
//                    Toast.makeText(FaceDetector.this,"Face Detector could not be set up on your device", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//                Frame frame = new Frame.Builder().setBitmap(myBitmap).build();
//                SparseArray<Face> sparseArray = facedetector.detect(frame);
//                for (int i=0; i < sparseArray.size();i++){
//                    Face face = sparseArray.valueAt(i);
//                    float x1 = face.getPosition().x;
//                    float y1 = face.getPosition().y;
//                    float x2 = x1+face.getWidth();
//                    float y2 = y1+face.getHeight();
//                    RectF rectF = new RectF(x1,y1,x2,y2);
//                    canvas.drawRoundRect(rectF,2,2, rectPaint);
//                    imgFace.setImageDrawable(new BitmapDrawable(getResources(),tempBitmap));
//                }
//            }
//        });
//
//
//
//    }
//}


///*package whatever do not write package name here*/
//
//import androidx.annotation.NonNull;
//        import androidx.annotation.Nullable;
//        import androidx.appcompat.app.AppCompatActivity;
//        import androidx.fragment.app.DialogFragment;
//        import android.content.Intent;
//        import android.graphics.Bitmap;
//        import android.os.Bundle;
//        import android.provider.MediaStore;
//        import android.view.View;
//        import android.widget.Button;
//        import android.widget.Toast;
//        import com.google.android.gms.tasks.OnFailureListener;
//        import com.google.android.gms.tasks.OnSuccessListener;
//        import com.google.firebase.FirebaseApp;
//        import com.google.firebase.ml.vision.FirebaseVision;
//        import com.google.firebase.ml.vision.common.FirebaseVisionImage;
//        import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
//        import com.google.firebase.ml.vision.face.FirebaseVisionFace;
//        import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
//        import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
//        import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;
//        import java.util.List;
//
//public class FaceDetection extends AppCompatActivity {
//    Button cameraButton;
//
//    // whenever we request for our customized permission, we
//    // need to declare an integer and initialize it to some
//    // value .
//    private final static int REQUEST_IMAGE_CAPTURE = 124;
//    FirebaseVisionImage image;
//    FirebaseVisionFaceDetector detector;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState)
//    {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.facerecognition);
//
//        // initializing our firebase in main activity
//        FirebaseApp.initializeApp(this);
//
//        // finding the elements by their id's alloted.
//        cameraButton = findViewById(R.id.camera_button);
//
//        // setting an onclick listener to the button so as
//        // to request image capture using camera
//        cameraButton.setOnClickListener(
//                new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v)
//                    {
//
//                        // makin a new intent for opening camera
//                        Intent intent = new Intent(
//                                MediaStore.ACTION_IMAGE_CAPTURE);
//                        if (intent.resolveActivity(
//                                getPackageManager())
//                                != null) {
//                            startActivityForResult(
//                                    intent, REQUEST_IMAGE_CAPTURE);
//                        }
//                        else {
//                            // if the image is not captured, set
//                            // a toast to display an error image.
//                            Toast
//                                    .makeText(
//                                            FaceDetection.this,
//                                            "Something went wrong",
//                                            Toast.LENGTH_SHORT)
//                                    .show();
//                        }
//                    }
//                });
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode,
//                                    int resultCode,
//                                    @Nullable Intent data)
//    {
//        // after the image is captured, ML Kit provides an
//        // easy way to detect faces from variety of image
//        // types like Bitmap
//
//        super.onActivityResult(requestCode, resultCode,
//                data);
//        if (requestCode == REQUEST_IMAGE_CAPTURE
//                && resultCode == RESULT_OK) {
//            Bundle extra = data.getExtras();
//            Bitmap bitmap = (Bitmap)extra.get("data");
//            detectFace(bitmap);
//        }
//    }
//
//    // If you want to configure your face detection model
//    // according to your needs, you can do that with a
//    // FirebaseVisionFaceDetectorOptions object.
//    private void detectFace(Bitmap bitmap)
//    {
//        FirebaseVisionFaceDetectorOptions options
//                = new FirebaseVisionFaceDetectorOptions
//                .Builder()
//                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
//                .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
//                .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
//                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
//                .setMinFaceSize(0.15f)
//                .enableTracking()
//
////                .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
////                .setModeType(
////                        FirebaseVisionFaceDetectorOptions
////                                .ACCURATE)
////                .setLandmarkType(
////                        FirebaseVisionFaceDetectorOptions
////                                .ALL_LANDMARKS)
////                .setClassificationType(
////                        FirebaseVisionFaceDetectorOptions
////                                .ALL_CLASSIFICATIONS)
//                .build();
//
//        // we need to create a FirebaseVisionImage object
//        // from the above mentioned image types(bitmap in
//        // this case) and pass it to the model.
//        try {
//            image = FirebaseVisionImage.fromBitmap(bitmap);
//            detector = FirebaseVision.getInstance()
//                    .getVisionFaceDetector(options);
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        // It’s time to prepare our Face Detection model.
//        detector.detectInImage(image)
//                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace> >() {
//                    @Override
//                    // adding an onSuccess Listener, i.e, in case
//                    // our image is successfully detected, it will
//                    // append it's attribute to the result
//                    // textview in result dialog box.
//                    public void onSuccess(
//                            List<FirebaseVisionFace>
//                                    firebaseVisionFaces)
//                    {
//                        String resultText = "";
//                        int i = 1;
//                        for (FirebaseVisionFace face :
//                                firebaseVisionFaces) {
//                            resultText
//                                    = resultText
//                                    .concat("\nFACE NUMBER. "
//                                            + i + ": ")
//                                    .concat(
//                                            "\nSmile: "
//                                                    + face.getSmilingProbability()
//                                                    * 100
//                                                    + "%")
//                                    .concat(
//                                            "\nleft eye open: "
//                                                    + face.getLeftEyeOpenProbability()
//                                                    * 100
//                                                    + "%")
//                                    .concat(
//                                            "\nright eye open "
//                                                    + face.getRightEyeOpenProbability()
//                                                    * 100
//                                                    + "%");
//                            i++;
//                        }
//
//                        // if no face is detected, give a toast
//                        // message.
//                        if (firebaseVisionFaces.size() == 0) {
//                            Toast
//                                    .makeText(FaceDetection.this,
//                                            "NO FACE DETECT",
//                                            Toast.LENGTH_SHORT)
//                                    .show();
//                        }
//                        else {
//                            Bundle bundle = new Bundle();
//                            bundle.putString(
//                                    LCOFaceDetection.RESULT_TEXT,
//                                    resultText);
//                            DialogFragment resultDialog
//                                    = new ResultDialog();
//                            resultDialog.setArguments(bundle);
//                            resultDialog.setCancelable(true);
//                            resultDialog.show(
//                                    getSupportFragmentManager(),
//                                    LCOFaceDetection.RESULT_DIALOG);
//                        }
//                    }
//                }) // adding an onfailure listener as well if
//                // something goes wrong.
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e)
//                    {
//                        Toast
//                                .makeText(
//                                        FaceDetection.this,
//                                        "Oops, Something went wrong",
//                                        Toast.LENGTH_SHORT)
//                                .show();
//                    }
//                });
//    }
//}
//
