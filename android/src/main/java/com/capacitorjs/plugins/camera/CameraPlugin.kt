package com.capacitorjs.plugins.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.getcapacitor.FileUtils
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Logger
import com.getcapacitor.PermissionState
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.ActivityCallback
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * The Camera plugin makes it easy to take a photo or have the user select a photo
 * from their albums.
 *
 * On Android, this plugin sends an intent that opens the stock Camera app.
 *
 * Adapted from https://developer.android.com/training/camera/photobasics.html
 */
@SuppressLint("InlinedApi")
@CapacitorPlugin(
    name = "Camera",
    permissions = [Permission(
        strings = [Manifest.permission.CAMERA],
        alias = CameraPlugin.CAMERA
    ), Permission(
        strings = [],
        alias = CameraPlugin.PHOTOS
    ), Permission(
        strings = [Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE],
        alias = CameraPlugin.SAVE_GALLERY
    ), Permission(
        strings = [Manifest.permission.READ_EXTERNAL_STORAGE],
        alias = CameraPlugin.READ_EXTERNAL_STORAGE
    )]
)
class CameraPlugin : Plugin() {

    // ========================================
    // SECTION 1: CONSTANTS & COMPANION
    // ========================================

    companion object {
        const val CAMERA = "camera"
        const val PHOTOS = "photos"
        const val SAVE_GALLERY = "saveGallery"
        const val READ_EXTERNAL_STORAGE = "readExternalStorage"
        const val STORE = "CameraStore"
        const val EDIT_FILE_NAME_KEY = "EditFileName"
        const val ERROR_FORMAT_PREFIX = "OS-PLUG-CAMR-"
        const val ENCODING_TYPE = 0
        const val MEDIA_TYPE_PICTURE = 0

        // Legacy error messages
        private const val INVALID_RESULT_TYPE_ERROR = "Invalid resultType option"
        private const val PERMISSION_DENIED_ERROR_CAMERA = "User denied access to camera"
        private const val NO_CAMERA_ERROR = "Device doesn't have a camera available"
        private const val NO_CAMERA_ACTIVITY_ERROR = "Unable to resolve camera activity"
        private const val NO_PHOTO_ACTIVITY_ERROR = "Unable to resolve photo activity"
        private const val IMAGE_FILE_SAVE_ERROR = "Unable to create photo on disk"
        private const val IMAGE_PROCESS_NO_FILE_ERROR = "Unable to process image, file not found on disk"
        private const val UNABLE_TO_PROCESS_IMAGE = "Unable to process image"
        private const val IMAGE_EDIT_ERROR = "Unable to edit image"
        private const val IMAGE_GALLERY_SAVE_ERROR = "Unable to save the image in the gallery"
        private const val USER_CANCELLED = "User cancelled photos app"
    }

    // ========================================
    // SECTION 2: STATE VARIABLES
    // ========================================

    // 2.1 Managers (Ion-based)
    private var cameraManager: IONCAMRCameraManager? = null
    private var videoManager: IONCAMRVideoManager? = null
    private var editManager: IONCAMREditManager? = null
    private var galleryManager: IONCAMRGalleryManager? = null

    // 2.2 Activity Launchers (Ion-based - eager registration)
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraCropLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryCropLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var videoLauncher: ActivityResultLauncher<Intent>
    private lateinit var editLauncher: ActivityResultLauncher<Intent>

    // 2.3 Activity Launchers (Legacy - lazy registration)
    private var pickMultipleMedia: ActivityResultLauncher<PickVisualMediaRequest>? = null
    private var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>? = null

    // 2.4 Settings
    private var cameraSettings: CameraSettings? = null
    private var videoSettings = VideoSettings()
    private var gallerySettings = GallerySettings()
    private var editParameters = IONCAMREditParameters(
        editURI = "",
        fromUri = false,
        saveToGallery = false,
        includeMetadata = false
    )

    // 2.5 Legacy state tracking
    private var imageFileSavePath: String? = null
    private var imageEditedFileSavePath: String? = null
    private var imageFileUri: Uri? = null
    private var imagePickedContentUri: Uri? = null
    private var isEdited = false
    private var isSaved = false

    // 2.6 Request tracking
    private var currentCall: PluginCall? = null
    private var isFirstRequestLegacy = true
    private var isFirstRequestIon = true
    private val mNextLocalRequestCode = AtomicInteger()

    // ========================================
    // SECTION 3: LIFECYCLE METHODS
    // ========================================

    override fun load() {
        super.load()

        // Initialize Ion managers
        cameraManager = IONCAMRCameraManager(
            getAppId(),
            IONCAMRExifHelper(),
            IONCAMRFileHelper(),
            IONCAMRMediaHelper(),
            IONCAMRImageHelper()
        )

        videoManager = IONCAMRVideoManager(
            IONCAMRFileHelper()
        )

        galleryManager = IONCAMRGalleryManager(
            IONCAMRExifHelper(),
            IONCAMRFileHelper(),
            IONCAMRMediaHelper(),
            IONCAMRImageHelper()
        )

        editManager = IONCAMREditManager(
            getAppId(),
            IONCAMRExifHelper(),
            IONCAMRFileHelper(),
            IONCAMRMediaHelper(),
            IONCAMRImageHelper()
        )

        // Setup Ion launchers (eager registration)
        setupIonLaunchers()

        // Clean up any cached video files
        cameraManager?.deleteVideoFilesFromCache(activity)

        // Note: Legacy launchers (pickMedia, pickMultipleMedia) are registered lazily on first use
    }

    private fun setupIonLaunchers() {
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

    protected override fun saveInstanceState(): Bundle? {
        val bundle = super.saveInstanceState()
        // Save legacy state
        bundle?.putString("cameraImageFileSavePath", imageFileSavePath)
        return bundle
    }

    protected override fun restoreState(state: Bundle) {
        super.restoreState(state)
        // Restore legacy state
        imageFileSavePath = state.getString("cameraImageFileSavePath")
    }

    /**
     * Unregister activity result launches to prevent leaks.
     */
    protected override fun handleOnDestroy() {
        // Cleanup Ion resources
        cameraManager?.deleteVideoFilesFromCache(activity)

        // Cleanup Legacy launchers
        pickMedia?.unregister()
        pickMultipleMedia?.unregister()
    }

    // ========================================
    // SECTION 4: PLUGIN METHODS (WEB API)
    // ========================================

    @PluginMethod
    fun getPhoto(call: PluginCall) {
        isEdited = false
        cameraSettings = getSettings(call)
        legacyDoShow(call)
    }

    @PluginMethod
    fun takePhoto(call: PluginCall) {
        cameraSettings = getSettings(call)
        currentCall = call
        ionShowCamera(call)
    }

    @PluginMethod
    fun recordVideo(call: PluginCall) {
        videoSettings = getVideoSettings(call)
        currentCall = call
        ionOpenRecordVideo(call)
    }

    @PluginMethod
    fun playVideo(call: PluginCall) {
        currentCall = call
        ionOpenPlayVideo(call)
    }

    @PluginMethod
    fun chooseFromGallery(call: PluginCall) {
        gallerySettings = getGallerySettings(call)
        currentCall = call
        ionOpenGallery(call)
    }

    @PluginMethod
    fun editPhoto(call: PluginCall) {
        currentCall = call
        ionCallEditPhoto(call)
    }

    @PluginMethod
    fun editURIPhoto(call: PluginCall) {
        currentCall = call
        ionCallEditURIPhoto(call)
    }

    @PluginMethod
    fun pickImages(call: PluginCall) {
        cameraSettings = getSettings(call)
        legacyOpenPhotos(call, true)
    }

    @PluginMethod
    fun pickLimitedLibraryPhotos(call: PluginCall) {
        call.unimplemented("not supported on android")
    }

    @PluginMethod
    fun getLimitedLibraryPhotos(call: PluginCall) {
        call.unimplemented("not supported on android")
    }

    @PluginMethod
    override fun requestPermissions(call: PluginCall) {
        // If the camera permission is defined in the manifest, then we have to prompt the user
        // or else we will get a security exception when trying to present the camera. If, however,
        // it is not defined in the manifest then we don't need to prompt and it will just work.
        if (isPermissionDeclared(CAMERA)) {
            // just request normally
            super.requestPermissions(call)
        } else {
            // the manifest does not define camera permissions, so we need to decide what to do
            // first, extract the permissions being requested
            val providedPerms = call.getArray("permissions")
            var permsList: MutableList<String?>? = null
            if (providedPerms != null) {
                try {
                    permsList = providedPerms.toList<String?>()
                } catch (e: JSONException) {
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
                (permsList != null && permsList.size == 1 && (permsList.contains(CAMERA) || permsList.contains(
                    PHOTOS
                )))
            ) {
                // either we're on Android 13+ (storage permissions do not apply)
                // or the only thing being asked for was the camera so we can just return the current state
                checkPermissions(call)
            } else {
                requestPermissionForAlias(SAVE_GALLERY, call, "checkPermissions")
            }
        }
    }

    // ========================================
    // SECTION 5: PERMISSION CALLBACKS
    // ========================================

    /**
     * Completes the plugin call after a camera permission request
     *
     * @see .getPhoto
     * @param call the plugin call
     */
    @PermissionCallback
    private fun cameraPermissionsCallback(call: PluginCall) {
        legacyHandleCameraPermissionsCallback(call)
    }

    /**
     * Completes the plugin call after a camera permission request
     *
     * @see .takePhoto
     * @param call the plugin call
     */
    @PermissionCallback
    private fun ionCameraPermissionsCallback(call: PluginCall) {
        ionHandleCameraPermissionsCallback(call)
    }

    // ========================================
    // SECTION 6: ACTIVITY CALLBACKS
    // ========================================

    @ActivityCallback
    fun processCameraImage(call: PluginCall, result: ActivityResult) {
        legacyProcessCameraImage(call, result)
    }

    @ActivityCallback
    private fun processEditedImage(call: PluginCall, result: ActivityResult) {
        legacyProcessEditedImage(call, result)
    }

    // ========================================
    // SECTION 7: ION FLOW - CAMERA OPERATIONS
    // ========================================

    private fun ionShowCamera(call: PluginCall) {
        if (!getContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            sendError(IONCAMRError.NO_CAMERA_AVAILABLE_ERROR)
            return
        }
        ionOpenCamera(call)
    }

    private fun ionOpenCamera(call: PluginCall) {
        val settings = cameraSettings ?: run {
            sendError(IONCAMRError.INVALID_ARGUMENT_ERROR)
            return
        }
        if (ionCheckCameraPermissions(call, settings.saveToGallery)) {
            try {
                val manager = cameraManager ?: run {
                    sendError(IONCAMRError.CONTEXT_ERROR)
                    return
                }
                currentCall = call
                manager.takePhoto(getActivity(), ENCODING_TYPE, cameraLauncher)
            } catch (ex: Exception) {
                sendError(IONCAMRError.FAILED_TO_CAPTURE_IMAGE_ERROR)
            }
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
                    ionEditPhoto()
                } else {
                    ionProcessResult(result)
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

    private fun handleCameraCropResult(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> ionProcessResult(result)
            Activity.RESULT_CANCELED -> sendError(IONCAMRError.EDIT_OPERATION_CANCELLED_ERROR)
            else -> sendError(IONCAMRError.EDIT_IMAGE_ERROR)
        }
    }

    private fun ionEditPhoto() {
        val editor = editManager ?: run {
            sendError(IONCAMRError.CONTEXT_ERROR)
            return
        }

        val appId = getAppId()
        val tmpFile = FileProvider.getUriForFile(
            activity,
            "$appId.fileprovider",
            editor.createCaptureFile(
                activity,
                ENCODING_TYPE,
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

    // ========================================
    // SECTION 8: ION FLOW - VIDEO OPERATIONS
    // ========================================

    private fun ionOpenRecordVideo(call: PluginCall) {
        val settings = videoSettings

        if (ionCheckCameraPermissions(call, settings.saveToGallery)) {
            try {
                val manager = cameraManager ?: run {
                    sendError(IONCAMRError.CONTEXT_ERROR)
                    return
                }
                currentCall = call
                manager.recordVideo(
                    getActivity(),
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

    private fun ionOpenPlayVideo(call: PluginCall) {
        try {
            val manager = videoManager ?: run {
                sendError(IONCAMRError.CONTEXT_ERROR)
                return
            }

            val videoUri = call.getString("videoURI")
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

    private fun handleVideoResult(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                ionProcessResultFromVideo(result)
            }

            Activity.RESULT_CANCELED -> {
                sendError(IONCAMRError.CAPTURE_VIDEO_CANCELLED_ERROR)
            }

            else -> sendError(IONCAMRError.CAPTURE_VIDEO_ERROR)
        }
    }

    // ========================================
    // SECTION 9: ION FLOW - GALLERY OPERATIONS
    // ========================================

    private fun ionOpenGallery(call: PluginCall) {
        val manager = galleryManager ?: run {
            sendError(IONCAMRError.CONTEXT_ERROR)
            return
        }

        val settings = gallerySettings

        manager.chooseFromGallery(
            activity,
            settings.mediaType,
            settings.allowMultipleSelection,
            settings.limit,
            galleryLauncher
        )
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

                val settings = gallerySettings

                val uris = manager.extractUris(result.data)

                if (uris.isEmpty()) {
                    sendError(IONCAMRError.GENERIC_CHOOSE_MULTIMEDIA_ERROR)
                    return
                }

                if (settings.allowEdit && uris.size == 1 && settings.mediaType == IONCAMRMediaType.PICTURE) {
                    editor.openCropActivity(
                        activity,
                        uris.first(),
                        galleryCropLauncher
                    )
                } else {
                    ionProcessResultFromGallery(result)
                }
            }

            Activity.RESULT_CANCELED -> {
                sendError(IONCAMRError.CHOOSE_MULTIMEDIA_CANCELLED_ERROR)
            }

            else -> sendError(IONCAMRError.GENERIC_CHOOSE_MULTIMEDIA_ERROR)
        }
    }

    private fun handleGalleryCropResult(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val manager = galleryManager ?: run {
                    sendError(IONCAMRError.CONTEXT_ERROR)
                    return
                }

                val settings = gallerySettings

                CoroutineScope(Dispatchers.Default).launch {
                    manager.onChooseFromGalleryEditResult(
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

            Activity.RESULT_CANCELED -> sendError(IONCAMRError.EDIT_OPERATION_CANCELLED_ERROR)
            else -> sendError(IONCAMRError.EDIT_IMAGE_ERROR)
        }
    }

    // ========================================
    // SECTION 10: ION FLOW - EDIT OPERATIONS
    // ========================================

    private fun ionCallEditPhoto(call: PluginCall) {
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
        manager.editImage(activity, imageBase64, editLauncher)
    }

    private fun ionCallEditURIPhoto(call: PluginCall) {
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

    private fun handleEditResult(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> ionProcessResultFromEdit(result)
            Activity.RESULT_CANCELED -> sendError(IONCAMRError.EDIT_OPERATION_CANCELLED_ERROR)
            else -> sendError(IONCAMRError.EDIT_IMAGE_ERROR)
        }
    }

    // ========================================
    // SECTION 11: ION FLOW - RESULT PROCESSING
    // ========================================

    private fun ionProcessResult(result: ActivityResult) {
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

    private fun ionProcessResultFromVideo(result: ActivityResult) {
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
        val settings = videoSettings

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

    private fun ionProcessResultFromGallery(result: ActivityResult) {
        val manager = galleryManager ?: run {
            sendError(IONCAMRError.CONTEXT_ERROR)
            return
        }

        val settings = gallerySettings

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

    private fun ionProcessResultFromEdit(result: ActivityResult) {
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

    // ========================================
    // SECTION 12: ION FLOW - RESULT HANDLERS
    // ========================================

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

        val exif = ImageUtils.getExifData(context, bitmap, uri)
        val ret = JSObject()
        ret.put("format", "jpeg")
        ret.put("exif", exif.toJson())
        ret.put("path", mediaResult.uri)
        ret.put("webPath", FileUtils.getPortablePath(context, bridge.localUrl, uri))
        ret.put("saved", mediaResult.saved)
        currentCall?.resolve(ret)
        currentCall = null
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

        mediaResult.metadata?.let { metadata ->
            ret.put("duration", metadata.duration)
            ret.put("size", metadata.size)
            ret.put("format", metadata.format)
            ret.put("resolution", metadata.resolution)
            ret.put("creationDate", metadata.creationDate)
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
                FileUtils.getPortablePath(context, bridge.localUrl, uri)
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
    }

    // ========================================
    // SECTION 13: ION FLOW - PERMISSIONS
    // ========================================

    private fun ionCheckCameraPermissions(call: PluginCall, saveToGallery: Boolean): Boolean {
        // if the manifest does not contain the camera permissions key, we don't need to ask the user
        val needCameraPerms = isPermissionDeclared(CAMERA)
        val hasCameraPerms =
            !needCameraPerms || getPermissionState(CAMERA) == PermissionState.GRANTED
        val hasGalleryPerms =
            getPermissionState(SAVE_GALLERY) == PermissionState.GRANTED

        // If we want to save to the gallery, we need two permissions
        // actually we only need permissions to save to gallery for Android <= 9 (API 28)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // we might still need to request permission for the camera
            if (!hasCameraPerms) {
                requestPermissionForAlias(
                    CAMERA,
                    call,
                    "ionCameraPermissionsCallback"
                )
                return false
            }
            return true
        }

        // we need to request permissions to save to gallery for Android <= 9
        if (saveToGallery && !(hasCameraPerms && hasGalleryPerms) && isFirstRequestIon) {
            isFirstRequestIon = false
            val aliases: Array<String> = if (needCameraPerms) {
                arrayOf(CAMERA, SAVE_GALLERY)
            } else {
                arrayOf(SAVE_GALLERY)
            }
            requestPermissionForAliases(aliases, call, "ionCameraPermissionsCallback")
            return false
        } else if (!hasCameraPerms) {
            requestPermissionForAlias(
                CAMERA,
                call,
                "ionCameraPermissionsCallback"
            )
            return false
        }
        return true
    }

    private fun ionHandleCameraPermissionsCallback(call: PluginCall) {
        if (getPermissionState(CAMERA) != PermissionState.GRANTED) {
            sendError(IONCAMRError.CAMERA_PERMISSION_DENIED_ERROR)
            return
        }

        when (call.methodName) {
            "takePhoto" -> ionOpenCamera(call)
            "recordVideo" -> ionOpenRecordVideo(call)
            "pickImages" -> ionOpenGallery(call)
            else -> sendError(IONCAMRError.CONTEXT_ERROR)
        }
    }

    // ========================================
    // SECTION 14: LEGACY FLOW - MAIN OPERATIONS
    // ========================================

    private fun legacyDoShow(call: PluginCall) {
        when (cameraSettings?.source) {
            CameraSource.CAMERA -> legacyShowCamera(call)
            CameraSource.PHOTOS -> legacyShowPhotos(call)
            else -> legacyShowPrompt(call)
        }
    }

    private fun legacyShowPrompt(call: PluginCall) {
        // We have all necessary permissions, open the camera
        val options = mutableListOf<String>()
        options.add(call.getString("promptLabelPhoto", "From Photos") ?: "From Photos")
        options.add(call.getString("promptLabelPicture", "Take Picture") ?: "Take Picture")

        val fragment = CameraBottomSheetDialogFragment()
        fragment.setTitle(call.getString("promptLabelHeader", "Photo") ?: "Photo")
        fragment.setOptions(
            options,
            { index ->
                if (index == 0) {
                    cameraSettings?.source = CameraSource.PHOTOS
                    legacyOpenPhotos(call)
                } else if (index == 1) {
                    cameraSettings?.source = CameraSource.CAMERA
                    legacyOpenCamera(call)
                }
            },
            { call.reject(USER_CANCELLED) }
        )
        fragment.show(getActivity().supportFragmentManager, "capacitorModalsActionSheet")
    }

    private fun legacyShowCamera(call: PluginCall) {
        if (!getContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            call.reject(NO_CAMERA_ERROR)
            return
        }
        legacyOpenCamera(call)
    }

    private fun legacyShowPhotos(call: PluginCall) {
        legacyOpenPhotos(call)
    }

    // ========================================
    // SECTION 15: LEGACY FLOW - CAMERA/PHOTOS
    // ========================================

    private fun legacyOpenCamera(call: PluginCall) {
        if (legacyCheckCameraPermissions(call)) {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(getContext().packageManager) != null) {
                // If we will be saving the photo, send the target file along
                try {
                    val appId = getAppId()
                    val photoFile = CameraUtils.createImageFile(getActivity())
                    imageFileSavePath = photoFile.absolutePath
                    // TODO: Verify provider config exists
                    imageFileUri = FileProvider.getUriForFile(getActivity(), "$appId.fileprovider", photoFile)
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageFileUri)
                } catch (ex: Exception) {
                    call.reject(IMAGE_FILE_SAVE_ERROR, ex)
                    return
                }

                startActivityForResult(call, takePictureIntent, "processCameraImage")
            } else {
                call.reject(NO_CAMERA_ACTIVITY_ERROR)
            }
        }
    }

    private fun legacyOpenPhotos(call: PluginCall, multiple: Boolean = false) {
        try {
            if (multiple) {
                pickMultipleMedia = legacyRegisterActivityResultLauncher(legacyGetContractForCall(call)) { uris ->
                    if (uris.isNotEmpty()) {
                        val executor = Executors.newSingleThreadExecutor()
                        executor.execute {
                            val ret = JSObject()
                            val photos = JSArray()
                            for (imageUri in uris) {
                                try {
                                    val processResult = legacyProcessPickedImages(imageUri)
                                    if (processResult.getString("error") != null && processResult.getString("error")!!.isNotEmpty()) {
                                        call.reject(processResult.getString("error"))
                                        return@execute
                                    } else {
                                        photos.put(processResult)
                                    }
                                } catch (ex: SecurityException) {
                                    call.reject("SecurityException")
                                }
                            }
                            ret.put("photos", photos)
                            call.resolve(ret)
                        }
                    } else {
                        call.reject(USER_CANCELLED)
                    }
                    pickMultipleMedia?.unregister()
                }
                pickMultipleMedia?.launch(
                    PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly).build()
                )
            } else {
                pickMedia = legacyRegisterActivityResultLauncher(ActivityResultContracts.PickVisualMedia()) { uri ->
                    if (uri != null) {
                        imagePickedContentUri = uri
                        legacyProcessPickedImage(uri, call)
                    } else {
                        call.reject(USER_CANCELLED)
                    }
                    pickMedia?.unregister()
                }
                pickMedia?.launch(
                    PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly).build()
                )
            }
        } catch (ex: ActivityNotFoundException) {
            call.reject(NO_PHOTO_ACTIVITY_ERROR)
        }
    }

    private fun <I, O> legacyRegisterActivityResultLauncher(
        contract: ActivityResultContract<I, O>,
        callback: ActivityResultCallback<O>
    ): ActivityResultLauncher<I> {
        val key = "cap_activity_rq#${mNextLocalRequestCode.getAndIncrement()}"
        if (getBridge().fragment != null) {
            val host = getBridge().fragment.host
            if (host is ActivityResultRegistryOwner) {
                return host.activityResultRegistry.register(key, contract, callback)
            }
            return getBridge().fragment.requireActivity().activityResultRegistry.register(key, contract, callback)
        }
        return getBridge().activity.activityResultRegistry.register(key, contract, callback)
    }

    private fun legacyGetContractForCall(call: PluginCall): ActivityResultContract<PickVisualMediaRequest, List<Uri>> {
        var limit = call.getInt("limit", 0) ?: 0

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val maxLimit = MediaStore.getPickImagesMaxLimit()
            if (limit > maxLimit) {
                limit = maxLimit
            }
        }

        return if (limit > 1) {
            ActivityResultContracts.PickMultipleVisualMedia(limit)
        } else {
            ActivityResultContracts.PickMultipleVisualMedia()
        }
    }

    // ========================================
    // SECTION 16: LEGACY FLOW - IMAGE PROCESSING
    // ========================================

    private fun legacyProcessCameraImage(call: PluginCall, result: ActivityResult) {
        cameraSettings = getSettings(call)
        if (imageFileSavePath == null) {
            call.reject(IMAGE_PROCESS_NO_FILE_ERROR)
            return
        }
        // Load the image as a Bitmap
        val f = File(imageFileSavePath!!)
        val bmOptions = BitmapFactory.Options()
        val contentUri = Uri.fromFile(f)
        val bitmap = BitmapFactory.decodeFile(imageFileSavePath, bmOptions)

        if (bitmap == null) {
            call.reject(USER_CANCELLED)
            return
        }

        legacyReturnResult(call, bitmap, contentUri)
    }

    private fun legacyProcessPickedImage(imageUri: Uri, call: PluginCall) {
        var imageStream: InputStream? = null

        try {
            imageStream = getContext().contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(imageStream)

            if (bitmap == null) {
                call.reject("Unable to process bitmap")
                return
            }

            legacyReturnResult(call, bitmap, imageUri)
        } catch (err: OutOfMemoryError) {
            call.reject("Out of memory")
        } catch (ex: FileNotFoundException) {
            call.reject("No such image found", ex)
        } finally {
            if (imageStream != null) {
                try {
                    imageStream.close()
                } catch (e: IOException) {
                    Logger.error(getLogTag(), UNABLE_TO_PROCESS_IMAGE, e)
                }
            }
        }
    }

    private fun legacyProcessPickedImages(imageUri: Uri): JSObject {
        var imageStream: InputStream? = null
        val ret = JSObject()
        try {
            imageStream = getContext().contentResolver.openInputStream(imageUri)
            var bitmap = BitmapFactory.decodeStream(imageStream)

            if (bitmap == null) {
                ret.put("error", "Unable to process bitmap")
                return ret
            }

            val exif = ImageUtils.getExifData(getContext(), bitmap, imageUri)
            try {
                bitmap = legacyPrepareBitmap(bitmap, imageUri, exif)
            } catch (e: IOException) {
                ret.put("error", UNABLE_TO_PROCESS_IMAGE)
                return ret
            }
            // Compress the final image and prepare for output to client
            val bitmapOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, cameraSettings?.quality ?: 90, bitmapOutputStream)

            val newUri = legacyGetTempImage(imageUri, bitmapOutputStream)
            exif.copyExif(newUri?.path)
            if (newUri != null) {
                ret.put("format", "jpeg")
                ret.put("exif", exif.toJson())
                ret.put("path", newUri.toString())
                ret.put("webPath", FileUtils.getPortablePath(getContext(), getBridge().localUrl, newUri))
            } else {
                ret.put("error", UNABLE_TO_PROCESS_IMAGE)
            }
            return ret
        } catch (err: OutOfMemoryError) {
            ret.put("error", "Out of memory")
        } catch (ex: FileNotFoundException) {
            ret.put("error", "No such image found")
            Logger.error(getLogTag(), "No such image found", ex)
        } finally {
            if (imageStream != null) {
                try {
                    imageStream.close()
                } catch (e: IOException) {
                    Logger.error(getLogTag(), UNABLE_TO_PROCESS_IMAGE, e)
                }
            }
        }
        return ret
    }

    private fun legacyProcessEditedImage(call: PluginCall, result: ActivityResult) {
        isEdited = true
        cameraSettings = getSettings(call)
        if (result.resultCode == Activity.RESULT_CANCELED) {
            // User cancelled the edit operation, if this file was picked from photos,
            // process the original picked image, otherwise process it as a camera photo
            if (imagePickedContentUri != null) {
                legacyProcessPickedImage(imagePickedContentUri!!, call)
            } else {
                legacyProcessCameraImage(call, result)
            }
        } else {
            val data = result.data
            if (data == null) {
                call.reject(USER_CANCELLED)
                return
            }

            val u = data.data
            imagePickedContentUri = u

            if (u != null) {
                legacyProcessPickedImage(u, call)
            }
        }
    }

    // ========================================
    // SECTION 17: LEGACY FLOW - RESULT HANDLING
    // ========================================

    @Suppress("DEPRECATION")
    private fun legacyReturnResult(call: PluginCall, bitmap: Bitmap, u: Uri) {
        var processedBitmap = bitmap
        val exif = ImageUtils.getExifData(getContext(), processedBitmap, u)
        try {
            processedBitmap = legacyPrepareBitmap(processedBitmap, u, exif)
        } catch (e: IOException) {
            call.reject(UNABLE_TO_PROCESS_IMAGE)
            return
        }
        // Compress the final image and prepare for output to client
        val bitmapOutputStream = ByteArrayOutputStream()
        processedBitmap.compress(Bitmap.CompressFormat.JPEG, cameraSettings?.quality ?: 90, bitmapOutputStream)

        if (cameraSettings?.allowEditing == true && !isEdited) {
            legacyEditImage(call, u, bitmapOutputStream)
            return
        }

        val saveToGallery = call.getBoolean("saveToGallery", CameraSettings.DEFAULT_SAVE_IMAGE_TO_GALLERY) ?: false
        if (saveToGallery && (imageEditedFileSavePath != null || imageFileSavePath != null)) {
            isSaved = true
            try {
                val fileToSavePath = imageEditedFileSavePath ?: imageFileSavePath
                val fileToSave = File(fileToSavePath!!)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = getContext().contentResolver
                    val values = ContentValues()
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileToSave.name)
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM)

                    val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    val uri = resolver.insert(contentUri, values)

                    if (uri == null) {
                        throw IOException("Failed to create new MediaStore record.")
                    }

                    val stream = resolver.openOutputStream(uri)
                    if (stream == null) {
                        throw IOException("Failed to open output stream.")
                    }

                    val inserted = processedBitmap.compress(Bitmap.CompressFormat.JPEG, cameraSettings?.quality ?: 90, stream)

                    if (!inserted) {
                        isSaved = false
                    }
                } else {
                    val inserted = MediaStore.Images.Media.insertImage(
                        getContext().contentResolver,
                        fileToSavePath,
                        fileToSave.name,
                        ""
                    )

                    if (inserted == null) {
                        isSaved = false
                    }
                }
            } catch (e: FileNotFoundException) {
                isSaved = false
                Logger.error(getLogTag(), IMAGE_GALLERY_SAVE_ERROR, e)
            } catch (e: IOException) {
                isSaved = false
                Logger.error(getLogTag(), IMAGE_GALLERY_SAVE_ERROR, e)
            }
        }

        when (cameraSettings?.resultType) {
            CameraResultType.BASE64 -> legacyReturnBase64(call, exif, bitmapOutputStream)
            CameraResultType.URI -> legacyReturnFileURI(call, exif, processedBitmap, u, bitmapOutputStream)
            CameraResultType.DATAURL -> legacyReturnDataUrl(call, exif, bitmapOutputStream)
            else -> call.reject(INVALID_RESULT_TYPE_ERROR)
        }
        // Result returned, clear stored paths and images
        if (cameraSettings?.resultType != CameraResultType.URI) {
            legacyDeleteImageFile()
        }
        imageFileSavePath = null
        imageFileUri = null
        imagePickedContentUri = null
        imageEditedFileSavePath = null
    }

    private fun legacyReturnFileURI(call: PluginCall, exif: ExifWrapper, bitmap: Bitmap, u: Uri, bitmapOutputStream: ByteArrayOutputStream) {
        val newUri = legacyGetTempImage(u, bitmapOutputStream)
        exif.copyExif(newUri?.path)
        if (newUri != null) {
            val ret = JSObject()
            ret.put("format", "jpeg")
            ret.put("exif", exif.toJson())
            ret.put("path", newUri.toString())
            ret.put("webPath", FileUtils.getPortablePath(getContext(), getBridge().localUrl, newUri))
            ret.put("saved", isSaved)
            call.resolve(ret)
        } else {
            call.reject(UNABLE_TO_PROCESS_IMAGE)
        }
    }

    private fun legacyReturnDataUrl(call: PluginCall, exif: ExifWrapper, bitmapOutputStream: ByteArrayOutputStream) {
        val byteArray = bitmapOutputStream.toByteArray()
        val encoded = Base64.encodeToString(byteArray, Base64.NO_WRAP)

        val data = JSObject()
        data.put("format", "jpeg")
        data.put("dataUrl", "data:image/jpeg;base64,$encoded")
        data.put("exif", exif.toJson())
        call.resolve(data)
    }

    private fun legacyReturnBase64(call: PluginCall, exif: ExifWrapper, bitmapOutputStream: ByteArrayOutputStream) {
        val byteArray = bitmapOutputStream.toByteArray()
        val encoded = Base64.encodeToString(byteArray, Base64.NO_WRAP)

        val data = JSObject()
        data.put("format", "jpeg")
        data.put("base64String", encoded)
        data.put("exif", exif.toJson())
        call.resolve(data)
    }

    // ========================================
    // SECTION 18: LEGACY FLOW - IMAGE EDITING
    // ========================================

    private fun legacyEditImage(call: PluginCall, uri: Uri, bitmapOutputStream: ByteArrayOutputStream) {
        try {
            val tempImage = legacyGetTempImage(uri, bitmapOutputStream)
            val editIntent = legacyCreateEditIntent(tempImage)
            if (editIntent != null) {
                startActivityForResult(call, editIntent, "processEditedImage")
            } else {
                call.reject(IMAGE_EDIT_ERROR)
            }
        } catch (ex: Exception) {
            call.reject(IMAGE_EDIT_ERROR, ex)
        }
    }

    private fun legacyCreateEditIntent(origPhotoUri: Uri?): Intent? {
        try {
            val editFile = File(origPhotoUri?.path)
            val editUri = FileProvider.getUriForFile(
                getActivity(),
                "${getContext().packageName}.fileprovider",
                editFile
            )
            val editIntent = Intent(Intent.ACTION_EDIT)
            editIntent.setDataAndType(editUri, "image/*")
            imageEditedFileSavePath = editFile.absolutePath
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            editIntent.addFlags(flags)
            editIntent.putExtra(MediaStore.EXTRA_OUTPUT, editUri)

            val resInfoList: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getContext()
                    .packageManager
                    .queryIntentActivities(editIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
            } else {
                legacyQueryIntentActivities(editIntent)
            }

            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                getContext().grantUriPermission(packageName, editUri, flags)
            }
            return editIntent
        } catch (ex: Exception) {
            return null
        }
    }

    // ========================================
    // SECTION 19: LEGACY FLOW - PERMISSIONS
    // ========================================

    private fun legacyCheckCameraPermissions(call: PluginCall): Boolean {
        // if the manifest does not contain the camera permissions key, we don't need to ask the user
        val needCameraPerms = isPermissionDeclared(CAMERA)
        val hasCameraPerms = !needCameraPerms || getPermissionState(CAMERA) == PermissionState.GRANTED
        val hasGalleryPerms = getPermissionState(SAVE_GALLERY) == PermissionState.GRANTED

        // If we want to save to the gallery, we need two permissions
        // actually we only need permissions to save to gallery for Android <= 9 (API 28)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // we might still need to request permission for the camera
            if (!hasCameraPerms) {
                requestPermissionForAlias(CAMERA, call, "cameraPermissionsCallback")
                return false
            }
            return true
        }

        // we need to request permissions to save to gallery for Android <= 9
        if (cameraSettings?.saveToGallery == true && !(hasCameraPerms && hasGalleryPerms) && isFirstRequestLegacy) {
            isFirstRequestLegacy = false
            val aliases: Array<String> = if (needCameraPerms) {
                arrayOf(CAMERA, SAVE_GALLERY)
            } else {
                arrayOf(SAVE_GALLERY)
            }
            requestPermissionForAliases(aliases, call, "cameraPermissionsCallback")
            return false
        }
        // If we don't need to save to the gallery, we can just ask for camera permissions
        else if (!hasCameraPerms) {
            requestPermissionForAlias(CAMERA, call, "cameraPermissionsCallback")
            return false
        }
        return true
    }

    private fun legacyHandleCameraPermissionsCallback(call: PluginCall) {
        if (call.methodName == "pickImages") {
            legacyOpenPhotos(call, true)
        } else {
            if (cameraSettings?.source == CameraSource.CAMERA && getPermissionState(CAMERA) != PermissionState.GRANTED) {
                Logger.debug(getLogTag(), "User denied camera permission: ${getPermissionState(CAMERA)}")
                call.reject(PERMISSION_DENIED_ERROR_CAMERA)
                return
            }
            legacyDoShow(call)
        }
    }

    // ========================================
    // SECTION 20: LEGACY FLOW - HELPERS
    // ========================================

    private fun legacyPrepareBitmap(bitmap: Bitmap, imageUri: Uri, exif: ExifWrapper): Bitmap {
        var processedBitmap = bitmap
        if (cameraSettings?.shouldCorrectOrientation == true) {
            val newBitmap = ImageUtils.correctOrientation(getContext(), processedBitmap, imageUri, exif)
            processedBitmap = legacyReplaceBitmap(processedBitmap, newBitmap)
        }

        if (cameraSettings?.shouldResize == true) {
            val newBitmap = ImageUtils.resize(processedBitmap, cameraSettings?.width ?: 0, cameraSettings?.height ?: 0)
            processedBitmap = legacyReplaceBitmap(processedBitmap, newBitmap)
        }

        return processedBitmap
    }

    private fun legacyReplaceBitmap(bitmap: Bitmap, newBitmap: Bitmap): Bitmap {
        var result = newBitmap
        if (bitmap != newBitmap) {
            bitmap.recycle()
        } else {
            result = bitmap
        }
        return result
    }

    private fun legacyGetTempImage(u: Uri, bitmapOutputStream: ByteArrayOutputStream): Uri? {
        var bis: ByteArrayInputStream? = null
        var newUri: Uri? = null
        try {
            bis = ByteArrayInputStream(bitmapOutputStream.toByteArray())
            newUri = legacySaveImage(u, bis)
        } catch (ex: IOException) {
        } finally {
            if (bis != null) {
                try {
                    bis.close()
                } catch (e: IOException) {
                    Logger.error(getLogTag(), UNABLE_TO_PROCESS_IMAGE, e)
                }
            }
        }
        return newUri
    }

    private fun legacySaveImage(uri: Uri, inputStream: InputStream): Uri {
        var outFile: File? = if (uri.scheme == "content") {
            legacyGetTempFile(uri)
        } else {
            File(uri.path)
        }
        try {
            legacyWritePhoto(outFile!!, inputStream)
        } catch (ex: FileNotFoundException) {
            // Some gallery apps return read only file url, create a temporary file for modifications
            outFile = legacyGetTempFile(uri)
            legacyWritePhoto(outFile, inputStream)
        }
        return Uri.fromFile(outFile)
    }

    private fun legacyWritePhoto(outFile: File, inputStream: InputStream) {
        val fos = FileOutputStream(outFile)
        val buffer = ByteArray(1024)
        var len: Int
        while (inputStream.read(buffer).also { len = it } != -1) {
            fos.write(buffer, 0, len)
        }
        fos.close()
    }

    private fun legacyGetTempFile(uri: Uri): File {
        var filename = Uri.parse(Uri.decode(uri.toString())).lastPathSegment ?: "image.jpeg"
        if (!filename.contains(".jpg") && !filename.contains(".jpeg")) {
            filename += ".${java.util.Date().time}.jpeg"
        }
        val cacheDir = getContext().cacheDir
        return File(cacheDir, filename)
    }

    private fun legacyDeleteImageFile() {
        if (imageFileSavePath != null && cameraSettings?.saveToGallery != true) {
            val photoFile = File(imageFileSavePath!!)
            if (photoFile.exists()) {
                photoFile.delete()
            }
        }
    }

    // ========================================
    // SECTION 21: COMMON UTILITIES
    // ========================================

    fun getSettings(call: PluginCall): CameraSettings {
        val settings = CameraSettings()
        settings.resultType = getResultType(call.getString("resultType"))
        settings.saveToGallery =
            call.getBoolean("saveToGallery", CameraSettings.DEFAULT_SAVE_IMAGE_TO_GALLERY)!!
        settings.allowEditing = call.getBoolean("allowEditing", false)!!
        settings.quality = call.getInt("quality", CameraSettings.DEFAULT_QUALITY)!!
        settings.width = call.getInt("width", 0)!!
        settings.height = call.getInt("height", 0)!!
        settings.shouldResize = settings.width > 0 || settings.height > 0
        settings.shouldCorrectOrientation =
            call.getBoolean("correctOrientation", CameraSettings.DEFAULT_CORRECT_ORIENTATION)!!
        try {
            settings.source =
                CameraSource.valueOf(call.getString("source", CameraSource.PROMPT.getSource())!!)
        } catch (ex: IllegalArgumentException) {
            settings.source = CameraSource.PROMPT
        }
        return settings
    }

    fun getVideoSettings(call: PluginCall): VideoSettings {
        return VideoSettings(
            saveToGallery = call.getBoolean("saveToGallery") ?: false,
            includeMetadata = call.getBoolean("includeMetadata") ?: false,
            isPersistent = call.getBoolean("isPersistent") ?: true
        )
    }

    fun getGallerySettings(call: PluginCall): GallerySettings {
        return GallerySettings(
            mediaType = IONCAMRMediaType.fromValue((call.getInt("mediaType") ?: 0)),
            allowMultipleSelection = call.getBoolean("allowMultipleSelection") ?: false,
            includeMetadata = call.getBoolean("includeMetadata") ?: false,
            allowEdit = call.getBoolean("allowEdit") ?: false,
            limit = call.getInt("limit") ?: 0,
            editInApp = call.getBoolean("editInApp") ?: true
        )
    }

    private fun getResultType(resultType: String?): CameraResultType? {
        if (resultType == null) {
            return null
        }
        try {
            return CameraResultType.valueOf(resultType.uppercase())
        } catch (ex: java.lang.IllegalArgumentException) {
            Logger.debug(
                getLogTag(),
                "Invalid result type \"$resultType\", defaulting to base64"
            )
            return CameraResultType.BASE64
        }
    }

    override fun requestPermissionForAliases(
        aliases: Array<String>,
        call: PluginCall,
        callbackName: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            for (i in aliases.indices) {
                if (aliases[i] == SAVE_GALLERY) {
                    aliases[i] = READ_EXTERNAL_STORAGE
                }
            }
        }
        super.requestPermissionForAliases(aliases, call, callbackName)
    }

    override fun getPermissionStates(): MutableMap<String?, PermissionState?> {
        val permissionStates = super.getPermissionStates()

        // If Camera is not in the manifest and therefore not required, say the permission is granted
        if (!isPermissionDeclared(CAMERA)) {
            permissionStates.put(CAMERA, PermissionState.GRANTED)
        }

        if (permissionStates.containsKey(PHOTOS)) {
            permissionStates.put(PHOTOS, PermissionState.GRANTED)
        }

        // If the SDK version is 30 or higher, update the SAVE_GALLERY state to match the READ_EXTERNAL_STORAGE state.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val alias: String = READ_EXTERNAL_STORAGE
            if (permissionStates.containsKey(alias)) {
                permissionStates.put(SAVE_GALLERY, permissionStates.get(alias))
            }
        }

        return permissionStates
    }

    @Suppress("deprecation")
    private fun legacyQueryIntentActivities(intent: Intent): MutableList<ResolveInfo> {
        return getContext().packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }

    // ========================================
    // SECTION 22: ION ERROR HANDLING
    // ========================================

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
        }
    }

    private fun formatErrorCode(code: Int): String {
        val stringCode = Integer.toString(code)
        return ERROR_FORMAT_PREFIX + "0000$stringCode".substring(stringCode.length)
    }

    // ========================================
    // SECTION 23: EXTENSION FUNCTIONS
    // ========================================

    private fun CameraSettings.toIonParameters(): IONCAMRCameraParameters {
        val useLatestVersion = (resultType == CameraResultType.URI)
        return IONCAMRCameraParameters(
            mQuality = quality,
            targetWidth = width,
            targetHeight = height,
            encodingType = ENCODING_TYPE, // JPEG
            mediaType = MEDIA_TYPE_PICTURE,
            allowEdit = allowEditing,
            correctOrientation = shouldCorrectOrientation,
            saveToPhotoAlbum = saveToGallery,
            //TODO this value should come from settings, possibly from new interface TakePhotoOptions
            includeMetadata = false,
            latestVersion = useLatestVersion
        )
    }
}
