package com.example.asif.facedetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.*;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.imgproc.Imgproc;

import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import android.media.MediaPlayer;


import android.os.AsyncTask;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "MainActivity";
    private Mat mRgba;
    private Mat mGrey;

    private File myFaceCascadeFile;
    private File myEyesCascadeFile;

    private float myRelativeFaceSize = 0.2f;
    private int myAbsoluteFaceSize = 0;

    private CascadeClassifier myFaceDetector;
    private CascadeClassifier myEyeDetector;

    private Bitmap myBitmap;

    public MediaPlayer mediaPlayer;


    private CameraBridgeViewBase myOpenCVCameraView;

    private int mPreviousEyesState = -1;

    private Button btn_start;




    private BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV Loaded Successfully");

                    try {
                        //To Load Face Cascade File
                        InputStream ISFace = getResources().openRawResource(R.raw.haarcascade_frontalface_default);
                        File cascadeDirectory = getDir("cascade", Context.MODE_PRIVATE);
                        myFaceCascadeFile = new File(cascadeDirectory, "haarcascade_frontalface_default.xml");
                        FileOutputStream OSFace = new FileOutputStream(myFaceCascadeFile);

                        //Storing Data to External Memory(SD)
                        byte[] buffer = new byte[4096];
                        int bytesReadFace;
                        while ((bytesReadFace = ISFace.read(buffer)) != -1) {
                            OSFace.write(buffer, 0, bytesReadFace);

                        }
                        ISFace.close();
                        OSFace.close();

                        InputStream ISEyes = getResources().openRawResource((R.raw.haarcascade_eye_tree_eyeglasses));
                        File cascadeDirectoryEyes = getDir("cascadeeyes", Context.MODE_PRIVATE);
                        myEyesCascadeFile = new File(cascadeDirectoryEyes, "haarcascade_eye_tree_eyeglasses.xml");
                        FileOutputStream OSEyes = new FileOutputStream(myEyesCascadeFile);

                        byte[] buffer2 = new byte[4096];
                        int bytesReadEyes;
                        while ((bytesReadEyes = ISEyes.read(buffer2)) != -1) {
                            OSEyes.write(buffer2, 0, bytesReadEyes);
                        }
                        ISEyes.close();
                        OSEyes.close();


                        myFaceDetector = new CascadeClassifier(myFaceCascadeFile.getAbsolutePath());
                        myFaceDetector.load(myFaceCascadeFile.getAbsolutePath());
                        if (myFaceDetector.empty()) {
                            Log.e(TAG, "Failed to load Facecascade");
                        } else {
                            Log.i(TAG, "Loaded Face Cascade Successfully");
                        }
                        cascadeDirectory.delete();

                        myEyeDetector = new CascadeClassifier(myEyesCascadeFile.getAbsolutePath());
                        myEyeDetector.load(myEyesCascadeFile.getAbsolutePath());
                        if (myEyeDetector.empty()) {
                            Log.e(TAG, "Failed to load Eye Cascade");
                        } else {
                            Log.i(TAG, "Loaded Eye Cascade Successfully");
                        }
                        cascadeDirectoryEyes.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascades. Exception Thrown");
                    }

                    myOpenCVCameraView.setCameraIndex(1);
                    myOpenCVCameraView.enableFpsMeter();
                    myOpenCVCameraView.enableView();

                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "Called on Create");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        //To  Initiate Camera
        myOpenCVCameraView = (CameraBridgeViewBase) findViewById(R.id.java_camera_view);
        myOpenCVCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        myOpenCVCameraView.setCvCameraViewListener(this);

        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.sample);


    }


    @Override
    protected void onPause() {
        super.onPause();
        if (myOpenCVCameraView != null) {
            myOpenCVCameraView.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (myOpenCVCameraView != null) {
            myOpenCVCameraView.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "Successfully Loaded");
            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        } else {
            Log.i(TAG, "Unsuccessful");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, this, mLoaderCallBack);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

        mGrey = new Mat();
        mRgba = new Mat();


    }


    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mGrey.release();
    }


    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGrey = inputFrame.gray();

        if (myAbsoluteFaceSize == 0) {
            int height = mGrey.rows();
            if (Math.round(height * myRelativeFaceSize) > 0) {
                myAbsoluteFaceSize = Math.round(height * myRelativeFaceSize);
            }

        }
         /*
        MatOfRect Face = new MatOfRect();
        if ((myFaceDetector) != null) {
            //To Detect Faces
            myFaceDetector.detectMultiScale(mGrey, Face, 1.05, 2, 2, new Size(myAbsoluteFaceSize, myAbsoluteFaceSize), new Size());
        }
        Rect[] FaceArray = Face.toArray();
               //Draw Rect around face
                Imgproc.rectangle(mRgba, FaceArray[0].tl(), FaceArray[0].br(), new Scalar(0, 255, 0, 255), 3);
                */




            MatOfRect Eyes = new MatOfRect();
            if (myEyeDetector != null) {
                //To Detect Eyes
                myEyeDetector.detectMultiScale(mRgba, Eyes, 1.05, 2, 2, new Size(myAbsoluteFaceSize, myAbsoluteFaceSize), new Size());
            }

            //Stores Detected Eyes in an Array
            Rect[] EyesArray = Eyes.toArray();
            //To Map a Rectangle Around Eyes
            for (int j = 0; j < EyesArray.length; j++) {
                Imgproc.rectangle(mRgba, EyesArray[j].tl(), EyesArray[j].br(), new Scalar(0, 255, 0, 255), 3);
            }


            //To Sound Alarm
            try {
                if (EyesArray.length<2) { //sometimes it picks up a 3rd eye lol
                    mediaPlayer.start();
                } else {
                    mediaPlayer.stop();
                    mediaPlayer.prepare();
                }


            } catch (Exception e) {
                Log.e(TAG, "Failed to Configure Alarm");
            }




            //To Detect Amount of Redness in the Eyes
            try {
                int R = 0;
                Utils.matToBitmap(Eyes, myBitmap);

                for (int i = 0; i < EyesArray[1].width; i++) {
                    for (int j = 0; j < EyesArray[1].height; j++) {
                        int pixelclr = myBitmap.getPixel(i, j);
                        R += Color.red(pixelclr);

                    }
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }


            return mRgba;
        }
    }










