package com.example.android;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.kyanogen.signatureview.SignatureView;
import com.samsung.android.sdk.penremote.AirMotionEvent;
import com.samsung.android.sdk.penremote.ButtonEvent;
import com.samsung.android.sdk.penremote.SpenEvent;
import com.samsung.android.sdk.penremote.SpenEventListener;
import com.samsung.android.sdk.penremote.SpenRemote;
import com.samsung.android.sdk.penremote.SpenUnit;
import com.samsung.android.sdk.penremote.SpenUnitManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import yuku.ambilwarna.AmbilWarnaDialog;

public class MainActivity extends AppCompatActivity {
    // defining default pen colour
    int defaultColor;
    // defining views
    SignatureView signatureView;
    ImageButton imgEraser, imgColor, imgSave;
    SeekBar seekBar;
    private TextView txtPenSize;

    private TextView mButtonState;

    private SpenRemote mSpenRemote;
    private SpenUnitManager mSpenUnitManager;

    // creating path to save the drawings
    private static String fileName;
    File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/myPaintings");

    private ImageView image1;

    private ImageView mDrawView;
    private Bitmap bitmap;
    private Canvas canvas;
    private Paint paint = new Paint();
    private boolean penDown = false;

    private int N = 5, n = 0;
    private float aVXPrev = 0, aVYPrev = 0, r = 50, smaX = 0, smaY = 0, emaXPrev = 0, emaYPrev = 0, emaX = 0, emaY = 0;
    private float[][] mAArr = {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}};
    private long tInit = 0, tPrev = 0, start, end;

    private boolean motionOn = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialising all the views
//        signatureView = findViewById(R.id.signature_view);
        seekBar = findViewById(R.id.penSize);
        txtPenSize = findViewById((R.id.txtPenSize));
        imgColor = findViewById(R.id.btnColor);
        imgEraser = findViewById(R.id.btnEraser);
        imgSave = findViewById(R.id.btnConn);

        mButtonState = findViewById(R.id.button_state);

        image1 = (ImageView) findViewById(R.id.image1);

        mDrawView = findViewById(R.id.image);

//        image1.setOnTouchListener(onTouchListener());

        mSpenRemote = SpenRemote.getInstance();
        mSpenRemote.setConnectionStateChangeListener(new SpenRemote.ConnectionStateChangeListener() {
            @Override
            public void onChange(int i) {
                Toast.makeText(MainActivity.this, "Connection State = " + i, Toast.LENGTH_SHORT).show();
            }
        });

        askPermission();

        // creating a string of date and time for unique filename
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String date = format.format(new Date());
        fileName = path + "/" + date + ".png";

        // if path does not exist, then create one
        if (!path.exists()) {
            path.mkdirs();
        }

        defaultColor = ContextCompat.getColor(MainActivity.this, R.color.black);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                // this runs when the seekBar value is changed
                txtPenSize.setText(i + "dp");
                r = i;
                seekBar.setMin(50);
                seekBar.setMax(100);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        imgColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openColorPicker();
            }
        });

        imgEraser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bitmap = Bitmap.createBitmap(mDrawView.getWidth(),
                        mDrawView.getHeight(),
                        Bitmap.Config.ARGB_8888);
                mDrawView.setImageBitmap(bitmap);
                bitmap = null;
            }
        });

        imgSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // check if the canvas is empty
//                if (!signatureView.isBitmapEmpty()){
//                    try {
//                        saveImage();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                        Toast.makeText(MainActivity.this,"Couldn't save", Toast.LENGTH_SHORT).show();
//                    }
//                }
                if (motionOn){
                    SpenUnit airMotionUnit = mSpenUnitManager.getUnit(SpenUnit.TYPE_AIR_MOTION);
                    mSpenUnitManager.unregisterSpenEventListener(airMotionUnit);
                    Log.d(TAG, "End".concat(String.valueOf(image1.getX())));
                    Log.d(TAG, String.valueOf(image1.getY()));
                } else {
                    SpenUnit airMotionUnit = mSpenUnitManager.getUnit(SpenUnit.TYPE_AIR_MOTION);
                    mSpenUnitManager.registerSpenEventListener(mAirMotionEventListener, airMotionUnit);
                    Log.d(TAG, "Start".concat(String.valueOf(image1.getX())));
                    Log.d(TAG, String.valueOf(image1.getY()));
                }
                image1.setX(mDrawView.getWidth()/2 - image1.getWidth()/2);
                image1.setY(mDrawView.getHeight()/2 - image1.getHeight()/2);
                motionOn = !motionOn;
            }
        });

        connectToSpenRemote();

    }

    // function to open color picker
    private void openColorPicker() {
        AmbilWarnaDialog ambilWarnaDialog = new AmbilWarnaDialog(this, defaultColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {

            }

            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                defaultColor = color;
//                signatureView.setPenColor(color);
            }
        });
        ambilWarnaDialog.show();
    }

    // ask for reading and writing permission from the device
    private void askPermission() {
        Dexter.withContext(this)
                .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                        Toast.makeText(MainActivity.this, "Granted!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest(); // keep asking for permission until user agrees to give permission
                    }
                }).check();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mSpenRemote.isConnected()) {
            disconnectSpenRemote();
        }
    }

    private void connectToSpenRemote() {
        if (mSpenRemote.isConnected()) {
            Log.d(TAG, "Already Connected!");
            Toast.makeText(this, "Already Connected.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "connectToSpenRemote");

        mSpenRemote.setConnectionStateChangeListener(new SpenRemote.ConnectionStateChangeListener() {
            @Override
            public void onChange(int state) {
                if (state == SpenRemote.State.DISCONNECTED
                        || state == SpenRemote.State.DISCONNECTED_BY_UNKNOWN_REASON) {
                    Toast.makeText(MainActivity.this, "Disconnected : " + state, Toast.LENGTH_SHORT).show();
                }
            }
        });

        mSpenRemote.connect(this, mConnectionResultCallback);

    }

    private void disconnectSpenRemote() {
        if (mSpenRemote != null) {
            mSpenRemote.disconnect(this);
        }
    }

    private SpenRemote.ConnectionResultCallback mConnectionResultCallback = new SpenRemote.ConnectionResultCallback() {
        @Override
        public void onSuccess(SpenUnitManager spenUnitManager) {
            Log.d(TAG, "onConnected");
            Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
            mSpenUnitManager = spenUnitManager;

            SpenUnit buttonUnit = mSpenUnitManager.getUnit(SpenUnit.TYPE_BUTTON);
            mSpenUnitManager.registerSpenEventListener(mButtonEventListener, buttonUnit);
        }

        @Override
        public void onFailure(int i) {
            Log.d(TAG, "onFailure");
            Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
        }
    };

    private SpenEventListener mButtonEventListener = new SpenEventListener() {
        @Override
        public void onEvent(SpenEvent event) {
            ButtonEvent button = new ButtonEvent(event);

            if (button.getAction() == ButtonEvent.ACTION_DOWN) {
                mButtonState.setText("Pressed");

                if (!mSpenRemote.isConnected()) {
                    Log.e(TAG, "not connected!");
                    return;
                }

//                SpenUnit airMotionUnit = mSpenUnitManager.getUnit(SpenUnit.TYPE_AIR_MOTION);
//                mSpenUnitManager.registerSpenEventListener(mAirMotionEventListener, airMotionUnit);


            } else if (button.getAction() == ButtonEvent.ACTION_UP) {
                mButtonState.setText("Released");
                penDown = !(penDown);
//                SpenUnit airMotionUnit = mSpenUnitManager.getUnit(SpenUnit.TYPE_AIR_MOTION);
//                mSpenUnitManager.registerSpenEventListener(mAirMotionEventListener, airMotionUnit);
            }
        }
    };

    private SpenEventListener mAirMotionEventListener = new SpenEventListener() {
        @Override
        public void onEvent(SpenEvent event) {
            if (tInit == 0){
                tInit = event.getTimeStamp();
            }
            AirMotionEvent airMotion = new AirMotionEvent(event);
            float aVX = airMotion.getDeltaX();
            float aVY = -airMotion.getDeltaY();
            long t = airMotion.getTimeStamp() - tInit;
            start = System.currentTimeMillis();
            Log.d(TAG, "x".concat(String.valueOf(aVX)));
//            Log.d(TAG, "y".concat(String.valueOf(aVY)));
//            Log.d(TAG, String.valueOf(t));
            calculate(aVX, aVY, t); // EMA
//            calculate2(aVX, aVY, t); // IAV
//            calculate3(aVX, aVY, t); // debugging purposes
        }
    };

    private void calculate3(float aVX, float aVY, long t) {
        float tChange = t - tPrev;
        txtPenSize.setText(String.valueOf(tChange));
        tPrev = t;
// to find the size of the board
//            int[] location = new int[2];
//            signatureView.getLocationOnScreen(location);
//            int x = location[0];
//            int y = location[1];
//            txtPenSize.setText("" + x + ", " + y); // 0, 165
//        Rect rectf = new Rect();
//        image1.getLocalVisibleRect(rectf);
//        int x = rectf.width();
//        int y = rectf.height();
//        txtPenSize.setText("" + x + ", " + y); // 2560, 1198
    }

    // function for IAV
    private void calculate2(float aVX, float aVY, long t) {
        float tChange = t - tPrev;

        float xFrom = image1.getX();
        float a = (aVX-aVXPrev)/tChange;
        float xTo = xFrom + r * (a*tChange*tChange/2 + aVXPrev*tChange);

        float yFrom = image1.getY();
        a = (aVY-aVYPrev)/tChange;
        float yTo = yFrom + r * (a*tChange*tChange/2 + aVYPrev*tChange);


//        txtPenSize.setText("" + xFrom + ", " + yFrom);
        if (xTo >= 0 &&
                xTo < 2560 &&
                yTo > 0 &&
                yTo < 1198
        ) {
//                image1.animate().translationXBy(aVX).translationYBy(aVY).setDuration(1);
            image1.setX(xTo);
            image1.setY(yTo);
            if (penDown){
                draw(xFrom, yFrom, xTo, yTo);
            }
            // for response time analysis
//            end = System.currentTimeMillis();
//            Log.d(TAG, "t".concat(String.valueOf(t)));
//            Log.d(TAG, String.valueOf(end-start));
        }
        else {
            aVX = 0;
            aVY = 0;
            if (xTo < 0) xTo = 0;
            else xTo = 2559;
            if (yTo < 0) yTo = 0;
            else yTo = 1197;
        }
        txtPenSize.setText(String.valueOf(xTo-xFrom));
//        Log.d(TAG, "".concat(String.valueOf(xTo-xFrom)));
        aVXPrev = aVX;
        aVYPrev = aVY;
        tPrev = t;
    }

    // function for EMA
    private void calculate(float aVX, float aVY, long t) {
        if (n < N){
            n ++;
            smaX += aVX;
            smaY += aVY;
            if (n == N) {
                smaX /= N;
                smaY /= N;
                emaXPrev = smaX;
                emaYPrev = smaY;
            }
            return;
        }

        float tChange = t - tPrev;
        float k = 2 / ((float)N+1);

        float xFrom = image1.getX();
        float yFrom = image1.getY();

        emaX = aVX * k + emaXPrev * (1-k);
        emaY = aVY * k + emaYPrev * (1-k);

        float xTo = xFrom + r * emaX * tChange;
        float yTo = yFrom + r * emaY * tChange;

//        Log.d(TAG, String.valueOf(k));

        txtPenSize.setText("" + xFrom + ", " + yFrom);
        if (xTo >= 0 &&
                xTo < 2560 &&
                yTo > 0 &&
                yTo < 1198
        ) {
            image1.setX(xTo);
            image1.setY(yTo);
            if (penDown){
                draw(xFrom, yFrom, xTo, yTo);
            }
            // for response time analysis
//            end = System.currentTimeMillis();
//            Log.d(TAG, "t".concat(String.valueOf(t)));
//            Log.d(TAG, String.valueOf(end-start));

        }
        else {
            emaX = 0;
            emaY = 0;
            if (xTo < 0) xTo = 0;
            else xTo = 2559;
            if (yTo < 0) yTo = 0;
            else yTo = 1197;
        }

        emaXPrev = emaX;
        emaYPrev = emaY;
        tPrev = t;
    }

    private void draw(float xFrom, float yFrom, float xTo, float yTo){
        // creating an empty bitmap for drawing
        if (bitmap == null){
            bitmap = Bitmap.createBitmap(mDrawView.getWidth(),
                    mDrawView.getHeight(),
                    Bitmap.Config.ARGB_8888);

            canvas = new Canvas(bitmap);

            paint.setColor(Color.RED);
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(8);
        }

        canvas.drawLine(xFrom, yFrom, xTo, yTo, paint);

        mDrawView.setImageBitmap(bitmap);
    }
}