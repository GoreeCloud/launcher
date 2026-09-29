# GoreeCloud Launcher visual identity

## Canonical authority

All GoreeCloud Launcher logos, icons, symbols, illustrations, and artwork are canonical only in **`GoreeCloud/goreecloud-branding-assets`**.

Canonical Launcher asset:

- repository: `GoreeCloud/goreecloud-branding-assets`
- path: `products/launcher/app-icon.svg`
- pinned canonical blob: `d6768114e689058f1c911beca4050f33c96bd7c2`
- pinned branding repository revision for this synchronization: `e8dba369cd7bbaf2c2f97ed740caf0def1c2c0ee`

`branding/provenance.json` is the machine-readable record of this relationship.

This repository is a **consumer**, not an independent branding source of truth. It may contain only synchronized/generated/packaged Android derivatives required to build GoreeCloud Launcher.

## Superseded local candidate

An earlier Development slice placed a newly authored portal-style candidate under `branding/source/` and described the Launcher repository as its canonical source. That authority model is superseded. The local source is removed so it cannot compete with `goreecloud-branding-assets`.

Any future Launcher logo/icon/artwork revision must be authored, reviewed, and approved in `GoreeCloud/goreecloud-branding-assets` first. Consumer derivatives in this repository may be updated only from that canonical asset.

## Current synchronized Android derivatives

The Development APK consumes Android derivatives synchronized from `products/launcher/app-icon.svg`:

- `app/src/main/res/drawable/ic_launcher_background.xml`
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/drawable/ic_launcher_monochrome.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `app/src/main/res/mipmap-anydpi-v33/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v33/ic_launcher_round.xml`

The full-color derivative preserves the canonical cyan-to-indigo field and four white rounded application cells. The Android 13+ monochrome derivative preserves the same four-cell identity geometry for themed-icon rendering.

## Approval and release boundary

The existence of synchronized derivatives does not by itself establish production visual-identity acceptance. The canonical branding asset and required derivatives still require the applicable GoreeCloud product/visual review, small-size recognition checks, adaptive-mask checks, themed-icon checks, Light/Dark surrounding-context checks, default-Home chooser presentation, accessibility/design review, and release acceptance before Stable qualification.

If the canonical asset changes, update `branding/provenance.json`, regenerate/synchronize the platform derivatives, rerun identity/build validation, and review the rendered result. Do not edit a consumer derivative into a new canonical design.
