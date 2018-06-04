package com.github.triplet.gradle.play.internal

enum class ResolutionStrategy(val publishedName: String) {
    AUTO("auto"),
    FAIL("fail"),
    IGNORE("ignore")
}
