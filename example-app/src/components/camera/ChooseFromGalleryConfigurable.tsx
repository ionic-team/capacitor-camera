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
  MediaResult,
  MediaTypeSelection,
  ChooseFromGalleryOptions,
} from "@capacitor/camera";


interface ChooseFromGalleryConfigurableProps {
  buttonLabel?: string;
  onMediaResult: (results: MediaResult[]) => void;
}

interface ChooseFromGalleryConfigurableState {
  config: ChooseFromGalleryOptions;
}

class ChooseFromGalleryConfigurable extends React.Component<
  ChooseFromGalleryConfigurableProps,
  ChooseFromGalleryConfigurableState
> {
  constructor(props: ChooseFromGalleryConfigurableProps) {
    super(props);
    // Initialize with API defaults from ChooseFromGalleryOptions
    this.state = {
      config: {
        mediaType: MediaTypeSelection.Photo,
        allowMultipleSelection: false,
        limit: 0,
        includeMetadata: false,
        allowEdit: false,
        editInApp: true,
        presentationStyle: 'fullscreen',
        quality: 100,
        correctOrientation: true,
      },
    };
  }

  updateConfig = (field: keyof ChooseFromGalleryOptions, value: any): void => {
    this.setState({
      config: { ...this.state.config, [field]: value },
    });
  };

  executeDefault = async (): Promise<void> => {
    try {
      const result = await Camera.chooseFromGallery({
        mediaType: MediaTypeSelection.Photo
      });
      console.log('chooseFromGallery result', result);

      this.props.onMediaResult(result.results);
    } catch (e) {
      const error = e as any;
      const errorMessage = error.code ? `[${error.code}] ${error.message}` : error.message;
      alert(`Failed to choose from gallery with error:\n${errorMessage}`);
    }
  };

  executeWithConfig = async (): Promise<void> => {
    try {
      const config = this.state.config;
      const result = await Camera.chooseFromGallery({
        mediaType: config.mediaType,
        allowMultipleSelection: config.allowMultipleSelection,
        limit: config.limit,
        includeMetadata: config.includeMetadata,
        allowEdit: config.allowEdit,
        editInApp: config.editInApp,
        presentationStyle: config.presentationStyle,
        quality: config.quality,
        targetWidth: config.targetWidth,
        targetHeight: config.targetHeight,
        correctOrientation: config.correctOrientation,
        webUseInput: config.webUseInput,
      });
      console.log('chooseFromGallery result', result);

      this.props.onMediaResult(result.results);
    } catch (e) {
      const error = e as any;
      const errorMessage = error.code ? `[${error.code}] ${error.message}` : error.message;
      alert(`Failed to choose from gallery with error:\n${errorMessage}`);
    }
  };

  render() {
    const { buttonLabel = 'chooseFromGallery (Default)' } = this.props;
    const { config } = this.state;

    return (
      <>
        <IonButton expand="block" onClick={() => this.executeDefault()} style={{ marginTop: '16px' }}>
          {buttonLabel}
        </IonButton>

        <IonAccordionGroup>
          <IonAccordion value="chooseFromGallery">
            <IonItem slot="header" lines="none">
              <IonLabel
                style={{
                  fontSize: '0.9em',
                  color: 'var(--ion-color-medium)',
                }}
              >
                Expand to call chooseFromGallery with configurable options
              </IonLabel>
            </IonItem>
            <div slot="content" style={{ padding: '16px' }}>
              <IonItem>
                <IonLabel position="stacked">Media Type</IonLabel>
                <IonSelect
                  value={config.mediaType}
                  onIonChange={(e) => this.updateConfig('mediaType', parseInt(e.detail.value!))}
                >
                  <IonSelectOption value={0}>Picture</IonSelectOption>
                  <IonSelectOption value={1}>Video</IonSelectOption>
                  <IonSelectOption value={2}>All</IonSelectOption>
                </IonSelect>
              </IonItem>

              <IonItem>
                <IonLabel>Allow Multiple Selection</IonLabel>
                <IonToggle
                  checked={config.allowMultipleSelection}
                  onIonChange={(e) => this.updateConfig('allowMultipleSelection', e.detail.checked)}
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
                  onIonInput={(e) =>
                    this.updateConfig("limit", parseInt(e.detail.value!) || 0)
                  }
                />
              </IonItem>

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
                <IonLabel position="stacked">Target Width (optional)</IonLabel>
                <IonInput
                  type="number"
                  placeholder="Leave empty for no constraint"
                  value={config.targetWidth ?? ""}
                  onIonInput={(e) =>
                    this.updateConfig(
                      "targetWidth",
                      e.detail.value ? parseInt(e.detail.value) : undefined
                    )
                  }
                />
              </IonItem>

              <IonItem>
                <IonLabel position="stacked">Target Height (optional)</IonLabel>
                <IonInput
                  type="number"
                  placeholder="Leave empty for no constraint"
                  value={config.targetHeight ?? ""}
                  onIonInput={(e) =>
                    this.updateConfig(
                      "targetHeight",
                      e.detail.value ? parseInt(e.detail.value) : undefined
                    )
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

              {/* Boolean toggles */}
              <IonItem>
                <IonLabel>Include Metadata</IonLabel>
                <IonToggle
                  checked={config.includeMetadata}
                  onIonChange={(e) => this.updateConfig('includeMetadata', e.detail.checked)}
                />
              </IonItem>

              <IonItem>
                <IonLabel>Allow Edit</IonLabel>
                <IonToggle
                  checked={config.allowEdit}
                  onIonChange={(e) => this.updateConfig('allowEdit', e.detail.checked)}
                />
              </IonItem>

              <IonItem>
                <IonLabel>Edit In App</IonLabel>
                <IonToggle
                  checked={config.editInApp}
                  onIonChange={(e) => this.updateConfig('editInApp', e.detail.checked)}
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

              <IonButton
                expand="block"
                color="primary"
                style={{ marginTop: '16px' }}
                onClick={() => this.executeWithConfig()}
              >
                Execute chooseFromGallery with Configuration
              </IonButton>
            </div>
          </IonAccordion>
        </IonAccordionGroup>
      </>
    );
  }
}

export default ChooseFromGalleryConfigurable;
