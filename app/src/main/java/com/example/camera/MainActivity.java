package com.example.camera;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.hardware.Camera.Size;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener2, OnTouchListener {
    private static final String TAG = "OCVSample::Activity";
    private static final int SHOW_ORIGINAL = 1;
    private static final int SHOW_GREEN = 2;
    private static final int SHOW_YELLOW = 3;

    private CameraView mOpenCvCameraView;
    private List<Size> mResolutionList;
    private MenuItem[] mEffectMenuItems;
    private SubMenu mColorEffectsMenu;
    private MenuItem[] mResolutionMenuItems;
    private SubMenu mResolutionMenu;

    private TextView mTextView;
    private Button mButtonOriginal;
    private Button mButtonGreen;
    private Button mButtonYellow;

    private int show = SHOW_ORIGINAL;
    private Mat mMatRGB;
    private Mat mMatGreen;
    private Mat mMatYellow;
    private Scalar mScalarGreenRGB;
    private Scalar mScalarGreenHSV;
    private Scalar mScalarYellowRGB;
    private Scalar mScalarYellowHSV;
    private ColorBlobDetector mDetectorGreen;
    private ColorBlobDetector mDetectorYellow;
    private Point mPointGreen;
    private Point mPointYellow;
    private double xGreen = 0.0;
    private double yGreen = 0.0;
    private double xYellow = 0.0;
    private double yYellow = 0.0;
    private double mWidth = 0.0;
    private double mHeight = 0.0;

    private Handler mHandler;

    public static String format4(double d) {
        return String.format("%4.2f", d);
    }

    public static String format3(double d) {
        return String.format("%3.2f", d);
    }

    Runnable runnableUi = new Runnable() {
        @Override
        public void run() {
            switch (show) {
                case SHOW_GREEN:
                    mTextView.setText(String.valueOf("x : \t" + MainActivity.format4(xGreen) + "/" + mWidth
                            + " y : \t" + MainActivity.format3(yGreen) + "/" + mHeight));
                    break;
                case SHOW_YELLOW:
                    mTextView.setText(String.valueOf("x : \t" + MainActivity.format4(xYellow) + "/" + mWidth
                            + " y : \t" + MainActivity.format3(yYellow) + "/" + mHeight));
                    break;
                default:
                    break;
            }
        }
    };

    class MyClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_original:
                    show = SHOW_ORIGINAL;
                    break;
                case R.id.button_green:
                    show = SHOW_GREEN;
                    break;
                case R.id.button_yellow:
                    show = SHOW_YELLOW;
                    break;
                default:
                    show = SHOW_ORIGINAL;
                    break;
            }
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CameraView) findViewById(R.id.tutorial3_activity_java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);

        mTextView = (TextView)findViewById(R.id.text);
        mButtonOriginal = (Button)findViewById(R.id.button_original);
        mButtonGreen = (Button)findViewById(R.id.button_green);
        mButtonYellow = (Button)findViewById(R.id.button_yellow);

        mButtonOriginal.setOnClickListener(new MyClickListener());
        mButtonGreen.setOnClickListener(new MyClickListener());
        mButtonYellow.setOnClickListener(new MyClickListener());

        mHandler = new Handler();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mHeight = height;
        mWidth = width;

        mMatRGB = new Mat(height, width, CvType.CV_8UC4);
        mMatGreen = new Mat(height, width, CvType.CV_8UC4);
        mMatYellow = new Mat(height, width, CvType.CV_8UC4);

        mScalarGreenRGB = new Scalar(0.0, 255.0, 0.0, 0);
        mScalarGreenHSV = new Scalar(60.0, 205.0, 205.0);
        mScalarYellowRGB = new Scalar(255.0, 255.0, 0.0, 0);
        mScalarYellowHSV = new Scalar(30.0, 205.0, 205.0);

        mDetectorGreen = new ColorBlobDetector();
        mDetectorGreen.setHsvColor(mScalarGreenHSV);
        mDetectorYellow = new ColorBlobDetector();
        mDetectorYellow.setHsvColor(mScalarYellowHSV);

        mPointGreen = new Point();
        mPointYellow = new Point();
    }

    public void onCameraViewStopped() {
        mMatRGB.release();
        mMatGreen.release();
        mMatYellow.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        mMatRGB = inputFrame.rgba();
        double[] data = {255.0, 0.0, 0.0, 255.0};
        switch (show) {
            case SHOW_ORIGINAL:
                return mMatRGB;
            case SHOW_GREEN:
                mMatGreen = inputFrame.rgba();
                mDetectorGreen.process(mMatGreen);
                List<MatOfPoint> greenContour = mDetectorGreen.getContours();
                mPointGreen = mDetectorGreen.getCenter();
                Imgproc.drawContours(mMatGreen, greenContour, -1, mScalarGreenRGB);
                xGreen = mPointGreen.x;
                yGreen = mPointGreen.y;
                for (int i = (int)xGreen; i < (int)xGreen + 20; i++)
                    for (int j = (int)yGreen; j < (int)yGreen + 20; j++)
                        mMatGreen.put(j, i, data);
                mHandler.post(runnableUi);
                return mMatGreen;
            case SHOW_YELLOW:
                mMatYellow = inputFrame.rgba();
                mDetectorYellow.process(mMatYellow);
                List<MatOfPoint> yellowContour = mDetectorYellow.getContours();
                mPointYellow = mDetectorYellow.getCenter();
                Imgproc.drawContours(mMatYellow, yellowContour, -1, mScalarYellowRGB);
                xYellow = mPointYellow.x;
                yYellow = mPointYellow.y;
                for (int i = (int)xYellow; i < (int)xYellow + 20; i++)
                    for (int j = (int)yYellow; j < (int)yYellow + 20; j++)
                        mMatYellow.put(j, i, data);
                mHandler.post(runnableUi);
                return mMatYellow;
            default:
                return mMatRGB;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        List<String> effects = mOpenCvCameraView.getEffectList();

        if (effects == null) {
            Log.e(TAG, "Color effects are not supported by device!");
            return true;
        }

        mColorEffectsMenu = menu.addSubMenu("Color Effect");
        mEffectMenuItems = new MenuItem[effects.size()];

        int idx = 0;
        ListIterator<String> effectItr = effects.listIterator();
        while(effectItr.hasNext()) {
            String element = effectItr.next();
            mEffectMenuItems[idx] = mColorEffectsMenu.add(1, idx, Menu.NONE, element);
            idx++;
        }

        mResolutionMenu = menu.addSubMenu("Resolution");
        mResolutionList = mOpenCvCameraView.getResolutionList();
        mResolutionMenuItems = new MenuItem[mResolutionList.size()];

        ListIterator<Size> resolutionItr = mResolutionList.listIterator();
        idx = 0;
        while(resolutionItr.hasNext()) {
            Size element = resolutionItr.next();
            mResolutionMenuItems[idx] = mResolutionMenu.add(2, idx, Menu.NONE,
                    Integer.valueOf(element.width).toString() + "x" + Integer.valueOf(element.height).toString());
            idx++;
        }

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item.getGroupId() == 1)
        {
            mOpenCvCameraView.setEffect((String) item.getTitle());
            Toast.makeText(this, mOpenCvCameraView.getEffect(), Toast.LENGTH_SHORT).show();
        }
        else if (item.getGroupId() == 2)
        {
            int id = item.getItemId();
            Size resolution = mResolutionList.get(id);
            mOpenCvCameraView.setResolution(resolution);
            resolution = mOpenCvCameraView.getResolution();
            String caption = Integer.valueOf(resolution.width).toString() + "x" + Integer.valueOf(resolution.height).toString();
            Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
        }

        return true;
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.i(TAG,"onTouch event");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String currentDateandTime = sdf.format(new Date());
        String fileName = Environment.getExternalStorageDirectory().getPath() +
                "/sample_picture_" + currentDateandTime + ".jpg";
        mOpenCvCameraView.takePicture(fileName);
        Toast.makeText(this, fileName + " saved", Toast.LENGTH_SHORT).show();
        return false;
    }
}
