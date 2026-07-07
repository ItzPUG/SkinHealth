package com.example.skincancerai;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ImageProcessor {

    public static final int IMG_SIZE = 224;

    private ImageProcessor() {
        // no instance
    }

    public static ByteBuffer bitmapToByteBuffer(Bitmap bitmap) {
        if (bitmap == null) {
            throw new IllegalArgumentException("Bitmap is null");
        }

        Bitmap resized = Bitmap.createScaledBitmap(bitmap, IMG_SIZE, IMG_SIZE, true);

        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * IMG_SIZE * IMG_SIZE * 3);
        buffer.order(ByteOrder.nativeOrder());
        buffer.rewind();

        int[] pixels = new int[IMG_SIZE * IMG_SIZE];
        resized.getPixels(pixels, 0, IMG_SIZE, 0, 0, IMG_SIZE, IMG_SIZE);

        for (int pixel : pixels) {
            float r = (pixel >> 16) & 0xFF;
            float g = (pixel >> 8) & 0xFF;
            float b = pixel & 0xFF;

            // Model mới yêu cầu RGB float32 0..255
            buffer.putFloat(r);
            buffer.putFloat(g);
            buffer.putFloat(b);
        }

        buffer.rewind();
        return buffer;
    }
}
