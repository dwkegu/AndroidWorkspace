package com.psf.imageclassify;

import android.graphics.BitmapFactory;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.util.zip.Inflater;

/**
 * Created by psf on 2017/5/10.
 *
 */

public class GridAdapter extends BaseAdapter {
    String[] imageFiles =null;
    String imagePath;
    boolean[] changed;
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
                LayoutInflater minflater = LayoutInflater.from(parent.getContext());
                convertView = minflater.inflate(R.layout.grid_item_view, parent, false);
                PlaceHolder mHodler = new PlaceHolder();
                mHodler.imageView = (ImageView) convertView.findViewById(R.id.grid_image);
                mHodler.imageView.setImageBitmap(BitmapFactory.decodeFile(imagePath+imageFiles[position]));
                mHodler.position = position;
                convertView.setTag(mHodler);
            }else {
                PlaceHolder mHolder = (PlaceHolder)convertView.getTag();
                mHolder.imageView.setImageBitmap(BitmapFactory.decodeFile(imagePath+imageFiles[position]));
                mHolder.position = position;
            }
        }else {
            if(changed[position]){
                PlaceHolder mHolder = (PlaceHolder)convertView.getTag();
                mHolder.imageView.setImageBitmap(BitmapFactory.decodeFile(imagePath+imageFiles[position]));
                mHolder.position = position;
                changed[position] = false;
            }
        }

        return convertView;

    }
    public void setData(String[] imageFiles){

        this.imageFiles = imageFiles;
        changed = new boolean[this.imageFiles.length];
        for(int i=0;i<this.imageFiles.length;i++){
            changed[i] = true;
        }

    }
    private class PlaceHolder{
        ImageView imageView;
        int position;
    }
}
