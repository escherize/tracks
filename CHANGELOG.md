# Change Log
All notable changes to this project will be documented in this file. This change log loosely follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [1.0.1] - 2016-1-17
### Breaking Changes
- `track` is now a thin layer over let + I reccomend using track/let more often.

## [0.1.7] - 2016-11-03
### Breaking Changes
- `track` is now a macro and works only with symbols
### Added
- `track` is now implemented in terms of let


## [0.1.5] - 2016-11-03
### Added
- `tracks.core/let` - let macro!

## [0.1.4] - 2016-10-16
### Added
- Tracks now runs on ClojureScript!
### Removed
- `tracks.core/tracks` is no longer an alias for `tracks.core/track`

## [0.1.3] - 2016-10-08
### Fixed
- Make tracks non-destructive, i.e. do not edit keys that are not operated on.
### Changed
- Improve docs

## [0.1.2] - 2016-10-04
### Added
- Added optional function-map to tracks.
### Changed
- Improve docs


## [0.1.0] - 2016-10-04
### Changed
- First relase of tracks
