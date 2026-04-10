# Capacitor Camera Example App

This is an example application demonstrating the Capacitor Camera plugin functionality. Built with Ionic React, TypeScript, and Vite.

## Setup

1. Install dependencies:
```bash
npm install
```

2. Build the web assets:
```bash
npm run build
```

3. Sync native projects (required after initial setup or when adding/updating plugins):
```bash
npx cap sync
```

## Running the App

### Web Development

Run the app in development mode with hot-reload:
```bash
npm run dev
```

The app will open at `http://localhost:5173` (or another port if 5173 is busy).

> **Note:** Camera and MediaDevices APIs require a secure context (HTTPS) to function properly in web browsers. While `localhost` is treated as secure, if you need to test on other devices or access the app via your local network IP, you'll need to serve it over HTTPS. You can use tools like [ngrok](https://ngrok.com/) to create a secure tunnel (you may also need to add the ngrok hostname to `server.allowedHosts` in `vite.config.ts`):
> ```bash
> # Install ngrok (if not already installed)
> # Then run your dev server and create a tunnel
> npm run dev
> # In another terminal:
> ngrok http 5173
> ```
> This will provide an HTTPS URL that you can use to test camera functionality on any device.

### iOS

1. Open the iOS project in Xcode:
```bash
npx cap open ios
```

2. In Xcode:
   - Select a target device or simulator
   - Click the Run button or press `Cmd + R`
   - Ensure you have a development team selected in Signing & Capabilities

Alternatively, run the app directly via Capacitor CLI with:

```bash
npx cap run ios
```

### Android

1. Open the Android project in Android Studio:
```bash
npx cap open android
```

Alternatively, you can manually open Android Studio and select the `android` folder.

2. In Android Studio:
   - Wait for Gradle sync to complete
   - Select a target device or emulator
   - Click the Run button or press `Shift + F10`

Alternatively, run the app directly via Capacitor CLI with:

```bash
npx cap run android
```

## Project Structure

```
.
├── android/              # Native Android project
├── ios/                  # Native iOS project
├── src/
│   ├── components/       # Reusable React components
│   ├── pages/           # Page-level components
│   ├── services/        # Business logic and services
│   ├── theme/           # Ionic theme customization
│   ├── App.tsx          # Main app component
│   ├── routes.tsx       # Route definitions
│   └── main.tsx         # App entry point
├── public/              # Static assets
├── dist/                # Built web assets (generated)
├── capacitor.config.ts  # Capacitor configuration
├── vite.config.ts       # Vite build configuration
└── package.json         # Dependencies and scripts
```

### Pages

- **Home** (`/`) - Landing page with navigation to all features
- **Take Picture** (`/camera`) - Captures photos using camera with configurable options. Uses `TakePictureConfigurable` and `PhotoWithMetadata` components. Supports in-app photo editing via `editURIPhoto`.
- **Gallery** (`/gallery`) - Selects single or multiple photos from device gallery. Uses `ChooseFromGalleryConfigurable`, `MediaCarousel`, and `PhotoWithMetadata` components.
- **Record Video** (`/video`) - Records video with configurable settings. Uses `RecordVideoConfigurable` and `VideoWithMetadata` components.
- **Edit Photo** (`/edit`) - Tests photo editing methods (`editPhoto` and `editURIPhoto`) in isolation using pre-loaded test images. Uses `EditPhotoConfigurable` and `EditURIPhotoConfigurable` components.
- **Permissions** (`/permissions`) - Checks and requests camera and photo library permissions.
- **Media History** (`/history`) - Displays all captured/selected media with metadata. Uses `MediaHistoryService` to track media interactions.

### Key Components

- **Menu** - Side navigation menu for accessing all pages
- **TakePictureConfigurable** - Configurable camera capture interface
- **ChooseFromGalleryConfigurable** - Gallery picker with single/multiple selection options
- **RecordVideoConfigurable** - Video recording interface with configuration options
- **PhotoWithMetadata** / **VideoWithMetadata** - Display media with metadata and edit capabilities
- **MediaCarousel** - Displays multiple photos in a swipeable carousel

