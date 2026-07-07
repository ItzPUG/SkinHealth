package com.example.skincancerai;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ImageUtils {

    private static final String TAG = "ImageUtils";

    public static Bitmap imageProxyToBitmap(ImageProxy image) {
        if (image == null) {
            Log.e(TAG, "ImageProxy is null");
            return null;
        }

        try {
            int width = image.getWidth();
            int height = image.getHeight();

            // CameraX cung cấp góc cần xoay để ảnh khớp với hướng hiển thị.
            int rotationDegrees = image.getImageInfo().getRotationDegrees();

            Log.d(
                    TAG,
                    "Converting ImageProxy, size: "
                            + width + "x" + height
                            + ", format: " + image.getFormat()
                            + ", rotation: " + rotationDegrees
            );

            ImageProxy.PlaneProxy[] planes = image.getPlanes();

            if (planes == null || planes.length == 0) {
                Log.e(
                        TAG,
                        "Invalid image planes: "
                                + (planes != null ? planes.length : 0)
                );
                return null;
            }

            Bitmap bitmap;

            // Một số thiết bị trả ảnh JPEG với một plane duy nhất.
            if (planes.length == 1 || image.getFormat() == ImageFormat.JPEG) {
                ByteBuffer jpegBuffer = planes[0].getBuffer();

                byte[] jpegBytes = new byte[jpegBuffer.remaining()];
                jpegBuffer.get(jpegBytes);

                bitmap = BitmapFactory.decodeByteArray(
                        jpegBytes,
                        0,
                        jpegBytes.length
                );

                if (bitmap == null) {
                    Log.e(TAG, "Failed to decode JPEG single-plane image");
                    return null;
                }

                return rotateBitmapIfNeeded(bitmap, rotationDegrees);
            }

            if (planes.length < 3) {
                Log.e(TAG, "Unsupported plane count: " + planes.length);
                return null;
            }

            byte[] nv21 = yuv420888ToNv21(
                    planes,
                    width,
                    height
            );

            if (width <= 0 || height <= 0) {
                Log.e(
                        TAG,
                        "Invalid image dimensions: "
                                + width + "x" + height
                );
                return null;
            }

            YuvImage yuvImage = new YuvImage(
                    nv21,
                    ImageFormat.NV21,
                    width,
                    height,
                    null
            );

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            boolean success = yuvImage.compressToJpeg(
                    new Rect(0, 0, width, height),
                    100,
                    out
            );

            if (!success) {
                Log.e(TAG, "Failed to compress YUV to JPEG");
                return null;
            }

            byte[] jpegBytes = out.toByteArray();

            bitmap = BitmapFactory.decodeByteArray(
                    jpegBytes,
                    0,
                    jpegBytes.length
            );

            if (bitmap == null) {
                Log.e(TAG, "Failed to decode JPEG bytes");
                return null;
            }

            return rotateBitmapIfNeeded(bitmap, rotationDegrees);

        } catch (Exception e) {
            Log.e(
                    TAG,
                    "Error converting ImageProxy to Bitmap: "
                            + e.getMessage(),
                    e
            );
            return null;
        }
    }

    /**
     * Xoay ảnh theo metadata của CameraX.
     * Hàm trả về chính bitmap cũ nếu không cần xoay.
     */
    private static Bitmap rotateBitmapIfNeeded(
            Bitmap source,
            int rotationDegrees
    ) {
        if (source == null) return null;

        if (rotationDegrees == 0) {
            return source;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);

        Bitmap rotated = Bitmap.createBitmap(
                source,
                0,
                0,
                source.getWidth(),
                source.getHeight(),
                matrix,
                true
        );

        // Giải phóng ảnh cũ để tránh chiếm RAM không cần thiết.
        if (rotated != source && !source.isRecycled()) {
            source.recycle();
        }

        return rotated;
    }

    private static byte[] yuv420888ToNv21(
            ImageProxy.PlaneProxy[] planes,
            int width,
            int height
    ) {
        int ySize = width * height;
        int uvSize = width * height / 2;

        byte[] nv21 = new byte[ySize + uvSize];

        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int yRowStride = planes[0].getRowStride();
        int yPixelStride = planes[0].getPixelStride();

        int uRowStride = planes[1].getRowStride();
        int uPixelStride = planes[1].getPixelStride();

        int vRowStride = planes[2].getRowStride();
        int vPixelStride = planes[2].getPixelStride();

        int outputOffset = 0;

        for (int row = 0; row < height; row++) {
            int rowStart = row * yRowStride;

            for (int col = 0; col < width; col++) {
                int index = rowStart + col * yPixelStride;
                nv21[outputOffset++] = yBuffer.get(index);
            }
        }

        int uvHeight = height / 2;
        int uvWidth = width / 2;

        outputOffset = ySize;

        for (int row = 0; row < uvHeight; row++) {
            int uRowStart = row * uRowStride;
            int vRowStart = row * vRowStride;

            for (int col = 0; col < uvWidth; col++) {
                int uIndex = uRowStart + col * uPixelStride;
                int vIndex = vRowStart + col * vPixelStride;

                nv21[outputOffset++] = vBuffer.get(vIndex);
                nv21[outputOffset++] = uBuffer.get(uIndex);
            }
        }

        return nv21;
    }
}
