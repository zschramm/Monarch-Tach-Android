# Third-party notices and provenance

Application source: GPL-3.0-only. Copyright (C) 2026 Monarch Utility contributors. Full text in LICENSE, from https://www.gnu.org/licenses/gpl-3.0.txt. This does not relicense third-party components.

- usb-serial-for-android 3.9.0: MIT. Copyright 2011-2013 Google Inc. and 2013 Mike Wakerly. Original notice in licenses/usb-serial-for-android-MIT.txt, from https://raw.githubusercontent.com/mik3y/usb-serial-for-android/v3.9.0/LICENSE.txt. Dependency resolved through published JitPack artifact.
- AndroidX runtime components: Apache-2.0. Source https://android.googlesource.com/platform/frameworks/support/; artifacts from Google Maven.
- Kotlin standard library 2.1.20 and JetBrains annotations: Apache-2.0. Sources https://github.com/JetBrains/kotlin and https://github.com/JetBrains/java-annotations.
- Gradle wrapper 8.11.1: Apache-2.0, Gradle Inc. and contributors. Copied from an existing build-tool installation. Wrapper SHA256: 2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046. Distribution SHA256 pinned in wrapper properties. Source https://github.com/gradle/gradle/tree/v8.11.1.
- Android build tools/SDK: external build prerequisites, not included in source export.
- JUnit 4.13.2: EPL-1.0; Hamcrest 1.3: BSD-3-Clause. Test-only dependencies, not part of app runtime or vendored. Sources https://github.com/junit-team/junit4 and https://github.com/hamcrest/JavaHamcrest.

Apache-2.0 text is included at licenses/Apache-2.0.txt from https://www.apache.org/licenses/LICENSE-2.0.txt. Preserve embedded dependency notices when distributing built packages.

No manufacturer binaries, manuals, logos or screenshots are bundled. Protocol implemented from public manuals linked in README. UI uses Android platform widgets, no third-party image/font assets. Monarch/ACT-1B identify compatibility only; no affiliation or endorsement is claimed. Tests use synthetic identifiers, not instrument serial numbers or user records.

## Resolved runtime inventory (0.2.0)

usb-serial-for-android 3.9.0 (MIT); Kotlin stdlib/common 2.1.20, JetBrains annotations 13.0, kotlinx-coroutines android/core/core-jvm 1.6.4, Guava listenablefuture 1.0 (Apache-2.0); AndroidX annotation/annotation-jvm 1.9.1, annotation-experimental 1.4.0, core 1.13.1, collection 1.0.0, concurrent-futures 1.1.0, interpolator 1.0.0, lifecycle runtime/common 2.6.2, arch.core common/runtime 2.2.0, profileinstaller 1.3.0, startup-runtime 1.1.1, tracing 1.0.0, versionedparcelable 1.1.1 (Apache-2.0).

Guava license inherited from guava-parent 26.0-android; source https://github.com/google/guava. Coroutines source https://github.com/Kotlin/kotlinx.coroutines. Runtime dependency POM license metadata and upstream USB license were checked. Gradle wrapper's original bundled license is retained at licenses/Gradle-wrapper-LICENSE.txt.
