package com.typito.uploader;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class RowAdapter extends ArrayAdapter<String> {
    Context _context;
    String[] videoFileList;
    //HashMap<String, Bitmap> cacheBitmap = new HashMap<String, Bitmap>();
    /*String filePath = Environment.getExternalStorageDirectory();*/
    public RowAdapter(Context context, int resource, String[] objects) {
        super(context, resource, objects);
        _context=context;
        videoFileList=objects;
    }

    /*private void initCacheBitmap() {
        for(String string:getVideoList(MainActivity.this))
            cacheBitmap.put(string, ThumbnailUtils.createVideoThumbnail(string, MediaStore.Video.Thumbnails.MICRO_KIND));
    }*/

    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if(row==null){
            LayoutInflater inflater= (LayoutInflater) this._context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row=inflater.inflate(R.layout.row, parent, false);
        }
        TextView textfilePath = (TextView)row.findViewById(R.id.FilePath);
        textfilePath.setText(videoFileList[position]);
//        ImageView imageThumbnail = (ImageView)row.findViewById(R.id.Thumbnail);
        new SetImageTask((ImageView)row.findViewById(R.id.Thumbnail)).execute(videoFileList[position]);
        return row;
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

    public class SetImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;
        public SetImageTask(ImageView bmImage) {
            this.bmImage=bmImage;
        }
        protected void onPreExecute() {
            super.onPreExecute();
        }
        protected Bitmap doInBackground(String... urls) {
            String file = urls[0];
            Bitmap bmThumbnail=null;
            try {
                bmThumbnail = ThumbnailUtils.createVideoThumbnail(file, MediaStore.Video.Thumbnails.MICRO_KIND);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return bmThumbnail;
        }
        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }
}