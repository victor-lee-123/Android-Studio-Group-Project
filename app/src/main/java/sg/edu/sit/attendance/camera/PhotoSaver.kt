package sg.edu.sit.attendance.camera

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

object PhotoSaver {

    fun createOutputFile(context: Context): File {
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        return File(dir, "ATT_$name.jpg")
    }

    fun takePhoto(
        context: Context,
        imageCapture: ImageCapture,
        onSaved: (Uri) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val file = createOutputFile(context)
        val output = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture.takePicture(
            output,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val uri = outputFileResults.savedUri ?: Uri.fromFile(file)
                    onSaved(uri)
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    }
}