package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.EditResponse
import com.github.triplet.gradle.androidpublisher.FakeEditManager
import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.androidpublisher.newSuccessEditResponse
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test

class ProcessArtifactVersionCodesIntegrationTest : IntegrationTestBase() {
    override val factoryInstallerStatement = "com.github.triplet.gradle.play.tasks." +
            "ProcessArtifactVersionCodesIntegrationTest.installFactories()"

    @Test
    fun `Task only runs on release`() {
        // language=gradle
        val config = """
            play.resolutionStrategy = ResolutionStrategy.AUTO
        """

        val result = execute(config, "assembleDebug")

        assertThat(result.task(":processDebugMetadata")).isNull()
    }

    @Test
    fun `Task doesn't run by default`() {
        val result = execute("", "assembleRelease")

        assertThat(result.task(":processReleaseVersionCodes")).isNull()
    }

    @Test
    fun `Version code is incremented`() {
        // language=gradle
        val config = """
            play.resolutionStrategy = ResolutionStrategy.AUTO

            onVariantProperties {
                for (output in outputs) {
                    output.versionName.set(output.versionCode.map {
                        println('versionCode=' + it)
                        it.toString()
                    })
                }
            }
        """.withAndroidBlock()

        val result = execute(config, "assembleRelease")

        assertThat(result.task(":processReleaseVersionCodes")).isNotNull()
        assertThat(result.task(":processReleaseVersionCodes")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("versionCode=42")
    }

    @Test
    fun `Version code with splits is patch incremented`() {
        // language=gradle
        val config = """
            play.resolutionStrategy = ResolutionStrategy.AUTO

            splits.density {
                enable true
                reset()
                include "xxhdpi", "xxxhdpi"
            }


            def count = 0
            onVariantProperties {
                if (name == 'release') {
                    for (output in outputs) {
                        output.versionCode.set(count++)
                        output.versionName.set(output.versionCode.map {
                            println('versionCode=' + it)
                            it.toString()
                        })
                    }
                }
            }
        """.withAndroidBlock()

        val result = execute(config, "assembleRelease")

        assertThat(result.task(":processReleaseVersionCodes")).isNotNull()
        assertThat(result.task(":processReleaseVersionCodes")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("versionCode=42")
        assertThat(result.output).contains("versionCode=44")
        assertThat(result.output).contains("versionCode=46")
    }

    companion object {
        @JvmStatic
        fun installFactories() {
            val publisher = object : FakePlayPublisher() {
                override fun insertEdit(): EditResponse {
                    println("insertEdit()")
                    return newSuccessEditResponse("edit-id")
                }

                override fun getEdit(id: String): EditResponse {
                    println("getEdit($id)")
                    return newSuccessEditResponse(id)
                }
            }
            val edits = object : FakeEditManager() {
                override fun findMaxAppVersionCode(): Long = 41
            }

            publisher.install()
            edits.install()
        }
    }
}
