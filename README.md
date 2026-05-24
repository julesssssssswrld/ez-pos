# EZ-POS

EZ-POS is a offline Point of Sale (POS) Android application designed for small to medium-sized retail operations. Built using Jetpack Compose and Material 3, the app features a sleek, modern design, and offers tools to manage inventory, track transactions, process payments, and analyze sales metrics offline.

---

## Key Features

### Cashier / Point of Sale
* **Cart Management**: Add, remove, and update quantities of items.
* **Drag-to-Reorder**: Reorder cart items using smooth drag-and-drop animations.
* **Barcode Scanning**: Built-in, high-speed camera barcode scanning powered by **Google ML Kit Barcode Scanning** and **CameraX**.

### Admin Panel & Management
* **Inventory Management**: Complete CRUD operations for products, with low-stock and out-of-stock tracking indicators.
* **Audit Logs (Inventory Logs)**: Transparent transaction history tracking every stock addition, deduction, or sale transaction with notes and timestamps.
* **Sales History**: Complete list of all historical sales, complete with detailed breakdowns of items sold.
* **Visual Analytics**: Metrics and interactive analytics dashboards showing sales trends over time, top-selling products, and revenue breakdowns.

### Modern UI/UX
* **Material Design 3**: Modern component aesthetics, responsive layouts, and animations.

---

## Technical Stack

* **Core**: [Kotlin](https://kotlinlang.org/)
* **UI Framework**: [Jetpack Compose](https://developer.android.com/compose) with Material 3
* **Database**: [Room Persistence Library](https://developer.android.com/training/data-storage/room) (SQLite)
* **Lifecycle & Navigation**: AndroidX Navigation Compose, AndroidX Lifecycle ViewModels, LiveData/Flows
* **Image Processing & Camera**: [Google ML Kit Barcode Scanning](https://developers.google.com/ml-kit/vision/barcode-scanning) and [CameraX](https://developer.android.com/training/camerax)
* **Animations & Lists**: [Reorderable](https://github.com/Calvin-Ko/Reorderable) by Calvin-Ko for drag-and-drop lists
* **Build System**: Gradle Kotlin DSL (`build.gradle.kts`)
* **CI/CD**: GitHub Actions

---

## Project Structure

```
Soap/
├── .github/
│   └── workflows/
│       └── android-ci.yml           # GitHub Actions automated build checks
├── android/
│   ├── app/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/com/iandevs/ezpos/
│   │   │   │   │   ├── data/        # Room Database, Repository, DAOs, & Entities
│   │   │   │   │   │   ├── dao/     # ProductDao, SaleDao, InventoryLogDao
│   │   │   │   │   │   ├── entity/  # Product, Sale, SaleItem, InventoryLog
│   │   │   │   │   │   ├── SoapDatabase.kt
│   │   │   │   │   │   └── SoapRepository.kt
│   │   │   │   │   ├── theme/       # App styling (Color, Theme, Type)
│   │   │   │   │   ├── ui/          # Compose Screen Components & ViewModels
│   │   │   │   │   │   ├── admin/   # Inventory, Log, History, & Analytics screens
│   │   │   │   │   │   ├── cashier/ # Checkout, Cart, & Barcode Camera screens
│   │   │   │   │   │   └── main/    # Root screen entry
│   │   │   │   │   ├── util/        # Shared helpers (Currency Formatters)
│   │   │   │   │   ├── MainActivity.kt
│   │   │   │   │   ├── Navigation.kt
│   │   │   │   │   └── SoapApp.kt
│   │   │   │   └── AndroidManifest.xml
│   │   │   └── res/                 # App assets and icons
│   │   └── build.gradle.kts         # App-level build configurations
│   ├── build.gradle.kts             # Project-level build configurations
│   └── settings.gradle.kts          # Project settings
└── README.md                        # Documentation
```
