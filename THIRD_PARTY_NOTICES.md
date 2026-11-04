# Third-party notices

FreeLibrary bundles the following third-party material.

## Libraries compiled into the app

These libraries are part of the app and are used under the Apache License, Version 2.0. The full text of the licence
is in `app/src/main/assets/licenses/Apache-2.0.txt` and ships inside the app. Each project is the work of its own
authors; follow the link for its copyright and, where it has one, its NOTICE text.

| Library | Used for | Project |
| --- | --- | --- |
| AndroidX (Core, AppCompat, ConstraintLayout, Navigation, Lifecycle, Room, DataStore, Paging) | The app's structure, database, settings and lists | https://developer.android.com/jetpack/androidx |
| Material Components for Android | Buttons, dialogs, sheets and other controls | https://github.com/material-components/material-components-android |
| Dagger and Hilt | Dependency injection | https://github.com/google/dagger |
| Coil | Loading and caching cover pictures | https://github.com/coil-kt/coil |
| Retrofit, with its kotlinx.serialization converter | The catalog update requests | https://github.com/square/retrofit |
| OkHttp and Okio | Network connections and downloads | https://github.com/square/okhttp, https://github.com/square/okio |
| kotlinx.serialization and kotlinx.coroutines | Reading the catalog and backups; background work | https://github.com/Kotlin/kotlinx.serialization, https://github.com/Kotlin/kotlinx.coroutines |
| Kotlin standard library | The Kotlin language runtime | https://github.com/JetBrains/kotlin |

The libraries these depend on (for example `javax.inject` and the JetBrains annotations) are also under the Apache
License, Version 2.0. This list is kept by hand from `gradle/libs.versions.toml`: when a library is added to the
app, add it here. Libraries used only to build the app, to run the tests or by the catalog tool are not part of
the app and are not listed. FreeLibrary itself is under the GNU General Public License, version 3, which accepts
code under the Apache License, Version 2.0.

## Literata (fonts)

- Files: `app/src/main/res/font/literata_medium.ttf` and `app/src/main/res/font/literata_semibold.ttf`
- Copyright 2017 The Literata Project Authors (https://github.com/googlefonts/literata)
- License: SIL Open Font License, Version 1.1. The full text is in
  `app/src/main/assets/licenses/OFL-Literata.txt` and ships inside the app.
- Source: the Google Fonts repository (`ofl/literata`).
- Modifications: the two files are static instances of the upstream variable font (weights 500
  and 600 at optical size 28), reduced to the Latin characters the app uses.

## Reading fonts (Settings, Appearance)

Ten fonts can be chosen in the readers, for plain text books and for EPUB books. Each is licensed under the SIL
Open Font License, Version 1.1; the full text of each is in `app/src/main/assets/licenses/` and ships inside the app.
The files are in `app/src/main/res/font/`.

| Font | Copyright | Source | Licence text |
| --- | --- | --- | --- |
| Literata | 2017 The Literata Project Authors | Google Fonts `ofl/literata` | `OFL-Literata.txt` |
| Noto Serif | 2022 The Noto Project Authors | Google Fonts `ofl/notoserif` | `OFL-NotoSerif.txt` |
| Lora | 2011 The Lora Project Authors (Reserved Font Name "Lora") | Google Fonts `ofl/lora` | `OFL-Lora.txt` |
| Source Serif 4 | 2014 The Source Serif 4 Project Authors | Google Fonts `ofl/sourceserif4` | `OFL-SourceSerif4.txt` |
| Crimson Pro | 2018 The Crimson Pro Project Authors | Google Fonts `ofl/crimsonpro` | `OFL-CrimsonPro.txt` |
| EB Garamond | 2017 The EB Garamond Project Authors | Google Fonts `ofl/ebgaramond` | `OFL-EBGaramond.txt` |
| Libre Baskerville | 2012 The Libre Baskerville Project Authors (Reserved Font Name "Libre Baskerville") | Google Fonts `ofl/librebaskerville` | `OFL-LibreBaskerville.txt` |
| Open Sans | 2020 The Open Sans Project Authors | Google Fonts `ofl/opensans` | `OFL-OpenSans.txt` |
| Atkinson Hyperlegible | 2020 Braille Institute of America, Inc. | Google Fonts `ofl/atkinsonhyperlegible` | `OFL-Atkinson.txt` |
| OpenDyslexic | 2019 Abbie Gonzalez, with Reserved Font Name "OpenDyslexic" | https://github.com/antijingoist/opendyslexic | `OFL-OpenDyslexic.txt` |

- Modifications: Lora, Libre Baskerville, Atkinson Hyperlegible and OpenDyslexic, which declare Reserved Font Names
  or are static families, are bundled exactly as published. Literata, Noto Serif, Source Serif 4, Crimson Pro, EB
  Garamond and Open Sans, which declare no Reserved Font Name, are reduced to Latin, Latin Extended, Greek, Cyrillic
  and Vietnamese characters, with the optical size and width axes fixed (the weight axis is kept).
- Text in other scripts, such as Persian, Arabic or Chinese, is drawn by the phone's own font.

## Fraunces (font)

- File: `app/src/main/res/font/fraunces_display.ttf`
- Copyright 2018 The Fraunces Project Authors (https://github.com/undercasetype/Fraunces)
- License: SIL Open Font License, Version 1.1. The full text is in
  `app/src/main/assets/licenses/OFL-Fraunces.txt` and ships inside the app.
- Source: the Google Fonts repository (`ofl/fraunces`).
- Modifications: the file is a static instance of the upstream variable font (weight 600, optical
  size 36, softness 50, no wonk), reduced to the Latin characters the app uses.

## SSL.com TLS RSA Root CA 2022 (root certificate)

- File: `app/src/main/res/raw/ssl_com_tls_rsa_root_ca_2022.pem`
- A public root certificate of SSL.com, published at https://www.ssl.com/repository/
  (SHA-256 fingerprint 8F:AF:7D:2E:2C:B4:70:9B:B8:E0:B3:36:66:BF:75:A5:DD:45:B5:DE:48:0F:8E:A8:D4:BF:E6:BE:BC:17:F2:ED).
- Why: Project Gutenberg's website certificate is issued under this root, which older Android
  versions do not know. The app trusts it for gutenberg.org only (`res/xml/network_security_config.xml`).
