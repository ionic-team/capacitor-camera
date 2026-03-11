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
import {
  Camera,
  CameraSource,
  GalleryPhoto,
} from "@capacitor/camera";
import PhotoWithMetadata from "../components/camera/PhotoWithMetadata";
import PhotoGrid from "../components/camera/PhotoGrid";
import {
  GetPhotoConfigurable,
  PickImagesConfigurable,
  SimpleCameraButton,
} from "../components/camera/old-methods";

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

  handlePhotoResult = (result: {
    path?: string;
    webPath?: string;
    exif?: any;
  }): void => {
    this.setState({
      singlePhoto: {
        filePath: result.path ?? result.webPath ?? null,
        metadata: JSON.stringify(result.exif, null, 2),
      },
      multiplePhotos: null,
    });
  };

  handlePhotosResult = (photos: GalleryPhoto[]): void => {
    this.setState({
      singlePhoto: null,
      multiplePhotos: photos,
    });
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
                    <GetPhotoConfigurable
                      defaultSource={CameraSource.Photos}
                      defaultWebUseInput={true}
                      onPhotoResult={this.handlePhotoResult}
                    />

                    <PickImagesConfigurable
                      onPhotosResult={this.handlePhotosResult}
                    />

                    {/* Parameter-less methods */}
                    <div style={{ marginTop: "16px" }}>
                      <SimpleCameraButton
                        label="pickLimitedLibraryPhotos"
                        onClick={this.pickLimitedLibraryPhotos}
                      />
                      <SimpleCameraButton
                        label="getLimitedLibraryPhotos"
                        onClick={this.getLimitedLibraryPhotos}
                      />
                    </div>
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
