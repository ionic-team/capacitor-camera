import React from "react";
import Camera from "./pages/Camera";
import Home from "./pages/Home";

export interface Page {
  readonly url: string;
  readonly title: string;
  readonly icon: JSX.Element;
  readonly component: typeof React.Component | React.FC;
}

export const routes: Page[] = [
  {
    url: "/home",
    title: "Home",
    icon: (
      <span role="img" aria-label="home">
        🏡
      </span>
    ),
    component: Home,
  },
  {
    url: "/camera",
    title: "Camera",
    icon: (
      <span role="img" aria-label="camera">
        📷
      </span>
    ),
    component: Camera,
  },
];
