import React from "react";
import { IonCard, IonCardContent } from "@ionic/react";
import { Capacitor } from "@capacitor/core";
import { MediaMetadata } from "@capacitor/camera";

interface IVideoWithMetadataProps {
  filePath: string;
  metadata?: MediaMetadata | string | null;
  saved?: boolean;
  thumbnail?: string;
}

const VideoWithMetadata: React.FC<IVideoWithMetadataProps> = ({
  filePath,
  metadata,
  saved,
  thumbnail,
}) => {
  const formatMetadata = (
    meta: MediaMetadata | string | null | undefined,
    savedToGallery?: boolean
  ): string => {
    const parts: string[] = [];

    if (meta) {
      // If it's already a string (legacy), add it as-is
      if (typeof meta === "string") {
        parts.push(meta);
      } else {
        // Format MediaMetadata object
        if (meta.duration !== undefined) {
          const mins = Math.floor(meta.duration / 60);
          const secs = Math.floor(meta.duration % 60);
          parts.push(`Duration: ${mins > 0 ? `${mins}m ${secs}s` : `${secs}s`}`);
        }
        if (meta.size !== undefined) {
          const sizeKB = (meta.size / 1024).toFixed(1);
          const sizeMB = (meta.size / (1024 * 1024)).toFixed(2);
          parts.push(`Size: ${meta.size < 1024 * 1024 ? sizeKB + " KB" : sizeMB + " MB"}`);
        }
        if (meta.format) parts.push(`Format: ${meta.format}`);
        if (meta.resolution) parts.push(`Resolution: ${meta.resolution}`);
        if (meta.creationDate) {
          const date = new Date(meta.creationDate);
          parts.push(`Created: ${date.toLocaleString()}`);
        }
        if (meta.exif) {
          parts.push(`\nEXIF Data:\n${JSON.stringify(meta.exif, null, 2)}`);
        }
      }
    }

    // Always add saved status if defined, even when metadata is undefined
    if (savedToGallery !== undefined) {
      parts.push(`Saved to Gallery: ${savedToGallery ? "Yes" : "No"}`);
    }

    return parts.join("\n");
  };

  return (
    <IonCard>
      <IonCardContent>
        <div>
          <video
            src={Capacitor.convertFileSrc(filePath)}
            poster={thumbnail ? `data:image/jpeg;base64,${thumbnail}` : undefined}
            controls
            style={{ width: "100%", maxWidth: "100%" }}
          />
        </div>
        {(metadata || saved !== undefined) && (
          <div>
            <pre>{formatMetadata(metadata, saved)}</pre>
          </div>
        )}
      </IonCardContent>
    </IonCard>
  );
};

export default VideoWithMetadata;
