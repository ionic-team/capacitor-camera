package com.capacitorjs.plugins.camera


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.capacitorjs.plugins.camera.IonCameraSettings.Companion.DEFAULT_CORRECT_ORIENTATION
import com.capacitorjs.plugins.camera.IonCameraSettings.Companion.DEFAULT_QUALITY
import com.getcapacitor.Bridge
import com.getcapacitor.FileUtils
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.PermissionState
import com.getcapacitor.PluginCall
import io.ionic.libs.ioncameralib.helper.IONCAMRExifHelper
import io.ionic.libs.ioncameralib.helper.IONCAMRFileHelper
import io.ionic.libs.ioncameralib.helper.IONCAMRImageHelper
import io.ionic.libs.ioncameralib.helper.IONCAMRMediaHelper
import io.ionic.libs.ioncameralib.manager.IONCAMRCameraManager
import io.ionic.libs.ioncameralib.manager.IONCAMREditManager
import io.ionic.libs.ioncameralib.manager.IONCAMRGalleryManager
import io.ionic.libs.ioncameralib.manager.IONCAMRVideoManager
import io.ionic.libs.ioncameralib.model.IONCAMRCameraParameters
import io.ionic.libs.ioncameralib.model.IONCAMREditParameters
import io.ionic.libs.ioncameralib.model.IONCAMRVideoParameters
import io.ionic.libs.ioncameralib.model.IONCAMRError
import io.ionic.libs.ioncameralib.model.IONCAMRMediaResult
import io.ionic.libs.ioncameralib.model.IONCAMRMediaType
import io.ionic.libs.ioncameralib.view.IONCAMRImageEditorActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class IonCameraFlow(
    private val context: Context,
    private val activity: AppCompatActivity,
    private val bridge: Bridge,
    private val appId: String,
    private val permissionHelper: PermissionHelper
) {
    private var isFirstRequest = true
    private var cameraManager: IONCAMRCameraManager? = null
    private var videoManager: IONCAMRVideoManager? = null
    private var editManager: IONCAMREditManager? = null
    private var galleryManager: IONCAMRGalleryManager? = null
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraCropLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryCropLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var videoLauncher: ActivityResultLauncher<Intent>
    private lateinit var editLauncher: ActivityResultLauncher<Intent>
    private var currentCall: PluginCall? = null
    private var cameraSettings: IonCameraSettings? = null
    private var gallerySettings: IonGallerySettings? = null
    private var editParameters = IONCAMREditParameters(
        editURI = "", fromUri = false, saveToGallery = false, includeMetadata = false
    )
    private var videoParameters: IONCAMRVideoParameters? = null
    private var lastEditUri: String? = null

    companion object {
        private const val AUTHORITY = ".camera.provider"
        private const val CAMERA = "camera"
        private const val SAVE_GALLERY = "saveGallery"
        private const val STORE = "CameraStore"
        private const val EDIT_FILE_NAME_KEY = "EditFileName"
        private const val ERROR_FORMAT_PREFIX = "OS-PLUG-CAMR-"
        private const val MEDIA_TYPE_PHOTO = 0
    }

    fun load() {
        setupLaunchers()
        cameraManager = IONCAMRCameraManager(
            appId,
            IONCAMRExifHelper(),
            IONCAMRFileHelper(),
            IONCAMRMediaHelper(),
            IONCAMRImageHelper()
        )

        videoManager = IONCAMRVideoManager(
            IONCAMRFileHelper(),
        )

        galleryManager = IONCAMRGalleryManager(
            IONCAMRExifHelper(),
            IONCAMRFileHelper(),
            IONCAMRMediaHelper(),
            IONCAMRImageHelper()
        )

        editManager = IONCAMREditManager(
            appId,
            IONCAMRExifHelper(),
            IONCAMRFileHelper(),
            IONCAMRMediaHelper(),
            IONCAMRImageHelper()
        )

        cameraManager?.deleteVideoFilesFromCache(activity)
    }

    fun takePhoto(call: PluginCall) {
        cameraSettings = getCameraSettings(call)
        currentCall = call
        showCamera(call)
    }

    fun recordVideo(call: PluginCall) {
        videoParameters = getVideoSettings(call)
        currentCall = call
        openRecordVideo(call)
    }

    fun playVideo(call: PluginCall) {
        currentCall = call
        openPlayVideo(call)
    }

    fun chooseFromGallery(call: PluginCall) {
        gallerySettings = getGallerySettings(call)
        currentCall = call
        openGallery(call)
    }

    fun editPhoto(call: PluginCall) {
        currentCall = call
        callEditPhoto(call)
    }

    fun editURIPhoto(call: PluginCall) {
        currentCall = call
        callEditURIPhoto(call)
    }

    // ----------------------------------------------------
    // Launchers
    // ----------------------------------------------------
    private fun setupLaunchers() {
        cameraLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleCameraResult(result)
        }

        cameraCropLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleCameraCropResult(result)
        }

        videoLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleVideoResult(result)
        }

        galleryLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleGalleryResult(result)
        }

        galleryCropLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleGalleryCropResult(result)
        }

        editLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleEditResult(result)
        }

    }

    fun getVideoSettings(call: PluginCall): IONCAMRVideoParameters {
        return IONCAMRVideoParameters(
            saveToGallery = call.getBoolean("saveToGallery") ?: false,
            includeMetadata = call.getBoolean("includeMetadata") ?: false,
            isPersistent = call.getBoolean("isPersistent") ?: true
        )
    }

    fun getGallerySettings(call: PluginCall): IonGallerySettings {
        return IonGallerySettings(
            mediaType = IONCAMRMediaType.fromValue((call.getInt("mediaType") ?: 0)),
            allowMultipleSelection = call.getBoolean("allowMultipleSelection") ?: false,
            limit = call.getInt("limit") ?: 0,
            includeMetadata = call.getBoolean("includeMetadata") ?: false,
            editable = IonEditableMode.fromString(call.getString("editable")),
            quality = call.getInt("quality") ?: DEFAULT_QUALITY,
            width = call.getInt("targetWidth") ?: 0,
            height = call.getInt("targetHeight") ?: 0,
            correctOrientation = call.getBoolean("correctOrientation") ?:  DEFAULT_CORRECT_ORIENTATION
        )
    }

    data class IonGallerySettings (
        var mediaType: IONCAMRMediaType = IONCAMRMediaType.ALL,
        var allowMultipleSelection: Boolean = false,
        var limit: Int = 0,
        var includeMetadata: Boolean = false,
        var editable: IonEditableMode = IonEditableMode.NO,
        var quality: Int = 90,
        var width: Int = 0,
        var height: Int = 0,
        var correctOrientation: Boolean = true
    )

    fun getCameraSettings(call: PluginCall): IonCameraSettings {
        val settings = IonCameraSettings()
        settings.quality = call.getInt("quality") ?: IonCameraSettings.DEFAULT_QUALITY

        val width = call.getInt("targetWidth") ?: 0
        val height = call.getInt("targetHeight") ?: 0

        settings.targetWidth = if (width < 1) -1 else width
        settings.targetHeight = if (height < 1) -1 else height
        settings.correctOrientation = call.getBoolean("correctOrientation") ?: IonCameraSettings.DEFAULT_CORRECT_ORIENTATION
        settings.encodingType = call.getInt("encodingType") ?: IonCameraSettings.DEFAULT_ENCODING_TYPE
        settings.saveToGallery = call.getBoolean("saveToGallery") ?: IonCameraSettings.DEFAULT_SAVE_IMAGE_TO_GALLERY
        settings.editable = IonEditableMode.fromString(call.getString("editable"))
        settings.includeMetadata = call.getBoolean("includeMetadata") ?: false
        settings.shouldResize = settings.targetWidth > 0 || settings.targetHeight > 0
        return settings
    }


    private fun showCamera(call: PluginCall) {
        if (!context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        ) {
            sendError(IONCAMRError.NO_CAMERA_AVAILABLE_ERROR)
            return
        }
        openCamera(call)
    }

    fun openCamera(call: PluginCall) {

        val settings = cameraSettings ?: run {
            sendError(IONCAMRError.INVALID_ARGUMENT_ERROR)
            return
        }
        if (checkCameraPermissions(call, settings.saveToGallery)) {
            try {
                val manager = cameraManager ?: run {
                    sendError(IONCAMRError.CONTEXT_ERROR)
                    return
                }
                currentCall = call
                manager.takePhoto(activity, settings.encodingType, cameraLauncher)
            } catch (ex: Exception) {
                sendError(IONCAMRError.TAKE_PHOTO_ERROR)
            }
        }
    }

    fun openRecordVideo(call: PluginCall) {
        val settings = videoParameters ?: run {
            sendError(IONCAMRError.INVALID_ARGUMENT_ERROR)
            return
        }

        if (checkCameraPermissions(call, settings.saveToGallery)) {
            try {
                val manager = cameraManager ?: run {
                    sendError(IONCAMRError.CONTEXT_ERROR)
                    return
                }
                currentCall = call
                manager.recordVideo(
                    activity,
                    settings.saveToGallery,
                    videoLauncher
                ) {
                    sendError(it)
                }
            } catch (ex: Exception) {
                sendError(IONCAMRError.CAPTURE_VIDEO_ERROR)
            }
        }
    }

    private fun openPlayVideo(call: PluginCall) {
        try {
            val manager = videoManager ?: run {
                sendError(IONCAMRError.CONTEXT_ERROR)
                return
            }

            val videoUri = call.getString("uri")
                ?: return sendError(IONCAMRError.PLAY_VIDEO_GENERAL_ERROR)
            manager.playVideo(activity, videoUri, {
                call.resolve()
            }, {
                sendError(it)
            })
        } catch (_: Exception) {
            sendError(IONCAMRError.PLAY_VIDEO_GENERAL_ERROR)
            return
        }
    }

    private fun openGallery(call: PluginCall) {
        if (!checkGalleryPermissions(call)) return

        val manager = galleryManager ?: run {
            sendError(IONCAMRError.CONTEXT_ERROR)
            return
        }

        val settings = gallerySettings ?: run {
            sendError(IONCAMRError.INVALID_ARGUMENT_ERROR)
            return
        }

        manager.chooseFromGallery(
            activity,
            settings.mediaType,
            settings.allowMultipleSelection,
            settings.limit,
            galleryLauncher
        )
    }

    private fun callEditPhoto(call: PluginCall) {
        val manager = editManager ?: run {
            sendError(IONCAMRError.CONTEXT_ERROR)
            return
        }

        editParameters = IONCAMREditParameters(
            "",
            fromUri = false,
            saveToGallery = false,
            includeMetadata = false
        )

        val imageBase64 = call.getString("inputImage")
        if (imageBase64.isNullOrEmpty()) {
            sendError(IONCAMRError.INVALID_ARGUMENT_ERROR)
            return
        }
        manager.editImage(activity, imageBase64, editLauncher)
    }

    private fun callEditURIPhoto(call: PluginCall) {
        val manager = editManager ?: run {
            sendError(IONCAMRError.CONTEXT_ERROR)
            return
        }

        val photoPath = call.getString("uri")
        val saveToGallery = call.getBoolean("saveToGallery") ?: false
        val includeMetadata = call.getBoolean("includeMetadata") ?: false
        if (photoPath == null) return

        editParameters = IONCAMREditParameters(
            editURI = photoPath,
            fromUri = true,
            saveToGallery = saveToGallery,
            includeMetadata = includeMetadata
        )

        manager.editURIPicture(activity, photoPath, editLauncher) {
            sendError(IONCAMRError.EDIT_IMAGE_ERROR)
        }
    }

    private fun handleCameraResult(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val settings = cameraSettings ?: run {
                    sendError(IONCAMRError.INVALID_ARGUMENT_ERROR)
                    return
                }
                when (settings.editable) {
                    IonEditableMode.IN_APP -> {
                        editPhoto()
                    }
                    IonEditableMode.EXTERNAL -> {
                        val editor = editManager ?: run {
                            sendError(IONCAMRError.CONTEXT_ERROR)
                            return
                        }

                        val appId = appId
                        val tmpFile = FileProvider.getUriForFile(
                            activity,
                            "$appId$AUTHORITY",
                            editor.createCaptureFile(
                                activity,
                                settings.encodingType,
                                activity.getSharedPreferences(
                                    STORE,
                                    Context.MODE_PRIVATE
                                ).getString(EDIT_FILE_NAME_KEY, "") ?: ""
                            )
                        )

                        val editIntent = createEditIntent(tmpFile)
                        if (editIntent != null) {
                            cameraCropLauncher.launch(editIntent)
                        } else {
                            editPhoto()
                        }
                    }
                    IonEditableMode.NO -> {
                        processResult(result.data)
                    }
                }
            }
            Activity.RESULT_CANCELED -> {
                sendError(IONCAMRError.NO_PICTURE_TAKEN_ERROR)
            }
            else -> {
                sendError(IONCAMRError.TAKE_PHOTO_ERROR)
            }
        }
    }

    private fun handleVideoResult(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                processResultFromVideo(result)
            }
            Activity.RESULT_CANCELED -> {
                sendError(IONCAMRError.CAPTURE_VIDEO_CANCELLED_ERROR)
            }
            else -> sendError(IONCAMRError.CAPTURE_VIDEO_ERROR)
        }
    }

    private fun handleGalleryResult(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val editor = editManager ?: run {
                    sendError(IONCAMRError.CONTEXT_ERROR)
                    return
                }

                val manager = galleryManager ?: run {
                    sendError(IONCAMRError.CONTEXT_ERROR)
                    return
                }

                val settings = gallerySettings ?: run {
                    sendError(IONCAMRError.INVALID_ARGUMENT_ERROR)
                    return
                }

                val uris = manager.extractUris(result.data)

                if (uris.isEmpty()) {
                    sendError(IONCAMRError.GENERIC_CHOOSE_MULTIMEDIA_ERROR)
                    return
                }

                if (settings.editable != IonEditableMode.NO && uris.size == 1 && settings.mediaType == IONCAMRMediaType.PICTURE) {
                    val originalUri = uris.first()
                    when (settings.editable) {
                        IonEditableMode.IN_APP -> {
                            editor.openCropActivity(
                                activity,
                                originalUri,
                                galleryCropLauncher
                            )
                        }
                        IonEditableMode.EXTERNAL -> {
                            val tempUri = if (originalUri.scheme == "content") {
                                IonCameraUtils.getGalleryTempImage(activity, originalUri)
                            } else {
                                originalUri
                            }

                            if (tempUri == null) {
                                sendError(IONCAMRError.EDIT_IMAGE_ERROR)
                                return
                            }

                            val editIntent = createEditIntent(tempUri)
                            if (editIntent != null) {
                                galleryCropLauncher.launch(editIntent)
                            } else {
                                editor.openCropActivity(
                                    activity,
                                    originalUri,
                                    galleryCropLauncher
                                )
                            }
                        }
                        else -> {}
                    }
                } else {
                    processResultFromGallery(result)
                }
            }
            Activity.RESULT_CANCELED -> {
                sendError(IONCAMRError.CHOOSE_MULTIMEDIA_CANCELLED_ERROR)
            }
            else -> sendError(IONCAMRError.GENERIC_CHOOSE_MULTIMEDIA_ERROR)
        }
    }

    private fun handleGalleryCropResult(result: ActivityResult) {
        val settings = gallerySettings ?: run {
            sendError(IONCAMRError.INVALID_ARGUMENT_ERROR)
            return
        }

        when (result.resultCode) {
            Activity.RESULT_OK -> {
                var intent = result.data
                val resultPath = intent?.getStringExtra(IONCAMRImageEditorActivity.IMAGE_OUTPUT_URI_EXTRAS)

                if (resultPath.isNullOrEmpty()) {
                    if (lastEditUri.isNullOrEmpty()) {
                        sendError(IONCAMRError.EDIT_IMAGE_ERROR)
                        return
                    }
                    intent = Intent().apply {
                        putExtra(IONCAMRImageEditorActivity.IMAGE_OUTPUT_URI_EXTRAS, lastEditUri)
                    }
                }
                processResultEditFromGallery(intent)
                lastEditUri = null
            }
            Activity.RESULT_CANCELED -> {
                if (settings.editable == IonEditableMode.EXTERNAL && !lastEditUri.isNullOrEmpty()) {
                    val intent = Intent().apply {
                        putExtra(IONCAMRImageEditorActivity.IMAGE_OUTPUT_URI_EXTRAS, lastEditUri)
                    }
                    processResultEditFromGallery(intent)
                } else {
                    lastEditUri = null
                    sendError(IONCAMRError.EDIT_CANCELLED_ERROR)
                }
            }
            else -> sendError(IONCAMRError.EDIT_IMAGE_ERROR)
        }
    }

    private fun processResultEditFromGallery(intent: Intent) {
        val manager = galleryManager ?: run {
            sendError(IONCAMRError.CONTEXT_ERROR)
            return
        }

        val settings = gallerySettings ?: run {
            sendError(IONCAMRError.INVALID_ARGUMENT_ERROR)
            return
        }

        CoroutineScope(Dispatchers.Default).launch {
            manager.onChooseFromGalleryEditResult(
                activity,
                Activity.RESULT_OK,
                intent,
                settings.includeMetadata,
                { handleGalleryMediaResults(it) },
                { sendError(it) }
            )
        }
    }

    private fun handleEditResult(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> processResultFromEdit(result)
            Activity.RESULT_CANCELED -> sendError(IONCAMRError.EDIT_CANCELLED_ERROR)
            else -> sendError(IONCAMRError.EDIT_IMAGE_ERROR)
        }
    }

    private fun editPhoto() {
        val editor = editManager ?: run {
            sendError(IONCAMRError.CONTEXT_ERROR)
            return
        }

        val settings = cameraSettings ?: run {
            sendError(IONCAMRError.INVALID_ARGUMENT_ERROR)
            return
        }

        val appId = appId
        val tmpFile = FileProvider.getUriForFile(
            activity,
            "$appId$AUTHORITY",
            editor.createCaptureFile(
                activity,
                settings.encodingType,
                activity.getSharedPreferences(
                    STORE,
                    Context.MODE_PRIVATE
                ).getString(EDIT_FILE_NAME_KEY, "") ?: ""
            )
        )

        editor.openCropActivity(
            activity,
            tmpFile,
            cameraCropLauncher
        )
    }

    private fun createEditIntent(origPhotoUri: Uri): Intent? {
        return try {

            var editUri = origPhotoUri
            if (origPhotoUri.scheme == "file") {
                val editFile = File(origPhotoUri.path!!)
                editUri = FileProvider.getUriForFile(
                    activity,
                    context.packageName + AUTHORITY,
                    editFile
                )
                lastEditUri = editFile.absolutePath
            } else if (origPhotoUri.scheme == "content") {
                val tempUri = IonCameraUtils.getCameraTempImage(activity, origPhotoUri) ?: return null
                val editFile = File(tempUri.path!!)
                editUri = FileProvider.getUriForFile(
                    activity,
                    context.packageName + AUTHORITY,
                    editFile
                )
                lastEditUri = editFile.absolutePath
            }

            val editIntent = Intent(Intent.ACTION_EDIT)
            editIntent.setDataAndType(editUri, "image/*")
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            editIntent.addFlags(flags)
            editIntent.putExtra(MediaStore.EXTRA_OUTPUT, editUri)

            val resInfoList: MutableList<ResolveInfo>?

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                resInfoList = context
                    .packageManager
                    .queryIntentActivities(editIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
            } else {
                resInfoList = legacyQueryIntentActivities(editIntent)
            }

            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(packageName, editUri, flags)
            }

            editIntent
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("deprecation")
    private fun legacyQueryIntentActivities(intent: Intent): MutableList<ResolveInfo> {
        return context.packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }

    private fun handleCameraCropResult(result: ActivityResult) {
        val settings = cameraSettings ?: run {
            sendError(IONCAMRError.INVALID_ARGUMENT_ERROR)
            return
        }

        when (result.resultCode) {
            Activity.RESULT_OK -> {
                var intent = result.data
                val resultPath = intent?.getStringExtra(IONCAMRImageEditorActivity.IMAGE_OUTPUT_URI_EXTRAS)

                if (resultPath.isNullOrEmpty()) {
                    if (lastEditUri.isNullOrEmpty()) {
                        sendError(IONCAMRError.EDIT_IMAGE_ERROR)
                        return
                    }
                    intent = Intent().apply {
                        putExtra(IONCAMRImageEditorActivity.IMAGE_OUTPUT_URI_EXTRAS, lastEditUri)
                    }
                }

                processResult(intent)
                lastEditUri = null
            }
            Activity.RESULT_CANCELED -> {
                if (settings.editable == IonEditableMode.EXTERNAL && !lastEditUri.isNullOrEmpty()) {
                    val intent = Intent().apply {
                        putExtra(IONCAMRImageEditorActivity.IMAGE_OUTPUT_URI_EXTRAS, lastEditUri)
                    }
                    processResult(intent)
                } else {
                    lastEditUri = null
                    sendError(IONCAMRError.EDIT_CANCELLED_ERROR)
                }
            }
            else -> sendError(IONCAMRError.EDIT_IMAGE_ERROR)
        }
    }

    private fun handleEditBase64Result(image: String) {
        val ret = JSObject()
        ret.put("outputImage", image)
        currentCall?.resolve(ret)
        currentCall = null
    }

    private fun handleMediaResult(mediaResult: IONCAMRMediaResult) {
        val file = File(mediaResult.uri)
        val uri = Uri.fromFile(file)
        val bitmap = BitmapFactory.decodeFile(mediaResult.uri)
        if (bitmap == null) {
            sendError(IONCAMRError.PROCESS_IMAGE_ERROR)
            return
        }

        val exif = ImageUtils.getExifData(context, bitmap, uri)
        val ret = JSObject()
        ret.put("type", mediaResult.type)
        ret.put("uri", mediaResult.uri)
        ret.put("thumbnail", mediaResult.thumbnail)
        ret.put("webPath", FileUtils.getPortablePath(context, bridge.localUrl, uri))
        ret.put("saved", mediaResult.saved)

        mediaResult.metadata?.let {
            val metadata = JSObject()
            metadata.put("duration", it.duration)
            metadata.put("size", it.size)
            metadata.put("format", it.format)
            metadata.put("resolution", it.resolution)
            metadata.put("creationDate", it.creationDate)
            metadata.put("exif", exif.toJson())
            ret.put("metadata", metadata)
        }

        currentCall?.resolve(ret)
        currentCall = null
        lastEditUri = null
    }

    private fun handleVideoMediaResult(mediaResult: IONCAMRMediaResult) {
        val file = File(mediaResult.uri)
        val uri = Uri.fromFile(file)

        val ret = JSObject()
        ret.put("type", mediaResult.type)
        ret.put("uri", mediaResult.uri)
        ret.put("thumbnail", mediaResult.thumbnail)
        ret.put("webPath", FileUtils.getPortablePath(context, bridge.localUrl, uri))
        ret.put("saved", mediaResult.saved)

        mediaResult.metadata?.let {
            val metadata = JSObject()
            metadata.put("duration", it.duration)
            metadata.put("size", it.size)
            metadata.put("format", it.format)
            metadata.put("resolution", it.resolution)
            metadata.put("creationDate", it.creationDate)
            ret.put("metadata", metadata)
        }

        currentCall?.resolve(ret)
        currentCall = null
    }

    private fun handleGalleryMediaResults(results: List<IONCAMRMediaResult>) {
        val photos = JSArray()
        results.forEach { mediaResult ->
            val file = File(mediaResult.uri)
            val uri = Uri.fromFile(file)

            val ret = JSObject()
            ret.put("type", mediaResult.type)
            ret.put("uri", mediaResult.uri)
            ret.put("thumbnail", mediaResult.thumbnail)
            ret.put("saved", mediaResult.saved)
            ret.put(
                "webPath",
                FileUtils.getPortablePath(context, bridge.localUrl, uri)
            )

            mediaResult.metadata?.let {
                val metadata = JSObject()
                metadata.put("duration", it.duration)
                metadata.put("size", it.size)
                metadata.put("format", it.format)
                metadata.put("resolution", it.resolution)
                metadata.put("creationDate", it.creationDate)

                if (mediaResult.type == IONCAMRMediaType.PICTURE.type) {
                    val bitmap = BitmapFactory.decodeFile(mediaResult.uri)
                    if (bitmap == null) {
                        sendError(IONCAMRError.PROCESS_IMAGE_ERROR)
                        return
                    }

                    val exif = ImageUtils.getExifData(context, bitmap, uri)
                    metadata.put("exif", exif.toJson())
                }

                ret.put("metadata", metadata)
            }
            photos.put(ret)
        }

        val ret = JSObject()
        ret.put("results", photos)
        currentCall?.resolve(ret)
        currentCall = null
        lastEditUri = null
    }

    private fun processResult(intent: Intent?) {
        val manager = cameraManager ?: run {
            sendError(IONCAMRError.CONTEXT_ERROR)
            return
        }

        val settings = cameraSettings ?: run {
            sendError(IONCAMRError.INVALID_ARGUMENT_ERROR)
            return
        }
        val ionParams = settings.toIonParameters()
        manager.processResultFromCamera(
            activity,
            intent,
            ionParams,
            { mediaResult ->
                handleMediaResult(mediaResult)
            },
            { error ->
                sendError(error)
            }
        )
    }

    private fun processResultFromVideo(result: ActivityResult) {
        val manager = cameraManager ?: run {
            sendError(IONCAMRError.CONTEXT_ERROR)
            return
        }
        var uri = result.data?.data
        if (uri == null) {
            val fromPreferences =
                activity.getSharedPreferences(STORE, Context.MODE_PRIVATE)
                    .getString(STORE, "")
            fromPreferences.let { uri = Uri.parse(fromPreferences) }
        }
        if (activity == null) {
            sendError(IONCAMRError.CAPTURE_VIDEO_ERROR)
            return
        }
        val settings = videoParameters ?: run {
            sendError(IONCAMRError.INVALID_ARGUMENT_ERROR)
            return
        }

        CoroutineScope(Dispatchers.Default).launch {
            manager.processResultFromVideo(
                activity,
                uri,
                settings.saveToGallery,
                settings.isPersistent,
                settings.includeMetadata,
                { mediaResult ->
                    handleVideoMediaResult(mediaResult)
                },
                {
                    sendError(IONCAMRError.CAPTURE_VIDEO_ERROR)
                })
        }
    }

    private fun processResultFromGallery(result: ActivityResult) {
        val manager = galleryManager ?: run {
            sendError(IONCAMRError.CONTEXT_ERROR)
            return
        }

        val settings = gallerySettings ?: run {
            sendError(IONCAMRError.INVALID_ARGUMENT_ERROR)
            return
        }

        CoroutineScope(Dispatchers.Default).launch {
            manager.onChooseFromGalleryResult(
                activity,
                result.resultCode,
                result.data,
                settings.includeMetadata,
                {
                    handleGalleryMediaResults(it)
                },
                { sendError(it) })
        }
    }

    private fun processResultFromEdit(result: ActivityResult) {
        val manager = editManager ?: run {
            sendError(IONCAMRError.CONTEXT_ERROR)
            return
        }

        manager.processResultFromEdit(
            activity,
            result.data,
            editParameters,
            { image ->
                handleEditBase64Result(image)
            },
            { mediaResult ->
                handleMediaResult(mediaResult)
            },
            { error ->
                sendError(error)
            }
        )
    }

    private fun IonCameraSettings.toIonParameters(): IONCAMRCameraParameters {
        return IONCAMRCameraParameters(
            mQuality = quality,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            encodingType = encodingType,
            mediaType = MEDIA_TYPE_PHOTO,
            allowEdit = editable != IonEditableMode.NO,
            correctOrientation = correctOrientation,
            saveToPhotoAlbum = saveToGallery,
            includeMetadata = includeMetadata,
        )
    }

    fun checkCameraPermissions(call: PluginCall, saveToGallery: Boolean): Boolean {
        // if the manifest does not contain the camera permissions key, we don't need to ask the user
        val needCameraPerms = permissionHelper.isPermissionDeclared(CAMERA)
        val hasCameraPerms =
            !needCameraPerms || permissionHelper.getPermissionState(CAMERA) == PermissionState.GRANTED
        val hasGalleryPerms =
            permissionHelper.getPermissionState(SAVE_GALLERY) == PermissionState.GRANTED

        // If we want to save to the gallery, we need two permissions
        // actually we only need permissions to save to gallery for Android <= 9 (API 28)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // we might still need to request permission for the camera
            if (!hasCameraPerms) {
                permissionHelper.requestPermissionForAlias(
                    CAMERA,
                    call,
                    "ionCameraPermissionsCallback"
                )
                return false
            }
            return true
        }

        // we need to request permissions to save to gallery for Android <= 9
        if (saveToGallery && !(hasCameraPerms && hasGalleryPerms) && isFirstRequest) {
            isFirstRequest = false
            val aliases: Array<String> = if (needCameraPerms) {
                arrayOf(CAMERA, SAVE_GALLERY)
            } else {
                arrayOf(SAVE_GALLERY)
            }
            permissionHelper.requestPermissionForAliases(aliases, call, "ionCameraPermissionsCallback")
            return false
        } else if (!hasCameraPerms) {
            permissionHelper.requestPermissionForAlias(
                CAMERA,
                call,
                "ionCameraPermissionsCallback"
            )
            return false
        }
        return true
    }

    private fun checkGalleryPermissions(call: PluginCall): Boolean {
        // Android 10+ does not require storage permissions to use the system gallery picker
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true
        }
        val needGalleryPerms = permissionHelper.isPermissionDeclared(SAVE_GALLERY)
        val hasGalleryPerms = !needGalleryPerms || permissionHelper.getPermissionState(SAVE_GALLERY) == PermissionState.GRANTED
        if (!hasGalleryPerms) {
            permissionHelper.requestPermissionForAlias(SAVE_GALLERY, call, "ionCameraPermissionsCallback")
            return false
        }
        return true
    }

    fun handlePermissionsCallback(call: PluginCall) {
        // chooseFromGallery does not require CAMERA permission
        if (call.methodName != "chooseFromGallery" &&
            permissionHelper.getPermissionState(CAMERA) != PermissionState.GRANTED) {
            sendError(IONCAMRError.CAMERA_PERMISSION_DENIED_ERROR)
            return
        }

        // On Android <= 9, SAVE_GALLERY (READ/WRITE_EXTERNAL_STORAGE) is required:
        // - for takePhoto/recordVideo when saveToGallery is true
        // - always for chooseFromGallery (READ_EXTERNAL_STORAGE is needed to access the gallery)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val needsGalleryPerm = when (call.methodName) {
                "takePhoto" -> cameraSettings?.saveToGallery ?: false
                "recordVideo" -> videoParameters?.saveToGallery ?: false
                "chooseFromGallery" -> true
                else -> false
            }
            val galleryPermDeclared = permissionHelper.isPermissionDeclared(SAVE_GALLERY)
            if (needsGalleryPerm && galleryPermDeclared && permissionHelper.getPermissionState(SAVE_GALLERY) != PermissionState.GRANTED) {
                sendError(IONCAMRError.GALLERY_PERMISSION_DENIED_ERROR)
                return
            }
        }

        when (call.methodName) {
            "takePhoto" -> openCamera(call)
            "recordVideo" -> openRecordVideo(call)
            "chooseFromGallery" -> openGallery(call)
            else -> sendError(IONCAMRError.CONTEXT_ERROR)
        }
    }

    private fun sendError(error: IONCAMRError) {
        try {
            val jsonResult = JSObject()
            jsonResult.put("code", formatErrorCode(error.code))
            jsonResult.put("message", error.description)
            currentCall?.reject(error.description, formatErrorCode(error.code))
            currentCall = null
        } catch (e: Exception) {
            currentCall?.reject("There was an error performing the operation.")
            currentCall = null
        } finally {
            lastEditUri = null
        }
    }

    private fun formatErrorCode(code: Int): String {
        val stringCode = Integer.toString(code)
        return ERROR_FORMAT_PREFIX + "0000$stringCode".substring(stringCode.length)
    }

    fun onDestroy() {
        cameraManager?.deleteVideoFilesFromCache(activity)
    }
}