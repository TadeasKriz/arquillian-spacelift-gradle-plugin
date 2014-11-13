package org.arquillian.spacelift.gradle

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

/**
 * Asserts that installations can be specified without home
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 *
 */
public class InstallationParsingTest {

    @Test
    public void noHomeInstallation() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.spacelift {
            tools { rhc {  command "rhc" } }
            profiles {
            }
            installations { eap { } }
            tests {
            }
        }

        // initialize current project tools - this is effectively init-tools task
        GradleSpacelift.currentProject(project)

        project.spacelift.installations.each { installation ->
            assertThat installation.home, is(notNullValue())
            assertThat installation.home.exists(), is(true)
        }
    }

    @Test
    public void preconditionTest() {

        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.spacelift {
            tools {
            }
            profiles {
            }
            tests {
            }
            installations {
                didNotMeetPreconditionInstallation {
                    preconditions { false }
                    postActions {
                        new File(System.getProperty("java.io.tmpdir"), "preconditionTestFile.tmp").createNewFile()
                    }
                }
            }
        }

        GradleSpacelift.currentProject(project)

        project.spacelift.installations.each { installation ->
            installation.install()
        }

        assertThat new File(System.getProperty("java.io.tmpdir"), "preconditionTestFile.tmp").exists(), is(false)
    }
}
