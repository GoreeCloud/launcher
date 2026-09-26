# GoreeCloud Launcher Icon Pack Authoring

**Status:** Development contract  
**Audience:** icon-pack creators and Launcher contributors

GoreeCloud Launcher supports locally installed Android icon packs. The Launcher does not require a GoreeCloud account, remote registry, network request, analytics service, or creator-hosted API to apply a pack.

## Discovery

An icon-pack application should expose at least one compatible theme action. The preferred GoreeCloud action is:

`com.goreecloud.launcher.ICON_PACK`

For compatibility with existing Android icon packs, Launcher also discovers packages exposing these common actions:

- `org.adw.launcher.THEMES`
- `com.novalauncher.THEME`
- `com.anddoes.launcher.THEME`
- `com.gau.go.launcherex.theme`

A minimal preferred manifest declaration is:

```xml
<intent-filter>
    <action android:name="com.goreecloud.launcher.ICON_PACK" />
    <category android:name="android.intent.category.DEFAULT" />
</intent-filter>
```

## App-to-icon mapping

Launcher reads the conventional `appfilter.xml` format. The file may be provided as an XML resource named `appfilter` or as `assets/appfilter.xml`.

Each mapped icon uses an `item` entry:

```xml
<resources>
    <item
        component="ComponentInfo{com.example.app/com.example.app.MainActivity}"
        drawable="example_app" />
</resources>
```

The `drawable` value must name a local drawable or mipmap resource contained in the icon-pack APK.

Launcher also accepts flattened Android component strings such as:

```xml
<item
    component="com.example.app/.MainActivity"
    drawable="example_app" />
```

## Fallback behavior

A pack does not need to contain every installed application. When no valid mapping or drawable exists, Launcher falls back to the original Android-provided app icon. A missing or malformed mapping must not make the application disappear.

## Shapes

Icon packs provide artwork; Launcher icon-shape preferences are applied independently. Users can currently choose Rounded square, Original/no Launcher mask, Squircle, Circle, or Teardrop. Rounded square is the default.

Creators should therefore keep important artwork inside a conservative optical safe area and avoid placing essential detail at the extreme corners.

## Privacy and security boundary

Launcher reads only local package metadata and icon-pack resources needed for discovery and rendering. Icon packs do not receive Launcher search history, app usage counts, account data, contacts, messages, files, or remote credentials through this contract.

## Development boundary

This contract is part of the Launcher Development line. Compatibility claims are limited to the implemented discovery and `appfilter.xml` behavior at the exact source revision under review. Representative-device acceptance and broader icon-pack ecosystem compatibility remain separate validation gates.
