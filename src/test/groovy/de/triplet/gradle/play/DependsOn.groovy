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
        if (task.dependsOn == null) {
            return false
        }

        for (def o : task.dependsOn) {
            if (Task.class.isAssignableFrom(o.class)) {
                if (((Task) o).name == mDependsOn) {
                    return true
                }
            }
        }

        return false
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
