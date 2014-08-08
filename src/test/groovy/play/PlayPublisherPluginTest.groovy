package play

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.hasSize
import static org.junit.Assert.assertEquals


class PlayPublisherPluginTest {

    @Test(expected = IllegalStateException.class)
    public void testThrowsOnLibraryProjects() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'android-library'
        project.apply plugin: 'play'
    }

    @Test
    public void testCreatesTasks() {
        Project project = evaluatableProject()
        project.evaluate()

        assertThat(project.getTasksByName("publishRelease", true), hasSize(1))
        assertEquals(project.tasks.publishRelease.inputFile, project.tasks.zipalignRelease.outputFile)
    }

    def evaluatableProject() {
        Project project = ProjectBuilder.builder().withProjectDir(new File("src/test/fixtures/android_app")).build();
        project.apply plugin: 'android'
        project.apply plugin: 'play'
        project.android {
            compileSdkVersion 20
            buildToolsVersion '20.0.0'

            buildTypes {
                release {
                    signingConfig signingConfigs.debug
                }
            }
        }
        return project
    }
}
