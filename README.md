<p align="center">
    <img alt="Logo" src="assets/logo.svg" width="25%" />
</p>

<h1 align="center">
    Gradle Play Publisher
</h1>

<p align="center">
    <a href="https://travis-ci.org/Triple-T/gradle-play-publisher">
        <img src="https://img.shields.io/travis/Triple-T/gradle-play-publisher/master.svg?style=flat-square" />
    </a>
    <a href="https://plugins.gradle.org/plugin/com.github.triplet.play">
        <img src="https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com/github/triplet/play/com.github.triplet.play.gradle.plugin/maven-metadata.xml.svg?label=Gradle%20Plugins%20Portal" />
    </a>
</p>

Gradle Play Publisher is a Gradle plugin that allows you to upload your App Bundle or APK and other
app details to the Google Play Store.

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
   1. [Promoting artifacts](#promoting-artifacts)
   1. [Handling version conflicts](#handling-version-conflicts)
1. [Managing Play Store metadata](#managing-play-store-metadata)
   1. [Quickstart](#quickstart)
   1. [Directory structure](#directory-structure)
   1. [Publishing listings](#publishing-listings)
   1. [Publishing in-app products](#publishing-in-app-products)
1. [Advanced topics](#advanced-topics)
   1. [Using CLI options](#using-cli-options)
   1. [Encrypting Service Account keys](#encrypting-service-account-keys)
   1. [Using multiple Service Accounts](#using-multiple-service-accounts)

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

If no publishing tasks were created, you most likely haven't added a valid signing configuration to
your release builds. Be sure to
[add one](https://developer.android.com/studio/publish/app-signing#gradle-sign).

### Service Account

To use GPP, you must create a service account with access to the Play Developer API. You'll find a
guide to setting up the service account
[here](https://developers.google.com/android-publisher/getting_started#using_a_service_account).
Once that's done, you'll need to grant at least the following permissions to your service account
for GPP to work (go to Settings -> Developer account -> Users & permissions):

<p>
    <img alt="Minimum Service Account permissions" src="assets/min-perms.png" width="66%" />
</p>

## Basic setup

### Installation

Apply the plugin to each individual `com.android.application` module where you want to use GPP
through the `plugins {}` DSL:

<details open><summary>Kotlin</summary>

```kt
plugins {
    id("com.android.application")
    id("com.github.triplet.play") version "2.0.0"
}
```

</details>

<details><summary>Groovy</summary>

```groovy
plugins {
    id 'com.android.application'
    id 'com.github.triplet.play' version '2.0.0'
}
```

</details>

#### Snapshot builds

If you're prepared to cut yourself on the bleeding edge of GPP development, snapshot builds are
available from
[Sonatype's `snapshots` repository](https://oss.sonatype.org/content/repositories/snapshots/):

<details open><summary>Kotlin</summary>

```kt
buildscript {
    repositories {
        // ...
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }

    dependencies {
        // ...
        classpath("com.github.triplet.gradle:play-publisher:2.1.0-SNAPSHOT")
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
        classpath 'com.github.triplet.gradle:play-publisher:2.1.0-SNAPSHOT'
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

#### Using a PKCS12 key instead

```kt
play {
    serviceAccountEmail = "service-account-name@project-id.iam.gserviceaccount.com"
    serviceAccountCredentials = file("your-key.p12")
}
```

## Task organization

GPP follows the Android Gradle Plugin's naming convention: `[action][Variant][Thing]`. For example,
`publishPaidReleaseBundle` will be generated if have a `paid` product flavor.

Lifecycle tasks to publish multiple product flavors at once are also available. For example,
`publishBundle` publishes all variants.

To find available tasks, run `./gradlew tasks` and look under the publishing section.

## Managing artifacts

GPP supports uploading both the App Bundle and APK. Once uploaded, GPP also supports promoting those
artifacts.

### Common configuration

Several options are available to customize how your artifacts are published:

* `track` is the target stage for an artifact, i.e. alpha/beta/prod
* `releaseStatus` is the type of release, i.e. draft/completed/in progress
* `userFraction` is the percentage of users who will received a staged release

Example configuration:

```kt
play {
    // ...
    track = "production"
    userFraction = 0.5
    releaseStatus = "inProgress"
}
```

#### Uploading release notes

While GPP can automatically build and find your artifact, you'll need to tell the plugin about your
release notes.

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

### Promoting artifacts

Existing releases can be promoted and/or updated to the [configured track](#common-configuration)
with `./gradlew promoteArtifact`.

By default, the track from which to promote a release will be determined by the most unstable
channel that contains a release. Example: if the alpha channel has no releases, but the beta and
prod channels do, the beta channel will be picked. To configure this manually, use the `fromTrack`
property:

```kt
play {
    // ...
    fromTrack = "alpha"
} 
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

Directory | Max # of images
--- | ---
`icon` | 1
`feature-graphic` | 1
`promo-graphic` | 1
`phone-screenshots` | 8
`tablet-screenshots` | 8
`large-tablet-screenshots` | 8
`tv-banner` | 1
`tv-screenshots` | 8
`wear-screenshots` | 8

### Publishing in-app products

Run `./gradlew publishProducts`.

Manually setting up in-app purchase files is not recommended. [Bootstrap them instead](#quickstart)
with `./gradlew bootstrap --products`.

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

### Using multiple Service Accounts

If you need to publish each build flavor to a separate Play Store account, GPP supports flavor
specific `play` configurations through the `playConfigs` block:

<details open><summary>Kotlin</summary>

```kt
android {
    // ...

    flavorDimensions("customer", "version")
    productFlavors {
        register("firstCustomer") {
            setDimension("customer")
            // ...
        }

        register("secondCustomer") {
            setDimension("customer")
            // ...
        }

        register("demo") {
            setDimension("version")
            // ...
        }

        register("full") {
            setDimension("version")
            // ...
        }
    }

    playConfigs {
        register("firstCustomer") {
            serviceAccountCredentials = file("customer-one-key.json")
        }

        register("secondCustomer") {
            serviceAccountCredentials = file("customer-two-key.json")
        }
    }
}

play {
    // Defaults
}
```

</details>

<details><summary>Groovy</summary>

```groovy
android {
    // ...

    flavorDimensions 'customer', 'version'
    productFlavors {
        firstCustomer {
            dimension 'customer'
            // ...
        }

        secondCustomer {
            dimension 'customer'
            // ...
        }

        demo {
            dimension 'version'
            // ...
        }

        full {
            dimension 'version'
            // ...
        }
    }

    playConfigs {
        firstCustomer {
            serviceAccountCredentials = file('customer-one-key.json')
        }

        secondCustomer {
            serviceAccountCredentials = file('customer-two-key.json')
        }
    }
}

play {
    // Defaults
}
```

</details>
