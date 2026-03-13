import {
  IonButtons,
  IonContent,
  IonHeader,
  IonPage,
  IonMenuButton,
  IonTitle,
  IonToolbar,
} from "@ionic/react";
import React, { useState } from "react";
import TestImageSelector from "../components/camera/TestImageSelector";
import EditPhotoConfigurable from "../components/camera/EditPhotoConfigurable";
import EditURIPhotoConfigurable from "../components/camera/EditURIPhotoConfigurable";
import { TestImage } from "../components/camera/TestImageData";

const EditPhotoPage: React.FC = () => {
  const [selectedImage, setSelectedImage] = useState<TestImage | null>(null);

  const handleImageSelected = (image: TestImage): void => {
    setSelectedImage(image);
  };

  return (
    <IonPage>
      <IonHeader>
        <IonToolbar>
          <IonButtons slot="start">
            <IonMenuButton />
          </IonButtons>
          <IonTitle>Edit Photo</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent>
        <div style={{ padding: "16px", fontSize: "0.9em", color: "var(--ion-color-medium)" }}>
          <p>
            This page tests Edit methods (<strong>editPhoto</strong> and <strong>editURIPhoto</strong>) in isolation using pre-loaded test images.
          </p>
          <p>
            To test Edit methods with Camera/Gallery workflows, visit the <strong>Take Picture</strong> or <strong>Gallery</strong> pages.
          </p>
        </div>
        <TestImageSelector onImageSelected={handleImageSelected} />
        <EditPhotoConfigurable selectedImage={selectedImage} />
        <EditURIPhotoConfigurable selectedImage={selectedImage} />
      </IonContent>
    </IonPage>
  );
};

export default EditPhotoPage;
