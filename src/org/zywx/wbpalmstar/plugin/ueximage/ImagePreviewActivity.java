/*
 * Copyright (c) 2015.  The AppCan Open Source Project.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */
package org.zywx.wbpalmstar.plugin.ueximage;

import java.io.File;
import java.util.List;

import org.json.JSONArray;
import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.engine.universalex.EUExUtil;
import org.zywx.wbpalmstar.plugin.ueximage.model.PictureInfo;
import org.zywx.wbpalmstar.plugin.ueximage.util.CommonUtil;
import org.zywx.wbpalmstar.plugin.ueximage.util.Constants;
import org.zywx.wbpalmstar.plugin.ueximage.util.EUEXImageConfig;
import org.zywx.wbpalmstar.plugin.ueximage.util.UEXImageUtil;

import com.ace.universalimageloader.core.DisplayImageOptions;
import com.ace.universalimageloader.core.ImageLoader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ImagePreviewActivity extends ImageBaseView {

    public final static String TAG = "ImagePreviewView";
    private ViewPager viewPager;
    private String folderName;
    private ImageView ivGoBack;
    private TextView tvTitle;
    private Button btnFinishInTitle;
    private CheckBox cbChoose;
    private TextView tvCheckbox;
    private List<String> checkedItems;
    private UEXImageUtil uexImageUtil;
    private ImageView imageView;
    private List<PictureInfo> picList;
    private int picIndex;
    private boolean isOpenBrowser;
    // 仅在浏览图片时有用。
    private TextView tvShare;
    private TextView tvToGrid;
    /** * 切换到Grid浏览模式 */
    private ImageView ivToGrid;
    private RelativeLayout rlTitle;
    private RelativeLayout rlBottom;
    private AlphaAnimation fadeInAnim;
    private AlphaAnimation fadeOutAnim;
    /** *单张浏览模式下，3s没有任何操作，隐藏切换到Grid浏览模式的ImageView */
    private Handler hideIvToGridHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case Constants.WHAT_HIDE_IV_TO_GRID:
                if (ivToGrid != null) {
                    ivToGrid.setVisibility(View.INVISIBLE);
                }
                break;
            case Constants.WHAT_SHOW_IV_TO_GRID:
                if (ivToGrid != null) {
                    ivToGrid.setVisibility(View.VISIBLE);
                }
                break;
            }
        }
    };

    private OnClickListener toGridClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            startPictureGridActivity(mContext);
        }
    };

    /**
     * @param context
     * @param mEUExImage
     * @param folderName
     *            isOpenBrowser为false时，才使用此值
     * @param picIndex
     *            isOpenBrowser为false时，才使用此值
     * @param requestCode
     *            this code will be returned in finish() when the view exits.
     */
    public ImagePreviewActivity(Context context, EUExImage mEUExImage,
            String folderName, int picIndex, int requestCode) {
        super(context, mEUExImage, requestCode);
        mContext = context;
        onCreate(context, folderName, picIndex);
    }

    private void onCreate(Context context, String folderName, int picIndex) {
        LayoutInflater.from(context)
                .inflate(
                        EUExUtil.getResLayoutID(
                                "plugin_uex_image_activity_image_preview"),
                        this, true);
        uexImageUtil = UEXImageUtil.getInstance();
        isOpenBrowser = EUEXImageConfig.getInstance().getIsOpenBrowser();
        rlTitle = (RelativeLayout) findViewById(
                EUExUtil.getResIdID("title_layout"));
        ivGoBack = (ImageView) findViewById(
                EUExUtil.getResIdID("iv_left_on_title"));
        tvTitle = (TextView) findViewById(EUExUtil.getResIdID("tv_title"));
        ivToGrid = (ImageView) findViewById(EUExUtil.getResIdID("iv_to_grid"));
        btnFinishInTitle = (Button) findViewById(
                EUExUtil.getResIdID("btn_finish_title"));
        viewPager = (ViewPager) findViewById(EUExUtil.getResIdID("vp_picture"));
        cbChoose = (CheckBox) findViewById(EUExUtil.getResIdID("checkbox"));
        rlBottom = (RelativeLayout) findViewById(
                EUExUtil.getResIdID("rl_bottom"));
        rlTitle.setAlpha(0.9f);
        rlBottom.setAlpha(0.9f);

        initData(folderName, picIndex);
        if (isOpenBrowser) {
            initViewForBrowser(context);
        } else {
            initViewForPicker(context);
        }
        initAnimation();
    }

    private void initData(String folder, int index) {
        if (isOpenBrowser) {
            JSONArray imageDataArray = EUEXImageConfig.getInstance()
                    .getDataArray();
            picList = uexImageUtil.transformData(imageDataArray);
            picIndex = EUEXImageConfig.getInstance().getStartIndex();
        } else {
            folderName = folder;
            picIndex = index;
            checkedItems = uexImageUtil.getCheckedItems();
            picList = uexImageUtil.getCurrentPicList();
        }
    }

    private void initViewForPicker(final Context context) {
        tvTitle.setText(folderName);
        ivGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(TAG, Activity.RESULT_CANCELED);
            }
        });
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(picIndex);
        viewPager.setOnPageChangeListener(onPageChangeListener);
        if (checkedItems.size() > 0) {
            btnFinishInTitle.setText("完成(" + checkedItems.size() + "/"
                    + EUEXImageConfig.getInstance().getMaxImageCount() + ")");
            btnFinishInTitle.setEnabled(true);
        }
        cbChoose.setTag(picList.get(picIndex).getSrc());
        cbChoose.setOnCheckedChangeListener(onCheckedChangeListener);
        btnFinishInTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkedItems.size() >= EUEXImageConfig.getInstance()
                        .getMinImageCount()) {
                    // setResult(RESULT_OK, null);
                    finish(TAG, Activity.RESULT_OK);
                } else {
                    String str = String.format(
                            EUExUtil.getString(
                                    "plugin_uex_image_at_least_choose"),
                            EUEXImageConfig.getInstance().getMinImageCount());
                    Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_UP:
            if ((Constants.NEW_STYLE == EUEXImageConfig.getInstance()
                    .getUIStyle()) && (ivToGrid != null)) {
                showIvToGridDelayed();
                if (hideIvToGridHandler
                        .hasMessages(Constants.WHAT_HIDE_IV_TO_GRID)) {
                    hideIvToGridHandler
                            .removeMessages(Constants.WHAT_HIDE_IV_TO_GRID);
                }
                hideIvToGridDelayed();
            }
            break;
        }
        return super.dispatchTouchEvent(event);
    }

    private void startPictureGridActivity(Context context) {
        finish(TAG, Activity.RESULT_CANCELED);
        View imagePreviewView = new PictureGridActivity(context, mEUExImage,
                "", Constants.REQUEST_IMAGE_BROWSER);
        mEUExImage.addViewToWebView(imagePreviewView, PictureGridActivity.TAG);
    }

    private void initViewForBrowser(final Context context) {

        ivGoBack.setVisibility(View.INVISIBLE);
        tvCheckbox = (TextView) findViewById(
                EUExUtil.getResIdID("tv_checkbox"));
        cbChoose.setVisibility(View.INVISIBLE);
        tvCheckbox.setVisibility(View.INVISIBLE);

        switch (EUEXImageConfig.getInstance().getUIStyle()) {
        case Constants.OLD_STYLE:
            tvShare = (TextView) findViewById(EUExUtil.getResIdID("tv_share"));
            tvToGrid = (TextView) findViewById(
                    EUExUtil.getResIdID("tv_to_grid"));
            tvToGrid.setVisibility(View.VISIBLE);
            tvToGrid.setOnClickListener(toGridClickListener);

            btnFinishInTitle.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    // setResult(RESULT_OK, null);
                    finish(TAG, Activity.RESULT_OK);
                }
            });
            if (EUEXImageConfig.getInstance().isDisplayActionButton()) {
                tvShare.setVisibility(View.VISIBLE);
                tvShare.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        final String src = picList.get(picIndex).getSrc();
                        Bitmap bitmap;
                        if (src.substring(0, 4)
                                .equalsIgnoreCase(Constants.HTTP)) {
                            bitmap = ImageLoader.getInstance()
                                    .loadImageSync(src);
                        } else {
                            bitmap = CommonUtil.getLocalImage(context, src);
                        }
                        File file = new File(
                                Environment.getExternalStorageDirectory(),
                                File.separator + UEXImageUtil.TEMP_PATH
                                        + File.separator
                                        + "uex_image_to_share.jpg");
                        if (bitmap == null) {
                            Toast.makeText(context, "当前图片尚未加载完毕，请稍后重试",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (CommonUtil.saveBitmap2File(bitmap, file)) {
                            Intent shareIntent = new Intent();
                            shareIntent.setAction(Intent.ACTION_SEND);
                            shareIntent.putExtra(Intent.EXTRA_STREAM,
                                    Uri.fromFile(file));
                            shareIntent.setType("image/*");
                            // startActivity(
                            // Intent.createChooser(shareIntent, "分享到"));
                        } else {
                            Toast.makeText(context, "图片操作失败，请重试",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                if (!EUEXImageConfig.getInstance().isEnableGrid()) {
                    ((View) tvShare.getParent()).setVisibility(View.INVISIBLE);
                }
            }
            break;
        case Constants.NEW_STYLE:
            rlTitle.setVisibility(View.INVISIBLE);
            // rlBottom.setVisibility(View.INVISIBLE);
            ivToGrid.setVisibility(View.VISIBLE);
            ivToGrid.setOnClickListener(toGridClickListener);
            hideIvToGridDelayed();
            break;
        default:
            BDebug.e(TAG, "EUExImage UIStyle is error.");
            break;
        }
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(picIndex);
        viewPager.setOnPageChangeListener(onPageChangeListener);
    }

    private void showIvToGridDelayed() {
        hideIvToGridHandler.sendEmptyMessageDelayed(
                Constants.WHAT_SHOW_IV_TO_GRID,
                Constants.SHOW_IV_TO_GRID_TIMEOUT);
    }

    private void hideIvToGridDelayed() {
        hideIvToGridHandler.sendEmptyMessageDelayed(
                Constants.WHAT_HIDE_IV_TO_GRID,
                Constants.HIDE_IV_TO_GRID_TIMEOUT);
    }

    @Override
    protected void onResume() {
        if (!isOpenBrowser) {
            cbChoose.setChecked(checkedItems.contains(picList.get(picIndex)));
        }
        if (1 == picList.size()) {
            tvTitle.setText("1" + "/" + picList.size());
        } else {
            tvTitle.setText((picIndex + 1) + "/" + picList.size());
        }
    }

    private PagerAdapter adapter = new PagerAdapter() {

        @Override
        public int getCount() {
            return picList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            return view == o;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            LayoutInflater inflater = (LayoutInflater) container.getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(
                    EUExUtil.getResLayoutID("plugin_uex_image_view_pager_item"),
                    null);
            imageView = (ImageView) view
                    .findViewById(EUExUtil.getResIdID("image"));

            // 显示图片的配置
            DisplayImageOptions options = new DisplayImageOptions.Builder()
                    .cacheInMemory(true).cacheOnDisk(true)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .showImageOnLoading(
                            EUExUtil.getResIdID("plugin_uex_image_loading"))
                    .considerExifParams(true)// 考虑Exif旋转
                    .build();

            final String src = picList.get(position).getSrc();
            if (!isOpenBrowser) {
                ImageLoader.getInstance().displayImage(src, imageView, options);
            } else {// 浏览图片：对于传入的图片的加载
                if (src.substring(0, 4).equalsIgnoreCase(Constants.HTTP)) {
                    // 如果是从网上下载图片，需要将下载后的图片存到缓存中
                    ImageLoader.getInstance().displayImage(src, imageView,
                            options);
                } else {
                    Bitmap bitmap = CommonUtil.getLocalImage(mContext, src);
                    imageView.setImageBitmap(bitmap);
                }
            }
            imageView.setOnClickListener(imageClickListener);
            imageView.setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    mEUExImage.onImageLongClick();
                    return false;
                }
            });
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position,
                Object object) {
            container.removeView((View) object);
        }
    };

    private View.OnClickListener imageClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (EUEXImageConfig.getInstance().getUIStyle()) {
            case Constants.OLD_STYLE:
                toogleView();
                break;
            case Constants.NEW_STYLE:
                if (hideIvToGridHandler
                        .hasMessages(Constants.WHAT_SHOW_IV_TO_GRID)) {
                    hideIvToGridHandler
                            .removeMessages(Constants.WHAT_SHOW_IV_TO_GRID);
                }
                // setResult(RESULT_OK, null);
                finish(TAG, Activity.RESULT_OK);
                break;
            default:
                break;
            }
        }
    };

    private void toogleView() {
        if (rlTitle.getVisibility() == View.VISIBLE) {
            rlTitle.setVisibility(View.INVISIBLE);
            rlTitle.startAnimation(fadeOutAnim);
            rlBottom.setVisibility(View.INVISIBLE);
            rlBottom.startAnimation(fadeOutAnim);
        } else {
            rlTitle.setVisibility(View.VISIBLE);
            rlTitle.startAnimation(fadeInAnim);
            rlBottom.setVisibility(View.VISIBLE);
            rlBottom.startAnimation(fadeInAnim);
        }
    }

    private void initAnimation() {
        final int duration = 300;
        LinearInterpolator interpolator = new LinearInterpolator();
        fadeInAnim = new AlphaAnimation(0, 1);
        fadeInAnim.setDuration(duration);
        fadeInAnim.setInterpolator(interpolator);

        fadeOutAnim = new AlphaAnimation(1, 0);
        fadeOutAnim.setDuration(duration);
        fadeOutAnim.setInterpolator(interpolator);
    }

    private ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i1) {
        }

        @Override
        public void onPageSelected(int i) {
            picIndex = i;
            if (!isOpenBrowser) {
                cbChoose.setChecked(
                        checkedItems.contains(picList.get(i).getSrc()));
            }
            if (1 == picList.size()) {
                tvTitle.setText("1" + "/" + picList.size());
            } else {
                tvTitle.setText((picIndex + 1) + "/" + picList.size());
            }
        }

        @Override
        public void onPageScrollStateChanged(int i) {
            if (!isOpenBrowser) {
                if (i == 1) {// 开始滑动
                    cbChoose.setOnCheckedChangeListener(null);
                } else if (i == 0) {// 静止
                    cbChoose.setOnCheckedChangeListener(
                            onCheckedChangeListener);
                    cbChoose.setTag(picList.get(picIndex).getSrc());
                }
            }
        }
    };
    // 仅当在选择图片时才会用得上
    private CheckBox.OnCheckedChangeListener onCheckedChangeListener = new CheckBox.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked) {
            if (isChecked) {
                if (!checkedItems.contains(buttonView.getTag())) {
                    if (checkedItems.size() >= EUEXImageConfig.getInstance()
                            .getMaxImageCount()) {
                        Toast.makeText(mContext,
                                "最多选择" + EUEXImageConfig.getInstance()
                                        .getMaxImageCount() + "张图片",
                                Toast.LENGTH_SHORT).show();
                        buttonView.setChecked(false);
                        return;
                    }
                    checkedItems.add((String) (buttonView.getTag()));
                }
            } else {
                if (checkedItems.contains(buttonView.getTag())) {
                    checkedItems.remove(buttonView.getTag());
                }
            }
            if (checkedItems.size() > 0) {
                btnFinishInTitle.setText("完成(" + checkedItems.size() + "/"
                        + EUEXImageConfig.getInstance().getMaxImageCount()
                        + ")");
                btnFinishInTitle.setEnabled(true);
            } else {
                btnFinishInTitle.setText("完成");
                btnFinishInTitle.setEnabled(false);
            }
        }
    };
}
