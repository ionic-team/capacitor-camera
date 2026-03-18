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
  TakePhotoOptions,
  MediaResult,
  EncodingType,
  CameraDirection,
} from "@capacitor/camera";

interface TakePictureConfigurableProps {
  buttonLabel?: string;
  onPhotoResult: (result: MediaResult) => void;
}

interface TakePictureConfigurableState {
  config: TakePhotoOptions;
}

class TakePictureConfigurable extends React.Component<
  TakePictureConfigurableProps,
  TakePictureConfigurableState
> {
  constructor(props: TakePictureConfigurableProps) {
    super(props);
    // Initialize with API defaults from TakePhotoOptions
    this.state = {
      config: {
        quality: 100,
        allowEdit: false,
        encodingType: EncodingType.JPEG,
        saveToGallery: false,
        correctOrientation: true,
        cameraDirection: CameraDirection.Rear,
        presentationStyle: 'fullscreen',
        editInApp: true,
        includeMetadata: false,
      },
    };
  }

  updateConfig = (field: keyof TakePhotoOptions, value: any): void => {
    this.setState({
      config: { ...this.state.config, [field]: value },
    });
  };

  executeDefault = async (): Promise<void> => {
    try {
      const options: TakePhotoOptions = {};
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
      const result = await Camera.takePhoto(this.state.config);
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
                <IonLabel position="stacked">Encoding Type</IonLabel>
                <IonSelect
                  value={config.encodingType}
                  onIonChange={(e) =>
                    this.updateConfig("encodingType", e.detail.value)
                  }
                >
                  <IonSelectOption value={EncodingType.JPEG}>
                    JPEG
                  </IonSelectOption>
                  <IonSelectOption value={EncodingType.PNG}>
                    PNG
                  </IonSelectOption>
                </IonSelect>
              </IonItem>

              {/* Platform-specific */}
              <IonItem>
                <IonLabel position="stacked">Camera Direction (iOS/Web)</IonLabel>
                <IonSelect
                  value={config.cameraDirection}
                  onIonChange={(e) =>
                    this.updateConfig("cameraDirection", e.detail.value)
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
                <IonLabel>Allow Edit</IonLabel>
                <IonToggle
                  checked={config.allowEdit}
                  onIonChange={(e) =>
                    this.updateConfig("allowEdit", e.detail.checked)
                  }
                />
              </IonItem>

              <IonItem>
                <IonLabel>Edit In App</IonLabel>
                <IonToggle
                  checked={config.editInApp}
                  onIonChange={(e) =>
                    this.updateConfig("editInApp", e.detail.checked)
                  }
                />
              </IonItem>

              <IonItem>
                <IonLabel>Include Metadata</IonLabel>
                <IonToggle
                  checked={config.includeMetadata}
                  onIonChange={(e) =>
                    this.updateConfig("includeMetadata", e.detail.checked)
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
