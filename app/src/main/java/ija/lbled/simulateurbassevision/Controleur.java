package ija.lbled.simulateurbassevision;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

/**
 * Created by l.bled on 19/05/2015.
 */
public class Controleur extends Activity {

    private static final int SELECT_PICTURE = 1;

    private String selectedImagePath;
    private boolean imageChargee = false;
    private int valeurLumi = 50, valeurContraste = 1;
    private ImageView image;
    private Bitmap bMap;
    private Bitmap original;
    private SeekBar scotomeSB, tubulaireSB, hemiaSB, contrasteSB, luminositeSB;
    RadioButton normalRB, scotomeRB, tubuRB, hemiaRB;
    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.application_activity);
        //Valeurs Acuite : 0.1, 0.2, 0.3, 0.4, 0.5, 0.63, 0.8, 1/10, 1.3/10, 1.6/10, 2/10, 2.5/10, 3.2/10, 4/10
        //Valeurs Distance : 4m, 2.5m, 40cm, 10cm
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mOpenCVCallBack))
        {
            Log.e("OpenCV", "Cannot connect to OpenCV Manager");
        }
        //fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        image = (ImageView)findViewById(R.id.image);
        normalRB = (RadioButton)findViewById(R.id.radioButton_normal);
        scotomeRB = (RadioButton)findViewById(R.id.radioButton_scotome);
        tubuRB = (RadioButton)findViewById(R.id.radioButton_tubulaire);
        hemiaRB = (RadioButton)findViewById(R.id.radioButton_hemianopsie);
        findViewById(R.id.button_importer).setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {

                // in onCreate or any event where your want the user to
                // select a file
                Intent galleryIntent = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, 1);
            }
        });

        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                miseAJourSliders();
                mettreValeursDefautChampVisuel();
            }
        });

        final TextView scotomeValue = (TextView)findViewById(R.id.textView_scotome);
        scotomeSB = (SeekBar)findViewById(R.id.seekBar_scotome);
        scotomeSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                scotomeValue.setText(String.valueOf(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        final TextView tubulaireValue = (TextView)findViewById(R.id.textView_tubu);
        tubulaireSB = (SeekBar)findViewById(R.id.seekBar_tubulaire);
        tubulaireSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tubulaireValue.setText(String.valueOf(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        final TextView hemiaValue = (TextView)findViewById(R.id.textView_hemia);
        hemiaSB = (SeekBar)findViewById(R.id.seekBar_hemia);
        hemiaSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                hemiaValue.setText(String.valueOf(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        final TextView contrasteValue = (TextView)findViewById(R.id.textView_contraste_sb);
        contrasteSB = (SeekBar)findViewById(R.id.seekBar_contraste);
        contrasteSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                contrasteValue.setText(String.valueOf(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (imageChargee) {
                    bMap = original.copy(Bitmap.Config.ARGB_8888, true);
                    Mat imageMat = new Mat(bMap.getHeight(), bMap.getWidth(), CvType.CV_8UC1);
                    Utils.bitmapToMat(bMap, imageMat);
                    /*int valeur = 0;
                    if (seekBar.getProgress() > 50) {
                        valeur = (seekBar.getProgress() - 50) * 2;
                    } else if (seekBar.getProgress() < 50) {
                        valeur = 0 - ((50 - seekBar.getProgress())*2);
                    } else {
                        valeur = 0;
                    }*/
                    //valeurContraste = 1 - seekBar.getProgress()/100;
                    imageMat.convertTo(imageMat, -1, 0.5, valeurLumi);
                    Utils.matToBitmap(imageMat, bMap);
                    image.setImageBitmap(bMap);
                }
            }
        });

        final TextView luminositeValue = (TextView)findViewById(R.id.textView_luminosite_sb);
        luminositeSB = (SeekBar)findViewById(R.id.seekBar_luminosite);
        luminositeSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                luminositeValue.setText(String.valueOf(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (imageChargee) {
                    bMap = original.copy(Bitmap.Config.ARGB_8888, true);
                    Mat imageMat = new Mat(bMap.getHeight(), bMap.getWidth(), CvType.CV_8UC1);
                    Utils.bitmapToMat(bMap, imageMat);
                    if (seekBar.getProgress() > 50) {
                        valeurLumi = (seekBar.getProgress() - 50) * 2;
                    } else if (seekBar.getProgress() < 50) {
                        valeurLumi = 0 - ((50 - seekBar.getProgress())*2);
                    } else {
                        valeurLumi = 0;
                    }
                    imageMat.convertTo(imageMat, -1, valeurContraste, valeurLumi);
                    Utils.matToBitmap(imageMat, bMap);
                    image.setImageBitmap(bMap);
                }
            }
        });
        mettreValeursDefaut();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                mettreValeursDefaut();
                Uri selectedImageUri = data.getData();
                selectedImagePath = getPath(selectedImageUri);
                bMap = BitmapFactory.decodeFile(selectedImagePath);
                Mat imageMat = new Mat ( bMap.getHeight(), bMap.getWidth(), CvType.CV_8UC1);
               // Bitmap myBitmap32 = bMap.copy(Bitmap.Config.ARGB_8888, true);
                Utils.bitmapToMat(bMap, imageMat);
               // Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_RGB2BGR);
                //imageMat.convertTo(imageMat, -1, 2, 0);
                //Bitmap resultBitmap = Bitmap.createBitmap(imageMat.cols(), imageMat.rows(),Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(imageMat, bMap);
                if (bMap.getHeight() < bMap.getWidth()) {
                    Bitmap.createScaledBitmap(bMap, 1920, 1080, false);
                } else if (bMap.getHeight() == bMap.getWidth()) {
                    Bitmap.createScaledBitmap(bMap, 1080, 1080, false);
                } else {
                    Bitmap.createScaledBitmap(bMap, 1080, 1920, false);
                }
                image.setImageBitmap(bMap);
                original = bMap;
                imageChargee = true;
            }
        }
    }

    /**
     * Remet les valeurs par défaut quand on charge une image
     */
    private void mettreValeursDefaut () {
        scotomeSB.setProgress(0);
        tubulaireSB.setProgress(100);
        hemiaSB.setProgress(100);
        contrasteSB.setProgress(100);
        luminositeSB.setProgress(50);
        normalRB.setChecked(true);
        miseAJourSliders();
    }

    /**
     * Met à jour les slider en fonction des radio buttons
     */
    private void miseAJourSliders() {
        if (normalRB.isChecked()) {
            scotomeSB.setEnabled(false);
            tubulaireSB.setEnabled(false);
            hemiaSB.setEnabled(false);
        } else if (scotomeRB.isChecked()) {
            scotomeSB.setEnabled(true);
            tubulaireSB.setEnabled(false);
            hemiaSB.setEnabled(false);
        } else if (tubuRB.isChecked()) {
            scotomeSB.setEnabled(false);
            tubulaireSB.setEnabled(true);
            hemiaSB.setEnabled(false);
        } else if (hemiaRB.isChecked()) {
            scotomeSB.setEnabled(false);
            tubulaireSB.setEnabled(false);
            hemiaSB.setEnabled(true);
        }
    }

    /**
     * Met les valeurs de sliders du champ visuel à leur valeur par défaut
     */
    private void mettreValeursDefautChampVisuel(){
        scotomeSB.setProgress(0);
        tubulaireSB.setProgress(100);
        hemiaSB.setProgress(100);
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

   /* private class ModifierImage extends AsyncTask<Integer, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(Integer... params) {
            bMap = original.copy(Bitmap.Config.ARGB_8888, true);
            Mat imageMat = new Mat ( bMap.getHeight(), bMap.getWidth(), CvType.CV_8UC1);
            Utils.bitmapToMat(bMap, imageMat);
            imageMat.convertTo(imageMat, -1, 1, params[0]);
            Utils.matToBitmap(imageMat, bMap);
            return bMap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            image.setImageBitmap(result);
        }
    }*/
}
