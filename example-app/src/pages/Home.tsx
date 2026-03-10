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
            <IonButton expand="block" routerLink="/permissions">
              Permissions
            </IonButton>
            <IonButton expand="block" routerLink="/camera">
              Take Picture
            </IonButton>
            <IonButton expand="block" routerLink="/gallery">
              Gallery
            </IonButton>
          </IonCardContent>
        </IonCard>
      </IonContent>
    </IonPage>
  );
};

export default Home;
