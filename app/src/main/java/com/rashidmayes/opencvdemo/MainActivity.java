package com.rashidmayes.opencvdemo;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, TextView.OnEditorActionListener, TextWatcher {

    private WebView mWebView;
    private Button mProcessButton;
    private EditText mURLText;
    private ImageView mPreview;
    private View mRoot;
    private InputMethodManager mInputMethodManager;

    static{ System.loadLibrary("opencv_java3"); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mWebView = (WebView)findViewById(R.id.webview);
        mProcessButton = (Button)findViewById(R.id.process);
        mURLText = (EditText)findViewById(R.id.urltext);
        mPreview = (ImageView)findViewById(R.id.preview);
        mRoot = findViewById(R.id.root);


        mWebView.setDrawingCacheEnabled(true);
        mPreview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.setVisibility(View.GONE);
                return true;
            }
        });


        mURLText.addTextChangedListener(this);
        mURLText.setOnEditorActionListener(this);

        mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        mProcessButton.setOnClickListener(this);

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        mWebView.setWebViewClient(new MyWebViewClient());

        mURLText.setText("http://www.accenture.com");
        mWebView.loadUrl(mURLText.getText().toString());
    }


    @Override
    public void onBackPressed() {
        if ( mWebView.canGoBack() ) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View v) {
        if ( v.getId() == R.id.process) {

            AsyncTask asyncTask = new AsyncTask() {

                Bitmap bitmap;
                Canvas canvas;
                ProgressDialog progressDialog;

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();

                    progressDialog = ProgressDialog.show(MainActivity.this,"","Processing");

                    bitmap = Bitmap.createBitmap(mWebView.getWidth(), mWebView.getHeight(), Bitmap.Config.ARGB_8888);
                    canvas = new Canvas(bitmap);
                    Paint paint = new Paint();
                    canvas.drawBitmap(bitmap, 0, 0, paint);
                    mWebView.draw(canvas);
                }

                @Override
                protected void onPostExecute(Object o) {
                    if ( bitmap != null ) {
                        mPreview.setImageBitmap(bitmap);
                        mPreview.setVisibility(View.VISIBLE);
                    }

                    if ( progressDialog != null ) {
                        progressDialog.dismiss();
                    }
                }

                @Override
                protected Object doInBackground(Object[] params) {

                    Mat img = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC4);
                    Utils.bitmapToMat(bitmap, img);


                    Imgproc.cvtColor(img, img, Imgproc.COLOR_RGBA2BGR, 3);
                    Imgproc.cvtColor(img, img, Imgproc.COLOR_RGBA2GRAY);

                    Size size = new Size(5,5);
                    Imgproc.GaussianBlur(img, img, size, 0);
                    Core.bitwise_not(img, img);

                    Mat circles = new Mat();
                    Imgproc.HoughCircles(img, circles, Imgproc.CV_HOUGH_GRADIENT, 1, 5, 100, 90, 3, 300);

                    Paint paint = new Paint();
                    paint.setColor(0x88FF0000);
                    paint.setStrokeWidth(7);
                    paint.setStyle(Paint.Style.STROKE);

                    float circle[] = new float[3];
                    int r;
                    int margin = 10;
                    for(int i=0; i<circles.cols(); i++)
                    {
                        circles.get(0, i, circle);
                        r = Math.round(circle[2]);
                        canvas.drawCircle(circle[0], circle[1], r + margin, paint);
                    }

                    return true;
                }
            };

            asyncTask.execute();
        }

    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
    {
        if (event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))
        {
            mInputMethodManager.hideSoftInputFromWindow(mURLText.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

            String urlString = mURLText.getText().toString();
            if ( !TextUtils.isEmpty(urlString) )
            {
                urlString = urlString.trim();
                if ( urlString.startsWith("http") ) {
                    mWebView.loadUrl(urlString);
                } else {
                    mWebView.loadUrl("http://"+ urlString);
                }
            }

            return true;
        }
        return false;
    }

    public void afterTextChanged(Editable editable)
    {

    }

    public void beforeTextChanged(CharSequence s, int start, int count,	int after)
    {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count)
    {
    }


    private class MyWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            mProcessButton.setEnabled(false);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            mProcessButton.setEnabled(true);
            super.onPageFinished(view, url);
        }
    }
}
