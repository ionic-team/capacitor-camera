import {
  IonButtons,
  IonContent,
  IonHeader,
  IonPage,
  IonMenuButton,
  IonTitle,
  IonToolbar,
  IonCard,
  IonCardContent,
  IonList,
  IonItem,
  IonLabel,
  IonBadge,
  useIonViewWillEnter,
} from "@ionic/react";
import React, { useState } from "react";
import {
  MediaHistoryService,
  MediaHistoryItem,
} from "../services/MediaHistoryService";
import { FileViewer } from "@capacitor/file-viewer";

const MediaHistoryPage: React.FC = () => {
  const [history, setHistory] = useState<MediaHistoryItem[]>([]);

  useIonViewWillEnter(() => {
    loadHistory();
  });

  const loadHistory = (): void => {
    const history = MediaHistoryService.getAllMedia();
    setHistory(history);
  };

  const openFile = async (item: MediaHistoryItem): Promise<void> => {
    try {
      const filePath = item.uri ?? item.path ?? '';
      if (!filePath) {
        alert('No file path available for this item');
        return;
      }
      await FileViewer.openDocumentFromLocalPath({ path: filePath });
    } catch (e) {
      const error = e as any;
      const errorMessage = error.code ? `[${error.code}] ${error.message}` : error.message;
      alert(`Failed to open file with error:\n${errorMessage}`);
    }
  };

  const formatDate = (timestamp: number): string => {
    const date = new Date(timestamp);
    const now = new Date();
    const isToday = date.toDateString() === now.toDateString();

    const timeString = date.toLocaleTimeString("en-US", {
      hour: "numeric",
      minute: "2-digit",
      hour12: true,
    });

    if (isToday) {
      return `Today, ${timeString}`;
    }

    const dateString = date.toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
    });

    return `${dateString}, ${timeString}`;
  };

  const formatSize = (bytes?: number): string => {
    if (!bytes) {
      return "N/A";
    }

    const kb = bytes / 1024;
    if (kb < 1024) {
      return `${kb.toFixed(1)} KB`;
    }

    const mb = kb / 1024;
    return `${mb.toFixed(1)} MB`;
  };

  const formatDuration = (seconds?: number): string => {
    if (!seconds) {
      return "N/A";
    }

    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);

    if (mins > 0) {
      return `${mins}m ${secs}s`;
    }

    return `${secs}s`;
  };

  return (
    <IonPage>
      <IonHeader>
        <IonToolbar>
          <IonButtons slot="start">
            <IonMenuButton />
          </IonButtons>
          <IonTitle>Media History</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent>
        <IonCard>
          <IonCardContent>
            <p>
              This page displays a chronological list of all media operations
              performed using the Camera plugin. History is stored in
              localStorage and persists across sessions.
            </p>
            <p>Click a media item to open them.</p>
          </IonCardContent>
        </IonCard>

        {history.length === 0 ? (
          <IonCard>
            <IonCardContent>
              <p style={{ textAlign: "center", color: "#666" }}>
                No media history yet. Capture or select photos/videos to see
                them here.
              </p>
            </IonCardContent>
          </IonCard>
        ) : (
          <IonCard>
            <IonCardContent>
              <IonList>
                {history.map((item) => (
                  <IonItem
                    key={item.id}
                    lines="full"
                    button={true}
                    onClick={() => openFile(item)}
                  >
                    <IonLabel>
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          marginBottom: "8px",
                        }}
                      >
                        <IonBadge
                          color={item.mediaType === "photo" ? "primary" : "secondary"}
                          style={{ marginRight: "8px" }}
                        >
                          {item.mediaType === "photo" ? "📷" : "🎥"}{" "}
                          {item.mediaType}
                        </IonBadge>
                        <strong>{item.method}</strong>
                      </div>
                      <p style={{ margin: "4px 0", color: "#666" }}>
                        {formatDate(item.timestamp)}
                      </p>
                      <p style={{ margin: "4px 0", fontSize: "0.9em" }}>
                        Format: {item.metadata?.format ?? item.format ?? "N/A"} | Size:{" "}
                        {formatSize(item.metadata?.size ?? item.size)}
                        {item.mediaType === "video" &&
                          ` | Duration: ${formatDuration(item.metadata?.duration ?? item.duration)}`}
                        {item.saved !== undefined &&
                          ` | Saved: ${item.saved ? "Yes" : "No"}`}
                      </p>
                      <p
                        style={{
                          margin: "4px 0",
                          fontSize: "0.8em",
                          color: "#999",
                          wordBreak: "break-all",
                        }}
                      >
                        {item.uri ?? item.path ?? "N/A"}
                      </p>
                    </IonLabel>
                  </IonItem>
                ))}
              </IonList>
            </IonCardContent>
          </IonCard>
        )}

        <div style={{ height: "80px" }} />
      </IonContent>
    </IonPage>
  );
};

export default MediaHistoryPage;
