package com.sxd.taobaocoupon.ui.activity;


import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.sxd.taobaocoupon.R;
import com.sxd.taobaocoupon.presenter.impl.TicketPresentImpl;
import com.sxd.taobaocoupon.util.ConstantsUtils;
import com.vondear.rxfeature.activity.ActivityScanerCode;
import com.vondear.rxfeature.module.scaner.CameraManager;
import com.vondear.rxfeature.module.scaner.OnRxScanerListener;
import com.vondear.rxfeature.module.scaner.PlanarYUVLuminanceSource;
import com.vondear.rxfeature.module.scaner.decoding.InactivityTimer;
import com.vondear.rxfeature.tool.RxQrBarTool;
import com.vondear.rxtool.RxAnimationTool;
import com.vondear.rxtool.RxBarTool;
import com.vondear.rxtool.RxBeepTool;
import com.vondear.rxtool.RxConstants;
import com.vondear.rxtool.RxDataTool;
import com.vondear.rxtool.RxPhotoTool;
import com.vondear.rxtool.RxSPTool;
import com.vondear.rxtool.view.RxToast;
import com.vondear.rxui.view.dialog.RxDialogSure;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import static android.content.ContentValues.TAG;


// ???com.vondear.rxfeature.activity?????????ActivityScanerCode?????????
// ????????????????????????????????????????????????????????????????????????????????????private??????????????????get???????????????????????????copy??????????????????
public class ScanQrCodeActivity extends FragmentActivity {

    /**
     * ??????????????????
     */
    private static OnRxScanerListener mScannerListener;

    private InactivityTimer inactivityTimer;

    /**
     * ????????????
     */
    private CaptureActivityHandler handler;

    /**
     * ???????????????
     */
    private RelativeLayout mContainer = null;

    /**
     * ??????????????????
     */
    private RelativeLayout mCropLayout = null;

    /**
     * ?????????????????????
     */
    private int mCropWidth = 0;

    /**
     * ?????????????????????
     */
    private int mCropHeight = 0;

    /**
     * ???????????????
     */
    private boolean hasSurface;

    /**
     * ???????????????????????????
     */
    private boolean vibrate = true;

    /**
     * ?????????????????????
     */
    private boolean mFlashing = true;

    /**
     * ??????????????? & ????????? ??????
     */
    private LinearLayout mLlScanHelp;

    /**
     * ????????? ??????
     */
    private ImageView mIvLight;

    /**
     * ?????????????????????
     */
    private RxDialogSure rxDialogSure;

    /**
     * ????????????????????????
     */
    public static void setScanerListener(OnRxScanerListener scanerListener) {
        mScannerListener = scanerListener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RxBarTool.setNoTitle(this);
        setContentView(R.layout.activity_scaner_qr_code);
        RxBarTool.setTransparentStatusBar(this);
        //?????????????????????
        initDecode();
        initView();
        //???????????????
        initPermission();
        //?????????????????????
        initScanerAnimation();
        //????????? CameraManager
        CameraManager.init(this);
        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);
    }

    private void initDecode() {
        multiFormatReader = new MultiFormatReader();

        // ???????????????
        Hashtable<DecodeHintType,Object> hints = new Hashtable<DecodeHintType,Object>(2);
        // ???????????????????????????
        Vector<BarcodeFormat> decodeFormats = new Vector<BarcodeFormat>();
        if(decodeFormats == null || decodeFormats.isEmpty()) {
            decodeFormats = new Vector<BarcodeFormat>();

            Vector<BarcodeFormat> PRODUCT_FORMATS = new Vector<BarcodeFormat>(5);
            PRODUCT_FORMATS.add(BarcodeFormat.UPC_A);
            PRODUCT_FORMATS.add(BarcodeFormat.UPC_E);
            PRODUCT_FORMATS.add(BarcodeFormat.EAN_13);
            PRODUCT_FORMATS.add(BarcodeFormat.EAN_8);
            // PRODUCT_FORMATS.add(BarcodeFormat.RSS14);
            Vector<BarcodeFormat> ONE_D_FORMATS = new Vector<BarcodeFormat>(PRODUCT_FORMATS.size() + 4);
            ONE_D_FORMATS.addAll(PRODUCT_FORMATS);
            ONE_D_FORMATS.add(BarcodeFormat.CODE_39);
            ONE_D_FORMATS.add(BarcodeFormat.CODE_93);
            ONE_D_FORMATS.add(BarcodeFormat.CODE_128);
            ONE_D_FORMATS.add(BarcodeFormat.ITF);
            Vector<BarcodeFormat> QR_CODE_FORMATS = new Vector<BarcodeFormat>(1);
            QR_CODE_FORMATS.add(BarcodeFormat.QR_CODE);
            Vector<BarcodeFormat> DATA_MATRIX_FORMATS = new Vector<BarcodeFormat>(1);
            DATA_MATRIX_FORMATS.add(BarcodeFormat.DATA_MATRIX);

            // ????????????????????????????????????????????????????????????
            decodeFormats.addAll(ONE_D_FORMATS);
            decodeFormats.addAll(QR_CODE_FORMATS);
            decodeFormats.addAll(DATA_MATRIX_FORMATS);
        }
        hints.put(DecodeHintType.POSSIBLE_FORMATS,decodeFormats);

        multiFormatReader.setHints(hints);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        super.onResume();
        SurfaceView surfaceView = findViewById(R.id.capture_preview);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if(hasSurface) {
            //Camera?????????
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceChanged(SurfaceHolder holder,int format,int width,int height) {

                }

                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    if(!hasSurface) {
                        hasSurface = true;
                        initCamera(holder);
                    }
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    hasSurface = false;

                }
            });
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(handler != null) {
            handler.quitSynchronously();
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
        CameraManager.get().closeDriver();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        mScannerListener = null;
        super.onDestroy();
    }

    private void initView() {
        mIvLight = findViewById(R.id.top_mask);
        mContainer = findViewById(R.id.capture_containter);
        mCropLayout = findViewById(R.id.capture_crop_layout);
        mLlScanHelp = findViewById(R.id.ll_scan_help);
    }

    private void initPermission() {
        //??????Camera?????? ??? ???????????? ??????
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }
    }

    private void initScanerAnimation() {
        ImageView mQrLineView = findViewById(R.id.capture_scan_line);
        RxAnimationTool.ScaleUpDowm(mQrLineView);
    }

    public int getCropWidth() {
        return mCropWidth;
    }

    public void setCropWidth(int cropWidth) {
        mCropWidth = cropWidth;
        CameraManager.FRAME_WIDTH = mCropWidth;

    }

    public int getCropHeight() {
        return mCropHeight;
    }

    public void setCropHeight(int cropHeight) {
        this.mCropHeight = cropHeight;
        CameraManager.FRAME_HEIGHT = mCropHeight;
    }

    public void btn(View view) {
        int viewId = view.getId();
        if(viewId == R.id.top_mask) {
            light();
        } else if(viewId == R.id.top_back) {
            finish();
        } else if(viewId == R.id.top_openpicture) {
            RxPhotoTool.openLocalImage(this);
        }
    }

    private void light() {
        if(mFlashing) {
            mFlashing = false;
            // ????????????
            CameraManager.get().openLight();
        } else {
            mFlashing = true;
            // ????????????
            CameraManager.get().offLight();
        }

    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            CameraManager.get().openDriver(surfaceHolder);
            Point point = CameraManager.get().getCameraResolution();
            AtomicInteger width = new AtomicInteger(point.y);
            AtomicInteger height = new AtomicInteger(point.x);
            int cropWidth = mCropLayout.getWidth() * width.get() / mContainer.getWidth();
            int cropHeight = mCropLayout.getHeight() * height.get() / mContainer.getHeight();
            setCropWidth(cropWidth);
            setCropHeight(cropHeight);
        } catch(IOException | RuntimeException ioe) {
            return;
        }
        if(handler == null) {
            handler = new CaptureActivityHandler();
        }
    }


    //--------------------------------------????????????????????????????????? start---------------------------------
    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode == Activity.RESULT_OK) {
            ContentResolver resolver = getContentResolver();
            // ???????????????????????????
            Uri originalUri = data.getData();
            try {
                // ??????ContentProvider??????URI??????????????????
                Bitmap photo = MediaStore.Images.Media.getBitmap(resolver,originalUri);

                // ???????????????????????????
                Result rawResult = RxQrBarTool.decodeFromPhoto(photo);
                if(rawResult != null) {
                    if(mScannerListener == null) {
                        initDialogResult(rawResult);
                    } else {
                        mScannerListener.onSuccess("From to Picture",rawResult);
                    }
                } else {
                    if(mScannerListener == null) {
                        RxToast.error("??????????????????.");
                    } else {
                        mScannerListener.onFail("From to Picture","??????????????????");
                    }
                }
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
    //========================================????????????????????????????????? end=================================


    private void initDialogResult(Result result) {
        BarcodeFormat type = result.getBarcodeFormat();
        String realContent = result.getText();

        if(rxDialogSure == null) {
            //????????????
            rxDialogSure = new RxDialogSure(this);
        }

        if(BarcodeFormat.QR_CODE.equals(type)) {
            rxDialogSure.setTitle("?????????????????????");
        } else if(BarcodeFormat.EAN_13.equals(type)) {
            rxDialogSure.setTitle("?????????????????????");
        } else {
            rxDialogSure.setTitle("????????????");
        }

        rxDialogSure.setContent(realContent);
        rxDialogSure.setSureListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rxDialogSure.cancel();
            }
        });
        rxDialogSure.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if(handler != null) {
                    // ???????????????????????????????????????????????????????????????????????????

                    handler.sendEmptyMessage(com.vondear.rxfeature.R.id.restart_preview);
                }
            }
        });

        if(!rxDialogSure.isShowing()) {
            rxDialogSure.show();
        }

        RxSPTool.putContent(this,RxConstants.SP_SCAN_CODE,RxDataTool.stringToInt(RxSPTool.getContent(this,RxConstants.SP_SCAN_CODE)) + 1 + "");
    }

    // sxd : ?????????????????????????????? ???????????????TicketActivity??????
    //
    public void handleDecode(Result result) {
        inactivityTimer.onActivity();
        //??????????????????????????????????????????
        RxBeepTool.playBeep(this,vibrate);

        final String resultText = result.getText();
//        Log.e("sxd",result1);
        //??????????????????
        // ??????????????????????????????
        if(resultText.contains("taobao.com")) {
            // ???TicketPrensent????????????
            // ???????????????????????????????????????????????????????????????url
            //????????????????????????
            Intent intent = new Intent(this, TicketActivity.class);
            intent.putExtra(ConstantsUtils.KEY_INTENT_TICKET_TITLE, "");
            intent.putExtra(ConstantsUtils.KEY_INTENT_TICKET_COVER, "");
            intent.putExtra(ConstantsUtils.KEY_INTENT_TICKET_URL, resultText);
            Log.e("sxd", resultText);
            startActivity(intent);
        } else {
            //???????????????
//            ToastUtil.showToast("?????????????????????");
            Toast.makeText(this, "???????????????!", Toast.LENGTH_LONG).show();
        }
    }
    //==============================================================================================???????????? ??? ???????????? end

    final class CaptureActivityHandler extends Handler {

        DecodeThread decodeThread = null;
        private State state;

        public CaptureActivityHandler() {
            decodeThread = new DecodeThread();
            decodeThread.start();
            state = State.SUCCESS;
            CameraManager.get().startPreview();
            restartPreviewAndDecode();
        }

        @Override
        public void handleMessage(Message message) {
            if(message.what == com.vondear.rxfeature.R.id.auto_focus) {
                if(state == State.PREVIEW) {
                    CameraManager.get().requestAutoFocus(this,com.vondear.rxfeature.R.id.auto_focus);
                }
            } else if(message.what == com.vondear.rxfeature.R.id.restart_preview) {
                restartPreviewAndDecode();
            } else if(message.what == com.vondear.rxfeature.R.id.decode_succeeded) {
                state = State.SUCCESS;
                handleDecode((Result) message.obj);// ?????????????????????
            } else if(message.what == com.vondear.rxfeature.R.id.decode_failed) {
                state = State.PREVIEW;
                CameraManager.get().requestPreviewFrame(decodeThread.getHandler(),com.vondear.rxfeature.R.id.decode);
            }
        }

        public void quitSynchronously() {
            state = State.DONE;
            decodeThread.interrupt();
            CameraManager.get().stopPreview();
            Message quit = Message.obtain(decodeThread.getHandler(),com.vondear.rxfeature.R.id.quit);
            quit.sendToTarget();
            try {
                decodeThread.join();
            } catch(InterruptedException e) {
                e.printStackTrace();
            } finally {
                removeMessages(com.vondear.rxfeature.R.id.decode_succeeded);
                removeMessages(com.vondear.rxfeature.R.id.decode_failed);
                removeMessages(com.vondear.rxfeature.R.id.decode);
                removeMessages(com.vondear.rxfeature.R.id.auto_focus);
            }

        }

        private void restartPreviewAndDecode() {
            if(state == State.SUCCESS) {
                state = State.PREVIEW;
                CameraManager.get().requestPreviewFrame(decodeThread.getHandler(),com.vondear.rxfeature.R.id.decode);
                CameraManager.get().requestAutoFocus(this,com.vondear.rxfeature.R.id.auto_focus);
            }
        }
    }

    final class DecodeThread extends Thread {

        private final CountDownLatch handlerInitLatch;
        private Handler handler;

        DecodeThread() {
            handlerInitLatch = new CountDownLatch(1);
        }

        Handler getHandler() {
            try {
                handlerInitLatch.await();
            } catch(InterruptedException ie) {
                // continue?
            }
            return handler;
        }

        @Override
        public void run() {
            Looper.prepare();
            handler = new DecodeHandler();
            handlerInitLatch.countDown();
            Looper.loop();
        }
    }

    final class DecodeHandler extends Handler {
        DecodeHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            if(message.what == com.vondear.rxfeature.R.id.decode) {
                decode((byte[]) message.obj,message.arg1,message.arg2);
            } else if(message.what == com.vondear.rxfeature.R.id.quit) {
                Looper.myLooper().quit();
            }
        }
    }

    private MultiFormatReader multiFormatReader;

    private void decode(byte[] data,int width,int height) {
        long start = System.currentTimeMillis();
        Result rawResult = null;

        //modify here
        byte[] rotatedData = new byte[data.length];
        for(int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++) {
                rotatedData[x * height + height - y - 1] = data[x + y * width];
            }
        }
        // Here we are swapping, that's the difference to #11
        int tmp = width;
        width = height;
        height = tmp;

        PlanarYUVLuminanceSource source = CameraManager.get().buildLuminanceSource(rotatedData,width,height);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            rawResult = multiFormatReader.decodeWithState(bitmap);
        } catch(ReaderException e) {
            // continue
        } finally {
            multiFormatReader.reset();
        }

        if(rawResult != null) {
            long end = System.currentTimeMillis();
            Log.d(TAG,"Found barcode (" + (end - start) + " ms):\n" + rawResult.toString());
            Message message = Message.obtain(handler,com.vondear.rxfeature.R.id.decode_succeeded,rawResult);
            Bundle bundle = new Bundle();
            bundle.putParcelable("barcode_bitmap",source.renderCroppedGreyscaleBitmap());
            message.setData(bundle);
            //Log.d(TAG, "Sending decode succeeded message...");
            message.sendToTarget();
        } else {
            Message message = Message.obtain(handler,com.vondear.rxfeature.R.id.decode_failed);
            message.sendToTarget();
        }
    }

    private enum State {
        //??????
        PREVIEW,
        //??????
        SUCCESS,
        //??????
        DONE
    }

    // sxd ??????finish?????? ???activity??????????????? ???????????????
    @Override
    public void finish() {
        moveTaskToBack(true);
    }

}
