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
  MediaResult,
  MediaMetadata,
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

interface IGalleryPageState {
  singlePhoto: {
    filePath: string | null;
    metadata: MediaMetadata | string | null;
  } | null;
  multiplePhotos: MediaResult[] | null;
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

  // Adapter to convert GalleryPhoto[] to MediaResult[] for legacy methods
  private convertGalleryPhotosToMediaResults(photos: GalleryPhoto[]): MediaResult[] {
    return photos.map((photo) => ({
      type: 0, // MediaType.picture
      uri: photo.path,
      webPath: photo.webPath,
      saved: false,
      thumbnail: undefined,
      metadata: photo.exif
        ? {
            format: photo.format,
            resolution: '',
            exif: JSON.stringify(photo.exif),
          }
        : {
            format: photo.format,
            resolution: '',
          },
    }));
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

  handlePhotosResult = (results: MediaResult[]): void => {
    this.setState({
      singlePhoto: null,
      multiplePhotos: results,
    });

    results.forEach((result) => {
      MediaHistoryService.addMedia({
        mediaType: "photo",
        method: "chooseFromGallery",
        uri: result.uri,
        webPath: result.webPath,
        thumbnail: result.thumbnail,
        format: result.metadata?.format,
        size: result.metadata?.size,
        saved: result.saved,
        metadata: result.metadata,
      });
    });
  };

  // Handler for old methods that still return GalleryPhoto[]
  handleLegacyPhotosResult = (photos: GalleryPhoto[]): void => {
    const mediaResults = this.convertGalleryPhotosToMediaResults(photos);
    this.handlePhotosResult(mediaResults);
  };

  pickLimitedLibraryPhotos = async (): Promise<void> => {
    try {
      const res = await Camera.pickLimitedLibraryPhotos();
      const mediaResults = this.convertGalleryPhotosToMediaResults(res.photos);
      this.handlePhotosResult(mediaResults);
    } catch (e) {
      const error = e as any;
      const errorMessage = error.code ? `[${error.code}] ${error.message}` : error.message;
      alert(`Failed to pick limited library photos with error:\n${errorMessage}`);
    }
  };

  getLimitedLibraryPhotos = async (): Promise<void> => {
    try {
      const res = await Camera.getLimitedLibraryPhotos();
      const mediaResults = this.convertGalleryPhotosToMediaResults(res.photos);
      this.handlePhotosResult(mediaResults);
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
        uri: result.uri,
        webPath: result.webPath,
        thumbnail: result.thumbnail,
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
                      onPhotosResult={this.handleLegacyPhotosResult}
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

export default GalleryPage;
