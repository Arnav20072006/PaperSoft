# PaperSoft — Image to PDF Converter

PaperSoft is a native Android application developed in **Java** using **Android Studio**. It lets users select images, arrange their order, and combine them into a PDF document that can be opened or shared from the app.

> **Project scope:** This is a collaborative academic Android project. It demonstrates Android development, image handling, PDF generation, and document-sharing workflows; it is not presented as a production-ready release.

## Overview

PaperSoft provides a simple workflow for creating a PDF from photos or other images. Users can select multiple images, review and reorder them, choose an output filename, and generate a PDF with one page per processed image.

## Features

- **Multiple image selection** using Android's content picker
- **Image preview and management** with move-up, move-down, and delete controls
- **PDF generation** using Android's `PdfDocument` API, with images scaled to fit pages
- **Custom filenames** with a default filename and automatic numeric suffixes for existing names
- **Open generated PDFs** using an installed PDF viewer
- **Share generated PDFs** through Android's share sheet
- **Light, dark, and system-default theme options**, with the selected theme saved locally

## Technologies

- **Language:** Java
- **IDE:** Android Studio
- **UI:** Android XML layouts
- **Build:** Gradle with Kotlin DSL
- **Libraries / APIs:** AndroidX, Android `PdfDocument`, Activity Result APIs, `FileProvider`, and `SharedPreferences`

## How it works

1. Launch PaperSoft and optionally enter a name for your PDF.
2. Tap **Select Images** and choose one or more images.
3. Use **Up**, **Down**, or **Delete** to arrange the selected images.
4. Tap **Generate PDF** to create the document.
5. Use **Open PDF** or **Share PDF** to access or share the generated file.

## Project structure

```text
PaperSoft/
├── app/
│   ├── src/main/java/com/example/papersoft/MainActivity.java
│   ├── src/main/res/layout/activity_main.xml
│   ├── src/main/res/xml/file_paths.xml
│   ├── src/main/AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
└── settings.gradle.kts
```

The main activity currently handles image selection, ordering, PDF generation, theme preferences, and document actions.

## Getting started

1. Clone this repository:
   ```bash
   git clone https://github.com/Arnav20072006/PaperSoft.git
   ```
2. Open the cloned project folder in **Android Studio**.
3. Allow Android Studio to sync the Gradle project and install any required SDK components.
4. Run the `app` configuration on an Android emulator or a connected device.

**Note:** A successful build and execution on every Android version has not been independently verified.

## Current limitations

- The PDF-saving implementation uses legacy public Downloads file access; behavior on modern Android versions may vary due to scoped-storage restrictions.
- Large images are decoded at full resolution, and PDF generation runs on the main thread, which can affect performance for large image sets.
- The repository currently contains example test files rather than dedicated automated tests for the main PDF workflow.

These are potential areas for future improvement, not claims of completed fixes.


## Project status

**Academic project — initial portfolio version.** Core application code is available in this repository; further reliability and compatibility improvements are planned for future iterations.
