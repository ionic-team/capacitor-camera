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
import { Camera } from "@capacitor/camera";
import VideoWithMetadata from "../components/camera/VideoWithMetadata";
import RecordVideoConfigurable from "../components/camera/RecordVideoConfigurable";

interface IRecordVideoPageState {
  filePath: string | null;
  metadata: string | null;
}

class RecordVideoPage extends React.Component<{}, IRecordVideoPageState> {
  constructor(props: {}) {
    super(props);
    this.state = {
      filePath: null,
      metadata: null,
    };
  }

  handleVideoResult = (result: {
    path: string;
    webPath: string;
    duration?: number;
    size: number;
    format: string;
    saved: boolean;
  }): void => {
    const metadata = {
      duration: result.duration,
      size: result.size,
      format: result.format,
      saved: result.saved,
    };

    this.setState({
      filePath: result.path ?? result.webPath,
      metadata: JSON.stringify(metadata, null, 2),
    });
  };

  playVideo = async (): Promise<void> => {
    if (!this.state.filePath) return;

    try {
      await Camera.playVideo({ videoURI: this.state.filePath });
    } catch (e) {
      alert(`Failed to play video with error:\n'${e}'`);
    }
  };

  clearVideo = (): void => {
    this.setState({
      filePath: null,
      metadata: null,
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
              />
            </>
          )}
        </IonContent>
      </IonPage>
    );
  }
}

export default RecordVideoPage;
