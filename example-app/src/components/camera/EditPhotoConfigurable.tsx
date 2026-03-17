import {
  IonButton,
  IonCard,
  IonCardContent,
  IonCardHeader,
  IonCardTitle,
} from "@ionic/react";
import React from "react";
import {
  Camera,
  EditPhotoResult,
} from "@capacitor/camera";
import { TestImage } from "./TestImageData";

interface EditPhotoConfigurableProps {
  selectedImage: TestImage | null;
}

interface EditPhotoConfigurableState {
  base64Image: string | null;
  editedResult: EditPhotoResult | null;
  isLoading: boolean;
}

class EditPhotoConfigurable extends React.Component<
  EditPhotoConfigurableProps,
  EditPhotoConfigurableState
> {
  constructor(props: EditPhotoConfigurableProps) {
    super(props);
    this.state = {
      base64Image: null,
      editedResult: null,
      isLoading: false,
    };
  }

  componentDidUpdate(prevProps: EditPhotoConfigurableProps): void {
    if (this.props.selectedImage !== prevProps.selectedImage) {
      this.setState({
        base64Image: null,
        editedResult: null,
      });
    }
  }

  loadImageAsBase64 = async (): Promise<void> => {
    if (!this.props.selectedImage) {
      alert("No image selected. Please select an image first.");
      return;
    }

    this.setState({ isLoading: true });

    try {
      const image = this.props.selectedImage;

      if (image.type === "base64") {
        this.setState({
          base64Image: image.data,
          editedResult: null,
          isLoading: false,
        });
      } else {
        // Asset image - fetch and convert to base64
        const response = await fetch(image.data);
        const blob = await response.blob();
        const reader = new FileReader();

        reader.onloadend = () => {
          const base64String = reader.result as string;
          // Remove the data:image/...;base64, prefix
          const base64Data = base64String.split(",")[1];
          this.setState({
            base64Image: base64Data,
            editedResult: null,
            isLoading: false,
          });
        };

        reader.onerror = () => {
          alert("Failed to convert image to base64");
          this.setState({ isLoading: false });
        };

        reader.readAsDataURL(blob);
      }
    } catch (e) {
      const error = e as any;
      const errorMessage = error.code ? `[${error.code}] ${error.message}` : error.message;
      alert(`Failed to load image with error:\n${errorMessage}`);
      this.setState({ isLoading: false });
    }
  };

  editPhoto = async (): Promise<void> => {
    if (!this.state.base64Image) {
      alert("No photo loaded. Please load an image first.");
      return;
    }

    try {
      const result = await Camera.editPhoto({
        base64: this.state.base64Image,
      });
      this.setState({ editedResult: result });
    } catch (e) {
      const error = e as any;
      const errorMessage = error.code ? `[${error.code}] ${error.message}` : error.message;
      alert(`Failed to edit photo with error:\n${errorMessage}`);
    }
  };

  clearPhotos = (): void => {
    this.setState({
      base64Image: null,
      editedResult: null,
    });
  };

  render(): React.ReactNode {
    const { base64Image, editedResult, isLoading } = this.state;
    const { selectedImage } = this.props;

    return (
      <IonCard>
        <IonCardHeader>
          <IonCardTitle>Test editPhoto (base64)</IonCardTitle>
        </IonCardHeader>
        <IonCardContent>
          {selectedImage && (
            <div style={{ marginBottom: "8px", fontSize: "0.9em", color: "var(--ion-color-medium)" }}>
              Selected: {selectedImage.name}
            </div>
          )}

          <IonButton
            expand="block"
            onClick={this.loadImageAsBase64}
            disabled={!selectedImage || isLoading}
          >
            {isLoading ? "Loading..." : "Load Selected Image as Base64"}
          </IonButton>

          {base64Image && (
            <>
              <IonButton
                expand="block"
                color="secondary"
                onClick={this.editPhoto}
                style={{ marginTop: "8px" }}
              >
                Edit Photo (base64)
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
                <h4>Original Photo:</h4>
                <img
                  src={`data:image/png;base64,${base64Image}`}
                  alt="Original"
                  style={{ width: "100%", maxHeight: "300px", objectFit: "contain" }}
                />
              </div>
            </>
          )}

          {editedResult && (
            <div style={{ marginTop: "16px" }}>
              <h4>Edited Photo:</h4>
              <p>
                <strong>Format:</strong> {editedResult.format}
              </p>
              <img
                src={`data:image/${editedResult.format};base64,${editedResult.base64String}`}
                alt="Edited"
                style={{ width: "100%", maxHeight: "300px", objectFit: "contain" }}
              />
            </div>
          )}
        </IonCardContent>
      </IonCard>
    );
  }
}

export default EditPhotoConfigurable;
