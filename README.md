# gradle-play-publisher

Gradle plugin to upload your APK and app details to the Google Play Store. Needs the ```com.android.application``` plugin applied. Supports the Android Application Plugin as of version ```0.12.2```.

[![Build Status](https://travis-ci.org/Triple-T/gradle-play-publisher.svg?branch=master)](https://travis-ci.org/Triple-T/gradle-play-publisher)

## Requirements

To use the publisher plugin you have to create a service account for your existing Google Play Account. See https://developers.google.com/android-publisher/getting_started for more information.

Once you finished the setup you have a so called *service account email address* and a *p12 key file* that we will use later on.

## Usage

Add it to your buildscript dependencies:

```groovy
buildscript {

    repositories {
        mavenCentral()
    }
    
    dependencies {
        classpath 'com.github.triplet.gradle:play-publisher:0.0.4'
    }
}
```

Apply it:

```groovy
apply plugin: 'play'
```

## Configuration

Once you have applied this plugin to your android application project you can configure it via the ```play``` closure. Drop in your service account email address and the p12 key file you generated in the API Console here.

```groovy
play {
    serviceAccountEmail = 'your-service-account-email'
    pk12File = file('key.p12')
}
```

As a default your APK is published to the alpha track and you can promote it to beta or production manually. If you want to directly publish to another track you can specify it via the ```track``` property:

```groovy
play {
    // ...
    track = 'production' // or 'beta' or 'alpha'
}
```


## Run

The plugin adds a publish task for every flavor of your app. So in the most simple case (where there are no flavors at all) that would be ```publishRelease```. If there are flavors like *free* and *paid* you would get the tasks ```publishFreeRelease```and ```publishPaidRelease```.


## Play Store Metadata

You can also update the Play Store Metadata automatically along with your APK. 

To use this feature, create a special source folder called ```play```. Inside, create a folder for each locale you want to support. Then drop your summary of recent changes into a file called ```whatsnew```. The title,  the description and the short description go into their own files in a subfolder called ```listing```. Once setup, your project should look something like this:

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

Note: You can provide different texts for different locales and product flavors. You may even support additional locales for some product flavors.

## Custom APK names

The plugin automatically finds out where your APK file is located if you are using the default behavior. If, for some reason, you are changing this default output, you have to tell the plugin. In those cases, you probably have some code in a `applicationVariants.all {...}` block that looks something like this:

```groovy
applicationVariants.all { variant ->
    if(variant.zipAlign) {
        variant.outputFile = new File("path/to/my/custom.apk")
    }
}
```
In order to teach the plugin that you changed the output file for your variant, you have to override the `apkFile` property of the `publishApkRelease` task:

```groovy
applicationVariants.all { variant ->
    // ... setup your custom apk name
    
    if (variant.buildType.name.equals("release")) {
        project.tasks."publishApk${variant.name.capitalize()}".apkFile = variant.outputFile
    }
}
```

## License

	 The MIT License (MIT)
	 
	 Copyright (c) 2014 Christian Becker
	 Copyright (c) 2014 Björn Hurling

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
