package tv.cloudwalker.cwnxt.transavrotv;

import android.Manifest;
import android.app.Instrumentation;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

public class MainActivity extends FragmentActivity  implements RecognitionListener {

    private SpeechRecognizer recognizer;
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final String TAG = "MainActivity";
    public static final String VOICE_COMMAND = "voiceCommands";
    public static final String WAKE_WORD = "wakeWord";
    private Instrumentation m_Instrumentation ;
    private PackageManager packageManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        m_Instrumentation = new Instrumentation();
        packageManager = getPackageManager();
        // Check if user has given permission to record audio
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
        new SetupTask(this).execute();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull  int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                new SetupTask(this).execute();
            } else {
                finish();
            }
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Glide.get(this).onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Glide.get(this).onTrimMemory(level);
    }

    private static class SetupTask extends AsyncTask<Void, Void, Exception> {
        WeakReference<MainActivity> activityReference;
        SetupTask(MainActivity activity) {
            this.activityReference = new WeakReference<>(activity);
        }
        @Override
        protected Exception doInBackground(Void... params) {
            try {
                Assets assets = new Assets(activityReference.get());
                File assetDir = assets.syncAssets();
                activityReference.get().setupRecognizer(assetDir);
            } catch (IOException e) {
                return e;
            }
            return null;
        }
        @Override
        protected void onPostExecute(Exception result) {
            if (result != null) {
                Log.e(TAG, "onPostExecute: ", result);
            } else {
                activityReference.get().switchSearch(WAKE_WORD);
            }
        }
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        Log.i(TAG, "setupRecognizer: ");

        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "6448.dic"))
                .setKeywordThreshold(1.0f)
                .getRecognizer();

        recognizer.addListener(this);


        recognizer.addKeyphraseSearch(WAKE_WORD, "Hey makasare tv");

        File languageModel = new File(assetsDir, "6448.lm");
        recognizer.addNgramSearch(VOICE_COMMAND, languageModel);

    }


    @Override
    public void onBeginningOfSpeech() {
        Log.i(TAG, "onBeginningOfSpeech: ");
    }

    @Override
    public void onEndOfSpeech() {
        Log.i(TAG, "onEndOfSpeech: "+recognizer.getSearchName());
        if (!recognizer.getSearchName().equals(WAKE_WORD))
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "EOS Yes !! Listening.. Go ahead", Toast.LENGTH_SHORT).show();
                }
            });
        switchSearch(VOICE_COMMAND);
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {

    }

    private void switchSearch(String searchName) {
        recognizer.stop();
        if (searchName.equals(WAKE_WORD))
            recognizer.startListening(searchName);
        else
            recognizer.startListening(searchName, 10000);
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            Log.i(TAG, "onResult: "+hypothesis.getHypstr()+"    "+hypothesis.getProb()+"  "+hypothesis.getBestScore());
            trrigerCommands(hypothesis.getHypstr());
        }
    }

    private void trrigerCommands(String commands){
        switch (commands)
        {
            case "MOVE TO LEFT":
            case "LEFT":
            case "GO TO LEFT":
            {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        m_Instrumentation.sendKeyDownUpSync(21);
                        Thread.currentThread().interrupt();
                    }
                }).start();

            }
            break;
            case "MOVE TO RIGHT":
            case "RIGHT":
            case "GO TO RIGHT":
            {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        m_Instrumentation.sendKeyDownUpSync(22);
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
            break;
            case "TOP":
            {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        m_Instrumentation.sendKeyDownUpSync(19);
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
            break;
            case "OPEN":
            case "SELECT":
            {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        m_Instrumentation.sendKeyDownUpSync(23);
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
            break;
            case "BACK":
            {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        m_Instrumentation.sendKeyDownUpSync(3);
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
            break;
            case "GO TO NETFLIX":
            {

            }
            break;
            case "GO TO YOUTUBE":
            {
                startActivity(packageManager.getLeanbackLaunchIntentForPackage("com.google.android.youtube.tv"));
            }
            break;
            case "GO TO HOTSTAR":
            {
                startActivity(packageManager.getLeanbackLaunchIntentForPackage("in.startv.hotstar"));
            }
            break;
            case "GO TO PRIME":
            case "GO TO AMAZON PRIME":
            case "OPEN PRIME":
            case "OPEN AMAZON PRIME":
            {

            }
            break;
            case "GO TO HUNGAMA PLAY":
            case "OPEN HUNGAMA PLAY":
            {

            }
            break;
            case "GO TO ZEE FIVE":
            case "OPEN ZEE FIVE":
            {

            }
            break;
            case "GO TO VOOT":
            case "OPEN VOOT":
            {

            }
            break;
            case "GO TO MOVIE BOX":
            case "OPEN MOVIE BOX":
            {
                startActivity(packageManager.getLaunchIntentForPackage("tv.cloudwalker.cloudwalkeruniverse"));
            }
            break;
            case "GO TO ALL APPS":
            case "MOVE TO ALL APPS":
            {

            }
            break;
            case "GO TO SEARCH":
            case "MOVE TO SEARCH":
            {

            }
            break;
            default:
            {
                Log.i(TAG, "trrigerCommands: "+commands);
            }
        }
    }

    @Override
    public void onError(Exception e) {
        Log.i(TAG, "onError: "+e.getMessage());
        System.out.println(e.getMessage());
    }

    @Override
    public void onTimeout() {
        Log.i(TAG, "onTimeout: ");
        switchSearch(VOICE_COMMAND);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }
}































//    ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("192.168.1.143", 50051).usePlaintext().build();
//    TileServiceGrpc.TileServiceStub tileServiceAsyncStub = TileServiceGrpc.newStub(managedChannel);
//        startTime = System.currentTimeMillis();
//                tileServiceAsyncStub.getMovieTiles(TileServiceOuterClass.RowId.newBuilder().setRowId("cloudwalkerLatestMovies").build(), new StreamObserver<TileServiceOuterClass.MovieTile>() {
//        int counter = 0;
//@Override
//public void onNext(TileServiceOuterClass.MovieTile value) {
//        Log.i(TAG, "onNext: "+counter++);
//        if(tempCounter == 0){
//        endTime = System.currentTimeMillis();
//        tempCounter++ ;
//        }
//        }
//
//@Override
//public void onError(Throwable t) {
//        Log.e(TAG, "onError: ",t );
//        }
//
//@Override
//public void onCompleted() {
//        long duration = (endTime - startTime);
//        Log.i(TAG, "onCompleted: "+duration+" "+counter);
//        }
//        });