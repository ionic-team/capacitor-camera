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
  IonButton,
} from "@ionic/react";
import React from "react";
import {
  Camera,
  CameraSource,
  GalleryPhoto,
} from "@capacitor/camera";
import PhotoWithMetadata from "../components/camera/PhotoWithMetadata";
import MediaCarousel from "../components/camera/MediaCarousel";
import ChooseFromGalleryConfigurable from "../components/camera/ChooseFromGalleryConfigurable";
import {
  GetPhotoConfigurable,
  PickImagesConfigurable,
} from "../components/camera/old-methods";
import { MediaHistoryService } from "../services/MediaHistoryService";

interface MediaResult {
  path: string;
  webPath: string;
  duration?: number;
  size: number;
  format: string;
  saved: boolean;
}

interface IGalleryPageState {
  singlePhoto: {
    filePath: string | null;
    metadata: string | null;
  } | null;
  multiplePhotos: GalleryPhoto[] | null;
  editedPhoto: MediaResult | null;
}

class GalleryPage extends React.Component<{}, IGalleryPageState> {
  constructor(props: {}) {
    super(props);
    this.state = {
      singlePhoto: null,
      multiplePhotos: null,
      editedPhoto: null,
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

    if (result.path || result.webPath) {
      MediaHistoryService.addMedia({
        mediaType: "photo",
        method: "getPhoto",
        path: result.path || "",
        webPath: result.webPath || "",
      });
    }
  };

  handlePhotosResult = (photos: GalleryPhoto[]): void => {
    this.setState({
      singlePhoto: null,
      multiplePhotos: photos,
    });

    photos.forEach((photo) => {
      MediaHistoryService.addMedia({
        mediaType: "photo",
        method: "chooseFromGallery",
        path: photo.path || "",
        webPath: photo.webPath,
        format: photo.format,
      });
    });
  };

  pickLimitedLibraryPhotos = async (): Promise<void> => {
    try {
      const res = await Camera.pickLimitedLibraryPhotos();
      this.handlePhotosResult(res.photos);
    } catch (e) {
      const error = e as any;
      const errorMessage = error.code ? `[${error.code}] ${error.message}` : error.message;
      alert(`Failed to pick limited library photos with error:\n${errorMessage}`);
    }
  };

  getLimitedLibraryPhotos = async (): Promise<void> => {
    try {
      const res = await Camera.getLimitedLibraryPhotos();
      this.handlePhotosResult(res.photos);
    } catch (e) {
      const error = e as any;
      const errorMessage = error.code ? `[${error.code}] ${error.message}` : error.message;
      alert(`Failed to get limited library photos with error:\n${errorMessage}`);
    }
  };

  clearMedia = (): void => {
    this.setState({
      singlePhoto: null,
      multiplePhotos: null,
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
            <IonTitle>Gallery</IonTitle>
          </IonToolbar>
        </IonHeader>
        <IonContent>
          {/* Placeholder for future new methods */}
          <IonCard>
            <IonCardContent>
              <ChooseFromGalleryConfigurable
                onMediaResult={this.handlePhotosResult}
              />

              {/* Parameter-less methods */}
              <div style={{ marginTop: "16px" }}>
                <IonButton
                  expand="block"
                  onClick={this.pickLimitedLibraryPhotos}
                >
                  (iOS only) pickLimitedLibraryPhotos
                </IonButton>
                <IonButton
                  expand="block"
                  onClick={this.getLimitedLibraryPhotos}
                >
                  (iOS only) getLimitedLibraryPhotos
                </IonButton>
              </div>
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
                  </div>
                </IonAccordion>
              </IonAccordionGroup>
            </IonCardContent>
          </IonCard>
          {(this.state.singlePhoto !== null ||
            (this.state.multiplePhotos !== null &&
              this.state.multiplePhotos.length > 0)) && (
            <IonButton
              expand="block"
              color="danger"
              fill="outline"
              onClick={this.clearMedia}
              style={{ margin: "0 16px 16px 16px" }}
            >
              Clear Media
            </IonButton>
          )}
          {this.state.singlePhoto !== null &&
            this.state.singlePhoto.filePath !== null && (
              <PhotoWithMetadata
                filePath={this.state.singlePhoto.filePath}
                metadata={this.state.singlePhoto.metadata}
                onEdit={this.handleEditPhoto}
              />
            )}
          {this.state.multiplePhotos !== null &&
            this.state.multiplePhotos.length > 0 && (
              <MediaCarousel
                media={this.state.multiplePhotos}
                onEditPhoto={this.handleEditPhoto}
              />
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

export default GalleryPage;
