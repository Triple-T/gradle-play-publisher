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
