package com.github.triplet.gradle.play.tasks

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.IDevice
import com.android.ddmlib.MultiLineReceiver
import com.google.api.client.json.gson.GsonFactory
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
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@DisableCachingByDefault
internal abstract class InstallInternalSharingArtifact @Inject constructor(
        private val extension: ApplicationExtension,
        private val componentExtension: ApplicationAndroidComponentsExtension,
        private val executor: WorkerExecutor,
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
        executor.noIsolation().submit(Installer::class) {
            uploadedArtifacts.set(uploads)
            adbExecutable.set(componentExtension.sdkComponents.adb)
            timeOutInMs.set(extension.installation.timeOutInMs)
        }
    }

    abstract class Installer : WorkAction<Installer.Params> {
        override fun execute() {
            val uploads = parameters.uploadedArtifacts.get().asFileTree
            val latestUpload = checkNotNull(
                    uploads.maxByOrNull { it.nameWithoutExtension.toLong() }
            ) { "Failed to find uploaded artifacts in ${uploads.joinToString()}" }
            val launchUrl = latestUpload.inputStream().use {
                GsonFactory.getDefaultInstance().createJsonParser(it).parse(Map::class.java)
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
                    timeOutInMs: Int,
            ): AdbShell = factory.create(adbExecutable, timeOutInMs)
        }
    }

    private class DefaultAdbShell(
            private val adb: AndroidDebugBridge,
            private val timeOutInMs: Long,
            private val targetSerial: String?,
    ) : AdbShell {
        override fun executeShellCommand(command: String): Boolean {
            return try {
                launchIntents(command)
            } finally {
                // Clean up ADB connection
                @Suppress("DEPRECATION")
                AndroidDebugBridge.disconnectBridge()
                AndroidDebugBridge.terminate()
            }
        }

        private fun launchIntents(command: String): Boolean {
            var successfulLaunches = 0
            val devices = getTargetDevices()

            if (devices.isEmpty()) {
                Logging.getLogger(InstallInternalSharingArtifact::class.java)
                    .warn("No connected devices found")
                return false
            }

            for (device in devices) {
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

        private fun getTargetDevices(): List<IDevice> {
            val allDevices = adb.devices

            return if (targetSerial != null) {
                allDevices.filter { it.serialNumber == targetSerial }
            } else {
                allDevices.toList()
            }
        }

        companion object : AdbShell.Factory {
            override fun create(adbExecutable: File, timeOutInMs: Int): AdbShell {
                // Initialize ADB bridge - using deprecated API as the replacement
                // requires significantly more infrastructure. The deprecation warnings
                // are less critical than the DeviceProvider removal in AGP 9.
                @Suppress("DEPRECATION")
                AndroidDebugBridge.initIfNeeded(false)

                val adb = AndroidDebugBridge.createBridge(
                        adbExecutable.absolutePath,
                        false,
                        timeOutInMs.toLong(),
                        TimeUnit.MILLISECONDS
                )

                // Wait for device list to populate
                var count = 0
                while (!adb.hasInitialDeviceList() && count < 50) {
                    Thread.sleep(100)
                    count++
                }

                if (!adb.hasInitialDeviceList()) {
                    Logging.getLogger(InstallInternalSharingArtifact::class.java)
                        .warn("Timeout waiting for device list from ADB")
                }

                return DefaultAdbShell(
                        adb,
                        timeOutInMs.toLong(),
                        System.getenv("ANDROID_SERIAL")
                )
            }
        }
    }
}
