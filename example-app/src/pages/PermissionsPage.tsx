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
  IonItem,
  IonLabel,
  IonBadge,
  IonList,
} from "@ionic/react";
import React from "react";
import {
  Camera,
  CameraPluginPermissions,
  CameraPermissionState,
} from "@capacitor/camera";

interface IPermissionsPageState {
  cameraPermission: CameraPermissionState | null;
  photosPermission: CameraPermissionState | null;
}

class PermissionsPage extends React.Component<{}, IPermissionsPageState> {
  constructor(props: {}) {
    super(props);
    this.state = {
      cameraPermission: null,
      photosPermission: null,
    };
  }

  async componentDidMount(): Promise<void> {
    await this.loadPermissions();
  }

  loadPermissions = async (): Promise<void> => {
    try {
      const permissionStates = await Camera.checkPermissions();
      this.setState({
        cameraPermission: permissionStates.camera,
        photosPermission: permissionStates.photos,
      });
    } catch (e) {
      const error = e as any;
      const errorMessage = error.code ? `[${error.code}] ${error.message}` : error.message;
      alert(`Failed to check permissions with error:\n${errorMessage}`);
    }
  };

  requestPermissions = async (
    permissions?: CameraPluginPermissions,
  ): Promise<void> => {
    try {
      const permissionStates = await Camera.requestPermissions(permissions);
      this.setState({
        cameraPermission: permissionStates.camera,
        photosPermission: permissionStates.photos,
      });
    } catch (e) {
      const error = e as any;
      const errorMessage = error.code ? `[${error.code}] ${error.message}` : error.message;
      alert(`Failed to request permissions with error:\n${errorMessage}`);
    }
  };

  getPermissionColor = (permission: CameraPermissionState | null): string => {
    if (permission === "granted") {
      return "success";
    }
    if (permission === "denied") {
      return "danger";
    }
    if (permission === "prompt" || permission === "limited") {
      return "warning";
    }
    return "medium";
  };

  render() {
    return (
      <IonPage>
        <IonHeader>
          <IonToolbar>
            <IonButtons slot="start">
              <IonMenuButton />
            </IonButtons>
            <IonTitle>Permissions</IonTitle>
          </IonToolbar>
        </IonHeader>
        <IonContent>
          <IonCard>
            <IonCardContent>
              <IonList>
                <IonItem>
                  <IonLabel>Camera Permission</IonLabel>
                  <IonBadge
                    color={this.getPermissionColor(this.state.cameraPermission)}
                  >
                    {this.state.cameraPermission ?? "unknown"}
                  </IonBadge>
                </IonItem>
                <IonItem>
                  <IonLabel>Photos Permission</IonLabel>
                  <IonBadge
                    color={this.getPermissionColor(this.state.photosPermission)}
                  >
                    {this.state.photosPermission ?? "unknown"}
                  </IonBadge>
                </IonItem>
              </IonList>
            </IonCardContent>
          </IonCard>
          <IonCard>
            <IonCardContent>
              <IonButton
                expand="block"
                onClick={() =>
                  this.requestPermissions({ permissions: ["camera"] })
                }
              >
                Request Camera Permissions
              </IonButton>
              <IonButton
                expand="block"
                onClick={() =>
                  this.requestPermissions({ permissions: ["photos"] })
                }
              >
                Request Photo Permissions
              </IonButton>
              <IonButton
                expand="block"
                onClick={() => this.requestPermissions()}
              >
                Request All Permissions
              </IonButton>
            </IonCardContent>
          </IonCard>
        </IonContent>
      </IonPage>
    );
  }
}

export default PermissionsPage;
