import {
  IonButton,
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
import { Camera, CameraSource } from "@capacitor/camera";
import PhotoWithMetadata from "../components/camera/PhotoWithMetadata";
import TakePictureConfigurable from "../components/camera/TakePictureConfigurable";
import { GetPhotoConfigurable } from "../components/camera/old-methods";
import { MediaHistoryService } from "../services/MediaHistoryService";

interface MediaResult {
  path: string;
  webPath: string;
  duration?: number;
  size: number;
  format: string;
  saved: boolean;
}

interface ITakePicturePageState {
  filePath: string | null;
  metadata: string | null;
  editedPhoto: MediaResult | null;
}

class TakePicturePage extends React.Component<{}, ITakePicturePageState> {
  constructor(props: {}) {
    super(props);
    this.state = {
      filePath: null,
      metadata: null,
      editedPhoto: null,
    };
  }

  handleTakePhotoResult = (result: {
    path: string;
    webPath: string;
    duration?: number;
    size: number;
    format: string;
    saved: boolean;
  }): void => {
    const metadata = {
      size: result.size,
      format: result.format,
      saved: result.saved,
    };

    this.setState({
      filePath: result.path ?? result.webPath,
      metadata: JSON.stringify(metadata, null, 2),
    });

    MediaHistoryService.addMedia({
      mediaType: "photo",
      method: "takePhoto",
      path: result.path,
      webPath: result.webPath,
      format: result.format,
      size: result.size,
      saved: result.saved,
    });
  };

  handlePhotoResult = (result: {
    path?: string;
    webPath?: string;
    exif?: any;
  }): void => {
    this.setState({
      filePath: result.path ?? result.webPath ?? null,
      metadata: JSON.stringify(result.exif, null, 2),
    });

    if (result.path || result.webPath) {
      MediaHistoryService.addMedia({
        mediaType: "photo",
        method: "getPhoto",
        path: result.path || "",
        webPath: result.webPath || "",
      });
    }
  };

  clearPhoto = (): void => {
    this.setState({
      filePath: null,
      metadata: null,
      editedPhoto: null,
    });
  };

  handleEditPhoto = async (filePath: string): Promise<void> => {
    try {
      const result = await Camera.editURIPhoto({
        uri: filePath,
        saveToGallery: false,
        includeMetadata: true,
      });
      this.setState({ editedPhoto: result });

      MediaHistoryService.addMedia({
        mediaType: "photo",
        method: "editURIPhoto",
        path: result.path,
        webPath: result.webPath,
        format: result.format,
        size: result.size,
        saved: result.saved,
      });
    } catch (e) {
      const error = e as any;
      const errorMessage = error.code ? `[${error.code}] ${error.message}` : error.message;
      alert(`Failed to edit photo with error:\n${errorMessage}`);
    }
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
          <IonCard>
            <IonCardContent>
              <TakePictureConfigurable
                onPhotoResult={this.handleTakePhotoResult}
              />
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
            <>
              <IonButton
                expand="block"
                color="danger"
                fill="outline"
                onClick={this.clearPhoto}
                style={{ margin: "0 16px 16px 16px" }}
              >
                Clear Photo
              </IonButton>
              <PhotoWithMetadata
                filePath={this.state.filePath}
                metadata={this.state.metadata}
                onEdit={this.handleEditPhoto}
              />
            </>
          )}
          {this.state.editedPhoto !== null && (
            <>
              <div style={{ padding: "0 16px", marginTop: "16px" }}>
                <h3>Edited Photo</h3>
              </div>
              <PhotoWithMetadata
                filePath={this.state.editedPhoto.path}
                metadata={JSON.stringify(
                  {
                    size: this.state.editedPhoto.size,
                    format: this.state.editedPhoto.format,
                    saved: this.state.editedPhoto.saved,
                  },
                  null,
                  2
                )}
              />
            </>
          )}
          <div style={{ height: '80px' }} />
        </IonContent>
      </IonPage>
    );
  }
}

export default TakePicturePage;
