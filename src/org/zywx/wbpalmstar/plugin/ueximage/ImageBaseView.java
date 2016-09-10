package org.zywx.wbpalmstar.plugin.ueximage;

import org.zywx.wbpalmstar.plugin.ueximage.util.Constants;

import android.content.Context;
import android.view.KeyEvent;
import android.widget.RelativeLayout;

public class ImageBaseView extends RelativeLayout {
    protected Context mContext;
    protected EUExImage mEUExImage;
    protected int mRequestCode;
    protected ViewEvent mViewEvent;
    private String TAG = "";

    public ImageBaseView(Context context, EUExImage eUExImage,
            int requestCode, ViewEvent viewEvent, String tag) {
        super(context);
        mContext = context;
        mEUExImage = eUExImage;
        mRequestCode = requestCode;
        mViewEvent = viewEvent;
        TAG = tag;
        requestViewFocus();
    }

    protected void requestViewFocus() {
        if (isInTouchMode()) {
            setFocusableInTouchMode(true);
            requestFocusFromTouch();
        } else {
            setFocusable(true);
            requestFocus();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            finish(TAG, Constants.OPERATION_CANCELLED);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    protected void onResume() {
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    /**
     * @param resultCode
     *            原来只有Activity.RESULT_OK,如不通过Activity.setResult设置resultCode，
     *            resultCode默认为Activity.RESULT_CANCELED，故原来未setResult的，
     *            添加默认值Activity.RESULT_CANCELED。
     */
    protected void finish(String viewTag, int resultCode) {
        if (mViewEvent != null) {
            mViewEvent.resultCallBack();
        }
        if (mEUExImage != null) {
            mEUExImage.removeViewFromCurWindow(viewTag, mRequestCode, resultCode);
        }
    }

    public static interface ViewEvent {
        public void resultCallBack();
    };

}
