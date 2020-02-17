package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.EditResponse
import com.github.triplet.gradle.androidpublisher.FakeEditManager
import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import com.github.triplet.gradle.androidpublisher.newSuccessEditResponse
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import java.io.File

class PublishBundleIntegrationTest : IntegrationTestBase() {
    override val factoryInstallerStatement = "com.github.triplet.gradle.play.tasks." +
            "PublishBundleIntegrationTest.installFactories()"

    @Test
    fun `Builds bundle on-the-fly by default`() {
        val result = execute("", "publishReleaseBundle")

        assertThat(result.task(":bundleRelease")).isNotNull()
        assertThat(result.task(":bundleRelease")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadBundle(")
        assertThat(result.output).contains(".aab")
    }

    @Test
    fun `Rebuilding bundle on-the-fly uses cached build`() {
        val result1 = execute("", "publishReleaseBundle")
        val result2 = execute("", "publishReleaseBundle")

        assertThat(result1.task(":publishReleaseBundle")).isNotNull()
        assertThat(result1.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result2.task(":publishReleaseBundle")).isNotNull()
        assertThat(result2.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `Using non-existent custom artifact fails build with warning`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${escapedTempDir()}')
            }
        """

        val result = executeExpectingFailure(config, "publishReleaseBundle")

        assertThat(result.task(":publishReleaseBundle")).isNotNull()
        assertThat(result.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("Warning")
        assertThat(result.output).contains(tempDir.root.name)
    }

    @Test
    fun `Using custom artifact with multiple bundles fails build with warning`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${escapedTempDir()}')
            }
        """

        File(tempDir.root, "1.aab").createNewFile()
        File(tempDir.root, "2.aab").createNewFile()
        val result = executeExpectingFailure(config, "publishReleaseBundle")

        assertThat(result.task(":publishReleaseBundle")).isNotNull()
        assertThat(result.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("Warning")
        assertThat(result.output).contains(tempDir.root.name)
    }

    @Test
    fun `Using custom artifact skips on-the-fly bundle build`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${escapedTempDir()}')
            }
        """

        File(tempDir.root, "foo.aab").createNewFile()
        val result = execute(config, "publishReleaseBundle")

        assertThat(result.task(":bundleRelease")).isNull()
        assertThat(result.task(":publishReleaseBundle")).isNotNull()
        assertThat(result.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadBundle(")
        assertThat(result.output).contains(tempDir.root.name)
    }

    @Test
    fun `Reusing custom artifact uses cached build`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${escapedTempDir()}')
            }
        """

        File(tempDir.root, "foo.aab").createNewFile()
        val result1 = execute(config, "publishReleaseBundle")
        val result2 = execute(config, "publishReleaseBundle")

        assertThat(result1.task(":publishReleaseBundle")).isNotNull()
        assertThat(result1.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result2.task(":publishReleaseBundle")).isNotNull()
        assertThat(result2.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `Using custom artifact CLI arg skips on-the-fly bundle build`() {
        File(tempDir.root, "foo.aab").createNewFile()
        val result = execute("", "publishReleaseBundle", "--artifact-dir=${tempDir.root}")

        assertThat(result.task(":bundleRelease")).isNull()
        assertThat(result.task(":publishReleaseBundle")).isNotNull()
        assertThat(result.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadBundle(")
        assertThat(result.output).contains(tempDir.root.name)
    }

    @Test
    fun `CLI params can be used to configure task`() {
        val result = execute(
                "",
                "publishReleaseBundle",
                "--no-commit",
                "--release-name=myRelName",
                "--release-status=draft",
                "--resolution-strategy=ignore",
                "--track=myCustomTrack",
                "--user-fraction=.88"
        )

        assertThat(result.task(":publishReleaseBundle")).isNotNull()
        assertThat(result.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadBundle(")
        assertThat(result.output).contains("releaseName=myRelName")
        assertThat(result.output).contains("releaseStatus=DRAFT")
        assertThat(result.output).contains("strategy=IGNORE")
        assertThat(result.output).contains("trackName=myCustomTrack")
        assertThat(result.output).contains("userFraction=0.88")
    }

    @Test
    fun `Global CLI params can be used to configure task`() {
        // language=gradle
        val config = """
            playConfigs {
                release {
                    track = 'unused'
                }
            }
        """

        val result = execute(
                config,
                "publishBundle",
                "--no-commit",
                "--release-name=myRelName",
                "--release-status=draft",
                "--resolution-strategy=ignore",
                "--track=myCustomTrack",
                "--user-fraction=.88"
        )

        assertThat(result.task(":publishReleaseBundle")).isNotNull()
        assertThat(result.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadBundle(")
        assertThat(result.output).contains("releaseName=myRelName")
        assertThat(result.output).contains("releaseStatus=DRAFT")
        assertThat(result.output).contains("strategy=IGNORE")
        assertThat(result.output).contains("trackName=myCustomTrack")
        assertThat(result.output).contains("userFraction=0.88")
    }

    @Test
    fun `Build generates and commits edit by default`() {
        val result = execute("", "publishReleaseBundle")

        assertThat(result.task(":publishReleaseBundle")).isNotNull()
        assertThat(result.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("insertEdit()")
        assertThat(result.output).contains("commitEdit(edit-id)")
    }

    @Test
    fun `Build skips commit when no-commit flag is passed`() {
        // language=gradle
        val config = """
            play.commit = false
        """

        val result = execute(config, "publishReleaseBundle")

        assertThat(result.task(":publishReleaseBundle")).isNotNull()
        assertThat(result.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("insertEdit()")
        assertThat(result.output).doesNotContain("commitEdit(")
    }

    @Test
    fun `Build properly assigns didSkipCommit param when no-commit flag is passed`() {
        // language=gradle
        val config1 = """
            play.commit = false
        """

        // language=gradle
        val config2 = """
            android.defaultConfig.versionCode 2
            play.commit = false
        """

        val result1 = execute(config1, "publishReleaseBundle")
        val result2 = execute(config2, "publishReleaseBundle")

        assertThat(result1.task(":publishReleaseBundle")).isNotNull()
        assertThat(result1.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result1.output).contains("uploadBundle(")
        assertThat(result1.output).contains("didPreviousBuildSkipCommit=false")

        assertThat(result2.task(":publishReleaseBundle")).isNotNull()
        assertThat(result2.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result2.output).contains("uploadBundle(")
        assertThat(result2.output).contains("didPreviousBuildSkipCommit=true")
    }

    @Test
    fun `Build processes manifest when resolution strategy is set to auto`() {
        // language=gradle
        val config = """
            play.resolutionStrategy = 'auto'
        """

        val result = execute(config, "publishReleaseBundle")

        assertThat(result.task(":processReleaseMetadata")).isNotNull()
        assertThat(result.task(":processReleaseMetadata")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":publishReleaseBundle")).isNotNull()
        assertThat(result.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("findMaxAppVersionCode(")
        assertThat(result.output).contains("uploadBundle(")
    }

    @Test
    fun `Build uploads mapping file when available`() {
        // language=gradle
        val config = """
            android.buildTypes.release {
                shrinkResources true
                minifyEnabled true
                proguardFiles(getDefaultProguardFile("proguard-android.txt"))
            }
        """

        val result = execute(config, "publishReleaseBundle")

        assertThat(result.task(":publishReleaseBundle")).isNotNull()
        assertThat(result.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadBundle(")
        assertThat(result.output).contains("mappingFile=")
        assertThat(result.output).doesNotContain("mappingFile=null")
    }

    @Test
    fun `Build fails by default when upload fails`() {
        // language=gradle
        val config = """
            System.setProperty("FAIL", "true")
        """

        val result = executeExpectingFailure(config, "publishReleaseBundle")

        assertThat(result.task(":publishReleaseBundle")).isNotNull()
        assertThat(result.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("Upload failed")
    }

    @Test
    fun `Build uses correct version code`() {
        // language=gradle
        val config = """
            android.defaultConfig.versionCode 8
        """

        val result = execute(config, "publishReleaseBundle")

        assertThat(result.task(":publishReleaseBundle")).isNotNull()
        assertThat(result.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadBundle(")
        assertThat(result.output).contains("versionCode=8")
    }

    @Test
    fun `Build uses correct variant name`() {
        val result = execute("", "publishReleaseBundle")

        assertThat(result.task(":publishReleaseBundle")).isNotNull()
        assertThat(result.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadBundle(")
        assertThat(result.output).contains("variantName=release")
    }

    @Test
    fun `Build uses correct track`() {
        // language=gradle
        val config = """
            play.track 'myCustomTrack'
        """

        val result = execute(config, "publishReleaseBundle")

        assertThat(result.task(":publishReleaseBundle")).isNotNull()
        assertThat(result.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadBundle(")
        assertThat(result.output).contains("trackName=myCustomTrack")
    }

    @Test
    fun `Build uses correct release status`() {
        // language=gradle
        val config = """
            play.releaseStatus 'draft'
        """

        val result = execute(config, "publishReleaseBundle")

        assertThat(result.task(":publishReleaseBundle")).isNotNull()
        assertThat(result.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadBundle(")
        assertThat(result.output).contains("releaseStatus=DRAFT")
    }

    @Test
    fun `Build picks default release name when no track specific ones are available`() {
        // language=gradle
        val config = """
            android.buildTypes {
                consoleNames {}
            }
        """

        val result = execute(config, "publishConsoleNamesBundle")

        assertThat(result.task(":publishConsoleNamesBundle")).isNotNull()
        assertThat(result.task(":publishConsoleNamesBundle")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadBundle(")
        assertThat(result.output).contains("releaseName=myDefaultName")
    }

    @Test
    fun `Build picks track specific release name when available`() {
        // language=gradle
        val config = """
            android.buildTypes {
                consoleNames {}
            }

            play.track 'custom-track'
        """

        val result = execute(config, "publishConsoleNamesBundle")

        assertThat(result.task(":publishConsoleNamesBundle")).isNotNull()
        assertThat(result.task(":publishConsoleNamesBundle")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadBundle(")
        assertThat(result.output).contains("releaseName=myCustomName")
    }

    @Test
    fun `Build ignores promote track specific release name when available`() {
        // language=gradle
        val config = """
            android.buildTypes {
                consoleNames {}
            }

            play.track 'custom-track'
            play.promoteTrack 'promote-track'
        """

        val result = execute(config, "publishConsoleNamesBundle")

        assertThat(result.task(":publishConsoleNamesBundle")).isNotNull()
        assertThat(result.task(":publishConsoleNamesBundle")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadBundle(")
        assertThat(result.output).contains("releaseName=myCustomName")
    }

    @Test
    fun `Build picks default release notes when no track specific ones are available`() {
        // language=gradle
        val config = """
            android.buildTypes {
                releaseNotes {}
            }
        """

        val result = execute(config, "publishReleaseNotesBundle")

        assertThat(result.task(":publishReleaseNotesBundle")).isNotNull()
        assertThat(result.task(":publishReleaseNotesBundle")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadBundle(")
        assertThat(result.output).contains("releaseNotes={en-US=My default release notes, " +
                                                   "fr-FR=Mes notes de mise à jour}")
    }

    @Test
    fun `Build picks track specific release notes when available`() {
        // language=gradle
        val config = """
            android.buildTypes {
                releaseNotes {}
            }

            play.track 'custom-track'
        """

        val result = execute(config, "publishReleaseNotesBundle")

        assertThat(result.task(":publishReleaseNotesBundle")).isNotNull()
        assertThat(result.task(":publishReleaseNotesBundle")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadBundle(")
        assertThat(result.output).contains("releaseNotes={en-US=Custom track release notes, " +
                                                   "fr-FR=Mes notes de mise à jour}")
    }

    @Test
    fun `Build ignores promote track specific release notes when available`() {
        // language=gradle
        val config = """
            android.buildTypes {
                releaseNotes {}
            }

            play.track 'custom-track'
            play.promoteTrack 'promote-track'
        """

        val result = execute(config, "publishReleaseNotesBundle")

        assertThat(result.task(":publishReleaseNotesBundle")).isNotNull()
        assertThat(result.task(":publishReleaseNotesBundle")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadBundle(")
        assertThat(result.output).contains("releaseNotes={en-US=Custom track release notes, " +
                                                   "fr-FR=Mes notes de mise à jour}")
    }

    @Test
    fun `Build uses correct user fraction`() {
        // language=gradle
        val config = """
            play.userFraction 0.123
        """

        val result = execute(config, "publishReleaseBundle")

        assertThat(result.task(":publishReleaseBundle")).isNotNull()
        assertThat(result.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadBundle(")
        assertThat(result.output).contains("userFraction=0.123")
    }

    @Test
    fun `Build uses correct retained artifacts`() {
        // language=gradle
        val config = """
            play.retain.artifacts = [1, 2, 3]
        """

        val result = execute(config, "publishReleaseBundle")

        assertThat(result.task(":publishReleaseBundle")).isNotNull()
        assertThat(result.task(":publishReleaseBundle")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadBundle(")
        assertThat(result.output).contains("retainableArtifacts=[1, 2, 3]")
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

                override fun commitEdit(id: String) {
                    println("commitEdit($id)")
                }
            }
            val edits = object : FakeEditManager() {
                override fun findMaxAppVersionCode(): Long {
                    println("findMaxAppVersionCode()")
                    return 123
                }

                override fun uploadBundle(
                        bundleFile: File,
                        mappingFile: File?,
                        strategy: ResolutionStrategy,
                        versionCode: Long,
                        variantName: String,
                        didPreviousBuildSkipCommit: Boolean,
                        trackName: String,
                        releaseStatus: ReleaseStatus,
                        releaseName: String?,
                        releaseNotes: Map<String, String?>?,
                        userFraction: Double?,
                        retainableArtifacts: List<Long>?
                ) {
                    println("uploadBundle(" +
                                    "bundleFile=$bundleFile, " +
                                    "mappingFile=$mappingFile, " +
                                    "strategy=$strategy, " +
                                    "versionCode=$versionCode, " +
                                    "variantName=$variantName, " +
                                    "didPreviousBuildSkipCommit=$didPreviousBuildSkipCommit, " +
                                    "trackName=$trackName, " +
                                    "releaseStatus=$releaseStatus, " +
                                    "releaseName=$releaseName, " +
                                    "releaseNotes=$releaseNotes, " +
                                    "userFraction=$userFraction, " +
                                    "retainableArtifacts=$retainableArtifacts)")

                    if (System.getProperty("FAIL") != null) error("Upload failed")
                }
            }

            publisher.install()
            edits.install()
        }
    }
}
