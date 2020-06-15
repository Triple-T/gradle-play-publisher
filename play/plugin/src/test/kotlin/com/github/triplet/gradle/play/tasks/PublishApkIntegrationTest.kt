package com.github.triplet.gradle.play.tasks

import com.github.triplet.gradle.androidpublisher.EditResponse
import com.github.triplet.gradle.androidpublisher.FakeEditManager
import com.github.triplet.gradle.androidpublisher.FakePlayPublisher
import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import com.github.triplet.gradle.androidpublisher.newSuccessEditResponse
import com.github.triplet.gradle.common.utils.nullOrFull
import com.github.triplet.gradle.common.utils.safeCreateNewFile
import com.github.triplet.gradle.play.helpers.IntegrationTestBase
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class PublishApkIntegrationTest : IntegrationTestBase() {
    override val factoryInstallerStatement = "com.github.triplet.gradle.play.tasks." +
            "PublishApkIntegrationTest.installFactories()"

    @Test
    fun `Builds apk on-the-fly by default`() {
        val result = execute("", "publishReleaseApk")

        assertThat(result.task(":packageRelease")).isNotNull()
        assertThat(result.task(":packageRelease")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadApk(")
        assertThat(result.output).contains(".apk")
        assertThat(result.output).contains("publishApk(")
    }

    @Test
    fun `Rebuilding apk on-the-fly uses cached build`() {
        val result1 = execute("", "publishReleaseApk")
        val result2 = execute("", "publishReleaseApk")

        assertThat(result1.task(":publishReleaseApk")).isNotNull()
        assertThat(result1.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result2.task(":publishReleaseApk")).isNotNull()
        assertThat(result2.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `Using non-existent custom artifact fails build with warning`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${playgroundDir.escaped()}')
            }
        """

        val result = execute(config, "publishReleaseApk")

        assertThat(result.task(":packageRelease")).isNull()
        assertThat(result.task(":publishReleaseApk")).isNotNull()
        assertThat(result.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
    }

    @Test
    fun `Using custom artifact with multiple APKs uploads each one`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${playgroundDir.escaped()}')
            }
        """

        File(playgroundDir, "1.apk").safeCreateNewFile()
        File(playgroundDir, "2.apk").safeCreateNewFile()
        val result = execute(config, "publishReleaseApk")

        assertThat(result.task(":publishReleaseApk")).isNotNull()
        assertThat(result.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("1.apk")
        assertThat(result.output).contains("2.apk")
    }

    @Test
    fun `Using custom artifact skips on-the-fly apk build`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${playgroundDir.escaped()}')
            }
        """

        File(playgroundDir, "foo.apk").safeCreateNewFile()
        val result = execute(config, "publishReleaseApk")

        assertThat(result.task(":packageRelease")).isNull()
        assertThat(result.task(":publishReleaseApk")).isNotNull()
        assertThat(result.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadApk(")
        assertThat(result.output).contains(playgroundDir.name)
    }

    @Test
    fun `Using custom artifact file skips on-the-fly apk build`() {
        val app = File(playgroundDir, "foo.apk").safeCreateNewFile()
        // language=gradle
        val config = """
            play {
                artifactDir = file('${app.escaped()}')
            }
        """

        val result = execute(config, "publishReleaseApk")

        assertThat(result.task(":packageRelease")).isNull()
        assertThat(result.task(":publishReleaseApk")).isNotNull()
        assertThat(result.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadApk(")
        assertThat(result.output).contains(playgroundDir.name)
        assertThat(result.output).contains("foo.apk")
    }

    @Test
    fun `Reusing custom artifact uses cached build`() {
        // language=gradle
        val config = """
            play {
                artifactDir = file('${playgroundDir.escaped()}')
            }
        """

        File(playgroundDir, "foo.apk").safeCreateNewFile()
        val result1 = execute(config, "publishReleaseApk")
        val result2 = execute(config, "publishReleaseApk")

        assertThat(result1.task(":publishReleaseApk")).isNotNull()
        assertThat(result1.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result2.task(":publishReleaseApk")).isNotNull()
        assertThat(result2.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `Using custom artifact correctly tracks dependencies`() {
        // language=gradle
        val config = """
            abstract class CustomTask extends DefaultTask {
                @OutputDirectory
                abstract DirectoryProperty getAppDir()

                @TaskAction
                void doStuff() {
                    appDir.get().file("foo.apk").asFile.createNewFile()
                }
            }

            def c = tasks.register("myCustomTask", CustomTask) {
                appDir.set(layout.projectDirectory.dir('${playgroundDir.escaped()}'))
            }

            play {
                artifactDir = c.flatMap { it.appDir }
            }
        """

        val result = execute(config, "publishReleaseApk")

        assertThat(result.task(":packageRelease")).isNull()
        assertThat(result.task(":myCustomTask")).isNotNull()
        assertThat(result.task(":myCustomTask")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":publishReleaseApk")).isNotNull()
        assertThat(result.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadApk(")
        assertThat(result.output).contains(playgroundDir.name)
        assertThat(result.output).contains("foo.apk")
    }

    @Test
    fun `Using custom artifact CLI arg skips on-the-fly APK build`() {
        File(playgroundDir, "foo.apk").safeCreateNewFile()
        val result = execute("", "publishReleaseApk", "--artifact-dir=${playgroundDir}")

        assertThat(result.task(":packageRelease")).isNull()
        assertThat(result.task(":publishReleaseApk")).isNotNull()
        assertThat(result.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadApk(")
        assertThat(result.output).contains(playgroundDir.name)
    }

    @Test
    fun `CLI params can be used to configure task`() {
        val result = execute(
                "",
                "publishReleaseApk",
                "--no-commit",
                "--release-name=myRelName",
                "--release-status=draft",
                "--resolution-strategy=ignore",
                "--track=myCustomTrack",
                "--user-fraction=.88",
                "--update-priority=3"
        )

        assertThat(result.task(":publishReleaseApk")).isNotNull()
        assertThat(result.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("publishApk(")
        assertThat(result.output).contains("releaseName=myRelName")
        assertThat(result.output).contains("releaseStatus=DRAFT")
        assertThat(result.output).contains("strategy=IGNORE")
        assertThat(result.output).contains("trackName=myCustomTrack")
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
                "publishApk",
                "--no-commit",
                "--release-name=myRelName",
                "--release-status=draft",
                "--resolution-strategy=ignore",
                "--track=myCustomTrack",
                "--user-fraction=.88",
                "--update-priority=3"
        )

        assertThat(result.task(":publishReleaseApk")).isNotNull()
        assertThat(result.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("publishApk(")
        assertThat(result.output).contains("releaseName=myRelName")
        assertThat(result.output).contains("releaseStatus=DRAFT")
        assertThat(result.output).contains("strategy=IGNORE")
        assertThat(result.output).contains("trackName=myCustomTrack")
        assertThat(result.output).contains("userFraction=0.88")
        assertThat(result.output).contains("updatePriority=3")
        assertThat(result.output).contains("insertEdit()")
        assertThat(result.output).doesNotContain("commitEdit(")
    }

    @Test
    fun `Eagerly evaluated global CLI artifact-dir param skips on-the-fly APK build`() {
        // language=gradle
        val config = """
            playConfigs {
                release {
                    track.set('hello')
                }
            }

            tasks.all {}
        """.withAndroidBlock()

        File(playgroundDir, "foo.apk").safeCreateNewFile()
        val result = execute(
                config,
                "publishApk",
                "--artifact-dir=$playgroundDir"
        )

        assertThat(result.task(":packageRelease")).isNull()
        assertThat(result.task(":publishReleaseApk")).isNotNull()
        assertThat(result.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("publishApk(")
        assertThat(result.output).contains("trackName=hello")
        assertThat(result.output).contains(playgroundDir.name)
    }

    @Test
    fun `Build generates and commits edit by default`() {
        val result = execute("", "publishReleaseApk")

        assertThat(result.task(":publishReleaseApk")).isNotNull()
        assertThat(result.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("insertEdit()")
        assertThat(result.output).contains("commitEdit(edit-id)")
    }

    @Test
    fun `Build skips commit when no-commit flag is passed`() {
        // language=gradle
        val config = """
            play.commit = false
        """

        val result = execute(config, "publishReleaseApk")

        assertThat(result.task(":publishReleaseApk")).isNotNull()
        assertThat(result.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
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

        val result1 = execute(config1, "publishReleaseApk")
        val result2 = execute(config2, "publishReleaseApk")

        assertThat(result1.task(":publishReleaseApk")).isNotNull()
        assertThat(result1.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result1.output).contains("publishApk(")
        assertThat(result1.output).contains("didPreviousBuildSkipCommit=false")

        assertThat(result2.task(":publishReleaseApk")).isNotNull()
        assertThat(result2.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result2.output).contains("publishApk(")
        assertThat(result2.output).contains("didPreviousBuildSkipCommit=true")
    }

    @Test
    fun `Build processes manifest when resolution strategy is set to auto`() {
        // language=gradle
        val config = """
            play.resolutionStrategy = ResolutionStrategy.AUTO
        """

        val result = execute(config, "publishReleaseApk")

        assertThat(result.task(":processReleaseVersionCodes")).isNotNull()
        assertThat(result.task(":processReleaseVersionCodes")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":publishReleaseApk")).isNotNull()
        assertThat(result.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("findMaxAppVersionCode(")
        assertThat(result.output).contains("uploadApk(")
    }

    @Test
    fun `Build uploads mapping file when available`() {
        // language=gradle
        val config = """
            buildTypes.release {
                shrinkResources true
                minifyEnabled true
                proguardFiles(getDefaultProguardFile("proguard-android.txt"))
            }
        """.withAndroidBlock()

        val result = execute(config, "publishReleaseApk")

        assertThat(result.task(":publishReleaseApk")).isNotNull()
        assertThat(result.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadApk(")
        assertThat(result.output).contains("mappingFile=")
        assertThat(result.output).doesNotContain("mappingFile=null")
    }

    @Test
    fun `Build fails by default when upload fails`() {
        // language=gradle
        val config = """
            System.setProperty("FAIL", "true")
        """

        val result = executeExpectingFailure(config, "publishReleaseApk")

        assertThat(result.task(":publishReleaseApk")).isNotNull()
        assertThat(result.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("Upload failed")
    }

    @Test
    fun `Build doesn't publish APKs when no uploads succeeded`() {
        // language=gradle
        val config = """
            System.setProperty("SOFT_FAIL", "true")
        """

        val result = execute(config, "publishReleaseApk")

        assertThat(result.task(":publishReleaseApk")).isNotNull()
        assertThat(result.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadApk(")
        assertThat(result.output).contains("Soft failure")
        assertThat(result.output).contains("publishApk(versionCodes=[]")
    }

    @Test
    fun `Build uploads multiple APKs when splits are used`() {
        // language=gradle
        val config = """
            splits.density {
                enable true
                reset()
                include "xxhdpi", "xxxhdpi"
            }

            def versionCodes = ''
            def count = 0
            applicationVariants.all { variant ->
                if (variant.name == 'release') {
                    variant.outputs.each { output ->
                        output.versionCodeOverride = count++
                        versionCodes += count + ', '
                    }
                }
            }

            afterEvaluate {
                System.setProperty("VERSION_CODES", versionCodes.take(versionCodes.length() - 2))
            }
        """.withAndroidBlock()

        val result = execute(config, "publishReleaseApk")

        assertThat(result.task(":publishReleaseApk")).isNotNull()
        assertThat(result.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output.split("\n").filter {
            it.contains("uploadApk(")
        }).hasSize(3)
        assertThat(result.output).contains("app-universal")
        assertThat(result.output).contains("app-xxxhdpi")
        assertThat(result.output).contains("app-xxhdpi")
        assertThat(result.output.split("\n").filter {
            it.contains("publishApk(")
        }).hasSize(1)
        assertThat(result.output).contains("versionCodes=[1, 2, 3]")
    }

    @Test
    fun `Build uses correct version code`() {
        // language=gradle
        val config = """
            System.setProperty("VERSION_CODES", "8")
        """

        val result = execute(config, "publishReleaseApk")

        assertThat(result.task(":publishReleaseApk")).isNotNull()
        assertThat(result.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadApk(")
        assertThat(result.output).contains("versionCodes=[8]")
    }

    @Test
    fun `Build uses correct track`() {
        // language=gradle
        val config = """
            play.track = 'myCustomTrack'
        """

        val result = execute(config, "publishReleaseApk")

        assertThat(result.task(":publishReleaseApk")).isNotNull()
        assertThat(result.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadApk(")
        assertThat(result.output).contains("trackName=myCustomTrack")
    }

    @Test
    fun `Build uses correct release status`() {
        // language=gradle
        val config = """
            play.releaseStatus = ReleaseStatus.DRAFT
        """

        val result = execute(config, "publishReleaseApk")

        assertThat(result.task(":publishReleaseApk")).isNotNull()
        assertThat(result.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("uploadApk(")
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

        val result = execute(config, "publishConsoleNamesApk")

        assertThat(result.task(":publishConsoleNamesApk")).isNotNull()
        assertThat(result.task(":publishConsoleNamesApk")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("publishApk(")
        assertThat(result.output).contains("releaseName=myDefaultName")
    }

    @Test
    fun `Build picks track specific release name when available`() {
        // language=gradle
        val config = """
            android.buildTypes {
                consoleNames {}
            }

            play.track = 'custom-track'
        """

        val result = execute(config, "publishConsoleNamesApk")

        assertThat(result.task(":publishConsoleNamesApk")).isNotNull()
        assertThat(result.task(":publishConsoleNamesApk")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("publishApk(")
        assertThat(result.output).contains("releaseName=myCustomName")
    }

    @Test
    fun `Build ignores promote track specific release name when available`() {
        // language=gradle
        val config = """
            android.buildTypes {
                consoleNames {}
            }

            play.track = 'custom-track'
            play.promoteTrack = 'promote-track'
        """

        val result = execute(config, "publishConsoleNamesApk")

        assertThat(result.task(":publishConsoleNamesApk")).isNotNull()
        assertThat(result.task(":publishConsoleNamesApk")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("publishApk(")
        assertThat(result.output).contains("releaseName=myCustomName")
    }

    @Test
    fun `Build picks default release notes when no track specific ones are available`() {
        // language=gradle
        val config = """
            buildTypes {
                releaseNotes {}
            }
        """.withAndroidBlock()

        val result = execute(config, "publishReleaseNotesApk")

        assertThat(result.task(":publishReleaseNotesApk")).isNotNull()
        assertThat(result.task(":publishReleaseNotesApk")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("publishApk(")
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

            play.track = 'custom-track'
        """

        val result = execute(config, "publishReleaseNotesApk")

        assertThat(result.task(":publishReleaseNotesApk")).isNotNull()
        assertThat(result.task(":publishReleaseNotesApk")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("publishApk(")
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

            play.track = 'custom-track'
            play.promoteTrack = 'promote-track'
        """

        val result = execute(config, "publishReleaseNotesApk")

        assertThat(result.task(":publishReleaseNotesApk")).isNotNull()
        assertThat(result.task(":publishReleaseNotesApk")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("publishApk(")
        assertThat(result.output).contains("releaseNotes={en-US=Custom track release notes, " +
                                                   "fr-FR=Mes notes de mise à jour}")
    }

    @Test
    fun `Build uses correct user fraction`() {
        // language=gradle
        val config = """
            play.userFraction = 0.123d
        """

        val result = execute(config, "publishReleaseApk")

        assertThat(result.task(":publishReleaseApk")).isNotNull()
        assertThat(result.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("publishApk(")
        assertThat(result.output).contains("userFraction=0.123")
    }

    @Test
    fun `Build uses correct update priority`() {
        // language=gradle
        val config = """
            play.updatePriority = 5
        """

        val result = execute(config, "publishReleaseApk")

        assertThat(result.task(":publishReleaseApk")).isNotNull()
        assertThat(result.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("publishApk(")
        assertThat(result.output).contains("updatePriority=5")
    }

    @Test
    fun `Build uses correct retained artifacts`() {
        // language=gradle
        val config = """
            play.retain.artifacts = [1l, 2l, 3l]
        """

        val result = execute(config, "publishReleaseApk")

        assertThat(result.task(":publishReleaseApk")).isNotNull()
        assertThat(result.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("publishApk(")
        assertThat(result.output).contains("retainableArtifacts=[1, 2, 3]")
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

        assertThat(result.task(":publishReleaseApk")).isNotNull()
        assertThat(result.task(":publishReleaseApk")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
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

                override fun commitEdit(id: String) {
                    println("commitEdit($id)")
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
                        strategy: ResolutionStrategy,
                        mainObbRetainable: Int?,
                        patchObbRetainable: Int?
                ): Long? {
                    println("uploadApk(" +
                                    "apkFile=$apkFile, " +
                                    "mappingFile=$mappingFile, " +
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

                override fun publishApk(
                        versionCodes: List<Long>,
                        didPreviousBuildSkipCommit: Boolean,
                        trackName: String,
                        releaseStatus: ReleaseStatus?,
                        releaseName: String?,
                        releaseNotes: Map<String, String?>?,
                        userFraction: Double?,
                        updatePriority: Int?,
                        retainableArtifacts: List<Long>?
                ) {
                    println("publishApk(" +
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
