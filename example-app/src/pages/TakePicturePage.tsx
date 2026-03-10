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
  IonButton,
} from "@ionic/react";
import React from "react";
import {
  Camera,
  ImageOptions,
  CameraSource,
  CameraResultType,
} from "@capacitor/camera";
import PhotoWithMetadata from "../components/camera/PhotoWithMetadata";

interface ITakePicturePageState {
  filePath: string | null;
  metadata: string | null;
}

class TakePicturePage extends React.Component<{}, ITakePicturePageState> {
  constructor(props: {}) {
    super(props);
    this.state = { filePath: null, metadata: null };
  }

  addPhoto = async (
    source: CameraSource,
    save: boolean = false,
    editing: boolean = false,
  ): Promise<void> => {
    try {
      const options: ImageOptions = {
        quality: 100,
        resultType: CameraResultType.Uri,
        source,
        saveToGallery: save,
        allowEditing: editing,
        webUseInput: false,
      };
      const photo = await Camera.getPhoto(options);
      this.setState({
        filePath: photo.path ?? photo.webPath ?? null,
        metadata: JSON.stringify(photo.exif, null, 2),
      });
    } catch (e) {
      alert(`Failed to get picture with error:\n'${e}'`);
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
              <IonButton
                expand="block"
                onClick={() => this.addPhoto(CameraSource.Camera)}
              >
                Take Picture
              </IonButton>
              <IonButton
                expand="block"
                onClick={() => this.addPhoto(CameraSource.Camera, true)}
              >
                Take Picture and Save
              </IonButton>
              <IonButton
                expand="block"
                onClick={() => this.addPhoto(CameraSource.Camera, false, true)}
              >
                Take Picture and Edit
              </IonButton>
              <IonButton
                expand="block"
                onClick={() => this.addPhoto(CameraSource.Prompt)}
              >
                Prompt
              </IonButton>
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
