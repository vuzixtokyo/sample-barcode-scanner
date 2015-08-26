package com.vuzix.qramazon;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.IOException;

public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private static final String TAG = MainActivity.class.getSimpleName();

    private String mAmazonUrl;

    private Camera mCamera;

    private SurfaceView mSurfaceView;

    private final MultiFormatReader mMultiFormatReader = new MultiFormatReader();

    private final Camera.AutoFocusCallback mAutoFocusCallback = new Camera.AutoFocusCallback() {

        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (success) {
                camera.setOneShotPreviewCallback(mPreviewCallback);
            } else {
                Toast.makeText(getApplicationContext(), "focus failed.", Toast.LENGTH_SHORT).show();
            }

            if (mCamera != null) {
                mCamera.autoFocus(this);
            }
        }
    };

    private final Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Size size = camera.getParameters().getPreviewSize();
            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data, size.width,
                    size.height, 0, 0, size.width, size.height, false);

            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            try {
                Result rawResult = mMultiFormatReader.decode(bitmap);

                if (rawResult.getBarcodeFormat() == BarcodeFormat.EAN_13) {
                    search(rawResult.getText());
                }
            } catch (NotFoundException e) {
                // do nothing
            }
        }
    };

    private void search(String ean13) {
        String url = mAmazonUrl + ean13;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mAmazonUrl = getString(R.string.amazon_url);

        mSurfaceView = new SurfaceView(this);
        mSurfaceView.getHolder().addCallback(this);
        setContentView(mSurfaceView);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // http://stackoverflow.com/questions/11495842/how-surfaceholder-callbacks-are-related-to-activity-lifecycle
        mSurfaceView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mSurfaceView.setVisibility(View.INVISIBLE);

        if (mCamera != null) {
            mCamera.cancelAutoFocus();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mCamera = Camera.open();

        try {
            mCamera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        mCamera.startPreview();
        mCamera.autoFocus(mAutoFocusCallback);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }
}
