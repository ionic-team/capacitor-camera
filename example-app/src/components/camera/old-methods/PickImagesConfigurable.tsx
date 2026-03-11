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
  GalleryImageOptions,
  GalleryPhoto,
} from "@capacitor/camera";
import { PickImagesConfig } from "./types";

interface PickImagesConfigurableProps {
  buttonLabel?: string;
  onPhotosResult: (photos: GalleryPhoto[]) => void;
}

interface PickImagesConfigurableState {
  config: PickImagesConfig;
}

class PickImagesConfigurable extends React.Component<
  PickImagesConfigurableProps,
  PickImagesConfigurableState
> {
  constructor(props: PickImagesConfigurableProps) {
    super(props);
    this.state = {
      config: {
        quality: 100,
        width: undefined,
        height: undefined,
        correctOrientation: true,
        presentationStyle: 'fullscreen',
        limit: 0,
      },
    };
  }

  updateConfig = (field: keyof PickImagesConfig, value: any): void => {
    this.setState({
      config: { ...this.state.config, [field]: value },
    });
  };

  executeDefault = async (): Promise<void> => {
    try {
      const options: GalleryImageOptions = {
        quality: 100,
        limit: 0,
      };
      const photosResult = await Camera.pickImages(options);
      console.log("photos result", photosResult);
      this.props.onPhotosResult(photosResult.photos);
    } catch (e) {
      alert(`Failed to pick images with error:\n'${e}'`);
    }
  };

  executeWithConfig = async (): Promise<void> => {
    try {
      const config = this.state.config;
      const options: GalleryImageOptions = {
        quality: config.quality,
        width: config.width,
        height: config.height,
        correctOrientation: config.correctOrientation,
        presentationStyle: config.presentationStyle,
        limit: config.limit,
      };
      const photosResult = await Camera.pickImages(options);
      console.log("photos result", photosResult);
      this.props.onPhotosResult(photosResult.photos);
    } catch (e) {
      alert(`Failed to pick images with error:\n'${e}'`);
    }
  };

  render() {
    const { buttonLabel = "pickImages (Default)" } = this.props;
    const { config } = this.state;

    return (
      <>
        <IonButton
          expand="block"
          onClick={() => this.executeDefault()}
          style={{ marginTop: "16px" }}
        >
          {buttonLabel}
        </IonButton>

        <IonAccordionGroup>
          <IonAccordion value="pickImages">
            <IonItem slot="header" lines="none">
              <IonLabel
                style={{
                  fontSize: "0.9em",
                  color: "var(--ion-color-medium)",
                }}
              >
                Configure
              </IonLabel>
            </IonItem>
            <div slot="content" style={{ padding: "16px" }}>
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

              <IonItem>
                <IonLabel position="stacked">
                  Limit (0 = unlimited, Android 13+ & iOS)
                </IonLabel>
                <IonInput
                  type="number"
                  min="0"
                  value={config.limit}
                  onIonChange={(e) =>
                    this.updateConfig("limit", parseInt(e.detail.value!) || 0)
                  }
                />
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
                Execute pickImages with Configuration
              </IonButton>
            </div>
          </IonAccordion>
        </IonAccordionGroup>
      </>
    );
  }
}

export default PickImagesConfigurable;
