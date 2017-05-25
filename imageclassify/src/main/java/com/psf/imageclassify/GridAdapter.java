package com.psf.imageclassify;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.nfc.Tag;
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
    String[] notes =null;
    String imagePath;
    Bitmap[] bitmaps;
    int[] distance = null;
    GridAdapter(){
        imagePath = Environment.getExternalStorageDirectory().getPath()+"/imageRetrieval/image/";
    }
    @Override
    public int getCount() {
        if(bitmaps==null){
            return 0;
        }
        return bitmaps.length;
    }

    @Override
    public Object getItem(int position) {
        return bitmaps[position];
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(bitmaps==null) return null;
        if(convertView==null){
            Log.v("GridAdapter:", "convertView is null:"+String.valueOf(position));
            LayoutInflater minflater = LayoutInflater.from(parent.getContext());
            convertView = minflater.inflate(R.layout.grid_item_view, parent, false);
            PlaceHolder mHolder = new PlaceHolder();
            mHolder.imageView = (ImageView) convertView.findViewById(R.id.grid_image);
            mHolder.textView = (TextView) convertView.findViewById(R.id.item_distance);
            mHolder.imageView.setImageBitmap(bitmaps[position]);
            mHolder.textView.setText(notes[position]+String.valueOf(distance[position]));
            mHolder.position = position;
            convertView.setTag(mHolder);
        }else {
            Log.v("GridAdapter:", String.valueOf(position));
            PlaceHolder mHolder = (PlaceHolder)convertView.getTag();
            mHolder.imageView.setImageBitmap(bitmaps[position]);
            mHolder.textView.setText(notes[position]+String.valueOf(distance[position]));
            mHolder.position = position;
        }

        return convertView;

    }
    public void setData(Bitmap[] imageFiles, String[] notes, int[] value){

        this.bitmaps = imageFiles;
        this.notes = notes;
        distance = value;
    }
    private class PlaceHolder{
        ImageView imageView;
        TextView textView;
        int position;
    }
}
