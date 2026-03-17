import React from "react";
import { IonCard, IonCardContent } from "@ionic/react";
import { Capacitor } from "@capacitor/core";

interface IVideoWithMetadataProps {
  filePath: string;
  metadata?: string | null;
}

const VideoWithMetadata: React.FC<IVideoWithMetadataProps> = ({
  filePath,
  metadata,
}) => {
  return (
    <IonCard>
      <IonCardContent>
        <div>
          <video
            src={Capacitor.convertFileSrc(filePath)}
            controls
            style={{ width: "100%", maxWidth: "100%" }}
          />
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

export default VideoWithMetadata;
