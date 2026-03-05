package com.capacitorjs.plugins.camera

import io.ionic.libs.ioncameralib.model.IONMediaType

class GallerySettings {
    var mediaType: IONMediaType = IONMediaType.ALL
    var allowMultipleSelection: Boolean = false
    var includeMetadata: Boolean = false
    var allowEdit: Boolean = false
}