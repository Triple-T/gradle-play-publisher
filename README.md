# gradle-play-publisher

Gradle plugin to upload your APK and app details to the Google Play Store.

[![Build Status](https://travis-ci.org/Triple-T/gradle-play-publisher.svg?branch=master)](https://travis-ci.org/Triple-T/gradle-play-publisher)
[![Latest Version](https://maven-badges.herokuapp.com/maven-central/com.github.triplet.gradle/play-publisher/badge.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.github.triplet.gradle%22%20AND%20a%3A%22play-publisher%22)

## Quick Start Guide

1. Upload the first version of your APK using the web interface.
1. Create a Google Play Service Account (see [Prerequisites](#google-play-service-account)).
1. Assign a valid `signingConfig` to your release build type.
1. Add the plugin to your buildscript dependencies (see [Usage](#usage)).
1. Apply the plugin (see [Usage](#usage)).
1. Create `playAccountConfigs` and add them to your build (see [Authentication](#authentication)).

## Prerequisites

### Initial Play Store Upload

The first APK of your App needs to be uploaded via the web interface. This is to register the application id and cannot be done using the Play Developer API. For all subsequent uploads and changes this plugin can be used.

### Google Play Service Account

To use the publisher plugin you have to create a service account for your existing Google Play Account. See https://developers.google.com/android-publisher/getting_started for more information.

Due to the way the Google Play Publisher API works, you have to grant at least the following permissions to that service account:

![permissions.png](https://cloud.githubusercontent.com/assets/242983/17809992/95ea4eaa-661a-11e6-9879-521df4f14735.png)

Once you finished the setup you have a so called *service account email address* and a *p12 key file* that we will use later on.

### Signing Configuration

Please make sure to assign a valid signing configuration to your release build type. Otherwise, there won't be a publishable (signed) APK. In that case, the plugin won't create any of its tasks.

## Usage

Add it to your buildscript dependencies (top-level build.gradle file):

```groovy
buildscript {

    repositories {
        mavenCentral()
    }

    dependencies {
    	// ...
        classpath 'com.github.triplet.gradle:play-publisher:1.2.0'
    }
}
```

[![Latest Version](https://maven-badges.herokuapp.com/maven-central/com.github.triplet.gradle/play-publisher/badge.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.github.triplet.gradle%22%20AND%20a%3A%22play-publisher%22)

Apply it to your app-level build.gradle file as following below

```groovy
apply plugin:'com.android.application'
apply plugin: 'com.github.triplet.play'
```

The plugin creates the following tasks for you:

* `publishApkRelease` - Uploads the APK and the summary of recent changes.
* `publishListingRelease` - Uploads the descriptions and images for the Play Store listing.
* `publishRelease` - Uploads everything.
* `bootstrapReleasePlayResources` - Fetches all existing data from the Play Store to bootstrap the required files and folders.

Make sure to set a valid `signingConfig` for the release build type. Otherwise, there won't be a publishable APK and the above tasks won't be available.

In case you are using product flavors you will get one of the above tasks for every flavor. E.g. `publishApkPaidRelease` or `publishListingPaidRelease`.

## Authentication

Similar to Android's `signingConfigs` you need to setup so called `playAccountConfigs` to authorize your requests. Drop in your service account email address and the p12 key file you generated in the API Console here.

```groovy
android {
    playAccountConfigs {
        defaultAccountConfig {
            serviceAccountEmail = 'your-service-account-email'
            pk12File = file('key.p12')
        }
    }
    
    defaultConfig {
        // ...
        
        playAccountConfig = playAccountConfigs.defaultAccountConfig
    }
}
```

Alternatively you can use a JSON file that can be generated in the API Console.

```groovy
android {
    playAccountConfigs {
        defaultAccountConfig {
            jsonFile = file('keys.json')
        }
    }
    
    defaultConfig {
        // ...
        
        playAccountConfig = playAccountConfigs.defaultAccountConfig
    }
}
```

## Configuration

Once you have applied this plugin to your android application project you can configure it via the ```play``` block.

### Specify the track

As a default your APK is published to the alpha track and you can promote it to beta or production manually. If you want to directly publish to another track you can specify it via the ```track``` property:

```groovy
play {
    // ...
    track = 'production' // or 'rollout' or 'beta' or 'alpha'
    userFraction = 0.2 // only necessary for 'rollout', in this case default is 0.1 (10% of the target)
}
```

When defining the track as (staged) `rollout` you can also define a ```userFraction``` which is the portion of users who should get the staged rollout version of the APK.

### Untrack conflicting versions

The Google Play Developer API does not allow us to publish a beta version if there is an alpha version with a lower version code. If you want to publish to higher track and automatically disable conflicting APKs from a lower track, this can be specified by setting the `untrackOld` property to `true`.

```groovy
play {
    // ...
    track = 'beta'
    untrackOld = true // will untrack 'alpha' while upload to 'beta'
}
```

This will untrack whatever versions are currently blocking the publishing process. That is: every APK with a version lower than the one being uploaded in any of the tracks lower than specified by `track`.

Example: Publishing an APK with version 42 to production will untrack versions 41 and lower from alpha and beta. It will not, however untrack versions 43 or higher from those channels because they do not conflict.

Setting that flag to `false` or not setting it at all will stop the publishing process in case of conflicts. Keep that behaviour if you want to manually untrack conflicting versions rather than doing automatic untracking.

### Play Store Metadata

You can also update the Play Store Metadata automatically along with your APK.

To use this feature, create a special source folder called ```play```. Inside of it, create a folder for each locale you want to support. Then drop your summary of recent changes into a file called ```whatsnew```. The title, the description, the short description and the YouTube video url go into their own files in a subfolder called ```listing```. App details like contact email or the default language have their own files right inside the ```play``` folder as those details are not translated. Once set up, your project should look something like this:

```
- [src]
  |
  + - [main]
      |
      + - [play]
          |
          + - [en-US]
          |   |
          |   + - [listing]
          |   |   |
          |   |   + - fulldescription
          |   |   |
          |   |   + - shortdescription
          |   |   |
          |   |   + - title
          |   |   |
          |   |   + - video
          |   |
          |   + - whatsnew
          |
          + - [de-DE]
          |   |
          |   + - [listing]
          |   |   |
          |   |   + - fulldescription
          |   |   |
          |   |   + - shortdescription
          |   |   |
          |   |   + - title
          |   |   |
          |   |   + - video
          |   |
          |   + - whatsnew
          |
          + - contactEmail
          |
          + - contactPhone
          |
          + - contactWebsite
          |
          + - defaultLanguage
```

Note: You can provide different texts for different locales, build types and product flavors. You may even support additional locales for some build types or product flavors.

It is also possible to provide a separate summary of recent changes for each track. Just drop in a special `whatsnew-alpha` text file alongside your main `whatsnew` file and that one will be used if you publish to the alpha track.

### Text requirements
To make sure your texts comply to the requirements of the Play Store, there is built-in a check that causes the build to fail if your texts exceed the allowed lengths.

The limits are:

* Title: *30 characters*
* Short description: *80 characters*
* Long description: *4000 characters*
* Recent changes : *500 characters*

To prevent this check from failing the build, you can toggle it with the ```errorOnSizeLimit``` property:

```groovy
play {
    // ...
    errorOnSizeLimit = false
}
```

### Upload Images

To speed things up a little, images are only uploaded if you explicitly say so:

```groovy
play {
    // ...
    uploadImages = true
}
```

In that case the plugin looks for the Play Store images in your `play` folder. So just drop all your images into a folder structure similar to:

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

Note: The plugin does not enforce the correct size and file type. If you try to upload invalid files, the Google API will fail with a detailed error message.

Note: The plugin copies and merges the contents of the different play folders into a build folder for upload. If there are still images left from a previous build, this might lead to undesired behaviour. Please make sure to always do a `./gradlew clean` whenever you rename or delete images in those directories.

## Advanced Topics

### Multiple service accounts

If you are developing for several customers you might run into a situation where each build flavor needs to be published into a separate Play Store Account. The plugin supports these use cases by defining separate `playAccountConfigs` and attach them to the flavors:

```groovy
android {
    playAccountConfigs {
        firstCustomerAccount {
            serviceAccountEmail = 'first-mail@exmaple.com'
            pk12File = project.file('secret.pk12')
        }
        secondCustomerAccount {
            serviceAccountEmail = 'another-mail@exmaple.com'
            pk12File = project.file('another-secret.pk12')
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

### Run custom tasks before publishing

Sometimes it's required to execute some custom tasks right before executing tasks from the `play` plugin.

For example, one can have necessity to download images for the store listing from a remote 3rd-party server.
This should happen before executing the `generateReleasePlayResources` task which is responsible for collecting all the play store assets for upload.

Let's assume we have a task called `loadStoreListingFromRemote` that fetches store listing information from a remote server and places it under `src/main/play` as needed by the `play` plugin. Our `generateReleasePlayResources` task should now depend on that other task. In order to do that, we have to add the follwing lines to our build script:

```groovy
project.afterEvaluate {
    def generateReleasePlayResources = project.tasks.getByName("generateReleasePlayResources")
    generateReleasePlayResources.dependsOn loadStoreListingFromRemote
}
```

Note that we have to wait for the evaluation phase to complete before the `generateReleasePlayResources` task becomes visible.
