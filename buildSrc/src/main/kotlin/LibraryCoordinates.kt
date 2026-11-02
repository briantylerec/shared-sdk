package com.redbull.lab.build

/** Coordinates shared by every published artifact in this SDK repository. */
object LibraryCoordinates {
    const val GROUP_ID = "com.redbull.lab"
    const val ANDROID_SDK_ARTIFACT_ID = "android-sdk"
    const val ANDROID_DATA_ARTIFACT_ID = "android-data"
    const val ANDROID_DOMAIN_ARTIFACT_ID = "android-domain"

    /** Local and Maven Local default; CI may override it with -PreleaseVersion. */
    const val VERSION_NAME = "0.2.2"
}
