/**
 * Created by pentarex on 26.01.18.
 */

package org.pentarex.rngallerymanager;

import android.database.Cursor;
import android.os.Build;
import android.provider.MediaStore;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;

import java.util.ArrayList;
import java.util.List;

public class RNGalleryManagerModule extends ReactContextBaseJavaModule {

    public static final String RNGALLERY_MANAGER = "RNGalleryManager";
    private static ReactApplicationContext reactContext = null;


    public RNGalleryManagerModule(ReactApplicationContext context) {
        super(context);
        reactContext = context;
    }

    @Override
    public String getName() {
        return RNGALLERY_MANAGER;
    }


    @ReactMethod
    public void getAssets(final ReadableMap params, final Promise promise) {
        if (isJellyBeanOrLater()) {
            promise.reject(new Exception("Version of Android must be > JellyBean"));
            return;
        }

        String requestedType = "all";
        if (params.hasKey("type")) {
            requestedType = params.getString("type");
        }

        Integer limit = 10;
        if (params.hasKey("limit")) {
            limit = params.getInt("limit");
        }
        Integer startFrom = 0;
        if (params.hasKey("startFrom")) {
            startFrom = params.getInt("startFrom");
        }
        String albumName = null;
        if (params.hasKey("albumName")) {
            albumName = params.getString("albumName");
        }

        WritableMap response = new WritableNativeMap();

        Cursor gallery = null;
        try {
            gallery = GalleryCursorManager.getAssetCursor(requestedType, albumName, reactContext);
            WritableArray assets = new WritableNativeArray();

            if(gallery.getCount() <= startFrom ) {
                promise.reject("gallery index out of bound", "");
                return;
            } else {
                response.putInt("totalAssets", gallery.getCount());
                boolean hasMore = gallery.getCount() > startFrom + limit;
                response.putBoolean("hasMore", hasMore);
                if(hasMore) {
                    response.putInt("next", startFrom + limit);
                } else {
                    response.putInt("next", gallery.getCount());
                }
                gallery.moveToPosition(startFrom);
            }

            do {
                WritableMap asset = getAsset(gallery);
                assets.pushMap(asset);
                if (gallery.getPosition() == (startFrom + limit) - 1) break;
            } while (gallery.moveToNext());

            response.putArray("assets", assets);

            promise.resolve(response);

        } catch (SecurityException ex) {
            System.err.println(ex);
        } finally {
            if (gallery != null) gallery.close();
        }
    }


    @ReactMethod
    public void getAlbums(final Promise promise) {
        if (isJellyBeanOrLater()) {
            promise.reject(new Exception("Version of Android must be > JellyBean"));
            return;
        }

        WritableMap response = new WritableNativeMap();


        Cursor gallery = null;
        try {
            if(isQOrLater()){
                gallery = GalleryCursorManager.getQAlbumCursor(reactContext);
            } else {
                gallery = GalleryCursorManager.getAlbumCursor(reactContext);
            }
            WritableArray albums = new WritableNativeArray();

            gallery.moveToFirst();
            List<String> albumList = new ArrayList<String>();
            int total = 0;

            do {
                WritableMap album = getAlbum(gallery);
                if (total > 0 && albumList.contains(album.getString("title"))) {
                } else {
                albumList.add(album.getString("title"));
                    albums.pushMap(album);
                    total += 1;
                }
            } while (gallery.moveToNext());

            response.putInt("totalAlbums", albumList.size());
            response.putArray("albums", albums);

            promise.resolve(response);

        } catch (SecurityException ex) {
            System.err.println(ex);
        } finally {
            if (gallery != null) gallery.close();
        }

    }

    private WritableMap getAsset(Cursor gallery) {
        WritableMap asset = new WritableNativeMap();
        int mediaType = gallery.getInt(gallery.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE));
        String mimeType = gallery.getString(gallery.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE));
        String creationDate = gallery.getString(gallery.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED));
        String fileName = gallery.getString(gallery.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME));
        Double height = gallery.getDouble(gallery.getColumnIndex(MediaStore.Files.FileColumns.HEIGHT));
        Double width = gallery.getDouble(gallery.getColumnIndex(MediaStore.Files.FileColumns.WIDTH));
        String uri = "file://" + gallery.getString(gallery.getColumnIndex(MediaStore.Files.FileColumns.DATA));
        Double id = gallery.getDouble(gallery.getColumnIndex(MediaStore.Files.FileColumns._ID));
        String orientation = gallery.getString(gallery.getColumnIndex(MediaStore.Images.Media.ORIENTATION));


        asset.putString("mimeType", mimeType);
        asset.putString("creationDate", creationDate);
        asset.putString("orientation", orientation);
        asset.putDouble("height", height);
        asset.putDouble("width", width);
        asset.putString("filename", fileName);
        asset.putDouble("id", id);
        asset.putString("uri", uri);

        if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
            asset.putDouble("duration", 0);
            asset.putString("type", "image");

        } else if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
            Double duration = gallery.getDouble(gallery.getColumnIndex(MediaStore.Video.VideoColumns.DURATION));
            asset.putDouble("duration", duration / 1000);
            asset.putString("type", "video");
        }
        return asset;
    }

    private WritableMap getAlbum(Cursor gallery) {
        WritableMap album = new WritableNativeMap();
        String albumName = gallery.getString(gallery.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME));
        album.putString("title", albumName);
        return album;
    }


    private Boolean isJellyBeanOrLater() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN;
    }

    private Boolean isQOrLater() {
        return Build.VERSION.SDK_INT >= 29;
    }
}
