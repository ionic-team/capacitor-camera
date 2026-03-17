import {
  IonButton,
  IonCard,
  IonCardContent,
  IonCardHeader,
  IonCardTitle,
  IonItem,
  IonLabel,
  IonToggle,
} from "@ionic/react";
import React from "react";
import { Camera, MediaResult, EditURIPhotoOptions } from "@capacitor/camera";
import { Filesystem, Directory } from "@capacitor/filesystem";
import { Capacitor } from "@capacitor/core";
import { TestImage } from "./TestImageData";
import { MediaHistoryService } from "../../services/MediaHistoryService";

interface EditURIPhotoConfigurableProps {
  selectedImage: TestImage | null;
}

interface EditURIPhotoConfigurableState {
  savedFileUri: string | null;
  savedFileWebPath: string | null;
  editedPhoto: MediaResult | null;
  config: Omit<EditURIPhotoOptions, 'uri'>; // Config excludes 'uri' since it comes from saved file
  isLoading: boolean;
}

class EditURIPhotoConfigurable extends React.Component<
  EditURIPhotoConfigurableProps,
  EditURIPhotoConfigurableState
> {
  constructor(props: EditURIPhotoConfigurableProps) {
    super(props);
    // Initialize with API defaults from EditURIPhotoOptions
    this.state = {
      savedFileUri: null,
      savedFileWebPath: null,
      editedPhoto: null,
      config: {
        saveToGallery: false,
        includeMetadata: false,
      },
      isLoading: false,
    };
  }

  componentDidUpdate(prevProps: EditURIPhotoConfigurableProps): void {
    if (this.props.selectedImage !== prevProps.selectedImage) {
      this.setState({
        savedFileUri: null,
        savedFileWebPath: null,
        editedPhoto: null,
      });
    }
  }

  updateConfig = (field: keyof Omit<EditURIPhotoOptions, 'uri'>, value: boolean): void => {
    this.setState({
      config: { ...this.state.config, [field]: value },
    });
  };

  saveImageToFile = async (): Promise<void> => {
    if (!this.props.selectedImage) {
      alert("No image selected. Please select an image first.");
      return;
    }

    this.setState({ isLoading: true });

    try {
      const image = this.props.selectedImage;
      let base64Data: string;

      if (image.type === "base64") {
        base64Data = image.data;
      } else {
        // Fetch asset image and convert to base64
        const response = await fetch(image.data);
        const blob = await response.blob();
        const base64String = await new Promise<string>((resolve, reject) => {
          const reader = new FileReader();
          reader.onloadend = () => resolve(reader.result as string);
          reader.onerror = reject;
          reader.readAsDataURL(blob);
        });
        // Remove the data:image/...;base64, prefix
        base64Data = base64String.split(",")[1];
      }

      // Save to filesystem
      const fileName = `test-image-${Date.now()}.png`;
      const result = await Filesystem.writeFile({
        path: fileName,
        data: base64Data,
        directory: Directory.Cache,
      });

      // Get the file URI
      const fileUri = result.uri;
      const webPath = Capacitor.convertFileSrc(fileUri);

      this.setState({
        savedFileUri: fileUri,
        savedFileWebPath: webPath,
        editedPhoto: null,
        isLoading: false,
      });
    } catch (e) {
      const error = e as any;
      const errorMessage = error.code ? `[${error.code}] ${error.message}` : error.message;
      alert(`Failed to save image to file with error:\n${errorMessage}`);
      this.setState({ isLoading: false });
    }
  };

  editURIPhoto = async (): Promise<void> => {
    if (!this.state.savedFileUri) {
      alert("No file saved. Please save the image to file first.");
      return;
    }

    try {
      const result = await Camera.editURIPhoto({
        uri: this.state.savedFileUri,
        saveToGallery: this.state.config.saveToGallery,
        includeMetadata: this.state.config.includeMetadata,
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

  clearPhotos = (): void => {
    this.setState({
      savedFileUri: null,
      savedFileWebPath: null,
      editedPhoto: null,
    });
  };

  render(): React.ReactNode {
    const { savedFileUri, savedFileWebPath, editedPhoto, config, isLoading } = this.state;
    const { selectedImage } = this.props;

    return (
      <IonCard>
        <IonCardHeader>
          <IonCardTitle>Test editURIPhoto</IonCardTitle>
        </IonCardHeader>
        <IonCardContent>
          {selectedImage && (
            <div style={{ marginBottom: "8px", fontSize: "0.9em", color: "var(--ion-color-medium)" }}>
              Selected: {selectedImage.name}
            </div>
          )}

          <IonButton
            expand="block"
            onClick={this.saveImageToFile}
            disabled={!selectedImage || isLoading}
          >
            {isLoading ? "Saving..." : "Save Selected Image to File"}
          </IonButton>

          {savedFileUri && (
            <>
              <div style={{ marginTop: "16px" }}>
                <h4>Configuration:</h4>
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
              </div>

              <IonButton
                expand="block"
                color="secondary"
                onClick={this.editURIPhoto}
                style={{ marginTop: "16px" }}
              >
                Edit Photo (URI)
              </IonButton>

              <IonButton
                expand="block"
                color="danger"
                fill="outline"
                onClick={this.clearPhotos}
                style={{ marginTop: "8px" }}
              >
                Clear Photos
              </IonButton>

              <div style={{ marginTop: "16px" }}>
                <h4>Saved File:</h4>
                <img
                  src={savedFileWebPath || ""}
                  alt="Saved"
                  style={{ width: "100%", maxHeight: "300px", objectFit: "contain" }}
                />
                <p style={{ fontSize: "0.8em", wordBreak: "break-all" }}>
                  <strong>URI:</strong> {savedFileUri}
                </p>
              </div>
            </>
          )}

          {editedPhoto && (
            <div style={{ marginTop: "16px" }}>
              <h4>Edited Photo:</h4>
              <img
                src={editedPhoto.webPath}
                alt="Edited"
                style={{ width: "100%", maxHeight: "300px", objectFit: "contain" }}
              />
              <p>
                <strong>URI:</strong> {editedPhoto.uri}
              </p>
              {editedPhoto.metadata?.size && (
                <p>
                  <strong>Size:</strong> {editedPhoto.metadata.size} bytes
                </p>
              )}
              {editedPhoto.metadata?.format && (
                <p>
                  <strong>Format:</strong> {editedPhoto.metadata.format}
                </p>
              )}
              <p>
                <strong>Saved:</strong> {editedPhoto.saved ? "Yes" : "No"}
              </p>
            </div>
          )}
        </IonCardContent>
      </IonCard>
    );
  }
}

export default EditURIPhotoConfigurable;
