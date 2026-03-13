import React from "react";
import { IonCard, IonCardContent, IonButton } from "@ionic/react";
import { Capacitor } from "@capacitor/core";

interface IPhotoWithMetadataProps {
  filePath: string;
  metadata?: string | null;
  onEdit?: (filePath: string) => void;
}

const PhotoWithMetadata: React.FC<IPhotoWithMetadataProps> = ({
  filePath,
  metadata,
  onEdit,
}) => {
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
        {metadata && (
          <div>
            <pre>{metadata}</pre>
          </div>
        )}
      </IonCardContent>
    </IonCard>
  );
};

export default PhotoWithMetadata;
