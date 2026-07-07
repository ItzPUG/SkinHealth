package com.example.skincancerai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Interpreter.Options;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class SkinInputValidator {

    private static final String TAG = "SkinInputValidator";

    private static final String MODEL_NAME = "skin_nonskinmodel.tflite";

    // Kích thước input cho model skin/non-skin
    private static final int MODEL_INPUT_SIZE = 224;

    // Ngưỡng phân loại skin
    private static final float SKIN_THRESHOLD = 0.5f;

    // Dùng để kiểm tra chất lượng ảnh (giữ lại logic cũ để không hỏng flow)
    private static final int ANALYZE_SIZE = 128;

    // Quality thresholds
    private static final float MIN_MEAN_BRIGHTNESS = 45f;
    private static final float MAX_MEAN_BRIGHTNESS = 210f;
    private static final float MAX_DARK_PIXEL_RATIO = 0.45f;
    private static final float MAX_OVEREXPOSED_RATIO = 0.35f;
    private static final float MIN_LAPLACIAN_VARIANCE = 80f;

    private final Interpreter interpreter;

    public static class ValidationResult {
        public final boolean isSkin;
        public final float confidence;
        public final float overallSkinRatio;   // dùng để giữ tương thích với code cũ
        public final float centerSkinRatio;    // dùng để giữ tương thích với code cũ
        public final float combinedScore;      // dùng để lưu probability của model
        public final float meanBrightness;
        public final float laplacianVariance;
        public final boolean qualityPass;
        public final String reason;

        public ValidationResult(boolean isSkin,
                                float confidence,
                                float overallSkinRatio,
                                float centerSkinRatio,
                                float combinedScore,
                                float meanBrightness,
                                float laplacianVariance,
                                boolean qualityPass,
                                String reason) {
            this.isSkin = isSkin;
            this.confidence = confidence;
            this.overallSkinRatio = overallSkinRatio;
            this.centerSkinRatio = centerSkinRatio;
            this.combinedScore = combinedScore;
            this.meanBrightness = meanBrightness;
            this.laplacianVariance = laplacianVariance;
            this.qualityPass = qualityPass;
            this.reason = reason;
        }
    }

    public SkinInputValidator(Context context) {
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
            throw new RuntimeException("Khởi tạo model kiểm tra da thất bại", e);
        } catch (Exception e) {
            Log.e(TAG, "Interpreter init failed", e);
            throw new RuntimeException("Khởi tạo TensorFlow Lite thất bại", e);
        }
    }

    public ValidationResult analyze(Bitmap bitmap) {
        if (bitmap == null) {
            throw new IllegalArgumentException("Bitmap is null");
        }

        // 1) kiểm tra chất lượng ảnh như cũ
        QualityInfo quality = analyzeImageQuality(bitmap);
        boolean passBrightness = quality.passBrightness;
        boolean passSharpness = quality.passSharpness;
        boolean qualityPass = passBrightness && passSharpness;

        if (!qualityPass) {
            String reason = buildRejectReason(false, passBrightness, passSharpness);
            return new ValidationResult(
                    false,
                    0.99f,
                    0f,
                    0f,
                    0f,
                    quality.meanBrightness,
                    quality.laplacianVariance,
                    false,
                    reason
            );
        }

        // 2) kiểm tra skin/non-skin bằng model
        float skinProb = predictSkinProbability(bitmap);
        boolean isSkin = skinProb >= SKIN_THRESHOLD;

        String reason = isSkin ? "OK" : "Khong phat hien du vung da";

        // confidence = độ chắc chắn
        float confidence = isSkin ? skinProb : (1f - skinProb);
        confidence = clamp(confidence, 0.50f, 0.99f);

        Log.d(TAG, "Skin probability = " + skinProb
                + ", isSkin = " + isSkin
                + ", confidence = " + confidence
                + ", meanBrightness = " + quality.meanBrightness
                + ", laplacianVariance = " + quality.laplacianVariance
                + ", reason = " + reason);

        return new ValidationResult(
                isSkin,
                confidence,
                skinProb,     // giữ tương thích
                skinProb,     // giữ tương thích
                skinProb,     // combinedScore = probability
                quality.meanBrightness,
                quality.laplacianVariance,
                true,
                reason
        );
    }

    private float predictSkinProbability(Bitmap bitmap) {
        ByteBuffer input = bitmapToByteBuffer(bitmap);

        float[][] output = new float[1][1];
        interpreter.run(input, output);

        float prob = output[0][0];

        if (Float.isNaN(prob) || Float.isInfinite(prob)) {
            throw new RuntimeException("Skin model output không hợp lệ: " + prob);
        }

        prob = clamp(prob, 0f, 1f);
        return prob;
    }

    /**
     * Tiền xử lý ảnh cho model skin/non-skin.
     *
     * Nếu model của bạn train với rescale=1./255 thì giữ /255f như dưới đây.
     * Nếu model train với 0..255 thì bỏ /255f.
     */
    private ByteBuffer bitmapToByteBuffer(Bitmap bitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, true);

        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * MODEL_INPUT_SIZE * MODEL_INPUT_SIZE * 3);
        buffer.order(ByteOrder.nativeOrder());
        buffer.rewind();

        int[] pixels = new int[MODEL_INPUT_SIZE * MODEL_INPUT_SIZE];
        resized.getPixels(pixels, 0, MODEL_INPUT_SIZE, 0, 0, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE);

        for (int pixel : pixels) {
            float r = ((pixel >> 16) & 0xFF) / 255.0f;
            float g = ((pixel >> 8) & 0xFF) / 255.0f;
            float b = (pixel & 0xFF) / 255.0f;

            buffer.putFloat(r);
            buffer.putFloat(g);
            buffer.putFloat(b);
        }

        buffer.rewind();
        return buffer;
    }

    // -----------------------------
    // GIỮ LẠI PHẦN KIỂM TRA CHẤT LƯỢNG ẢNH
    // -----------------------------

    private static class QualityInfo {
        final float meanBrightness;
        final float laplacianVariance;
        final boolean passBrightness;
        final boolean passSharpness;

        QualityInfo(float meanBrightness,
                    float laplacianVariance,
                    boolean passBrightness,
                    boolean passSharpness) {
            this.meanBrightness = meanBrightness;
            this.laplacianVariance = laplacianVariance;
            this.passBrightness = passBrightness;
            this.passSharpness = passSharpness;
        }
    }

    private QualityInfo analyzeImageQuality(Bitmap bitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, ANALYZE_SIZE, ANALYZE_SIZE, true);

        int width = resized.getWidth();
        int height = resized.getHeight();

        int[] pixels = new int[width * height];
        resized.getPixels(pixels, 0, width, 0, 0, width, height);

        int totalCount = 0;
        int darkCount = 0;
        int overexposedCount = 0;
        float brightnessSum = 0f;

        int[] gray = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                gray[y * width + x] = (r + g + b) / 3;
            }
        }

        for (int y = 0; y < height; y += 2) {
            for (int x = 0; x < width; x += 2) {
                int intensity = gray[y * width + x];
                brightnessSum += intensity;

                if (intensity < 25) darkCount++;
                if (intensity > 245) overexposedCount++;

                totalCount++;
            }
        }

        float meanBrightness = totalCount > 0 ? brightnessSum / totalCount : 0f;
        float darkRatio = totalCount > 0 ? (float) darkCount / totalCount : 1f;
        float overexposedRatio = totalCount > 0 ? (float) overexposedCount / totalCount : 1f;
        float laplacianVariance = computeLaplacianVariance(gray, width, height);

        boolean passBrightness = meanBrightness >= MIN_MEAN_BRIGHTNESS
                && meanBrightness <= MAX_MEAN_BRIGHTNESS
                && darkRatio <= MAX_DARK_PIXEL_RATIO
                && overexposedRatio <= MAX_OVEREXPOSED_RATIO;

        boolean passSharpness = laplacianVariance >= MIN_LAPLACIAN_VARIANCE;

        return new QualityInfo(
                meanBrightness,
                laplacianVariance,
                passBrightness,
                passSharpness
        );
    }

    private String buildRejectReason(boolean passSkinRatio, boolean passBrightness, boolean passSharpness) {
        if (!passBrightness) {
            return "Anh qua toi hoac qua sang";
        }
        if (!passSharpness) {
            return "Anh bi mo, vui long chup ro hon";
        }
        if (!passSkinRatio) {
            return "Khong phat hien du vung da";
        }
        return "OK";
    }

    private float computeLaplacianVariance(int[] gray, int width, int height) {
        double sum = 0.0;
        double sumSq = 0.0;
        int count = 0;

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int idx = y * width + x;

                int lap =
                        gray[idx - width] +
                                gray[idx - 1] -
                                4 * gray[idx] +
                                gray[idx + 1] +
                                gray[idx + width];

                sum += lap;
                sumSq += (double) lap * lap;
                count++;
            }
        }

        if (count == 0) return 0f;

        double mean = sum / count;
        double variance = (sumSq / count) - (mean * mean);
        return (float) variance;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public void close() {
        try {
            interpreter.close();
        } catch (Exception e) {
            Log.w(TAG, "Interpreter close failed", e);
        }
    }
}
