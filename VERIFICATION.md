Manual Verification — Kanjoos

1) Build & package (local):

```bash
./gradlew assembleDebug
```

If the build fails with AAR metadata / compileSdk errors, update `compileSdk` to at least 37 in `app/build.gradle.kts` (or align dependency versions). Example:

```kotlin
android {
  compileSdk = 37
  // keep targetSdk at 36 if you prefer, or update both
}
```

2) Run on emulator/device:

```bash
./gradlew installDebug
```

3) Manual UI checks:
- Open app: verify top bar shows "Kanjoos".
- Confirm current month net balance displays with color rules (+ green, − red, grey for zero).
- Scroll month history horizontally and verify month labels and amounts.
- Tap "+" to open the add-transaction sheet.
- Add an income and an expense; verify the current month net updates after saving.

4) Backend verification:
- Ensure `local.properties` contains `SUPABASE_URL` and `SUPABASE_KEY`.
- Verify inserted rows appear in Supabase `transactions` table.

5) Troubleshooting notes:
- If compilation errors point to `compileSdk` mismatch, update `app/build.gradle.kts` to `compileSdk = 37` and rerun assemble.
- If networking fails on device, check network permissions and that the Supabase URL is reachable.
