import React from "react";
import { IonCard, IonCardContent, IonButton } from "@ionic/react";
import { Capacitor } from "@capacitor/core";
import { MediaMetadata } from "@capacitor/camera";

interface IPhotoWithMetadataProps {
  filePath: string;
  metadata?: MediaMetadata | string | null;
  saved?: boolean;
  onEdit?: (filePath: string) => void;
}

const PhotoWithMetadata: React.FC<IPhotoWithMetadataProps> = ({
  filePath,
  metadata,
  saved,
  onEdit,
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
          <img src={Capacitor.convertFileSrc(filePath)} alt="Captured" />
        </div>
        {onEdit && (
          <IonButton
            expand="block"
            color="tertiary"
            onClick={() => onEdit(filePath)}
            style={{ marginTop: "8px" }}
          >
            Edit This Photo
          </IonButton>
        )}
        {(metadata || saved !== undefined) && (
          <div>
            <pre>{formatMetadata(metadata, saved)}</pre>
          </div>
        )}
      </IonCardContent>
    </IonCard>
  );
};

export default PhotoWithMetadata;
