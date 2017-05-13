package com.psf.imageclassify;


import java.io.Serializable;

/**
 * save net result
 * Created by psf on 2017/5/13.
 */
 class NetResult implements Serializable {
    byte[] hashCode;
    int[] topK;
    NetResult(){
        topK=new int[ImageNet.topKnum];
    }

}
