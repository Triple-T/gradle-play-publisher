package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.CommitResponse
import com.github.triplet.gradle.androidpublisher.EditResponse
import com.github.triplet.gradle.androidpublisher.FakeEditManager
import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import com.github.triplet.gradle.androidpublisher.newSuccessCommitResponse
import com.github.triplet.gradle.androidpublisher.newSuccessEditResponse
import com.github.triplet.gradle.common.utils.nullOrFull
import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.github.triplet.gradle.play.tasks.shared.ArtifactIntegrationTests
import com.github.triplet.gradle.play.tasks.shared.LifecycleIntegrationTests
import com.github.triplet.gradle.play.tasks.shared.PublishArtifactIntegrationTests
import com.github.triplet.gradle.play.tasks.shared.PublishOrPromoteArtifactIntegrationTests
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class PublishApkIntegrationTest : IntegrationTestBase(), ArtifactIntegrationTests,
        PublishOrPromoteArtifactIntegrationTests, PublishArtifactIntegrationTests,
        LifecycleIntegrationTests {
    override fun taskName(taskVariant: String) = ":publish${taskVariant}Apk"

    override fun customArtifactName(name: String) = "$name.apk"

    override fun assertCustomArtifactResults(result: BuildResult, executed: Boolean) {
        assertThat(result.task(":packageRelease")).isNull()
        if (executed) {
            assertArtifactUpload(result)
        }
    }

    override fun assertArtifactUpload(result: BuildResult) {
        assertThat(result.output).contains("uploadApk(")
    }

    @Test
    fun `Builds apk on-the-fly by default`() {
        val result = execute("", "publishReleaseApk")

        result.requireTask(":packageRelease", outcome = SUCCESS)
        assertThat(result.output).contains("uploadApk(")
        assertThat(result.output).contains(".apk")
        assertThat(result.output).contains("publishArtifacts(")
    }

    @Test
    fun `Using custom artifact with single default mapping file uploads it`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${playgroundDir.escaped()}')
            }
        """

        File(playgroundDir, "1.apk").safeCreateNewFile()
        File(playgroundDir, "mapping.txt").safeCreateNewFile()
        val result = execute(config, "publishReleaseApk")

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("mapping.txt")
    }

    @Test
    fun `Using custom artifact with single specific mapping file uploads it`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${playgroundDir.escaped()}')
            }
        """

        File(playgroundDir, "1.apk").safeCreateNewFile()
        File(playgroundDir, "1.mapping.txt").safeCreateNewFile()
        val result = execute(config, "publishReleaseApk")

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("1.mapping.txt")
    }

    @Test
    fun `Using custom artifact with default and specific mapping files uploads specific`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${playgroundDir.escaped()}')
            }
        """

        File(playgroundDir, "1.apk").safeCreateNewFile()
        File(playgroundDir, "1.mapping.txt").safeCreateNewFile()
        File(playgroundDir, "mapping.txt").safeCreateNewFile()
        val result = execute(config, "publishReleaseApk")

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("1.mapping.txt")
        assertThat(result.output).doesNotContain("/mapping.txt")
    }

    @Test
    fun `Using custom artifact with mix of mapping files uploads them`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${playgroundDir.escaped()}')
            }
        """

        File(playgroundDir, "1.apk").safeCreateNewFile()
        File(playgroundDir, "2.apk").safeCreateNewFile()
        File(playgroundDir, "2.mapping.txt").safeCreateNewFile()
        File(playgroundDir, "mapping.txt").safeCreateNewFile()
        val result = execute(config, "publishReleaseApk")

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("/mapping.txt")
        assertThat(result.output).contains("2.mapping.txt")
    }

    @Test
    fun `Build uploads mapping file when available`() {
        // language=gradle
        val config = """
            buildTypes.release {
                shrinkResources true
                minifyEnabled true
            }
        """.withAndroidBlock()

        val result = execute(config, "publishReleaseApk")

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("uploadApk(")
        assertThat(result.output).contains("mappingFile=")
        assertThat(result.output).doesNotContain("mappingFile=null")
    }

    @Test
    fun `Build uploads debug symbols when available`() {
        // language=gradle
        val config = """
            defaultConfig.ndk.debugSymbolLevel = 'full'

            externalNativeBuild {
                cmake {
                    path = file("src/main/cpp/CMakeLists.txt")
                }
            }
        """.withAndroidBlock()

        val result = execute(config, "publishReleaseApk")

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("uploadApk(")
        assertThat(result.output).contains("debugSymbolsFile=")
        assertThat(result.output).doesNotContain("debugSymbolsFile=null")
    }

    @Test
    fun `Build uses correct retained OBBs`() {
        // language=gradle
        val config = """
            play.retain {
                mainObb = 123
                patchObb = 321
            }
        """

        val result = execute(config, "publishReleaseApk")

        result.requireTask(outcome = SUCCESS)
        assertThat(result.output).contains("uploadApk(")
        assertThat(result.output).contains("mainObbRetainable=123")
        assertThat(result.output).contains("patchObbRetainable=321")
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

                override fun commitEdit(id: String, sendChangesForReview: Boolean): CommitResponse {
                    println("commitEdit($id, $sendChangesForReview)")
                    return newSuccessCommitResponse()
                }

                override fun validateEdit(id: String) {
                    println("validateEdit($id)")
                }
            }
            val edits = object : FakeEditManager() {
                val versionCodeIndex = AtomicInteger()

                override fun findMaxAppVersionCode(): Long {
                    println("findMaxAppVersionCode()")
                    return 123
                }

                override fun uploadApk(
                        apkFile: File,
                        mappingFile: File?,
                        debugSymbolsFile: File?,
                        strategy: ResolutionStrategy,
                        mainObbRetainable: Int?,
                        patchObbRetainable: Int?,
                ): Long? {
                    println("uploadApk(" +
                                    "apkFile=$apkFile, " +
                                    "mappingFile=$mappingFile, " +
                                    "debugSymbolsFile=$debugSymbolsFile, " +
                                    "strategy=$strategy, " +
                                    "mainObbRetainable=$mainObbRetainable, " +
                                    "patchObbRetainable=$patchObbRetainable)")

                    if (System.getProperty("FAIL") != null) error("Upload failed")
                    if (System.getProperty("SOFT_FAIL") != null) {
                        println("Soft failure")
                        return null
                    }

                    val versionCodes = System.getProperty("VERSION_CODES").nullOrFull().orEmpty()
                            .replace(" ", "").ifEmpty { "1" }.split(",")
                            .map { it.toLong() }
                    val index = versionCodeIndex.getAndUpdate {
                        (it + 1).coerceAtMost(versionCodes.size - 1)
                    }
                    return versionCodes[index]
                }

                override fun publishArtifacts(
                        versionCodes: List<Long>,
                        didPreviousBuildSkipCommit: Boolean,
                        trackName: String,
                        releaseStatus: ReleaseStatus?,
                        releaseName: String?,
                        releaseNotes: Map<String, String?>?,
                        userFraction: Double?,
                        updatePriority: Int?,
                        retainableArtifacts: List<Long>?,
                ) {
                    println("publishArtifacts(" +
                                    "versionCodes=$versionCodes, " +
                                    "didPreviousBuildSkipCommit=$didPreviousBuildSkipCommit, " +
                                    "trackName=$trackName, " +
                                    "releaseStatus=$releaseStatus, " +
                                    "releaseName=$releaseName, " +
                                    "releaseNotes=$releaseNotes, " +
                                    "userFraction=$userFraction, " +
                                    "updatePriority=$updatePriority, " +
                                    "retainableArtifacts=$retainableArtifacts)")
                }
            }

            publisher.install()
            edits.install()
        }
    }
}
