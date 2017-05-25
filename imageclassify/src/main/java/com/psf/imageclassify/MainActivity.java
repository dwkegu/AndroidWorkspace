package com.psf.imageclassify;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.BoolRes;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.psf.imageclassify.Loader.ImageLoader;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    static final int WRITE_EX = 100;
    static final int READ_EX = 101;
    static final int IMAGEREQUESTCODE = 200;
    static final int ADDIMAGEREQUESTCODE = 201;
    static final int HASHCODERESULT = 1;
    static final int IMAGEINFORESULT = 2;
    public static final int READIMAGERESULT=3;
    final String TAG="MAINACTIVITY:";
    String imageNote = null;
    GridView mgrid=null;
    GridAdapter madapter=null;
    ImageNet net;
    TextView mtv;
    ImageView searchView;
    ProgressDialog mpd;
    AlertDialog.Builder mbuilder;
    FloatingActionButton fab;
    String[] notes;
    int[] distances;
    public Handler mhander = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.v(TAG, "handler received");
            Bundle data = msg.getData();
            String imagePath = data.getString(ImageNet.IMAGEPATH);
            switch (msg.what){
                case HASHCODERESULT:
                    Log.v(TAG, "HASHCODERESULT");
                    String[] res = data.getStringArray(ImageNet.SEARCH_RESULT);
                    String[] notes = data.getStringArray(ImageNet.SEARCH_RESULTNOTES);
                    int[] distance = data.getIntArray(ImageNet.IMAGEDISTANCE);
                    if (res==null||notes==null||distance==null){
                        Log.v(TAG, "search result is null");
                        mpd.dismiss();
                        return true;
                    }
                    Log.v(TAG, "here1");
                    for(String item:res){
                        Log.v(TAG, item);
                    }
                    Log.v(TAG, "here2");
                    searchView.setImageBitmap(BitmapFactory.decodeFile(imagePath));
                    MainActivity.this.notes = notes;
                    MainActivity.this.distances = distance;
                    ImageLoader myLoader = new ImageLoader(res, mhander);
                    myLoader.start();
                    break;
                case IMAGEINFORESULT:
                    Log.v(TAG, "IMAGEINFORESULT");
                    NetResult info = (NetResult)data.getSerializable(ImageNet.NET_RESULT);
                    if(info==null||imagePath==null){
                        mpd.dismiss();
                        return true;
                    }
                    ImageAdd.addImageInfo(info,imagePath, imageNote);
                    mpd.dismiss();
                    break;
                case READIMAGERESULT:
                    Bitmap[] bitmaps= (Bitmap[])data.getParcelableArray(ImageLoader.LOADEDIMAGES);
                    if(bitmaps==null) break;
                    madapter.setData(bitmaps,MainActivity.this.notes, MainActivity.this.distances);
                    madapter.notifyDataSetChanged();
                    mpd.dismiss();
                    break;
                default:
                    break;
            }

            return true;
        }
    });
    private class InitThread extends Thread{
        ProgressDialog mpd;
        InitThread(ProgressDialog mpd, Handler handler){
            super();
            this.mpd = mpd;
        }
        @Override
        public void run() {
            net = new ImageNet(getAssets());
            net.setHandler(mhander);
            mpd.dismiss();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mgrid=(GridView) findViewById(R.id.retrieval_res);
        madapter = new GridAdapter();
        mgrid.setAdapter(madapter);
        checkPermission();
        mtv = (TextView)findViewById(R.id.text_main);
        searchView = (ImageView)findViewById(R.id.query_image);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mintent = new Intent(Intent.ACTION_PICK);
                mintent.setType("image/*");
                startActivityForResult(mintent, IMAGEREQUESTCODE);
//                BitmapFactory.Options mo = new BitmapFactory.Options();
//                mo.inJustDecodeBounds=false;
//                mo.outHeight=299;
//                mo.outWidth=299;
//                Snackbar.make(view, "已经", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
            }
        });
        mbuilder = new AlertDialog.Builder(this, R.style.MyAlertDialogStyle);
        mpd = new ProgressDialog(this, 0);
        mpd.setMessage("初始化系统中......");
        mpd.setCancelable(false);
        mpd.show();
        InitThread initThread = new InitThread(mpd, mhander);
        initThread.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        Toast.makeText(getApplicationContext(), "getCode"+resultCode, Toast.LENGTH_LONG).show();
        switch (requestCode){
            case IMAGEREQUESTCODE:
                if(resultCode!=RESULT_OK) return;
//                Toast.makeText(getApplicationContext(),"hello:"+ data.getDataString(), Toast.LENGTH_LONG).show();
                Uri imageUri = data.getData();
                try(InputStream min1 = getContentResolver().openInputStream(imageUri)){
                    Log.v(TAG, imageUri.toString());
                    Bitmap bitmap = BitmapFactory.decodeStream(min1);
//                        String exPath = Environment.getExternalStorageDirectory().getPath();
//                        BitmapFactory.decodeFile(exPath+"/image/test.jpg");
//                        Bitmap bitmap = BitmapFactory.decodeFile(exPath+"/image/test.jpg");
                    mpd.setMessage("正在搜索相似图像......");
                    mpd.show();
                    net.run(bitmap, ImageAdd.getRealPathFromURI(getApplicationContext(), imageUri), true);
                    Log.v(TAG, "finish");
                }catch (IOException e){
                    e.printStackTrace();
                }
                break;
            case ADDIMAGEREQUESTCODE:
                if(resultCode!=RESULT_OK) return;
                Uri addImageUri = data.getData();
                try(InputStream min2 = getContentResolver().openInputStream(addImageUri)){
                    Bitmap bitmap = BitmapFactory.decodeStream(min2);
                    if(mbuilder==null) {
                        mbuilder = new AlertDialog.Builder(getApplicationContext());
                    }
                    showAddImageDialog(mbuilder, ImageAdd.getRealPathFromURI(getApplicationContext(), addImageUri),
                            bitmap);
                }catch (IOException e){
                    e.printStackTrace();
                }
                break;
        }
    }
    /**
     * 检查权限
     */
    public void checkPermission(){
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if(permissionCheck != PackageManager.PERMISSION_GRANTED){
            if(!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EX);
            }
        }
        permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(permissionCheck != PackageManager.PERMISSION_GRANTED){
            if(!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, READ_EX);
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case READ_EX:
            case WRITE_EX:
                break;
            default:
                System.exit(-1);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_add_image) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, ADDIMAGEREQUESTCODE);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private void showAddImageDialog(AlertDialog.Builder builder, final String imagePath,
                                    final Bitmap imageBitmap){
        builder.setTitle(R.string.add_image_dialog_title);
        LinearLayout mview = (LinearLayout)getLayoutInflater().inflate(R.layout.add_image_dialog, null);
        builder.setView(mview);
        ImageView selectedImage = (ImageView)mview.findViewById(R.id.selected_image);
        final EditText selectedImageNote = (EditText)mview.findViewById(R.id.add_image_info);
        selectedImage.setImageBitmap(imageBitmap);
        builder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                imageNote = selectedImageNote.getText().toString().trim();
                if(imageNote.equals("")){
                    selectedImageNote.setHintTextColor(getResources().getColor(R.color.colorPrimary));
                    return;
                }
                imageNote = imageNote.replaceAll(",","、");
                if(mpd==null){
                    mpd = new ProgressDialog(getApplicationContext(), 0);
                }
                mpd.setMessage("添加图像......");
                dialog.dismiss();
                mpd.show();
                net.run(imageBitmap, imagePath,  false);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog mdialog = builder.create();
        mdialog.setCancelable(true);
        mdialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(net!=null){
            net.close();
        }
    }
}
