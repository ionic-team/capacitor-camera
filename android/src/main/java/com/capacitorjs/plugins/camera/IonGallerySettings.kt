package com.capacitorjs.plugins.camera

import io.ionic.libs.ioncameralib.model.IONCAMRMediaType

class IonGallerySettings {
    var mediaType: IONCAMRMediaType = IONCAMRMediaType.ALL
    var allowMultipleSelection: Boolean = false
    var limit: Int = 0
    var includeMetadata: Boolean = false
    var editable: IonEditableMode = IonEditableMode.NO
    var quality: Int = DEFAULT_QUALITY
    var width: Int = 0
    var height: Int = 0
    var correctOrientation: Boolean = DEFAULT_CORRECT_ORIENTATION

    companion object {
        const val DEFAULT_QUALITY: Int = 90
        const val DEFAULT_CORRECT_ORIENTATION: Boolean = true
    }
}