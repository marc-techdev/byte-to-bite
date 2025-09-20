package com.example.testdesign

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TensorFlowLiteClassifier(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private var inputImageWidth: Int = 0
    private var inputImageHeight: Int = 0
    private var modelInputSize: Int = 0

    companion object {
        private const val TAG = "TensorFlowLiteClassifier"
        private const val MODEL_PATH = "model.tflite"
        private const val LABELS_PATH = "labels.txt"
        private const val INPUT_SIZE = 224 // Default size for most models from Teachable Machine
        private const val PIXEL_SIZE = 3 // RGB
        private const val IMAGE_MEAN = 127.5f
        private const val IMAGE_STD = 127.5f
    }

    init {
        try {
            loadModel()
            loadLabels()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing classifier: ${e.message}")
        }
    }

    private fun loadModel() {
        try {
            val modelBuffer = loadModelFile()
            val options = Interpreter.Options()
            interpreter = Interpreter(modelBuffer, options)

            // Get input tensor dimensions
            val inputShape = interpreter?.getInputTensor(0)?.shape()
            inputShape?.let {
                inputImageWidth = it[1]
                inputImageHeight = it[2]
                modelInputSize = inputImageWidth * inputImageHeight * PIXEL_SIZE
            }

            Log.d(TAG, "Model loaded successfully. Input size: ${inputImageWidth}x${inputImageHeight}")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model: ${e.message}")
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(MODEL_PATH)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadLabels() {
        try {
            labels = context.assets.open(LABELS_PATH).bufferedReader().useLines { lines ->
                lines.map { it.trim() }.toList()
            }
            Log.d(TAG, "Labels loaded: ${labels.size} classes")
        } catch (e: IOException) {
            Log.e(TAG, "Error loading labels: ${e.message}")
        }
    }

    fun classifyImage(bitmap: Bitmap): List<Recognition> {
        if (interpreter == null || labels.isEmpty()) {
            Log.e(TAG, "Classifier not properly initialized")
            return emptyList()
        }

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputImageWidth, inputImageHeight, true)
        val input = convertBitmapToByteBuffer(resizedBitmap)
        val output = Array(1) { FloatArray(labels.size) }

        try {
            interpreter?.run(input, output)
            return getSortedResult(output[0])
        } catch (e: Exception) {
            Log.e(TAG, "Error during inference: ${e.message}")
            return emptyList()
        }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * modelInputSize)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputImageWidth * inputImageHeight)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF)
            val g = ((pixel shr 8) and 0xFF)
            val b = (pixel and 0xFF)

            // Normalize pixel values to [-1, 1] (typical for Teachable Machine models)
            byteBuffer.putFloat((r - IMAGE_MEAN) / IMAGE_STD)
            byteBuffer.putFloat((g - IMAGE_MEAN) / IMAGE_STD)
            byteBuffer.putFloat((b - IMAGE_MEAN) / IMAGE_STD)
        }

        return byteBuffer
    }

    private fun getSortedResult(output: FloatArray): List<Recognition> {
        val recognitions = mutableListOf<Recognition>()

        for (i in output.indices) {
            val confidence = output[i]
            if (confidence > 0.01f) { // Only include results with >1% confidence
                recognitions.add(
                    Recognition(
                        id = i.toString(),
                        title = if (i < labels.size) labels[i] else "Unknown",
                        confidence = confidence
                    )
                )
            }
        }

        return recognitions.sortedByDescending { it.confidence }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }

    data class Recognition(
        val id: String,
        val title: String,
        val confidence: Float
    ) {
        fun getConfidencePercentage(): Int {
            return (confidence * 100).toInt()
        }
    }
}