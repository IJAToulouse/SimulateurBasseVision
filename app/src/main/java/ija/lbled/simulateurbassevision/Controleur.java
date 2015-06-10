package ija.lbled.simulateurbassevision;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Created by l.bled on 19/05/2015.
 */
public class Controleur extends Activity {

    private static final int SELECT_PICTURE = 1;

    private boolean imageChargee = false;
    private int valeurLumi = 50;
    private double valeurContraste = 0, valeurAcuite = 10;
    private ImageView image;
    private Bitmap bMap;
    private Bitmap original;
    private TextView tubulaireValue, scotomeValue, hemiaValue;
    private SeekBar scotomeSB, tubulaireSB, hemiaSB, contrasteSB, luminositeSB;
    private RadioButton normalRB, scotomeRB, tubuRB, hemiaRB;
    private Spinner hemiaSpinner;
    private CheckBox niveauDeGrisCB;
    private EditText acuiteText;

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

    /**
     * Permet d'iniatiliser OpenCV pour toutes les versions d'android (Lolipop inclut)
     * @param Version
     * @param AppContext
     * @param Callback
     * @return
     */
    public static boolean initOpenCV(String Version, final Context AppContext,
                                     final LoaderCallbackInterface Callback) {
        AsyncServiceHelper helper = new AsyncServiceHelper(Version, AppContext,
                Callback);
        Intent intent = new Intent("org.opencv.engine.BIND");
        intent.setPackage("org.opencv.engine");
        if (AppContext.bindService(intent, helper.mServiceConnection,
                Context.BIND_AUTO_CREATE)) {
            return true;
        } else {
            AppContext.unbindService(helper.mServiceConnection);
            helper.InstallService(AppContext, Callback);
            return false;
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.application_activity);
        if (!initOpenCV(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mOpenCVCallBack))
        {
            Log.e("OpenCV", "Cannot connect to OpenCV Manager");
        }

        //fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hemiaSpinner = (Spinner)findViewById(R.id.spinner_hemia);
        image = (ImageView)findViewById(R.id.image);
        normalRB = (RadioButton)findViewById(R.id.radioButton_normal);
        scotomeRB = (RadioButton)findViewById(R.id.radioButton_scotome);
        tubuRB = (RadioButton)findViewById(R.id.radioButton_tubulaire);
        hemiaRB = (RadioButton)findViewById(R.id.radioButton_hemianopsie);
        niveauDeGrisCB = (CheckBox)findViewById(R.id.checkBox_niveauDeGris);
        acuiteText = (EditText)findViewById(R.id.editText_acuite);
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

        /**
         * Listener pour le champ editable de l'acuité, on vérifie le texte lorsqu'on fait "OK" sur le clavier
         */
        acuiteText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (imageChargee) {
                    // If the event is a key-down event on the "enter" button
                    if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                            (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        if (acuiteText.getText().toString().equals("")) {
                            String acuite = Double.toString(valeurAcuite);
                            acuiteText.setText(acuite.toCharArray(), 0, acuite.length());
                        } else {
                            Double valeur = Double.parseDouble(acuiteText.getText().toString());
                            final Double valeurBuff;
                            if (valeur > 10) {
                                acuiteText.setText("10".toCharArray(), 0, 2);
                                valeurBuff = 10.0;
                            } else {
                                valeurBuff = valeur;
                            }
                            // On ne recharge pas l'image si l'acuité n'est pas différente
                            if (valeurBuff != valeurAcuite) {
                                final ProgressDialog progDailog = ProgressDialog.show(Controleur.this, "Chargement de l'image ...",
                                        "Veuillez patienter", true);
                                new Thread() {
                                    public void run() {
                                        try {
                                            bMap = original.copy(Bitmap.Config.ARGB_8888, true);
                                            valeurAcuite = valeurBuff;
                                            if (niveauDeGrisCB.isChecked()) {
                                                bMap = convertionNiveauDeGris(bMap);
                                            }
                                            image.setImageBitmap(
                                                    changerLuminosite(
                                                            changerContraste(
                                                                    changementFlou(bMap, valeurAcuite), valeurContraste), valeurLumi));
                                        } catch (Exception e) {
                                        }
                                        progDailog.dismiss();
                                    }
                                }.start();
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
        });

        /**
         * Listener pour la SeekBar Scotome et le texte associé
         */
        scotomeValue = (TextView)findViewById(R.id.textView_scotome);
        scotomeSB = (SeekBar)findViewById(R.id.seekBar_scotome);
        scotomeSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                scotomeValue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        /**
         * Listener pour la SeekBar Tubulaire et le texte associé
         */
        tubulaireValue = (TextView)findViewById(R.id.textView_tubu);
        tubulaireValue.setEnabled(false);
        tubulaireSB = (SeekBar)findViewById(R.id.seekBar_tubulaire);
        tubulaireSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tubulaireValue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        /**
         * Listener pour la SeekBar Hemianopsie et le texte associé
         */
        hemiaValue = (TextView)findViewById(R.id.textView_hemia);
        hemiaSB = (SeekBar)findViewById(R.id.seekBar_hemia);
        hemiaSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                hemiaValue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        /**
         * Listener pour la SeekBar Contraste et le texte associé
         */
        final TextView contrasteValue = (TextView)findViewById(R.id.textView_contraste_sb);
        contrasteSB = (SeekBar)findViewById(R.id.seekBar_contraste);
        contrasteSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                contrasteValue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (imageChargee) {
                    final SeekBar mySeekBar = seekBar;
                    final ProgressDialog progDailog = ProgressDialog.show(Controleur.this, "Chargement de l'image ...",
                            "Veuillez patienter", true);
                    new Thread() {
                        public void run() {
                            try {
                                bMap = original.copy(Bitmap.Config.ARGB_8888, true);
                                valeurContraste = 0 - ((100 - (mySeekBar.getProgress())) * 0.8);
                                if (niveauDeGrisCB.isChecked()) {
                                    bMap = convertionNiveauDeGris(bMap);
                                }
                                image.setImageBitmap(
                                        changerLuminosite(
                                                changerContraste(
                                                        changementFlou(bMap, valeurAcuite), valeurContraste), valeurLumi));
                            } catch (Exception e) {
                            }
                            progDailog.dismiss();
                        }
                    }.start();
                }
            }
        });

        /**
         * Listener pour la SeekBar Luminosite et le texte associé
         */
        final TextView luminositeValue = (TextView)findViewById(R.id.textView_luminosite_sb);
        luminositeSB = (SeekBar)findViewById(R.id.seekBar_luminosite);
        luminositeSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                luminositeValue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (imageChargee) {
                    final SeekBar mySeekBar = seekBar;
                    final ProgressDialog progDailog = ProgressDialog.show(Controleur.this, "Chargement de l'image ...",
                            "Veuillez patienter", true);
                    new Thread() {
                        public void run() {
                            try {
                                bMap = original.copy(Bitmap.Config.ARGB_8888, true);
                                if (mySeekBar.getProgress() > 50) {
                                    valeurLumi = (mySeekBar.getProgress() - 50) * 2;
                                } else if (mySeekBar.getProgress() < 50) {
                                    valeurLumi = 0 - ((50 - mySeekBar.getProgress()) * 2);
                                } else {
                                    valeurLumi = 0;
                                }
                                if (niveauDeGrisCB.isChecked()) {
                                    bMap = convertionNiveauDeGris(bMap);
                                }
                                image.setImageBitmap(
                                        changerLuminosite(
                                                changerContraste(
                                                        changementFlou(bMap, valeurAcuite), valeurContraste), valeurLumi));
                            } catch (Exception e) {
                            }
                            progDailog.dismiss();
                        }
                    }.start();
                }
            }
        });

        /**
         * Listener pour la checkBox
         */
        niveauDeGrisCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (imageChargee) {
                    final Boolean checked = isChecked;
                    final ProgressDialog progDailog = ProgressDialog.show(Controleur.this, "Chargement de l'image ...",
                            "Veuillez patienter", true);
                    new Thread() {
                        public void run() {
                            try {
                                bMap = original.copy(Bitmap.Config.ARGB_8888, true);
                                if (checked) {
                                    bMap = convertionNiveauDeGris(bMap);
                                }
                                image.setImageBitmap(
                                        changerLuminosite(
                                                changerContraste(
                                                        changementFlou(bMap, valeurAcuite), valeurContraste), valeurLumi));
                            } catch (Exception e) {
                            }
                            progDailog.dismiss();
                        }
                    }.start();
                }
            }
        });
        mettreValeursDefaut();
    }

    /**
     * Ajout du menu à la barre d'action
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    /**
     * Action à faire lorsque qu'on tap sur l'icone d'A propos
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_about:
                Log.i("A propos", "A propos");
                new AlertDialog.Builder(this)
                        .setTitle("À propos")
                        .setMessage("Simulateur Basse Vision ©\n\nVersion application : 0.2")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // ne rien faire
                            }
                        })
                        .setIcon(R.drawable.dialog_information)
                        .show();
                // Comportement du bouton "A Propos"
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Résultat de l'activité lorsqu'une image est sélectionnée
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                mettreValeursDefaut();
                Uri selectedImageUri = data.getData();
                String selectedImagePath = getPath(selectedImageUri);
                bMap = BitmapFactory.decodeFile(selectedImagePath);
                Mat imageMat = new Mat ( bMap.getHeight(), bMap.getWidth(), CvType.CV_8UC1);
                Utils.bitmapToMat(bMap, imageMat);
                Utils.matToBitmap(imageMat, bMap);
                Log.i("Format", (float)bMap.getHeight() /(float)bMap.getWidth()+"");
                if ((float)bMap.getHeight() /(float)bMap.getWidth() == .5625 ||
                        (float)bMap.getWidth() / (float)bMap.getHeight() == .5625) { //16/9
                    if (bMap.getHeight() < bMap.getWidth()) {
                        bMap = Bitmap.createScaledBitmap(bMap, 1024, 576, false);
                    } else {
                        bMap = Bitmap.createScaledBitmap(bMap, 576, 1024, false);
                    }
                } else if ((float)bMap.getHeight() / (float)bMap.getWidth() == .625 ||
                        (float)bMap.getWidth() / (float)bMap.getHeight() == .625) { //16/10
                    if (bMap.getHeight() < bMap.getWidth()) {
                        bMap = Bitmap.createScaledBitmap(bMap, 960, 600, false);
                    } else {
                        bMap = Bitmap.createScaledBitmap(bMap, 600, 960, false);
                    }
                } else if ((float)bMap.getHeight() / (float)bMap.getWidth() == .75 ||
                        (float)bMap.getWidth() / (float)bMap.getHeight() == .75) { //4/3
                    if (bMap.getHeight() < bMap.getWidth()) {
                        bMap = Bitmap.createScaledBitmap(bMap, 960, 720, false);
                    } else {
                        bMap = Bitmap.createScaledBitmap(bMap, 720, 960, false);
                    }
                }
                image.setImageBitmap(bMap);
                original = bMap;
                imageChargee = true;
            }
        }
    }

    /**
     * Modifie le flou gaussien de l'image
     * @param src
     * @return bitmap avec flou modifié
     */
    public static Bitmap changementFlou(Bitmap src, double valeur) {
        if (valeur != 10) {
            Mat tmp = new Mat(src.getWidth(), src.getHeight(), CvType.CV_8UC1);
            Utils.bitmapToMat(src, tmp);
            Double size = 100 - (valeur/10 * 100);
            if (size % 2 == 0) {
                size ++;
            }
            org.opencv.core.Size s = new Size(size, size);
            Log.i("acuite", size+"");
            Imgproc.GaussianBlur(tmp, tmp, s, 0, 0);
            Utils.matToBitmap(tmp, src);
        }
        return src;
    }

    /**
     * Convertit l'image en niveau de gris
     * @param src
     * @return bitmap en niveau de gris
     */
    public static Bitmap convertionNiveauDeGris(Bitmap src) {
        Mat tmp = new Mat(src.getWidth(), src.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(src, tmp);
        Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_RGB2GRAY);
        Utils.matToBitmap(tmp, src);
        return src;
    }

    /**
     * Change la luminosite de l'image
     * @param src
     * @param valeur
     * @return bitmap avec nouvelle luminosite
     */
    public static Bitmap changerLuminosite(Bitmap src, int valeur) {
        Mat imageMat = new Mat(src.getHeight(), src.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(src, imageMat);
        imageMat.convertTo(imageMat, -1, 1, valeur);
        Utils.matToBitmap(imageMat, src);
        return src;
    }

    /**
     * Change la cotraste de l'image
     * @param src
     * @param valeur
     * @return bitmap avec nouvelle contraste
     */
    public static Bitmap changerContraste(Bitmap src, double valeur) {
        // image size
        int width = src.getWidth();
        int height = src.getHeight();
        // create output bitmap
        Bitmap bmOut = Bitmap.createBitmap(width, height, src.getConfig());
        // color information
        int A, R, G, B;
        int pixel;
        // get contrast value
        double contrast = Math.pow((100 + valeur) / 100, 2);

        // scan through all pixels
        for(int x = 0; x < width; ++x) {
            for(int y = 0; y < height; ++y) {
                // get pixel color
                pixel = src.getPixel(x, y);
                A = Color.alpha(pixel);
                // apply filter contrast for every channel R, G, B
                R = Color.red(pixel);
                R = (int)(((((R / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
                if(R < 0) { R = 0; }
                else if(R > 255) { R = 255; }

                G = Color.green(pixel);
                G = (int)(((((G / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
                if(G < 0) { G = 0; }
                else if(G > 255) { G = 255; }

                B = Color.blue(pixel);
                B = (int)(((((B / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
                if(B < 0) { B = 0; }
                else if(B > 255) { B = 255; }

                // set new pixel color to output bitmap
                bmOut.setPixel(x, y, Color.argb(A, R, G, B));
            }
        }
        // return final image
        return bmOut;
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
        niveauDeGrisCB.setChecked(false);
        miseAJourSliders();
        acuiteText.setText("10".toCharArray(), 0, 2);
    }

    /**
     * Met à jour les slider en fonction des radio buttons
     */
    private void miseAJourSliders() {
        if (normalRB.isChecked()) {
            scotomeSB.setEnabled(false);
            tubulaireSB.setEnabled(false);
            hemiaSB.setEnabled(false);
            scotomeValue.setEnabled(false);
            tubulaireValue.setEnabled(false);
            hemiaValue.setEnabled(false);
            hemiaSpinner.setEnabled(false);
        } else if (scotomeRB.isChecked()) {
            scotomeSB.setEnabled(true);
            tubulaireSB.setEnabled(false);
            hemiaSB.setEnabled(false);
            scotomeValue.setEnabled(true);
            tubulaireValue.setEnabled(false);
            hemiaValue.setEnabled(false);
            hemiaSpinner.setEnabled(false);
        } else if (tubuRB.isChecked()) {
            scotomeSB.setEnabled(false);
            tubulaireSB.setEnabled(true);
            hemiaSB.setEnabled(false);
            scotomeValue.setEnabled(false);
            tubulaireValue.setEnabled(true);
            hemiaValue.setEnabled(false);
            hemiaSpinner.setEnabled(false);
        } else if (hemiaRB.isChecked()) {
            scotomeSB.setEnabled(false);
            tubulaireSB.setEnabled(false);
            hemiaSB.setEnabled(true);
            scotomeValue.setEnabled(false);
            tubulaireValue.setEnabled(false);
            hemiaValue.setEnabled(true);
            hemiaSpinner.setEnabled(true);
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
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
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
