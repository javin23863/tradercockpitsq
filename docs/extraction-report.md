# Extraction report

## Input

- source archive: `C:\Users\MSI\Downloads\SQX_144_2953_win_20260601.zip`
- extracted source directory: `C:\Users\MSI\Downloads\SQX_144_2953_win_20260601`
- product/build: StrategyQuant X Build 144.2953
- package date in the archive name: June 1, 2026
- primary engine archive: `internal\libs\SQTradingLib.jar`
- primary building-block archive: `internal\libs\Snippets.jar`

The archive contains 9,977 files. The source extraction is organized by
responsibility rather than mirroring the opaque installation layout.

## Included source inventory

| Repository root | Java files | Approx. source bytes | Origin |
| --- | ---: | ---: | --- |
| `sources/engine-core` | 844 | 4,256,961 | `SQTradingLib.jar`, Vineflower |
| `sources/platform-runtime` | 223 | 1,217,436 | embedded `SQLib` runtime, CFR with targeted syntax repairs |
| `sources/launcher-app` | 7 | 48,428 | embedded application archive, CFR |
| `sources/indicators-building-blocks` | 929 | 2,322,793 | `Snippets.jar`, Vineflower |
| `sources/data-lib` | 178 | 702,662 | `SQDataLib.jar`, Vineflower |
| `sources/grid-lib` | 57 | 154,694 | `SQGridLib2.jar`, Vineflower |
| `sources/jobs-lib` | 8 | 7,153 | `SQJobsLib.jar`, Vineflower |
| `sources/plugin-api` | 17 | 17,843 | `SQPluginLib.jar`, Vineflower |
| `sources/web-gui-lib` | 25 | 166,642 | `SQWebGUILib.jar`, Vineflower |
| `sources/wizard-business` | 22 | 54,150 | `SQWizardBusiness.jar`, Vineflower |
| `sources/plugins` | 404 | 2,530,133 | all 176 plugin archives, Vineflower |
| **Total** | **2,714** | **11,478,895** | source-derived Java |

The reference layer contains 9,747 files, approximately 19,173,278 bytes,
including plugin assets, templates, snippet extensions, workflow exports,
readable custom-indicator material, extracted readable `internal.dat` entries,
and readable resources/service descriptors from vendor archives.

## Decompiler provenance

- Vineflower 1.12.0 was used for the core and vendor archives.
- CFR 0.152 was used for the recovered embedded `SQLib` and launcher archives,
  where its output preserved fewer structural issues.
- CFR SHA-256: `F686E8F3DED377D7BC87D216A90E9E9512DF4156E75B06C655A16648AE8765B`
- The exact embedded runtime was recovered from the installed package into a
  temporary working location for decompilation; the recovered binary archives
  are not part of this repository.
- A small number of decompiler artifacts were repaired where the intended
  control flow was unambiguous. The repaired files are listed in the working
  notes from the extraction and remain source-derived, not original source.

## Deliberate exclusions

- no SQX installer
- no license key or activation material
- no activation server
- no unmodified SQX application binary
- no copied third-party dependency JARs
- binary-only custom-indicator files were not copied into the reference layer

Readable source/configuration/template material was retained where it helps
reproduce module behavior without redistributing excluded binaries.

## Verification status

The source inventory check verifies all expected source roots and representative
classes for the builder, backtester, optimizer, genetic engine, Monte Carlo,
robustness, indicators, building blocks, stops, and task plugins.

The complete decompiled tree was also attempted against local dependency
classpaths. It is not currently compile-clean: decompiler reconstruction leaves
generic/type-inference and synthetic-method errors across the combined source
set. This branch therefore claims complete source/reference coverage and
provenance, not a finished clean-room build. The next engineering phase should
normalize packages and generics behind a tested backend API, then add focused
parity tests against known SQX fixtures.
