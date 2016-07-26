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

    public void onResume() {
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

}
