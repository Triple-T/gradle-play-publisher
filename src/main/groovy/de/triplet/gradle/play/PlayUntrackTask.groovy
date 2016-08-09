package de.triplet.gradle.play

import com.google.api.services.androidpublisher.model.Track
import org.gradle.api.tasks.TaskAction

class PlayUntrackTask extends PlayPublishTask {

    String channel = 'alpha'

    boolean untrackNotNeeded() {
      !extension.untrackFormat?.trim()
    }

    List<Integer> getVersionsToKeep(List<Integer> playStoreVersions) {
      def pattern = extension.untrackFormat
      if ("*".equals(pattern)) {
        return new ArrayList<Integer>()
      }
      List<Integer> newVersions = playStoreVersions.findAll {
        !(String.valueOf(it) ==~ /$pattern/)
      }
      return newVersions;
    }

    @TaskAction
    untrack() {
        if (untrackNotNeeded()) {
            return
        }

        super.publish()
        channel = extension.untrack
        Track trackInfo = edits.tracks().get(variant.applicationId, editId, channel).execute();
        List<Integer> newVersions = getVersionsToKeep(trackInfo.getVersionCodes());
        trackInfo.setVersionCodes(newVersions);

        edits.tracks().update(variant.applicationId, editId, channel, trackInfo).execute()
        edits.commit(variant.applicationId, editId).execute()
    }
}
