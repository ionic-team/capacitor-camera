package com.capacitorjs.plugins.camera

enum class IonEditableMode(val value: String) {
    NO("no"),
    IN_APP("in-app"),
    EXTERNAL("external");

    companion object {
        fun fromString(value: String?): IonEditableMode {
            if (value == null) return NO
            return values().find {
                it.value.equals(value, ignoreCase = true)
            } ?: NO
        }
    }
}
