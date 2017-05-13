package com.psf.imageclassify;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;


import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;


/**
 * users can add images to the image collection
 * Created by psf on 2017/5/13.
 */

public class ImageAdd {
    static final String imageInfoPath = Environment.getExternalStorageDirectory()+"/imageRetrieval/imageInfo.csv";
    static final String hashCodePath = Environment.getExternalStorageDirectory()+"/imageRetrieval/hashCode.hc";

    /**
     * 添加图片到图片库，包含其文件路径，文件top20分类信息，1536位哈希码
     * @param result 包含神经网络处理后的信息 top20分类，1536位哈希码信息
     * @param imagePath 文件的路径
     * @return
     */
    static boolean addImageInfo(NetResult result, String imagePath){
        try(FileWriter mfw = new FileWriter(imageInfoPath, true); FileOutputStream fos = new FileOutputStream(hashCodePath, true)){
            StringBuilder sb = new StringBuilder(imagePath);
            for(int i = 0; i < result.topK.length; i++){
                sb.append(',');
                sb.append(result.topK[i]);
            }
            sb.append('\n');
            mfw.write(sb.toString());
            fos.write(result.hashCode);
            mfw.flush();
            fos.flush();
        }catch (IOException e){
            e.printStackTrace();
            Log.v("imageAdd", "read imageInfo or hashcode meet error");
            return false;
        }
        return true;
    }

    /**
     * 从Uri得到绝对路径
     * @param context 上下文，用于获取ContentResolver
     * @param contentUri 包含文件路劲的Uri
     * @return 文件的绝对路径
     */
    static String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            if(cursor==null) return null;
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
