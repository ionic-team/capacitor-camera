package com.capacitorjs.plugins.camera

class IonCameraSettings {
    var quality: Int = DEFAULT_QUALITY
    var targetWidth: Int = 0
    var targetHeight: Int = 0
    var correctOrientation: Boolean = DEFAULT_CORRECT_ORIENTATION
    var encodingType: Int = 0 //JPEG
    var saveToGallery: Boolean = DEFAULT_SAVE_IMAGE_TO_GALLERY
    var editable: IonEditableMode = IonEditableMode.NO
    var includeMetadata: Boolean = false
    var shouldResize: Boolean = false

    companion object {
        const val DEFAULT_QUALITY: Int = 90
        const val DEFAULT_SAVE_IMAGE_TO_GALLERY: Boolean = false
        const val DEFAULT_CORRECT_ORIENTATION: Boolean = true
        const val DEFAULT_ENCODING_TYPE: Int = 0 //JPEG
    }
}
