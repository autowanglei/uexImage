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
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.plugin.ueximage.crop.Crop;
import org.zywx.wbpalmstar.plugin.ueximage.util.CommonUtil;
import org.zywx.wbpalmstar.plugin.ueximage.util.Constants;
import org.zywx.wbpalmstar.plugin.ueximage.util.DataParser;
import org.zywx.wbpalmstar.plugin.ueximage.util.EUEXImageConfig;
import org.zywx.wbpalmstar.plugin.ueximage.util.UEXImageUtil;
import org.zywx.wbpalmstar.plugin.ueximage.vo.ViewFrameVO;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class EUExImage extends EUExBase {
    private static final String TAG = "EUExImage";

    private double cropQuality = 0.5;
    private boolean cropUsePng = false;
    // 当前Android只支持方型裁剪
    private int cropMode = 1;
    private File cropOutput = null;
    private Context context;
    private UEXImageUtil uexImageUtil;
    // private ResoureFinder finder;
    private final String FILE_SYSTEM_ERROR = "文件系统操作出错";
    private final String SAME_FILE_IN_DCIM = "系统相册中存在同名文件";
    private final String JSON_FORMAT_ERROR = "json格式错误";
    private final String NOT_SUPPORT_CROP = "你的设备不支持剪切功能！";
    /** * 保存添加到网页的view */
    private static Map<String, View> addToWebViewsMap = new HashMap<String, View>();

    public EUExImage(Context context, EBrowserView eBrowserView) {
        super(context, eBrowserView);
        this.context = context;
        // 创建缓存文件夹
        File f = new File(Environment.getExternalStorageDirectory(),
                File.separator + UEXImageUtil.TEMP_PATH);
        if (!f.exists()) {
            f.mkdirs();
        }
        CommonUtil.initImageLoader(context);
        uexImageUtil = UEXImageUtil.getInstance();
        // finder = ResoureFinder.getInstance(context);
    }

    @Override
    protected boolean clean() {
        return false;
    }

    public void openPicker(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        String json = params[0];
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(json);
            if (jsonObject.has("min")) {
                int min = jsonObject.getInt("min");
                EUEXImageConfig.getInstance().setMinImageCount(min);
            }
            if (jsonObject.has("max")) {
                int max = jsonObject.getInt("max");
                EUEXImageConfig.getInstance().setMaxImageCount(max);
            }
            if (jsonObject.has("quality")) {
                double quality = jsonObject.getDouble("quality");
                EUEXImageConfig.getInstance().setQuality(quality);
            }
            if (jsonObject.has("usePng")) {
                Boolean usePng = jsonObject.getBoolean("usePng");
                EUEXImageConfig.getInstance().setIsUsePng(usePng);
            }
            if (jsonObject.has("detailedInfo")) {
                Boolean detailedInfo = jsonObject.getBoolean("detailedInfo");
                EUEXImageConfig.getInstance()
                        .setIsShowDetailedInfo(detailedInfo);
            }
            EUEXImageConfig.getInstance().setIsOpenBrowser(false);
            Intent intent = new Intent(context, AlbumListActivity.class);
            startActivityForResult(intent, Constants.REQUEST_IMAGE_PICKER);

        } catch (JSONException e) {
            if (BDebug.DEBUG) {
                Log.i(TAG, e.getMessage());
            }
            Toast.makeText(context, "JSON解析错误", Toast.LENGTH_SHORT).show();
        }
    }

    public void openBrowser(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        String json = params[0];
        try {
            JSONObject jsonObject = new JSONObject(json);
            EUEXImageConfig config = EUEXImageConfig.getInstance();
            if (!jsonObject.has("data")) {
                Toast.makeText(context, "data不能为空", Toast.LENGTH_SHORT).show();
                return;
            } else {
                JSONArray data = jsonObject.getJSONArray("data");
                for (int i = 0; i < data.length(); i++) {
                    if (data.get(i) instanceof String) {
                        String path = data.getString(i);
                        String realPath = BUtility.makeRealPath(
                                BUtility.makeUrl(mBrwView.getCurrentUrl(),
                                        path),
                                mBrwView.getCurrentWidget().m_widgetPath,
                                mBrwView.getCurrentWidget().m_wgtType);
                        data.put(i, realPath);
                    } else {
                        JSONObject obj = data.getJSONObject(i);
                        if (!obj.has("src")) {
                            Toast.makeText(context,
                                    "data中第" + (i + 1) + "个元素的src不能为空",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String src = obj.getString("src");
                        String srcPath = BUtility.makeRealPath(
                                BUtility.makeUrl(mBrwView.getCurrentUrl(), src),
                                mBrwView.getCurrentWidget().m_widgetPath,
                                mBrwView.getCurrentWidget().m_wgtType);
                        obj.put("src", srcPath);
                        if (obj.has("thumb")) {
                            String thumb = obj.getString("thumb");
                            String thumbPath = BUtility.makeRealPath(
                                    BUtility.makeUrl(mBrwView.getCurrentUrl(),
                                            thumb),
                                    mBrwView.getCurrentWidget().m_widgetPath,
                                    mBrwView.getCurrentWidget().m_wgtType);
                            obj.put("thumb", thumbPath);
                        }
                    }
                }
                config.setDataArray(data);
            }
            if (jsonObject.has("displayActionButton")) {
                boolean isDisplayActionButton = jsonObject
                        .getBoolean("displayActionButton");
                config.setIsDisplayActionButton(isDisplayActionButton);
            }
            if (jsonObject.has("enableGrid")) {
                boolean enableGrid = jsonObject.getBoolean("enableGrid");
                config.setEnableGrid(enableGrid);
            }
            if (jsonObject.has("startOnGrid")) {
                boolean isStartOnGrid = jsonObject.getBoolean("startOnGrid");
                config.setIsStartOnGrid(isStartOnGrid);
                if (!config.isEnableGrid() && isStartOnGrid) {
                    Toast.makeText(context,
                            "startOnGrid为true时，enableGrid不能为false",
                            Toast.LENGTH_SHORT).show();
                }
            }
            // Android不支持
            // boolean isDisplayNavArrows =
            // jsonObject.getBoolean("displayNavArrows");

            if (jsonObject.has("startIndex")) {
                int startIndex = jsonObject.getInt("startIndex");
                if (startIndex < 0) {
                    startIndex = 0;
                }
                config.setStartIndex(startIndex);
            }
            if (jsonObject.has(Constants.UI_STYLE)) {
                config.setUIStyle(jsonObject.optInt(Constants.UI_STYLE));
            }
            config.setviewFramePicPreview(getViewFrameVO(jsonObject));
            if (jsonObject.has(Constants.GRID_VIEW_BACKGROUND)) {
                config.setViewGridBackground(Color.parseColor(
                        jsonObject.optString(Constants.GRID_VIEW_BACKGROUND)));
            }
            if (jsonObject.has(Constants.GRID_BROWSER_TITLE)) {
                config.setGridBrowserTitle(
                        jsonObject.optString(Constants.GRID_BROWSER_TITLE));
            }
            config.setIsOpenBrowser(true);
            View imagePreviewView = null;
            String viewTag = "";
            if (config.isStartOnGrid()) {
                viewTag = PictureGridActivity.TAG;
                imagePreviewView = new PictureGridActivity(context, this,
                        "", Constants.REQUEST_IMAGE_BROWSER);
            } else {
                viewTag = ImagePreviewActivity.TAG;
                imagePreviewView = new ImagePreviewActivity(context, this, "",
                        0, Constants.REQUEST_IMAGE_BROWSER);
            }
            addViewToWebView(imagePreviewView, viewTag);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(context, "JSON解析错误", Toast.LENGTH_SHORT).show();
        }
    }

    private ViewFrameVO getViewFrameVO(JSONObject jsonObject) {
        ViewFrameVO viewFrameVO = null;
        if (jsonObject.has(Constants.VIEW_FRAME_PIC_PREVIEW)) {
            viewFrameVO = DataParser.viewFrameVOParser(
                    jsonObject.optString(Constants.VIEW_FRAME_PIC_PREVIEW));
        }
        if (null == viewFrameVO) {
            WindowManager manager = ((Activity) mContext).getWindowManager();
            DisplayMetrics outMetrics = new DisplayMetrics();
            manager.getDefaultDisplay().getMetrics(outMetrics);
            viewFrameVO = new ViewFrameVO();
            viewFrameVO.x = 0;
            viewFrameVO.y = 0;
            viewFrameVO.width = outMetrics.widthPixels;
            viewFrameVO.height = outMetrics.heightPixels;
        }
        return viewFrameVO;
    }

    public void openCropper(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        String json = params[0];
        String src = "";
        String srcPath = "";
        try {
            JSONObject jsonObject = new JSONObject(json);
            if (!jsonObject.has("src")
                    || TextUtils.isEmpty(jsonObject.getString("src"))) {
                Toast.makeText(context, "src不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            src = jsonObject.getString("src");
            srcPath = BUtility.makeRealPath(
                    BUtility.makeUrl(mBrwView.getCurrentUrl(), src),
                    mBrwView.getCurrentWidget().m_widgetPath,
                    mBrwView.getCurrentWidget().m_wgtType);
            if (jsonObject.has("quality")) {
                double qualityParam = jsonObject.getDouble("quality");
                if (qualityParam < 0 || qualityParam > 1) {
                    Toast.makeText(context, "quality 只能在0-1之间",
                            Toast.LENGTH_SHORT).show();
                } else {
                    cropQuality = qualityParam;
                }
            }
            if (jsonObject.has("usePng")) {
                cropUsePng = jsonObject.getBoolean("usePng");
            }
            if (jsonObject.has("mode")) {
                int i = jsonObject.getInt("mode");
                if (3 == i) {
                    cropMode = i;
                } else {
                    cropMode = 1;
                }
            }
        } catch (JSONException e) {
            Log.i(TAG, e.getMessage());
            Toast.makeText(context, "JSON解析错误", Toast.LENGTH_SHORT).show();
        }
        File file;
        // 先将assets文件写入到临时文件夹中
        if (src.startsWith(BUtility.F_Widget_RES_SCHEMA)) {
            String fileName = ".png";
            if (!src.endsWith("PNG") && !src.endsWith("png")) {
                fileName = ".jpg";
            }
            // 为res对应的文件生成一个临时文件到系统中
            File destFile = new File(Environment.getExternalStorageDirectory(),
                    File.separator + UEXImageUtil.TEMP_PATH + File.separator
                            + "crop_res_temp" + fileName);
            try {
                destFile.deleteOnExit();
                destFile.createNewFile();
            } catch (IOException e) {
                Toast.makeText(context, FILE_SYSTEM_ERROR, Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            if (srcPath.startsWith("/data")) {
                CommonUtil.copyFile(new File(srcPath), destFile);
                file = destFile;
            } else if (CommonUtil.saveFileFromAssetsToSystem(context, srcPath,
                    destFile)) {
                file = destFile;
            } else {
                Toast.makeText(context, FILE_SYSTEM_ERROR, Toast.LENGTH_SHORT)
                        .show();
                return;
            }
        } else {
            file = new File(srcPath);
        }
        updateGallery(file.getAbsolutePath());
        performCrop(file);
    }

    private void performCrop(File imageFile) {
        try {

            String fileName = null;
            Long time = new Date().getTime();
            if (cropUsePng) {
                fileName = "crop_temp_" + time + ".png";
            } else {
                fileName = "crop_temp_" + time + ".jpg";
            }

            cropOutput = new File(Environment.getExternalStorageDirectory(),
                    File.separator + UEXImageUtil.TEMP_PATH + File.separator
                            + fileName);
            cropOutput.createNewFile();
            Uri destination = Uri.fromFile(cropOutput);
            registerActivityResult();
            Crop.of(Uri.fromFile(imageFile), destination, cropQuality,
                    cropUsePng).asSquare().start((Activity) mContext);
        } catch (Exception exception) {
            Toast.makeText(context, NOT_SUPPORT_CROP, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private void updateGallery(String filename) {
        MediaScannerConnection.scanFile(context, new String[] { filename },
                null, new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }

    public void addViewToWebView(View view, String tag) {
        ViewFrameVO viewFrameVO = EUEXImageConfig.getInstance()
                .getviewFramePicPreview();
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                viewFrameVO.width, viewFrameVO.height);
        lp.leftMargin = viewFrameVO.x;
        lp.topMargin = viewFrameVO.y;
        if (addToWebViewsMap.get(tag) != null) {
            removeViewFromCurWindow(tag);
        }
        addViewToCurrentWindow(view, lp);
        addToWebViewsMap.put(tag, view);
    }

    private void removeViewFromCurWindow(String viewTag) {
        View removeView = addToWebViewsMap.get(viewTag);
        if (removeView != null) {
            removeViewFromCurrentWindow(removeView);
            removeView.destroyDrawingCache();
            addToWebViewsMap.remove(viewTag);
        }
    }

    public static void onActivityResume(Context context) {
        Set<String> tagList = addToWebViewsMap.keySet();
        for (String tag : tagList) {
            if (!TextUtils.isEmpty(tag)) {
                ((ImageBaseView) addToWebViewsMap.get(tag)).onResume();
            }
        }
    }

    public void removeViewFromCurWindow(String viewTag, int requestCode,
            int resultCode) {
        removeViewFromCurWindow(viewTag);
        switch (requestCode) {
        case Constants.REQUEST_IMAGE_PICKER:
            switch (resultCode) {
            case Activity.RESULT_OK:

                break;
            case Constants.OPERATION_CANCELLED:

                break;

            default:
                break;
            }
            break;
        case Constants.REQUEST_IMAGE_BROWSER:
            if (Activity.RESULT_OK == resultCode) {
                callBackPluginJs(JsConst.CALLBACK_ON_BROWSER_CLOSED,
                        "pic browser closed");
            }
            break;
        default:
            break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 裁剪图片
        if (requestCode == Constants.REQUEST_CROP) {
            cropCallBack(resultCode);
        }
        // 选择图片
        if (requestCode == Constants.REQUEST_IMAGE_PICKER) {
            if (resultCode == Activity.RESULT_OK) {
                JSONObject jsonObject = uexImageUtil.getChoosedPicInfo(context);
                callBackPluginJs(JsConst.CALLBACK_ON_PICKER_CLOSED,
                        jsonObject.toString());
            } else if (resultCode == Constants.OPERATION_CANCELLED) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("isCancelled", true);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                callBackPluginJs(JsConst.CALLBACK_ON_PICKER_CLOSED,
                        jsonObject.toString());
            }
            uexImageUtil.resetData();
        }
        // 浏览图片
        if (requestCode == Constants.REQUEST_IMAGE_BROWSER) {
            callBackPluginJs(JsConst.CALLBACK_ON_BROWSER_CLOSED,
                    "pic browser closed");
        }
    }

    private void cropCallBack(int resultCode) {
        // 如果是用户取消，则删除这个临时文件
        if (cropOutput.length() == 0) {
            cropOutput.delete();
        }
        updateGallery(cropOutput.getAbsolutePath());
        JSONObject result = new JSONObject();
        try {
            switch (Crop.cropStatus) {
            case 1:
                result.put("isCancelled", false);
                result.put("data", cropOutput.getAbsolutePath());
                break;
            case 2:
                result.put("isCancelled", false);
                result.put("data", "系统错误");
                break;
            case 3:
                result.put("isCancelled", true);
                break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        callBackPluginJs(JsConst.CALLBACK_ON_CROPPER_CLOSED, result.toString());
    }

    public void saveToPhotoAlbum(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        String json = params[0];

        // 回调的结果
        JSONObject resultObject = new JSONObject();
        try {
            JSONObject jsonObject = new JSONObject(json);
            if (!jsonObject.has("localPath")
                    || TextUtils.isEmpty(jsonObject.getString("localPath"))) {
                Toast.makeText(context, "localPath不能为空", Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            if (jsonObject.has("extraInfo")) {
                resultObject.put("extraInfo",
                        jsonObject.getString("extraInfo"));
            }
            String path = jsonObject.getString("localPath");
            String realPath = BUtility.makeRealPath(
                    BUtility.makeUrl(mBrwView.getCurrentUrl(), path),
                    mBrwView.getCurrentWidget().m_widgetPath,
                    mBrwView.getCurrentWidget().m_wgtType);
            // 如果传的是res,则会复制一份到相册
            if (path.startsWith(BUtility.F_Widget_RES_SCHEMA)) {
                // 获取文件名
                String fileName = path.replace(BUtility.F_Widget_RES_SCHEMA,
                        "");
                String dcimPath = Environment.getExternalStorageDirectory()
                        + File.separator + Environment.DIRECTORY_DCIM
                        + File.separator;
                File file = new File(dcimPath, fileName);
                if (file.exists()) {
                    resultObject.put("isSuccess", false);
                    resultObject.put("errorStr", SAME_FILE_IN_DCIM);
                    callBackPluginJs(JsConst.CALLBACK_SAVE_TO_PHOTO_ALBUM,
                            resultObject.toString());
                    return;
                }
                file.createNewFile();
                if (realPath.startsWith("/data")) {
                    CommonUtil.copyFile(new File(realPath), file);
                    resultObject.put("isSuccess", true);
                    updateGallery(file.getAbsolutePath());
                } else if (CommonUtil.saveFileFromAssetsToSystem(context,
                        realPath, file)) {
                    resultObject.put("isSuccess", true);
                    updateGallery(file.getAbsolutePath());
                } else {
                    resultObject.put("isSuccess", false);
                    resultObject.put("errorStr", FILE_SYSTEM_ERROR);
                }
            } else {// 如果傳的是別的路徑，也復制一份吧。
                File fromFile = new File(realPath);
                String fileName = fromFile.getName();

                String dcimPath = Environment.getExternalStorageDirectory()
                        + File.separator + Environment.DIRECTORY_DCIM
                        + File.separator;
                File destFile = new File(dcimPath, fileName);
                if (destFile.exists()) {
                    resultObject.put("isSuccess", false);
                    resultObject.put("errorStr", SAME_FILE_IN_DCIM);
                    callBackPluginJs(JsConst.CALLBACK_SAVE_TO_PHOTO_ALBUM,
                            resultObject.toString());
                    return;
                }

                if (CommonUtil.copyFile(new File(realPath), destFile)) {
                    resultObject.put("isSuccess", true);
                    updateGallery(destFile.getAbsolutePath());
                } else {
                    resultObject.put("isSuccess", false);
                    resultObject.put("errorStr", FILE_SYSTEM_ERROR);
                }
            }
            callBackPluginJs(JsConst.CALLBACK_SAVE_TO_PHOTO_ALBUM,
                    resultObject.toString());
        } catch (JSONException e) {
            Log.i(TAG, e.getMessage());
            try {
                resultObject.put("isSuccess", false);
                resultObject.put("errorStr", JSON_FORMAT_ERROR);
            } catch (JSONException e2) {
                Log.i(TAG, e2.getMessage());
            }
            callBackPluginJs(JsConst.CALLBACK_SAVE_TO_PHOTO_ALBUM,
                    resultObject.toString());
        } catch (IOException e) {
            Log.i(TAG, e.getMessage());
            try {
                resultObject.put("isSuccess", false);
                resultObject.put("errorStr", FILE_SYSTEM_ERROR);
            } catch (JSONException e2) {
                Log.i(TAG, e2.getMessage());
            }
            callBackPluginJs(JsConst.CALLBACK_SAVE_TO_PHOTO_ALBUM,
                    resultObject.toString());
        }
    }

    public void clearOutputImages(String[] params) {
        JSONObject jsonResult = new JSONObject();
        File directory = new File(Environment.getExternalStorageDirectory(),
                File.separator + UEXImageUtil.TEMP_PATH);
        for (File file : directory.listFiles()) {
            file.delete();
        }
        try {
            jsonResult.put("status", "ok");
        } catch (JSONException e) {
            Log.i(TAG, e.getMessage());
        }
        callBackPluginJs(JsConst.CALLBACK_CLEAR_OUTPUT_IMAGES,
                jsonResult.toString());
    }

    public void onImageLongClick() {
        callBackPluginJs(JsConst.CALLBACK_ON_IAMGE_LONG_CLICKED, "");
    }

    private void callBackPluginJs(String methodName, String jsonData) {
        String js = SCRIPT_HEADER + "if(" + methodName + "){" + methodName
                + "('" + jsonData + "');}";
        onCallback(js);
    }

}
