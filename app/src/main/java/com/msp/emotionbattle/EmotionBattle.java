package com.msp.emotionbattle;

import android.app.Application;

import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;

/**
 * Created by tomwang on 2017/2/28.
 */

public class EmotionBattle extends Application {

    private static FaceServiceRestClient faceServiceRestClient;
    private static EmotionServiceRestClient emotionServiceRestClient;
    @Override
    public void onCreate(){
        super.onCreate();
        faceServiceRestClient = new FaceServiceRestClient(getString(R.string.faceSubscription_key));
        emotionServiceRestClient = new EmotionServiceRestClient(getString(R.string.emotionSubscription_key));
    }

    public static FaceServiceRestClient getFaceServiceRestClient(){
        return faceServiceRestClient;
    }

    public static EmotionServiceRestClient getEmotionServiceRestClient(){
        return emotionServiceRestClient;
    }
}
