#!/usr/bin/env bash

publish_plugin_ios () {
    PLUGIN_PATH=$1
    # Only try to publish if the directory contains a package.json podspec file
    if ! test -f "$PLUGIN_PATH/package.json"; then
        printf %"s\n\n" "$PLUGIN_PATH does not appear to be a plugin (has no package.json), skipping..."
        return
    fi

    PLUGIN_VERSION=$(grep '"version": ' "$PLUGIN_PATH"/package.json | awk '{print $2}' | tr -d '",')
    PLUGIN_NAME=$(grep '"name": ' "$PLUGIN_PATH"/package.json | awk '{print $2}' | tr -d '",')
    PLUGIN_NAME=${PLUGIN_NAME#@capacitor/}
    # capitalize the name, because .podspec file name is capitalized
    first_char=$(printf '%s' "$PLUGIN_NAME" | cut -c1 | tr '[:lower:]' '[:upper:]')
    rest=$(printf '%s' "$PLUGIN_NAME" | cut -c2-)
    PLUGIN_NAME="${first_char}${rest}"
    POD_NAME="Capacitor$PLUGIN_NAME"
    PODSPEC_FILE_PATH="$PLUGIN_PATH/$POD_NAME.podspec"
    if ! test -f $PODSPEC_FILE_PATH; then
        printf %"s\n\n" "Was looking for podspec file $PODSPEC_FILE_PATH, but does not seem to exist, skipping..."
        return
    fi

    # check if version already exists in Trunk
    if pod trunk info "$POD_NAME" 2>/dev/null | grep -q " - $PLUGIN_VERSION ("; then
        printf %"s\n\n" "Duplicate: a published plugin $PLUGIN_NAME exists for version $PLUGIN_VERSION, skipping..."
        return
    fi

    LOG_OUTPUT=./tmp/$PLUGIN_NAME.txt
    # Make log dir if doesnt exist
    mkdir -p ./tmp
    # publish to Trunk
    printf %"s\n" "Attempting to build and publish plugin $PLUGIN_NAME for version $PLUGIN_VERSION to production..."
    pod trunk push $PODSPEC_FILE_PATH --allow-warnings > $LOG_OUTPUT 2>&1
    if grep -q "passed" $LOG_OUTPUT; then
        printf %"s\n\n" "Success: $PLUGIN_NAME published to CocoaPods Trunk."
    else
        printf %"s\n\n" "Error publishing $PLUGIN_NAME, check $LOG_OUTPUT for more info!"
        cat $LOG_OUTPUT
        exit 1
    fi
}

publish_plugin_ios '.'
