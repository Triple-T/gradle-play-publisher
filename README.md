# gradle-play-publisher

Gradle plugin to upload your release APKs to the Google Play Store. Needs the ```com.android.application``` plugin applied. Supports the Android Application Plugin as of version ```0.12.2```.

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
        classpath 'com.github.triplet.gradle:play-publisher:0.0.2'
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
