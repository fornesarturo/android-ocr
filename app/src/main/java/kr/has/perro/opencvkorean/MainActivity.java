package kr.has.perro.opencvkorean;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

    TextView text;
    ImageView image;
    Bitmap imageBitmap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = (TextView) findViewById(R.id.tv_image_text);
        image = (ImageView) findViewById(R.id.iv_image);
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
                    break;
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeName = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
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
