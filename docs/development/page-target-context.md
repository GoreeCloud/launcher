# Home Page Move Target Context — Development

The app move-to-page menu now shows the same authoritative page context used by the Home page switcher. A destination is labeled with its page number plus current app and unsupported-item counts before the move is requested.

The label is derived read-only from the terminal Room workspace projection; it does not create a second page state or mutate workspace contents. Unsupported workspace items remain visible in the count instead of being silently omitted.

Existing move authority, exact-cell rules, page ordering, and Room persistence remain unchanged.

This slice does not add folders/widgets, new mutation semantics, synchronization authority, production Room cutover acceptance, or Stable qualification.
