package com.example.testdesign

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max

class CameraActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var captureButton: FloatingActionButton
    private lateinit var backButton: FloatingActionButton
    private lateinit var helpButton: FloatingActionButton

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var classifier: TensorFlowLiteClassifier

    companion object {
        private const val TAG = "CameraActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        const val EXTRA_SCANNED_INGREDIENT = "scanned_ingredient"
        const val EXTRA_CONFIDENCE = "confidence"

        // ---- NEW: simple, strict gates (tune as needed) ----
        private const val ACCEPT_THRESHOLD = 0.55f     // try 0.55–0.65
        private const val MIN_MARGIN       = 0.15f     // top1 - top2
        private val REJECT_LABELS          = setOf("unknown")

        // Only accept labels you actually support in UI/recipes
        private val SUPPORTED_LABELS = setOf(
            "Calamansi",
            "Carrot", "Carrots",
            "Chayote",
            "Egg",
            "Eggplant",
            "Garlic",
            "Ginger",
            "Potato", "Potatoes",
            "Radish",
            "Red Onion",
            "Tomato", "Tomatoes",
            "White Onion"
        )

        private const val MSG_TRY_AGAIN = "Couldn't confidently identify that—try better lighting and fill the frame with one ingredient."
        private const val MSG_UNKNOWN   = "We couldn’t identify that ingredient. Please try again."
        private const val MSG_AMBIG     = "That looks too similar to another ingredient. Try a clearer shot."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        initViews()

        classifier = TensorFlowLiteClassifier(this)

        if (allPermissionsGranted()) startCamera()
        else ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)

        cameraExecutor = Executors.newSingleThreadExecutor()
        setupClickListeners()
    }

    private fun initViews() {
        previewView   = findViewById(R.id.preview_view)
        captureButton = findViewById(R.id.btn_capture)
        backButton    = findViewById(R.id.btn_back)
        helpButton    = findViewById(R.id.btn_help)
    }

    private fun setupClickListeners() {
        captureButton.setOnClickListener { takePhoto() }
        backButton.setOnClickListener { finish() }
        helpButton.setOnClickListener { showSnapTips() }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setTargetResolution(Size(224, 224))
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val outputStream = ByteArrayOutputStream()
        val opts = ImageCapture.OutputFileOptions.Builder(outputStream).build()

        imageCapture.takePicture(
            opts,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    Toast.makeText(this@CameraActivity, "Failed to capture image", Toast.LENGTH_SHORT).show()
                }
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val bytes = outputStream.toByteArray()
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    classifyImage(bmp)
                }
            }
        )
    }

    private fun classifyImage(bitmap: Bitmap) {
        cameraExecutor.execute {
            try {
                val results = classifier.classifyImage(bitmap) // already sorted high→low
                runOnUiThread {
                    if (results.isEmpty()) {
                        Toast.makeText(this@CameraActivity, MSG_TRY_AGAIN, Toast.LENGTH_LONG).show()
                        return@runOnUiThread
                    }

                    // Debug: see why something was accepted/rejected
                    val dbg = results.take(3).joinToString { "${it.title}:${"%.2f".format(it.confidence)}" }
                    Log.d(TAG, "Predictions: $dbg")

                    val top       = results[0]
                    val topLabel  = top.title.trim()
                    val topConf   = top.confidence
                    val secondConf = results.getOrNull(1)?.confidence ?: 0f
                    val margin    = topConf - secondConf

                    // Block explicit "Unknown"
                    if (REJECT_LABELS.contains(topLabel.lowercase())) {
                        Toast.makeText(this@CameraActivity, MSG_UNKNOWN, Toast.LENGTH_LONG).show()
                        return@runOnUiThread
                    }

                    // Confidence gate
                    if (topConf < ACCEPT_THRESHOLD) {
                        Toast.makeText(this@CameraActivity, MSG_TRY_AGAIN, Toast.LENGTH_LONG).show()
                        return@runOnUiThread
                    }

                    // Ambiguity gate
                    if (margin < MIN_MARGIN) {
                        Toast.makeText(this@CameraActivity, MSG_AMBIG, Toast.LENGTH_LONG).show()
                        return@runOnUiThread
                    }

                    // Whitelist gate
                    if (!isSupported(topLabel)) {
                        Toast.makeText(this@CameraActivity, MSG_TRY_AGAIN, Toast.LENGTH_LONG).show()
                        return@runOnUiThread
                    }

                    // Passed all gates → return result
                    val intent = Intent().apply {
                        putExtra(EXTRA_SCANNED_INGREDIENT, normalizeForUi(topLabel))
                        putExtra(EXTRA_CONFIDENCE, topConf)
                    }
                    setResult(RESULT_OK, intent)
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Classification error: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@CameraActivity, "Error processing image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ---- NEW: helpers ----
    private fun isSupported(raw: String): Boolean {
        SUPPORTED_LABELS.forEach { if (it.equals(raw, ignoreCase = true)) return true }
        return false
    }

    private fun normalizeForUi(raw: String): String = when {
        raw.equals("Carrots",  true) -> "Carrot"
        raw.equals("Potatoes", true) -> "Potato"
        raw.equals("Tomatoes", true) -> "Tomato"
        else -> raw
    }

    private fun showSnapTips() {
        try {
            Log.d(TAG, "Creating BottomSheetDialog")
            val dialog = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.sheet_snap_tips, null)
            dialog.setContentView(view)
            val button = view.findViewById<Button>(R.id.btnContinue)
            button?.setOnClickListener { dialog.dismiss() }
            dialog.show()
        } catch (e: Exception) {
            Log.e(TAG, "showSnapTips failed: ${e.message}", e)
            e.printStackTrace()
            Toast.makeText(this, "Tips: Fill square with one ingredient, well-lit and sharp. Avoid multiple items or being too close/far.", Toast.LENGTH_LONG).show()
        }
    }

    private fun allPermissionsGranted() =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission is required to scan ingredients.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::cameraExecutor.isInitialized) {
                cameraExecutor.shutdown()
            }
            if (::classifier.isInitialized) {
                classifier.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}
