package com.psf.imageclassify;

/**
 *图片错误
 * Created by psf on 2017/5/9.
 */

public class ImageSizeException extends Exception {
    public ImageSizeException(String message) {
        super(message);
    }

    @Override
    public void printStackTrace() {
        super.printStackTrace();
    }
}
