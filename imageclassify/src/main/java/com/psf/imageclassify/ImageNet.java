package com.psf.imageclassify;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.util.SortedList;
import android.util.Log;
import org.tensorflow.Operation;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by psf on 2017/5/9.
 * 进行图片的识别，搜索
 */

public class ImageNet {
    static boolean hasInit = false;
    final String TAG ="IMAGENET:";
    static final String SEARCH_RESULT = "topk";
    static final String SEARCH_RESULTNOTES = "topNotes";
    static final String NET_RESULT = "NetResult";
    static final String IMAGEPATH = "imagePath";
    static final String IMAGEDISTANCE = "imageDistance";
    private final String inputName = "input";
    //InceptionV4/Logits/Logits/BiasAdd
    private final String[] outputNames = {"InceptionV4/Logits/PreLogitsFlatten/Reshape","InceptionV4/Logits/Logits/BiasAdd","InceptionV4/Logits/top_K"};
    private final String modelFilename = "file:///android_asset/InceptionV4cpp.pb";
    private final int inputSize = 299;
    private final int imageMean = 128;
    private final float imagestd = 1f;
    private final int hashCodeNum = 1536;
    private final int classNum = 1001;
    static int topKnum;
    private Operation operation[];
    private float[] fValue;
    private TensorFlowInferenceInterface mInference;
    private Handler mhandler;
    private AssetManager asm;
    ImageNet(AssetManager assetManager){
        asm = assetManager;
        operation = new Operation[3];
        mInference = new TensorFlowInferenceInterface(assetManager,
                modelFilename);
        Operation ops = mInference.graph().operation(inputName);
        Log.v(TAG, "preLogits:"+ops.numOutputs()+"  "+ops.output(0).shape().toString() +  ops.output(0).shape().size(1));
        for(int i=0;i<outputNames.length;i++){
            operation[i] = mInference.graph().operation(outputNames[i]);

            Log.v(TAG, "topk:"+ops.numOutputs()+"  "+operation[i].output(0).shape().toString() + operation[i].output(0).dataType());
        }
        topKnum=(int)operation[2].output(0).shape().size(1);

        hasInit=true;
    }

    /**
     * 运行神经网络，进行图像搜索或者获取图像特征信息，单独的线程，不会阻塞UI
     * @param bitmap 输入的图像
     * @param runForSearch true 表示进行图像搜索，false表示获取图像信息。
     */
    void run(final Bitmap bitmap, final String imagePath, final boolean runForSearch){
        new Thread(new Runnable() {
            @Override
            public void run() {
                //获取图片特征
                Log.v(TAG, imagePath);
                NetResult res = getHashCode(bitmap);
                Message msg = Message.obtain();
                Bundle data = new Bundle();
                if(runForSearch){
                    for(int i=0;i<res.topK.length;i++){
                        Log.v(TAG, String.valueOf(res.topK[i]));
                    }
                    try(InputStream hc = new FileInputStream(ImageAdd.hashCodePathV2);
                        //读取图片库特征文件
                        InputStream imageInfo = new FileInputStream(ImageAdd.imageInfoPathV2)){
                        //读取图片库图片名和top排名
                        InputStreamReader infoReader = new InputStreamReader(imageInfo);
                        BufferedReader bfr = new BufferedReader(infoReader);
                        //图片库中的图片数量
                        int imageNum = hc.available()/192;
                        Log.v(TAG, "hashcode available:" + String.valueOf(imageNum));
                        //保存前10名的相似图片信息
                        SortedPair mpair = new SortedPair(10);
                        //当前比较图片库的图片哈希码信息
                        byte[] temp = new byte[192];
                        byte comRes;
                        //当前比较的图片信息
                        String fileName;
                        String note;
                        //当前比较图片的前20名分类
                        int[] topk = new int[20];
                        //比较在图片库中的每个图片
                        for(int i=0;i<imageNum;i++){
                            //读取一个图片的文件名和分类信息
                            fileName = bfr.readLine();
                            String[] info = fileName.split(",");
                            if(info.length!=22) continue;
                            fileName = info[0];
                            note = info[1];
                            //获取前20名的分类
                            for(int j = 2; j < 22; j++){
                                topk[j-2] = Integer.valueOf(info[j].trim());
                            }
                            if(hc.read(temp)<192){
                                //如果哈希码少于192byte，则发生错误。
                                Log.v(TAG, "读取数据错误");
                                break;
                            }else {
                                //查看当前是否包含前20名的分类
                                boolean check = false;
                                //每个前20的分类提取出来病比较
                                for(int topi=0;topi<topk.length;topi++) {
                                    //检查当前分类是否在检索图片的前20
                                    check = false;
                                    for (int topj = 0; topj < res.topK.length; topj++) {
                                        if (topk[topi] == res.topK[topj]) {
                                            //如果检测到，则需要比较哈希码，跳出
                                            check = true;
                                            break;
                                        }
                                    }
                                    if(check) break;
                                }
                                //如果检测到，则跳出
                                if (check) {
                                    int dist = 0;
                                    // 比较汉明码距
                                    for (int j = 0; j < 192; j++) {
                                        comRes = (byte) (res.hashCode[j] ^ temp[j]);
                                        for (int k = 0; k < 8; k++) {
                                            dist += (comRes & 0x01);
                                            comRes = (byte) (comRes >> 1);
                                        }
                                    }
                                    //将码距和文件名同时排名
                                    // Log.v(TAG, "push");
                                    if(dist<500){
                                        mpair.push(dist, fileName, note);
                                    }
                                }
                            }
                        }
                        Log.v(TAG, "查找结束");
                        msg.what = MainActivity.HASHCODERESULT;
                        String[] similarImages = mpair.getTopK();
                        String[] similarImageNote = mpair.getNotes();
                        int[] topValue = mpair.getTopValue();
                        data.putStringArray(SEARCH_RESULT, similarImages);
                        data.putStringArray(SEARCH_RESULTNOTES, similarImageNote);
                        data.putString(IMAGEPATH, imagePath);
                        data.putIntArray(IMAGEDISTANCE, topValue);
                    }catch (IOException e){
                        Log.v(TAG, "read image error");
                        e.printStackTrace();
                    }
                }else{
                    msg.what = MainActivity.IMAGEINFORESULT;
                    data.putSerializable(NET_RESULT, res);
                    data.putString(IMAGEPATH, imagePath);
                }
                msg.setData(data);
                mhandler.sendMessage(msg);



            }
        }).start();
    }
    private NetResult getHashCode(Bitmap bitmap){
        fValue = imageEvalProcess(bitmap, 0.875f);
        mInference.feed(inputName, fValue, 1, inputSize, inputSize, 3);
        mInference.run(outputNames);
        float[] preLogits = new float[hashCodeNum];
        float[] predicts = new float[classNum];
        float[] topK = new float[topKnum];
        mInference.fetch(outputNames[0], preLogits);
//        for (float prelogit : preLogits) {
//            Log.v(TAG, "Prelogits:" + String.valueOf(prelogit));
//        }
        mInference.fetch(outputNames[1], predicts);
        mInference.fetch(outputNames[2], topK);
        for (float predict : topK) {
            Log.v(TAG, "Prediction:" + String.valueOf(predict));
        }
        NetResult res= new NetResult();
        res.hashCode = getCode(preLogits);
        for(int i=0;i<topK.length;i++){
            int loc = 1;
            while(predicts[loc]!=topK[i]&&loc<classNum){
                loc++;
            }
            res.topK[i]=loc;
        }
        return res;
    }
    void setHandler(Handler handler){
        mhandler = handler;
    }
    void close(){
        mInference.close();
    }

    private class SortedPair{
        int maxLength;
        String[] posi;
        String[] notes;
        int[] value;
        int currentNum;
        SortedPair(int maxLength){
            this.maxLength = maxLength;
            posi = new String[maxLength];
            notes = new String[maxLength];
            value = new int[maxLength];
            currentNum = 0;
        }
        void push(int value, String location, String note){
            Log.v(TAG, "PUSH:" + String.valueOf(value) + location );
            if(currentNum<maxLength){
                if(currentNum==0||value>=this.value[currentNum-1]){
                    posi[currentNum] = location;
                    this.value[currentNum] = value;
                    notes[currentNum] = note;
                }else{
                    for(int i=0;i<currentNum;i++){
                        if(this.value[i]>value){
                            //将value插入
                            for(int j=currentNum; j>i;j--){
                                this.value[j]=this.value[j-1];
                                this.posi[j]=this.posi[j-1];
                                notes[j] = notes[j-1];
                            }
                            this.value[i]=value;
                            this.posi[i]=location;
                            notes[i]=note;
                            break;
                        }
                    }
                }
                currentNum++;
            }else {
                if(value>=this.value[currentNum-1]) return;
                for(int i=0;i<currentNum;i++){
                    if(this.value[i]>value){
                        //将value插入
                        for(int j=currentNum-1; j>i;j--){
                            this.value[j]=this.value[j-1];
                            this.posi[j]=this.posi[j-1];
                            notes[j] = notes[j-1];
                        }
                        this.value[i]=value;
                        this.posi[i]=location;
                        notes[i]=note;
                        break;
                    }
                }

            }
        }

        String[] getTopK() {
//            for(int i = 0; i < posi.length; i++){
//                Log.v(TAG, posi[i]);
//            }
            String[] topk = new String[currentNum];
            for(int i=0;i<currentNum;i++){
                topk[i]=posi[i];
            }
            return topk;
        }
        String[] getNotes(){
            String[] topNotes = new String[currentNum];
            for(int i=0;i<currentNum;i++){
                topNotes[i]=notes[i];
            }
            return topNotes;
        }
        int[] getTopValue(){
            int[] topVlaue = new int[currentNum];
            for(int i=0;i<currentNum;i++){
                topVlaue[i]=value[i];
            }
            return topVlaue;
        }
    }

    private int[] getTopK(float[] predicts){
        int[] min20 = new int[20];
        SortedList<IFPair> msl = new SortedList<>(IFPair.class, new SortedList.Callback<IFPair>() {
            @Override
            public int compare(IFPair o1, IFPair o2) {
                if(o1.value>o2.value) return -1;
                else if(o1.value<o2.value) return 1;
                else return 0;
            }

            @Override
            public void onChanged(int position, int count) {

            }

            @Override
            public boolean areContentsTheSame(IFPair oldItem, IFPair newItem) {
                return oldItem.value == newItem.value;
            }

            @Override
            public boolean areItemsTheSame(IFPair item1, IFPair item2) {
                return item1.posi == item2.posi;
            }

            @Override
            public void onInserted(int position, int count) {

            }

            @Override
            public void onRemoved(int position, int count) {

            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {

            }
        });
        for(int i=1;i<predicts.length;i++){
            //Log.v(TAG,"getTopK"+String.valueOf(i));
            if(msl.size()<20){
                IFPair item = new IFPair(i, predicts[i]);
                //Log.v(TAG, String.valueOf(predicts[i]));
                msl.add(item);
            }else {

                if(msl.get(msl.size()-1).value<predicts[i]){
                    msl.removeItemAt(msl.size()-1);
                    IFPair item = new IFPair(i, predicts[i]);
                    msl.add(item);
                }
            }
        }
        for(int i=0;i<20;i++){
            Log.v(TAG, "sorted value" + String.valueOf(msl.get(i).value) + " posi:"+ String.valueOf(msl.get(i).posi));
            min20[i]=msl.get(i).posi;
            Log.v(TAG, String.valueOf(min20[i]));
        }
        return min20;
    }
    private class IFPair{
        int posi;
        float value;
        IFPair(int posi, float value){
            this.posi = posi;
            this.value = value;
        }
    }
    private byte[] getCode(float[] preLogits){
        byte[] hashcode = new byte[1536/8];
        double sum = 0;
        for(int i=0;i<1536;i++){
            sum+=preLogits[i];
        }
        StringBuilder sb = new StringBuilder(1536);
        float mean = (float)(sum/1536);
        Log.v(TAG, "MEAN:"+ String.valueOf(mean));
        for(int i=0;i<1536;i++){
            if(preLogits[i]>mean){
                sb.append('1');
                hashcode[i/8]=(byte)((hashcode[i/8]<<1)+1);
            }else {
                sb.append('0');
                hashcode[i/8]=(byte)(hashcode[i/8]<<1);
            }
        }

        Log.v(TAG, sb.toString());
        return hashcode;
    }
    private float[] imageEvalProcess(Bitmap bitmap, float centralFraction){
        if(centralFraction>1||centralFraction<=0) return null;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] data = new int[299*299];
        int offsetX = width/(int)(1 /((1.0f-centralFraction)/2.0f));
        int offsetY = height/(int)(1 /((1.0f-centralFraction)/2.0f));
        //保留部分图像
       // bitmap.getPixels(data,0,width,offsetX,offsetY,width-offsetX,height-offsetY);
        Bitmap subBitmap = Bitmap.createBitmap(bitmap, offsetX, offsetY, width-2*offsetX, height-2*offsetY);
        subBitmap = Bitmap.createScaledBitmap(subBitmap,299,299,true);
        subBitmap.getPixels(data,0,299,0,0,299,299);
        float[] result = new float[data.length * 3];
        for(int i=0;i<data.length;i++){
            final int val =data[i];
            result[i * 3] = (((val >> 16) & 0xFF) / 255f - 0.5f) * 2.0f;
            result[i * 3 + 1] = (((val >> 8) & 0xFF) /255f - 0.5f) * 2.0f;
            result[i * 3 + 2] = ((val & 0xFF) /255f- -0.5f) * 2.0f;
        }
        return result;
    }

    /**
     * 因为android 在缩放的时候没有做双线性插值，所以这里补上
     * @param src    原图像
     * @param width     缩放后的宽度
     * @param height    缩放后的高度
     * @return          缩放后的图片
     */
    Bitmap createScaledBitmapBiolinear(Bitmap src, int width, int height){
        if(src==null) return null;
        if(src.getWidth()==width&&src.getHeight()==height) return src;
        Bitmap dst = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        //宽度缩放系数
        double scalew = (double) width/src.getWidth();
        //高度缩放系数
        double scaleh = (double) height/src.getHeight();
        if(scalew>1){
            //宽度放大处理
            int[] point1 = new int[3];
            int[] point2 = new int[3];
            for(int i=0;i<height;i++){
                for(int j=0;j<width;j++){
                    double posi = j/scalew;
                    posi -=(int)posi;
                    final int val1 =src.getPixel(i, (int)posi);
                    final int val2 = src.getPixel(i,(int)posi+1);
                    point1[0] = (val1 >> 16) & 0xFF;
                    point1[1] = (val1 >> 8) & 0xFF;
                    point1[2] = val1 & 0xFF;
                    point2[0] = (val2 >> 16) & 0xFF;
                    point2[1] = (val2 >> 8) & 0xFF;
                    point2[2] = val2 & 0xFF;
                    int setPoint = 0;
                    for(int k =0;k<3;k++){
                        setPoint = (setPoint<<8)+(int)(point1[k]*(1-posi)+point2[k]*posi);
                    }
                    dst.setPixel(i,j,setPoint);
                }
            }
        }else{
            //宽度缩小处理
            int[] point1 = new int[3];
            int[] point2 = new int[3];
            for(int i=0;i<width;i++){
                for(int j=0;j<height;j++){
                    double posi = j/scaleh;
                    posi -=(int)posi;
                    final int val1 =src.getPixel((int)posi,i);
                    final int val2 = src.getPixel((int)posi+1,i);
                    point1[0] = (val1 >> 16) & 0xFF;
                    point1[1] = (val1 >> 8) & 0xFF;
                    point1[2] = val1 & 0xFF;
                    point2[0] = (val2 >> 16) & 0xFF;
                    point2[1] = (val2 >> 8) & 0xFF;
                    point2[2] = val2 & 0xFF;
                    int setPoint = 0;
                    for(int k =0;k<3;k++){
                        setPoint = (setPoint<<8)+(int)(point1[k]*(1-posi)+point2[k]*posi);
                    }
                    dst.setPixel(j,i,setPoint);
                }
            }
        }
        if(scaleh>1){
            //高度放大处理
            int[] point1 = new int[3];
            int[] point2 = new int[3];
            for(int i=0;i<width;i++){
                for(int j=0;j<height;j++){
                    double posi = j/scaleh;
                    posi -=(int)posi;
                    final int val1 =src.getPixel((int)posi,i);
                    final int val2 = src.getPixel((int)posi+1,i);
                    point1[0] = (val1 >> 16) & 0xFF;
                    point1[1] = (val1 >> 8) & 0xFF;
                    point1[2] = val1 & 0xFF;
                    point2[0] = (val2 >> 16) & 0xFF;
                    point2[1] = (val2 >> 8) & 0xFF;
                    point2[2] = val2 & 0xFF;
                    int setPoint = 0;
                    for(int k =0;k<3;k++){
                        setPoint = (setPoint<<8)+(int)(point1[k]*(1-posi)+point2[k]*posi);
                    }
                    dst.setPixel(j,i,setPoint);
                }
            }
        }else {
            //高度缩小处理
        }
        return dst;
    }
}
