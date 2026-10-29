# Third-party notices

FreeLibrary bundles the following third-party material.

## Literata (fonts)

- Files: `app/src/main/res/font/literata_medium.ttf` and `app/src/main/res/font/literata_semibold.ttf`
- Copyright 2017 The Literata Project Authors (https://github.com/googlefonts/literata)
- License: SIL Open Font License, Version 1.1. The full text is in
  `app/src/main/assets/licenses/OFL-Literata.txt` and ships inside the app.
- Source: the Google Fonts repository (`ofl/literata`).
- Modifications: the two files are static instances of the upstream variable font (weights 500
  and 600 at optical size 28), reduced to the Latin characters the app uses.

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
