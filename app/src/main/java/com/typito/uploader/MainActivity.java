package com.typito.uploader;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.StringBuilderPrinter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    String[] videoFileList;
    private ListView listView;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView= (ListView) findViewById(R.id.listView);
        ArrayList<String> t=getVideoList(MainActivity.this);
        videoFileList=t.toArray(new String[t.size()]);
        listView.setAdapter(new RowAdapter(MainActivity.this, R.layout.row, videoFileList));
    }

    public class RowAdapter extends ArrayAdapter<String> {
        public RowAdapter(Context context, int resource, String[] objects) {
            super(context, resource, objects);
        }
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if(row==null){
                LayoutInflater inflater=getLayoutInflater();
                row=inflater.inflate(R.layout.row, parent, false);
            }
            TextView textfilePath = (TextView)row.findViewById(R.id.FilePath);
            textfilePath.setText(videoFileList[position]);
            //ImageView imageThumbnail = (ImageView)row.findViewById(R.id.Thumbnail);
            //Bitmap bmThumbnail;
            //bmThumbnail = ThumbnailUtils.createVideoThumbnail(videoFileList[position], MediaStore.Video.Thumbnails.MICRO_KIND);
            //imageThumbnail.setImageBitmap(bmThumbnail);
            return row;
        }
    }
    public static ArrayList<String> getVideoList(Context context) {
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = { MediaStore.Video.VideoColumns.DATA };
        ArrayList<String> tmp=new ArrayList<String>();
        int i=0;
        Cursor c = context.getContentResolver().query(uri, projection, null, null, null);
        int vidsCount = 0;
        if (c != null) {
            vidsCount = c.getCount();
            while (c.moveToNext()) {
                Log.d("MainActivity", c.getString(0));
                tmp.add(i++, c.getString(0));
            }
            c.close();
        }
        return tmp;
    }
}
