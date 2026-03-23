package com.capacitorjs.plugins.camera

import com.getcapacitor.PermissionState
import com.getcapacitor.PluginCall

/**
 * Helper class for wrapping permission-related functionality.
 */
class PermissionHelper(
    private val isPermissionDeclaredFn: (String) -> Boolean,
    private val getPermissionStateFn: (String) -> PermissionState,
    private val requestPermissionForAliasFn: (String, PluginCall, String) -> Unit,
    private val requestPermissionForAliasesFn: (Array<String>, PluginCall, String) -> Unit
) {

    fun isPermissionDeclared(alias: String): Boolean {
        return isPermissionDeclaredFn(alias)
    }

    fun getPermissionState(alias: String): PermissionState {
        return getPermissionStateFn(alias)
    }

    fun requestPermissionForAlias(
        alias: String,
        call: PluginCall,
        callbackName: String
    ) {
        requestPermissionForAliasFn(alias, call, callbackName)
    }

    fun requestPermissionForAliases(
        aliases: Array<String>,
        call: PluginCall,
        callbackName: String
    ) {
        requestPermissionForAliasesFn(aliases, call, callbackName)
    }
}
