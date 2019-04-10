//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license.
//
// Microsoft Cognitive Services (formerly Project Oxford): https://www.microsoft.com/cognitive-services
//
// Microsoft Cognitive Services (formerly Project Oxford) GitHub:
// https://github.com/Microsoft/Cognitive-Emotion-Android
//
// Copyright (c) Microsoft Corporation
// All rights reserved.
//
// MIT License:
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
package com.msp.emotionbattle;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.microsoft.projectoxford.emotion.EmotionServiceClient;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.emotion.contract.FaceRectangle;
import com.microsoft.projectoxford.emotion.contract.RecognizeResult;
import com.microsoft.projectoxford.emotion.rest.EmotionServiceException;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.IdentifyResult;
import com.microsoft.projectoxford.face.contract.TrainingStatus;
import com.msp.emotionbattle.helper.ImageHelper;
import com.msp.emotionbattle.helper.StorageHelper;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class RecognizeActivity extends AppCompatActivity {

    // Flag to indicate which task is to be performed.
    private static final int REQUEST_SELECT_IMAGE = 0;
    private static final int REQUEST_SELECT_IMAGE2 = 1;

    // The button to select an image
    private Button mButtonSelectImage;
    private Button mButtonSelectImage2;

    private boolean player1_finish = false;
    private boolean player2_finish = false;


    // The image selected to detect.
    private Bitmap mBitmap;
    private Bitmap mBitmap2;

    // The edit to show status and result.
    private TextView mTextView;
    private TextView mTextView2;

    private int question_emotion = 0;
    private TextView question_text;

    private TextView winner;
    private TextView p1_notification;
    private TextView p2_notification;
    private double scoreA = 0.0;
    private double scoreB = 0.0;
    private String[] questions;
    private ProgressDialog progressDialog;

    // Background task of face detection.
    private class DetectionTask extends AsyncTask<Void, String, Face[]>{

        private InputStream mInputStream;

        DetectionTask(InputStream inputStream) {
            mInputStream = inputStream;
        }

        protected Face[] doInBackground(Void... params){
            // Get an instance of face service client to detect faces in image.
            FaceServiceRestClient faceServiceRestClient = EmotionBattle.getFaceServiceRestClient();
            try {
                publishProgress("Detecting faces...");

                // Start detection.
                return faceServiceRestClient.detect(
                        mInputStream,  /* Input stream of image to detect */
                        true,       /* Whether to return face ID */
                        false,       /* Whether to return face landmarks */
                        /* Which face attributes to analyze, currently we support:
                           age,gender,headPose,smile,facialHair */
                        null
                );
            } catch (Exception e){
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute(){
            setUiBeforeBackgroundTask();
        }

        @Override
        protected void onProgressUpdate(String... values){
            // Show the status of background detection task on screen.
            setUiDuringBackgroundTask(values[0]);
        }

        @Override
        protected void onPostExecute(final Face[] faces){
            progressDialog.dismiss();

            // Start identification task only if the image to detect is selected.
            if (faces != null) {

                String personGroupID = StorageHelper.getPersonGroupId(RecognizeActivity.this);
                Log.d("recognize", personGroupID);
                if (personGroupID != null && faces.length > 0) {
                    List<UUID> faceIds = new ArrayList<>();
                    for (Face face: faces){
                        faceIds.add(face.faceId);
                    }
                    new IdentificationTask(personGroupID, mInputStream).execute(faceIds.toArray(new UUID[faceIds.size()]));
                } else {
                    if (personGroupID != null)
                        noFaceFound();
                    else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(RecognizeActivity.this);
                        builder.setTitle(R.string.no_person_group_warning)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setMessage(R.string.no_person_group_tips)
                                .setPositiveButton("Manage", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(RecognizeActivity.this, PersonGroupActivity.class);
                                        startActivity(intent);
                                    }
                                })
                                .setNegativeButton("Skip", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        try {
                                            mInputStream.reset();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                        new doRequest().execute(mInputStream);
                                    }
                                })
                                .setCancelable(true);

                        builder.create().show();
                    }
                }
            }
            else
                noFaceFound();

        }
    }

    private class IdentificationTask extends AsyncTask<UUID, String, IdentifyResult[]> {

        String mPersonGroupId;
        InputStream mInputStream;

        IdentificationTask(String personGroupId, InputStream inputStream){
            mPersonGroupId = personGroupId;
            mInputStream = inputStream;
        }

        protected IdentifyResult[] doInBackground(UUID... params) {
            FaceServiceRestClient faceServiceRestClient = EmotionBattle.getFaceServiceRestClient();
            try {
                publishProgress("Getting person group status...");

                TrainingStatus trainingStatus = faceServiceRestClient.getPersonGroupTrainingStatus(this.mPersonGroupId);
                if (trainingStatus.status != TrainingStatus.Status.Succeeded) {
                    publishProgress("Person group training status is " + trainingStatus.status);
                    return null;
                }

                publishProgress("Identifying...");

                // Start identification.
                return faceServiceRestClient.identity(
                        this.mPersonGroupId,   /* personGroupId */
                        params,                  /* faceIds */
                        1);  /* maxNumOfCandidatesReturned */
            }catch (Exception e){
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute(){
            setUiBeforeBackgroundTask();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            // Show the status of background detection task on screen.a
            setUiDuringBackgroundTask(values[0]);
        }

        @Override
        protected void onPostExecute(IdentifyResult[] results){
            //TODO: Save the name of persons into an array
            int num_of_unknown = 0;

            if (results == null) {
                winner.setText(getString(R.string.faceids_not_in_valid_range));
            } else {
                List<String> names = new ArrayList<>();

                for (IdentifyResult identifyResult : results) {
                    if (identifyResult.candidates.size() > 0) {
                        String personID = identifyResult.candidates.get(0).personId.toString();
                        String personName = StorageHelper.getPersonName(personID, mPersonGroupId, RecognizeActivity.this);
                        names.add(personName);
                    } else
                        num_of_unknown++;
                }

                for (int i = 0; i < names.size(); i++){
                    printMessage(names.get(i));
                    if (i < names.size()-1)
                        printMessage(",");
                    else if (i == names.size()-2 && num_of_unknown == 0)
                        printMessage("and ");

                    printMessage(" ");
                }

                if (num_of_unknown > 0) {
                    if (names.size() > 0)
                        printMessage("and ");
                    printMessage(Integer.toString(num_of_unknown) + " " + getString(R.string.unknown_face) + "(s) ");
                }
                printMessage("inside\n");
            }

            try {
                mInputStream.reset();
            }catch (Exception e){
                e.printStackTrace();
            }
            new doRequest().execute(mInputStream);
        }
    }

    private void setUiBeforeBackgroundTask() {
        progressDialog.show();
    }

    private void setUiDuringBackgroundTask(String progress){
        progressDialog.setMessage(progress);
    }

    private void noFaceFound() {
        if (player2_finish) {
            tryAgainDialog("No face detected :(\nTry again!");
            mButtonSelectImage2.setEnabled(true);
            mButtonSelectImage.setEnabled(false);
            player1_finish = false;
        }
        else{
            tryAgainDialog("No face detected :(\nTry again!");
            mButtonSelectImage.setEnabled(true);
            mButtonSelectImage2.setEnabled(false);
            player2_finish = false;
        }
    }

    private void printMessage(CharSequence charSequence){
        if (player2_finish)
            mTextView2.append(charSequence);
        else
            mTextView.append(charSequence);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognize);

        mButtonSelectImage = (Button) findViewById(R.id.buttonSelectImage);
        mButtonSelectImage2 = (Button) findViewById(R.id.buttonSelectImage2);
        mTextView = (TextView) findViewById(R.id.editTextResult);
        mTextView2 = (TextView) findViewById(R.id.editTextResult2);
        question_text = (TextView) findViewById(R.id.questiontext);
        p1_notification = (TextView) findViewById(R.id.notificationp1);
        p2_notification = (TextView) findViewById(R.id.notificationp2);
        winner = (TextView) findViewById(R.id.winner);

        mButtonSelectImage2.setEnabled(false);
        p1_notification.setText("Player 1 , \nit's your turn !");
        p2_notification.setText("Please wait \nplayer 1...");
        p1_notification.setTextColor(Color.rgb(255, 0, 0));
        p2_notification.setTextColor(Color.rgb(0, 0, 0));
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Loading");

        Resources res = getResources();
        questions = res.getStringArray(R.array.questions);

        Random rand = new Random();
        question_emotion = rand.nextInt(8); //0-7

        question_text.setText(("Q:").concat(questions[question_emotion]));

        mButtonSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clear();
                player1_finish = true;
                startActivityForResult(selectImage(v), REQUEST_SELECT_IMAGE);

            }
        });

        mButtonSelectImage2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player2_finish = true;
                startActivityForResult(selectImage(v), REQUEST_SELECT_IMAGE2);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_recognize, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar wills
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id){
            case R.id.anger:
                question_emotion = 0;
                break;
            case R.id.contempt:
                question_emotion = 1;
                break;
            case R.id.disgust:
                question_emotion = 2;
                break;
            case R.id.fear:
                question_emotion = 3;
                break;
            case R.id.happiness:
                question_emotion = 4;
                break;
            case R.id.neutral:
                question_emotion = 5;
                break;
            case R.id.sadness:
                question_emotion = 6;
                break;
            case R.id.surprise:
                question_emotion = 7;
                break;
            default:
                Random rand = new Random();
                question_emotion = rand.nextInt(8); //0-7
                break;
        }

        question_text.setText(("Q:").concat(questions[question_emotion]));


        return super.onOptionsItemSelected(item);
    }

    // Called when image selection is done.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("RecognizeActivity", "onActivityResult");
        switch (requestCode) {
            case REQUEST_SELECT_IMAGE:
                if (resultCode == RESULT_OK) {
                    // If image is selected successfully, set the image URI and bitmap.
                    Uri mImageUri = data.getData();
                    mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(mImageUri, getContentResolver());

                    if (mBitmap != null) {
                        // Show the image on screen.
                        ImageView imageView = (ImageView) findViewById(R.id.selectedImage);
                        imageView.setImageBitmap(mBitmap);

                        // Add detection log.
                        Log.d("RecognizeActivity", "Image: " + mImageUri + " resized to " + mBitmap.getWidth()
                                + "x" + mBitmap.getHeight());

                        doRecognize();
                    }

                }
                break;

            case REQUEST_SELECT_IMAGE2:
                if (resultCode == RESULT_OK) {
                    // If image is selected successfully, set the image URI and bitmap.
                    Uri mImageUri2 = data.getData();

                    mBitmap2 = ImageHelper.loadSizeLimitedBitmapFromUri(
                            mImageUri2, getContentResolver());

                    if (mBitmap2 != null) {
                        // Show the image on screen.
                        ImageView imageView2 = (ImageView) findViewById(R.id.selectedImage2);
                        imageView2.setImageBitmap(mBitmap2);
                        // Add detection log.
                        Log.d("RecognizeActivity", "Image: " + mImageUri2 + " resized to " + mBitmap2.getWidth()
                                + "x" + mBitmap2.getHeight());

                        doRecognize();
                    }
                }
                break;
            default:
                break;
        }
    }

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        //if Airplane mode has been turned on, netInfo will be null
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public void doRecognize() {
        mButtonSelectImage.setEnabled(false);
        mButtonSelectImage2.setEnabled(false);

        //Image compression
        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if(player2_finish){
            mBitmap2.compress(Bitmap.CompressFormat.JPEG, 100, output);
        }else if (player1_finish){
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        if(isOnline()){
            // Do emotion detection using auto-detected faces.
            /*try {
                new doRequest().execute();
            } catch (Exception e) {
                mTextView.append("Error encountered. Exception is: " + e.toString());
            }*/
            try{
                new DetectionTask(inputStream).execute();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        else{
            AlertDialog.Builder builder = new AlertDialog.Builder(RecognizeActivity.this);
            builder.setTitle(R.string.connection_failed)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.connection_failed_tip)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setCancelable(true);

            builder.create().show();
        }

    }


    // Called when the "Select Image" button is clicked.
    public Intent selectImage(View view) {
        //mTextView.setText("");
        return new Intent(RecognizeActivity.this, com.msp.emotionbattle.helper.SelectImageActivity.class);
    }


    private void tryAgainDialog(CharSequence sequence){
        AlertDialog.Builder builder = new AlertDialog.Builder(RecognizeActivity.this);
        builder.setTitle("Error");
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(sequence);
        builder.setPositiveButton("Choose another", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(RecognizeActivity.this, com.msp.emotionbattle.helper.SelectImageActivity.class);
                if (player2_finish)
                    startActivityForResult(intent, REQUEST_SELECT_IMAGE2);
                else
                    startActivityForResult(intent, REQUEST_SELECT_IMAGE);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private class doRequest extends AsyncTask<InputStream, String, List<RecognizeResult>> {

        @Override
        protected List<RecognizeResult> doInBackground(InputStream... params) {
            //Use face detection
            publishProgress("Analyzing emotions...");

            EmotionServiceRestClient emotionServiceRestClient = EmotionBattle.getEmotionServiceRestClient();
            try {
                return emotionServiceRestClient.recognizeImage(params[0]);
            } catch (Exception e) {
                e.printStackTrace();    // Store error
            }

            return null;
        }

        @Override
        protected void onPreExecute(){
            setUiBeforeBackgroundTask();
        }

        @Override
        protected void onProgressUpdate(String... values){
            // Show the status of background detection task on screen.
            setUiDuringBackgroundTask(values[0]);
        }

        @Override
        protected void onPostExecute(List<RecognizeResult> result) {
            super.onPostExecute(result);
            progressDialog.dismiss();

            // Display based on error existence
            if (result == null){
                noFaceFound();
            }
            else if (result.size() == 0) {
                noFaceFound();

            } else {
                // Covert bitmap to a mutable bitmap by copying it
                Bitmap bitmapCopy;
                if (player2_finish)
                    bitmapCopy = mBitmap2.copy(Bitmap.Config.ARGB_8888, true);
                else
                    bitmapCopy = mBitmap.copy(Bitmap.Config.ARGB_8888, true);

                Canvas faceCanvas = new Canvas(bitmapCopy);

                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5);
                paint.setColor(Color.RED);

                for (RecognizeResult r : result) {
                    if(player2_finish){
                        if(question_emotion == 0)
                            scoreA = r.scores.anger*100;

                        else if(question_emotion == 1)
                            scoreA = r.scores.contempt*100;

                        else if(question_emotion == 2)
                            scoreA = r.scores.disgust*100;

                        else if(question_emotion == 3)
                            scoreA = r.scores.fear*100;

                        else if(question_emotion == 4)
                            scoreA = r.scores.happiness*100;

                        else if(question_emotion == 5)
                            scoreA = r.scores.neutral*100;

                        else if(question_emotion == 6)
                            scoreA = r.scores.sadness*100;

                        else if(question_emotion == 7)
                            scoreA = r.scores.surprise*100;

                        else {
                            mTextView2.append("Your score: error\n");
                            scoreA = 0;
                        }

                        printMessage(String.format("Your score:\n"+"%1$.5f\n", scoreA));
                        p1_notification.setText("Player 1 , \nit's your turn !");
                        p2_notification.setText("Please wait \nplayer 1...");
                        p1_notification.setTextColor(Color.rgb(255, 0, 0));
                        p2_notification.setTextColor(Color.rgb(0, 0, 0));
                        mButtonSelectImage.setEnabled(true);
                        mButtonSelectImage2.setEnabled(false);
                    }
                    else{
                        if(question_emotion == 0)
                            scoreB = r.scores.anger*100;

                        else if(question_emotion == 1)
                            scoreB = r.scores.contempt*100;

                        else if(question_emotion == 2)
                            scoreB = r.scores.disgust*100;

                        else if(question_emotion == 3)
                            scoreB = r.scores.fear*100;

                        else if(question_emotion == 4)
                            scoreB = r.scores.happiness*100;

                        else if(question_emotion == 5)
                            scoreB = r.scores.neutral*100;

                        else if(question_emotion == 6)
                            scoreB = r.scores.sadness*100;

                        else if(question_emotion == 7)
                            scoreB = r.scores.surprise*100;

                        else {
                            mTextView.append("Your score: error\n");
                            scoreB = 0;
                        }

                        printMessage(String.format("Your score:\n"+"%1$.5f\n", scoreB));
                        p1_notification.setText("Please wait \nplayer 2...");
                        p2_notification.setText("Player 2 , \nit's your turn !");
                        p2_notification.setTextColor(Color.rgb(255, 0, 0));
                        p1_notification.setTextColor(Color.rgb(0, 0, 0));
                        mButtonSelectImage.setEnabled(false);
                        mButtonSelectImage2.setEnabled(true);
                    }

                    if (r.equals(result.get(result.size()-1)))
                        paint.setColor(Color.GREEN);
                    
                    faceCanvas.drawRect(r.faceRectangle.left, r.faceRectangle.top, r.faceRectangle.left + r.faceRectangle.width, r.faceRectangle.top + r.faceRectangle.height, paint);
                    Log.d("FaceRect",String.format("%d %d %d %d", r.faceRectangle.left, r.faceRectangle.top, r.faceRectangle.width, r.faceRectangle.height));

                }

                if(player2_finish){
                    ImageView imageView = (ImageView) findViewById(R.id.selectedImage2);
                    imageView.setImageBitmap(bitmapCopy);
                }else {
                    ImageView imageView = (ImageView) findViewById(R.id.selectedImage);
                    imageView.setImageBitmap(bitmapCopy);
                }
            }

            if (scoreA != 0 && scoreB != 0){
                player1_finish = player2_finish = false;
                if (scoreA < scoreB)
                    winner.setText(R.string.p1_win);
                else if (scoreA > scoreB)
                    winner.setText(R.string.p2_win);
                else
                    winner.setText(R.string.tie);
            }

        }
    }

    private void clear(){
        mTextView.setText("");
        mTextView2.setText("");
        ImageView imageView = (ImageView) findViewById(R.id.selectedImage2);
        imageView.setImageDrawable(null);
    }
}
