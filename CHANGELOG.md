**2.0.0 - to be released**

* Convert plugin to Kotlin
* Bump Android Publisher plugin v2 version

**1.2.2 - 2018-05-24**

* More descriptive error message when texts exceed allowed length - #172
* Update character limit for app title to 50 characters - #224, #229
* Support for internal test track - #253

**1.2.1 - 2018-05-23**

*Broken artifact on MavenCentral*

**1.2.0 - 2017-03-23**

* Support for multiple service accounts - #76, #161
* Support ABI splits - #39
* Support for uploading proguard mapping files - #132
* Automatically untrack older versions in other channels during upload - #121
* Make sure to only read the first line for items that expect a single line string - #143
* Remove trailing linebreaks - #187

**1.1.5 - 2016-08-09**

* Support Filipino in Metadata - #142
* Support Screenshots for Wear devices - #127
* Fixed character count for line breaks on Windows - #136

**1.1.4 - 2015-10-14**

* Do not require signingConfig for bootstrap and listing tasks - #100
* Support JSON file credentials - #97

**1.1.3 - 2015-09-01**

* Support staged rollouts - #79, #80
* Bugfix: Bootstrap into main source set for projects without flavors - #87

**1.1.2 - 2015-07-16**

* Bugfix: Corretly set contact web parameter - #82

**1.1.1 - 2015-07-11**

* Support for tv related images - #77
* Support for app details - #44
* Addes some explanation to google's applicationNotFound exception - #75

**1.1.0 - 2015-05-09**

* Breaking change: Renamed plugin to `com.github.triplet.play` so it does not collide with gradle's internal play plugin - #50
* Throw errors if texts exceed the size limits - #66

**1.0.4 - 2015-04-14**

* Bootstrap and upload promo video urls - #60

**1.0.3 - 2015-04-13**

* Defer retrieval of applicationId to task execution - #54
* Bugfix: Use UTF-8 encoding in bootstrap task - #59

**1.0.2 - 2015-02-15**

* Support build type specific resources - #38
* Bugfix: Trim texts for listing before upload - #41
* Skip upload of recent changes if play folder does not exist - #40
* Exclude guava-jdk5 dependency that conflicts with a newer version pulled in by the Android Plugin - #21

**1.0.1 - 2015-02-06**

* Create publish tasks for all non-debuggable build types
* Better error message for unsupported locales

**1.0.0 - 2015-01-07**

* Updated to Android plugin 1.0.0
* Find APK file based on the variant output. Eliminates workarounds for custom APK names
* Upload images in alphabetical order

**0.14.1 - 2014-11-15**

* Additional check if zipAlign task exists
* Use variant's applicationId instead of parsing the manifest

**0.14.0 - 2014-11-12**

* Updated to Android plugin 0.14.+
* Changed versioning scheme to better reflect what version of the Android plugin is supported
* Added descriptions to publisher tasks
* Added task `bootstrapReleasePlayResources` (and flavors) to help setting up the folder structure
* Allow separate `whatsnew` text files for different tracks

**0.0.4 - 2014-10-11**

* Added separate tasks for uploading the apk and for updating the listing
* Allow uploading screenshots and images with property `uploadImages true`
