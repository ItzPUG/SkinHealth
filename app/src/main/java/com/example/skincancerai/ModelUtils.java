package com.example.skincancerai;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class ModelUtils {

    private static final String TAG = "ModelUtils";

    public static MappedByteBuffer loadModel(Context context, String modelName)
            throws IOException {

        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        if (modelName == null || modelName.isEmpty()) {
            throw new IllegalArgumentException("Model name cannot be null or empty");
        }

        AssetFileDescriptor fileDescriptor = null;
        FileInputStream inputStream = null;

        try {
            fileDescriptor = context.getAssets().openFd(modelName);
            if (fileDescriptor == null) {
                throw new IOException("AssetFileDescriptor is null for model: " + modelName);
            }

            inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel channel = inputStream.getChannel();

            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();

            if (declaredLength <= 0) {
                throw new IOException("Invalid model file size for: " + modelName);
            }

            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
            
            if (buffer == null) {
                throw new IOException("Failed to map model file to memory: " + modelName);
            }

            Log.d(TAG, "Model loaded successfully: " + modelName + " (size: " + declaredLength + " bytes)");
            return buffer;

        } catch (IOException e) {
            Log.e(TAG, "Error loading model " + modelName + ": " + e.getMessage(), e);
            throw e;
        } finally {
            // Note: FileInputStream and AssetFileDescriptor will be closed automatically
            // when the MappedByteBuffer is garbage collected, but we can't close them here
            // as the buffer still needs access to the file descriptor
        }
    }
}
