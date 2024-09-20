# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

## [2024.09.20]
### Fixed
- Android client build.
### Upgraded
- Keyple components
  - keyple-service-java-lib `3.2.1` -> `3.3.1`
  - keyple-distributed-network-java-lib `2.3.1` -> `2.5.1`
  - keyple-distributed-local-java-lib `2.3.1` -> `2.5.1`
  - keyple-card-calypso-java-lib `3.1.1` -> `3.1.3`

## [2024.04.23]
### Upgraded
- keyple-demo-common-lib `2.0.0-SNAPSHOT` -> `2.0.1-SNAPSHOT`
- All Keyple components (compiled to java 8)
  - keypop-reader-java-api `2.0.0` -> `2.0.1`
  - keypop-calypso-card-java-api `2.0.0` -> `2.1.0`
  - keypop-calypso-crypto-legacysam-java-api `0.3.0` -> `0.6.0`
  - keyple-common-java-api `2.0.0` -> `2.0.1`
  - keyple-util-java-lib `2.3.1` -> `2.4.0`
  - keyple-service-java-lib `3.0.1` -> `3.2.1`
  - keyple-card-calypso-java-lib `3.0.1` -> `3.1.1`
  - keyple-card-calypso-crypto-legacysam-java-lib `0.4.0` -> `0.7.0`
  - keyple-plugin-android-nfc-java-lib `2.0.1` -> `2.2.0`
- Other components (Gradle wrapper, Android Gradle Plugin, etc.)

## [2023.12.06]
### Upgraded
- Calypsonet Terminal Reader API `1.3.0` -> Keypop Reader API `2.0.0`
- Calypsonet Terminal Calypso API `1.8.0` -> Keypop Calypso Card API `2.0.0`
- Keyple Service Library `2.3.1` -> `3.0.1`
- Keyple Calypso Card Library `2.3.5` -> `3.0.1`
- Keyple Util Library `2.3.0` -> `2.3.1`

### Added
New dependencies
- Keypop Crypto Legacy SAM API `0.3.0`
- Keyple Calypso Crypto LegacySAM Library `0.4.0`

## [2023.06.01]
### Upgraded
- `keyple-demo-common-lib:2.0.0-SNAPSHOT`
- `calypsonet-terminal-reader-java-api:1.3.0`
- `calypsonet-terminal-calypso-java-api:1.8.0`
- `keyple-service-java-lib:2.3.1`
- `keyple-card-calypso-java-lib:2.3.5`

## [2023.02.24]
### Upgraded
- `calypsonet-terminal-reader-java-api:1.2.0`
- `calypsonet-terminal-calypso-java-api:1.6.0`
- `keyple-service-java-lib:2.1.3`
- `keyple-card-calypso-java-lib:2.3.2`
- `com.google.code.gson:gson:2.10.1`

## [2022.11.18]
### Fixed
- Various erroneous behaviors and displays.
### Added
- Setting screen to indicate the type of device used.
- CI: `java-test` GitHub action.
- New location "Barcelona".
### Changed
- Build process using flavours mechanism replaced by standard single build process.
- Major refactoring of the source code.
### Upgraded
- `keyple-demo-common-lib:1.0.0-SNAPSHOT`
- `calypsonet-terminal-reader-java-api:1.1.0`
- `calypsonet-terminal-calypso-java-api:1.4.1`
- `keyple-service-java-lib:2.1.1`
- `keyple-card-calypso-java-lib:2.2.5`
- `keyple-plugin-android-nfc-java-lib:2.0.1`
- `keyple-plugin-cna-coppernic-cone2-java-lib:2.0.2`
- `keyple-plugin-cna-famoco-se-communication-java-lib:2.0.2`
- `keyple-plugin-cna-bluebird-specific-nfc-java-lib-2.1.1-mock` (mocked library)
- `keyple-plugin-cna-flowbird-android-java-lib-2.0.2-mock` (mocked library)
- `keyple-util-java-lib:2.3.0`

[Unreleased]: https://github.com/calypsonet/keyple-android-demo-control/compare/2024.09.20...HEAD
[2024.09.20]: https://github.com/calypsonet/keyple-android-demo-control/compare/2024.04.23...2024.09.20
[2024.04.23]: https://github.com/calypsonet/keyple-android-demo-control/compare/2023.12.06...2024.04.23
[2023.12.06]: https://github.com/calypsonet/keyple-android-demo-control/compare/2023.06.01...2023.12.06
[2023.06.01]: https://github.com/calypsonet/keyple-android-demo-control/compare/2023.02.24...2023.06.01
[2023.02.24]: https://github.com/calypsonet/keyple-android-demo-control/compare/2022.11.18...2023.02.24
[2022.11.18]: https://github.com/calypsonet/keyple-android-demo-control/compare/v2021.11...2022.11.18