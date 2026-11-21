# Food Ordering POS (Android)

A starter Android Studio project for a single-tablet, offline waiter ordering app.

## Included
- General restaurant starter menu: Rice, Noodles, Drinks, Snacks
- Table number and item quantity controls
- Special instructions
- Order numbering and local order history
- Pending / Preparing / Completed statuses
- Basic daily completed-sales total
- Menu add/delete
- Bluetooth ESC/POS printing to an already-paired printer
- 58mm / 80mm paper-width setting

## Build the APK
1. Install Android Studio on a computer.
2. Open this `FoodOrderingPOS` folder in Android Studio.
3. Allow Gradle sync and install any requested Android SDK platform (API 35).
4. Choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Install the generated APK on the Android tablet.

## Important notes
- This is a starter version; it has not been compiled or tested on a physical tablet in this environment.
- Bluetooth printing is intended for paired ESC/POS thermal printers. Printer models vary, so a real printer test may require a small compatibility adjustment.
- Orders and menu are stored locally on this tablet. There is no cloud sync or payment processing.
- The initial menu is sample data. Use Menu Management to add or delete items.
- For Android 12+, grant the nearby devices/Bluetooth permission when prompted.
