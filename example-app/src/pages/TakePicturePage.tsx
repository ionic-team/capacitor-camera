import {
  IonButtons,
  IonContent,
  IonHeader,
  IonPage,
  IonMenuButton,
  IonTitle,
  IonToolbar,
  IonCardContent,
  IonCard,
  IonAccordion,
  IonAccordionGroup,
  IonItem,
  IonLabel,
} from "@ionic/react";
import React from "react";
import { CameraSource } from "@capacitor/camera";
import PhotoWithMetadata from "../components/camera/PhotoWithMetadata";
import { GetPhotoConfigurable } from "../components/camera/old-methods";

interface ITakePicturePageState {
  filePath: string | null;
  metadata: string | null;
}

class TakePicturePage extends React.Component<{}, ITakePicturePageState> {
  constructor(props: {}) {
    super(props);
    this.state = {
      filePath: null,
      metadata: null,
    };
  }

  handlePhotoResult = (result: {
    path?: string;
    webPath?: string;
    exif?: any;
  }): void => {
    this.setState({
      filePath: result.path ?? result.webPath ?? null,
      metadata: JSON.stringify(result.exif, null, 2),
    });
  };

  render() {
    return (
      <IonPage>
        <IonHeader>
          <IonToolbar>
            <IonButtons slot="start">
              <IonMenuButton />
            </IonButtons>
            <IonTitle>Take Picture</IonTitle>
          </IonToolbar>
        </IonHeader>
        <IonContent>
          {/* Placeholder for future new methods */}
          <IonCard>
            <IonCardContent>
              {/* New methods will be added here */}
            </IonCardContent>
          </IonCard>
          <IonCard>
            <IonCardContent>
              <IonAccordionGroup>
                <IonAccordion value="old-methods">
                  <IonItem slot="header">
                    <IonLabel><b>Old methods</b></IonLabel>
                  </IonItem>
                  <div slot="content" style={{ padding: "16px 0" }}>
                    <GetPhotoConfigurable
                      defaultSource={CameraSource.Prompt}
                      onPhotoResult={this.handlePhotoResult}
                    />
                  </div>
                </IonAccordion>
              </IonAccordionGroup>
            </IonCardContent>
          </IonCard>
          {this.state.filePath !== null && (
            <PhotoWithMetadata
              filePath={this.state.filePath}
              metadata={this.state.metadata}
            />
          )}
        </IonContent>
      </IonPage>
    );
  }
}

export default TakePicturePage;
