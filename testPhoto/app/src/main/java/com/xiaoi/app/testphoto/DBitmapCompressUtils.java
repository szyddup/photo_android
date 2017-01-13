package com.xiaoi.app.testphoto;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 图片压缩帮助类
 */
public class DBitmapCompressUtils {
	private static String TAG = "BitmapCompressUtils";

	/**
	 * 图片压缩
	 * 
	 * @param srcFile
	 * @param destFile
	 * @param width
	 * @param height
	 */
	public static void compress(File srcFile, File destFile, int width, int height) {
		BufferedOutputStream bos = null;
		try {
			if (srcFile == null || destFile == null) {
				return;
			}
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			options.inSampleSize = 2;
			// options.inSampleSize = calculateInSampleSize(options, width, height);
			options.inJustDecodeBounds = false;
			Bitmap bitmap = BitmapFactory.decodeFile(srcFile.getAbsolutePath(), options);
			// 输出
			bos = new BufferedOutputStream(new FileOutputStream(destFile));
			bitmap.compress(Bitmap.CompressFormat.JPEG, 30, bos);
			bos.flush();
		} catch (IOException e) {
			Log.e(TAG, "压缩图片时发生错误，错误原因：" + e.getMessage());
			e.printStackTrace();
		} finally {
			if (bos != null) {
				try {
					bos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
//
//	/**
//	 * 计算压缩比
//	 *
//	 * @param options
//	 * @param reqWidth
//	 * @param reqHeight
//	 * @return
//	 */
//	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
//		final int picheight = options.outHeight;
//		final int picwidth = options.outWidth;
//		int targetheight = picheight;
//		int targetwidth = picwidth;
//		int inSampleSize = 1;
//		if (targetheight > reqHeight || targetwidth > reqWidth) {
//			while (targetheight >= reqHeight && targetwidth >= reqWidth) {
//				inSampleSize += 1;
//				targetheight = picheight / inSampleSize;
//				targetwidth = picwidth / inSampleSize;
//			}
//		}
//		return inSampleSize;
//	}

	/**
	 * 压缩图片
	 * 
	 * @param image
	 * @return
	 */

	public static Bitmap compressImage(Bitmap image) {
		ByteArrayInputStream isBm = null;
		ByteArrayOutputStream baos = null;
		Bitmap bitmap = null;
		try {
			baos = new ByteArrayOutputStream();
			image.compress(Bitmap.CompressFormat.JPEG, 30, baos);// 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
			int options = 100;
			while (baos.toByteArray().length / 1024 > 300) { // 循环判断如果压缩后图片是否大于100kb,大于继续压缩
				options -= 10;// 每次都减少10
				baos.reset();// 重置baos即清空baos
				image.compress(Bitmap.CompressFormat.JPEG, options, baos);// 这里压缩options%，把压缩后的数据存放到baos中
			}
			isBm = new ByteArrayInputStream(baos.toByteArray());// 把压缩后的数据baos存放到ByteArrayInputStream中
			bitmap = BitmapFactory.decodeStream(isBm, null, null);// 把ByteArrayInputStream数据生成图片
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				isBm.close();
				baos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return bitmap;
	}

	//***************************************************
	public static Bitmap decodeBitmapFromFile(String imagePath, int requestWidth, int requestHeight) {
		if (!TextUtils.isEmpty(imagePath)) {
			Log.i(TAG, "requestWidth: " + requestWidth);
			Log.i(TAG, "requestHeight: " + requestHeight);
			if (requestWidth <= 0 || requestHeight <= 0) {
				Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
				return bitmap;
			}
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;//不加载图片到内存，仅获得图片宽高
			BitmapFactory.decodeFile(imagePath, options);
			Log.i(TAG, "original height: " + options.outHeight);
			Log.i(TAG, "original width: " + options.outWidth);
			if (options.outHeight == -1 || options.outWidth == -1) {
				try {
					ExifInterface exifInterface = new ExifInterface(imagePath);
					int height = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, ExifInterface.ORIENTATION_NORMAL);//获取图片的高度
					int width = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, ExifInterface.ORIENTATION_NORMAL);//获取图片的宽度
					Log.i(TAG, "exif height: " + height);
					Log.i(TAG, "exif width: " + width);
					options.outWidth = width;
					options.outHeight = height;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			options.inSampleSize = calculateInSampleSize(options, requestWidth, requestHeight); //计算获取新的采样率
			Log.i(TAG, "inSampleSize: " + options.inSampleSize);
			options.inJustDecodeBounds = false;
			return BitmapFactory.decodeFile(imagePath, options);

		} else {
			return null;
		}
	}


	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;
		Log.i(TAG, "height: " + height);
		Log.i(TAG, "width: " + width);
		if (height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}

			long totalPixels = width * height / inSampleSize;

			final long totalReqPixelsCap = reqWidth * reqHeight * 2;

			while (totalPixels > totalReqPixelsCap) {
				inSampleSize *= 2;
				totalPixels /= 2;
			}
		}
		return inSampleSize;
	}


	/**
	 * 通过URI拿到文件路径
	 *
	 * @param context
	 * @param uri
	 * @return
	 */
	@TargetApi(19)
	public static String getRealFilePath(final Context context, final Uri uri) {
		final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
		if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
			if (isExternalStorageDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];
				if ("primary".equalsIgnoreCase(type)) {
					return Environment.getExternalStorageDirectory() + "/" + split[1];
				}
			} else if (isDownloadsDocument(uri)) {
				final String id = DocumentsContract.getDocumentId(uri);
				final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
				return getDataColumn(context, contentUri, null, null);
			} else if (isMediaDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];
				Uri contentUri = null;
				if ("image".equals(type)) {
					contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				} else if ("video".equals(type)) {
					contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
				} else if ("audio".equals(type)) {
					contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
				}
				final String selection = "_id=?";
				final String[] selectionArgs = new String[]{split[1]};
				return getDataColumn(context, contentUri, selection, selectionArgs);
			}
		} else if ("content".equalsIgnoreCase(uri.getScheme())) {
			if (isGooglePhotosUri(uri))
				return uri.getLastPathSegment();
			return getDataColumn(context, uri, null, null);
		} else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}
		return null;
	}

	/**
	 * @return The value of the _data column, which is typically a file path.
	 */

	public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
		Cursor cursor = null;
		final String column = "_data";
		final String[] projection = {column};
		try {
			cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
			if (cursor != null && cursor.moveToFirst()) {
				final int index = cursor.getColumnIndexOrThrow(column);
				return cursor.getString(index);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}

	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is ExternalStorageProvider.
	 */
	public static boolean isExternalStorageDocument(Uri uri) {
		return "com.android.externalstorage.documents".equals(uri.getAuthority());
	}

	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is DownloadsProvider.
	 */
	public static boolean isDownloadsDocument(Uri uri) {
		return "com.android.providers.downloads.documents".equals(uri.getAuthority());
	}

	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is MediaProvider.
	 */
	public static boolean isMediaDocument(Uri uri) {
		return "com.android.providers.media.documents".equals(uri.getAuthority());
	}

	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is Google Photos.
	 */
	public static boolean isGooglePhotosUri(Uri uri) {
		return "com.google.android.apps.photos.content".equals(uri.getAuthority());
	}
}
