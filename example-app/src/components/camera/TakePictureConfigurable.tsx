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
  CameraResultType,
  CameraDirection,
} from "@capacitor/camera";

interface TakePictureConfig {
  quality: number;
  allowEditing: boolean;
  resultType: CameraResultType;
  saveToGallery: boolean;
  width: number | undefined;
  height: number | undefined;
  correctOrientation: boolean;
  direction: CameraDirection;
  presentationStyle: 'fullscreen' | 'popover';
}

interface PhotoResult {
  path: string;
  webPath: string;
  duration?: number;
  size: number;
  format: string;
  saved: boolean;
}

interface TakePictureConfigurableProps {
  buttonLabel?: string;
  onPhotoResult: (result: PhotoResult) => void;
}

interface TakePictureConfigurableState {
  config: TakePictureConfig;
}

class TakePictureConfigurable extends React.Component<
  TakePictureConfigurableProps,
  TakePictureConfigurableState
> {
  constructor(props: TakePictureConfigurableProps) {
    super(props);
    this.state = {
      config: {
        quality: 100,
        allowEditing: false,
        resultType: CameraResultType.Uri, // TODO confirm if we want this in the new API?
        saveToGallery: false,
        width: undefined,
        height: undefined,
        correctOrientation: true,
        direction: CameraDirection.Rear,
        presentationStyle: 'fullscreen',
      },
    };
  }

  updateConfig = (field: keyof TakePictureConfig, value: any): void => {
    this.setState({
      config: { ...this.state.config, [field]: value },
    });
  };

  executeDefault = async (): Promise<void> => {
    try {
      const options: ImageOptions = {
        resultType: CameraResultType.Uri,
      };
      const result = await Camera.takePhoto(options);
      this.props.onPhotoResult(result);
    } catch (e) {
      const error = e as any;
      const errorMessage = error.code ? `[${error.code}] ${error.message}` : error.message;
      alert(`Failed to take picture with error:\n${errorMessage}`);
    }
  };

  executeWithConfig = async (): Promise<void> => {
    try {
      const config = this.state.config;
      const options: ImageOptions = {
        quality: config.quality,
        allowEditing: config.allowEditing,
        resultType: config.resultType,
        saveToGallery: config.saveToGallery,
        width: config.width,
        height: config.height,
        correctOrientation: config.correctOrientation,
        direction: config.direction,
        presentationStyle: config.presentationStyle,
      };
      const result = await Camera.takePhoto(options);
      this.props.onPhotoResult(result);
    } catch (e) {
      const error = e as any;
      const errorMessage = error.code ? `[${error.code}] ${error.message}` : error.message;
      alert(`Failed to take picture with error:\n${errorMessage}`);
    }
  };

  render() {
    const { buttonLabel = "takePhoto (Default)" } = this.props;
    const { config } = this.state;

    return (
      <>
        <IonButton expand="block" onClick={() => this.executeDefault()}>
          {buttonLabel}
        </IonButton>

        <IonAccordionGroup>
          <IonAccordion value="takePhoto">
            <IonItem slot="header" lines="none">
              <IonLabel
                style={{
                  fontSize: "0.9em",
                  color: "var(--ion-color-medium)",
                }}
              >
                Expand to call takePhoto with configurable options
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
                  onIonChange={(e) =>
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
                  onIonChange={(e) =>
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
                  onIonChange={(e) =>
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

              <IonButton
                expand="block"
                color="primary"
                style={{ marginTop: "16px" }}
                onClick={() => this.executeWithConfig()}
              >
                Execute takePhoto with Configuration
              </IonButton>
            </div>
          </IonAccordion>
        </IonAccordionGroup>
      </>
    );
  }
}

export default TakePictureConfigurable;
