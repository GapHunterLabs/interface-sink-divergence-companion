<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Interface Sink Divergence Companion Changelog

## [Unreleased]

## [0.1.0]

### Added

- Combines real Class Hierarchy Analysis with taint-to-sink detection:
  flags a call through an interface-typed reference, passing tainted
  HTTP input, where some real implementations log it unsanitized and
  at least one other real implementation never does.

[Unreleased]: https://github.com/GapHunterLabs/interface-sink-divergence-companion/compare/0.1.0...HEAD
[0.1.0]: https://github.com/GapHunterLabs/interface-sink-divergence-companion/commits/0.1.0
