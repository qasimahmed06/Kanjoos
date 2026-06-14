# Implementation Plan - Finance Tracker "Kanjoos"

We will build the lightweight personal finance tracker Android app called **Kanjoos** in Kotlin using Jetpack Compose and Supabase. The app will support logging income and expense transactions, displaying the current month's net balance, and showing a horizontal scrolling history list of previous months.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///home/qasim/Desktop/Everything/Projects/Kanjoos/gradle/libs.versions.toml)
- Add versions for Supabase client (`3.6.0`), Ktor client (`3.0.0`), and Kotlinx Serialization JSON (`1.7.3`).
- Add library definitions for Supabase BOM, Supabase Postgrest module, Ktor OkHttp client, Kotlinx Serialization JSON, and lifecycle-viewmodel-compose.
- Add plugin definition for `kotlin-serialization`.

#### [MODIFY] [build.gradle.kts (app)](file:///home/qasim/Desktop/Everything/Projects/Kanjoos/app/build.gradle.kts)
- Apply the Kotlin Serialization plugin.
- Enable `buildConfig` feature flags.
- Read `SUPABASE_URL` and `SUPABASE_KEY` from `local.properties` (with fallbacks) and expose them as `BuildConfig` constants.
- Declare the new dependencies added to the version catalog.

### Models and Backend Data Layer

#### [NEW] [Transaction.kt](file:///home/qasim/Desktop/Everything/Projects/Kanjoos/app/src/main/java/com/example/kanjoos/model/Transaction.kt)
- Create a `@Serializable` data class `Transaction` corresponding to the Supabase database schema:
  - `id`: `String?` (default `null`)
  - `amount`: `Double`
  - `label`: `String?` (default `null`)
  - `created_at`: `String?` (default `null`)

#### [NEW] [SupabaseClient.kt](file:///home/qasim/Desktop/Everything/Projects/Kanjoos/app/src/main/java/com/example/kanjoos/data/SupabaseClient.kt)
- Define a singleton initialization of the Supabase Client using `BuildConfig.SUPABASE_URL` and `BuildConfig.SUPABASE_KEY`.
- Install the `Postgrest` module.

#### [NEW] [TransactionRepository.kt](file:///home/qasim/Desktop/Everything/Projects/Kanjoos/app/src/main/java/com/example/kanjoos/data/TransactionRepository.kt)
- Create a repository class exposing methods to:
  - Fetch all transactions from the `transactions` table.
  - Insert a new transaction into the `transactions` table.

### Business Logic

#### [NEW] [MainViewModel.kt](file:///home/qasim/Desktop/Everything/Projects/Kanjoos/app/src/main/java/com/example/kanjoos/MainViewModel.kt)
- Handle loading state, list of transactions, and operations to refresh state and add transactions in coroutine scopes.
- Client-side grouping/summing calculations:
  - Calculate net balance of the current calendar month.
  - Calculate net balance of previous months for the history list, sorted in descending order of month (most recent first).

### User Interface

#### [NEW] [NetBalanceDisplay.kt](file:///home/qasim/Desktop/Everything/Projects/Kanjoos/app/src/main/java/com/example/kanjoos/ui/NetBalanceDisplay.kt)
- A Compose component showing the net profit/loss for the current month.
- Colors: Green (`#4CAF50`) if positive, Red (`#F44336`) if negative, Grey (`#9E9E9E`) if zero.
- Currency formatted with `+` / `−` sign prefixes and comma-separated thousands (e.g., `+Rs. 5,000`, `−Rs. 2,300`, `Rs. 0`).
- Text label displaying the current month and year (e.g., `June 2026`).

#### [NEW] [MonthHistoryRow.kt](file:///home/qasim/Desktop/Everything/Projects/Kanjoos/app/src/main/java/com/example/kanjoos/ui/MonthHistoryRow.kt)
- A compact, horizontally scrollable row containing month history cards.
- Each card shows: Month Name + Year (e.g., `May 2026`) and net amount for that month with the same color/sign rules.
- Ordered most-recent-first (excluding current month).

#### [NEW] [AddTransactionSheet.kt](file:///home/qasim/Desktop/Everything/Projects/Kanjoos/app/src/main/java/com/example/kanjoos/ui/AddTransactionSheet.kt)
- A ModalBottomSheet to add a new transaction:
  - Numeric input field for amount.
  - Segmented control / tab toggle: Income / Expense.
  - Optional text field for label.
  - Save button (validates and triggers the VM insert operation, then dismisses).

#### [NEW] [MainScreen.kt](file:///home/qasim/Desktop/Everything/Projects/Kanjoos/app/src/main/java/com/example/kanjoos/ui/MainScreen.kt)
- The root single-screen scaffold containing the top bar, net balance display, history row, and triggering the bottom sheet.
- Support loading and error states gracefully.

#### [MODIFY] [MainActivity.kt](file:///home/qasim/Desktop/Everything/Projects/Kanjoos/app/src/main/java/com/example/kanjoos/MainActivity.kt)
- Update to set the content to `MainScreen` with the main ViewModel.

---

## Verification Plan

### Automated Build & Compilation
- Run `./gradlew compileDebugKotlin` to ensure no syntax/type errors in Kotlin compilation.
- Run `./gradlew assembleDebug` to verify packaging and configurations.

### Manual Verification Instructions
1. Run the app on an Android emulator or device.
2. Verify visual aesthetics:
   - Modern Material 3 UI.
   - Net balance display matching requirements.
   - History scrollable cards.
3. Test functionality:
   - Click "+" to open the sheet.
   - Input amount, toggle type (income/expense), type an optional label, and save.
   - Verify UI updates net balance and local history appropriately.
   - Check Supabase backend database to confirm row insertion.
