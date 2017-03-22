package de.triplet.gradle.play

/*
import org.gradle.api.Task
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

class DependsOn(private val dependsOn: String) : TypeSafeMatcher<Task>() {
    override fun matchesSafely(task: Task): Boolean {
        return task.dependsOn.any {
            when (it) {
                is Task -> return it.name == dependsOn
                is String -> return it == dependsOn
                else -> return false
            }
        }
    }

    override fun describeTo(description: Description) {
        description.appendText("Task to depend on ").appendValue(dependsOn)
    }

    override fun describeMismatchSafely(item: Task, description: Description) {
        description.appendText("doesn\'t")
    }
}
*/
