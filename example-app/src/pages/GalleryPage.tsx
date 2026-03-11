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
  IonAccordion,
  IonAccordionGroup,
  IonItem,
  IonLabel,
} from "@ionic/react";
import React from "react";
import {
  Camera,
  ImageOptions,
  CameraSource,
  CameraResultType,
  GalleryPhoto,
  GalleryImageOptions,
} from "@capacitor/camera";
import PhotoWithMetadata from "../components/camera/PhotoWithMetadata";
import PhotoGrid from "../components/camera/PhotoGrid";

interface IGalleryPageState {
  singlePhoto: {
    filePath: string | null;
    metadata: string | null;
  } | null;
  multiplePhotos: GalleryPhoto[] | null;
}

class GalleryPage extends React.Component<{}, IGalleryPageState> {
  constructor(props: {}) {
    super(props);
    this.state = {
      singlePhoto: null,
      multiplePhotos: null,
    };
  }

  choosePicture = async (): Promise<void> => {
    try {
      const options: ImageOptions = {
        quality: 100,
        resultType: CameraResultType.Uri,
        source: CameraSource.Photos,
        saveToGallery: false,
        allowEditing: false,
        webUseInput: true,
      };
      const photo = await Camera.getPhoto(options);
      this.setState({
        singlePhoto: {
          filePath: photo.path ?? photo.webPath ?? null,
          metadata: JSON.stringify(photo.exif, null, 2),
        },
        multiplePhotos: null,
      });
    } catch (e) {
      alert(`Failed to get picture with error:\n'${e}'`);
    }
  };

  pickPhotos = async (limit: number = 0): Promise<void> => {
    try {
      const options: GalleryImageOptions = {
        quality: 100,
        limit,
      };
      const photosResult = await Camera.pickImages(options);
      console.log("photos result", photosResult);
      this.setState({
        singlePhoto: null,
        multiplePhotos: photosResult.photos,
      });
    } catch (e) {
      alert(`Failed to get picture with error:\n'${e}'`);
    }
  };

  pickLimitedLibraryPhotos = async (): Promise<void> => {
    try {
      const res = await Camera.pickLimitedLibraryPhotos();
      console.log("res", res);
    } catch (e) {
      alert(`Failed to pick limited library photos with error:\n'${e}'`);
    }
  };

  getLimitedLibraryPhotos = async (): Promise<void> => {
    try {
      const res = await Camera.getLimitedLibraryPhotos();
      console.log("res", res);
    } catch (e) {
      alert(`Failed to get limited library photos with error:\n'${e}'`);
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
            <IonTitle>Gallery</IonTitle>
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
                    <IonButton
                      expand="block"
                      onClick={() => this.choosePicture()}
                    >
                      Choose Picture
                    </IonButton>
                    <IonButton expand="block" onClick={() => this.pickPhotos()}>
                      Pick Photos
                    </IonButton>
                    <IonButton
                      expand="block"
                      onClick={() => this.pickPhotos(3)}
                    >
                      Pick 3 Photos
                    </IonButton>
                    <IonButton
                      expand="block"
                      onClick={() => this.pickLimitedLibraryPhotos()}
                    >
                      pickLimitedLibraryPhotos
                    </IonButton>
                    <IonButton
                      expand="block"
                      onClick={() => this.getLimitedLibraryPhotos()}
                    >
                      getLimitedLibraryPhotos
                    </IonButton>
                  </div>
                </IonAccordion>
              </IonAccordionGroup>
            </IonCardContent>
          </IonCard>
          {this.state.singlePhoto !== null &&
            this.state.singlePhoto.filePath !== null && (
              <PhotoWithMetadata
                filePath={this.state.singlePhoto.filePath}
                metadata={this.state.singlePhoto.metadata}
              />
            )}
          {this.state.multiplePhotos !== null &&
            this.state.multiplePhotos.length > 0 && (
              <PhotoGrid photos={this.state.multiplePhotos} />
            )}
        </IonContent>
      </IonPage>
    );
  }
}

export default GalleryPage;
