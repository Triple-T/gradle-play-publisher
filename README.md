# Gradle Play Publisher

[![Build Status](https://travis-ci.org/Triple-T/gradle-play-publisher.svg?branch=master)](https://travis-ci.org/Triple-T/gradle-play-publisher)
[![Latest Version](https://maven-badges.herokuapp.com/maven-central/com.github.triplet.gradle/play-publisher/badge.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.github.triplet.gradle%22%20AND%20a%3A%22play-publisher%22)

Gradle Play Publisher is a Gradle plugin that allows you to upload your APK and other app details to
the Google Play Store from a continuous integration server or anywhere you have a command line.

## Table of contents

1. [Quick start guide](#quick-start-guide)
1. [Prerequisites](#prerequisites)
   1. [Initial Play Store upload](#initial-play-store-upload)
   1. [Signing configuration](#signing-configuration)
   1. [Service Account](#service-account)
1. [Usage](#usage)
   1. [Installation](#installation)
   1. [Sample tasks](#sample-tasks)
   1. [Authenticating the plugin](#authenticating-the-plugin)
1. [Configuration](#configuration)
   1. [Specify the track](#specify-the-track)
   1. [Untrack conflicting versions](#untrack-conflicting-versions)
   1. [Play Store metadata](#play-store-metadata)
   1. [Uploading images](#uploading-images)
   1. [Using multiple Service Accounts](#using-multiple-service-accounts)
   1. [Encrypting Service Account keys](#encrypting-service-account-keys)
   1. [Running custom tasks before publishing](#running-custom-tasks-before-publishing)

## Quick start guide

1. Upload the first version of your APK or App Bundle using the
   [Google Play Console](https://play.google.com/apps/publish)
1. [Create a Google Play Service Account](#google-play-service-account)
1. [Sign your release builds](https://developer.android.com/studio/publish/app-signing#gradle-sign)
   with a valid `signingConfig`
1. [Add and apply the plugin](#installation)
1. [Authenticate the plugin](#authenticating-the-plugin)

## Prerequisites

### Initial Play Store upload

The first APK or App Bundle needs to be uploaded via the Google Play Console because registering the
app with the Play Store cannot be done using the Play Developer API. For all subsequent uploads and
changes this plugin can be used.

### Signing configuration

If the plugin didn't create any tasks, you most likely haven't added a valid signing configuration
to your release builds. Be sure to
[add one](https://developer.android.com/studio/publish/app-signing#gradle-sign).

### Service Account

To use this plugin, you must create a service account with access to the Play Developer API. You'll
find a guide to setting up the service account
[here](https://developers.google.com/android-publisher/getting_started#using_a_service_account).
Once that's done, you'll need to grant the following permissions to your service account for this
plugin to work (go to Settings -> Developer account -> API access -> Service Accounts):

![permissions.png](https://cloud.githubusercontent.com/assets/242983/17809992/95ea4eaa-661a-11e6-9879-521df4f14735.png)

## Usage

### Installation

In your root `build.gradle(.kts)` file, add the Gradle Play Publisher dependency:

```groovy
buildscript {
    repositories {
        // ...
        jcenter()
    }

    dependencies {
        // ...
        classpath 'com.github.triplet.gradle:play-publisher:1.2.2'
    }
}
```

Then apply it to each individual `com.android.application` module where you want to use this plugin.
For example, `app/build.gradle(.kts)` is a common app module:

```groovy
apply plugin: 'com.android.application'
apply plugin: 'com.github.triplet.play'
```

#### Snapshot builds

If you're prepared to cut yourself on the bleeding edge of this plugin, snapshot builds are
available through JitPack:

```groovy
buildscript {
    repositories {
        // ...
        maven { url 'https://jitpack.io' }
    }

    dependencies {
        // ...
        implementation 'com.github.Triple-T:gradle-play-publisher:commitId' // E.g. 75bed587f8
    }
}
```

### Sample tasks

Here are some example tasks the plugin might create:

* `publishApkRelease` - Uploads the APK and the summary of recent changes.
* `publishListingRelease` - Uploads the descriptions and images for the Play Store listing.
* `publishRelease` - Uploads everything.
* `bootstrapReleasePlayResources` - Fetches all existing data from the Play Store to bootstrap the required files and folders.

Should you choose to use product flavors, there will be an appropriately named task for each flavor.
E.g. `publishApkPaidRelease` or `publishListingPaidRelease`.

### Authenticating the plugin

After you've gone through the [Service Account setup](#service-account), you should have a JSON file
with your private key. Add a `play` block alongside your `android` one with the JSON file's
location:

```groovy
android { ... }

play {
    jsonFile = file('your-key.json')
}
```

#### Using a PKCS12 key instead

```groovy
play {
    serviceAccountEmail = 'service-account-name@project-id.iam.gserviceaccount.com'
    pk12File = file('your-key.p12')
}
```

## Configuration

Once you've setup the plugin, you can continue to configure it through the `play` block.

### Specify the track

By default, your app is published to the `alpha` channel where you can promote it to the beta or
stable channels later from the Play Console. However, the Gradle Play Publisher plugin lets you
choose another `track` if needed:

```groovy
play {
    // ...
    track = 'production' // Or any of 'rollout', 'beta', 'alpha' or 'internal'
    userFraction = 0.2 // Only necessary for 'rollout'. The default is 0.1 (10%).
}
```

When initiating a staged release (`rollout`), the `userFraction` property will decide what
percentage of your users should start receiving the update.

### Untrack conflicting versions

The Google Play Developer API does not allow a beta version to be published while there is an alpha
version. If you want to automatically publish to a higher track and disable conflicting APKs from
lower tracks, simply set the `untrackOld` property to `true`:

```groovy
play {
    // ...
    track = 'beta'
    untrackOld = true // Will untrack 'alpha' if needed to upload to 'beta'
}
```

`untrackOld` will untrack all versions blocking a publication: that is, every APK with a version
code lower than the one being uploaded _and_ a lower track will be untracked. Example: publishing an
APK with version code 42 to the beta channel will untrack versions 41 and lower from alpha and
above. However, it will not untrack the stable channel or the alpha channel as long as its version
code is 43 or higher.

By default, `untrackOld` is false and will stop the publishing process in the event of a conflict.
Keep the default behaviour if you want to manually untrack conflicting versions.

### Play Store metadata

The Gradle Play Publisher plugin also lets you automatically upload Play Store metadata such as the
app title, description, etc.

To use this feature, create a special source folder called `play` under your main source sets.
Inside of it, create a folder for each locale you want to support, within which you will place your
app's metadata filed by locale.

Legend: `[folder]`, `filename`

```
- [src]
  |
  + - [main] // Or a variant like `paid`
      |
      + - [play]
          |
          + - [en-US]
          |   |
          |   + - [listing]
          |   |   |
          |   |   + - title // App title
          |   |   |
          |   |   + - video // Youtube product video
          |   |   |
          |   |   + - shortdescription // Tagline
          |   |   |
          |   |   + - fulldescription // Expanded description
          |   |
          |   + - whatsnew // Summary of recent changes
          |   |
          |   + - whatsnew-alpha // Optional, allows specifying the recent changes for a specific channel
          |
          + - [de-DE]
          |   |
          |   + ...
          |
          + - contactEmail // Developer email
          |
          + - contactPhone // Developer phone
          |
          + - contactWebsite // Developer website
          |
          + - defaultLanguage
```

#### Requirements

Because the Play Store limits the length of your metadata, the plugin will fail your build if the
following requirements aren't met:

* Title: 50 characters
* Short description: 80 characters
* Full description: 4000 characters
* Recent changes: 500 characters

If you'd rather not fail the build when one of those conditions aren't met, set the
`errorOnSizeLimit` property to false:

```groovy
play {
    // ...
    errorOnSizeLimit = false
}
```

### Uploading images

To speed things up a little, images are only uploaded if you explicitly say so:

```groovy
play {
    // ...
    uploadImages = true
}
```

Images are filed by locale as before:

```
- [src]
  |
  + - [main]
      |
      + - [play]
          |
          + - [en-US]
              |
              + - [listing]
                  |
                  + - [featureGraphic]
                  |
                  + - [icon]
                  |
                  + - [phoneScreenshots]
                  |
                  + - [promoGraphic]
                  |
                  + - [sevenInchScreenshots]
                  |
                  + - [tenInchScreenshots]
                  |
                  + - [tvBanner]
                  |
                  + - [tvScreenshots]
                  |
                  + - [wearScreenshots]
```

Note: the plugin does not validate uploaded files. Should the file be invalid, the Play Publisher
API will fail with a detailed error message.

Note: the plugin copies and merges the contents of the different play folders into a build folder
for upload. If there are still images left from a previous build, this might lead to undesired
behaviour. Please make sure to always do a `./gradlew clean` whenever you rename or delete images in
those directories.

### Using multiple Service Accounts

If you are developing for several customers, you might run into a situation where each build flavor
needs to be published into a separate Play Store account. Should that be the case, Gradle Play
Publisher supports flavor specific `playAccountConfigs`:

```groovy
android {
    playAccountConfigs {
        firstCustomerAccount {
            jsonFile = file('customer-one-key.json')
        }

        secondCustomerAccount {
            jsonFile = file('customer-two-key.json')
        }
    }

    productFlavors {
        firstCustomer {
            playAccountConfig = playAccountConfigs.firstCustomerAccount
        }

        secondCustomer {
            playAccountConfig = playAccountConfigs.secondCustomerAccount
        }
    }
}
```

### Encrypting Service Account keys

In many cases, you will want to publish your app from a continuous integration server. However, you
can't commit unencrypted Service Account keys or you run the risk of letting anyone access your
Google Play account. To circumvent this issue, many CI servers support encrypting files while
keeping fake versions in public source control. Here is a set of
[common fake files](https://github.com/SUPERCILEX/Robot-Scouter/tree/467f23681c3a422d3342d408ce16ae6d0ff441cf/travis-dummies)
you might need and ways to encrypt your real keys for a few common CI servers:

- [Travis CI](https://docs.travis-ci.com/user/encrypting-files/)
- [CircleCI](https://github.com/circleci/encrypted-files)
- [Jenkins](https://github.com/samrocketman/jervis/wiki/Secure-secrets-in-repositories)

### Running custom tasks before publishing

Sometimes custom tasks may need to execute right before those in the Gradle Play Publisher plugin.
For example, downloading images from a remote server for the store listing should happen before
executing the `generateReleasePlayResources` task (which is responsible for collecting all Play
Store assets for upload). Let's assume we have a task called `loadStoreListingFromRemote` that
fetches store listing information from a remote server. Our `generateReleasePlayResources` task
should now depend on that other task like so:

```groovy
project.afterEvaluate {
    generateReleasePlayResources.dependsOn loadStoreListingFromRemote
}
```

Note that we have to wait for the evaluation phase to complete before the
`generateReleasePlayResources` task becomes visible.
