# Security Policy

## Supported versions

| Version | Supported |
| --- | --- |
| 1.4 | Yes |

## Reporting a vulnerability

Please report security issues **privately** using GitHub Security Advisories on
this repository (Security → Advisories → New draft security advisory). Do not
open a public issue with exploit details.

Include:

- app version and Android version;
- phone model;
- steps to reproduce;
- impact and any suggested fix.

## Scope

IR Voider has no network permission and no server component. Relevant risks are
local, for example:

- a malicious `.ir` file causing a crash or excessive memory use;
- an imported pattern that behaves unexpectedly on a device;
- local data handling.

## Out of scope

- IR interference with devices you do not own.
- Hardware range limitations of the phone's IR LED.
- Forks or modified builds not published by the maintainer.

No bug bounty is offered. Reports are handled on a best-effort basis.
