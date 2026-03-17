import {
  IonButtons,
  IonContent,
  IonHeader,
  IonMenuButton,
  IonPage,
  IonTitle,
  IonToolbar,
  IonCard,
  IonCardContent,
  IonButton,
} from "@ionic/react";
import "./Home.css";

const Home: React.FC = () => {
  return (
    <IonPage>
      <IonHeader>
        <IonToolbar>
          <IonButtons slot="start">
            <IonMenuButton />
          </IonButtons>
          <IonTitle>Capacitor Camera Example</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent fullscreen>
        <IonHeader collapse="condense">
          <IonToolbar>
            <IonTitle size="large">Capacitor Camera Example</IonTitle>
          </IonToolbar>
        </IonHeader>
        <IonCard>
          <IonCardContent>
            <p>
              Demo app showcasing the Capacitor Camera plugin features. Click one of the buttons to test a particular portion of the plugin.
            </p>
          </IonCardContent>
        </IonCard>
        <IonCard>
          <IonCardContent>
            <IonButton expand="block" routerLink="/permissions" style={{ marginBottom: "16px" }}>
              Permissions
            </IonButton>
            <IonButton expand="block" routerLink="/camera" style={{ marginBottom: "16px" }}>
              Take Picture
            </IonButton>
            <IonButton expand="block" routerLink="/gallery" style={{ marginBottom: "16px" }}>
              Gallery
            </IonButton>
            <IonButton expand="block" routerLink="/video" style={{ marginBottom: "16px" }}>
              Record Video
            </IonButton>
            <IonButton expand="block" routerLink="/edit" style={{ marginBottom: "16px" }}>
              Edit Photo
            </IonButton>
            <IonButton expand="block" routerLink="/history" style={{ marginBottom: "16px" }}>
              Media History
            </IonButton>
          </IonCardContent>
        </IonCard>
      </IonContent>
    </IonPage>
  );
};

export default Home;
