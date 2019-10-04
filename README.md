<p align="center">
    <img alt="Logo" src="assets/logo.svg" width="25%" />
</p>

<h1 align="center">
    Gradle Play Publisher
</h1>

<p align="center">
    <a href="https://circleci.com/gh/Triple-T/gradle-play-publisher">
        <img src="https://circleci.com/gh/Triple-T/gradle-play-publisher.svg?style=svg" />
    </a>
    <a href="https://plugins.gradle.org/plugin/com.github.triplet.play">
        <img src="https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com/github/triplet/play/com.github.triplet.play.gradle.plugin/maven-metadata.xml.svg?label=Gradle%20Plugins%20Portal" />
    </a>
</p>

Gradle Play Publisher is Android's unofficial release automation Gradle Plugin. It can do anything
from building, uploading, and then promoting your App Bundle or APK to publishing app listings and
other metadata.

## Table of contents

1. [Quickstart guide](#quickstart-guide)
1. [Prerequisites](#prerequisites)
   1. [Initial Play Store upload](#initial-play-store-upload)
   1. [Signing configuration](#signing-configuration)
   1. [Service Account](#service-account)
1. [Basic setup](#basic-setup)
   1. [Installation](#installation)
   1. [Authenticating Gradle Play Publisher](#authenticating-gradle-play-publisher)
1. [Task organization](#task-organization)
1. [Managing artifacts](#managing-artifacts)
   1. [Common configuration](#common-configuration)
   1. [Publishing an App Bundle](#publishing-an-app-bundle)
   1. [Publishing APKs](#publishing-apks)
   1. [Uploading an Internal Sharing artifact](#uploading-an-internal-sharing-artifact)
   1. [Promoting artifacts](#promoting-artifacts)
   1. [Handling version conflicts](#handling-version-conflicts)
1. [Managing Play Store metadata](#managing-play-store-metadata)
   1. [Quickstart](#quickstart)
   1. [Directory structure](#directory-structure)
   1. [Publishing listings](#publishing-listings)
   1. [Publishing in-app products](#publishing-in-app-products)
1. [Working with product flavors](#working-with-product-flavors)
   1. [Disabling publishing](#disabling-publishing)
   1. [Combining artifacts into a single release](#combining-artifacts-into-a-single-release)
   1. [Using multiple Service Accounts](#using-multiple-service-accounts)
1. [Advanced topics](#advanced-topics)
   1. [Using CLI options](#using-cli-options)
   1. [Encrypting Service Account keys](#encrypting-service-account-keys)
   1. [Using HTTPS proxies](#using-https-proxies)

## Quickstart guide

1. Upload the first version of your APK or App Bundle using the
   [Google Play Console](https://play.google.com/apps/publish)
1. [Create a Google Play Service Account](#service-account)
1. [Sign your release builds](https://developer.android.com/studio/publish/app-signing#gradle-sign)
   with a valid `signingConfig`
1. [Add and apply the plugin](#installation)
1. [Authenticate GPP](#authenticating-gradle-play-publisher)

## Prerequisites

### Initial Play Store upload

The first APK or App Bundle needs to be uploaded via the Google Play Console because registering the
app with the Play Store cannot be done using the Play Developer API. For all subsequent uploads and
changes, GPP may be used.

### Signing configuration

To successfully upload apps to the Play Store, they must be signed with your developer key. Make
sure you have
[a valid signing configuration](https://developer.android.com/studio/publish/app-signing#gradle-sign).

### Service Account

To use GPP, you must create a service account with access to the Play Developer API:

1. If you don't already have one, create a GCP project for your app(s)
1. Create a
   [service account key](https://console.cloud.google.com/apis/credentials/serviceaccountkey)
   1. Select `New service account`
   1. Give it a name, but don't select any roles
   1. Leave JSON checked
   1. If it asks for roles, continue without selecting any
1. Move the downloaded JSON credentials into your project and
   [tell GPP about it](#authenticating-gradle-play-publisher)
1. [Link your developer account](https://play.google.com/apps/publish#ApiAccessPlace) to the GCP
   project in which you created the service account
1. Give your service account
   [permissions to publish apps](https://play.google.com/apps/publish#AdminPlace) on your behalf
   1. Click `Invite new user`
   1. Copypasta the service account email (you can find it in the JSON credentials)
   1. Don't touch the roles
   1. Specify which apps the service account should have access to:
      <img alt="Minimum Service Account permissions" src="assets/min-perms.png" width="66%" />

## Basic setup

### Installation

Apply the plugin to each individual `com.android.application` module where you want to use GPP
through the `plugins {}` DSL:

<details open><summary>Kotlin</summary>

```kt
plugins {
    id("com.android.application")
    id("com.github.triplet.play") version "2.4.2"
}
```

</details>

<details><summary>Groovy</summary>

```groovy
plugins {
    id 'com.android.application'
    id 'com.github.triplet.play' version '2.4.2'
}
```

</details>

#### Snapshot builds

If you're prepared to cut yourself on the bleeding edge of GPP development, snapshot builds are
available from
[Sonatype's `snapshots` repository](https://oss.sonatype.org/content/repositories/snapshots/com/github/triplet/gradle/play-publisher/):

<details open><summary>Kotlin</summary>

```kt
buildscript {
    repositories {
        // ...
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }

    dependencies {
        // ...
        classpath("com.github.triplet.gradle:play-publisher:2.5.0-SNAPSHOT")
    }
}
```

</details>

<details><summary>Groovy</summary>

```groovy
buildscript {
    repositories {
        // ...
        maven { url 'https://oss.sonatype.org/content/repositories/snapshots' }
    }

    dependencies {
        // ...
        classpath 'com.github.triplet.gradle:play-publisher:2.5.0-SNAPSHOT'
    }
}
```

</details>

### Authenticating Gradle Play Publisher

After you've gone through the [Service Account setup](#service-account), you should have a JSON file
with your private key. Add a `play` block alongside your `android` one with the file's location:

```kt
android { ... }

play {
    serviceAccountCredentials = file("your-key.json")
}
```

## Task organization

GPP follows the Android Gradle Plugin's naming convention: `[action][Variant][Thing]`. For example,
`publishPaidReleaseBundle` will be generated if have a `paid` product flavor.

Lifecycle tasks to publish multiple product flavors at once are also available. For example,
`publishBundle` publishes all variants.

To find available tasks, run `./gradlew tasks --group publishing` and use
`./gradlew help --task [task]` where `task` is something like `publishBundle` to get more detailed
documentation for a specific task.

> Note: if a task conflict occurs, say with the `maven-publish` plugin for example, be sure to apply
> the GPP plugin *last*. Conflicting tasks will then be prefixed with `gpp` (ex: `publish` ->
> `gppPublish`).

## Managing artifacts

GPP supports uploading both the App Bundle and APK. Once uploaded, GPP also supports promoting those
artifacts to different tracks.

### Common configuration

Several options are available to customize how your artifacts are published:

* `track` is the target stage for an artifact, i.e. `internal`/`alpha`/`beta`/`production` or any
  custom track
  * Defaults to `internal`
* `releaseStatus` is the type of release, i.e. `completed`/`draft`/`inProgress`/`halted`
  * Defaults to `completed`
* `userFraction` is the percentage of users who will receive a staged release
  * Defaults to `0.1` aka 10%
  * **Note:** the `userFraction` is only applicable where `releaseStatus=[inProgress/halted]`

Example configuration:

```kt
play {
    // Overrides defaults
    track = "production"
    userFraction = 0.5
    releaseStatus = "inProgress"

    // ...
}
```

#### Uploading release notes

While GPP can automatically build and find your artifact, you'll need to tell the plugin where to
find your release notes.

Add a file under `src/[sourceSet]/play/release-notes/[language]/[track].txt` where `sourceSet`
is a [full variant name](https://developer.android.com/studio/build/build-variants#sourceset-build),
`language` is one of the
[Play Store supported codes](https://support.google.com/googleplay/android-developer/answer/3125566),
and `track` is the channel you want these release notes to apply to (or `default` if unspecified).

As an example, let's assume you have these two different release notes:

```
src/main/play/release-notes/en-US/default.txt
.../beta.txt
```

When you publish to the beta channel, the `beta.txt` release notes will be uploaded. For any other
channel, `default.txt` will be uploaded.

> Note: the Play Store limits your release notes to a maximum of 500 characters.

#### Uploading developer facing release names

The Play Console supports customizing release names. These aren't visible to users, but may be
useful for internal processes. Similar to release notes, release names may be specified by placing
a `[track].txt` file in the `release-names` directory under your `play` folder. For example, here's
a custom release name for the alpha track in the `play/release-names/alpha.txt` file:

```
My custom release name
```

There is also a `--release-name` CLI option for quick access. For example,
`./gradlew publishBundle --release-name "Hello World!"`.

> Note: the Play Store limits your release names to a maximum of 50 characters.

#### Uploading a pre-existing artifact

By default, GPP will build your artifact from source. In advanced use cases, this might not be the
desired behavior. For example, if you need to inject translations into your APK or App Bundle after
building it but before publishing it. Or perhaps you simply already have an artifact you wish to
publish. GPP supports this class of use cases by letting you specify a directory in which
publishable artifacts may be found:

```kt
play {
    // ...
    artifactDir = file("path/to/apk-or-app-bundle/dir")
}
```

For quick access, you can also use the `--artifact-dir` CLI option:

```sh
./gradlew publishBundle --artifact-dir path/to/app-bundle/dir
```

> Note: all artifacts in the specified directory will be published.

#### Retaining artifacts

GPP supports keeping around old artifacts such as OBB files or WearOS APKs:

```kt
play {
    // ...
    retain {
        artifacts = listOf(123) // Old APK version code
        mainObb = 123 // Old main OBB version code
        patchObb = 123 // Old patch OBB version code
    }
}
```

### Publishing an App Bundle

Run `./gradlew publishBundle`.

#### Defaulting to the App Bundle

You'll notice that if you run `./gradlew publish`, it uploads an APK by default. To change this,
default to the App Bundle:

```kt
play {
    // ...
    defaultToAppBundles = true
}
```

### Publishing APKs

Run `./gradlew publishApk`. Splits will be uploaded if available.

### Uploading an Internal Sharing artifact

Run `./gradlew uploadReleasePrivateBundle` for App Bundles and `./gradlew uploadReleasePrivateApk`
for APKs. To upload an existing artifact, read about
[how to do so](#uploading-a-pre-existing-artifact).

### Promoting artifacts

Existing releases can be promoted and/or updated to the [configured track](#common-configuration)
with `./gradlew promoteArtifact`.

By default, the track *from* which to promote a release is determined by the most unstable channel
that contains a release. Example: if the alpha channel has no releases, but the beta and prod
channels do, the beta channel will be picked. To configure this manually, use the `fromTrack`
property:

```kt
play {
    // ...
    fromTrack = "alpha"
}
```

Similarly, the track *to* which to promote a release defaults to the `promoteTrack` property. If
unspecified, the `track` property will be used instead. Example configuration:

```kt
play {
    // ...
    promoteTrack = "beta"
}
```

If you need to execute a one-time promotion, you can use the CLI args. For example, this is how you
would promote an artifact from the alpha ➡️ beta track with only 25% of users getting the release:

```sh
./gradlew promoteArtifact \
  --from-track alpha --promote-track beta \
  --release-status inProgress --user-fraction .25
```

### Handling version conflicts

If an artifact already exists with a version code greater than or equal to the one you're trying to
upload, an error will be thrown when attempting to publish the new artifact. You have two options:

* Ignore the error and continue (`ignore`)
* Automatically pick the correct version code so you don't have to manually update it (`auto`)

Example configuration:

```kt
play {
    // ...
    resolutionStrategy = "ignore"
}
```

#### Post-processing outputs sanitized by auto resolution

For example, you could update you app's version name based on the new version code:

```kt
play {
    // ...
    resolutionStrategy = "auto"
    outputProcessor { // this: ApkVariantOutput
        versionNameOverride = "$versionNameOverride.$versionCode"
    }
}
```

## Managing Play Store metadata

GPP supports uploading any metadata you might want to change with each release, from screenshots and
descriptions to in-app purchases and subscriptions.

### Quickstart

GPP includes a bootstrap task that pulls down your existing listing and initializes everything for
you. To use it, run `./gradlew bootstrap`.

> Note: if you have a pre-existing `play` folder, it will be reset.

### Directory structure

GPP follows the Android Gradle Plugin's source set
[guidelines and priorities](https://developer.android.com/studio/build/build-variants#sourceset-build).
`src/[sourceSet]/play` is the base directory for Play Store metadata. Since `main` is the most
common source set, it will be assumed in all following examples.

In addition to merging metadata across variants, GPP merges translations. That is, if a resources is
provided in a default language such as `en-US` but not in `fr-FR`, the resource will be copied over
when uploading French metadata.

### Publishing listings

Run `./gradlew publishListing`.

#### Uploading global app metadata

Base directory: `play`

File | Description
--- | ---
`contact-email.txt` | Developer email
`contact-phone.txt` | Developer phone
`contact-website.txt` | Developer website
`default-language.txt` | The default language for both your Play Store listing and translation merging as described above

#### Uploading text based listings

Base directory: `play/listings/[language]` where `language` is one of the
[Play Store supported codes](https://support.google.com/googleplay/android-developer/answer/3125566)

File | Description | Character limit
--- | --- | ---
`title.txt`| App title | 50
`short-description.txt` | Tagline | 80
`full-description.txt` | Full description | 4000
`video-url.txt` | Youtube product video | N/A

#### Uploading graphic bases listings

Directory: `play/listings/[language]/graphics` where `language` is defined as in the previous
section

Image files are organized a bit differently than in previous sections. Instead of the file name, the
parent directory's name is used as the media type. This is because multiple images may be provided
for the same media type. While file names are arbitrary, they will be uploaded in alphabetical order
and presented on the Play Store as such. Therefore, we recommend using a number as the file name
(`1.png` for example). Both PNG and JPEG images are supported.

Directory | Max # of images | Image dimension constraints (px)
--- | --- | ---
`icon` | 1 | 512x512
`feature-graphic` | 1 | 1024x500
`promo-graphic` | 1 | 180x120
`phone-screenshots` | 8 | [320..3840]x[320..3840]
`tablet-screenshots` | 8 | [320..3840]x[320..3840]
`large-tablet-screenshots` | 8 | [320..3840]x[320..3840]
`tv-banner` | 1 | 1280x720
`tv-screenshots` | 8 | [320..3840]x[320..3840]
`wear-screenshots` | 8 | [320..3840]x[320..3840]

### Publishing in-app products

Run `./gradlew publishProducts`.

Manually setting up in-app purchase files is not recommended. [Bootstrap them instead](#quickstart)
with `./gradlew bootstrap --products`.

## Working with product flavors

When working with product flavors, granular configuration is key. GPP provides varying levels of
granularity to best support your needs, all through the `playConfigs` block:

<details open><summary>Kotlin</summary>

```kt
play {
    // In a simple app, this play block is all you'll need. However, in an app with product flavors,
    // the play block becomes a place to store default configurations. Anything configured in here
    // will apply to all product flavors, that is, unless an override is supplied in the playConfigs
    // block.
}

android {
    // Suppose we have the following flavors
    flavorDimensions("customer", "type")
    productFlavors {
        register("firstCustomer") { setDimension("customer") }
        register("secondCustomer") { setDimension("customer") }

        register("demo") { setDimension("type") }
        register("full") { setDimension("type") }
    }

    playConfigs {
        // Now, we can configure GPP however precisely is required.

        // Configuration overrides occur in a cascading manner from most to least specific. That is,
        // a property configured in a build type + flavor combo overrides that same property
        // configured in a flavor combo, which overrides a build type combo, which in turn overrides
        // the play block. Properties not configured are inherited.
        register("firstCustomerFullRelease") { ... } // Build type + flavor
        register("firstCustomer") { ... } // Flavor
        register("release") { ... } // Build type
    }
}
```

</details>

<details><summary>Groovy</summary>

```groovy
play {
    // In a simple app, this play block is all you'll need. However, in an app with product flavors,
    // the play block becomes a place to store default configurations. Anything configured in here
    // will apply to all product flavors, that is, unless an override is supplied in the playConfigs
    // block.
}

android {
    // Suppose we have the following flavors
    flavorDimensions 'customer', 'type'
    productFlavors {
        firstCustomer { dimension 'customer' }
        secondCustomer { dimension 'customer' }

        demo { dimension 'type' }
        full { dimension 'type' }
    }

    playConfigs {
        // Now, we can configure GPP however precisely is required.

        // Configuration overrides occur in a cascading manner from most to least specific. That is,
        // a property configured in a build type + flavor combo overrides that same property
        // configured in a flavor combo, which overrides a build type combo, which in turn overrides
        // the play block. Properties not configured are inherited.
        firstCustomerFullRelease { ... } // Build type + flavor
        firstCustomer { ... } // Flavor
        release { ... } // Build type
    }
}
```

</details>

### Disabling publishing

Sometimes, you may not want to publish all variants of your app. Or maybe you don't want publishing
enabled on CI or local dev machines. Whatever the case may be, GPP can be disabled with the
`enabled` property:

<details open><summary>Kotlin</summary>

```kt
android {
    // ...

    playConfigs {
        register("myCustomVariantOrProductFlavor") {
            isEnabled = true
        }

        // ...
    }
}

play {
    isEnabled = false // This disables GPP by default. It could be the other way around.
    // ...
}
```

</details>

<details><summary>Groovy</summary>

```groovy
android {
    // ...

    playConfigs {
        myCustomVariantOrProductFlavor {
            enabled = true
        }

        // ...
    }
}

play {
    enabled = false // This disables GPP by default. It could be the other way around.
    // ...
}
```

</details>

### Combining artifacts into a single release

By default, GPP assumes every product flavor consists of a separate, independent app. To tell GPP
this isn't the case, you must use the `commit` property:

<details open><summary>Kotlin</summary>

```kt
android {
    // ...

    playConfigs {
        register("someFlavor1") {
            commit = false
        }

        register("someFlavor[2..N)") {
            commit = false
        }

        register("someFlavorN") {
            // This isn't actually needed since the default is true. Here's what you *do* need:
            // 1. A starter no-commit variant (someFlavor1 in this case)
            // 2. (Optional) Intermediate no-commit variants (someFlavor2, someFlavor3, ...)
            // 3. One finisher variant to commit (aka do NOT mark someFlavorN as no-commit)
            commit = true
        }

        // ...
    }
}

afterEvaluate {
    // Now make sure the tasks execute in the right order
    val intermediateTasks = listOf(
            "publishSomeFlavor2Release[Apk/Bundle]",
            "publishSomeFlavor3Release[Apk/Bundle]",
            ...
    )
    tasks.matching { it.name in intermediateTasks }.configureEach {
        mustRunAfter("publishSomeFlavor1Release[Apk/Bundle]")
    }
    tasks.named("publishSomeFlavorNRelease[Apk/Bundle]").configure {
        mustRunAfter(intermediateTasks)
    }
}
```

</details>

<details><summary>Groovy</summary>

```groovy
android {
    // ...

    playConfigs {
        someFlavor1 {
            commit = false
        }

        someFlavor[2..N) {
            commit = false
        }

        someFlavorN {
            // This isn't actually needed since the default is true. Here's what you *do* need:
            // 1. A starter no-commit variant (someFlavor1 in this case)
            // 2. (Optional) Intermediate no-commit variants (someFlavor2, someFlavor3, ...)
            // 3. One finisher variant to commit (aka do NOT mark someFlavorN as no-commit)
            commit = true
        }

        // ...
    }
}

afterEvaluate {
    // Now make sure the tasks execute in the right order
    def intermediateTasks = [
            "publishSomeFlavor2Release[Apk/Bundle]",
            "publishSomeFlavor3Release[Apk/Bundle]",
            ...
    ]
    tasks.matching { intermediateTasks.contains(it.name) }.configureEach {
        mustRunAfter("publishSomeFlavor1Release[Apk/Bundle]")
    }
    tasks.named("publishSomeFlavorNRelease[Apk/Bundle]").configure {
        mustRunAfter(intermediateTasks)
    }
}
```

</details>

### Using multiple Service Accounts

If you need to publish each build flavor to a separate Play Store account, simply provide separate
credentials per product flavor.

<details open><summary>Kotlin</summary>

```kt
android {
    // ...

    playConfigs {
        register("firstCustomer") {
            serviceAccountCredentials = file("customer-one-key.json")
        }

        register("secondCustomer") {
            serviceAccountCredentials = file("customer-two-key.json")
        }
    }
}
```

</details>

<details><summary>Groovy</summary>

```groovy
android {
    // ...

    playConfigs {
        firstCustomer {
            serviceAccountCredentials = file('customer-one-key.json')
        }

        secondCustomer {
            serviceAccountCredentials = file('customer-two-key.json')
        }
    }
}
```

</details>

## Advanced topics

### Using CLI options

All configuration options available in the `play` block are also available as CLI options so you
don't have to update your build file when making one-time changes. For example, to configure
`play.track` on demand, use the `--track` option. `camelCase` options are converted to
`kebab-case` ones.

To get a list of options and their quick documentation, use `./gradlew help --task [task]` where
`task` is something like `publishBundle`.

### Encrypting Service Account keys

If you commit unencrypted Service Account keys to source, you run the risk of letting anyone access
your Google Play account. To circumvent this issue, many CI servers support encrypting files while
keeping fake versions in public source control. Here is a set of
[common fake files](https://github.com/SUPERCILEX/Robot-Scouter/tree/38407b3d6db74edb6c9de33b862655dfbd010a70/ci-dummies)
you might need and ways to encrypt your real keys for a few common CI servers:

- [Travis CI](https://docs.travis-ci.com/user/encrypting-files/)
- [CircleCI](https://github.com/circleci/encrypted-files)
- [Jenkins](https://github.com/samrocketman/jervis/wiki/Secure-secrets-in-repositories)

### Using HTTPS proxies

If you need to use GPP behind an HTTPS-proxy, but it fails with an `SSLHandshakeException`, you can
provide your own truststore via the `javax.net.ssl.trustStore` property in your project's
`gradle.properties`:

```properties
systemProp.javax.net.ssl.trustStore=/path/to/your/truststore.ks
systemProp.javax.net.ssl.trustStorePassword=YourTruststorePassword
```

GPP will automatically pick it up and use your proxy.
