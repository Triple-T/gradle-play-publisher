## 2.0.0 - to be released

* Convert plugin to Kotlin
* Bump Android Publisher plugin version to v3
* Allow overriding configuration via command line options - #326
* Fully support multi-dimensional flavors - #130, #311
* Fill missing metadata with data from the default language - #107, #323
* Support gradle cache and incremental builds - #304, #308
* Support different resolution strategies in case of version conflicts - #301
* Add group tasks to publish all variants at once - #117, #273
* Provide publishing tasks even when `signingConfig` is missing - #244, #298
* Improve error messages and logging - #238, #268
* Add status indicators and upload progress - #298
* Support Android App Bundles - #262, #319
* Support uploading in-app products - #181, #322

### Breaking changes

#### Updated listings structure

As a precondition for supporting multi-dimensional flavors, the structure of listings metadata
has changed:

For example, english listing files have moved:

```
../play/en-US/listing/shortdescription --> ../play/listings/en-US/shortdescription
```

TODO: link to README with new structure, add link to migration tool

#### Removed configuration properties

* `uploadImages`: Since the plugin now makes use of Gradle's caching system and incremental builds
  (#308) to only upload images if they have changed, this property has become obsolete.
* `untrackOld`: With the introduction of conflict resolution strategies (#301), this property has
  become obsolete.
* `errorOnSizeLimit`: The plugin will now always error on size limit to provide deterministic
  behavior.

##### Simplified Service Account credentials API

The `jsonFile` and `pk12File` properties have been replaced with a unified
`serviceAccountCredentials` property.

#### Renamed tasks to follow AGP conventions

For example, `publishApkRelease` -> `publishReleaseApk`. Note: the old tasks are still available for
now, but will be removed in a future release.

## 1.2.2 - 2018-05-24

* More descriptive error message when texts exceed allowed length - #172
* Update character limit for app title to 50 characters - #224, #229
* Support for internal test track - #253

## 1.2.1 - 2018-05-23

*Broken artifact on MavenCentral*

## 1.2.0 - 2017-03-23

* Support for multiple service accounts - #76, #161
* Support ABI splits - #39
* Support for uploading proguard mapping files - #132
* Automatically untrack older versions in other channels during upload - #121
* Make sure to only read the first line for items that expect a single line string - #143
* Remove trailing linebreaks - #187

## 1.1.5 - 2016-08-09

* Support Filipino in Metadata - #142
* Support Screenshots for Wear devices - #127
* Fixed character count for line breaks on Windows - #136

## 1.1.4 - 2015-10-14

* Do not require signingConfig for bootstrap and listing tasks - #100
* Support JSON file credentials - #97

## 1.1.3 - 2015-09-01

* Support staged rollouts - #79, #80
* Bugfix: Bootstrap into main source set for projects without flavors - #87

## 1.1.2 - 2015-07-16

* Bugfix: Corretly set contact web parameter - #82

## 1.1.1 - 2015-07-11

* Support for tv related images - #77
* Support for app details - #44
* Addes some explanation to google's applicationNotFound exception - #75

## 1.1.0 - 2015-05-09

* Breaking change: Renamed plugin to `com.github.triplet.play` so it does not collide with gradle's
  internal play plugin - #50
* Throw errors if texts exceed the size limits - #66

## 1.0.4 - 2015-04-14

* Bootstrap and upload promo video urls - #60

## 1.0.3 - 2015-04-13

* Defer retrieval of applicationId to task execution - #54
* Bugfix: Use UTF-8 encoding in bootstrap task - #59

## 1.0.2 - 2015-02-15

* Support build type specific resources - #38
* Bugfix: Trim texts for listing before upload - #41
* Skip upload of recent changes if play folder does not exist - #40
* Exclude guava-jdk5 dependency that conflicts with a newer version pulled in by the Android
  Plugin - #21

## 1.0.1 - 2015-02-06

* Create publish tasks for all non-debuggable build types
* Better error message for unsupported locales

## 1.0.0 - 2015-01-07

* Updated to Android plugin 1.0.0
* Find APK file based on the variant output. Eliminates workarounds for custom APK names
* Upload images in alphabetical order

## 0.14.1 - 2014-11-15

* Additional check if zipAlign task exists
* Use variant's applicationId instead of parsing the manifest

## 0.14.0 - 2014-11-12

* Updated to Android plugin 0.14.+
* Changed versioning scheme to better reflect what version of the Android plugin is supported
* Added descriptions to publisher tasks
* Added task `bootstrapReleasePlayResources` (and flavors) to help setting up the folder structure
* Allow separate `whatsnew` text files for different tracks

## 0.0.4 - 2014-10-11

* Added separate tasks for uploading the apk and for updating the listing
* Allow uploading screenshots and images with property `uploadImages true`
