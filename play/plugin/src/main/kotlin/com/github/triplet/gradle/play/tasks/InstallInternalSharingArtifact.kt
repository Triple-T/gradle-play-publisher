package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.LoggerWrapper
import com.android.builder.testing.ConnectedDeviceProvider
import com.android.builder.testing.api.DeviceProvider
import com.android.ddmlib.MultiLineReceiver
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.common.annotations.VisibleForTesting
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.util.concurrent.ExecutionException
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
        val uploads = uploadedArtifacts.get().asFileTree
        val latestUpload = checkNotNull(uploads.maxBy { it.nameWithoutExtension.toLong() }) {
            "Failed to find uploaded artifacts in ${uploads.joinToString()}"
        }
        val launchUrl = latestUpload.inputStream().use {
            JacksonFactory.getDefaultInstance().createJsonParser(it).parse(Map::class.java)
        }["downloadUrl"] as String

        val shell = AdbShell(extension)
        val result = shell.executeShellCommand(
                "am start -a \"android.intent.action.VIEW\" -d $launchUrl")
        check(result) {
            "Failed to install on any devices."
        }
    }

    interface AdbShell {
        fun executeShellCommand(command: String): Boolean

        interface Factory {
            fun create(extension: AppExtension): AdbShell
        }

        companion object {
            private var factory: Factory = DefaultAdbShell

            @VisibleForTesting
            fun setFactory(factory: Factory) {
                Companion.factory = factory
            }

            operator fun invoke(
                    extension: AppExtension
            ): AdbShell = factory.create(extension)
        }
    }

    private class DefaultAdbShell(
            private val deviceProvider: DeviceProvider,
            private val timeOutInMs: Long
    ) : AdbShell {
        override fun executeShellCommand(command: String): Boolean {
            // TODO(#708): employ the #use method instead when AGP 3.6 is the minimum
            deviceProvider.init()
            return try {
                try {
                    launchIntents(deviceProvider, command)
                } catch (e: Exception) {
                    throw ExecutionException(e)
                }
            } finally {
                deviceProvider.terminate()
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
            override fun create(extension: AppExtension): AdbShell {
                val deviceProvider = ConnectedDeviceProvider(
                        extension.adbExecutable,
                        extension.adbOptions.timeOutInMs,
                        LoggerWrapper(Logging.getLogger(InstallInternalSharingArtifact::class.java))
                )
                return DefaultAdbShell(deviceProvider, extension.adbOptions.timeOutInMs.toLong())
            }
        }
    }
}
