package net.jitsi.sdktest

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.core.content.ContextCompat

class CameraHelper(private val context: Context, private val textureView: TextureView, private val cameraType: CameraType) {

    private var cameraDevice: CameraDevice? = null
    private lateinit var cameraCaptureSession: CameraCaptureSession
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private var cameraId: String? = null
    private var imageDimension: Size? = null
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null

    enum class CameraType {
        FRONT,
        BACK
    }

    fun openCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            try {
                cameraId = manager.cameraIdList.firstOrNull {
                    val characteristics = manager.getCameraCharacteristics(it)
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    facing == when (cameraType) {
                        CameraType.FRONT -> CameraCharacteristics.LENS_FACING_FRONT
                        CameraType.BACK -> CameraCharacteristics.LENS_FACING_BACK
                    }
                }

                val characteristics = manager.getCameraCharacteristics(cameraId!!)
                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) as StreamConfigurationMap
                imageDimension = map.getOutputSizes(SurfaceTexture::class.java)[0]

                if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textureView.setAspectRatio(imageDimension!!.width, imageDimension!!.height)
                } else {
                    textureView.setAspectRatio(imageDimension!!.height, imageDimension!!.width)
                }

                manager.openCamera(cameraId!!, stateCallback, null)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        } else {
            // Handle the case where the camera permission is not granted

        }
    }


    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice?.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice?.close()
            cameraDevice = null
        }
    }

    private fun createCameraPreview() {
        try {
            val texture = textureView.surfaceTexture
            if (texture != null) {
                texture.setDefaultBufferSize(imageDimension!!.width, imageDimension!!.height)
            }

            val surface = Surface(texture)

            captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder.addTarget(surface)

            cameraDevice?.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    if (cameraDevice == null) return
                    cameraCaptureSession = session
                    updatePreview()
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun updatePreview() {
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    fun closeCamera() {
        cameraDevice?.close()
        stopBackgroundThread()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("Camera Background")
        backgroundThread!!.start()
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun TextureView.setAspectRatio(width: Int, height: Int) {
        val viewWidth = width
        val viewHeight = height
        if (viewWidth > 0 && viewHeight > 0) {
            val newWidth: Int
            val newHeight: Int
            if (viewWidth > viewHeight) {
                newWidth = this.width
                newHeight = viewHeight * this.width / viewWidth
            } else {
                newWidth = viewWidth * this.height / viewHeight
                newHeight = this.height
            }
            val parent = this.parent as ViewGroup
            parent.setPadding(
                (parent.width - newWidth) / 2,
                (parent.height - newHeight) / 2,
                (parent.width - newWidth) / 2,
                (parent.height - newHeight) / 2
            )
            val layoutParams = this.layoutParams
            layoutParams.width = newWidth
            layoutParams.height = newHeight
            this.layoutParams = layoutParams
        }
    }

    init {
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture) = true

            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                startBackgroundThread()
                openCamera()
            }
        }
    }
}