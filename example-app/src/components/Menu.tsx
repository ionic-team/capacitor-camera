import {
  IonContent,
  IonHeader,
  IonItem,
  IonLabel,
  IonList,
  IonMenu,
  IonMenuToggle,
  IonTitle,
  IonToolbar,
} from "@ionic/react";
import { useLocation } from "react-router-dom";
import { routes } from "../routes";
import "./Menu.css";

const Menu: React.FC = () => {
  const location = useLocation();

  return (
    <IonMenu contentId="main">
      <IonHeader>
        <IonToolbar>
          <IonTitle>CameraPlugin</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent>
        <IonList>
          {routes.map((route) => (
            <IonMenuToggle key={route.url} autoHide={false}>
              <IonItem
                button
                routerLink={route.url}
                routerDirection="none"
                lines="none"
                detail={false}
                className={location.pathname === route.url ? "selected" : ""}
              >
                <div className="menu-icon">{route.icon}</div>
                <IonLabel>{route.title}</IonLabel>
              </IonItem>
            </IonMenuToggle>
          ))}
        </IonList>
      </IonContent>
    </IonMenu>
  );
};

export default Menu;
