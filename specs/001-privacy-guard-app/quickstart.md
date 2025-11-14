# Quickstart: Privacy Guard

This document provides instructions for setting up and running the Privacy Guard project.

## Prerequisites

-   Android Studio Iguana | 2023.2.1 or later
-   Android SDK 34+
-   A physical Android device with a front-facing camera running Android 12+

## Setup

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/samizouari/VibePrivacy.git
    ```
2.  **Open the project in Android Studio**:
    -   Select "Open an Existing Project".
    -   Navigate to the cloned repository and select the `VibePrivacy` directory.
3.  **Sync Gradle**:
    -   Android Studio will automatically sync the Gradle project. This may take a few minutes.

## Running the Application

1.  **Connect your Android device**:
    -   Enable developer options and USB debugging on your device.
    -   Connect the device to your computer via USB.
2.  **Run the 'app' configuration**:
    -   Select the 'app' run configuration from the dropdown menu in the toolbar.
    -   Click the 'Run' button (green play icon).
3.  **Grant Permissions**:
    -   On the first launch, the application will guide you through granting the necessary permissions (Camera, Microphone, Accessibility Service).

## Running Tests

To run the unit tests, execute the following command in the terminal:

```bash
./gradlew test
```

To run the instrumented tests, connect a device and run:

```bash
./gradlew connectedAndroidTest
```
