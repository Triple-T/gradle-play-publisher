package de.triplet.gradle.play

import org.gradle.api.Task
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

class DependsOn extends TypeSafeMatcher<Task> {

    static dependsOn(String dependsOn) {
        return new DependsOn(dependsOn)
    }

    final mDependsOn

    DependsOn(dependsOn) {
        mDependsOn = dependsOn
    }

    @Override
    protected boolean matchesSafely(Task task) {
        return task.dependsOn.any {
            if (it instanceof Task && (it as Task).name == mDependsOn) {
                return true
            }

            if (it instanceof String && it == mDependsOn) {
                return true
            }

            return false
        }
    }

    @Override
    void describeTo(Description description) {
        description.appendText('Task to depend on ').appendValue(mDependsOn)
    }

    @Override
    void describeMismatchSafely(Task item, Description description) {
        description.appendText('doesn\'t')
    }
}
