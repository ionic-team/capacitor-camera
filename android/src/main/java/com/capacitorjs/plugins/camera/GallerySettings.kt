package com.capacitorjs.plugins.camera

import io.ionic.libs.ioncameralib.model.IONCAMRMediaType

class GallerySettings {
    var mediaType: IONCAMRMediaType = IONCAMRMediaType.ALL
    var allowMultipleSelection: Boolean = false
    var limit : Int = 0
    var includeMetadata: Boolean = false
    var allowEdit: Boolean = false
    var editInApp: Boolean = true
}