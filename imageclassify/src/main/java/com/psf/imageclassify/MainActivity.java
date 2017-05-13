package com.psf.imageclassify;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    static final int WRITE_EX = 100;
    static final int READ_EX = 101;
    static final int IMAGEREQUESTCODE = 200;
    static final int ADDIMAGEREQUESTCODE = 201;
    static final int HASHCODERESULT = 1;
    static final int IMAGEINFORESULT = 2;
    final String TAG="MAINACTIVITY:";
    GridView mgrid=null;
    GridAdapter madapter=null;
    ImageNet net;
    TextView mtv;
    ImageView searchView;
    ProgressDialog mpd;
    public Handler mhander = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.v(TAG, "handler received");
            Bundle data = msg.getData();
            String imagePath = data.getString(ImageNet.IMAGEPATH);
            switch (msg.what){
                case HASHCODERESULT:
                    String[] res = data.getStringArray(ImageNet.SEARCH_RESULT);
                    if (res==null) return true;
                    for(String item:res){
                        Log.v(TAG, item);
                    }
                    searchView.setImageBitmap(BitmapFactory.decodeFile(imagePath));
                    madapter.setData(res);
                    madapter.notifyDataSetChanged();
                    mpd.dismiss();
                    break;
                case IMAGEINFORESULT:
                    NetResult info = (NetResult)data.getSerializable(ImageNet.NET_RESULT);
                    mpd.dismiss();
                    if(info!=null) ImageAdd.addImageInfo(info, imagePath);
                default:
                    break;
            }

            return true;
        }
    });
    class InitThread extends Thread{
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
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
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
        mpd = new ProgressDialog(this, 0);
        mpd.setMessage("初始化系统中......");
        mpd.show();
        InitThread initThread = new InitThread(mpd, mhander);
        initThread.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Toast.makeText(getApplicationContext(), "getCode"+resultCode, Toast.LENGTH_LONG).show();
        switch (requestCode){
            case IMAGEREQUESTCODE:
                Toast.makeText(getApplicationContext(),"hello:"+ data.getDataString(), Toast.LENGTH_LONG).show();
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
                Uri addImageUri = data.getData();
                try(InputStream min2 = getContentResolver().openInputStream(addImageUri)){
                    Bitmap bitmap = BitmapFactory.decodeStream(min2);
                    if(mpd==null){
                        mpd = new ProgressDialog(this, 0);
                    }
                    mpd.setMessage("添加图像......");
                    mpd.show();
                    net.run(bitmap, ImageAdd.getRealPathFromURI(getApplicationContext(), addImageUri), false);
                    Log.v(TAG, "call net add image");
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(net!=null){
            net.close();
        }
    }
}
