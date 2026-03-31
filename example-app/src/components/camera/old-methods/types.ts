import { CameraResultType, CameraSource, CameraDirection } from "@capacitor/camera";

export interface GetPhotoConfig {
  quality: number;
  allowEditing: boolean;
  resultType: CameraResultType;
  source: CameraSource;
  saveToGallery: boolean;
  width: number | undefined;
  height: number | undefined;
  correctOrientation: boolean;
  direction: CameraDirection;
  presentationStyle: 'fullscreen' | 'popover';
  webUseInput: boolean;
  promptLabelHeader: string;
  promptLabelCancel: string;
  promptLabelPhoto: string;
  promptLabelPicture: string;
}

export interface PickImagesConfig {
  quality: number;
  width: number | undefined;
  height: number | undefined;
  correctOrientation: boolean;
  presentationStyle: 'fullscreen' | 'popover';
  limit: number;
}

export interface PhotoResult {
  path?: string;
  webPath?: string;
  base64String?: string;
  dataUrl?: string;
  exif?: any;
  saved?: boolean;
}
