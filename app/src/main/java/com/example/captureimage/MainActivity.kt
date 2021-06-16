package com.example.captureimage

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.nfc.Tag
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.impl.ImageAnalysisConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var detector: FaceDetector
    private var cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        setContentView(R.layout.activity_main)


        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Set up the listener for take photo button
        camera_capture_button.setOnClickListener { takePhoto() }

        //Flip camera
        switch_camera.setOnClickListener {

            cameraSelector = if(cameraSelector== CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            startCamera()}

        //setting Activity
        val settingButton = findViewById<ImageView>(R.id.setting_Preferences)
        settingButton.setOnClickListener{
           Toast.makeText(this, "Set your Preference", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
        }

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()

        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        detector = FaceDetection.getClient(highAccuracyOpts)


    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }




    @SuppressLint("UnsafeOptInUsageError")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()


            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            // Preferences values
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val isSmilingModeOn = sharedPreferences.getBoolean(getString(R.string.setting_picture_click_by_smiling), false)
            val isBlinkingModeOn = sharedPreferences.getBoolean(getString(R.string.setting_picture_click_by_blinking_eye), false)
            if(isBlinkingModeOn && !isSmilingModeOn)
            {
                Toast.makeText(this, "Blinking Mode On ", Toast.LENGTH_SHORT).show()
            }
            else if(isSmilingModeOn && !isBlinkingModeOn)
            {
                Toast.makeText(this, "Smiling Mode On", Toast.LENGTH_SHORT).show()
            }
            else if(!isSmilingModeOn && !isBlinkingModeOn)
            {
                Toast.makeText(this, "Both Mode off", Toast.LENGTH_SHORT).show()
            }
            else if(isSmilingModeOn && isBlinkingModeOn)
            {
                Toast.makeText(this, "Both Mode on", Toast.LENGTH_SHORT).show()
            }


            imageCapture = ImageCapture.Builder()
                .build()

            // Select back camera as a default

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), { imageProxy ->

                val rotationalDegree = imageProxy.imageInfo.rotationDegrees
                val mediaImage = imageProxy.image
                if(mediaImage!=null)
                {
                    val image = InputImage.fromMediaImage(mediaImage, rotationalDegree)

                    detector.process(image)
                        .addOnSuccessListener { faces ->

                            for(face in faces)
                            {
//                                Log.i(TAG, "Smiling Probability: ${face.smilingProbability}")
//                                Log.i(TAG, "Left eye open Probability: ${face.leftEyeOpenProbability}")
//                                Log.i(TAG, "Right eye open Probability: ${face.rightEyeOpenProbability}")

                                if(isSmilingModeOn && !isBlinkingModeOn)
                                {
                                    if(face.smilingProbability!=null)
                                    {
                                        val smilingProb = face.smilingProbability
                                        if(smilingProb>0.5)
                                        {
                                            takePhoto()
                                        }
                                    }
                                }
                                else if(!isSmilingModeOn && isBlinkingModeOn)
                                {
                                    if(face.leftEyeOpenProbability!=null && face.rightEyeOpenProbability!=null)
                                    {
                                        if(face.leftEyeOpenProbability<0.4 || face.rightEyeOpenProbability<0.4)
                                        {
                                            takePhoto()
                                        }
                                    }
                                }
                                else if(isSmilingModeOn && isBlinkingModeOn)
                                {
                                    if(face.leftEyeOpenProbability!=null && face.rightEyeOpenProbability!=null && face.smilingProbability!=null)
                                    {
                                        if(face.smilingProbability>0.5 || face.leftEyeOpenProbability<0.4 || face.rightEyeOpenProbability<0.4)
                                        {
                                            takePhoto()
                                        }

                                    }
                                }

                            }
                            imageProxy.close()
                        }
                        .addOnFailureListener{ e ->
                            Log.e(TAG, "Error in detecting image: ${e.message}")
                            imageProxy.close()
                        }

                }


            })

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalysis)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            })
    }



    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRestart() {
        super.onRestart()
        startCamera()
    }


    companion object {
        private const val TAG = "MainActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }


}