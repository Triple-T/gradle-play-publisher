package com.github.triplet.gradle.play.lint

import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.OtherFileScanner
import com.android.tools.lint.detector.api.Scope
import java.io.File
import java.nio.file.Path

abstract class PlayDetectorBase : Detector(), OtherFileScanner {

    private var projectDir: Path? = null

    override fun getApplicableFiles() = Scope.OTHER_SCOPE

    override fun beforeCheckProject(context: Context) {
        projectDir = if (context.project.isAndroidProject) {
            context.file.toPath()
        } else {
            null
        }
    }

    override fun beforeCheckFile(context: Context) {
        projectDir?.let {
            val relativePath = it.relativize(context.file.toPath())

            // Check for src/main/play/[en-US]/[listing]

            if (relativePath[0] != "src") return
            if (relativePath[1] == null) return
            if (relativePath[2] != "play") return

            val path = relativePath[3] ?: return

            AppDetail.values().firstOrNull { it.fileName == path }?.let {
                checkAppDetail(context, it)
                return
            }

            if (LocaleFileFilter.accept(File(path))) {
                val path2 = relativePath[4] ?: return

                if (path2.startsWith(ListingDetail.WHATS_NEW.fileName)) {
                    checkListingDetail(context, ListingDetail.WHATS_NEW)
                    return
                } else if (path2 == "listing") {
                    val listingPath = relativePath[5] ?: return

                    ListingDetail.values().firstOrNull { it.fileName == listingPath }?.let {
                        checkListingDetail(context, it)
                        return
                    }

                    ImageType.values().firstOrNull { it.fileName == listingPath }?.let {
                        checkImageType(context, it)
                        return
                    }
                }
            }

            // TODO: Warn about unrecognized file?!
        }
    }

    internal open fun checkAppDetail(context: Context, appDetail: AppDetail) {}

    internal open fun checkListingDetail(context: Context, listingDetail: ListingDetail) {}

    internal open fun checkImageType(context: Context, imageType: ImageType) {}

    override fun afterCheckProject(context: Context) {
        projectDir = null
    }

}
