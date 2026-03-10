import React from "react";
import { IonGrid, IonRow, IonCol, IonImg } from "@ionic/react";
import { GalleryPhoto } from "@capacitor/camera";

interface IPhotoGridProps {
  photos: GalleryPhoto[];
}

const PhotoGrid: React.FC<IPhotoGridProps> = ({ photos }) => {
  return (
    <IonGrid>
      <IonRow>
        {photos.map((photo, index) => (
          <IonCol key={index} size="6">
            <IonImg src={photo.webPath} />
          </IonCol>
        ))}
      </IonRow>
    </IonGrid>
  );
};

export default PhotoGrid;
