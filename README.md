# gradle-play-publisher

Gradle plugin to upload your APK and app details to the Google Play Store. Needs the ```com.android.application``` plugin applied. Supports the Android Application Plugin as of version ```1.0.0```.

[![Build Status](https://travis-ci.org/Triple-T/gradle-play-publisher.svg?branch=master)](https://travis-ci.org/Triple-T/gradle-play-publisher)

## Prerequisites

To use the publisher plugin you have to create a service account for your existing Google Play Account. See https://developers.google.com/android-publisher/getting_started for more information. 

Due to the way the Google Play Publisher API works, you have to grant at least the following permissions to that service account:

![permissions.png](https://cloud.githubusercontent.com/assets/1361086/5045988/95eb902e-6bb9-11e4-9251-30840ba014d3.png)

Once you finished the setup you have a so called *service account email address* and a *p12 key file* that we will use later on.

## Usage

Add it to your buildscript dependencies:

```groovy
buildscript {

    repositories {
        mavenCentral()
    }
    
    dependencies {
    	// ...
        classpath 'com.github.triplet.gradle:play-publisher:1.0.2'
    }
}
```

Apply it:

```groovy
apply plugin: 'play'
```

The plugin creates the following tasks for you:

* `publishApkRelease` - Uploads the APK and the summary of recent changes.
* `publishListingRelease` - Uploads the descriptions and images for the Play Store listing.
* `publishRelease` - Uploads everything.
* `bootstrapReleasePlayResources` - Fetch all existing data from the Play Store to bootstrap the required files and folders.

In case you are using product flavors you will get one of the above tasks for every flavor. E.g. `publishApkPaidRelease` or `publishListingPaidRelease`.

## Configuration

Once you have applied this plugin to your android application project you can configure it via the ```play``` block.

### Credentials

Drop in your service account email address and the p12 key file you generated in the API Console here.

```groovy
play {
    serviceAccountEmail = 'your-service-account-email'
    pk12File = file('key.p12')
}
```

### Play Store Metadata

You can also update the Play Store Metadata automatically along with your APK. 

To use this feature, create a special source folder called ```play```. Inside, create a folder for each locale you want to support. Then drop your summary of recent changes into a file called ```whatsnew```. The title,  the description and the short description go into their own files in a subfolder called ```listing```. Once set up, your project should look something like this:

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
          |   |
          |   + - whatsnew
          |
          + - [de-DE]
              |
              + - [listing]
              |   |
              |   + - fulldescription
              |   |
              |   + - shortdescription
              |   |
              |   + - title
              |
              + - whatsnew
```
Note: Make sure your texts comply to the requirements of the Play Store, that is they do not exceed the allowed lengths of *30 characters* for the title, *80 characters* for the short description, *4000 characters* for the description and *500 characters* for the summary of recent changes.

Note: You can provide different texts for different locales, build types and product flavors. You may even support additional locales for some build types or product flavors.

### Specify the track

As a default your APK is published to the alpha track and you can promote it to beta or production manually. If you want to directly publish to another track you can specify it via the ```track``` property:

```groovy
play {
    // ...
    track = 'production' // or 'beta' or 'alpha'
}
```

It is also possible to provide a separate summary of recent changes for each track. Just drop in a special `whatsnew-alpha` text file alongside your main `whatsnew` file and that one will be used if you publish to the alpha track.

### Upload Images

Currently images are only uploaded if you explicitly say so:

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
```

Note: The plugin currently does not enforce the correct size and file type. If you try to upload invalid files, the Google API will fail with a detailed error message.

Note: We still have some issues when you change the images in those folders. For now you should do a full rebuild whenever you change them.

## License

	 The MIT License (MIT)
	 
	 Copyright (c) 2015 Christian Becker
	 Copyright (c) 2015 Björn Hurling

	 Permission is hereby granted, free of charge, to any person obtaining a copy
	 of this software and associated documentation files (the "Software"), to deal
	 in the Software without restriction, including without limitation the rights
	 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	 copies of the Software, and to permit persons to whom the Software is
	 furnished to do so, subject to the following conditions:

	 The above copyright notice and this permission notice shall be included in all
	 copies or substantial portions of the Software.

	 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 	 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
	 SOFTWARE.
