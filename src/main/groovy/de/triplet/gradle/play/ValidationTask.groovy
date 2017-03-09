package de.triplet.gradle.play

import de.triplet.gradle.play.validation.*
import de.triplet.gradle.play.validation.impl.FullDescriptionValidator
import de.triplet.gradle.play.validation.impl.ShortDescriptionValidator
import de.triplet.gradle.play.validation.impl.TitleValidator
import de.triplet.gradle.play.validation.impl.WhatsnewValidator
import org.gradle.api.tasks.TaskAction

class ValidationTask extends PlayPublishTask {

    static def FILE_NAME_FOR_TITLE = "title"
    static def FILE_NAME_FOR_SHORT_DESCRIPTION = "shortdescription"
    static def FILE_NAME_FOR_FULL_DESCRIPTION = "fulldescription"
    static def FILE_NAME_FOR_WHATSNEW = "whatsnew"
    static def LISTING_PATH = "listing/"

    File inputFolder

    TitleValidator titleValidator = new TitleValidator()
    FullDescriptionValidator fullDescriptionValidator = new FullDescriptionValidator()
    ShortDescriptionValidator shortDescriptionValidator = new ShortDescriptionValidator()
    WhatsnewValidator whatsnewValidator = new WhatsnewValidator()

    @TaskAction
    validate() {
        Set<ValidationError> invalidFiles = new HashSet<>();

        // Matches if locale have the correct naming e.g. en-US for play store
        inputFolder.eachDirMatch(matcher) { dir ->
            File listingDir = new File(dir, LISTING_PATH)

            def fileWhatsNew = new File(dir, FILE_NAME_FOR_WHATSNEW)
            if(!whatsnewValidator.validate(fileWhatsNew)) {
                invalidFiles.add(new ValidationError(fileWhatsNew, whatsnewValidator.getMaxLength()))
            }

            // Check if listing directory exist
            if (listingDir.exists()) {
                File fileTitle = new File(listingDir, FILE_NAME_FOR_TITLE)
                File fileShortDescription = new File(listingDir, FILE_NAME_FOR_SHORT_DESCRIPTION)
                File fileFullDescription = new File(listingDir, FILE_NAME_FOR_FULL_DESCRIPTION)

                if (!titleValidator.validate(fileTitle)) {
                    invalidFiles.add(new ValidationError(fileTitle, titleValidator.getMaxLength()))
                }
                if (!shortDescriptionValidator.validate(fileShortDescription)) {
                    invalidFiles.add(new ValidationError(fileShortDescription, shortDescriptionValidator.getMaxLength()))
                }
                if (!fullDescriptionValidator.validate(fileFullDescription)) {
                    invalidFiles.add(new ValidationError(fileFullDescription, fullDescriptionValidator.getMaxLength()))
                }
            }
        }

        if(invalidFiles.size() != 0) {
            throw new ValidationException(invalidFiles.toArray(new ValidationError[invalidFiles.size()]))
        }
    }
}

