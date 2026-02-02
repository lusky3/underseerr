# Security Policy

## Reporting a Vulnerability

We take the security of our project seriously. If you find a vulnerability, please report it to us immediately.

### GitHub Private Reporting (Preferred)

The best way to report a vulnerability is via the [GitHub Private Vulnerability Reporting](https://github.com/lusky3/underseerr/security/advisories/new) feature. This allows you to open a private issue where we can discuss the problem and collaborate on a fix securely.

### Email (Backup)

If you cannot use GitHub vulnerability reporting, you may send an email to **<security@lusk.app>**.

* Please include a detailed description of the vulnerability.
* If possible, provide steps to reproduce the issue.
* We aim to acknowledge all reports within **48 hours**.

---

## Supported Versions

We only support the latest version of the application. Please ensure you are testing against the latest release or the `main` branch.

| Version | Supported          |
| ------- | ------------------ |
| Latest  | :white_check_mark: |
| < 1.0   | :x:                |

## Disclosure Policy

* We ask that you do not disclose the vulnerability publicly until we have had a chance to verify and fix the issue.
* We will make our best effort to keep you informed of our progress.
* Once fixed, we will credit you (with your permission) in the release notes or security advisory.

## Scope

**In Scope:**

* The Android Client Application (codebase in this repository).
* API integration logic within the client.

**Out of Scope:**

* Vulnerabilities in the Overseerr or Radarr/Sonarr server software itself (unless caused specifically by our client's misuse).
* Attacks requiring physical access to the user's unlocked device.
* Spam or social engineering attacks.
