package ija.lbled.simulateurbassevision;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Spinner;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

/**
 * Created by l.bled on 19/05/2015.
 */
public class Image_test extends Activity {

    /*public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.application_activity);
        ImageView image = (ImageView) findViewById(R.id.test_image);
        Bitmap bMap = BitmapFactory.decodeFile("/storage/extSdCard/test.png");
        Log.i("Path", Environment.getExternalStorageDirectory().getAbsolutePath());
        image.setImageBitmap(bMap);
    }*/
    // this is the action code we use in our intent,
    // this way we know we're looking at the response from our own action
    private static final int SELECT_PICTURE = 1;

    private String selectedImagePath;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.application_activity);

        //Valeurs Acuite : 0.1, 0.2, 0.3, 0.4, 0.5, 0.63, 0.8, 1/10, 1.3/10, 1.6/10, 2/10, 2.5/10, 3.2/10, 4/10
        //Valeurs Distance : 4m, 2.5m, 40cm, 10cm

        //fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        findViewById(R.id.button_importer).setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {

                // in onCreate or any event where your want the user to
                // select a file
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,
                        "Select Picture"), SELECT_PICTURE);
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
                selectedImagePath = getPath(selectedImageUri);
                ImageView image = (ImageView) findViewById(R.id.image);
                Bitmap bMap = BitmapFactory.decodeFile(selectedImagePath);
                Log.i("taille", "hauteur"+bMap.getHeight());
                Log.i("taille", "largeur" + bMap.getWidth());
                Log.i("taille", "taille " + 1920 * (bMap.getHeight() / bMap.getWidth()));
                if (bMap.getHeight() < bMap.getWidth()) {
                    image.setImageBitmap(Bitmap.createScaledBitmap(bMap, 1920, 1080, false));
                } else {
                    image.setImageBitmap(Bitmap.createScaledBitmap(bMap, 1080, 1920, false));
                }
               /* Mat imageMat = new Mat ( bMap.getHeight(), bMap.getWidth(), CvType.CV_8U, new Scalar(4));
                Bitmap myBitmap32 = bMap.copy(Bitmap.Config.ARGB_8888, true);
                Utils.bitmapToMat(myBitmap32, imageMat);
                Utils.bitmapToMat(bMap, imageMat);
                Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_RGB2GRAY, 4);
                Log.i("Path", selectedImagePath);
                Bitmap resultBitmap = Bitmap.createBitmap(imageMat.cols(), imageMat.rows(),Bitmap.Config.ARGB_8888);;
                Utils.matToBitmap(imageMat, resultBitmap);*/
               // image.setImageBitmap(bMap);
            }
        }
    }

    /**
     * helper to retrieve the path of an image URI
     */
    public String getPath(Uri uri) {
        // just some safety built in
        if( uri == null ) {
            // TODO perform some logging or show user feedback
            return null;
        }
        // try to retrieve the image from the media store first
        // this will only work for images selected from gallery
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if( cursor != null ){
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        // this is our fallback here
        return uri.getPath();
    }
}
