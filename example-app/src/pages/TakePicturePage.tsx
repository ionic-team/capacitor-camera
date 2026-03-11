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
  IonInput,
  IonSelect,
  IonSelectOption,
  IonToggle,
} from "@ionic/react";
import React from "react";
import {
  Camera,
  ImageOptions,
  CameraSource,
  CameraResultType,
  CameraDirection,
} from "@capacitor/camera";
import PhotoWithMetadata from "../components/camera/PhotoWithMetadata";

interface ITakePicturePageState {
  filePath: string | null;
  metadata: string | null;
  getPhotoConfig: {
    quality: number;
    allowEditing: boolean;
    resultType: CameraResultType;
    source: CameraSource;
    saveToGallery: boolean;
    width: number | undefined;
    height: number | undefined;
    correctOrientation: boolean;
    direction: CameraDirection;
    presentationStyle: 'fullscreen' | 'popover';
    webUseInput: boolean;
    promptLabelHeader: string;
    promptLabelCancel: string;
    promptLabelPhoto: string;
    promptLabelPicture: string;
  };
}

class TakePicturePage extends React.Component<{}, ITakePicturePageState> {
  constructor(props: {}) {
    super(props);
    this.state = {
      filePath: null,
      metadata: null,
      getPhotoConfig: {
        quality: 100,
        allowEditing: false,
        resultType: CameraResultType.Uri,
        source: CameraSource.Prompt,
        saveToGallery: false,
        width: undefined,
        height: undefined,
        correctOrientation: true,
        direction: CameraDirection.Rear,
        presentationStyle: 'fullscreen',
        webUseInput: false,
        promptLabelHeader: 'Photo',
        promptLabelCancel: 'Cancel',
        promptLabelPhoto: 'From Photos',
        promptLabelPicture: 'Take Picture',
      },
    };
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

  updateGetPhotoConfig = (
    field: keyof typeof this.state.getPhotoConfig,
    value: any
  ): void => {
    this.setState({
      getPhotoConfig: { ...this.state.getPhotoConfig, [field]: value },
    });
  };

  executeGetPhotoWithConfig = async (): Promise<void> => {
    try {
      const config = this.state.getPhotoConfig;
      const options: ImageOptions = {
        quality: config.quality,
        allowEditing: config.allowEditing,
        resultType: config.resultType,
        source: config.source,
        saveToGallery: config.saveToGallery,
        width: config.width,
        height: config.height,
        correctOrientation: config.correctOrientation,
        direction: config.direction,
        presentationStyle: config.presentationStyle,
        webUseInput: config.webUseInput,
        promptLabelHeader: config.promptLabelHeader,
        promptLabelCancel: config.promptLabelCancel,
        promptLabelPhoto: config.promptLabelPhoto,
        promptLabelPicture: config.promptLabelPicture,
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
                    {/* getPhoto method */}
                    <IonButton
                      expand="block"
                      onClick={() => this.addPhoto(CameraSource.Prompt)}
                    >
                      getPhoto (Default)
                    </IonButton>

                    <IonAccordionGroup>
                      <IonAccordion value="getPhoto">
                        <IonItem slot="header" lines="none">
                          <IonLabel style={{ fontSize: "0.9em", color: "var(--ion-color-medium)" }}>
                            Configure
                          </IonLabel>
                        </IonItem>
                        <div slot="content" style={{ padding: "16px" }}>
                          {/* Core settings */}
                          <IonItem>
                            <IonLabel position="stacked">Quality (0-100)</IonLabel>
                            <IonInput
                              type="number"
                              min="0"
                              max="100"
                              value={this.state.getPhotoConfig.quality}
                              onIonChange={(e) =>
                                this.updateGetPhotoConfig(
                                  "quality",
                                  parseInt(e.detail.value!) || 100
                                )
                              }
                            />
                          </IonItem>

                          <IonItem>
                            <IonLabel position="stacked">Result Type</IonLabel>
                            <IonSelect
                              value={this.state.getPhotoConfig.resultType}
                              onIonChange={(e) =>
                                this.updateGetPhotoConfig("resultType", e.detail.value)
                              }
                            >
                              <IonSelectOption value={CameraResultType.Uri}>
                                Uri
                              </IonSelectOption>
                              <IonSelectOption value={CameraResultType.Base64}>
                                Base64
                              </IonSelectOption>
                              <IonSelectOption value={CameraResultType.DataUrl}>
                                DataUrl
                              </IonSelectOption>
                            </IonSelect>
                          </IonItem>

                          <IonItem>
                            <IonLabel position="stacked">Source</IonLabel>
                            <IonSelect
                              value={this.state.getPhotoConfig.source}
                              onIonChange={(e) =>
                                this.updateGetPhotoConfig("source", e.detail.value)
                              }
                            >
                              <IonSelectOption value={CameraSource.Prompt}>
                                Prompt
                              </IonSelectOption>
                              <IonSelectOption value={CameraSource.Camera}>
                                Camera
                              </IonSelectOption>
                              <IonSelectOption value={CameraSource.Photos}>
                                Photos
                              </IonSelectOption>
                            </IonSelect>
                          </IonItem>

                          {/* Platform-specific */}
                          <IonItem>
                            <IonLabel position="stacked">Direction (iOS)</IonLabel>
                            <IonSelect
                              value={this.state.getPhotoConfig.direction}
                              onIonChange={(e) =>
                                this.updateGetPhotoConfig("direction", e.detail.value)
                              }
                            >
                              <IonSelectOption value={CameraDirection.Rear}>
                                Rear
                              </IonSelectOption>
                              <IonSelectOption value={CameraDirection.Front}>
                                Front
                              </IonSelectOption>
                            </IonSelect>
                          </IonItem>

                          <IonItem>
                            <IonLabel position="stacked">
                              Presentation Style (iOS)
                            </IonLabel>
                            <IonSelect
                              value={this.state.getPhotoConfig.presentationStyle}
                              onIonChange={(e) =>
                                this.updateGetPhotoConfig(
                                  "presentationStyle",
                                  e.detail.value
                                )
                              }
                            >
                              <IonSelectOption value="fullscreen">
                                Fullscreen
                              </IonSelectOption>
                              <IonSelectOption value="popover">
                                Popover
                              </IonSelectOption>
                            </IonSelect>
                          </IonItem>

                          {/* Dimensions */}
                          <IonItem>
                            <IonLabel position="stacked">
                              Width (optional)
                            </IonLabel>
                            <IonInput
                              type="number"
                              placeholder="Leave empty for no constraint"
                              value={this.state.getPhotoConfig.width ?? ""}
                              onIonChange={(e) =>
                                this.updateGetPhotoConfig(
                                  "width",
                                  e.detail.value ? parseInt(e.detail.value) : undefined
                                )
                              }
                            />
                          </IonItem>

                          <IonItem>
                            <IonLabel position="stacked">
                              Height (optional)
                            </IonLabel>
                            <IonInput
                              type="number"
                              placeholder="Leave empty for no constraint"
                              value={this.state.getPhotoConfig.height ?? ""}
                              onIonChange={(e) =>
                                this.updateGetPhotoConfig(
                                  "height",
                                  e.detail.value ? parseInt(e.detail.value) : undefined
                                )
                              }
                            />
                          </IonItem>

                          {/* Boolean toggles */}
                          <IonItem>
                            <IonLabel>Allow Editing</IonLabel>
                            <IonToggle
                              checked={this.state.getPhotoConfig.allowEditing}
                              onIonChange={(e) =>
                                this.updateGetPhotoConfig(
                                  "allowEditing",
                                  e.detail.checked
                                )
                              }
                            />
                          </IonItem>

                          <IonItem>
                            <IonLabel>Save to Gallery</IonLabel>
                            <IonToggle
                              checked={this.state.getPhotoConfig.saveToGallery}
                              onIonChange={(e) =>
                                this.updateGetPhotoConfig(
                                  "saveToGallery",
                                  e.detail.checked
                                )
                              }
                            />
                          </IonItem>

                          <IonItem>
                            <IonLabel>Correct Orientation</IonLabel>
                            <IonToggle
                              checked={this.state.getPhotoConfig.correctOrientation}
                              onIonChange={(e) =>
                                this.updateGetPhotoConfig(
                                  "correctOrientation",
                                  e.detail.checked
                                )
                              }
                            />
                          </IonItem>

                          <IonItem>
                            <IonLabel>Web Use Input</IonLabel>
                            <IonToggle
                              checked={this.state.getPhotoConfig.webUseInput}
                              onIonChange={(e) =>
                                this.updateGetPhotoConfig(
                                  "webUseInput",
                                  e.detail.checked
                                )
                              }
                            />
                          </IonItem>

                          {/* Text customizations */}
                          <IonItem>
                            <IonLabel position="stacked">Prompt Label Header</IonLabel>
                            <IonInput
                              type="text"
                              value={this.state.getPhotoConfig.promptLabelHeader}
                              onIonChange={(e) =>
                                this.updateGetPhotoConfig(
                                  "promptLabelHeader",
                                  e.detail.value ?? "Photo"
                                )
                              }
                            />
                          </IonItem>

                          <IonItem>
                            <IonLabel position="stacked">Prompt Label Cancel</IonLabel>
                            <IonInput
                              type="text"
                              value={this.state.getPhotoConfig.promptLabelCancel}
                              onIonChange={(e) =>
                                this.updateGetPhotoConfig(
                                  "promptLabelCancel",
                                  e.detail.value ?? "Cancel"
                                )
                              }
                            />
                          </IonItem>

                          <IonItem>
                            <IonLabel position="stacked">Prompt Label Photo</IonLabel>
                            <IonInput
                              type="text"
                              value={this.state.getPhotoConfig.promptLabelPhoto}
                              onIonChange={(e) =>
                                this.updateGetPhotoConfig(
                                  "promptLabelPhoto",
                                  e.detail.value ?? "From Photos"
                                )
                              }
                            />
                          </IonItem>

                          <IonItem>
                            <IonLabel position="stacked">Prompt Label Picture</IonLabel>
                            <IonInput
                              type="text"
                              value={this.state.getPhotoConfig.promptLabelPicture}
                              onIonChange={(e) =>
                                this.updateGetPhotoConfig(
                                  "promptLabelPicture",
                                  e.detail.value ?? "Take Picture"
                                )
                              }
                            />
                          </IonItem>

                          <IonButton
                            expand="block"
                            color="primary"
                            style={{ marginTop: "16px" }}
                            onClick={() => this.executeGetPhotoWithConfig()}
                          >
                            Execute getPhoto with Configuration
                          </IonButton>
                        </div>
                      </IonAccordion>
                    </IonAccordionGroup>
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
