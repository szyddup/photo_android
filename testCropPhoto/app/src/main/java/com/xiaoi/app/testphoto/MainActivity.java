package com.xiaoi.app.testphoto;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.kevin.crop.UCrop;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @Bind(R.id.iv_photo)
    ImageView ivPhoto;
    public static final int PHOTO_TAKE = 0; //拍照
    public static final int PHOTO_ALBUM = 1; // 相册
    @Bind(R.id.btn_take_photo)
    Button btnTakePhoto;
    @Bind(R.id.btn_take_album)
    Button btnTakeAlbum;
    private String photoSavePath;
    private String photoSaveName;
    private String cropPhotoSaveName;
    private File file;
    private static final String TAG = "MainActivity";
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        photoSavePath = Environment.getExternalStorageDirectory() + "/DCIM/Camera/";
        photoSaveName = System.currentTimeMillis() + ".png";
    }


    @OnClick({R.id.iv_photo, R.id.btn_take_photo, R.id.btn_take_album})
    void onclick(View view) {
        switch (view.getId()) {
            case R.id.btn_take_photo:
                if (hasCamera()) {
                    Intent openCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    file = new File(photoSavePath, photoSaveName);
                    imageUri = Uri.fromFile(file);
                    openCameraIntent.putExtra(MediaStore.Images.Media.ORIENTATION, 0);
                    openCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    startActivityForResult(openCameraIntent, PHOTO_TAKE);
                } else {
                    Toast.makeText(this, "没有找到相机程序", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_take_album:
                Intent openAlbumIntent = new Intent(Intent.ACTION_GET_CONTENT);
                openAlbumIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(openAlbumIntent, PHOTO_ALBUM);
                break;
        }

    }

    /**
     * activity的回调事件
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PHOTO_TAKE:
                InputStream is = null;
                try {
                    if (Activity.RESULT_OK == resultCode) {
                        // 发送广播刷新相册
                        Intent localIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, imageUri);//更新相册
                        sendBroadcast(localIntent);
                        cropPhotoSaveName = System.currentTimeMillis()+"crop" + ".png";
                        file = new File(photoSavePath, cropPhotoSaveName);
                        if (!file.exists()) {
                            file.createNewFile();
                        }
                        Uri cropImageUri = Uri.fromFile(file);
                        startCropActivity(imageUri,cropImageUri);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "拍照返回时发生错误，错误原因：" + e.getMessage());
                } finally {
                    if (is != null)
                        try {
                            is.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                }
                break;
            case PHOTO_ALBUM:
                try {
                    Uri originalUri;
                    if (data != null) {//确认有照片
                        // 照片的原始资源地址
                        originalUri = data.getData();
                        file = new File(photoSavePath, photoSaveName);
                         if (!file.exists()) {
                        file.createNewFile();
                        }
                        Uri cropImageUri = Uri.fromFile(file);
                        startCropActivity(originalUri,cropImageUri);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "相册返回时发生错误，错误原因：" + e.getMessage());
                }
                break;
            default:
                break;
        }
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            final Uri resultUri = UCrop.getOutput(data);
            Bitmap newBitmap = DBitmapCompressUtils.decodeBitmapFromFile(DBitmapCompressUtils.getRealFilePath(this, resultUri), 1000, 1000);
            ivPhoto.setImageBitmap(newBitmap);
            // 发送广播刷新相册
            Intent localIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, resultUri);
            sendBroadcast(localIntent);
        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
            Toast.makeText(this,"裁剪照片出错",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 判断系统中是否存在可以启动的相机应用
     *
     * @return 存在返回true，不存在返回false
     */
    public boolean hasCamera() {
        PackageManager packageManager = this.getPackageManager();
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    /**
     * 裁剪图片方法实现
     *
     * @param sourseUrl,destionUrl
     */
    public void startCropActivity(Uri sourseUrl,Uri destionUrl) {
        UCrop.of(sourseUrl, destionUrl)
                .withAspectRatio(1, 1)
                .withMaxResultSize(1000, 1000)
                .withTargetActivity(CropActivity.class)
                .start(this);
    }
}
