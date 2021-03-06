package com.whysly.cropimageanddownload;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.gson.JsonObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private static final String CAPTURE_PATH = "/Profile_Alimi";
    private static final int PICK_FROM_CAMERA = 0;
    private static final int PICK_FROM_ALBUM = 1;
    private static final int CROP_FROM_IMAGE = 2;
    private String absolutePath;
    private Uri mImageCaptureUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences pref = getSharedPreferences("data", MODE_PRIVATE);
        File file = new File(pref.getString("profilepic_path", ""));
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part profile_img = MultipartBody.Part.createFormData("profile_img", file.getName(), requestFile);
        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://118.67.129.104/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        MyService service = retrofit.create(MyService.class);
        Call<JsonObject> upload_Image = service.uploadImage( "authorization", profile_img);
        upload_Image.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {

            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {

            }
        });
    }


    public void onPicClick() {


        DialogInterface.OnClickListener cameraListener = new DialogInterface.OnClickListener() {

            @Override

            public void onClick(DialogInterface dialog, int which) {

                doTakePhotoAction();

            }

        };

        DialogInterface.OnClickListener albumListener = new DialogInterface.OnClickListener() {

            @Override

            public void onClick(DialogInterface dialog, int which) {

                doTakeAlbumAction();

            }

        };


        DialogInterface.OnClickListener cancelListener = new DialogInterface.OnClickListener() {

            @Override

            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();

            }

        };

        new AlertDialog.Builder(this,R.style.Theme_AppCompat_Dialog_Alert)
                .setTitle("???????????? ????????? ??????")
                .setPositiveButton("????????????", cameraListener)
                .setNeutralButton("??????", cancelListener)
                .setNegativeButton("????????????", albumListener)
                .show();
    }



    public void doTakePhotoAction() // ????????? ?????? ??? ????????? ????????????

    {

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);


        // ????????? ????????? ????????? ????????? ??????

        String url = "tmp_" + String.valueOf(System.currentTimeMillis()) + ".jpg";

        mImageCaptureUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), url));


        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mImageCaptureUri);

        startActivityForResult(intent, PICK_FROM_CAMERA);

    }

    public void doTakeAlbumAction() // ???????????? ????????? ????????????

    {

        // ?????? ??????

        Intent intent = new Intent(Intent.ACTION_PICK);

        intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);

        startActivityForResult(intent, PICK_FROM_ALBUM);

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode,resultCode,data);
        SharedPreferences pref = getSharedPreferences("data", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();


        if(resultCode != RESULT_OK)

            return;


        switch(requestCode)

        {

            case PICK_FROM_ALBUM:

            {

                // ????????? ????????? ???????????? ???????????? ??????  break?????? ???????????????.

                // ?????? ??????????????? ?????? ???????????? ????????? ??????????????? ????????????.

                mImageCaptureUri = data.getData();

                Log.d("????????? ??????",mImageCaptureUri.getPath().toString());

            }


            case PICK_FROM_CAMERA:

            {

                // ???????????? ????????? ????????? ??????????????? ????????? ????????? ???????????????.

                // ????????? ????????? ?????? ????????????????????? ???????????? ?????????.

                Intent intent = new Intent("com.android.camera.action.CROP");

                intent.setDataAndType(mImageCaptureUri, "image/*");


                // CROP??? ???????????? 200*200 ????????? ??????

                intent.putExtra("outputX", 200); // CROP??? ???????????? x??? ??????

                intent.putExtra("outputY", 200); // CROP??? ???????????? y??? ??????

                intent.putExtra("aspectX", 1); // CROP ????????? X??? ??????

                intent.putExtra("aspectY", 1); // CROP ????????? Y??? ??????

                intent.putExtra("scale", true);

                intent.putExtra("return-data", true);

                startActivityForResult(intent, CROP_FROM_IMAGE); // CROP_FROM_CAMERA case??? ??????

                break;

            }


            case CROP_FROM_IMAGE: {

                // ????????? ??? ????????? ???????????? ?????? ????????????.

                // ??????????????? ???????????? ?????????????????? ???????????? ?????? ?????????

                // ?????? ????????? ???????????????.

                if (resultCode != RESULT_OK) {

                    return;

                }


                final Bundle extras = data.getExtras();
                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()

                        +"/" + System.currentTimeMillis()+".jpg";

                if(extras != null)

                {

                    Bitmap photo = extras.getParcelable("data"); // CROP??? BITMAP

                    //iv_UserPhoto.setImageBitmap(photo); // ??????????????? ??????????????? CROP??? BITMAP??? ?????????



                    storeCropImage(photo, filePath); // CROP??? ???????????? ???????????????, ????????? ????????????.

                    absolutePath = filePath;



                    break;


                }

                // ?????? ?????? ??????

                File f = new File(mImageCaptureUri.getPath());

                if(f.exists())

                {

                    f.delete();

                }

            }





        }

    }


    private void storeCropImage(Bitmap bitmap, String filePath) {

        // SmartWheel ????????? ???????????? ???????????? ???????????? ????????????.

        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + CAPTURE_PATH;

        File directory_SmartWheel = new File(dirPath);


        if(!directory_SmartWheel.exists()) // SmartWheel ??????????????? ????????? ????????? (?????? ???????????? ????????? ????????? ?????????.)

            directory_SmartWheel.mkdir();
        File copyFile = new File(filePath);

        BufferedOutputStream out = null;


        try {


            copyFile.createNewFile();

            out = new BufferedOutputStream(new FileOutputStream(copyFile));

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);


            // sendBroadcast??? ?????? Crop??? ????????? ????????? ???????????? ????????????.

            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,

                    Uri.fromFile(copyFile)));

            MediaScannerConnection.scanFile( getApplicationContext(),

                    new String[]{copyFile.getAbsolutePath()},

                    null,

                    new MediaScannerConnection.OnScanCompletedListener(){

                        @Override

                        public void onScanCompleted(String path, Uri uri) {

                            Log.v("File scan", "file:" + path + "was scanned seccessfully");

                        }

                    });



            out.flush();

            out.close();

        } catch (Exception e) {

            e.printStackTrace();

        }

        Log.d("file path", filePath);
        Uri imageUri = Uri.fromFile(copyFile);
//        Glide.with(getApplicationContext()).load(imageUri)
//                .centerCrop()
//                //.placeholder(R.drawable.alimi_sample)
//                //.error(R.drawable.alimi_sample)
//                .into(ivImage);


    }
}