# Security policy

## Supported versions

Security fixes are made for the latest release and for `main`.

## Reporting a vulnerability

Please report security problems privately. Do not open a public issue.

Use GitHub's private vulnerability reporting: open the **Security** tab of this repository and
choose **Report a vulnerability**. Include the app version, the Android version and device, the
steps to reproduce, and what you think the impact is.

You can expect an acknowledgement within 7 days and a status update within 30 days. Fixes are
released as soon as they are ready, and the reporter is credited in the release notes unless they
prefer to stay anonymous.

## Scope

In scope: the Android app, the catalog tool, the catalog data repository, and the build and
release workflows of this repository.

Out of scope: problems in Project Gutenberg's own website, in a phone's operating system, and
issues that need a rooted or already compromised device.

## Design notes for reviewers

- No accounts, no analytics and no third-party SDKs that collect data.
- Permissions: `INTERNET` and `ACCESS_NETWORK_STATE` only.
- HTTPS only. Cleartext traffic is disabled. The only added trust anchor is the SSL.com TLS RSA
  Root CA 2022, limited to `gutenberg.org` (see `app/src/main/res/xml/network_security_config.xml`).
- Downloaded files are checked before they are kept, and the bundled catalog is verified against a
  pinned SHA-256 at build time.
- The EPUB reader's web view has JavaScript, file access and network loading turned off and serves
  book content only from inside the book file.
- Cloud backup is off (`allowBackup="false"`).
