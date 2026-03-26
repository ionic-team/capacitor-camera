import {
  IonButton,
  IonAccordion,
  IonAccordionGroup,
  IonItem,
  IonLabel,
  IonInput,
  IonSelect,
  IonSelectOption,
  IonToggle,
} from "@ionic/react";
import React from "react";
import {
  Camera,
  ImageOptions,
  CameraSource,
  CameraResultType,
  CameraDirection,
} from "@capacitor/camera";
import { GetPhotoConfig, PhotoResult } from "./types";

interface GetPhotoConfigurableProps {
  buttonLabel?: string;
  defaultSource?: CameraSource;
  defaultWebUseInput?: boolean;
  onPhotoResult: (result: PhotoResult) => void;
}

interface GetPhotoConfigurableState {
  config: GetPhotoConfig;
}

class GetPhotoConfigurable extends React.Component<
  GetPhotoConfigurableProps,
  GetPhotoConfigurableState
> {
  constructor(props: GetPhotoConfigurableProps) {
    super(props);
    this.state = {
      config: {
        quality: 100,
        allowEditing: false,
        resultType: CameraResultType.Uri,
        source: props.defaultSource ?? CameraSource.Prompt,
        saveToGallery: false,
        width: undefined,
        height: undefined,
        correctOrientation: true,
        direction: CameraDirection.Rear,
        presentationStyle: 'fullscreen',
        webUseInput: props.defaultWebUseInput ?? false,
        promptLabelHeader: 'Photo',
        promptLabelCancel: 'Cancel',
        promptLabelPhoto: 'From Photos',
        promptLabelPicture: 'Take Picture',
      },
    };
  }

  updateConfig = (field: keyof GetPhotoConfig, value: any): void => {
    this.setState({
      config: { ...this.state.config, [field]: value },
    });
  };

  executeDefault = async (): Promise<void> => {
    try {
      const options: ImageOptions = {
        resultType: CameraResultType.Uri,
      };
      const photo = await Camera.getPhoto(options);
      this.props.onPhotoResult({
        path: photo.path,
        webPath: photo.webPath,
        base64String: photo.base64String,
        dataUrl: photo.dataUrl,
        exif: photo.exif,
      });
    } catch (e) {
      const error = e as any;
      const errorMessage = error.code ? `[${error.code}] ${error.message}` : error.message;
      alert(`Failed to get picture with error:\n${errorMessage}`);
    }
  };

  executeWithConfig = async (): Promise<void> => {
    try {
      const config = this.state.config;
      const options: ImageOptions = {
        quality: config.quality,
        allowEditing: config.allowEditing,
        resultType: config.resultType,
        source: config.source,
        saveToGallery: config.saveToGallery,
        width: config.width,
        height: config.height,
        correctOrientation: config.correctOrientation,
        direction: config.direction,
        presentationStyle: config.presentationStyle,
        webUseInput: config.webUseInput,
        promptLabelHeader: config.promptLabelHeader,
        promptLabelCancel: config.promptLabelCancel,
        promptLabelPhoto: config.promptLabelPhoto,
        promptLabelPicture: config.promptLabelPicture,
      };
      const photo = await Camera.getPhoto(options);
      this.props.onPhotoResult({
        path: photo.path,
        webPath: photo.webPath,
        base64String: photo.base64String,
        dataUrl: photo.dataUrl,
        exif: photo.exif,
      });
    } catch (e) {
      const error = e as any;
      const errorMessage = error.code ? `[${error.code}] ${error.message}` : error.message;
      alert(`Failed to get picture with error:\n${errorMessage}`);
    }
  };

  render() {
    const { buttonLabel = "getPhoto (Default)" } = this.props;
    const { config } = this.state;

    return (
      <>
        <IonButton expand="block" onClick={() => this.executeDefault()}>
          {buttonLabel}
        </IonButton>

        <IonAccordionGroup>
          <IonAccordion value="getPhoto">
            <IonItem slot="header" lines="none">
              <IonLabel
                style={{
                  fontSize: "0.9em",
                  color: "var(--ion-color-medium)",
                }}
              >
                Expand to call getPhoto with configurable options
              </IonLabel>
            </IonItem>
            <div slot="content" style={{ padding: "16px" }}>
              {/* Core settings */}
              <IonItem>
                <IonLabel position="stacked">Quality (0-100)</IonLabel>
                <IonInput
                  type="number"
                  min="0"
                  max="100"
                  value={config.quality}
                  onIonInput={(e) =>
                    this.updateConfig("quality", parseInt(e.detail.value!) || 100)
                  }
                />
              </IonItem>

              <IonItem>
                <IonLabel position="stacked">Result Type</IonLabel>
                <IonSelect
                  value={config.resultType}
                  onIonChange={(e) =>
                    this.updateConfig("resultType", e.detail.value)
                  }
                >
                  <IonSelectOption value={CameraResultType.Uri}>
                    Uri
                  </IonSelectOption>
                  <IonSelectOption value={CameraResultType.Base64}>
                    Base64
                  </IonSelectOption>
                  <IonSelectOption value={CameraResultType.DataUrl}>
                    DataUrl
                  </IonSelectOption>
                </IonSelect>
              </IonItem>

              <IonItem>
                <IonLabel position="stacked">Source</IonLabel>
                <IonSelect
                  value={config.source}
                  onIonChange={(e) => this.updateConfig("source", e.detail.value)}
                >
                  <IonSelectOption value={CameraSource.Prompt}>
                    Prompt
                  </IonSelectOption>
                  <IonSelectOption value={CameraSource.Camera}>
                    Camera
                  </IonSelectOption>
                  <IonSelectOption value={CameraSource.Photos}>
                    Photos
                  </IonSelectOption>
                </IonSelect>
              </IonItem>

              {/* Platform-specific */}
              <IonItem>
                <IonLabel position="stacked">Direction (iOS)</IonLabel>
                <IonSelect
                  value={config.direction}
                  onIonChange={(e) =>
                    this.updateConfig("direction", e.detail.value)
                  }
                >
                  <IonSelectOption value={CameraDirection.Rear}>
                    Rear
                  </IonSelectOption>
                  <IonSelectOption value={CameraDirection.Front}>
                    Front
                  </IonSelectOption>
                </IonSelect>
              </IonItem>

              <IonItem>
                <IonLabel position="stacked">
                  Presentation Style (iOS)
                </IonLabel>
                <IonSelect
                  value={config.presentationStyle}
                  onIonChange={(e) =>
                    this.updateConfig("presentationStyle", e.detail.value)
                  }
                >
                  <IonSelectOption value="fullscreen">
                    Fullscreen
                  </IonSelectOption>
                  <IonSelectOption value="popover">Popover</IonSelectOption>
                </IonSelect>
              </IonItem>

              {/* Dimensions */}
              <IonItem>
                <IonLabel position="stacked">Width (optional)</IonLabel>
                <IonInput
                  type="number"
                  placeholder="Leave empty for no constraint"
                  value={config.width ?? ""}
                  onIonInput={(e) =>
                    this.updateConfig(
                      "width",
                      e.detail.value ? parseInt(e.detail.value) : undefined
                    )
                  }
                />
              </IonItem>

              <IonItem>
                <IonLabel position="stacked">Height (optional)</IonLabel>
                <IonInput
                  type="number"
                  placeholder="Leave empty for no constraint"
                  value={config.height ?? ""}
                  onIonInput={(e) =>
                    this.updateConfig(
                      "height",
                      e.detail.value ? parseInt(e.detail.value) : undefined
                    )
                  }
                />
              </IonItem>

              {/* Boolean toggles */}
              <IonItem>
                <IonLabel>Allow Editing</IonLabel>
                <IonToggle
                  checked={config.allowEditing}
                  onIonChange={(e) =>
                    this.updateConfig("allowEditing", e.detail.checked)
                  }
                />
              </IonItem>

              <IonItem>
                <IonLabel>Save to Gallery</IonLabel>
                <IonToggle
                  checked={config.saveToGallery}
                  onIonChange={(e) =>
                    this.updateConfig("saveToGallery", e.detail.checked)
                  }
                />
              </IonItem>

              <IonItem>
                <IonLabel>Correct Orientation</IonLabel>
                <IonToggle
                  checked={config.correctOrientation}
                  onIonChange={(e) =>
                    this.updateConfig("correctOrientation", e.detail.checked)
                  }
                />
              </IonItem>

              <IonItem>
                <IonLabel>Web Use Input</IonLabel>
                <IonToggle
                  checked={config.webUseInput}
                  onIonChange={(e) =>
                    this.updateConfig("webUseInput", e.detail.checked)
                  }
                />
              </IonItem>

              {/* Text customizations */}
              <IonItem>
                <IonLabel position="stacked">Prompt Label Header</IonLabel>
                <IonInput
                  type="text"
                  value={config.promptLabelHeader}
                  onIonChange={(e) =>
                    this.updateConfig(
                      "promptLabelHeader",
                      e.detail.value ?? "Photo"
                    )
                  }
                />
              </IonItem>

              <IonItem>
                <IonLabel position="stacked">Prompt Label Cancel</IonLabel>
                <IonInput
                  type="text"
                  value={config.promptLabelCancel}
                  onIonChange={(e) =>
                    this.updateConfig(
                      "promptLabelCancel",
                      e.detail.value ?? "Cancel"
                    )
                  }
                />
              </IonItem>

              <IonItem>
                <IonLabel position="stacked">Prompt Label Photo</IonLabel>
                <IonInput
                  type="text"
                  value={config.promptLabelPhoto}
                  onIonChange={(e) =>
                    this.updateConfig(
                      "promptLabelPhoto",
                      e.detail.value ?? "From Photos"
                    )
                  }
                />
              </IonItem>

              <IonItem>
                <IonLabel position="stacked">Prompt Label Picture</IonLabel>
                <IonInput
                  type="text"
                  value={config.promptLabelPicture}
                  onIonChange={(e) =>
                    this.updateConfig(
                      "promptLabelPicture",
                      e.detail.value ?? "Take Picture"
                    )
                  }
                />
              </IonItem>

              <IonButton
                expand="block"
                color="primary"
                style={{ marginTop: "16px" }}
                onClick={() => this.executeWithConfig()}
              >
                Execute getPhoto with Configuration
              </IonButton>
            </div>
          </IonAccordion>
        </IonAccordionGroup>
      </>
    );
  }
}

export default GetPhotoConfigurable;
