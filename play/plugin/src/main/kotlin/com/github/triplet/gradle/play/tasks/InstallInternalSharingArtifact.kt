package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.LoggerWrapper
import com.android.build.gradle.internal.testing.ConnectedDeviceProvider
import com.android.builder.testing.api.DeviceProvider
import com.android.ddmlib.MultiLineReceiver
import com.google.api.client.json.jackson2.JacksonFactory
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal abstract class InstallInternalSharingArtifact @Inject constructor(
        private val extension: AppExtension
) : DefaultTask() {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    abstract val uploadedArtifacts: DirectoryProperty

    init {
        // Always out-of-date since we don't know anything about the target device
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun install() {
        val uploads = uploadedArtifacts
        project.serviceOf<WorkerExecutor>().noIsolation().submit(Installer::class) {
            uploadedArtifacts.set(uploads)
            adbExecutable.set(extension.adbExecutable)
            timeOutInMs.set(extension.adbOptions.timeOutInMs)
        }
    }

    abstract class Installer : WorkAction<Installer.Params> {
        override fun execute() {
            val uploads = parameters.uploadedArtifacts.get().asFileTree
            val latestUpload = checkNotNull(
                    uploads.maxBy { it.nameWithoutExtension.toLong() }
            ) { "Failed to find uploaded artifacts in ${uploads.joinToString()}" }
            val launchUrl = latestUpload.inputStream().use {
                JacksonFactory.getDefaultInstance().createJsonParser(it).parse(Map::class.java)
            }["downloadUrl"] as String

            val shell = AdbShell(
                    parameters.adbExecutable.get().asFile, parameters.timeOutInMs.get())
            val result = shell.executeShellCommand(
                    "am start -a \"android.intent.action.VIEW\" -d $launchUrl")
            check(result) {
                "Failed to install on any devices."
            }
        }

        interface Params : WorkParameters {
            val uploadedArtifacts: DirectoryProperty
            val adbExecutable: RegularFileProperty
            val timeOutInMs: Property<Int>
        }
    }

    interface AdbShell {
        fun executeShellCommand(command: String): Boolean

        interface Factory {
            fun create(adbExecutable: File, timeOutInMs: Int): AdbShell
        }

        companion object {
            private var factory: Factory = DefaultAdbShell

            internal fun setFactory(factory: Factory) {
                Companion.factory = factory
            }

            operator fun invoke(
                    adbExecutable: File,
                    timeOutInMs: Int
            ): AdbShell = factory.create(adbExecutable, timeOutInMs)
        }
    }

    private class DefaultAdbShell(
            private val deviceProvider: DeviceProvider,
            private val timeOutInMs: Long
    ) : AdbShell {
        override fun executeShellCommand(command: String): Boolean {
            return deviceProvider.use {
                launchIntents(deviceProvider, command)
            }
        }

        private fun launchIntents(deviceProvider: DeviceProvider, command: String): Boolean {
            var successfulLaunches = 0
            for (device in deviceProvider.devices) {
                val receiver = object : MultiLineReceiver() {
                    private var _hasErrored = false
                    val hasErrored get() = _hasErrored

                    override fun processNewLines(lines: Array<out String>) {
                        if (lines.any { it.contains("error", true) }) {
                            _hasErrored = true
                        }
                    }

                    override fun isCancelled() = false
                }

                device.executeShellCommand(
                        command,
                        receiver,
                        timeOutInMs,
                        TimeUnit.MILLISECONDS
                )

                if (!receiver.hasErrored) successfulLaunches++
            }

            return successfulLaunches > 0
        }

        companion object : AdbShell.Factory {
            override fun create(adbExecutable: File, timeOutInMs: Int): AdbShell {
                val deviceProvider = ConnectedDeviceProvider(
                        adbExecutable,
                        timeOutInMs,
                        LoggerWrapper(Logging.getLogger(
                                InstallInternalSharingArtifact::class.java))
                )
                return DefaultAdbShell(deviceProvider, timeOutInMs.toLong())
            }
        }
    }
}
