package com.capacitorjs.plugins.camera

import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import android.content.Context
import android.net.Uri
import java.io.File
import androidx.core.net.toUri

object IonCameraUtils {


    fun getTempImage(context: Context, uri: Uri): Uri? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            inputStream.use {
                saveImage(context, uri, it)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun saveImage(context: Context, uri: Uri, inputStream: InputStream): Uri {
        var outFile: File = if (uri.scheme == "content") {
            getTempFile(context, uri)
        } else {
            uri.path?.let { File(it) } ?: getTempFile(context, uri)
        }

        try {
            writePhoto(outFile, inputStream)
        } catch (_: FileNotFoundException) {
            outFile = getTempFile(context, uri)
            writePhoto(outFile, inputStream)
        }

        return Uri.fromFile(outFile)
    }

    private fun writePhoto(outFile: File, input: InputStream) {
        FileOutputStream(outFile).use { output ->
            input.copyTo(output)
        }
    }

    private fun getTempFile(context: Context, uri: Uri): File {
        var filename = Uri.decode(uri.toString()).toUri().lastPathSegment

        if (!filename!!.contains(".jpg") && !filename.contains(".jpeg")) {
            filename += "." + System.currentTimeMillis() + ".jpeg"
        }

        return File(context.cacheDir, filename)
    }

}