package com.github.triplet.gradle.build

import org.ajoberstar.grgit.Grgit
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class GPPBuildPlugin : Plugin<Project> {
    private val isMaster get() = System.getenv("CIRCLE_BRANCH") == "master"
    private val isPr get() = System.getenv("CIRCLE_PULL_REQUEST") != null

    override fun apply(project: Project) {
        check(project === project.rootProject) { "Cannot apply build plugin to subprojects." }

        project.tasks.register("ciBuild") {
            if (isReleaseBuild()) {
                val buildTasks = allTasks("build")
                dependsOn(buildTasks)
                if (isSnapshotBuild()) {
                    dependsOn(allTasks("publish").mustRunAfter(buildTasks))
                } else if (isPublishBuild()) {
                    dependsOn(allTasks("publishPlugins").mustRunAfter(buildTasks))
                }
            } else {
                dependsOn(allTasks("check"))
            }
        }
    }

    private fun isReleaseBuild() = isMaster && !isPr

    private fun Task.isSnapshotBuild() =
            project.allprojects.any { it.version.toString().contains("snapshot", true) }

    private fun Task.isPublishBuild() = Grgit.open { dir = project.rootDir }.use {
        if (!it.status().isClean) return@use false

        val latestCommit = it.head()
        val latestTag = it.tag.list().maxBy { it.commit.dateTime } ?: return@use false

        latestCommit.id == latestTag.commit.id
    }

    private fun Task.allTasks(name: String) =
            project.allprojects.mapNotNull { it.tasks.findByName(name) }

    private fun List<Task>.mustRunAfter(tasks: List<Task>) = onEach { it.mustRunAfter(tasks) }
}
