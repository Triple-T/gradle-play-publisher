package com.github.triplet.gradle.play.tasks.shared

import com.github.triplet.gradle.play.helpers.SharedIntegrationTest
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

interface LifecycleIntegrationTests : SharedIntegrationTest {
    @ParameterizedTest
    @ValueSource(strings = [
        "release",
        "paid",
        "free",
        "other",
        "paidOther",
        "freeOther",
    ])
    fun `Semi-global lifecycle tasks are supported`(qualifier: String) {
        // language=gradle
        val config = """
            flavorDimensions 'pricing', 'magic'
            productFlavors {
                free { dimension 'pricing' }
                paid { dimension 'pricing' }
                other { dimension 'magic' }
            }
        """.withAndroidBlock()

        val result = execute(config, taskName(qualifier.capitalize()))

        if (!qualifier.contains("paid", ignoreCase = true)) {
            assertThat(result.task(taskName("FreeOtherRelease"))).isNotNull()
        }
        if (!qualifier.contains("free", ignoreCase = true)) {
            assertThat(result.task(taskName("PaidOtherRelease"))).isNotNull()
        }
    }
}
