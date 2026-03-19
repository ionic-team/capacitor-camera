package com.capacitorjs.plugins.camera

data class IonVideoSettings(
    val saveToGallery: Boolean = false,
    val includeMetadata: Boolean = false,
    val isPersistent: Boolean = true
)