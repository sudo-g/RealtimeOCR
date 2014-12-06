package com.tronacademy.realtimeocr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Mat;
import org.opencv.android.Utils;

import com.googlecode.tesseract.android.TessBaseAPI;

import android.support.v7.app.ActionBarActivity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;


public class MainActivity extends ActionBarActivity implements CvCameraViewListener2 {
	
	private static final String TAG = "RealTimeOCR::MainActivity";
	
	// Fields of string paths for use with Tesseract OCR engine
	public static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/RealTimeOCR/";
	public static final String lang = "eng";
	
	private JavaCameraView mOpenCvCameraView;
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    private Mat work;		// For use with storing camera input frames

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // create directory to store trained data
        String[] paths = new String [] {DATA_PATH, DATA_PATH + "tessdata/"};
        for (String path : paths) {
        	File dir = new File(path);
        	if (!dir.exists()) {
        		if (!dir.mkdirs()) {
        			Log.v(TAG, "ERROR: Creation of directory " + path + " on sdcard failed");
        		} else {
        			Log.v(TAG, "Created directory " + path + "on sdcard");
        		}
        		
        	}
        }
        
        // move trained data into this directory
        if (!(new File(DATA_PATH + "tessdata/"+ lang + "traineddata")).exists()) {
        	try {
        		AssetManager assetManager = getAssets();
        		InputStream in = assetManager.open("eng.traineddata");
        		OutputStream out = new FileOutputStream(DATA_PATH + "tessdata/eng.traineddata");
        		
        		// Transfer bytes from in to out
        		byte[] buffer = new byte[1024];
        		int len;
        		while ((len = in.read(buffer)) > 0) {
        			out.write(buffer, 0, len);
        		}
        		in.close();
        		out.close();
        		
        		Log.v(TAG, "Copied " + lang + " traineddata");
        	} catch (IOException e) {
        		Log.e(TAG, "Was unable to copy " + lang + " traineddata " + e.toString());
        	}
        }
        
        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.java_camera_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);        
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    public void onResume() {
    	super.onResume();
    	OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
    }
    
    public void onDestrory() {
    	super.onDestroy();
    	if (mOpenCvCameraView != null) {
    		mOpenCvCameraView.disableView();
    	}
    }


	@Override
	public void onCameraViewStarted(int width, int height) {
		work = new Mat();		// instantiate Mat here because it needs to be freed explicitly
	}


	@Override
	public void onCameraViewStopped() {
		work.release();			// explicitly free input frame Mat
	}


	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		// instantiate interface to OCR engine
		TessBaseAPI baseApi = new TessBaseAPI();
		baseApi.setDebug(true);
		baseApi.init(DATA_PATH, lang);
		
		// Grab frame capture and convert to form usable by OCR engine
		work = inputFrame.rgba();
		Bitmap bmpImg = null;
		Utils.matToBitmap(work, bmpImg);
		
		baseApi.setImage(bmpImg);
		String recognizedText = baseApi.getUTF8Text();
		baseApi.end();				// frees OCR engine instance
		
		
		return inputFrame.rgba();
	}
}
