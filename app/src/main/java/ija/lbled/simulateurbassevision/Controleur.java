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
import android.graphics.drawable.BitmapDrawable;
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
import android.widget.AdapterView;
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
import static org.opencv.core.Core.rectangle;

/**
 * Created by l.bled on 19/05/2015.
 */
public class Controleur extends Activity {

    private double acuite;
    private static final int COEFFICIENT_LUMINOSITE_INF = 3;
    private static final double COEFFICIENT_CONTRASTE = .85, COEFFICIENT_SCOTOME= 12.5, COEFFICIENT_LUMI_SUP = 4.5, COEFFICIENT_VISION_TUBULAIRE = 12.5;
    private static final int SELECT_PICTURE = 1, SELECT_CONFIG = 2;

    private Configuration maConfig = new Configuration();
    private boolean imageChargee = false, fromUser =  true;
    private ImageView image, calque;
    private Bitmap bMap, original, bCalque, calqueOriginal;
    private TextView tubulaireValue, scotomeValue, hemiaValue, acuiteDeno, contrasteValue, luminositeValue;
    private SeekBar scotomeSB, tubulaireSB, hemiaSB, contrasteSB, luminositeSB;
    private RadioButton normalRB, scotomeRB, tubuRB, hemiaRB;
    private Spinner hemiaSpinner, distanceSpinner;
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

        hemiaSpinner = (Spinner)findViewById(R.id.spinner_hemianopsie);
        distanceSpinner = (Spinner)findViewById(R.id.spinner_distance);
        image = (ImageView)findViewById(R.id.image);
        calque = (ImageView)findViewById(R.id.calque);
        normalRB = (RadioButton)findViewById(R.id.radioButton_normal);
        scotomeRB = (RadioButton)findViewById(R.id.radioButton_scotome);
        tubuRB = (RadioButton)findViewById(R.id.radioButton_tubulaire);
        hemiaRB = (RadioButton)findViewById(R.id.radioButton_hemianopsie);
        niveauDeGrisCB = (CheckBox)findViewById(R.id.checkBox_niveauDeGris);
        acuiteText = (EditText)findViewById(R.id.editText_acuite);
        acuiteDeno = (TextView)findViewById(R.id.textView_acuite_deno);
        luminositeValue = (TextView)findViewById(R.id.textView_luminosite_sb);
        contrasteValue = (TextView)findViewById(R.id.textView_contraste_sb);

        /**
         * Listener pour le bouton pour importer l'image
         */
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

        /**
         * Listener pour le bouton importer
         */
        importButton = (Button)findViewById(R.id.button_importer_conf);
        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imageChargee) {
                    Intent explorerIntent = new Intent(
                            Intent.ACTION_PICK);
                    //explorerIntent.putExtra("CONTENT_TYPE", "file/*");
                    explorerIntent.setAction("com.sec.android.app.myfiles.PICK_DATA");
                    try {
                        startActivityForResult(explorerIntent, SELECT_CONFIG);
                    } catch (android.content.ActivityNotFoundException ex) {
                        Toast.makeText(Controleur.this, "Veuillez installez un explorateur de fichier",
                                Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        /**
         * Listener pour lorsqu'on coche quelque chose de différent dans le radio group
         */
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                miseAJourSliders();
                calque.setImageBitmap(calqueOriginal);
                if (tubuRB.isChecked()) {
                    mettreAJourCalque();
                }
                // On met seulement les valeurs par défaut si c'est un changement direct de l'utilisateur (et non l'importation)
                if (fromUser) {
                    mettreValeursDefautChampVisuel();
                }
            }
        });

        /**
         * Sert à cacher le clavier lorsqu'on le touche à côté de celui-ci
         */
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
                                acuite = maConfig.getAcuite();
                                distanceSpinner.setSelection(1);
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
         * Listener pour le spinner de la distance
         */
        distanceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        acuite = maConfig.getAcuite() / 2; //8m
                        break;
                    case 1:
                        acuite = maConfig.getAcuite(); //4m (defaut)
                        break;
                    case 2:
                        acuite = maConfig.getAcuite() * 2; //2m
                        break;
                    case 3:
                        acuite = maConfig.getAcuite() * 4; //1m
                        break;
                    case 4 : acuite = maConfig.getAcuite() * 8; //.5cm
                        break;
                    case 5 : acuite = maConfig.getAcuite() * 16; //.25cm
                        break;
                    default:
                }
                if (acuite > 10) {
                    acuite = 10;
                }
                Log.i("ACUITE", acuite+"");
                mettreAJourImage();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

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
                scotomeValue.setText(String.valueOf(progress)+"°");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (imageChargee) {
                    int valeur = seekBar.getProgress();
                    //On met à jour que si la valeur change
                    if (maConfig.getScotomeSB() != valeur) {
                        //Double to Int : Double -> String -> Int
                        Double valeurDouble = Math.floor(valeur * COEFFICIENT_SCOTOME);
                        maConfig.setScotome(doubleToInt(valeurDouble));
                        maConfig.setScotomeSB(valeur);
                        mettreAJourCalque();
                    }
                }
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
                tubulaireValue.setText(String.valueOf(progress)+"°");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (imageChargee) {
                    int valeur = seekBar.getProgress();
                    if (maConfig.getVisionTubuSB() != valeur) {
                        Double valeurDouble = Math.floor(valeur * COEFFICIENT_VISION_TUBULAIRE);
                        maConfig.setVisionTubu(doubleToInt(valeurDouble));
                        maConfig.setVisionTubuSB(valeur);
                        mettreAJourCalque();
                    }
                }
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
                hemiaValue.setText(String.valueOf(progress)+"%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (imageChargee) {
                    int valeur = seekBar.getProgress();
                    //On met à jour que si la valeur change
                    if (maConfig.getHemianopsieSB() != valeur) {
                        maConfig.setHemianopsie((100 - valeur));
                        maConfig.setHemianopsieSB(valeur);
                        mettreAJourCalque();
                    }
                }
            }
        });

        /**
         * Listener pour le spinner de l'hemianopsie
         */
        hemiaSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (imageChargee) {
                    if (maConfig.getHemianopsieSB() != 100) {
                        mettreAJourCalque();
                    }
                    maConfig.setHemianopsieID(hemiaSpinner.getSelectedItemPosition());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        /**
         * Listener pour la SeekBar Contraste et le texte associé
         */
        final TextView contrasteValue = (TextView)findViewById(R.id.textView_contraste_sb);
        contrasteSB = (SeekBar)findViewById(R.id.seekBar_contraste);
        contrasteSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                contrasteValue.setText(String.valueOf(progress)+"%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (imageChargee) {
                    int valeur = seekBar.getProgress();
                    // On met à jour l'image seulement si la valeur est différente
                    if (maConfig.getContrasteSB() != valeur) {
                        maConfig.setContrasteSB(valeur);
                        maConfig.setContraste(0 - ((100 - valeur) * COEFFICIENT_CONTRASTE));
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
                luminositeValue.setText(String.valueOf(progress)+"%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (imageChargee) {
                    int valeur = seekBar.getProgress();
                    // Mise à jour seulement si la valeur est différente
                    if (valeur != maConfig.getLuminositeSB()) {
                        maConfig.setLuminositeSB(valeur);
                        if (valeur > 50) {
                            Double valeurDouble = Math.floor((valeur - 50) * COEFFICIENT_LUMI_SUP);
                            maConfig.setLuminosite(doubleToInt(valeurDouble));
                           // maConfig.setLuminosite((valeur - 50) * COEFFICIENT_LUMINOSITE);
                        } else if (seekBar.getProgress() < 50) {
                            maConfig.setLuminosite(0 - ((50 - valeur) * COEFFICIENT_LUMINOSITE_INF));
                        } else {
                            maConfig.setLuminosite(0);
                        }
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
        calqueOriginal = ((BitmapDrawable)calque.getDrawable()).getBitmap();
        mettreValeursDefaut();
        desactiverElements();
    }

    private int doubleToInt(Double valeurDouble) {
        String valeurString = Double.toString(valeurDouble);
        valeurString = valeurString.substring(0, valeurString.length() - 2); // on enlève le ".0" à la fin
        return Integer.parseInt(valeurString);
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
     * Met à jour le calque avec la valeur et le champs visuel sélectionné
     */
    private void mettreAJourCalque() {
        final ProgressDialog progDailog = ProgressDialog.show(Controleur.this, "Chargement de l'image...",
                "Veuillez patienter", true);
        new Thread() {
            public void run() {
                try {
                    bCalque = calqueOriginal.copy(Bitmap.Config.ARGB_8888, true);
                    if (scotomeRB.isChecked()) {
                        bCalque = appliquerScotome(bCalque, maConfig.getScotome());
                    } else if (tubuRB.isChecked()) {
                        bCalque = appliquerVisionTubulaire(bCalque, maConfig.getVisionTubu());
                    } else if (hemiaRB.isChecked()) {
                        bCalque = appliquerHemianopsie(bCalque, maConfig.getHemianopsie());
                    }
                    calque.setImageBitmap(bCalque);
                } catch (Exception e) {
                }
                progDailog.dismiss();
            }
        }.start();
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
                                    changementFlou(bMap, acuite),
                                    maConfig.getContraste()),
                            maConfig.getLuminosite());
                    image.setImageBitmap(bMap);
                }
                catch (Exception e) {
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
                // Comportement du bouton "A Propos"
                new AlertDialog.Builder(this)
                        .setTitle("À propos")
                        .setMessage("Simulateur Basse Vision ©\n\nVersion application : 0.8")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // ne rien faire
                            }
                        })
                        .setIcon(R.drawable.dialog_information)
                        .show();
                return true;
            case R.id.menu_reset:
                if (imageChargee) {
                    mettreValeursDefaut();
                    image.setImageBitmap(original);
                    calque.setImageBitmap(calqueOriginal);
                }
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
                if ((float)bMap.getHeight() /(float)bMap.getWidth() == .5625 ||
                        (float)bMap.getWidth() / (float)bMap.getHeight() == .5625) { //16/9
                    if (bMap.getHeight() < bMap.getWidth()) {
                        //bMap = Bitmap.createScaledBitmap(bMap, 1024, 576, false);
                        bMap = Bitmap.createScaledBitmap(bMap, 1920, 1080, false);

                        calqueOriginal = Bitmap.createScaledBitmap(calqueOriginal, 1024, 576, false);
                    } else {
                        bMap = Bitmap.createScaledBitmap(bMap, 576, 1024, false);
                        calqueOriginal = Bitmap.createScaledBitmap(calqueOriginal, 576, 1024, false);
                    }
                } else if ((float)bMap.getHeight() / (float)bMap.getWidth() == .625 ||
                        (float)bMap.getWidth() / (float)bMap.getHeight() == .625) { //16/10
                    if (bMap.getHeight() < bMap.getWidth()) {
                        bMap = Bitmap.createScaledBitmap(bMap, 960, 600, false);
                        calqueOriginal = Bitmap.createScaledBitmap(calqueOriginal, 960, 600, false);
                    } else {
                        bMap = Bitmap.createScaledBitmap(bMap, 600, 960, false);
                        calqueOriginal = Bitmap.createScaledBitmap(calqueOriginal, 600, 960, false);
                    }
                } else if ((float)bMap.getHeight() / (float)bMap.getWidth() == .75 ||
                        (float)bMap.getWidth() / (float)bMap.getHeight() == .75) { //4/3
                    if (bMap.getHeight() < bMap.getWidth()) {
                        bMap = Bitmap.createScaledBitmap(bMap, 960, 720, false);
                        calqueOriginal = Bitmap.createScaledBitmap(calqueOriginal, 960, 720, false);
                    } else {
                        bMap = Bitmap.createScaledBitmap(bMap, 720, 960, false);
                        calqueOriginal = Bitmap.createScaledBitmap(calqueOriginal, 720, 960, false);
                    }
                }
                image.setImageBitmap(bMap);
                bCalque = calqueOriginal;
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
                    acuite = maConfig.getAcuite();
                    mettreAJourIHM();
                    mettreAJourImage();
                    mettreAJourCalque();
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
        distanceSpinner.setSelection(1);
        acuiteText.setText(acuite.toCharArray(), 0, acuite.length());
        if (maConfig.isScotome()) {
            fromUser = false;
            scotomeRB.setChecked(true);
            scotomeSB.setProgress(maConfig.getScotomeSB());
            fromUser = true;
        } else if (maConfig.isVisionTubu()) {
            fromUser = false;
            tubuRB.setChecked(true);
            tubulaireSB.setProgress(maConfig.getVisionTubuSB());
            fromUser = true;
        }
        else if (maConfig.isHemianopsie()) {
            fromUser = false;
            hemiaRB.setChecked(true);
            hemiaSB.setProgress(maConfig.getHemianopsieSB());
            hemiaSpinner.setSelection(maConfig.getHemianopsieID());
            fromUser = true;
        } else {
            normalRB.setChecked(true);
        }
        miseAJourSliders();
        niveauDeGrisCB.setChecked(maConfig.isNiveauDeGris());
        contrasteSB.setProgress(maConfig.getContrasteSB());
        luminositeSB.setProgress(maConfig.getLuminositeSB());
    }

    /**
     * Modifie le flou gaussien de l'image
     * @param src
     * @return bitmap avec flou modifié
     */
    private Bitmap changementFlou(Bitmap src, double valeur) {
        if (valeur < 10) {
            Mat tmp = new Mat(src.getWidth(), src.getHeight(), CvType.CV_8UC1);
            Utils.bitmapToMat(src, tmp);
            Double size = 0.;
            /*size = Math.exp(10.0 - valeur) / 100;
            if (Math.floor(size) % 2 != 1) {
                size ++;
            }
            Log.i("ACUITE", size + "");*/
            // Flou testé et proche de la réalité pour 4.5m
            if (valeur >= 6) {
                size = 1.;
            } else if (valeur >= 4) {
                size = 1.;
            } else if (valeur >= 3.2) {
                size = 7.;
            } else if (valeur >= 2.5) {
                size = 9.;
            } else if (valeur >= 2) {
                size = 11.;
            } else if (valeur >= 1.6) {
                size = 13.;
            } else if (valeur >= 1.3) {
                size = 15.;
            } else if (valeur >= 1) {
                size = 17.;
            } else if (valeur >= .8) {
                size = 31.;
            } else if (valeur >= .6) {
                size = 49.;
            } else if (valeur >= .5) {
                size = 63.;
            } else if (valeur >= .4) {
                size = 69.;
            } else if (valeur >= .25) {
                size = 81.;
            } else if (valeur >= .1) {
                size = 199.;
            } else if (valeur < .1) {
                size = 219.;
            }
            org.opencv.core.Size s = new Size(size, size);
            Imgproc.GaussianBlur(tmp, tmp, s, 0, 0);
            Utils.matToBitmap(tmp, src);

        }
        return src;
    }

    /**
     * Dessine une cercle flouté au centre de l'image avec un diamètre dépendant de valeur
     * @param bCalque
     * @param valeur
     * @return bitmap avec scotome appliqué
     */
    private Bitmap appliquerScotome(Bitmap bCalque, int valeur) {
        if (valeur != 0) {
            Mat tmp = new Mat(bCalque.getWidth(), bCalque.getHeight(), CvType.CV_8UC1);
            Utils.bitmapToMat(bCalque, tmp);
            circle(tmp,
                    new Point(tmp.width() / 2, tmp.height() / 2),
                    valeur,
                    new Scalar(235, 235, 235, 255),
                    -1);
            Imgproc.GaussianBlur(tmp, tmp, new org.opencv.core.Size(31, 31), 0, 0);
            Utils.matToBitmap(tmp, bCalque);
        }
        return bCalque;
    }

    /**
     * Dessine un rectangle noir de la taille de l'image puis dessine un cercle dont la taille dépend de valeur
     * @param bCalque
     * @param valeur
     * @return
     */
    private Bitmap appliquerVisionTubulaire(Bitmap bCalque, int valeur) {
        if (maConfig.getVisionTubuSB() != 100) {
            Mat tmp = new Mat(bCalque.getWidth(), bCalque.getHeight(), CvType.CV_8UC1);
            Utils.bitmapToMat(bCalque, tmp);
            rectangle(tmp,
                    new Point(0, 0),
                    new Point(bCalque.getWidth(), bCalque.getHeight()),
                    new Scalar(40, 40, 40, 255),
                    -1);
            Imgproc.GaussianBlur(tmp, tmp, new org.opencv.core.Size(21, 21), 0, 0);
            circle(tmp,
                    new Point(tmp.width() / 2, tmp.height() / 2),
                    valeur,
                    new Scalar(0, 0, 0, 0),
                    -1);
            Utils.matToBitmap(tmp, bCalque);
        }
        return bCalque;
    }

    /**
     * Dessine un rectange à gauche ou à droite ou en haut avec une taille dépendante de valeur
     * @param bCalque
     * @param valeur
     * @return
     */
    private Bitmap appliquerHemianopsie(Bitmap bCalque, int valeur) {
        if (valeur != 0) {
            Mat tmp = new Mat(bCalque.getWidth(), bCalque.getHeight(), CvType.CV_8UC1);
            Utils.bitmapToMat(bCalque, tmp);
            if (hemiaSpinner.getSelectedItemPosition() == 0) { // Gauche
                rectangle(tmp,
                        new Point(0, 0),
                        new Point(bCalque.getWidth() * valeur / 100, bCalque.getHeight()),
                        new Scalar(40, 40, 40, 255),
                        -1);
            } else if (hemiaSpinner.getSelectedItemPosition() == 1) { // Droite
                rectangle(tmp,
                        new Point(bCalque.getWidth(), 0),
                        new Point(bCalque.getWidth() - (bCalque.getWidth() * valeur / 100), bCalque.getHeight()),
                        new Scalar(40, 40, 40, 255),
                        -1);
            } else if (hemiaSpinner.getSelectedItemPosition() == 2) { // Haut
                rectangle(tmp,
                        new Point(0, 0),
                        new Point(bCalque.getWidth(), bCalque.getHeight() * valeur / 100),
                        new Scalar(40, 40, 40, 255),
                        -1);
            }
            Imgproc.GaussianBlur(tmp, tmp, new org.opencv.core.Size(21, 21), 0, 0);
            Utils.matToBitmap(tmp, bCalque);
        }
        return bCalque;
    }

    /**
     * Convertit l'image en niveau de gris
     * @param src
     * @return bitmap en niveau de gris
     */
    private static Bitmap convertionNiveauDeGris(Bitmap src) {
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
    private static Bitmap changerLuminosite(Bitmap src, int valeur) {
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
    private static Bitmap changerContraste(Bitmap src, double valeur) {
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
        acuite = maConfig.getAcuite();
        distanceSpinner.setSelection(1);

        scotomeSB.setProgress(0);
        maConfig.setScotomeSB(0);
        maConfig.setScotome(0);
        tubulaireSB.setProgress(100);
        maConfig.setVisionTubu(250); //20 * 12.5 = 250
        maConfig.setVisionTubuSB(20);
        hemiaSB.setProgress(100);
        maConfig.setHemianopsieSB(100);
        maConfig.setHemianopsie(0);

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
            maConfig.setIsScotome(false);
            maConfig.setIsVisionTubu(false);
            maConfig.setIsHemianopsie(false);
            scotomeSB.setEnabled(false);
            tubulaireSB.setEnabled(false);
            hemiaSB.setEnabled(false);
            scotomeValue.setEnabled(false);
            tubulaireValue.setEnabled(false);
            hemiaValue.setEnabled(false);
            hemiaSpinner.setEnabled(false);
        } else if (scotomeRB.isChecked()) {
            maConfig.setIsScotome(true);
            scotomeSB.setEnabled(true);
            tubulaireSB.setEnabled(false);
            hemiaSB.setEnabled(false);
            scotomeValue.setEnabled(true);
            tubulaireValue.setEnabled(false);
            hemiaValue.setEnabled(false);
            hemiaSpinner.setEnabled(false);
        } else if (tubuRB.isChecked()) {
            maConfig.setIsVisionTubu(true);
            scotomeSB.setEnabled(false);
            tubulaireSB.setEnabled(true);
            hemiaSB.setEnabled(false);
            scotomeValue.setEnabled(false);
            tubulaireValue.setEnabled(true);
            hemiaValue.setEnabled(false);
            hemiaSpinner.setEnabled(false);
        } else if (hemiaRB.isChecked()) {
            maConfig.setIsHemianopsie(true);
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
    private void mettreValeursDefautChampVisuel() {
        scotomeSB.setProgress(0);
        maConfig.setScotome(0);
        maConfig.setScotomeSB(0);

        tubulaireSB.setProgress(100);
        maConfig.setVisionTubu(250); // 20 * 12.5 = 250
        maConfig.setVisionTubuSB(20);

        hemiaSB.setProgress(100);
        maConfig.setHemianopsie(0);
        maConfig.setHemianopsieSB(100);
        hemiaSpinner.setSelection(0);
        maConfig.setHemianopsie(hemiaSpinner.getSelectedItemPosition());
    }

    /**
     * helper to retrieve the path of an image URI
     */
    public String getPath(Uri uri) {
        // just some safety built in
        if( uri == null ) {
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

    /**
     * Désactive tous les éléments de l'IHM
     */
    private void desactiverElements() {
        acuiteText.setEnabled(false);
        acuiteDeno.setEnabled(false);
        distanceSpinner.setEnabled(false);
        normalRB.setEnabled(false);
        scotomeRB.setEnabled(false);
        tubuRB.setEnabled(false);
        hemiaRB.setEnabled(false);
        contrasteSB.setEnabled(false);
        contrasteValue.setEnabled(false);
        luminositeSB.setEnabled(false);
        luminositeValue.setEnabled(false);
        niveauDeGrisCB.setEnabled(false);
        importButton.setEnabled(false);
        exportButton.setEnabled(false);
    }

    /**
     * Active tous les éléments de l'IHM
     */
    private void activerElements() {
        acuiteText.setEnabled(true);
        acuiteDeno.setEnabled(true);
        distanceSpinner.setEnabled(true);
        normalRB.setEnabled(true);
        scotomeRB.setEnabled(true);
        hemiaRB.setEnabled(true);
        tubuRB.setEnabled(true);
        contrasteSB.setEnabled(true);
        contrasteValue.setEnabled(true);
        luminositeSB.setEnabled(true);
        luminositeValue.setEnabled(true);
        niveauDeGrisCB.setEnabled(true);
        importButton.setEnabled(true);
        exportButton.setEnabled(true);
    }
}
