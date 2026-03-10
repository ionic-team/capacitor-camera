import React from "react";
import { IonCard, IonCardContent } from "@ionic/react";
import { Capacitor } from "@capacitor/core";

interface IPhotoWithMetadataProps {
  filePath: string;
  metadata?: string | null;
}

const PhotoWithMetadata: React.FC<IPhotoWithMetadataProps> = ({
  filePath,
  metadata,
}) => {
  return (
    <IonCard>
      <IonCardContent>
        <div>
          <img src={Capacitor.convertFileSrc(filePath)} alt="Captured" />
        </div>
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
