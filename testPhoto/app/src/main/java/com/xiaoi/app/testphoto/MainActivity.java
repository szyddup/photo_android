package com.xiaoi.app.testphoto;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import com.yalantis.ucrop.UCrop;

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
//                  Uri  imageUri = Uri.parse(photoSavePath+photoSaveName);
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
                        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
                        bitmapOptions.inSampleSize = 2;
                        is = getContentResolver().openInputStream(imageUri);
                        if (is != null) {//确认有照片
                            Bitmap bitmap = BitmapFactory.decodeStream(is, null, bitmapOptions);
                            Bitmap newBitmap = DBitmapCompressUtils.compressImage(bitmap);
                            ivPhoto.setImageBitmap(newBitmap);
                            bitmap.recycle();
                            // 发送广播刷新相册
                            Intent localIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, imageUri);
                            sendBroadcast(localIntent);
                        }
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
                    ContentResolver resolver = getContentResolver();
                    Uri originalUri;
                    if (data != null) {//确认有照片
                        // 照片的原始资源地址
                        originalUri = data.getData();
                        startCropActivity(originalUri);
//                        UCrop.of(originalUri, originalUri)
//                                .withAspectRatio(16, 9)
//                                .withMaxResultSize(maxWidth, maxHeight)
//                                .start(this);
                        if (originalUri != null) {
                            Bitmap newBitmap = DBitmapCompressUtils.decodeBitmapFromFile(DBitmapCompressUtils.getRealFilePath(this, originalUri), 1000, 1000);
                            // 使用ContentProvider通过URI获取原始图片
//                        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
//                        bitmapOptions.inSampleSize = 2;
//                        is = getContentResolver().openInputStream(originalUri);
//                        Bitmap bitmap = BitmapFactory.decodeStream(is, null, bitmapOptions);
//                        Bitmap newBitmap = DBitmapCompressUtils.compressImage(bitmap);
                            ivPhoto.setImageBitmap(newBitmap);
                        } else {
                            Bundle bundle = data.getExtras();
                            if (bundle != null) {
                                Bitmap photo = (Bitmap) bundle.get("data"); //get bitmap
                                Bitmap newBitmap = DBitmapCompressUtils.compressImage(photo);
                                ivPhoto.setImageBitmap(newBitmap);
                                photo.recycle();
                                //spath :生成图片取个名字和路径包含类型
//                                saveImage(Bitmap photo, String spath);
                            } else {
                                Toast.makeText(getApplicationContext(), "err****", Toast.LENGTH_LONG).show();
                                return;
                            }
                        }
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
        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
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
     * @param uri
     */
    public void startCropActivity(Uri uri) {
//        UCrop.of(uri, uri)
//                .withAspectRatio(1, 1)
//                .withMaxResultSize(512, 512)
//                .start(this);

//        UCrop.of(uri, uri)
//                .withAspectRatio(1, 1)
//                .withMaxResultSize(512, 512)
//                .withTargetActivity(CropActivity.class)
//                .start(mActivity, this);
    }
}
