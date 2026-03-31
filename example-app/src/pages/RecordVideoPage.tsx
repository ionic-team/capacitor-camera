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
import { Camera, MediaResult, MediaMetadata } from "@capacitor/camera";
import VideoWithMetadata from "../components/camera/VideoWithMetadata";
import RecordVideoConfigurable from "../components/camera/RecordVideoConfigurable";
import { MediaHistoryService } from "../services/MediaHistoryService";

interface IRecordVideoPageState {
  filePath: string | null;
  metadata: MediaMetadata | string | null;
  saved: boolean | null;
  thumbnail: string | null;
}

class RecordVideoPage extends React.Component<{}, IRecordVideoPageState> {
  constructor(props: {}) {
    super(props);
    this.state = {
      filePath: null,
      metadata: null,
      saved: null,
      thumbnail: null,
    };
  }

  handleVideoResult = (result: MediaResult): void => {
    this.setState({
      filePath: result.uri ?? result.webPath ?? '',
      metadata: result.metadata ?? null,
      saved: result.saved,
      thumbnail: result.thumbnail ?? null,
    });

    MediaHistoryService.addMedia({
      mediaType: "video",
      method: "recordVideo",
      uri: result.uri,
      webPath: result.webPath,
      format: result.metadata?.format,
      size: result.metadata?.size,
      duration: result.metadata?.duration,
      saved: result.saved,
      metadata: result.metadata,
    });
  };

  playVideo = async (): Promise<void> => {
    if (!this.state.filePath) return;

    try {
      await Camera.playVideo({ uri: this.state.filePath });
    } catch (e) {
      const error = e as any;
      const errorMessage = error.code ? `[${error.code}] ${error.message}` : error.message;
      alert(`Failed to play video with error:\n${errorMessage}`);
    }
  };

  clearVideo = (): void => {
    this.setState({
      filePath: null,
      metadata: null,
      saved: null,
      thumbnail: null,
    });
  };

  render() {
    return (
      <IonPage>
        <IonHeader>
          <IonToolbar>
            <IonButtons slot="start">
              <IonMenuButton />
            </IonButtons>
            <IonTitle>Record Video</IonTitle>
          </IonToolbar>
        </IonHeader>
        <IonContent>
          <IonCard>
            <IonCardContent>
              <RecordVideoConfigurable onVideoResult={this.handleVideoResult} />
            </IonCardContent>
          </IonCard>
          {this.state.filePath !== null && (
            <>
              <IonCard>
                <IonCardContent>
                  <IonButton expand="block" onClick={() => this.playVideo()}>
                    Play Video
                  </IonButton>
                </IonCardContent>
              </IonCard>
              <IonButton
                expand="block"
                color="danger"
                fill="outline"
                onClick={this.clearVideo}
                style={{ margin: "0 16px 16px 16px" }}
              >
                Clear Video
              </IonButton>
              <VideoWithMetadata
                filePath={this.state.filePath}
                metadata={this.state.metadata}
                saved={this.state.saved ?? undefined}
                thumbnail={this.state.thumbnail ?? undefined}
              />
            </>
          )}
          <div style={{ height: '80px' }} />
        </IonContent>
      </IonPage>
    );
  }
}

export default RecordVideoPage;
