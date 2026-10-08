# GoreeCloud Launcher product identity

## Status

GoreeCloud Launcher consumes a first-party product icon from the canonical GoreeCloud visual-asset repository. This document records consumer-side provenance and Android derivative behavior; it does **not** make `GoreeCloud/goreecloud-launcher` an artwork authority.

Production visual-identity acceptance and Stable qualification remain separate review gates.

## Canonical source

All Launcher logos, icons, symbols, illustrations, and artwork are canonical only in:

- repository: `GoreeCloud/goreecloud-branding-assets`
- asset: `products/launcher/app-icon.svg`
- pinned source blob for the current derivative synchronization: `d6768114e689058f1c911beca4050f33c96bd7c2`
- branding-repository revision observed for this synchronization: `e8dba369cd7bbaf2c2f97ed740caf0def1c2c0ee`

The machine-readable consumer record is `branding/provenance.json`.

Any future visual revision must be authored/reviewed in `goreecloud-branding-assets` first. A Launcher-repository derivative must never be edited into a competing canonical mark.

## Current identity DNA

The current canonical Launcher icon uses:

- a cyan-to-indigo rounded application field;
- four white rounded application cells arranged as a balanced 2×2 launcher grid;
- simple geometry that remains legible at small icon sizes;
- adaptive-mask compatibility without encoding a second independent outer silhouette; and
- a monochrome derivative that keeps the four-cell structure for Android themed icons.

This identity is distinct from a framework/default Android icon and from the generic GoreeCloud corporate/platform mark. It communicates Launcher/application-access semantics without copying another launcher identity.

## Android consumer derivatives

`GoreeCloud/goreecloud-launcher` carries only Android build derivatives:

- `app/src/main/res/drawable/ic_launcher_background.xml` — synchronized cyan-to-indigo gradient field;
- `app/src/main/res/drawable/ic_launcher_foreground.xml` — four outlined application cells;
- `app/src/main/res/drawable/ic_launcher_monochrome.xml` — themed-icon geometry;
- adaptive/round resources under `mipmap-anydpi-v26`; and
- Android 13+ monochrome-aware adaptive/round resources under `mipmap-anydpi-v33`.

These files are packaging/rendering derivatives, not source artwork.

## Superseded repository-local candidate

A previous Development slice authored a portal/activity-tile candidate in `branding/source/goreecloud-launcher-icon.svg` and described the Launcher repository as its canonical source. The project-wide branding authority is now explicit: `GoreeCloud/goreecloud-branding-assets` owns all logos/icons/artwork. The local source candidate is therefore removed and superseded; it must not be treated as an approved or canonical Launcher identity.

## Validation

`scripts/check_identity.py` fails closed when:

- the provenance record is missing or malformed;
- the canonical repository/path/blob metadata does not match the synchronized source;
- a competing Launcher-local canonical source path reappears;
- the Android background/foreground/monochrome derivatives are missing or lose required source-derived geometry/colors;
- adaptive or themed-icon resources lose derivative wiring; or
- the manifest no longer references the Launcher icon resources.

The check is intentionally consumer-side. It verifies what this repository claims to consume; it does not silently redefine or approve the canonical branding asset.

## Remaining acceptance

Before production/Stable visual acceptance, the canonical asset and synchronized derivatives still require the applicable rendered/device review, including adaptive masks, round launchers, monochrome/themed rendering, small-size recognition, system Home/default-app chooser presentation, Light/Dark surrounding contexts, representative densities/devices, and any applicable accessibility/design review.
