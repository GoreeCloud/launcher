# Workspace Portable Snapshot — Development Boundary

## Status

Development source foundation only. This checkpoint does **not** establish a complete GoreeCloud Launcher backup, restore, export, import, Everkeep integration, or production recovery path.

## Purpose

`WorkspacePortableSnapshot` provides a versioned, deterministic, framework-independent serialization boundary for the Launcher grid/page/placement model. It exists so workspace geometry can be validated and round-tripped without granting a decoder authority to mutate Room, DataStore, Android package/profile state, widgets, or the active HOME runtime.

Current format identifier:

`goreecloud-launcher-workspace-snapshot`

Current format version:

`1`

## Included state

The current snapshot contains only:

- grid column and row dimensions;
- page identities and contiguous zero-based page ranks;
- opaque item identities;
- item cell coordinates; and
- item spans.

The codec reuses `WorkspacePagedPlacement` and `WorkspaceGridPlacement` validation so duplicate page identities/ranks, duplicate items across pages, collisions, and out-of-bounds placement state are rejected rather than normalized silently.

## Deliberately excluded state

This checkpoint does not yet serialize or restore the complete Launcher state required by the product specification. In particular it does not claim coverage for:

- legacy or compatibility favorites and dock state;
- full Room item metadata and authoritative persistence records;
- folders and folder membership;
- widgets, widget IDs, or widget-provider rebinding;
- theme, wallpaper, density, or Home configuration;
- gestures;
- hidden-app state;
- search-provider settings;
- Android package availability;
- Android user/profile identity or profile rebinding;
- package/profile-derived reconstruction state; or
- other future Launcher settings that become part of the accepted backup contract.

Because those areas are absent, the repository-wide Platform Contract must continue to report Launcher backup and restore as required but missing, and product-wide export portability as unverified/unknown.

## Canonical representation and integrity

The encoded form is a bounded UTF-8 line format with canonical LF endings. Page and item identities are carried as unpadded Base64URL tokens so delimiter characters and Unicode identities do not alter the grammar. Records are emitted in deterministic order and the final record contains a SHA-256 checksum over the canonical payload.

The decoder rejects:

- snapshots larger than the one-megabyte Development limit;
- CRLF/non-canonical line endings;
- missing or malformed checksum records;
- checksum mismatches;
- unknown format identifiers or versions;
- non-canonical integers;
- grid dimensions outside the bounded range;
- malformed or non-canonical Base64URL identity tokens;
- unknown record types;
- duplicate page identities or ranks;
- non-contiguous page ranks;
- item records that reference unknown pages;
- invalid coordinate/span values; and
- any resulting workspace that fails the existing placement validator.

The checksum is an integrity/error-detection boundary, not encryption, authentication, a signature, or anti-tamper authorization.

## Privacy and authority boundary

The codec observes only already-materialized framework-independent workspace placement state supplied by its caller. It does not discover installed packages, inspect Android profiles, query user activity, contact a network service, or read arbitrary Launcher persistence.

Decoding returns a validated in-memory snapshot only. It does not write Room or DataStore state, replace the active workspace, bind widgets, launch applications, resolve profiles, or change HOME authority. A future import/restore coordinator must separately establish authorization, conflict behavior, package/profile rebinding rules, migration semantics, and transactional persistence.

## Everkeep and recovery boundary

The versioned format is a useful future input to an Everkeep-backed recovery design, but this checkpoint is not Everkeep integration. Before Launcher backup/restore can be considered implemented or verified, GoreeCloud still requires at minimum:

1. a complete and versioned product-owned backup scope;
2. explicit treatment of the excluded state above;
3. transactional restore/import behavior against the authoritative persistence layer;
4. safe widget and Android profile/package rebinding rules;
5. clean-target and upgrade/migration tests;
6. corruption, partial-state, rollback, and failure-injection tests;
7. accepted privacy/security review; and
8. successful Everkeep backup/restore acceptance evidence.

## Acceptance boundary

Passing unit tests for this codec proves only the source-level serialization and validation contract at the tested revision. It does not qualify Launcher for Stable, production recovery, user-facing import/export, or Everkeep acceptance.
