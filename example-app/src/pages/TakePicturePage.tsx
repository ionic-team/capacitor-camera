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
import { Camera, CameraSource, MediaResult, MediaMetadata } from "@capacitor/camera";
import PhotoWithMetadata from "../components/camera/PhotoWithMetadata";
import TakePictureConfigurable from "../components/camera/TakePictureConfigurable";
import { GetPhotoConfigurable } from "../components/camera/old-methods";
import { MediaHistoryService } from "../services/MediaHistoryService";

interface ITakePicturePageState {
  filePath: string | null;
  metadata: MediaMetadata | string | null;
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

  handleTakePhotoResult = (result: MediaResult): void => {
    this.setState({
      filePath: result.uri ?? result.webPath ?? '',
      metadata: result.metadata ?? null,
    });

    MediaHistoryService.addMedia({
      mediaType: "photo",
      method: "takePhoto",
      uri: result.uri,
      webPath: result.webPath,
      format: result.metadata?.format,
      size: result.metadata?.size,
      saved: result.saved,
      metadata: result.metadata,
    });
  };

  handlePhotoResult = (result: {
    path?: string;
    webPath?: string;
    base64String?: string;
    dataUrl?: string;
    exif?: any;
  }): void => {
    const filePath =
      result.path ??
      result.webPath ??
      result.dataUrl ??
      (result.base64String ? `data:image/jpeg;base64,${result.base64String}` : null);

    this.setState({
      filePath,
      metadata: JSON.stringify(result.exif, null, 2),
    });

    if (filePath) {
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
        uri: result.uri,
        webPath: result.webPath,
        format: result.metadata?.format,
        size: result.metadata?.size,
        saved: result.saved,
        metadata: result.metadata,
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
                filePath={this.state.editedPhoto.uri ?? this.state.editedPhoto.webPath ?? ''}
                metadata={this.state.editedPhoto.metadata ?? null}
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
