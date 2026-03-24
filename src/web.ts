import { WebPlugin, CapacitorException } from '@capacitor/core';

import { CameraSource, CameraDirection, MediaType, MediaTypeSelection } from './definitions';
import type {
  CameraPlugin,
  GalleryImageOptions,
  GalleryPhotos,
  ImageOptions,
  PermissionStatus,
  Photo,
  RecordVideoOptions,
  EditPhotoOptions,
  PlayVideoOptions,
  EditURIPhotoOptions,
  EditPhotoResult,
  MediaResult,
  MediaResults,
  ChooseFromGalleryOptions,
  TakePhotoOptions,
} from './definitions';

export class CameraWeb extends WebPlugin implements CameraPlugin {
  async takePhoto(options: TakePhotoOptions): Promise<MediaResult> {
    // eslint-disable-next-line no-async-promise-executor
    return new Promise<MediaResult>(async (resolve, reject) => {
      if (options.webUseInput) {
        this.takePhotoCameraInputExperience(options, resolve, reject);
      } else {
        this.takePhotoCameraExperience(options, resolve, reject);
      }
    });
  }

  async recordVideo(_options: RecordVideoOptions): Promise<MediaResult> {
    throw this.unimplemented('recordVideo is not implemented on Web.');
  }

  async playVideo(_options: PlayVideoOptions): Promise<void> {
    throw this.unimplemented('playVideo is not implemented on Web.');
  }

  async chooseFromGallery(options: ChooseFromGalleryOptions): Promise<MediaResults> {
    // eslint-disable-next-line no-async-promise-executor
    return new Promise<MediaResults>(async (resolve, reject) => {
      this.galleryInputExperience(options, resolve, reject);
    });
  }

  async editPhoto(_options: EditPhotoOptions): Promise<EditPhotoResult> {
    throw this.unimplemented('editPhoto is not implemented on Web.');
  }

  async editURIPhoto(_options: EditURIPhotoOptions): Promise<MediaResult> {
    throw this.unimplemented('editURIPhoto is not implemented on Web.');
  }

  async getPhoto(options: ImageOptions): Promise<Photo> {
    // eslint-disable-next-line no-async-promise-executor
    return new Promise<Photo>(async (resolve, reject) => {
      if (options.webUseInput || options.source === CameraSource.Photos) {
        this.fileInputExperience(options, resolve, reject);
      } else if (options.source === CameraSource.Prompt) {
        let actionSheet: any = document.querySelector('pwa-action-sheet');
        if (!actionSheet) {
          actionSheet = document.createElement('pwa-action-sheet');
          document.body.appendChild(actionSheet);
        }
        actionSheet.header = options.promptLabelHeader || 'Photo';
        actionSheet.cancelable = false;
        actionSheet.options = [
          { title: options.promptLabelPhoto || 'From Photos' },
          { title: options.promptLabelPicture || 'Take Picture' },
        ];
        actionSheet.addEventListener('onSelection', async (e: any) => {
          const selection = e.detail;
          if (selection === 0) {
            this.fileInputExperience(options, resolve, reject);
          } else {
            this.cameraExperience(options, resolve, reject);
          }
        });
      } else {
        this.cameraExperience(options, resolve, reject);
      }
    });
  }

  async pickImages(_options: GalleryImageOptions): Promise<GalleryPhotos> {
    // eslint-disable-next-line no-async-promise-executor
    return new Promise<GalleryPhotos>(async (resolve, reject) => {
      this.multipleFileInputExperience(resolve, reject);
    });
  }

  private async cameraExperience(options: ImageOptions, resolve: any, reject: any) {
    if (customElements.get('pwa-camera-modal')) {
      const cameraModal: any = document.createElement('pwa-camera-modal');
      cameraModal.facingMode = options.direction === CameraDirection.Front ? 'user' : 'environment';
      document.body.appendChild(cameraModal);
      try {
        await cameraModal.componentOnReady();
        cameraModal.addEventListener('onPhoto', async (e: any) => {
          const photo = e.detail;

          if (photo === null) {
            reject(new CapacitorException('User cancelled photos app'));
          } else if (photo instanceof Error) {
            reject(photo);
          } else {
            resolve(await this._getCameraPhoto(photo, options));
          }

          cameraModal.dismiss();
          document.body.removeChild(cameraModal);
        });

        cameraModal.present();
      } catch (e) {
        this.fileInputExperience(options, resolve, reject);
      }
    } else {
      console.error(
        `Unable to load PWA Element 'pwa-camera-modal'. See the docs: https://capacitorjs.com/docs/web/pwa-elements.`,
      );
      this.fileInputExperience(options, resolve, reject);
    }
  }

  private fileInputExperience(options: ImageOptions, resolve: any, reject: any) {
    let input = document.querySelector('#_capacitor-camera-input') as HTMLInputElement;

    const cleanup = () => {
      input.parentNode?.removeChild(input);
    };

    if (!input) {
      input = document.createElement('input') as HTMLInputElement;
      input.id = '_capacitor-camera-input';
      input.type = 'file';
      input.hidden = true;
      document.body.appendChild(input);
      input.addEventListener('change', (_e: any) => {
        const file = input.files![0];
        let format = 'jpeg';

        if (file.type === 'image/png') {
          format = 'png';
        } else if (file.type === 'image/gif') {
          format = 'gif';
        }

        if (options.resultType === 'dataUrl' || options.resultType === 'base64') {
          const reader = new FileReader();

          reader.addEventListener('load', () => {
            if (options.resultType === 'dataUrl') {
              resolve({
                dataUrl: reader.result,
                format,
              } as Photo);
            } else if (options.resultType === 'base64') {
              const b64 = (reader.result as string).split(',')[1];
              resolve({
                base64String: b64,
                format,
              } as Photo);
            }

            cleanup();
          });

          reader.readAsDataURL(file);
        } else {
          resolve({
            webPath: URL.createObjectURL(file),
            format: format,
          });
          cleanup();
        }
      });
      input.addEventListener('cancel', (_e: any) => {
        reject(new CapacitorException('User cancelled photos app'));
        cleanup();
      });
    }

    input.accept = 'image/*';
    (input as any).capture = true;

    if (options.source === CameraSource.Photos || options.source === CameraSource.Prompt) {
      input.removeAttribute('capture');
    } else if (options.direction === CameraDirection.Front) {
      (input as any).capture = 'user';
    } else if (options.direction === CameraDirection.Rear) {
      (input as any).capture = 'environment';
    }

    input.click();
  }

  private multipleFileInputExperience(resolve: any, reject: any) {
    let input = document.querySelector('#_capacitor-camera-input-multiple') as HTMLInputElement;

    const cleanup = () => {
      input.parentNode?.removeChild(input);
    };

    if (!input) {
      input = document.createElement('input') as HTMLInputElement;
      input.id = '_capacitor-camera-input-multiple';
      input.type = 'file';
      input.hidden = true;
      input.multiple = true;
      document.body.appendChild(input);
      input.addEventListener('change', (_e: any) => {
        const photos = [];
        // eslint-disable-next-line @typescript-eslint/prefer-for-of
        for (let i = 0; i < input.files!.length; i++) {
          const file = input.files![i];
          let format = 'jpeg';

          if (file.type === 'image/png') {
            format = 'png';
          } else if (file.type === 'image/gif') {
            format = 'gif';
          }
          photos.push({
            webPath: URL.createObjectURL(file),
            format: format,
          });
        }
        resolve({ photos });
        cleanup();
      });
      input.addEventListener('cancel', (_e: any) => {
        reject(new CapacitorException('User cancelled photos app'));
        cleanup();
      });
    }

    input.accept = 'image/*';

    input.click();
  }

  private _getCameraPhoto(photo: Blob, options: ImageOptions) {
    return new Promise<Photo>((resolve, reject) => {
      const reader = new FileReader();
      const format = photo.type.split('/')[1];
      if (options.resultType === 'uri') {
        resolve({
          webPath: URL.createObjectURL(photo),
          format: format,
          saved: false,
        });
      } else {
        reader.readAsDataURL(photo);
        reader.onloadend = () => {
          const r = reader.result as string;
          if (options.resultType === 'dataUrl') {
            resolve({
              dataUrl: r,
              format: format,
              saved: false,
            });
          } else {
            resolve({
              base64String: r.split(',')[1],
              format: format,
              saved: false,
            });
          }
        };
        reader.onerror = (e) => {
          reject(e);
        };
      }
    });
  }

  private async takePhotoCameraExperience(options: TakePhotoOptions, resolve: any, reject: any) {
    if (customElements.get('pwa-camera-modal')) {
      const cameraModal: any = document.createElement('pwa-camera-modal');
      cameraModal.facingMode = options.cameraDirection === CameraDirection.Front ? 'user' : 'environment';
      document.body.appendChild(cameraModal);
      try {
        await cameraModal.componentOnReady();
        cameraModal.addEventListener('onPhoto', async (e: any) => {
          const photo = e.detail;

          if (photo === null) {
            reject(new CapacitorException('User cancelled photos app'));
          } else if (photo instanceof Error) {
            reject(photo);
          } else {
            resolve(await this._getCameraPhotoAsMediaResult(photo));
          }

          cameraModal.dismiss();
          document.body.removeChild(cameraModal);
        });

        cameraModal.present();
      } catch (e) {
        this.takePhotoCameraInputExperience(options, resolve, reject);
      }
    } else {
      console.error(
        `Unable to load PWA Element 'pwa-camera-modal'. See the docs: https://capacitorjs.com/docs/web/pwa-elements.`,
      );
      this.takePhotoCameraInputExperience(options, resolve, reject);
    }
  }

  private takePhotoCameraInputExperience(options: TakePhotoOptions, resolve: any, reject: any) {
    let input = document.querySelector('#_capacitor-camera-input-takephoto') as HTMLInputElement;

    const cleanup = () => {
      input.parentNode?.removeChild(input);
    };

    if (!input) {
      input = document.createElement('input') as HTMLInputElement;
      input.id = '_capacitor-camera-input-takephoto';
      input.type = 'file';
      input.hidden = true;
      document.body.appendChild(input);
      input.addEventListener('change', async (_e: any) => {
        if (!input.files || input.files.length === 0) {
          reject(new CapacitorException('No file selected'));
          cleanup();
          return;
        }

        const file = input.files[0];
        let format = 'jpeg';

        if (file.type === 'image/png') {
          format = 'png';
        } else if (file.type === 'image/gif') {
          format = 'gif';
        }

        // Get resolution from image file
        const resolution = await this._getImageResolution(file);

        const reader = new FileReader();
        reader.addEventListener('load', () => {
          const b64 = (reader.result as string).split(',')[1];
          resolve({
            type: MediaType.Photo,
            thumbnail: b64,
            webPath: URL.createObjectURL(file),
            saved: false,
            metadata: {
              format,
              resolution,
              size: file.size,
              creationDate: new Date(file.lastModified).toISOString(),
            },
          } as MediaResult);
          cleanup();
        });

        reader.readAsDataURL(file);
      });
      input.addEventListener('cancel', (_e: any) => {
        reject(new CapacitorException('User cancelled photos app'));
        cleanup();
      });
    }

    input.accept = 'image/*';
    if (options.cameraDirection === CameraDirection.Front) {
      (input as any).capture = 'user';
    } else {
      // CameraDirection.Rear
      (input as any).capture = 'environment';
    }

    input.click();
  }

  private galleryInputExperience(options: ChooseFromGalleryOptions, resolve: any, reject: any) {
    let input = document.querySelector('#_capacitor-camera-input-gallery') as HTMLInputElement;

    const cleanup = () => {
      input.parentNode?.removeChild(input);
    };

    if (!input) {
      input = document.createElement('input') as HTMLInputElement;
      input.id = '_capacitor-camera-input-gallery';
      input.type = 'file';
      input.hidden = true;
      input.multiple = options.allowMultipleSelection ?? false;
      document.body.appendChild(input);
      input.addEventListener('change', async (_e: any) => {
        if (!input.files || input.files.length === 0) {
          reject(new CapacitorException('No files selected'));
          cleanup();
          return;
        }

        const results: MediaResult[] = [];

        // eslint-disable-next-line @typescript-eslint/prefer-for-of
        for (let i = 0; i < input.files.length; i++) {
          const file = input.files[i];
          let format = 'jpeg';
          let type = MediaType.Photo;
          let resolution = '0x0';

          if (file.type.startsWith('image/')) {
            if (file.type === 'image/png') {
              format = 'png';
            } else if (file.type === 'image/gif') {
              format = 'gif';
            }

            // Get resolution from image file
            resolution = await this._getImageResolution(file);
          } else if (file.type.startsWith('video/')) {
            type = MediaType.Video;
            format = file.type.split('/')[1];

            // Get resolution and duration from video file
            let duration: number | undefined;
            try {
              const videoMetadata = await this._getVideoMetadata(file);
              resolution = videoMetadata.resolution;
              duration = videoMetadata.duration;
            } catch (e) {
              console.warn('Failed to get video metadata:', e);
            }

            results.push({
              type,
              webPath: URL.createObjectURL(file),
              saved: false,
              metadata: {
                format,
                resolution,
                size: file.size,
                creationDate: new Date(file.lastModified).toISOString(),
                duration,
              },
            });
            continue;
          }

          results.push({
            type,
            webPath: URL.createObjectURL(file),
            saved: false,
            metadata: {
              format,
              resolution,
              size: file.size,
              creationDate: new Date(file.lastModified).toISOString(),
            },
          });
        }
        resolve({ results });
        cleanup();
      });
      input.addEventListener('cancel', (_e: any) => {
        reject(new CapacitorException('User cancelled photos app'));
        cleanup();
      });
    }

    // Set accept attribute based on mediaType
    const mediaType = options.mediaType ?? MediaTypeSelection.Photo;
    if (mediaType === MediaTypeSelection.Photo) {
      input.accept = 'image/*';
    } else if (mediaType === MediaTypeSelection.Video) {
      input.accept = 'video/*';
    } else {
      // MediaTypeSelection.All
      input.accept = 'image/*,video/*';
    }

    input.click();
  }

  private async _getCameraPhotoAsMediaResult(photo: Blob): Promise<MediaResult> {
    return new Promise<MediaResult>(async (resolve, reject) => {
      const reader = new FileReader();
      const format = photo.type.split('/')[1];

      // Get resolution from image blob
      const resolution = await this._getImageResolution(photo);

      reader.readAsDataURL(photo);
      reader.onloadend = () => {
        const r = reader.result as string;
        const b64 = r.split(',')[1];
        resolve({
          type: MediaType.Photo,
          thumbnail: b64,
          webPath: URL.createObjectURL(photo),
          saved: false,
          metadata: {
            format,
            resolution,
            size: photo.size,
            creationDate: new Date().toISOString(),
          },
        });
      };
      reader.onerror = (e) => {
        reject(e);
      };
    });
  }

  private async _getImageResolution(image: Blob | File): Promise<string> {
    try {
      const bitmap = await createImageBitmap(image);
      const resolution = `${bitmap.width}x${bitmap.height}`;
      bitmap.close();
      return resolution;
    } catch (e) {
      console.warn('Failed to get image resolution:', e);
      return '0x0';
    }
  }

  private _getVideoMetadata(videoFile: File): Promise<{ resolution: string; duration: number }> {
    return new Promise((resolve) => {
      const video = document.createElement('video');
      video.preload = 'metadata';

      video.onloadedmetadata = () => {
        // Clean up
        URL.revokeObjectURL(video.src);
        const resolution = `${video.videoWidth}x${video.videoHeight}`;
        const duration = video.duration;
        resolve({ resolution, duration });
      };

      video.onerror = () => {
        // Clean up and return defaults
        URL.revokeObjectURL(video.src);
        resolve({ resolution: '0x0', duration: 0 });
      };

      video.src = URL.createObjectURL(videoFile);
    });
  }

  async checkPermissions(): Promise<PermissionStatus> {
    if (typeof navigator === 'undefined' || !navigator.permissions) {
      throw this.unavailable('Permissions API not available in this browser');
    }

    try {
      // https://developer.mozilla.org/en-US/docs/Web/API/Permissions/query
      // the specific permissions that are supported varies among browsers that implement the
      // permissions API, so we need a try/catch in case 'camera' is invalid
      const permission = await window.navigator.permissions.query({
        name: 'camera',
      });
      return {
        camera: permission.state,
        photos: 'granted',
      };
    } catch {
      throw this.unavailable('Camera permissions are not available in this browser');
    }
  }

  async requestPermissions(): Promise<PermissionStatus> {
    throw this.unimplemented('Not implemented on web.');
  }

  async pickLimitedLibraryPhotos(): Promise<GalleryPhotos> {
    throw this.unavailable('Not implemented on web.');
  }

  async getLimitedLibraryPhotos(): Promise<GalleryPhotos> {
    throw this.unavailable('Not implemented on web.');
  }
}

const Camera = new CameraWeb();

export { Camera };
