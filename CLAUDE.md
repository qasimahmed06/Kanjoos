# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
**Kanjoos** is a lightweight personal finance tracker Android application built using Kotlin, Jetpack Compose, and Supabase.

## Architecture
- **Data Layer**: Uses `SupabaseClient` for backend communication and `TransactionRepository` for CRUD operations.
- **Business Logic**: `MainViewModel` manages application state, transaction flow, and client-side financial calculations (grouping by month, summing balances).
- **UI Layer**: Built with Jetpack Compose. `MainScreen` serves as the primary entry point, orchestrating smaller components like `NetBalanceDisplay`, `MonthHistoryRow`, and `AddTransactionSheet`.

## Development Commands
- **Build**: `./gradlew assembleDebug`
- **Lint**: `./gradlew lintDebug`
- **Tests**: `./gradlew test` (To run a specific test class: `./gradlew test --tests "com.qasim.kanjoos.ExampleUnitTest"`)
- **Compilation**: `./gradlew compileDebugKotlin`
