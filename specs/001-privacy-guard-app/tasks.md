# Tasks: Privacy Guard

**Input**: Design documents from `/specs/001-privacy-guard-app/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md

**Tests**: No test tasks were generated as they were not explicitly requested in the feature specification.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Paths follow the structure defined in `plan.md`: `app/src/main/java/com/n7/vibeprivacy/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and dependency configuration.

- [X] T001 Configure `build.gradle.kts` with required dependencies: CameraX, TensorFlow Lite, Room, and Android SensorManager in `app/build.gradle.kts`
- [X] T002 Create the base package structure as defined in `plan.md` inside `app/src/main/java/com/n7/vibeprivacy/`
- [X] T003 [P] Implement basic UI theme and colors in `app/src/main/res/values/`
- [X] T004 [P] Set up dependency injection framework (e.g., Hilt) in the `app/src/main/java/com/n7/vibeprivacy/di/` package.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T005 Define Room database entities (`ThreatEvent`, `ProtectionProfile`, `WhitelistedFace`) in `app/src/main/java/com/n7/vibeprivacy/data/models/`.
- [X] T006 Create Room database DAOs for each entity in `app/src/main/java/com/n7/vibeprivacy/data/source/`.
- [X] T007 Implement the Room database and repository classes in `app/src/main/java/com/n7/vibeprivacy/data/`.
- [X] T008 Create a foreground service for background monitoring in `app/src/main/java/com/n7/vibeprivacy/services/MonitoringService.kt`.
- [X] T009 Implement permission handling for Camera, Microphone, and other required permissions.

**Checkpoint**: Foundation ready - user story implementation can now begin.

---

## Phase 3: User Story 1 - Core Threat Detection and Screen Masking (Priority: P1) 🎯 MVP

**Goal**: Detect when someone is looking over the user's shoulder or when suspicious keywords are spoken, and automatically mask the screen.

**Independent Test**: With the app active, have a second person approach the phone and look at the screen. The screen should automatically apply a blur or overlay effect.

### Implementation for User Story 1

- [X] T010 [US1] Implement CameraX manager in `app/src/main/java/com/n7/vibeprivacy/core/camerax/` to capture and analyze frames from the front camera.
- [X] T011 [US1] Integrate a face detection model with TensorFlow Lite to identify the number of faces in `app/src/main/java/com/n7/vibeprivacy/core/ml/FaceDetector.kt`.
- [X] T012 [US1] Implement a basic keyword detection mechanism using the microphone in `app/src/main/java/com/n7/vibeprivacy/core/audio/KeywordDetector.kt`.
- [X] T013 [US1] Create a screen overlay/masking component that can be drawn over other apps in `app/src/main/java/com/n7/vibeprivacy/ui/overlay/ScreenMask.kt`.
- [X] T014 [US1] In `MonitoringService`, integrate face and keyword detection, triggering the screen mask when a threat is detected.
- [X] T015 [US1] When a threat is detected, create and save a `ThreatEvent` to the Room database.
- [X] T016 [US1] Create a basic main screen UI to show the service status (active/inactive) in `app/src/main/java/com/n7/vibeprivacy/features/main/MainScreen.kt`.

**Checkpoint**: At this point, User Story 1 should be functional and testable independently.

---

## Phase 4: User Story 2 - Advanced Sensor Fusion and Protection Modes (Priority: P2)

**Goal**: Use accelerometer, gyroscope, and proximity sensors for more subtle threat detection and allow the user to choose different protection modes.

**Independent Test**: With the app in "Paranoia" mode, quickly snatch the phone from the user's hand. The phone should instantly lock.

### Implementation for User Story 2

- [ ] T017 [US2] Implement a sensor manager in `app/src/main/java/com/n7/vibeprivacy/core/sensors/SensorDataManager.kt` to collect data from accelerometer, gyroscope, and proximity sensors.
- [ ] T018 [US2] Develop logic to detect sudden movements, tilts, or proximity changes in `app/src/main/java/com/n7/vibeprivacy/core/threats/MotionThreatDetector.kt`.
- [ ] T019 [US2] Implement alternative protection actions: "Decoy Screen" and "Device Lock" in `app/src/main/java/com/n7/vibeprivacy/services/ProtectionActions.kt`.
- [ ] T020 [US2] Create a settings screen allowing users to choose a protection mode (e.g., Paranoia, Balanced) in `app/srcsrc/main/java/com/n7/vibeprivacy/features/settings/SettingsScreen.kt`.
- [ ] T021 [US2] Update `MonitoringService` to incorporate motion-based threat detection and trigger the appropriate action based on the selected mode.

**Checkpoint**: At this point, User Stories 1 AND 2 should both work.

---

## Phase 5: User Story 3 - Customization and User Feedback (Priority: P3)

**Goal**: Allow users to customize which apps are protected, whitelist trusted faces, and view a history of detected privacy threats.

**Independent Test**: The user can add a new application to the "Critical" protection profile and see that the protection is immediately active for that app.

### Implementation for User Story 3

- [ ] T022 [P] [US3] Create a UI for managing protected applications and their `ProtectionProfile` in `app/src/main/java/com/n7/vibeprivacy/features/profiles/`.
- [ ] T023 [P] [US3] Create the Privacy Dashboard UI to display a list of `ThreatEvent` logs from the database in `app/src/main/java/com/n7/vibeprivacy/features/dashboard/DashboardScreen.kt`.
- [ ] T024 [US3] Implement the face whitelisting UI: allow a user to capture their face and save the `WhitelistedFace` data in `app/src/main/java/com/n7/vibeprivacy/features/whitelist/`.
- [ ] T025 [US3] Update the face detection logic in `FaceDetector.kt` to compare against whitelisted faces and ignore them.
- [ ] T026 [US3] Implement the "Just-in-Time" consent flow for the intruder photo feature as described in `research.md`.

**Checkpoint**: All user stories should now be independently functional.

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories.

- [ ] T027 [P] Write user documentation and help guides in the app.
- [ ] T028 Refactor and clean up code across all features.
- [ ] T029 Perform performance profiling and optimize battery consumption of the `MonitoringService`.
- [ ] T030 Validate all functionality against `quickstart.md`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies.
- **Foundational (Phase 2)**: Depends on Setup completion. BLOCKS all user stories.
- **User Stories (Phase 3+)**: All depend on Foundational phase completion.
- **Polish (Final Phase)**: Depends on all user stories being complete.

### User Story Dependencies

- **User Story 1 (P1)**: Starts after Foundational. No dependencies on other stories.
- **User Story 2 (P2)**: Starts after Foundational. Integrates with US1's service.
- **User Story 3 (P3)**: Starts after Foundational. Integrates with US1's detection and logging.

### Within Each User Story

- Core logic before UI.
- Services before features.
- Story complete before moving to next priority.

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently.

### Incremental Delivery

1. Complete Setup + Foundational.
2. Add User Story 1 → Test (MVP).
3. Add User Story 2 → Test.
4. Add User Story 3 → Test.
5. Each story adds value without breaking previous stories.
