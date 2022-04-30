package khushboo.rohit.osmnavi;

/**
 * Created by hp pc on 05-12-2018.
 */

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.AnalysisResult;
import com.microsoft.projectoxford.vision.contract.Caption;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ResultActivity extends AppCompatActivity {
    // free subscription key used here for working of the below function(api)
    public VisionServiceClient visionServiceClient = new VisionServiceRestClient("22f6b89ef3dd4e1ea36d02a96cc27f02","https://westcentralus.api.cognitive.microsoft.com/vision/v2.0");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        //getting photo info from image_recognition
        Bitmap photo  = getIntent().getExtras().getParcelable("name");
        ImageView imageView = (ImageView)this.findViewById(R.id.imageView1);
        imageView.setImageBitmap(photo);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        photo.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        //code for getting data from api after sending info regarding photo


        final AsyncTask<InputStream,String,String> visionTask = new AsyncTask<InputStream, String, String>() {
            ProgressDialog mdialog = new ProgressDialog(ResultActivity.this);
            @Override
            protected String doInBackground(InputStream... params) {

                try {
                    publishProgress("recognizing...");
                    String[] features = {"Description"};
                    String[] details = {};
                    //getting result from api and then converting it into required format
                    System.out.println(1);
                    AnalysisResult result = visionServiceClient.analyzeImage(inputStream,features,details);
                    System.out.println(result);
                    String strResult = new Gson().toJson(result);
                    return  strResult;
                } catch (Exception e) {
                    return e.getMessage();
                }

            }
            @Override
            protected void onPreExecute(){
                mdialog.show();
            }

            @Override
            protected void onPostExecute(String s){
                mdialog.dismiss();
                // setting the result we get from api to textview
                TextView textView = (TextView) findViewById(R.id.responseView);
                StringBuilder stringBuilder = new StringBuilder();
                try {
                    AnalysisResult result = new Gson().fromJson(s, AnalysisResult.class);
                    for (Caption caption: result.description.captions) {
                        stringBuilder.append(caption.text);
                    }
                } catch (Exception e) {
                    stringBuilder.append(e.getCause());
                }
                textView.setText(stringBuilder);
            }

            @Override
            protected void onProgressUpdate(String...values){
                mdialog.setMessage(values[0]);
            }
        };


        visionTask.execute(inputStream);

    }
}
