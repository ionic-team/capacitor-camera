package com.capacitorjs.plugins.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.result.ActivityResult
import com.getcapacitor.Logger
import com.getcapacitor.PermissionState
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.ActivityCallback
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback
import org.json.JSONException
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
        private const val CAMERA = "camera"
        private const val PHOTOS = "photos"
        private const val SAVE_GALLERY = "saveGallery"
        private const val READ_EXTERNAL_STORAGE = "readExternalStorage"
    }

    private lateinit var legacyFlow: LegacyCameraFlow
    private lateinit var ionFlow: IonCameraFlow

    override fun load() {
        super.load()

        val permissionHelper = PermissionHelper(
            isPermissionDeclaredFn = { alias -> isPermissionDeclared(alias) },
            getPermissionStateFn = { alias -> getPermissionState(alias) },
            requestPermissionForAliasFn = { alias, call, callbackName ->
                requestPermissionForAlias(alias, call, callbackName)
            },
            requestPermissionForAliasesFn = { aliases, call, callbackName ->
                requestPermissionForAliases(aliases, call, callbackName)
            }
        )

        legacyFlow = LegacyCameraFlow(
            context,
            activity,
            bridge,
            appId,
            permissionHelper,
            LegacyCameraFlow.ActivityStarter { call, intent, callbackName ->
                startActivityForResult(call, intent, callbackName)
            }
        )

        ionFlow = IonCameraFlow(
            context,
            activity,
            bridge,
            appId,
            permissionHelper
        )
        ionFlow.load()
    }

    @Deprecated("Use either takePhoto for CameraSource.Camera or chooseFromGallery for CameraSource.Photos")
    @PluginMethod
    fun getPhoto(call: PluginCall) {
        legacyFlow.getPhoto(call)
    }

    @PluginMethod
    fun takePhoto(call: PluginCall) {
        ionFlow.takePhoto(call)
    }

    @PluginMethod
    fun recordVideo(call: PluginCall) {
        ionFlow.recordVideo(call)
    }

    @PluginMethod
    fun playVideo(call: PluginCall) {
        ionFlow.playVideo(call)
    }

    @PluginMethod
    fun chooseFromGallery(call: PluginCall) {
        ionFlow.chooseFromGallery(call)
    }

    @PluginMethod
    fun editPhoto(call: PluginCall) {
        ionFlow.editPhoto(call)
    }

    @PluginMethod
    fun editURIPhoto(call: PluginCall) {
        ionFlow.editURIPhoto(call)
    }

    @Deprecated("Use chooseFromGallery instead")
    @PluginMethod
    fun pickImages(call: PluginCall) {
        legacyFlow.pickImages(call)
    }

    @PluginMethod
    fun pickLimitedLibraryPhotos(call: PluginCall) {
        legacyFlow.pickLimitedLibraryPhotos(call)
    }

    @PluginMethod
    fun getLimitedLibraryPhotos(call: PluginCall) {
        legacyFlow.getLimitedLibraryPhotos(call)
    }

    /**
     * Completes the plugin call after a camera permission request
     *
     * @see .getPhoto
     * @param call the plugin call
     */
    @PermissionCallback
    private fun cameraPermissionsCallback(call: PluginCall) {
        legacyFlow.handleCameraPermissionsCallback(call)
    }

    /**
     * Completes the plugin call after a camera permission request
     *
     * @see .takePhoto
     * @param call the plugin call
     */
    @PermissionCallback
    private fun ionCameraPermissionsCallback(call: PluginCall) {
        ionFlow.handlePermissionsCallback(call)
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
        legacyFlow.processCameraImage(call, result)
    }

    @ActivityCallback
    private fun processEditedImage(call: PluginCall, result: ActivityResult) {
        legacyFlow.processEditedImage(call, result)
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
        legacyFlow.onSaveInstanceState(bundle)
        return bundle
    }

    protected override fun restoreState(state: Bundle) {
        super.restoreState(state)
        legacyFlow.onRestoreState(state)
    }

    /**
     * Unregister activity result launches to prevent leaks.
     */
    protected override fun handleOnDestroy() {
        legacyFlow.onDestroy()
        ionFlow.onDestroy()
    }

}
