import { IonButton } from "@ionic/react";
import React from "react";

interface SimpleCameraButtonProps {
  label: string;
  onClick: () => Promise<void>;
}

const SimpleCameraButton: React.FC<SimpleCameraButtonProps> = ({
  label,
  onClick,
}) => {
  return (
    <IonButton expand="block" onClick={onClick}>
      {label}
    </IonButton>
  );
};

export default SimpleCameraButton;
