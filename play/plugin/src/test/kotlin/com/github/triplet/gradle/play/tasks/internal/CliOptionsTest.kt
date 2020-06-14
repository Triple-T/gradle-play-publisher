package com.github.triplet.gradle.play.tasks.internal

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class CliOptionsTest {
    @Test
    fun `Bootstrap holder defaults to true values`() {
        val holder = BootstrapOptions.Holder()

        assertThat(holder.downloadAppDetails).isTrue()
        assertThat(holder.downloadListings).isTrue()
        assertThat(holder.downloadReleaseNotes).isTrue()
        assertThat(holder.downloadProducts).isTrue()
    }

    @Test
    fun `Bootstrap holder sets other flags to false once one is manually updated`() {
        val holder = BootstrapOptions.Holder()

        holder.downloadAppDetails = true

        assertThat(holder.downloadAppDetails).isTrue()
        assertThat(holder.downloadListings).isFalse()
        assertThat(holder.downloadReleaseNotes).isFalse()
        assertThat(holder.downloadProducts).isFalse()
    }
}
