package com.example.skincancerai;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Interpreter.Options;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class SkinCancerClassifier {

    private static final String TAG = "SkinCancerClassifier";
    private static final String MODEL_NAME = "skin_model.tflite";

    private final Interpreter interpreter;

    public SkinCancerClassifier(Context context) {
        try {
            Options options = new Options();
            options.setUseXNNPACK(true);
            options.setNumThreads(4);

            interpreter = new Interpreter(
                    ModelUtils.loadModel(context, MODEL_NAME),
                    options
            );

            Log.d(TAG, "Loaded model: " + MODEL_NAME);
            Log.d(TAG, "Input shape: " + Arrays.toString(interpreter.getInputTensor(0).shape()));
            Log.d(TAG, "Input dtype: " + interpreter.getInputTensor(0).dataType());
            Log.d(TAG, "Output shape: " + Arrays.toString(interpreter.getOutputTensor(0).shape()));
            Log.d(TAG, "Output dtype: " + interpreter.getOutputTensor(0).dataType());

        } catch (IOException e) {
            Log.e(TAG, "Cannot load model " + MODEL_NAME, e);
            throw new RuntimeException("Khởi tạo model thất bại", e);
        } catch (Exception e) {
            Log.e(TAG, "Interpreter init failed", e);
            throw new RuntimeException("Khởi tạo TensorFlow Lite thất bại", e);
        }
    }

    public Result classify(Bitmap bitmap) {
        float malignantProb = predictMalignantProbability(bitmap);
        return toResult(malignantProb);
    }

    public Result classifyAveraged(Bitmap bitmap, int numRuns) {
        if (numRuns <= 1) {
            return classify(bitmap);
        }

        float sum = 0f;
        int successCount = 0;

        for (int i = 0; i < numRuns; i++) {
            try {
                float prob = predictMalignantProbability(bitmap);
                sum += prob;
                successCount++;
            } catch (Exception e) {
                Log.w(TAG, "Averaged run " + i + " failed", e);
            }
        }

        if (successCount == 0) {
            throw new RuntimeException("Không có lần infer nào thành công");
        }

        float avgProb = sum / successCount;
        Log.d(TAG, "Average malignant probability = " + avgProb + " over " + successCount + " runs");
        return toResult(avgProb);
    }

    private float predictMalignantProbability(Bitmap bitmap) {
        if (bitmap == null) {
            throw new IllegalArgumentException("Bitmap cannot be null");
        }

        ByteBuffer input = ImageProcessor.bitmapToByteBuffer(bitmap);
        float[][] output = new float[1][1];

        interpreter.run(input, output);

        float prob = output[0][0];

        if (Float.isNaN(prob) || Float.isInfinite(prob)) {
            throw new RuntimeException("Model output không hợp lệ: " + prob);
        }

        prob = Math.max(0f, Math.min(1f, prob));
        Log.d(TAG, "Raw malignant probability = " + prob);

        return prob;
    }

    private Result toResult(float malignantProb) {
        malignantProb = Math.max(0f, Math.min(1f, malignantProb));

        String label;
        float confidence = Math.max(malignantProb, 1f - malignantProb);

        if (malignantProb > 0.80f) {
            label = "Nguy cơ cao";
        } else if (malignantProb < 0.60f) {
            label = "Nguy cơ thấp";
        } else {
            label = "Nguy cơ trung bình";
        }

        Log.d(TAG, "Mapped result => label=" + label
                + ", confidence=" + confidence
                + ", malignantProb=" + malignantProb);

        return new Result(label, confidence);
    }
    public void close() {
        try {
            interpreter.close();
        } catch (Exception e) {
            Log.w(TAG, "Interpreter close failed", e);
        }
    }
}
