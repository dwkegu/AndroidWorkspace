package com.psf.imageclassify;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.zip.Inflater;

/**
 * Created by psf on 2017/5/10.
 *
 */

public class GridAdapter extends BaseAdapter {
    String[] imageFiles =null;
    String imagePath;
    boolean[] changed;
    Bitmap[] bitmaps;
    int[] distance = null;
    GridAdapter(){
        imagePath = Environment.getExternalStorageDirectory().getPath()+"/imageRetrieval/image/";
    }
    @Override
    public int getCount() {
        if(imageFiles==null){
            return 0;
        }
        return imageFiles.length;
    }

    @Override
    public Object getItem(int position) {
        return imageFiles[position];
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(imageFiles==null) return null;
        if(convertView==null||((PlaceHolder)(convertView.getTag())).position!=position){
            if(convertView==null){
                Log.v("GridAdapter:", "convertView is null:"+String.valueOf(position));
                LayoutInflater minflater = LayoutInflater.from(parent.getContext());
                convertView = minflater.inflate(R.layout.grid_item_view, parent, false);
                PlaceHolder mHolder = new PlaceHolder();
                mHolder.imageView = (ImageView) convertView.findViewById(R.id.grid_image);
                mHolder.textView = (TextView) convertView.findViewById(R.id.item_distance);
                if(bitmaps[position]==null){
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds=true;
                    BitmapFactory.decodeFile(imageFiles[position], options);
                    int sampleRatex = options.outWidth/200;
                    int sampleRatey = options.outHeight/200;
                    options.inSampleSize= sampleRatex>sampleRatey? sampleRatex:sampleRatey;
                    options.inJustDecodeBounds=false;
                    bitmaps[position] = BitmapFactory.decodeFile(imageFiles[position], options);
                }
                mHolder.imageView.setImageBitmap(bitmaps[position]);
                mHolder.textView.setText(String.valueOf(distance[position]));
                mHolder.position = position;
                convertView.setTag(mHolder);
            }else {
                Log.v("GridAdapter:", "position isn't correct:"+String.valueOf(position));
                PlaceHolder mHolder = (PlaceHolder)convertView.getTag();
                if(bitmaps[position]==null){
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds=true;
                    BitmapFactory.decodeFile(imageFiles[position], options);
                    int sampleRatex = options.outWidth/200;
                    int sampleRatey = options.outHeight/200;
                    options.inSampleSize= sampleRatex>sampleRatey? sampleRatex:sampleRatey;
                    options.inJustDecodeBounds=false;
                    bitmaps[position] = BitmapFactory.decodeFile(imageFiles[position], options);
                }
                mHolder.imageView.setImageBitmap(bitmaps[position]);
                mHolder.textView.setText(String.valueOf(distance[position]));
                mHolder.position = position;
            }
        }else {
            if(changed[position]){
                Log.v("GridAdapter:", String.valueOf(position));
                PlaceHolder mHolder = (PlaceHolder)convertView.getTag();
                if(bitmaps[position]==null){
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds=true;
                    BitmapFactory.decodeFile(imageFiles[position], options);
                    int sampleRatex = options.outWidth/200;
                    int sampleRatey = options.outHeight/200;
                    options.inSampleSize= sampleRatex>sampleRatey? sampleRatex:sampleRatey;
                    options.inJustDecodeBounds=false;
                    bitmaps[position] = BitmapFactory.decodeFile(imageFiles[position], options);
                }
                mHolder.imageView.setImageBitmap(bitmaps[position]);
                mHolder.textView.setText(String.valueOf(distance[position]));
                mHolder.position = position;
                changed[position] = false;
            }
        }

        return convertView;

    }
    public void setData(String[] imageFiles, int[] value){

        this.imageFiles = imageFiles;
        changed = new boolean[this.imageFiles.length];
        for(int i=0;i<this.imageFiles.length;i++){
            changed[i] = true;
        }
        bitmaps=new Bitmap[imageFiles.length];
        distance = value;
    }
    private class PlaceHolder{
        ImageView imageView;
        TextView textView;
        int position;
    }
}
