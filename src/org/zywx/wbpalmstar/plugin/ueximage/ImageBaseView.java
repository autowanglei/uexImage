package org.zywx.wbpalmstar.plugin.ueximage;

import android.content.Context;
import android.widget.RelativeLayout;

public class ImageBaseView extends RelativeLayout {
    protected Context mContext;
    protected EUExImage mEUExImage;

    public ImageBaseView(Context context, EUExImage eUExImage) {
        super(context);
        mContext = context;
        mEUExImage = eUExImage;
    }

    public void onResume() {
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

}
