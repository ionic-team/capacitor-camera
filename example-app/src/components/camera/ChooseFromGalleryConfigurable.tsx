import {
  IonButton,
  IonAccordion,
  IonAccordionGroup,
  IonItem,
  IonLabel,
  IonSelect,
  IonSelectOption,
  IonToggle,
} from "@ionic/react";
import React from "react";
import {
  Camera,
  MediaResult,
  MediaType,
} from "@capacitor/camera";

interface ChooseFromGalleryConfig {
  mediaType: number;
  allowMultipleSelection: boolean;
  includeMetadata: boolean;
  allowEdit: boolean;
}

interface ChooseFromGalleryConfigurableProps {
  buttonLabel?: string;
  onMediaResult: (results: MediaResult[]) => void;
}

interface ChooseFromGalleryConfigurableState {
  config: ChooseFromGalleryConfig;
}

class ChooseFromGalleryConfigurable extends React.Component<
  ChooseFromGalleryConfigurableProps,
  ChooseFromGalleryConfigurableState
> {
  constructor(props: ChooseFromGalleryConfigurableProps) {
    super(props);
    this.state = {
      config: {
        mediaType: MediaType.picture,
        allowMultipleSelection: false,
        includeMetadata: false,
        allowEdit: false,
      },
    };
  }

  updateConfig = (field: keyof ChooseFromGalleryConfig, value: any): void => {
    this.setState({
      config: { ...this.state.config, [field]: value },
    });
  };

  executeDefault = async (): Promise<void> => {
    try {
      const result = await Camera.chooseFromGallery({
        mediaType: MediaType.all,
        allowMultipleSelection: true,
        includeMetadata: false,
        allowEdit: false,
        limit: 2
      });
      console.log("chooseFromGallery result", result);

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
        includeMetadata: config.includeMetadata,
        allowEdit: config.allowEdit,
      });
      console.log("chooseFromGallery result", result);

      this.props.onMediaResult(result.results);
    } catch (e) {
      const error = e as any;
      const errorMessage = error.code ? `[${error.code}] ${error.message}` : error.message;
      alert(`Failed to choose from gallery with error:\n${errorMessage}`);
    }
  };

  render() {
    const { buttonLabel = "chooseFromGallery (Default)" } = this.props;
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
          <IonAccordion value="chooseFromGallery">
            <IonItem slot="header" lines="none">
              <IonLabel
                style={{
                  fontSize: "0.9em",
                  color: "var(--ion-color-medium)",
                }}
              >
                Expand to call chooseFromGallery with configurable options
              </IonLabel>
            </IonItem>
            <div slot="content" style={{ padding: "16px" }}>
              <IonItem>
                <IonLabel position="stacked">Media Type</IonLabel>
                <IonSelect
                  value={config.mediaType}
                  onIonChange={(e) =>
                    this.updateConfig("mediaType", parseInt(e.detail.value!))
                  }
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
                  onIonChange={(e) =>
                    this.updateConfig("allowMultipleSelection", e.detail.checked)
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
                <IonLabel>Allow Edit</IonLabel>
                <IonToggle
                  checked={config.allowEdit}
                  onIonChange={(e) =>
                    this.updateConfig("allowEdit", e.detail.checked)
                  }
                />
              </IonItem>

              <IonButton
                expand="block"
                color="primary"
                style={{ marginTop: "16px" }}
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
