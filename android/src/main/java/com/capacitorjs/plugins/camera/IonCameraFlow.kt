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
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
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
import io.ionic.libs.ioncameralib.model.IONCAMRError
import io.ionic.libs.ioncameralib.model.IONCAMRMediaResult
import io.ionic.libs.ioncameralib.model.IONCAMRMediaType
import io.ionic.libs.ioncameralib.view.IONCAMRImageEditorActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class IonCameraFlow(
    private val plugin: CameraPlugin
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
    private var cameraSettings : CameraSettings? = null
    private var videoSettings : VideoSettings? = null
    private var gallerySettings : GallerySettings? = null
    private var editParameters = IONCAMREditParameters(
        editURI = "", fromUri = false, saveToGallery = false, includeMetadata = false
    )
    private var lastEditUri: String? = null

    fun load() {
        setupLaunchers()
        cameraManager = IONCAMRCameraManager(
            plugin.getAppId(),
            ".fileprovider",
            IONCAMRExifHelper(),
            IONCAMRFileHelper(),
            IONCAMRMediaHelper(),
            IONCAMRImageHelper()
        )

        videoManager = IONCAMRVideoManager(
            ".fileprovider",
            IONCAMRFileHelper(),
        )

        galleryManager = IONCAMRGalleryManager(
            IONCAMRExifHelper(),
            IONCAMRFileHelper(),
            IONCAMRMediaHelper(),
            IONCAMRImageHelper()
        )

        editManager = IONCAMREditManager(
            plugin.getAppId(),
            ".fileprovider",
            IONCAMRExifHelper(),
            IONCAMRFileHelper(),
            IONCAMRMediaHelper(),
            IONCAMRImageHelper()
        )

        cameraManager?.deleteVideoFilesFromCache(plugin.activity)
    }

    fun takePhoto(call: PluginCall, settings: CameraSettings) {
        cameraSettings = settings
        currentCall = call
        showCamera(call)
    }

    fun recordVideo(call: PluginCall, settings: VideoSettings) {
        videoSettings = settings
        currentCall = call
        openRecordVideo(call)
    }

    fun playVideo(call: PluginCall) {
        currentCall = call
        openPlayVideo(call)
    }

    fun chooseFromGallery(call: PluginCall, settings: GallerySettings) {
        gallerySettings = settings
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
        cameraLauncher = plugin.activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleCameraResult(result)
        }

        cameraCropLauncher = plugin.activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleCameraCropResult(result)
        }

        videoLauncher = plugin.activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleVideoResult(result)
        }

        galleryLauncher = plugin.activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleGalleryResult(result)
        }

        galleryCropLauncher = plugin.activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleGalleryCropResult(result)
        }

        editLauncher = plugin.activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleEditResult(result)
        }

    }

    private fun showCamera(call: PluginCall) {
        if (!plugin.getContext().getPackageManager()
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
                manager.takePhoto(plugin.getActivity(), CameraPlugin.ENCODING_TYPE, cameraLauncher)
            } catch (ex: Exception) {
                sendError(IONCAMRError.FAILED_TO_CAPTURE_IMAGE_ERROR)
            }
        }
    }

    fun openRecordVideo(call: PluginCall) {
        val settings = videoSettings ?: run {
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
                    plugin.getActivity(),
                    settings.saveToGallery,
                    videoLauncher
                ) {
                    sendError(it)
                }
            } catch (ex: Exception) {
                sendError(IONCAMRError.VIDEO_CAPTURE_NOT_SUPPORTED_ERROR)
            }
        }
    }

    private fun openPlayVideo(call: PluginCall) {
        try {
            val manager = videoManager ?: run {
                sendError(IONCAMRError.CONTEXT_ERROR)
                return
            }

            val videoUri = call.getString("videoURI")
                ?: return sendError(IONCAMRError.PLAY_VIDEO_GENERAL_ERROR)
            manager.playVideo(plugin.activity, videoUri, {
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
        val manager = galleryManager ?: run {
            sendError(IONCAMRError.CONTEXT_ERROR)
            return
        }

        val settings = gallerySettings ?: run {
            sendError(IONCAMRError.INVALID_ARGUMENT_ERROR)
            return
        }

        manager.chooseFromGallery(
            plugin.activity,
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
        val imageBase64 = call.data.getString("base64")
        if (imageBase64 == null) return
        manager.editImage(plugin.activity, imageBase64, editLauncher)
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

        manager.editURIPicture(plugin.activity, photoPath, editLauncher) {
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
                if (settings.allowEditing) {
                    editPhoto()
                } else {
                    processResult(result)
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

                if (settings.allowEdit && uris.size == 1 && settings.mediaType == IONCAMRMediaType.PICTURE) {

                    val originalUri = uris.first()

                    if (settings.editInApp) {
                        editor.openCropActivity(
                            plugin.activity,
                            originalUri,
                            galleryCropLauncher
                        )
                    }else{
                        val tempUri = if (originalUri.scheme == "content") {
                            IonCameraUtils.getTempImage(plugin.activity, originalUri)
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
                                plugin.activity,
                                originalUri,
                                galleryCropLauncher
                            )
                        }
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
            }

            Activity.RESULT_CANCELED -> {
                if (!settings.editInApp && !lastEditUri.isNullOrEmpty()) {
                    val intent = Intent().apply {
                        putExtra(IONCAMRImageEditorActivity.IMAGE_OUTPUT_URI_EXTRAS, lastEditUri)
                    }
                    processResultEditFromGallery(intent)
                } else {
                    lastEditUri = null
                    sendError(IONCAMRError.EDIT_OPERATION_CANCELLED_ERROR)
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
                plugin.activity,
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
            Activity.RESULT_CANCELED -> sendError(IONCAMRError.EDIT_OPERATION_CANCELLED_ERROR)
            else -> sendError(IONCAMRError.EDIT_IMAGE_ERROR)
        }
    }
    private fun editPhoto() {
        val editor = editManager ?: run {
            sendError(IONCAMRError.CONTEXT_ERROR)
            return
        }

        val appId = plugin.getAppId()
        val tmpFile = FileProvider.getUriForFile(
            plugin.activity,
            "$appId.fileprovider",
            editor.createCaptureFile(
                plugin.activity,
                CameraPlugin.ENCODING_TYPE,
                plugin.activity.getSharedPreferences(
                    CameraPlugin.STORE,
                    Context.MODE_PRIVATE
                ).getString(CameraPlugin.EDIT_FILE_NAME_KEY, "") ?: ""
            )
        )

        editor.openCropActivity(
            plugin.activity,
            tmpFile,
            cameraCropLauncher
        )
    }

    private fun createEditIntent(origPhotoUri: Uri): Intent? {
        return try {
            val editFile = File(origPhotoUri.path!!)
            val editUri = FileProvider.getUriForFile(
                plugin.activity,
                plugin.context.packageName + ".fileprovider",
                editFile
            )

            lastEditUri = editFile.absolutePath

            val editIntent = Intent(Intent.ACTION_EDIT)
            editIntent.setDataAndType(editUri, "image/*")
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            editIntent.addFlags(flags)
            editIntent.putExtra(MediaStore.EXTRA_OUTPUT, editUri)

            val resInfoList: MutableList<ResolveInfo>?

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                resInfoList = plugin
                    .context
                    .packageManager
                    .queryIntentActivities(editIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
            } else {
                resInfoList = legacyQueryIntentActivities(editIntent)
            }

            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                plugin.context.grantUriPermission(packageName, editUri, flags)
            }

            editIntent

        } catch (e: Exception) {
            null
        }
    }

    @Suppress("deprecation")
    private fun legacyQueryIntentActivities(intent: Intent): MutableList<ResolveInfo> {
        return plugin.context.packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }

    private fun handleCameraCropResult(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> processResult(result)
            Activity.RESULT_CANCELED -> sendError(IONCAMRError.EDIT_OPERATION_CANCELLED_ERROR)
            else -> sendError(IONCAMRError.EDIT_IMAGE_ERROR)
        }
    }

    private fun handlePhotoBase64Result(image: String) {
        val ret = JSObject()
        ret.put("format", "jpeg")

        val settings = cameraSettings ?: run {
            sendError(IONCAMRError.INVALID_ARGUMENT_ERROR)
            return
        }

        when (settings.resultType) {
            CameraResultType.BASE64 -> {
                ret.put("base64String", image)
            }

            CameraResultType.DATAURL -> {
                ret.put("dataUrl", "data:image/jpeg;base64,$image")
            }

            else -> {
                sendError(IONCAMRError.PROCESS_IMAGE_ERROR)
                return
            }
        }

        currentCall?.resolve(ret)
        currentCall = null
    }

    private fun handleEditBase64Result(image: String) {
        val ret = JSObject()
        ret.put("format", "jpeg")
        ret.put("base64String", image)
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

        val exif = ImageUtils.getExifData(plugin.context, bitmap, uri)
        val ret = JSObject()
        ret.put("format", "jpeg")
        ret.put("exif", exif.toJson())
        ret.put("path", mediaResult.uri)
        ret.put("webPath", FileUtils.getPortablePath(plugin.context, plugin.bridge.localUrl, uri))
        ret.put("saved", mediaResult.saved)
        currentCall?.resolve(ret)
        currentCall = null
    }

    private fun handleVideoMediaResult(mediaResult: IONCAMRMediaResult) {
        val file = File(mediaResult.uri)
        val uri = Uri.fromFile(file)

        val ret = JSObject()
        ret.put("path", mediaResult.uri)
        ret.put("webPath", FileUtils.getPortablePath(plugin.context, plugin.bridge.localUrl, uri))
        ret.put("saved", mediaResult.saved)

        mediaResult.metadata?.let { metadata ->
            ret.put("duration", metadata.duration)
            ret.put("size", metadata.size)
            ret.put("format", metadata.format)
        }
        currentCall?.resolve(ret)
        currentCall = null
    }

    private fun handleGalleryMediaResults(results: List<IONCAMRMediaResult>) {
        val photos = JSArray()
        results.forEach { mediaResult ->
            val file = File(mediaResult.uri)
            val uri = Uri.fromFile(file)

            val obj = JSObject()
            obj.put("path", mediaResult.uri)
            obj.put(
                "webPath",
                FileUtils.getPortablePath(plugin.context, plugin.bridge.localUrl, uri)
            )
            obj.put("saved", mediaResult.saved)

            mediaResult.metadata?.let {
                obj.put("duration", it.duration)
                obj.put("size", it.size)
                obj.put("format", it.format)
            }

            photos.put(obj)
        }

        val ret = JSObject()
        ret.put("photos", photos)
        currentCall?.resolve(ret)
        currentCall = null
        lastEditUri = null
    }

    private fun processResult(result: ActivityResult) {
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
            plugin.activity,
            result.data,
            ionParams,
            { image ->
                handlePhotoBase64Result(image)
            },
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
                plugin.activity.getSharedPreferences(CameraPlugin.STORE, Context.MODE_PRIVATE)
                    .getString(CameraPlugin.STORE, "")
            fromPreferences.let { uri = Uri.parse(fromPreferences) }
        }
        if (plugin.activity == null) {
            sendError(IONCAMRError.CAPTURE_VIDEO_ERROR)
            return
        }
        val settings = videoSettings ?: run {
            sendError(IONCAMRError.INVALID_ARGUMENT_ERROR)
            return
        }

        CoroutineScope(Dispatchers.Default).launch {
            manager.processResultFromVideo(
                plugin.activity,
                uri,
                settings.saveToGallery,
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
                plugin.activity,
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
            plugin.activity,
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

    private fun CameraSettings.toIonParameters(): IONCAMRCameraParameters {
        val useLatestVersion = (resultType == CameraResultType.URI)
        return IONCAMRCameraParameters(
            mQuality = quality,
            targetWidth = width,
            targetHeight = height,
            encodingType = CameraPlugin.ENCODING_TYPE, // JPEG
            mediaType = CameraPlugin.MEDIA_TYPE_PICTURE,
            allowEdit = allowEditing,
            correctOrientation = shouldCorrectOrientation,
            saveToPhotoAlbum = saveToGallery,
            //TODO this value should come from settings, possibly from new interface TakePhotoOptions
            includeMetadata = false,
            latestVersion = useLatestVersion
        )
    }

    fun checkCameraPermissions(call: PluginCall, saveToGallery: Boolean): Boolean {
        // if the manifest does not contain the camera permissions key, we don't need to ask the user
        val needCameraPerms = plugin.isPermissionDeclared(CameraPlugin.CAMERA)
        val hasCameraPerms =
            !needCameraPerms || plugin.getPermissionState(CameraPlugin.CAMERA) == PermissionState.GRANTED
        val hasGalleryPerms =
            plugin.getPermissionState(CameraPlugin.SAVE_GALLERY) == PermissionState.GRANTED

        // If we want to save to the gallery, we need two permissions
        // actually we only need permissions to save to gallery for Android <= 9 (API 28)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // we might still need to request permission for the camera
            if (!hasCameraPerms) {
                plugin.requestLegacyPermissionForAlias(
                    CameraPlugin.CAMERA,
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
                arrayOf(CameraPlugin.CAMERA, CameraPlugin.SAVE_GALLERY)
            } else {
                arrayOf(CameraPlugin.SAVE_GALLERY)
            }
            plugin.requestLegacyPermissionForAliases(aliases, call, "ionCameraPermissionsCallback")
            return false
        } else if (!hasCameraPerms) {
            plugin.requestLegacyPermissionForAlias(
                CameraPlugin.CAMERA,
                call,
                "ionCameraPermissionsCallback"
            )
            return false
        }
        return true
    }

    fun handleCameraPermissionsCallback(call: PluginCall) {
        if (plugin.getPermissionState(CameraPlugin.CAMERA) != PermissionState.GRANTED) {
            sendError(IONCAMRError.CAMERA_PERMISSION_DENIED_ERROR)
            return
        }

        when (call.getMethodName()) {
            "takePhoto" -> openCamera(call)
            "recordVideo" -> openRecordVideo(call)
            "pickImages" -> openGallery(call)
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
        }finally {
            lastEditUri = null
        }
    }

    private fun formatErrorCode(code: Int): String {
        val stringCode = Integer.toString(code)
        return CameraPlugin.ERROR_FORMAT_PREFIX + "0000$stringCode".substring(stringCode.length)
    }

    fun onDestroy() {
        cameraManager?.deleteVideoFilesFromCache(plugin.activity)
    }
}