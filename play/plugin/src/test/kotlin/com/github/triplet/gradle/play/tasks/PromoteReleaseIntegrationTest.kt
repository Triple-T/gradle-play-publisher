package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.EditResponse
import com.github.triplet.gradle.androidpublisher.FakeEditManager
import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.androidpublisher.newSuccessEditResponse
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test

class PromoteReleaseIntegrationTest : IntegrationTestBase() {
    override val factoryInstallerStatement = "com.github.triplet.gradle.play.tasks." +
            "PromoteReleaseIntegrationTest.installFactories()"

    @Test
    fun `Promote uses promote track by default`() {
        // language=gradle
        val config = """
            play.promoteTrack = 'foobar'
        """

        val result = execute(config, "promoteReleaseArtifact")

        assertThat(result.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("fromTrackName=auto-track")
        assertThat(result.output).contains("promoteTrackName=foobar")
    }

    @Test
    fun `Promote uses promote track even with track specified`() {
        // language=gradle
        val config = """
            play.track = 'foobar'
            play.promoteTrack = 'not-foobar'
        """

        val result = execute(config, "promoteReleaseArtifact")

        assertThat(result.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("promoteTrackName=not-foobar")
    }

    @Test
    fun `Promote finds from track dynamically by default`() {
        val result = execute("", "promoteReleaseArtifact")

        assertThat(result.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("fromTrackName=auto-track")
        assertThat(result.output).contains("promoteTrackName=auto-track")
    }

    @Test
    fun `Promote uses from track when specified`() {
        // language=gradle
        val config = """
            play.fromTrack = 'foobar'
        """

        val result = execute(config, "promoteReleaseArtifact")

        assertThat(result.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("fromTrackName=foobar")
        assertThat(result.output).contains("promoteTrackName=foobar")
    }

    @Test
    fun `CLI params can be used to configure task`() {
        val result = execute(
                "",
                "promoteReleaseArtifact",
                "--no-commit",
                "--from-track=myFromTrack",
                "--promote-track=myPromoteTrack",
                "--release-name=myRelName",
                "--release-status=draft",
                "--user-fraction=.88",
                "--update-priority=3"
        )

        assertThat(result.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("fromTrackName=myFromTrack")
        assertThat(result.output).contains("promoteTrackName=myPromoteTrack")
        assertThat(result.output).contains("releaseName=myRelName")
        assertThat(result.output).contains("releaseStatus=DRAFT")
        assertThat(result.output).contains("userFraction=0.88")
        assertThat(result.output).contains("updatePriority=3")
        assertThat(result.output).contains("insertEdit()")
        assertThat(result.output).doesNotContain("commitEdit(")
    }

    @Test
    fun `Global CLI params can be used to configure task`() {
        // language=gradle
        val config = """
            playConfigs {
                release {
                    track.set('unused')
                }
            }
        """.withAndroidBlock()

        val result = execute(
                config,
                "promoteArtifact",
                "--no-commit",
                "--from-track=myFromTrack",
                "--promote-track=myPromoteTrack",
                "--release-name=myRelName",
                "--release-status=draft",
                "--user-fraction=.88",
                "--update-priority=3"
        )

        assertThat(result.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("fromTrackName=myFromTrack")
        assertThat(result.output).contains("promoteTrackName=myPromoteTrack")
        assertThat(result.output).contains("releaseName=myRelName")
        assertThat(result.output).contains("releaseStatus=DRAFT")
        assertThat(result.output).contains("userFraction=0.88")
        assertThat(result.output).contains("updatePriority=3")
        assertThat(result.output).contains("insertEdit()")
        assertThat(result.output).doesNotContain("commitEdit(")
    }

    @Test
    fun `CLI params can be used to update track`() {
        val result = execute("", "promoteReleaseArtifact", "--update=myUpdateTrack")

        assertThat(result.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("fromTrackName=myUpdateTrack")
        assertThat(result.output).contains("promoteTrackName=myUpdateTrack")
    }

    @Test
    fun `Build succeeds when mapping file is produced but unavailable`() {
        // language=gradle
        val config = """
            buildTypes.release {
                shrinkResources true
                minifyEnabled true
                proguardFiles(getDefaultProguardFile("proguard-android.txt"))
            }
        """.withAndroidBlock()

        val result = execute(config, ":promoteReleaseArtifact")

        assertThat(result.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
    }

    @Test
    fun `Build uses correct release status`() {
        // language=gradle
        val config = """
            play.releaseStatus = ReleaseStatus.DRAFT
        """

        val result = execute(config, "promoteReleaseArtifact")

        assertThat(result.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("releaseStatus=DRAFT")
    }

    @Test
    fun `Build picks default release name when no track specific ones are available`() {
        // language=gradle
        val config = """
            buildTypes {
                consoleNames {}
            }
        """.withAndroidBlock()

        val result = execute(config, "promoteConsoleNamesArtifact")

        assertThat(result.task(":promoteConsoleNamesArtifact")).isNotNull()
        assertThat(result.task(":promoteConsoleNamesArtifact")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("releaseName=myDefaultName")
    }

    @Test
    fun `Build picks track specific release name when available`() {
        // language=gradle
        val config = """
            android.buildTypes {
                consoleNames {}
            }

            play.promoteTrack = 'custom-track'
        """

        val result = execute(config, "promoteConsoleNamesArtifact")

        assertThat(result.task(":promoteConsoleNamesArtifact")).isNotNull()
        assertThat(result.task(":promoteConsoleNamesArtifact")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("releaseName=myCustomName")
    }

    @Test
    fun `Build picks promote track specific release name when available`() {
        // language=gradle
        val config = """
            android.buildTypes {
                consoleNames {}
            }

            play.track = 'custom-track'
            play.promoteTrack = 'promote-track'
        """

        val result = execute(config, "promoteConsoleNamesArtifact")

        assertThat(result.task(":promoteConsoleNamesArtifact")).isNotNull()
        assertThat(result.task(":promoteConsoleNamesArtifact")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("releaseName=myPromoteName")
    }

    @Test
    fun `Build picks default release notes when no track specific ones are available`() {
        // language=gradle
        val config = """
            android.buildTypes {
                releaseNotes {}
            }

            System.setProperty("AUTOTRACK", "other")
        """

        val result = execute(config, "promoteReleaseNotesArtifact")

        assertThat(result.task(":promoteReleaseNotesArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseNotesArtifact")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
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

            play.fromTrack = 'custom-track'
        """

        val result = execute(config, "promoteReleaseNotesArtifact")

        assertThat(result.task(":promoteReleaseNotesArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseNotesArtifact")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("releaseNotes={en-US=Custom track release notes, " +
                                                   "fr-FR=Mes notes de mise à jour}")
    }

    @Test
    fun `Build picks remote track specific release notes when available`() {
        // language=gradle
        val config = """
            buildTypes {
                releaseNotes {}
            }
        """.withAndroidBlock()

        val result = execute(config, "promoteReleaseNotesArtifact")

        assertThat(result.task(":promoteReleaseNotesArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseNotesArtifact")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("releaseNotes={en-US=Auto track release notes, " +
                                                   "fr-FR=Mes notes de mise à jour}")
    }

    @Test
    fun `Build uses correct user fraction`() {
        // language=gradle
        val config = """
            play.userFraction = 0.123d
        """

        val result = execute(config, "promoteReleaseArtifact")

        assertThat(result.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("userFraction=0.123")
    }

    @Test
    fun `Build uses correct update priority`() {
        // language=gradle
        val config = """
            play.updatePriority = 5
        """

        val result = execute(config, "promoteReleaseArtifact")

        assertThat(result.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("updatePriority=5")
    }

    @Test
    fun `Build uses correct retained artifacts`() {
        // language=gradle
        val config = """
            play.retain.artifacts = [1l, 2l, 3l]
        """

        val result = execute(config, "promoteReleaseArtifact")

        assertThat(result.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("promoteRelease(")
        assertThat(result.output).contains("retainableArtifacts=[1, 2, 3]")
    }

    @Test
    fun `Build is not cacheable`() {
        val result1 = execute("", "promoteReleaseArtifact")
        val result2 = execute("", "promoteReleaseArtifact")

        assertThat(result1.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result1.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result2.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result2.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result1.output).contains("promoteRelease(")
        assertThat(result2.output).contains("promoteRelease(")
    }

    @Test
    fun `Build generates and commits edit by default`() {
        val result = execute("", "promoteReleaseArtifact")

        assertThat(result.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("insertEdit()")
        assertThat(result.output).contains("commitEdit(edit-id)")
    }

    @Test
    fun `Build skips commit when no-commit flag is passed`() {
        // language=gradle
        val config = """
            play.commit = false
        """

        val result = execute(config, "promoteReleaseArtifact")

        assertThat(result.task(":promoteReleaseArtifact")).isNotNull()
        assertThat(result.task(":promoteReleaseArtifact")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("insertEdit()")
        assertThat(result.output).doesNotContain("commitEdit(")
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
                override fun findLeastStableTrackName(): String? {
                    println("findLeastStableTrackName()")
                    return System.getProperty("AUTOTRACK") ?: "auto-track"
                }

                override fun promoteRelease(
                        promoteTrackName: String,
                        fromTrackName: String,
                        releaseStatus: ReleaseStatus?,
                        releaseName: String?,
                        releaseNotes: Map<String, String?>?,
                        userFraction: Double?,
                        updatePriority: Int?,
                        retainableArtifacts: List<Long>?
                ) {
                    println("promoteRelease(" +
                                    "promoteTrackName=$promoteTrackName, " +
                                    "fromTrackName=$fromTrackName, " +
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
