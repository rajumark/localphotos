# LocalPhotos

<p align="center">
  <img src="docs/icon.svg" width="120" height="120" alt="LocalPhotos Logo">
</p>

<p align="center">
  <strong>On-device photo search by text content</strong>
  <br>
  Search your photos using OCR — completely offline, no data leaves your device.
</p>

## Features
 
- **OCR-powered search** — All photos are scanned using ML Kit Text Recognition v2 to extract text, enabling full-text search
- **Full-text search** — Search across all extracted text instantly with FTS4
- **Material Design 3** — Modern UI with Compose, dynamic color support (Android 12+)
- **Two tabs** — Browse all photos or filter by favorites
- **Filter chips** — Toggle between all photos and photos with detected text only
- **Full-screen viewer** — View photos with actions: view extracted text, share, delete, add to favorites
- **Automatic refresh** — New photos added while the app is paused are detected automatically
- **Pending indicator** — Shows how many photos are queued for OCR processing
- **Efficient processing** — Images processed 1–2 at a time to avoid UI lag
- **Thumbnail caching** — Fast, smooth grid scrolling with Coil
- **Pagination** — Large photo libraries handled gracefully with Paging 3
- **100% on-device** — No internet connection required, all processing happens locally

## Screenshots

*(Add screenshots here)*

## Tech Stack

| Component | Library |
|-----------|---------|
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| OCR | Google ML Kit Text Recognition v2 |
| Database | Room (with FTS4 full-text search) |
| Image Loading | Coil |
| DI | Koin |
| Architecture | MVVM + Repository Pattern |
| Pagination | Paging 3 + Paging Compose |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |

## Getting Started

### Prerequisites

- Android Studio Ladybug (or newer)
- JDK 17
- Android SDK 35

### Build & Install

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Run on emulator

```bash
./gradlew installDebug
```

## Architecture

```
app/
├── data/
│   ├── local/
│   │   ├── entities/     # Room entities (PhotoEntity, PhotoFtsEntity)
│   │   ├── AppDatabase.kt
│   │   └── PhotoDao.kt
│   ├── repository/       # PhotoRepository
│   └── model/            # Domain models
├── di/                   # Koin modules
├── ocr/                  # ML Kit OCR processor
├── navigation/           # Navigation routes
└── ui/
    ├── main/             # Main screen (search + grid)
    ├── detail/           # Full-screen photo viewer
    ├── favorites/        # Favorites tab
    ├── components/       # Reusable composables
    └── theme/            # Material 3 theming
```

## License

MIT License — see the [LICENSE](LICENSE) file for details.

Copyright (c) 2024 LocalPhotos

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
