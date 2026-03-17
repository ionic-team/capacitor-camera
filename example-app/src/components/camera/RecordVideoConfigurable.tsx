import {
  IonButton,
  IonAccordion,
  IonAccordionGroup,
  IonItem,
  IonLabel,
  IonToggle,
} from "@ionic/react";
import React from "react";
import { Camera, MediaResult } from "@capacitor/camera";

interface RecordVideoConfig {
  saveToGallery: boolean;
  includeMetadata: boolean;
  isPersistent: boolean;
}

interface RecordVideoConfigurableProps {
  buttonLabel?: string;
  onVideoResult: (result: MediaResult) => void;
}

interface RecordVideoConfigurableState {
  config: RecordVideoConfig;
}

class RecordVideoConfigurable extends React.Component<
  RecordVideoConfigurableProps,
  RecordVideoConfigurableState
> {
  constructor(props: RecordVideoConfigurableProps) {
    super(props);
    this.state = {
      config: {
        saveToGallery: false,
        includeMetadata: true,
        isPersistent: true,
      },
    };
  }

  updateConfig = (field: keyof RecordVideoConfig, value: any): void => {
    this.setState({
      config: { ...this.state.config, [field]: value },
    });
  };

  executeDefault = async (): Promise<void> => {
    try {
      const result = await Camera.recordVideo({});
      this.props.onVideoResult(result);
    } catch (e) {
      const error = e as any;
      const errorMessage = error.code ? `[${error.code}] ${error.message}` : error.message;
      alert(`Failed to record video with error:\n${errorMessage}`);
    }
  };

  executeWithConfig = async (): Promise<void> => {
    try {
      const config = this.state.config;
      const result = await Camera.recordVideo({
        saveToGallery: config.saveToGallery,
        includeMetadata: config.includeMetadata,
        isPersistent: config.isPersistent,
      });
      this.props.onVideoResult(result);
    } catch (e) {
      const error = e as any;
      const errorMessage = error.code ? `[${error.code}] ${error.message}` : error.message;
      alert(`Failed to record video with error:\n${errorMessage}`);
    }
  };

  render() {
    const { buttonLabel = "recordVideo (Default)" } = this.props;
    const { config } = this.state;

    return (
      <>
        <IonButton expand="block" onClick={() => this.executeDefault()}>
          {buttonLabel}
        </IonButton>

        <IonAccordionGroup>
          <IonAccordion value="recordVideo">
            <IonItem slot="header" lines="none">
              <IonLabel
                style={{
                  fontSize: "0.9em",
                  color: "var(--ion-color-medium)",
                }}
              >
                Expand to call recordVideo with configurable options
              </IonLabel>
            </IonItem>
            <div slot="content" style={{ padding: "16px" }}>
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
                <IonLabel>Include Metadata</IonLabel>
                <IonToggle
                  checked={config.includeMetadata}
                  onIonChange={(e) =>
                    this.updateConfig("includeMetadata", e.detail.checked)
                  }
                />
              </IonItem>

              <IonItem>
                <IonLabel>Is Persistent</IonLabel>
                <IonToggle
                  checked={config.isPersistent}
                  onIonChange={(e) =>
                    this.updateConfig("isPersistent", e.detail.checked)
                  }
                />
              </IonItem>

              <IonButton
                expand="block"
                color="primary"
                style={{ marginTop: "16px" }}
                onClick={() => this.executeWithConfig()}
              >
                Execute recordVideo with Configuration
              </IonButton>
            </div>
          </IonAccordion>
        </IonAccordionGroup>
      </>
    );
  }
}

export default RecordVideoConfigurable;
