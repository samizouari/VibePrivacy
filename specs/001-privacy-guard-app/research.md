# Research: Privacy Guard

This document addresses the clarification needed from the feature specification regarding user consent for taking photos of intruders.

## Decision: Implement "Just-in-Time" Consent with an option for Explicit Opt-In

After considering the options, the chosen approach is a combination of "Just-in-Time" consent and "Explicit Opt-In".

**Rationale**:

This hybrid approach provides the best balance of user experience, privacy, and effectiveness.

1.  **Just-in-Time Consent**: By asking for permission the first time an intrusion is detected, the user understands the context and value of the feature. This avoids presenting the user with a potentially alarming option during onboarding before they have experienced the app's core functionality.
2.  **Explicit Opt-In in Settings**: For users who decline the just-in-time consent but later decide they want the feature, an option will be available in the app's settings to enable it.

**Implementation Details**:

-   The first time a threat is detected that would trigger a photo, the app will show a dialog asking for permission to take photos of intruders in the future.
-   If the user grants permission, the app will store this preference and take photos in subsequent events.
-   If the user denies permission, the app will not ask again for a configurable period (e.g., 30 days) to avoid being intrusive.
-   All photos will be stored in the app's private, encrypted storage, accessible only through the privacy dashboard.

**Alternatives considered**:

-   **Explicit Opt-In During Onboarding**: Rejected because it may cause users to abandon the onboarding process due to privacy concerns before understanding the app's value.
-   **No Photos, Only Data**: Rejected because it diminishes the value of the "evidence" provided to the user, which is a key part of the feature's appeal.
