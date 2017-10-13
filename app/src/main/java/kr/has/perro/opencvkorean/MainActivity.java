package kr.has.perro.opencvkorean;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    static {
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV loaded successfully");
        }
        else {
            Log.d(TAG, "OpenCV NOT LOADED! :(");
        }
    }

    private static final int TAKE_PICTURE = 1;
    private static final int STORAGE_PERMISSION = 2;
    private String lastImageURI;
    private static final String dataPath = Environment.getExternalStorageDirectory().toString() + "/OpenCVKorean/";
    private static final String lang = "eng";

    TextView text;
    ImageView image;
    Bitmap imageBitmap;
//    String dataPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = (TextView) findViewById(R.id.tv_image_text);
        image = (ImageView) findViewById(R.id.iv_image);
//        dataPath = getFilesDir().getAbsolutePath();
        Log.d(TAG, "dataPath: " + dataPath);
//        checkFile(new File(dataPath + "tessdata/"), lang);

        String[] paths = new String[] { dataPath, dataPath + "/tessdata/", dataPath + "/tesseract/tessdata/" };

        for (String path : paths) {
            Log.d(TAG, "dataPath: " + path);
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.v(TAG, "ERROR: Creation of directory " + path + " on sdcard failed");
                    return;
                } else {
                    Log.v(TAG, "Created directory " + path + " on sdcard");
                }
            }
        }

        // lang.traineddata file with the app (in assets folder)
        // You can get them at:
        // http://code.google.com/p/tesseract-ocr/downloads/list
        // This area needs work and optimization
        if (!(new File(dataPath + "tessdata/" + lang + ".traineddata")).exists()) {
            Log.d(TAG, "Language file exist! yiiiiiiiihhh!");
            try {
                AssetManager assetManager = getAssets();
                InputStream in = assetManager.open("tessdata/" + lang + ".traineddata");
                //GZIPInputStream gin = new GZIPInputStream(in);
                OutputStream out = new FileOutputStream(dataPath
                        + "tessdata/" + lang + ".traineddata");

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                //while ((lenf = gin.read(buff)) > 0) {
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                //gin.close();
                out.close();

                Log.v(TAG, "Copied " + lang + " traineddata");
            } catch (IOException e) {
                Log.e(TAG, "Was unable to copy " + lang + " traineddata " + e.toString());
            }
        } else {
            Log.d(TAG, "Data file does not exist yiiihhh :c");
        }
    }

    private void checkFile(File dir, String language) {
        if (!dir.exists()&& dir.mkdirs()){
            copyFiles(language);
        }
        if(dir.exists()) {
            String datafilepath = dataPath + "/tessdata/"+ language + ".traineddata";
            File datafile = new File(datafilepath);

            if (!datafile.exists()) {
                copyFiles(language);
            }
        }
    }

    private void copyFiles(String language) {
        try {
            String filepath = dataPath + "/tessdata/" + language + ".traineddata";
            AssetManager assetManager = getAssets();

            InputStream instream = assetManager.open("tessdata/" + language + ".traineddata");
            OutputStream outstream = new FileOutputStream(filepath);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }

            outstream.flush();
            outstream.close();
            instream.close();

            File file = new File(filepath);
            if (!file.exists()) {
                throw new FileNotFoundException();
            }
            Log.d(TAG, "language file exist");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Activity.RESULT_OK) {
            switch(requestCode) {
                case TAKE_PICTURE:
                    Log.d(TAG, "Picture taken");
                    Log.d(TAG, "Image URI: " + lastImageURI);
                    imageBitmap = BitmapFactory.decodeFile(lastImageURI);
                    image.setImageBitmap(imageBitmap);
                    getTextFromImage();
                    break;
            }
        }
    }

    private void getTextFromImage() {
        Log.d(TAG, "Starting OCR");
        Bitmap bitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Log.d(TAG, "Copy bitmap");
        TessBaseAPI baseAPI = new TessBaseAPI();
        baseAPI.setDebug(true);
        baseAPI.init(dataPath, lang);
        Log.d(TAG, "Initialized BaseAPI");
        baseAPI.setImage(bitmap);
        Log.d(TAG, "Set Image");
        String recognizedText = "";
        try {
            recognizedText = baseAPI.getUTF8Text();
        } catch(Exception e) {
            e.printStackTrace();
        }

        baseAPI.end();

        Log.v(TAG, "OCRED TEXT: " + recognizedText);

        if ( lang.equalsIgnoreCase("eng") ) {
            recognizedText = recognizedText.replaceAll("[^a-zA-Z0-9]+", " ");
        }

        recognizedText = recognizedText.trim();

        if ( recognizedText.length() != 0 ) {
            text.setText(recognizedText);
        }
        bitmap.recycle();
    }

    private File createImageFile() throws IOException {
        String timeName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "IMAGE_" + timeName + "_";
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                fileName,
                ".jpg",
                dir
        );
        lastImageURI = image.getAbsolutePath();
        return image;
    }

    public void takePicture(View v) {
        if(Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED  &&
                checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                    STORAGE_PERMISSION);
        }
        else
        {
            takePicturePermitted();
        }
    }

    public void takePicturePermitted() {
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (i.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "kr.has.perro.opencvkorean.fileprovider",
                        photoFile);
                i.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(i, TAKE_PICTURE);
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if(requestCode == STORAGE_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
            Log.d(TAG, "Permission return.");
            takePicturePermitted();
        }
    }
}
