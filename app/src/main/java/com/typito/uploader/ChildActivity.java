package com.typito.uploader;

import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.games.video.Video;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ChildActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private SharedPreferences pref;
    private String pref_FILE="file_path", TAG, PACKAGE_NAME;
    private static final int REQUEST_CODE = 101, REQUEST_CODE_RESOLUTION = 3, PICKVIDEO_REQUEST_CODE=100;
//    private File textfile;
    private TextView tv_filepath;
    private Button bt_fileSelect, bt_upload;
    private ProgressBar pb_progress;
    private GoogleApiClient gApiClient;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child);
        init();
        //Log.i(TAG, "Legal Requirements: "+ GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(this));
        /*build the api client*/
        buildGoogleApiClient();
//        textfile=new File(Environment.getExternalStorageDirectory()+File.separator+"hy.txt");
        bt_fileSelect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent,PICKVIDEO_REQUEST_CODE);
            }
        });
        bt_upload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                pb_progress.setVisibility(View.VISIBLE);
                pb_progress.setProgress(0);
                saveToDrive();
            }
        });
    }
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart(): GAPI connecting...");
        gApiClient.connect();
    }
    protected void onStop() {
        super.onStop();
        if (gApiClient != null) {
            Log.i(TAG, "onStop(): GAPI disconnecting...");
            gApiClient.disconnect();
        }
    }
    protected void onResume() {
        super.onResume();
        read_pref();
        if(read_pref()!=null)
            tv_filepath.setText(read_pref());
    }
    protected void onPause() {
        super.onPause();
    }
    protected void onDestroy() {
        super.onDestroy();
    }

    private void init() {
        tv_filepath= (TextView) findViewById(R.id.tv_file);
        bt_fileSelect= (Button) findViewById(R.id.bt_selectVideo);
        bt_upload= (Button) findViewById(R.id.bt_upload);
        pb_progress= (ProgressBar) findViewById(R.id.pb_progress);
        pb_progress.setMax(10);
        PACKAGE_NAME = getApplicationContext().getPackageName();
        TAG = this.getClass().getSimpleName();
        pref=getSharedPreferences(PACKAGE_NAME, Context.MODE_PRIVATE);
    }
    public String read_pref(){
        String tmp=pref.getString(pref_FILE,null);
        Log.i(TAG, "reading_pref: file_path: "+tmp);
        return tmp;
    }
    public void write_pref(String tmp){
        pref.edit().putString(pref_FILE, tmp).commit();
        Log.i(TAG, "writing_pref: filep_path"+tmp);
    }
    private void buildGoogleApiClient() {
        if (gApiClient == null) {
            gApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;
        if (requestCode == PICKVIDEO_REQUEST_CODE){
            Uri VideoPath = data.getData();
            String path=getRealPathFromURI(VideoPath);
            write_pref(path);
            tv_filepath.setText(read_pref());
        }
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            Log.i(TAG, "onActivityResult(): GAPI connecting...");
            gApiClient.connect();
        }
    }
    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(getApplicationContext(), contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }

    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "onConnected(): GAPI connected...");
    }
    public void onConnectionSuspended(int i) {
        switch (i) {
            case 1:
                Log.i(TAG, "onConnectionSuspended(): "+"Service disconnected");
                break;
            case 2:
                Log.i(TAG, "onConnectionSuspended(): "+"Connection lost");
                break;
            default:
                Log.i(TAG, "onConnectionSuspended(): "+"Unknown");
                break;
        }
    }
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "onConnectionFailed(): "+connectionResult.toString());
        if (!connectionResult.hasResolution()) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, connectionResult.getErrorCode(), 0).show();
            return;
        }
        try {
            Log.i(TAG, "startResolutionForResult() with CODE:" + REQUEST_CODE_RESOLUTION);
            connectionResult.startResolutionForResult(this, REQUEST_CODE);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }

    private void saveToDrive() {
        Log.i(TAG, "saveToDrive(): creating new contents");
        Drive.DriveApi.newDriveContents(gApiClient).setResultCallback(driveContentsResultResultCallback);
    }

    public class uploadTask extends AsyncTask<DriveContents, Integer, Void> {
        protected void onPreExecute() {
            super.onPreExecute();
            Log.i(TAG, "uploadTask(): adding file to outputstream...");
        }
        protected void onProgressUpdate(Integer... values) {
            pb_progress.setProgress(values[0]);
        }

        protected Void doInBackground(DriveContents... driveContentses) {
            Log.i(TAG, "saveToDrive(): processing file: "+read_pref());
            OutputStream outputStream = driveContentses[0].getOutputStream();

            byte[] buffer = new byte[1024];
            int bytesRead;
            try{
                BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(read_pref()));
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer);
                }

            }catch (FileNotFoundException e) {
                System.out.println("File Not Found");
                e.printStackTrace();
            }catch (IOException e) {
                Log.i(TAG, "uploadTask(): problem converting input stream to output stream: " + e);
                e.printStackTrace();
            }
            return null;
        }
        protected void onPostExecute() {
            Log.i(TAG, "uploadTask(): adding file successful...");
            pb_progress.setVisibility(View.GONE);
        }
    }

    final ResultCallback<DriveApi.DriveContentsResult> driveContentsResultResultCallback =
        new ResultCallback<DriveApi.DriveContentsResult>() {
            public void onResult(DriveApi.DriveContentsResult result) {
                if (!result.getStatus().isSuccess()) {
                    Log.i(TAG, "saveToDrive(): failed to create new contents.");
                    return;
                }
                Log.i(TAG, "saveToDrive(): new contents created.");

                //new uploadTask().execute(result.getDriveContents());
                OutputStream outputStream=result.getDriveContents().getOutputStream();

                byte[] buffer = new byte[1024];
                int bytesRead;
                try{
                    BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(read_pref()));
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer);
                    }

                }catch (FileNotFoundException e) {
                    System.out.println("File Not Found");
                    e.printStackTrace();
                }catch (IOException e) {
                    Log.i(TAG, "uploadTask(): problem converting input stream to output stream: " + e);
                    e.printStackTrace();
                }

                MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                        .setTitle("testFile.mp4")
                        .setMimeType("video/mp4")
                        .setDescription("This is a text video uploaded from device")
                        .build();
/*
                Drive.DriveApi.getRootFolder(gApiClient)
                        .createFile(gApiClient, metadataChangeSet, result.getDriveContents())
                        .setResultCallback(fileResultResultCallback);
*/
                IntentSender intentSender = Drive.DriveApi
                        .newCreateFileActivityBuilder()
                        .setInitialMetadata(metadataChangeSet)
                        .setInitialDriveContents(result.getDriveContents())
                        .build(gApiClient);
                try {
                    startIntentSenderForResult(intentSender, 2, null, 0, 0, 0);
                } catch (IntentSender.SendIntentException e) {
                    Log.i(TAG, "Failed to launch file chooser.");
                }

            }
        };

    final private ResultCallback<DriveFolder.DriveFileResult> fileResultResultCallback =
        new ResultCallback<DriveFolder.DriveFileResult>() {
            public void onResult(DriveFolder.DriveFileResult result) {
                if (!result.getStatus().isSuccess()) {
                    Log.i(TAG, "afterDrive(): error creating the file");
                    Toast.makeText(ChildActivity.this,
                            "Error adding file to Drive", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.i(TAG, "afterDrive(): file added to Drive");
                Log.i(TAG, "afterDrive(): created a file with content: "
                        + result.getDriveFile().getDriveId());
                Toast.makeText(ChildActivity.this, "File successfully added to Drive", Toast.LENGTH_SHORT).show();
                final PendingResult<DriveResource.MetadataResult> metadata
                        = result.getDriveFile().getMetadata(gApiClient);
                metadata.setResultCallback(new ResultCallback<DriveResource.MetadataResult>() {
                    public void onResult(DriveResource.MetadataResult metadataResult) {
                        Metadata data = metadataResult.getMetadata();
                        Log.i(TAG, "afterDrive(): Title: " + data.getTitle());
                        String drive_id = data.getDriveId().encodeToString();
                        Log.i(TAG, "afterDrive(): DrivId: " + drive_id);
                        DriveId driveID = data.getDriveId();
                        Log.i(TAG, "afterDrive(): Description: " + data.getDescription().toString());
                        Log.i(TAG, "afterDrive(): MimeType: " + data.getMimeType());
                        Log.i(TAG, "afterDrive(): File size: " + String.valueOf(data.getFileSize()));
                    }
                });
            }
        };
}
