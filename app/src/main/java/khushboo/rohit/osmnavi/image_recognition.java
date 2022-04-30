package khushboo.rohit.osmnavi;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.AnalysisResult;
import com.microsoft.projectoxford.vision.contract.Caption;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Locale;

public class image_recognition extends AppCompatActivity {
    // free subscription key used here for working of the below function(api)
    public VisionServiceClient visionServiceClient = new VisionServiceRestClient("22f6b89ef3dd4e1ea36d02a96cc27f02","https://westcentralus.api.cognitive.microsoft.com/vision/v2.0");
    private static final int CAMERA_REQUEST = 1888;
    private ImageView imageView;
    Bitmap photo;
    TextToSpeech toSpeech;
    int result;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_recognition);
        this.imageView = (ImageView)this.findViewById(R.id.imageView1);
        Button photoButton = (Button) this.findViewById(R.id.button1);
        photoButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);

            }
        });

/*
     toSpeech = new TextToSpeech(image_recognition.this, new TextToSpeech.OnInitListener() {
         @Override
         public void onInit(int i) {
            if(i==TextToSpeech.SUCCESS){
                result = toSpeech.setLanguage(Locale.UK);
            }
            else{
                Toast.makeText(getApplicationContext(),"Feature not supported",Toast.LENGTH_SHORT).show();
                }
         }
     });*/

    }
/*
    public void TTS(View view){
      switch(view.getId()){
          case R.id.button2:
              if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                  Toast.makeText(getApplicationContext(),"Feature not supported",Toast.LENGTH_SHORT).show();
              }
              else{
                  TextView textView = (TextView) findViewById(R.id.responseView);
                  String input = textView.getText().toString();
                  toSpeech.speak(input,TextToSpeech.QUEUE_FLUSH,null);
              }
              break;
          case R.id.button3:
              if(toSpeech!=null){
                  toSpeech.stop();
              }
              break;

      }
    }
*/
/*
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(toSpeech!=null){
            toSpeech.stop();
            toSpeech.shutdown();
        }
    }*/

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            photo = (Bitmap) data.getExtras().get("data");
            Intent i = new Intent(this, ResultActivity.class);
            i.putExtra("name",photo);
            startActivity(i);

        }

    }
}
