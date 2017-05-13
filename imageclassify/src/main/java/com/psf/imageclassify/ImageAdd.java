package com.psf.imageclassify;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;


/**
 * users can add images to the image collection
 * Created by psf on 2017/5/13.
 */

public class ImageAdd {
    static final String imageInfoPath = Environment.getExternalStorageDirectory()+"/image/imageInfo.csv";
    static final String hashCodePath = Environment.getExternalStorageDirectory()+"/image/hashcode.hc";

    /**
     * 添加图片到图片库，包含其文件路径，文件top20分类信息，1536位哈希码
     * @param result 包含神经网络处理后的信息 top20分类，1536位哈希码信息
     * @param imagePath 文件的路径
     * @return
     */
    static boolean addImageInfo(ImageNet.NetResult result, String imagePath){
        try(FileWriter mfw = new FileWriter(imageInfoPath, true); FileOutputStream fos = new FileOutputStream(hashCodePath, true)){
            mfw.write(imagePath);
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < result.topK.length; i++){
                sb.append(result.topK[i]);
                sb.append(',');
            }
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
}
