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
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.File;

import static org.opencv.core.Core.circle;

/**
 * Created by l.bled on 19/05/2015.
 */
public class Controleur extends Activity {

    private static final int COEFFICIENT_LUMINOSITE = 2;
    private static final double COEFFICIENT_CONTRASTE = .8, COEFFICIENT_SCOTOME= 2.5;
    private static final int SELECT_PICTURE = 1, SELECT_CONFIG = 2;

    private Configuration maConfig = new Configuration();
    private boolean imageChargee = false;
    private ImageView image;
    private Bitmap bMap;
    private Bitmap original;
    private TextView tubulaireValue, scotomeValue, hemiaValue;
    private SeekBar scotomeSB, tubulaireSB, hemiaSB, contrasteSB, luminositeSB;
    private RadioButton normalRB, scotomeRB, tubuRB, hemiaRB;
    private Spinner hemiaSpinner;
    private CheckBox niveauDeGrisCB;
    private EditText acuiteText;
    private Button importButton, exportButton;

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
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, SELECT_PICTURE);
            }
        });

        /**
         * Listener pour le bouton exporter
         */
        exportButton = (Button)findViewById(R.id.button_exporter_conf);
        exportButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                if (imageChargee) {
                    final File directory = new File(Environment.getExternalStorageDirectory() + File.separator + "SBV Configurations");
                    directory.mkdirs();
                    final Serializer serializer = new Persister();
                    final AlertDialog.Builder alert = new AlertDialog.Builder(Controleur.this);

                    alert.setTitle("Exporter");
                    alert.setMessage("Entrez le nom du fichier XML");

                    // Set an EditText view to get user input
                    final EditText input = new EditText(Controleur.this);
                    alert.setView(input);

                    alert.setPositiveButton("Valider", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String nomFichier = input.getText().toString();
                            if (nomFichier.length() > 0) {
                                File result = new File(directory, input.getText().toString() + ".xml");
                                try {
                                    serializer.write(maConfig, result);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                Toast.makeText(Controleur.this, "Fichier de configuration " + nomFichier +".xml créé.",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(Controleur.this, "Erreur sauvegarde.",
                                        Toast.LENGTH_LONG).show();
                            }
                            hideKeyboard(input);
                        }
                    });

                    alert.setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Canceled.
                            hideKeyboard(input);
                        }
                    });
                    alert.show();
                }
            }
        });


        importButton = (Button)findViewById(R.id.button_importer_conf);
        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imageChargee) {
                    Intent explorerIntent = new Intent(
                            Intent.ACTION_GET_CONTENT);
                    explorerIntent.setType("file/xml");
                    try {
                        startActivityForResult(explorerIntent, SELECT_CONFIG);
                    } catch (android.content.ActivityNotFoundException ex) {
                        Toast.makeText(Controleur.this, "Veuillez installez un explorateur de fichier",
                                Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                miseAJourSliders();
                maConfig.setMonRadioGroup(group);
                mettreValeursDefautChampVisuel();
            }
        });


        acuiteText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(acuiteText);
                }
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
                            String acuite = Double.toString(maConfig.getAcuite());
                            acuiteText.setText(acuite.toCharArray(), 0, acuite.length());
                        } else {
                            Double valeur = Double.parseDouble(acuiteText.getText().toString());
                            if (valeur > 10) {
                                acuiteText.setText("10".toCharArray(), 0, 2);
                                valeur = 10.0;
                            } else if (valeur < .01) {
                                acuiteText.setText("0.01".toCharArray(), 0, 4);
                                valeur = .01;
                            }
                            // On recharge l'image seulement si l'acuité a changé
                            if (valeur != maConfig.getAcuite()) {
                                maConfig.setAcuite(valeur);
                                mettreAJourImage();
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
                int valeur = seekBar.getProgress();
                Log.i("Scotome", valeur+"");
                bMap = original.copy(Bitmap.Config.ARGB_8888, true);
                Mat tmp = new Mat(bMap.getWidth(), bMap.getHeight(), CvType.CV_8UC1);
                Utils.bitmapToMat(bMap, tmp);
                //Double to Int -> Double > String > Int
                valeur = Integer.parseInt(Double.toString(Math.floor(valeur * COEFFICIENT_SCOTOME)));
                circle(tmp,
                        new Point(tmp.width() / 2, tmp.height() / 2),
                        valeur,
                        new Scalar(235, 235, 235, 50),
                        -1);
                Utils.matToBitmap(tmp, bMap);
                image.setImageBitmap(bMap);
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
                    maConfig.setContrasteSB(seekBar.getProgress());
                    double buffer = maConfig.getContraste();
                    maConfig.setContraste(0 - ((100 - seekBar.getProgress()) * COEFFICIENT_CONTRASTE));
                    // On met à jour l'image seulement si la valeur est différente
                    if (buffer != maConfig.getContraste()) {
                        mettreAJourImage();
                    }
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
                    int valeur = seekBar.getProgress();
                    maConfig.setLuminositeSB(valeur);
                    int buffer = maConfig.getLuminosite();
                    if (valeur > 50) {
                        maConfig.setLuminosite((valeur - 50) * COEFFICIENT_LUMINOSITE);
                    } else if (seekBar.getProgress() < 50) {
                        maConfig.setLuminosite(0 - ((50 - valeur) * COEFFICIENT_LUMINOSITE));
                    } else {
                        maConfig.setLuminosite(0);
                    }
                    // On met à jour l'image seulement si la valeur est différente
                    if (buffer != maConfig.getLuminosite()) {
                        mettreAJourImage();
                    }
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
                    mettreAJourImage();
                    maConfig.setIsNiveauDeGris(isChecked);
                }
            }
        });

        mettreValeursDefaut();
        desactiverElements();
    }

    /**
     * Permet de cacher le clavier lorsqu'il n'est pas caché automatiquement
     * @param editText
     */
    public void hideKeyboard(EditText editText) {
        if (editText != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        }
    }

    /**
     * Met à jour l'image avec les valeurs sélectionnés
     * Création du thread pour afficher le chargement de l'image
     */
    public void mettreAJourImage() {
        final ProgressDialog progDailog = ProgressDialog.show(Controleur.this, "Chargement de l'image...",
                "Veuillez patienter", true);
        new Thread() {
            public void run() {
                try {
                    bMap = original.copy(Bitmap.Config.ARGB_8888, true);

                    if (maConfig.isNiveauDeGris()) {
                        bMap = convertionNiveauDeGris(bMap);
                    }

                    bMap = changerLuminosite(
                                changerContraste(
                                        changementFlou(bMap, maConfig.getAcuite()),
                                    maConfig.getContraste()),
                                maConfig.getLuminosite());
                    image.setImageBitmap(bMap);
                } catch (Exception e) {
                }
                progDailog.dismiss();
            }
        }.start();
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
                        .setMessage("Simulateur Basse Vision ©\n\nVersion application : 0.3")
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
                imageChargee = false;
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
                activerElements();
            } else if (requestCode == SELECT_CONFIG) {
                Serializer serializer = new Persister();
                Uri selectedConfigUri = data.getData();
                String selectedConfigPath = getPath(selectedConfigUri);
                File monXML = new File(selectedConfigPath);
                try {
                    serializer.read(maConfig, monXML);
                    Toast.makeText(Controleur.this, "Configuration importée",
                            Toast.LENGTH_LONG).show();
                    mettreAJourIHM();
                    mettreAJourImage();
                } catch (Exception e) {
                    Toast.makeText(Controleur.this, "Erreur importation",
                            Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Met à jour l'IHM en fonction des valeurs de maConfig
     */
    public void mettreAJourIHM() {
        String acuite = Double.toString(maConfig.getAcuite());
        acuiteText.setText(acuite.toCharArray(), 0, acuite.length());
        niveauDeGrisCB.setChecked(maConfig.isNiveauDeGris());
        contrasteSB.setProgress(maConfig.getContrasteSB());
        luminositeSB.setProgress(maConfig.getLuminositeSB());
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
            Double size = 0.;
            /*size = Math.exp(10.0 - valeur) / COEFFICIENT_FLOU;
            if (Math.floor(size) % 2 != 1) {
                size ++;
            }*/
            if (valeur >= 6) {
                size = 1.;
            } else if (valeur >= 4) {
                size = 1.8;
            } else if (valeur >= 3) {
                size = 3.;
            } else if (valeur >= 2) {
                size = 9.1;
            } else if (valeur >= 1.6) {
                size = 9.9;
            } else if (valeur >= 1.3) {
                size = 11.9;
            } else if (valeur >= 1) {
                size = 13.;
            } else if (valeur >= .5) {
                size = 49.;
            } else if (valeur >= .1) {
                size = 199.;
            } else if (valeur < .1) {
                size = 219.;
            }
            Log.i("Acuite", size.toString());
            org.opencv.core.Size s = new Size(size, size);
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
        if (valeur != 0) {
            Mat imageMat = new Mat(src.getHeight(), src.getWidth(), CvType.CV_8UC1);
            Utils.bitmapToMat(src, imageMat);
            imageMat.convertTo(imageMat, -1, 1, valeur);
            Utils.matToBitmap(imageMat, src);
        }
        return src;
    }

    /**
     * Change la cotraste de l'image
     * @param src
     * @param valeur
     * @return bitmap avec nouvelle contraste
     */
    public static Bitmap changerContraste(Bitmap src, double valeur) {
        if (valeur != 0) {
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
            for (int x = 0; x < width; ++x) {
                for (int y = 0; y < height; ++y) {
                    // get pixel color
                    pixel = src.getPixel(x, y);
                    A = Color.alpha(pixel);
                    // apply filter contrast for every channel R, G, B
                    R = Color.red(pixel);
                    R = (int) (((((R / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
                    if (R < 0) {
                        R = 0;
                    } else if (R > 255) {
                        R = 255;
                    }

                    G = Color.green(pixel);
                    G = (int) (((((G / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
                    if (G < 0) {
                        G = 0;
                    } else if (G > 255) {
                        G = 255;
                    }

                    B = Color.blue(pixel);
                    B = (int) (((((B / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
                    if (B < 0) {
                        B = 0;
                    } else if (B > 255) {
                        B = 255;
                    }

                    // set new pixel color to output bitmap
                    bmOut.setPixel(x, y, Color.argb(A, R, G, B));
                }
            }
            return bmOut;
        }
        // return final image
        return src;
    }

    /**
     * Remet les valeurs par défaut quand on charge une image
     */
    private void mettreValeursDefaut () {
        acuiteText.setText("10".toCharArray(), 0, 2);
        maConfig.setAcuite(10.0);

        scotomeSB.setProgress(0);
        tubulaireSB.setProgress(100);
        hemiaSB.setProgress(100);

        contrasteSB.setProgress(100);
        maConfig.setContrasteSB(100);
        maConfig.setContraste(0);

        luminositeSB.setProgress(50);
        maConfig.setLuminositeSB(50);
        maConfig.setLuminosite(0);

        normalRB.setChecked(true);

        niveauDeGrisCB.setChecked(false);
        maConfig.setIsNiveauDeGris(false);

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

    public void desactiverElements() {
        acuiteText.setEnabled(false);
        normalRB.setEnabled(false);
        scotomeRB.setEnabled(false);
        tubuRB.setEnabled(false);
        hemiaRB.setEnabled(false);
        contrasteSB.setEnabled(false);
        luminositeSB.setEnabled(false);
        niveauDeGrisCB.setEnabled(false);
        importButton.setEnabled(false);
        exportButton.setEnabled(false);
    }

    private void activerElements() {
        acuiteText.setEnabled(true);
        normalRB.setEnabled(true);
        scotomeRB.setEnabled(true);
        hemiaRB.setEnabled(true);
        tubuRB.setEnabled(true);
        contrasteSB.setEnabled(true);
        luminositeSB.setEnabled(true);
        niveauDeGrisCB.setEnabled(true);
        importButton.setEnabled(true);
        exportButton.setEnabled(true);
    }
}
