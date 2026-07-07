package com.example.skincancerai;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class TFLiteClassifier {

    private static final int IMG_SIZE = 224;

    private Interpreter interpreter;
    private List<String> labels;

    public TFLiteClassifier(Context context) throws IOException {
        interpreter = new Interpreter(loadModel(context));
        labels = loadLabels(context);
    }

    // ===============================
    // LOAD MODEL
    // ===============================
    private MappedByteBuffer loadModel(Context context) throws IOException {
        AssetFileDescriptor fd = context.getAssets().openFd("skin_cancer_model.tflite");
        FileInputStream fis = new FileInputStream(fd.getFileDescriptor());
        FileChannel channel = fis.getChannel();
        return channel.map(FileChannel.MapMode.READ_ONLY,
                fd.getStartOffset(), fd.getDeclaredLength());
    }

    // ===============================
    // LOAD LABEL MAP
    // ===============================
    private List<String> loadLabels(Context context) throws IOException {
        List<String> list = new ArrayList<>();
        BufferedReader br = new BufferedReader(
                new InputStreamReader(context.getAssets().open("label_map.json"))
        );
        String line;
        while ((line = br.readLine()) != null) {
            if (line.contains(":")) {
                list.add(line.split(":")[1]
                        .replace("\"", "")
                        .replace(",", "")
                        .replace("}", "")
                        .trim());
            }
        }
        br.close();
        return list;
    }

    // ===============================
    // PREPROCESS IMAGE
    // ===============================
    private ByteBuffer preprocess(Bitmap bitmap) {

        Bitmap resized = Bitmap.createScaledBitmap(bitmap, IMG_SIZE, IMG_SIZE, true);

        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * IMG_SIZE * IMG_SIZE * 3);
        buffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[IMG_SIZE * IMG_SIZE];
        resized.getPixels(pixels, 0, IMG_SIZE, 0, 0, IMG_SIZE, IMG_SIZE);

        for (int p : pixels) {
            float r = ((p >> 16) & 0xFF);
            float g = ((p >> 8) & 0xFF);
            float b = (p & 0xFF);

            // EfficientNet normalization
            buffer.putFloat((r - 127.5f) / 127.5f);
            buffer.putFloat((g - 127.5f) / 127.5f);
            buffer.putFloat((b - 127.5f) / 127.5f);
        }

        return buffer;
    }

    // ===============================
    // INFERENCE
    // ===============================
    public Result classify(Bitmap bitmap) {

        float[][] output = new float[1][1]; // sigmoid output

        interpreter.run(preprocess(bitmap), output);

        float prob = output[0][0];

        String label = prob > 0.5 ? "\u00C1c t\u00EDnh" : "L\u00E0nh t\u00EDnh";

        return new Result(label, prob);
    }


    // ===============================
    // RESULT CLASS
    // ===============================
    public static class Result {
        public String label;
        public float confidence;

        public Result(String l, float c) {
            label = l;
            confidence = c;
        }
    }
}

