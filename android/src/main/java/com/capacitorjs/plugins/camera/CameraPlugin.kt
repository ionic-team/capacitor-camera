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
import android.os.Parcelable
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
import io.ionic.libs.ioncameralib.view.IONCAMRImageEditorActivity
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
import java.util.Locale
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
    }

    // ============================================
    // STATE MANAGEMENT - ION FLOW
    // ============================================
    private var ionIsFirstRequest = true
    private var ionCameraManager: IONCAMRCameraManager? = null
    private var ionVideoManager: IONCAMRVideoManager? = null
    private var ionEditManager: IONCAMREditManager? = null
    private var ionGalleryManager: IONCAMRGalleryManager? = null
    private var ionCurrentCall: PluginCall? = null
    private var ionCameraSettings: IonCameraSettings? = null
    private var ionVideoSettings: IonVideoSettings? = null
    private var ionGallerySettings: IonGallerySettings? = null
    private var ionEditParameters = IONCAMREditParameters(
        editURI = "", fromUri = false, saveToGallery = false, includeMetadata = false
    )
    private var ionLastEditUri: String? = null

    // ============================================
    // STATE MANAGEMENT - LEGACY FLOW
    // ============================================
    private var legacyImageFileSavePath: String? = null
    private var legacyImageEditedFileSavePath: String? = null
    private var legacyImageFileUri: Uri? = null
    private var legacyImagePickedContentUri: Uri? = null
    private var legacyIsEdited = false
    private var legacyIsFirstRequest = true
    private var legacyIsSaved = false
    private var legacySettings = LegacyCameraSettings()
    private val legacyNextLocalRequestCode = AtomicInteger()

    // ============================================
    // ACTIVITY RESULT LAUNCHERS
    // ============================================
    // Ion flow launchers
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraCropLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryCropLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var videoLauncher: ActivityResultLauncher<Intent>
    private lateinit var editLauncher: ActivityResultLauncher<Intent>

    // Legacy flow launchers (dynamically registered)
    private var pickMultipleMedia: ActivityResultLauncher<PickVisualMediaRequest>? = null
    private var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>? = null

    // ============================================
    // LIFECYCLE METHODS
    // ============================================

    override fun load() {
        super.load()
        setupLaunchers()
        ionCameraManager = IONCAMRCameraManager(
            getAppId(),
            IONCAMRExifHelper(),
            IONCAMRFileHelper(),
            IONCAMRMediaHelper(),
            IONCAMRImageHelper()
        )

        ionVideoManager = IONCAMRVideoManager(
            IONCAMRFileHelper(),
        )

        ionGalleryManager = IONCAMRGalleryManager(
            IONCAMRExifHelper(),
            IONCAMRFileHelper(),
            IONCAMRMediaHelper(),
            IONCAMRImageHelper()
        )

        ionEditManager = IONCAMREditManager(
            getAppId(),
            IONCAMRExifHelper(),
            IONCAMRFileHelper(),
            IONCAMRMediaHelper(),
            IONCAMRImageHelper()
        )

        ionCameraManager?.deleteVideoFilesFromCache(activity)
    }

    // ============================================
    // ACTIVITY RESULT LAUNCHER SETUP
    // ============================================

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

    private fun <I, O> registerActivityResultLauncher(
        contract: ActivityResultContract<I, O>,
        callback: ActivityResultCallback<O>
    ): ActivityResultLauncher<I> {
        val key = "cap_activity_rq#" + legacyNextLocalRequestCode.getAndIncrement()
        if (getBridge().fragment != null) {
            val host = getBridge().fragment!!.host
            if (host is ActivityResultRegistryOwner) {
                return host.activityResultRegistry.register(key, contract, callback)
            }
            return getBridge().fragment!!.requireActivity().activityResultRegistry.register(key, contract, callback)
        }
        return getBridge().activity.activityResultRegistry.register(key, contract, callback)
    }

    private fun getContractForCall(call: PluginCall): ActivityResultContract<PickVisualMediaRequest, List<Uri>> {
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

    // ============================================
    // ION FLOW - SETTINGS & UTILITIES
    // ============================================

    private fun getIonCameraSettings(call: PluginCall): IonCameraSettings {
        val settings = IonCameraSettings()
        settings.resultType = CameraResultType.URI
        settings.saveToGallery = call.getBoolean("saveToGallery", IonCameraSettings.DEFAULT_SAVE_IMAGE_TO_GALLERY)!!
        settings.allowEdit = call.getBoolean("allowEdit", false)!!
        settings.quality = call.getInt("quality", IonCameraSettings.DEFAULT_QUALITY)!!
        settings.width = call.getInt("width", 0)!!
        settings.height = call.getInt("height", 0)!!
        settings.shouldResize = settings.width > 0 || settings.height > 0
        settings.shouldCorrectOrientation =
            call.getBoolean("correctOrientation", IonCameraSettings.DEFAULT_CORRECT_ORIENTATION)!!
        settings.editInApp = call.getBoolean("editInApp", true)!!
        settings.includeMetadata = call.getBoolean("includeMetadata", false)!!

        try {
            settings.source =
                CameraSource.valueOf(call.getString("source", CameraSource.PROMPT.getSource())!!)
        } catch (ex: IllegalArgumentException) {
            settings.source = CameraSource.PROMPT
        }
        return settings
    }

    private fun getIonVideoSettings(call: PluginCall): IonVideoSettings {
        return IonVideoSettings(
            saveToGallery = call.getBoolean("saveToGallery") ?: false,
            includeMetadata = call.getBoolean("includeMetadata") ?: false,
            isPersistent = call.getBoolean("isPersistent") ?: true
        )
    }

    private fun getIonGallerySettings(call: PluginCall): IonGallerySettings {
        return IonGallerySettings(
            mediaType = IONCAMRMediaType.fromValue((call.getInt("mediaType") ?: 0)),
            allowMultipleSelection = call.getBoolean("allowMultipleSelection") ?: false,
            includeMetadata = call.getBoolean("includeMetadata") ?: false,
            allowEdit = call.getBoolean("allowEdit") ?: false,
            limit = call.getInt("limit") ?: 0,
            editInApp = call.getBoolean("editInApp") ?: true
        )
    }

    private fun IonCameraSettings.toIonParameters(): IONCAMRCameraParameters {
        val useLatestVersion = (resultType == CameraResultType.URI)
        return IONCAMRCameraParameters(
            mQuality = quality,
            targetWidth = width,
            targetHeight = height,
            encodingType = ENCODING_TYPE,
            mediaType = MEDIA_TYPE_PICTURE,
            allowEdit = allowEdit,
            correctOrientation = shouldCorrectOrientation,
            saveToPhotoAlbum = saveToGallery,
            includeMetadata = includeMetadata,
            latestVersion = useLatestVersion
        )
    }

    private fun sendIonError(error: IONCAMRError) {
        try {
            val jsonResult = JSObject()
            jsonResult.put("code", formatIonErrorCode(error.code))
            jsonResult.put("message", error.description)
            ionCurrentCall?.reject(error.description, formatIonErrorCode(error.code))
            ionCurrentCall = null
        } catch (e: Exception) {
            ionCurrentCall?.reject("There was an error performing the operation.")
            ionCurrentCall = null
        } finally {
            ionLastEditUri = null
        }
    }

    private fun formatIonErrorCode(code: Int): String {
        val stringCode = Integer.toString(code)
        return ERROR_FORMAT_PREFIX + "0000$stringCode".substring(stringCode.length)
    }

    // ============================================
    // ION FLOW - CAMERA OPERATIONS
    // ============================================

    private fun showIonCamera(call: PluginCall) {
        if (!getContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            sendIonError(IONCAMRError.NO_CAMERA_AVAILABLE_ERROR)
            return
        }
        openIonCamera(call)
    }

    private fun openIonCamera(call: PluginCall) {
        val settings = ionCameraSettings ?: run {
            sendIonError(IONCAMRError.INVALID_ARGUMENT_ERROR)
            return
        }
        if (checkIonCameraPermissions(call, settings.saveToGallery)) {
            try {
                val manager = ionCameraManager ?: run {
                    sendIonError(IONCAMRError.CONTEXT_ERROR)
                    return
                }
                ionCurrentCall = call
                manager.takePhoto(activity, ENCODING_TYPE, cameraLauncher)
            } catch (ex: Exception) {
                sendIonError(IONCAMRError.FAILED_TO_CAPTURE_IMAGE_ERROR)
            }
        }
    }

    private fun openRecordVideo(call: PluginCall) {
        val settings = ionVideoSettings ?: run {
            sendIonError(IONCAMRError.INVALID_ARGUMENT_ERROR)
            return
        }

        if (checkIonCameraPermissions(call, settings.saveToGallery)) {
            try {
                val manager = ionCameraManager ?: run {
                    sendIonError(IONCAMRError.CONTEXT_ERROR)
                    return
                }
                ionCurrentCall = call
                manager.recordVideo(
                    activity,
                    settings.saveToGallery,
                    videoLauncher
                ) {
                    sendIonError(it)
                }
            } catch (ex: Exception) {
                sendIonError(IONCAMRError.VIDEO_CAPTURE_NOT_SUPPORTED_ERROR)
            }
        }
    }

    private fun openPlayVideo(call: PluginCall) {
        try {
            val manager = ionVideoManager ?: run {
                sendIonError(IONCAMRError.CONTEXT_ERROR)
                return
            }

            val videoUri = call.getString("videoURI")
                ?: return sendIonError(IONCAMRError.PLAY_VIDEO_GENERAL_ERROR)
            manager.playVideo(activity, videoUri, {
                call.resolve()
            }, {
                sendIonError(it)
            })
        } catch (_: Exception) {
            sendIonError(IONCAMRError.PLAY_VIDEO_GENERAL_ERROR)
            return
        }
    }

    private fun openIonGallery(call: PluginCall) {
        val manager = ionGalleryManager ?: run {
            sendIonError(IONCAMRError.CONTEXT_ERROR)
            return
        }

        val settings = ionGallerySettings ?: run {
            sendIonError(IONCAMRError.INVALID_ARGUMENT_ERROR)
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
        val manager = ionEditManager ?: run {
            sendIonError(IONCAMRError.CONTEXT_ERROR)
            return
        }

        ionEditParameters = IONCAMREditParameters(
            "",
            fromUri = false,
            saveToGallery = false,
            includeMetadata = false
        )
        val imageBase64 = call.data.getString("base64")
        if (imageBase64 == null) return
        manager.editImage(activity, imageBase64, editLauncher)
    }

    private fun callEditURIPhoto(call: PluginCall) {
        val manager = ionEditManager ?: run {
            sendIonError(IONCAMRError.CONTEXT_ERROR)
            return
        }

        val photoPath = call.getString("uri")
        val saveToGallery = call.getBoolean("saveToGallery") ?: false
        val includeMetadata = call.getBoolean("includeMetadata") ?: false
        if (photoPath == null) return

        ionEditParameters = IONCAMREditParameters(
            editURI = photoPath,
            fromUri = true,
            saveToGallery = saveToGallery,
            includeMetadata = includeMetadata
        )

        manager.editURIPicture(activity, photoPath, editLauncher) {
            sendIonError(IONCAMRError.EDIT_IMAGE_ERROR)
        }
    }

    private fun editIonPhoto() {
        val editor = ionEditManager ?: run {
            sendIonError(IONCAMRError.CONTEXT_ERROR)
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

    private fun createIonEditIntent(origPhotoUri: Uri): Intent? {
        return try {
            var editUri = origPhotoUri
            if (origPhotoUri.scheme == "file") {
                val editFile = File(origPhotoUri.path!!)
                editUri = FileProvider.getUriForFile(
                    activity,
                    context.packageName + ".fileprovider",
                    editFile
                )
                ionLastEditUri = editFile.absolutePath
            } else if (origPhotoUri.scheme == "content") {
                val tempUri = IonCameraUtils.getCameraTempImage(activity, origPhotoUri) ?: return null
                val editFile = File(tempUri.path!!)
                editUri = FileProvider.getUriForFile(
                    activity,
                    context.packageName + ".fileprovider",
                    editFile
                )
                ionLastEditUri = editFile.absolutePath
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
                resInfoList = ionLegacyQueryIntentActivities(editIntent)
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
    private fun ionLegacyQueryIntentActivities(intent: Intent): MutableList<ResolveInfo> {
        return context.packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }

    // ============================================
    // ION FLOW - PERMISSION HANDLING
    // ============================================

    private fun checkIonCameraPermissions(call: PluginCall, saveToGallery: Boolean): Boolean {
        val needCameraPerms = isPermissionDeclared(CAMERA)
        val hasCameraPerms =
            !needCameraPerms || getPermissionState(CAMERA) == PermissionState.GRANTED
        val hasGalleryPerms =
            getPermissionState(SAVE_GALLERY) == PermissionState.GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!hasCameraPerms) {
                requestLegacyPermissionForAlias(
                    CAMERA,
                    call,
                    "ionCameraPermissionsCallback"
                )
                return false
            }
            return true
        }

        if (saveToGallery && !(hasCameraPerms && hasGalleryPerms) && ionIsFirstRequest) {
            ionIsFirstRequest = false
            val aliases: Array<String> = if (needCameraPerms) {
                arrayOf(CAMERA, SAVE_GALLERY)
            } else {
                arrayOf(SAVE_GALLERY)
            }
            requestLegacyPermissionForAliases(aliases, call, "ionCameraPermissionsCallback")
            return false
        } else if (!hasCameraPerms) {
            requestLegacyPermissionForAlias(
                CAMERA,
                call,
                "ionCameraPermissionsCallback"
            )
            return false
        }
        return true
    }

    private fun handleIonCameraPermissionsCallback(call: PluginCall) {
        if (getPermissionState(CAMERA) != PermissionState.GRANTED) {
            sendIonError(IONCAMRError.CAMERA_PERMISSION_DENIED_ERROR)
            return
        }

        when (call.methodName) {
            "takePhoto" -> openIonCamera(call)
            "recordVideo" -> openRecordVideo(call)
            "pickImages" -> openIonGallery(call)
            else -> sendIonError(IONCAMRError.CONTEXT_ERROR)
        }
    }

    // ============================================
    // ION FLOW - RESULT HANDLERS
    // ============================================

    private fun handleCameraResult(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val settings = ionCameraSettings ?: run {
                    sendIonError(IONCAMRError.INVALID_ARGUMENT_ERROR)
                    return
                }
                if (settings.allowEdit) {
                    if (settings.editInApp) {
                        editIonPhoto()
                    } else {
                        val editor = ionEditManager ?: run {
                            sendIonError(IONCAMRError.CONTEXT_ERROR)
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

                        val editIntent = createIonEditIntent(tmpFile)
                        if (editIntent != null) {
                            cameraCropLauncher.launch(editIntent)
                        } else {
                            editIonPhoto()
                        }
                    }
                } else {
                    processIonResult(result.data)
                }
            }
            Activity.RESULT_CANCELED -> {
                sendIonError(IONCAMRError.NO_PICTURE_TAKEN_ERROR)
            }
            else -> {
                sendIonError(IONCAMRError.TAKE_PHOTO_ERROR)
            }
        }
    }

    private fun handleVideoResult(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                processResultFromVideo(result)
            }
            Activity.RESULT_CANCELED -> {
                sendIonError(IONCAMRError.CAPTURE_VIDEO_CANCELLED_ERROR)
            }
            else -> sendIonError(IONCAMRError.CAPTURE_VIDEO_ERROR)
        }
    }

    private fun handleGalleryResult(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val editor = ionEditManager ?: run {
                    sendIonError(IONCAMRError.CONTEXT_ERROR)
                    return
                }

                val manager = ionGalleryManager ?: run {
                    sendIonError(IONCAMRError.CONTEXT_ERROR)
                    return
                }

                val settings = ionGallerySettings ?: run {
                    sendIonError(IONCAMRError.INVALID_ARGUMENT_ERROR)
                    return
                }

                val uris = manager.extractUris(result.data)

                if (uris.isEmpty()) {
                    sendIonError(IONCAMRError.GENERIC_CHOOSE_MULTIMEDIA_ERROR)
                    return
                }

                if (settings.allowEdit && uris.size == 1 && settings.mediaType == IONCAMRMediaType.PICTURE) {
                    val originalUri = uris.first()
                    if (settings.editInApp) {
                        editor.openCropActivity(
                            activity,
                            originalUri,
                            galleryCropLauncher
                        )
                    } else {
                        val tempUri = if (originalUri.scheme == "content") {
                            IonCameraUtils.getGalleryTempImage(activity, originalUri)
                        } else {
                            originalUri
                        }

                        if (tempUri == null) {
                            sendIonError(IONCAMRError.EDIT_IMAGE_ERROR)
                            return
                        }

                        val editIntent = createIonEditIntent(tempUri)
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
                } else {
                    processResultFromGallery(result)
                }
            }
            Activity.RESULT_CANCELED -> {
                sendIonError(IONCAMRError.CHOOSE_MULTIMEDIA_CANCELLED_ERROR)
            }
            else -> sendIonError(IONCAMRError.GENERIC_CHOOSE_MULTIMEDIA_ERROR)
        }
    }

    private fun handleGalleryCropResult(result: ActivityResult) {
        val settings = ionGallerySettings ?: run {
            sendIonError(IONCAMRError.INVALID_ARGUMENT_ERROR)
            return
        }

        when (result.resultCode) {
            Activity.RESULT_OK -> {
                var intent = result.data
                val resultPath = intent?.getStringExtra(IONCAMRImageEditorActivity.IMAGE_OUTPUT_URI_EXTRAS)

                if (resultPath.isNullOrEmpty()) {
                    if (ionLastEditUri.isNullOrEmpty()) {
                        sendIonError(IONCAMRError.EDIT_IMAGE_ERROR)
                        return
                    }
                    intent = Intent().apply {
                        putExtra(IONCAMRImageEditorActivity.IMAGE_OUTPUT_URI_EXTRAS, ionLastEditUri)
                    }
                }
                processResultEditFromGallery(intent)
                ionLastEditUri = null
            }
            Activity.RESULT_CANCELED -> {
                if (!settings.editInApp && !ionLastEditUri.isNullOrEmpty()) {
                    val intent = Intent().apply {
                        putExtra(IONCAMRImageEditorActivity.IMAGE_OUTPUT_URI_EXTRAS, ionLastEditUri)
                    }
                    processResultEditFromGallery(intent)
                } else {
                    ionLastEditUri = null
                    sendIonError(IONCAMRError.EDIT_OPERATION_CANCELLED_ERROR)
                }
            }
            else -> sendIonError(IONCAMRError.EDIT_IMAGE_ERROR)
        }
    }

    private fun handleCameraCropResult(result: ActivityResult) {
        val settings = ionCameraSettings ?: run {
            sendIonError(IONCAMRError.INVALID_ARGUMENT_ERROR)
            return
        }

        when (result.resultCode) {
            Activity.RESULT_OK -> {
                var intent = result.data
                val resultPath = intent?.getStringExtra(IONCAMRImageEditorActivity.IMAGE_OUTPUT_URI_EXTRAS)

                if (resultPath.isNullOrEmpty()) {
                    if (ionLastEditUri.isNullOrEmpty()) {
                        sendIonError(IONCAMRError.EDIT_IMAGE_ERROR)
                        return
                    }
                    intent = Intent().apply {
                        putExtra(IONCAMRImageEditorActivity.IMAGE_OUTPUT_URI_EXTRAS, ionLastEditUri)
                    }
                }

                processIonResult(intent)
                ionLastEditUri = null
            }
            Activity.RESULT_CANCELED -> {
                if (!settings.editInApp && !ionLastEditUri.isNullOrEmpty()) {
                    val intent = Intent().apply {
                        putExtra(IONCAMRImageEditorActivity.IMAGE_OUTPUT_URI_EXTRAS, ionLastEditUri)
                    }
                    processIonResult(intent)
                } else {
                    ionLastEditUri = null
                    sendIonError(IONCAMRError.EDIT_OPERATION_CANCELLED_ERROR)
                }
            }
            else -> sendIonError(IONCAMRError.EDIT_IMAGE_ERROR)
        }
    }

    private fun handleEditResult(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> processResultFromEdit(result)
            Activity.RESULT_CANCELED -> sendIonError(IONCAMRError.EDIT_OPERATION_CANCELLED_ERROR)
            else -> sendIonError(IONCAMRError.EDIT_IMAGE_ERROR)
        }
    }

    // ============================================
    // ION FLOW - RESULT PROCESSING
    // ============================================

    private fun processIonResult(intent: Intent?) {
        val manager = ionCameraManager ?: run {
            sendIonError(IONCAMRError.CONTEXT_ERROR)
            return
        }

        val settings = ionCameraSettings ?: run {
            sendIonError(IONCAMRError.INVALID_ARGUMENT_ERROR)
            return
        }
        val ionParams = settings.toIonParameters()
        manager.processResultFromCamera(
            activity,
            intent,
            ionParams,
            { image ->
                handlePhotoBase64Result(image)
            },
            { mediaResult ->
                handleMediaResult(mediaResult)
            },
            { error ->
                sendIonError(error)
            }
        )
    }

    private fun processResultFromVideo(result: ActivityResult) {
        val manager = ionCameraManager ?: run {
            sendIonError(IONCAMRError.CONTEXT_ERROR)
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
            sendIonError(IONCAMRError.CAPTURE_VIDEO_ERROR)
            return
        }
        val settings = ionVideoSettings ?: run {
            sendIonError(IONCAMRError.INVALID_ARGUMENT_ERROR)
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
                    sendIonError(IONCAMRError.CAPTURE_VIDEO_ERROR)
                })
        }
    }

    private fun processResultFromGallery(result: ActivityResult) {
        val manager = ionGalleryManager ?: run {
            sendIonError(IONCAMRError.CONTEXT_ERROR)
            return
        }

        val settings = ionGallerySettings ?: run {
            sendIonError(IONCAMRError.INVALID_ARGUMENT_ERROR)
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
                { sendIonError(it) })
        }
    }

    private fun processResultEditFromGallery(intent: Intent) {
        val manager = ionGalleryManager ?: run {
            sendIonError(IONCAMRError.CONTEXT_ERROR)
            return
        }

        val settings = ionGallerySettings ?: run {
            sendIonError(IONCAMRError.INVALID_ARGUMENT_ERROR)
            return
        }

        CoroutineScope(Dispatchers.Default).launch {
            manager.onChooseFromGalleryEditResult(
                activity,
                Activity.RESULT_OK,
                intent,
                settings.includeMetadata,
                { handleGalleryMediaResults(it) },
                { sendIonError(it) }
            )
        }
    }

    private fun processResultFromEdit(result: ActivityResult) {
        val manager = ionEditManager ?: run {
            sendIonError(IONCAMRError.CONTEXT_ERROR)
            return
        }

        manager.processResultFromEdit(
            activity,
            result.data,
            ionEditParameters,
            { image ->
                handleEditBase64Result(image)
            },
            { mediaResult ->
                handleMediaResult(mediaResult)
            },
            { error ->
                sendIonError(error)
            }
        )
    }

    // ============================================
    // ION FLOW - RESPONSE BUILDERS
    // ============================================

    private fun handlePhotoBase64Result(image: String) {
        val ret = JSObject()
        ret.put("format", "jpeg")

        val settings = ionCameraSettings ?: run {
            sendIonError(IONCAMRError.INVALID_ARGUMENT_ERROR)
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
                sendIonError(IONCAMRError.PROCESS_IMAGE_ERROR)
                return
            }
        }

        ionCurrentCall?.resolve(ret)
        ionCurrentCall = null
    }

    private fun handleEditBase64Result(image: String) {
        val ret = JSObject()
        ret.put("format", "jpeg")
        ret.put("base64String", image)
        ionCurrentCall?.resolve(ret)
        ionCurrentCall = null
    }

    private fun handleMediaResult(mediaResult: IONCAMRMediaResult) {
        val file = File(mediaResult.uri)
        val uri = Uri.fromFile(file)
        val bitmap = BitmapFactory.decodeFile(mediaResult.uri)
        if (bitmap == null) {
            sendIonError(IONCAMRError.PROCESS_IMAGE_ERROR)
            return
        }

        val exif = ImageUtils.getExifData(context, bitmap, uri)
        val ret = JSObject()
        ret.put("format", "jpeg")
        ret.put("exif", exif.toJson())
        ret.put("path", mediaResult.uri)
        ret.put("webPath", FileUtils.getPortablePath(context, bridge.localUrl, uri))
        ret.put("saved", mediaResult.saved)
        ionCurrentCall?.resolve(ret)
        ionCurrentCall = null
        ionLastEditUri = null
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
        ionCurrentCall?.resolve(ret)
        ionCurrentCall = null
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
        ret.put("results", photos)
        ionCurrentCall?.resolve(ret)
        ionCurrentCall = null
        ionLastEditUri = null
    }

    // ============================================
    // LEGACY FLOW - SETTINGS & UTILITIES
    // ============================================

    private fun getLegacySettings(call: PluginCall): LegacyCameraSettings {
        val settings = LegacyCameraSettings()
        settings.resultType = getLegacyResultType(call.getString("resultType"))
        settings.saveToGallery = call.getBoolean("saveToGallery", LegacyCameraSettings.DEFAULT_SAVE_IMAGE_TO_GALLERY)!!
        settings.allowEditing = call.getBoolean("allowEditing", false)!!
        settings.quality = call.getInt("quality", LegacyCameraSettings.DEFAULT_QUALITY)!!
        settings.width = call.getInt("width", 0)!!
        settings.height = call.getInt("height", 0)!!
        settings.shouldResize = settings.width > 0 || settings.height > 0
        settings.shouldCorrectOrientation = call.getBoolean("correctOrientation", LegacyCameraSettings.DEFAULT_CORRECT_ORIENTATION)!!
        try {
            settings.source = CameraSource.valueOf(call.getString("source", CameraSource.PROMPT.getSource())!!)
        } catch (ex: IllegalArgumentException) {
            settings.source = CameraSource.PROMPT
        }
        return settings
    }

    private fun getLegacyResultType(resultType: String?): CameraResultType? {
        if (resultType == null) {
            return null
        }
        try {
            return CameraResultType.valueOf(resultType.uppercase(Locale.ROOT))
        } catch (ex: IllegalArgumentException) {
            Logger.debug(logTag, "Invalid result type \"$resultType\", defaulting to base64")
            return CameraResultType.BASE64
        }
    }

    // ============================================
    // LEGACY FLOW - PERMISSION HANDLING
    // ============================================

    private fun checkLegacyCameraPermissions(call: PluginCall): Boolean {
        val needCameraPerms = isPermissionDeclared(CAMERA)
        val hasCameraPerms = !needCameraPerms || getPermissionState(CAMERA) == PermissionState.GRANTED
        val hasGalleryPerms = getPermissionState(SAVE_GALLERY) == PermissionState.GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!hasCameraPerms) {
                requestLegacyPermissionForAlias(CAMERA, call, "cameraPermissionsCallback")
                return false
            }
            return true
        }

        if (legacySettings.saveToGallery && !(hasCameraPerms && hasGalleryPerms) && legacyIsFirstRequest) {
            legacyIsFirstRequest = false
            val aliases: Array<String> = if (needCameraPerms) {
                arrayOf(CAMERA, SAVE_GALLERY)
            } else {
                arrayOf(SAVE_GALLERY)
            }
            requestLegacyPermissionForAliases(aliases, call, "cameraPermissionsCallback")
            return false
        } else if (!hasCameraPerms) {
            requestLegacyPermissionForAlias(CAMERA, call, "cameraPermissionsCallback")
            return false
        }
        return true
    }

    private fun handleLegacyCameraPermissionsCallback(call: PluginCall) {
        if (call.methodName.equals("pickImages")) {
            openLegacyPhotos(call, true)
        } else {
            if (legacySettings.source == CameraSource.CAMERA && getPermissionState(CAMERA) != PermissionState.GRANTED) {
                Logger.debug(logTag, "User denied camera permission: ${getPermissionState(CAMERA)}")
                call.reject("User denied access to camera")
                return
            }
            doShow(call)
        }
    }

    // ============================================
    // LEGACY FLOW - CAMERA OPERATIONS
    // ============================================

    private fun doShow(call: PluginCall) {
        when (legacySettings.source) {
            CameraSource.CAMERA -> showLegacyCamera(call)
            CameraSource.PHOTOS -> showLegacyPhotos(call)
            else -> showPrompt(call)
        }
    }

    private fun showPrompt(call: PluginCall) {
        val options = ArrayList<String>()
        options.add(call.getString("promptLabelPhoto", "From Photos")!!)
        options.add(call.getString("promptLabelPicture", "Take Picture")!!)

        val fragment = CameraBottomSheetDialogFragment()
        fragment.setTitle(call.getString("promptLabelHeader", "Photo"))
        fragment.setOptions(
            options,
            { index ->
                if (index == 0) {
                    legacySettings.source = CameraSource.PHOTOS
                    openLegacyPhotos(call)
                } else if (index == 1) {
                    legacySettings.source = CameraSource.CAMERA
                    openLegacyCamera(call)
                }
            },
            { call.reject("User cancelled photos app") }
        )
        fragment.show(activity.supportFragmentManager, "capacitorModalsActionSheet")
    }

    private fun showLegacyCamera(call: PluginCall) {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            call.reject("Device doesn't have a camera available")
            return
        }
        openLegacyCamera(call)
    }

    private fun showLegacyPhotos(call: PluginCall) {
        openLegacyPhotos(call)
    }

    private fun openLegacyCamera(call: PluginCall) {
        if (checkLegacyCameraPermissions(call)) {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(context.packageManager) != null) {
                try {
                    val appId = getAppId()
                    val photoFile = CameraUtils.createImageFile(activity)
                    legacyImageFileSavePath = photoFile.absolutePath
                    legacyImageFileUri = FileProvider.getUriForFile(activity, "$appId.fileprovider", photoFile)
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, legacyImageFileUri)
                } catch (ex: Exception) {
                    call.reject("Unable to create photo on disk", ex)
                    return
                }

                startActivityForResult(call, takePictureIntent, "processCameraImage")
            } else {
                call.reject("Unable to resolve camera activity")
            }
        }
    }

    private fun openLegacyPhotos(call: PluginCall, multiple: Boolean = false) {
        try {
            if (multiple) {
                pickMultipleMedia = registerActivityResultLauncher(getContractForCall(call)) { uris ->
                    if (uris.isNotEmpty()) {
                        val executor = Executors.newSingleThreadExecutor()
                        executor.execute {
                            val ret = JSObject()
                            val photos = JSArray()
                            for (imageUri in uris) {
                                try {
                                    val processResult = processPickedImages(imageUri)
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
                        call.reject("User cancelled photos app")
                    }
                    pickMultipleMedia?.unregister()
                }
                pickMultipleMedia?.launch(
                    PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly).build()
                )
            } else {
                pickMedia = registerActivityResultLauncher(ActivityResultContracts.PickVisualMedia()) { uri ->
                    if (uri != null) {
                        legacyImagePickedContentUri = uri
                        processPickedImage(uri, call)
                    } else {
                        call.reject("User cancelled photos app")
                    }
                    pickMedia?.unregister()
                }
                pickMedia?.launch(
                    PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly).build()
                )
            }
        } catch (ex: ActivityNotFoundException) {
            call.reject("Unable to resolve photo activity")
        }
    }

    // ============================================
    // LEGACY FLOW - RESULT PROCESSING
    // ============================================

    private fun processPickedImage(call: PluginCall, result: ActivityResult) {
        legacySettings = getLegacySettings(call)
        val data = result.data
        if (data == null) {
            call.reject("User cancelled photos app")
            return
        }

        val u = data.data
        legacyImagePickedContentUri = u

        if (u != null) {
            processPickedImage(u, call)
        }
    }

    private fun processPickedImage(imageUri: Uri, call: PluginCall) {
        var imageStream: InputStream? = null

        try {
            imageStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(imageStream)

            if (bitmap == null) {
                call.reject("Unable to process bitmap")
                return
            }

            returnLegacyResult(call, bitmap, imageUri)
        } catch (err: OutOfMemoryError) {
            call.reject("Out of memory")
        } catch (ex: FileNotFoundException) {
            call.reject("No such image found", ex)
        } finally {
            if (imageStream != null) {
                try {
                    imageStream.close()
                } catch (e: IOException) {
                    Logger.error(logTag, "Unable to process image", e)
                }
            }
        }
    }

    private fun processPickedImages(imageUri: Uri): JSObject {
        var imageStream: InputStream? = null
        val ret = JSObject()
        try {
            imageStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(imageStream)

            if (bitmap == null) {
                ret.put("error", "Unable to process bitmap")
                return ret
            }

            val exif = ImageUtils.getExifData(context, bitmap, imageUri)
            val processedBitmap = try {
                prepareLegacyBitmap(bitmap, imageUri, exif)
            } catch (e: IOException) {
                ret.put("error", "Unable to process image")
                return ret
            }

            val bitmapOutputStream = ByteArrayOutputStream()
            processedBitmap.compress(Bitmap.CompressFormat.JPEG, legacySettings.quality, bitmapOutputStream)

            val newUri = getLegacyTempImage(imageUri, bitmapOutputStream)
            exif.copyExif(newUri?.path)
            if (newUri != null) {
                ret.put("format", "jpeg")
                ret.put("exif", exif.toJson())
                ret.put("path", newUri.toString())
                ret.put("webPath", FileUtils.getPortablePath(context, bridge.localUrl, newUri))
            } else {
                ret.put("error", "Unable to process image")
            }
            return ret
        } catch (err: OutOfMemoryError) {
            ret.put("error", "Out of memory")
        } catch (ex: FileNotFoundException) {
            ret.put("error", "No such image found")
            Logger.error(logTag, "No such image found", ex)
        } finally {
            if (imageStream != null) {
                try {
                    imageStream.close()
                } catch (e: IOException) {
                    Logger.error(logTag, "Unable to process image", e)
                }
            }
        }
        return ret
    }

    private fun returnLegacyResult(call: PluginCall, bitmap: Bitmap, u: Uri) {
        val exif = ImageUtils.getExifData(context, bitmap, u)
        val processedBitmap = try {
            prepareLegacyBitmap(bitmap, u, exif)
        } catch (e: IOException) {
            call.reject("Unable to process image")
            return
        }

        val bitmapOutputStream = ByteArrayOutputStream()
        processedBitmap.compress(Bitmap.CompressFormat.JPEG, legacySettings.quality, bitmapOutputStream)

        if (legacySettings.allowEditing && !legacyIsEdited) {
            editLegacyImage(call, u, bitmapOutputStream)
            return
        }

        val saveToGallery = call.getBoolean("saveToGallery", LegacyCameraSettings.DEFAULT_SAVE_IMAGE_TO_GALLERY)
        if (saveToGallery!! && (legacyImageEditedFileSavePath != null || legacyImageFileSavePath != null)) {
            legacyIsSaved = true
            try {
                val fileToSavePath = legacyImageEditedFileSavePath ?: legacyImageFileSavePath
                val fileToSave = File(fileToSavePath!!)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
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

                    val inserted = processedBitmap.compress(Bitmap.CompressFormat.JPEG, legacySettings.quality, stream)

                    if (!inserted) {
                        legacyIsSaved = false
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val inserted = MediaStore.Images.Media.insertImage(
                        context.contentResolver,
                        fileToSavePath,
                        fileToSave.name,
                        ""
                    )

                    if (inserted == null) {
                        legacyIsSaved = false
                    }
                }
            } catch (e: FileNotFoundException) {
                legacyIsSaved = false
                Logger.error(logTag, "Unable to save the image in the gallery", e)
            } catch (e: IOException) {
                legacyIsSaved = false
                Logger.error(logTag, "Unable to save the image in the gallery", e)
            }
        }

        when (legacySettings.resultType) {
            CameraResultType.BASE64 -> returnLegacyBase64(call, exif, bitmapOutputStream)
            CameraResultType.URI -> returnLegacyFileURI(call, exif, processedBitmap, u, bitmapOutputStream)
            CameraResultType.DATAURL -> returnLegacyDataUrl(call, exif, bitmapOutputStream)
            else -> call.reject("Invalid resultType option")
        }

        if (legacySettings.resultType != CameraResultType.URI) {
            deleteLegacyImageFile()
        }
        legacyImageFileSavePath = null
        legacyImageFileUri = null
        legacyImagePickedContentUri = null
        legacyImageEditedFileSavePath = null
    }

    private fun returnLegacyFileURI(call: PluginCall, exif: ExifWrapper, bitmap: Bitmap, u: Uri, bitmapOutputStream: ByteArrayOutputStream) {
        val newUri = getLegacyTempImage(u, bitmapOutputStream)
        exif.copyExif(newUri?.path)
        if (newUri != null) {
            val ret = JSObject()
            ret.put("format", "jpeg")
            ret.put("exif", exif.toJson())
            ret.put("path", newUri.toString())
            ret.put("webPath", FileUtils.getPortablePath(context, bridge.localUrl, newUri))
            ret.put("saved", legacyIsSaved)
            call.resolve(ret)
        } else {
            call.reject("Unable to process image")
        }
    }

    private fun returnLegacyBase64(call: PluginCall, exif: ExifWrapper, bitmapOutputStream: ByteArrayOutputStream) {
        val byteArray = bitmapOutputStream.toByteArray()
        val encoded = Base64.encodeToString(byteArray, Base64.NO_WRAP)

        val data = JSObject()
        data.put("format", "jpeg")
        data.put("base64String", encoded)
        data.put("exif", exif.toJson())
        call.resolve(data)
    }

    private fun returnLegacyDataUrl(call: PluginCall, exif: ExifWrapper, bitmapOutputStream: ByteArrayOutputStream) {
        val byteArray = bitmapOutputStream.toByteArray()
        val encoded = Base64.encodeToString(byteArray, Base64.NO_WRAP)

        val data = JSObject()
        data.put("format", "jpeg")
        data.put("dataUrl", "data:image/jpeg;base64,$encoded")
        data.put("exif", exif.toJson())
        call.resolve(data)
    }

    // ============================================
    // LEGACY FLOW - IMAGE PROCESSING
    // ============================================

    private fun prepareLegacyBitmap(bitmap: Bitmap, imageUri: Uri, exif: ExifWrapper): Bitmap {
        var processedBitmap = bitmap
        if (legacySettings.shouldCorrectOrientation) {
            val newBitmap = ImageUtils.correctOrientation(context, processedBitmap, imageUri, exif)
            processedBitmap = replaceBitmap(processedBitmap, newBitmap)
        }

        if (legacySettings.shouldResize) {
            val newBitmap = ImageUtils.resize(processedBitmap, legacySettings.width, legacySettings.height)
            processedBitmap = replaceBitmap(processedBitmap, newBitmap)
        }

        return processedBitmap
    }

    private fun replaceBitmap(bitmap: Bitmap, newBitmap: Bitmap): Bitmap {
        if (bitmap != newBitmap) {
            bitmap.recycle()
        }
        return newBitmap
    }

    private fun editLegacyImage(call: PluginCall, uri: Uri, bitmapOutputStream: ByteArrayOutputStream) {
        try {
            val tempImage = getLegacyTempImage(uri, bitmapOutputStream)
            val editIntent = createLegacyEditIntent(tempImage)
            if (editIntent != null) {
                startActivityForResult(call, editIntent, "processEditedImage")
            } else {
                call.reject("Unable to edit image")
            }
        } catch (ex: Exception) {
            call.reject("Unable to edit image", ex)
        }
    }

    private fun createLegacyEditIntent(origPhotoUri: Uri?): Intent? {
        if (origPhotoUri == null) return null
        try {
            val editFile = File(origPhotoUri.path!!)
            val editUri = FileProvider.getUriForFile(
                activity,
                context.packageName + ".fileprovider",
                editFile
            )
            val editIntent = Intent(Intent.ACTION_EDIT)
            editIntent.setDataAndType(editUri, "image/*")
            legacyImageEditedFileSavePath = editFile.absolutePath
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            editIntent.addFlags(flags)
            editIntent.putExtra(MediaStore.EXTRA_OUTPUT, editUri)

            val resInfoList: MutableList<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context
                    .packageManager
                    .queryIntentActivities(editIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
            } else {
                legacyQueryIntentActivities(editIntent)
            }

            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(packageName, editUri, flags)
            }
            return editIntent
        } catch (ex: Exception) {
            return null
        }
    }

    // ============================================
    // LEGACY FLOW - FILE OPERATIONS
    // ============================================

    private fun saveImageToFile(uri: Uri, inputStream: InputStream): Uri {
        val outFile = if (uri.scheme == "content") {
            getLegacyTempFile(uri)
        } else {
            File(uri.path!!)
        }
        try {
            writePhoto(outFile, inputStream)
        } catch (ex: FileNotFoundException) {
            val tempFile = getLegacyTempFile(uri)
            writePhoto(tempFile, inputStream)
            return Uri.fromFile(tempFile)
        }
        return Uri.fromFile(outFile)
    }

    private fun writePhoto(outFile: File, inputStream: InputStream) {
        val fos = FileOutputStream(outFile)
        val buffer = ByteArray(1024)
        var len: Int
        while (inputStream.read(buffer).also { len = it } != -1) {
            fos.write(buffer, 0, len)
        }
        fos.close()
    }

    private fun getLegacyTempFile(uri: Uri): File {
        var filename = Uri.parse(Uri.decode(uri.toString())).lastPathSegment ?: ""
        if (!filename.contains(".jpg") && !filename.contains(".jpeg")) {
            filename += ".${java.util.Date().time}.jpeg"
        }
        val cacheDir = context.cacheDir
        return File(cacheDir, filename)
    }

    private fun getLegacyTempImage(u: Uri, bitmapOutputStream: ByteArrayOutputStream): Uri? {
        var bis: ByteArrayInputStream? = null
        var newUri: Uri? = null
        try {
            bis = ByteArrayInputStream(bitmapOutputStream.toByteArray())
            newUri = saveImageToFile(u, bis)
        } catch (ex: IOException) {
            // Return null
        } finally {
            if (bis != null) {
                try {
                    bis.close()
                } catch (e: IOException) {
                    Logger.error(logTag, "Unable to process image", e)
                }
            }
        }
        return newUri
    }

    private fun deleteLegacyImageFile() {
        if (legacyImageFileSavePath != null && !legacySettings.saveToGallery) {
            val photoFile = File(legacyImageFileSavePath!!)
            if (photoFile.exists()) {
                photoFile.delete()
            }
        }
    }

    @Suppress("deprecation")
    private fun getLegacyParcelableArrayList(bundle: Bundle, key: String): ArrayList<Parcelable> {
        return bundle.getParcelableArrayList(key)!!
    }

    // ============================================
    // PUBLIC PLUGIN METHODS
    // ============================================

    @PluginMethod
    fun getPhoto(call: PluginCall) {
        legacyIsEdited = false
        legacySettings = getLegacySettings(call)
        doShow(call)
    }

    @PluginMethod
    fun takePhoto(call: PluginCall) {
        ionCameraSettings = getIonCameraSettings(call)
        ionCurrentCall = call
        showIonCamera(call)
    }

    @PluginMethod
    fun recordVideo(call: PluginCall) {
        ionVideoSettings = getIonVideoSettings(call)
        ionCurrentCall = call
        openRecordVideo(call)
    }

    @PluginMethod
    fun playVideo(call: PluginCall) {
        ionCurrentCall = call
        openPlayVideo(call)
    }

    @PluginMethod
    fun chooseFromGallery(call: PluginCall) {
        ionGallerySettings = getIonGallerySettings(call)
        ionCurrentCall = call
        openIonGallery(call)
    }

    @PluginMethod
    fun editPhoto(call: PluginCall) {
        ionCurrentCall = call
        callEditPhoto(call)
    }

    @PluginMethod
    fun editURIPhoto(call: PluginCall) {
        ionCurrentCall = call
        callEditURIPhoto(call)
    }

    @PluginMethod
    fun pickImages(call: PluginCall) {
        legacySettings = getLegacySettings(call)
        openLegacyPhotos(call, true)
    }

    @PluginMethod
    fun pickLimitedLibraryPhotos(call: PluginCall) {
        call.unimplemented("not supported on android")
    }

    @PluginMethod
    fun getLimitedLibraryPhotos(call: PluginCall) {
        call.unimplemented("not supported on android")
    }

    /**
     * Completes the plugin call after a camera permission request
     *
     * @see .getPhoto
     * @param call the plugin call
     */
    @PermissionCallback
    private fun cameraPermissionsCallback(call: PluginCall) {
        handleLegacyCameraPermissionsCallback(call)
    }

    /**
     * Completes the plugin call after a camera permission request
     *
     * @see .takePhoto
     * @param call the plugin call
     */
    @PermissionCallback
    private fun ionCameraPermissionsCallback(call: PluginCall) {
        handleIonCameraPermissionsCallback(call)
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

    @ActivityCallback
    fun processCameraImage(call: PluginCall, result: ActivityResult) {
        legacySettings = getLegacySettings(call)
        if (legacyImageFileSavePath == null) {
            call.reject("Unable to process image, file not found on disk")
            return
        }

        val f = File(legacyImageFileSavePath!!)
        val bmOptions = BitmapFactory.Options()
        val contentUri = Uri.fromFile(f)
        val bitmap = BitmapFactory.decodeFile(legacyImageFileSavePath, bmOptions)

        if (bitmap == null) {
            call.reject("User cancelled photos app")
            return
        }

        returnLegacyResult(call, bitmap, contentUri)
    }

    @ActivityCallback
    private fun processEditedImage(call: PluginCall, result: ActivityResult) {
        legacyIsEdited = true
        legacySettings = getLegacySettings(call)
        if (result.resultCode == Activity.RESULT_CANCELED) {
            if (legacyImagePickedContentUri != null) {
                processPickedImage(legacyImagePickedContentUri!!, call)
            } else {
                processCameraImage(call, result)
            }
        } else {
            processPickedImage(call, result)
        }
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

    private fun getResultType(resultType: String?): CameraResultType? {
        if (resultType == null) {
            return null
        }
        try {
            return CameraResultType.valueOf(resultType.uppercase())
        } catch (ex: java.lang.IllegalArgumentException) {
            Logger.debug(
                getLogTag(),
                "Invalid result type \"" + resultType + "\", defaulting to base64"
            )
            return CameraResultType.BASE64
        }
    }

    @Suppress("deprecation")
    private fun legacyQueryIntentActivities(intent: Intent): MutableList<ResolveInfo> {
        return getContext().getPackageManager()
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }

    protected override fun saveInstanceState(): Bundle? {
        val bundle = super.saveInstanceState()
        if (bundle != null) {
            bundle.putString("cameraImageFileSavePath", legacyImageFileSavePath)
        }
        return bundle
    }

    protected override fun restoreState(state: Bundle) {
        super.restoreState(state)
        val storedImageFileSavePath = state.getString("cameraImageFileSavePath")
        if (storedImageFileSavePath != null) {
            legacyImageFileSavePath = storedImageFileSavePath
        }
    }

    /**
     * Unregister activity result launches to prevent leaks.
     */
    protected override fun handleOnDestroy() {
        // Unregister legacy launchers
        pickMedia?.unregister()
        pickMultipleMedia?.unregister()

        // Clean up Ion managers
        ionCameraManager?.deleteVideoFilesFromCache(activity)
    }

    fun requestLegacyPermissionForAlias(alias: String, call: PluginCall, callbackName: String) {
        requestPermissionForAlias(alias, call, callbackName)
    }

    fun requestLegacyPermissionForAliases(
        aliases: Array<String>,
        call: PluginCall,
        callbackName: String
    ) {
        requestPermissionForAliases(aliases, call, callbackName)
    }

}
