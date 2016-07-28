package org.zywx.wbpalmstar.plugin.ueximage;

import android.content.Context;
import android.widget.RelativeLayout;

public class ImageBaseView extends RelativeLayout {
    protected Context mContext;
    protected EUExImage mEUExImage;
    protected int mRequestCode;

    public ImageBaseView(Context context, EUExImage eUExImage,
            int requestCode) {
        super(context);
        mContext = context;
        mEUExImage = eUExImage;
        mRequestCode = requestCode;
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
        if (mEUExImage != null) {
            mEUExImage.removeViewFromCurWindow(viewTag, mRequestCode, resultCode);
        }
    }

}
