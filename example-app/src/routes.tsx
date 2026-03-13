import React from "react";
import Home from "./pages/Home";
import PermissionsPage from "./pages/PermissionsPage";
import TakePicturePage from "./pages/TakePicturePage";
import GalleryPage from "./pages/GalleryPage";
import RecordVideoPage from "./pages/RecordVideoPage";
import EditPhotoPage from "./pages/EditPhotoPage";

export interface Page {
  readonly url: string;
  readonly title: string;
  readonly icon: React.ReactElement;
  readonly component: React.ComponentType<any>;
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
    url: "/permissions",
    title: "Permissions",
    icon: (
      <span role="img" aria-label="permissions">
        🔐
      </span>
    ),
    component: PermissionsPage,
  },
  {
    url: "/camera",
    title: "Take Picture",
    icon: (
      <span role="img" aria-label="camera">
        📷
      </span>
    ),
    component: TakePicturePage,
  },
  {
    url: "/gallery",
    title: "Gallery",
    icon: (
      <span role="img" aria-label="gallery">
        🖼️
      </span>
    ),
    component: GalleryPage,
  },
  {
    url: "/video",
    title: "Record Video",
    icon: (
      <span role="img" aria-label="video">
        🎥
      </span>
    ),
    component: RecordVideoPage,
  },
  {
    url: "/edit",
    title: "Edit Photo",
    icon: (
      <span role="img" aria-label="edit">
        ✏️
      </span>
    ),
    component: EditPhotoPage,
  },
];
