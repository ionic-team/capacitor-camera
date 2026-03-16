package com.capacitorjs.plugins.camera

import io.ionic.libs.ioncameralib.model.IONCAMRMediaType

data class GallerySettings(
    val mediaType: IONCAMRMediaType = IONCAMRMediaType.ALL,
    val allowMultipleSelection: Boolean = false,
    val limit: Int = 0,
    val includeMetadata: Boolean = false,
    val allowEdit: Boolean = false,
    val editInApp: Boolean = true
)