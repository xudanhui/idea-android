# Development Notes #

## Short-Term Tasks ##

  * Proper make support (ValidityState) for aidl and dex
  * Fix aidl compiler failure on ApiDemos project
  * Complete code insight for manifests
  * "Deploy on make" option (to avoid emulator restart when restarting app)
  * Select emulator startup options in run configuration settings
  * Auto-add android.jar to module dependencies when Android facet is detected
  * Quickfix to regenerate R.java for unresolved resource references

## Longer-Term Tasks ##

  * Debug support
  * Code insight for view definition files
  * Application level configurable for SDK paths
  * Inspection to register activities/intents/etc. in manifest if they aren't currently registered.

## Maybe Someday Tasks ##

  * Complete implementation of AIDL language. Looks like the only difference between AIDL and Java is the usage of in/out/inout modifiers on parameters of interface methods.
  * i18n actions to move string literals to resource files
  * Visual designer for views

## Problems ##

  * Android SDK location is an application scope setting (local) which is currently stored in .iml files (shared). Should have application-level configurable with mapping from SDK version to installation path, and store only SDK version in .iml
  * Completion of tag and attribute names in .xml files is not available because schema is not available for Android XML formats, and DOM-based tag/attribute name completion seems to be available only in post-7.0 IntelliJ IDEA versions.
  * Xerces validation should be automatically disabled for Android XML files. Looks like the only pluggable way to do this in IDEA < 7.0.2 is to create an inspection profile programmatically, and to assign it to the res directory and manifest file.
  * How to implement rename and find usages for resource IDs?
  * aapt source generating compiler is only correctly invoked for project compile scope. For module compile scope (used for run), the generate() method isn't called.
  * "adb shell am start" occasionally fails (probably because it's invoked too early during the emulator startup process). Should either find a better way to wait for emulator readiness, or auto-retry on failure.
  * Emulator fails to start if no resources are defined in an app (and resources.arsc is missing in generated .apk). Should probably check during make that at least one resource is present.