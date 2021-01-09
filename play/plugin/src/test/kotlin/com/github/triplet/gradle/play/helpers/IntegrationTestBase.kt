package com.github.triplet.gradle.play.helpers

import com.github.triplet.gradle.common.utils.orNull
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import kotlin.math.max
import kotlin.random.Random

abstract class IntegrationTestBase {
    @TempDir
    @JvmField
    var _tempDir: File? = null
    private val tempDir get() = _tempDir!!

    protected val appDir by lazy { File(tempDir, "app") }
    protected val playgroundDir by lazy { File(tempDir, UUID.randomUUID().toString()) }

    private val factoryInstallerStatement = run {
        try {
            javaClass.getDeclaredMethod("installFactories")
            "${javaClass.canonicalName}.installFactories()"
        } catch (_: NoSuchMethodException) {
            null
        }
    }

    @BeforeEach
    fun initTestResources() {
        File("src/test/fixtures/app").copyRecursively(appDir)
        File("src/test/fixtures/${javaClass.simpleName}").orNull()?.copyRecursively(appDir)
    }

    protected fun File.escaped() = toString().replace("\\", "\\\\")

    protected fun String.withAndroidBlock() = """
        android {
            $this
        }
    """.trimIndent()

    protected fun execute(config: String, vararg tasks: String) = execute(config, false, *tasks)

    protected fun executeExpectingFailure(config: String, vararg tasks: String) =
            execute(config, true, *tasks)

    protected fun executeGradle(
            expectFailure: Boolean,
            block: GradleRunner.() -> GradleRunner
    ): BuildResult = runWithTestDir { testDir ->
        val runner = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(appDir)
                .withTestKitDir(testDir)
                .let(block)

        // We're doing some pretty wack (and disgusting, shameful) shit to run integration tests without
        // actually publishing anything. The idea is have the build file call into the test class to run
        // some code. Unfortunately, it'll mostly be limited to printlns since we can't actually share
        // any state due to the same code being run in completely different classpaths (possibly
        // even different processes), but at least we can validate that tasks are trying to publish the
        // correct stuff now.
        runner.withPluginClasspath(runner.pluginClasspath + listOf(
                File("build/classes/kotlin/test"),
                File("../android-publisher/build/resources/testFixtures")
        ))

        val result = if (expectFailure) runner.buildAndFail() else runner.build()

        if ("--debug" !in runner.arguments) {
            println(result.output)
        }

        result
    }

    private fun execute(
            config: String,
            expectFailure: Boolean,
            vararg tasks: String
    ): BuildResult {
        val buildCacheDir = File(tempDir, "gradle").path.replace("\\", "\\\\")

        // language=gradle
        File(appDir, "settings.gradle").writeText("""
            buildCache.local.directory = new File('$buildCacheDir')
        """)

        // language=gradle
        File(appDir, "build.gradle").writeText("""
            import com.github.triplet.gradle.androidpublisher.ReleaseStatus
            import com.github.triplet.gradle.androidpublisher.ResolutionStrategy

            plugins {
                id 'com.android.application'
                id 'com.github.triplet.play'
            }

            allprojects {
                repositories {
                    google()
                    jcenter()
                }
            }

            android {
                compileSdkVersion 28

                defaultConfig {
                    applicationId "com.example.publisher"
                    minSdkVersion 21
                    targetSdkVersion 28
                    versionCode 1
                    versionName "1.0"
                }

                lintOptions {
                    checkReleaseBuilds false
                    abortOnError false
                }
            }

            play {
                serviceAccountCredentials = file('creds.json')
            }

            $config

            ${factoryInstallerStatement ?: ""}
        """)

        return executeGradle(expectFailure) {
            withArguments("-S", "--build-cache", *tasks)
        }
    }

    private companion object {
        val testDirPool: BlockingQueue<File>

        init {
            // Each test kit Gradle runner must start its own daemon which takes a significant
            // amount of time. Thus, we only want a small amount of concurrency. Furthermore,
            // having multiple test kits running in parallel without using different directories is
            // useless as they will all fight to acquire file locks, but initializing those
            // directories also takes some time.
            //
            // To balance all these tradeoffs, we generate a persistent, random set of test kit
            // directories and pool them for concurrent use by test threads.

            val random = Random(System.getProperty("user.name").hashCode())
            val tempDir = System.getProperty("java.io.tmpdir")

            val threads = max(1, Runtime.getRuntime().availableProcessors() / 4 - 1)
            val dirs = mutableListOf<File>()
            repeat(threads) {
                dirs.add(File("$tempDir/gppGradleTests${random.nextInt()}"))
            }

            testDirPool = ArrayBlockingQueue(threads, false, dirs)
        }

        fun <T> runWithTestDir(block: (File) -> T): T {
            val dir = testDirPool.take()
            try {
                return block(dir)
            } finally {
                testDirPool.put(dir)
            }
        }
    }
}
