package com.example.anilsathyan7.sketch;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.anilsathyan7.sketch.Classifier;
import com.example.anilsathyan7.sketch.Classifier.Device;
import com.example.anilsathyan7.sketch.Classifier.Model;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private SimpleDrawingView sketchview;
    private Bitmap bmp = null;

    //Model Variables
    private Model model = Model.FLOAT;
    private Device device = Device.CPU;
    private int numThreads = -1;
    private Classifier classifier;
    private static final Logger LOGGER = new Logger();
    private long lastProcessingTimeMs;
    public int k=0;
    public Bitmap mybmp=null;
    public TextView recognitionTextView;
    public TextToSpeech tts;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sketchview = (SimpleDrawingView) findViewById(R.id.simpleDrawingView1);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        recognitionTextView = (TextView)findViewById(R.id.textView);

        //Create classifier with default settings
        recreateClassifier(model, device, numThreads);
        if (classifier == null) {
            LOGGER.e("No classifier on preview!");
            return;
        }
        mybmp = BitmapFactory.decodeResource(getResources(), R.drawable.apple2);

        //Opencv load
        if (!OpenCVLoader.initDebug())
            Log.d("ERROR", "Unable to load OpenCV");
        else
            Log.d("SUCCESS", "OpenCV loaded");

        //TTS
        tts=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.UK);
                }
            }
        });
    }


 public void clear(View v){
        sketchview.clear();

 }

 public void save(View v){
        sketchview.save("sketch_img_");
        saveit("smallimg");
 }

    public void saveit (String name){


        Bitmap bmp = sketchview.sketchbitmap();
        String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
        OutputStream outStream = null;
        File file = new File(extStorageDirectory, name+k+".PNG");
        k++;
        try {
            outStream = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
            outStream.close();
        } catch(Exception e) {

        }


    }

 public void classify(View v){

     Bitmap bmp = sketchview.sketchbitmap(); //sketchview.getResizedBitmap(mybmp,28,28) ;
     final long startTime = SystemClock.uptimeMillis();
     final List<Classifier.Recognition> results = classifier.recognizeImage(bmp);
     lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
     Log.d("Time:", String.valueOf(lastProcessingTimeMs));
     Log.d("VAL", String.valueOf(results));
     LOGGER.v("Detect: %s", results);
     showResultsInBottomSheet(results);
 }

    protected void showResultsInBottomSheet(List<Classifier.Recognition> results) {
        if (results != null && results.size() >= 3) {
            Classifier.Recognition recognition = results.get(0);
            Classifier.Recognition recognition1 = results.get(1);
            Classifier.Recognition recognition2= results.get(2);
            if (recognition != null) {
                if (recognition.getTitle() != null) recognitionTextView.setText(recognition.getTitle()+ " : " + String.format("%.2f", (100 * recognition.getConfidence())) + "%" + ", " +
                                                                                recognition1.getTitle()+ " : " + String.format("%.2f", (100 * recognition1.getConfidence())) + "%"  + ", " +
                                                                                recognition2.getTitle()+ " : " + String.format("%.2f", (100 * recognition2.getConfidence())) + "%");

            }

            tts.speak("It looks like a "+recognition.getTitle(), TextToSpeech.QUEUE_FLUSH, null, "tts");

        }


    }

    private void recreateClassifier(Model model, Device device, int numThreads) {
        if (classifier != null) {
            LOGGER.d("Closing classifier.");
            classifier.close();
            classifier = null;
        }
        if (device == Device.GPU && model == Model.QUANTIZED) {
            LOGGER.d("Not creating classifier: GPU doesn't support quantized models.");
            runOnUiThread(
                    () -> {
                        Toast.makeText(this, "GPU does not yet supported quantized models.", Toast.LENGTH_LONG)
                                .show();
                    });
            return;
        }
        try {
            LOGGER.d(
                    "Creating classifier (model=%s, device=%s, numThreads=%d)", model, device, numThreads);
            classifier = Classifier.create(this, model, device, numThreads);
        } catch (IOException e) {
            LOGGER.e(e, "Failed to create classifier.");
        }
    }

}
