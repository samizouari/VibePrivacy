# Data Model: Privacy Guard

This document outlines the data entities for the Privacy Guard application.

## Entities

### ProtectionProfile

Represents a user-defined protection configuration for a specific application.

| Field | Type | Description |
|---|---|---|
| `id` | `Int` | Primary key. |
| `appName` | `String` | The name of the application (e.g., "Slack", "Gmail"). |
| `packageName` | `String` | The package name of the application (e.g., "com.slack"). |
| `sensitivityLevel` | `String` | The sensitivity level ("Critical", "Sensitive", "Normal", "Public"). |
| `protectionAction` | `String` | The action to take on threat detection ("Blur", "Decoy", "Lock"). |

### ThreatEvent

Represents a log of a detected privacy threat.

| Field | Type | Description |
|---|---|---|
| `id` | `Int` | Primary key. |
| `timestamp` | `Long` | The time the threat was detected. |
| `threatType` | `String` | The type of threat detected (e.g., "MultipleFaces", "SuspiciousSound", "SuddenMovement"). |
| `sensorData` | `String` | A JSON string containing relevant sensor data at the time of the event. |
| `actionTaken` | `String` | The protection action that was triggered. |
| `photoPath` | `String` | (Optional) The path to the encrypted photo of the intrusion, if taken. |

### WhitelistedFace

Represents a trusted face that will not trigger a protection action.

| Field | Type | Description |
|---|---|---|
| `id` | `Int` | Primary key. |
| `name` | `String` | A user-assigned name for the trusted face. |
| `faceEncoding` | `ByteArray` | The encoded representation of the trusted face, used for comparison. |
