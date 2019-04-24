package io.swagger.swaggerhub.plugin;

import io.swagger.swaggerhub.plugin.services.DefinitionFileFinder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.DefaultMaven;
import org.apache.maven.Maven;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;

import java.io.File;
import java.util.Arrays;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * Copied from https://github.com/ahgittin/license-audit-maven-plugin
 */

/**
 * Use this as you would {@link AbstractMojoTestCase},
 * where you want more of the standard maven defaults to be set
 * (and where the {@link AbstractMojoTestCase} leaves them as null or empty).
 * This includes:
 * <li> local repo, repo sessions and managers configured
 * <li> maven default remote repos installed (NB: this does not use your ~/.m2 local settings)
 * <li> system properties are copies
 * <p>
 * No changes to subclass code is needed; this simply intercepts the {@link #newMavenSession(MavenProject)} method
 * used by the various {@link #lookupMojo(String, File)} methods.
 * <p>
 * This also provides new methods, {@link #newMavenSession()} to conveniently create a maven session,
 * and {@link #lookupConfiguredMojo(File, String)} so you don't have to always build the project yourself.
 */
public class BetterMojoRule extends MojoRule {

    protected MavenSession newMavenSession() {
        try {
            MavenExecutionRequest request = new DefaultMavenExecutionRequest();
            MavenExecutionResult result = new DefaultMavenExecutionResult();

            // populate sensible defaults, including repository basedir and remote repos
            MavenExecutionRequestPopulator populator;
            populator = getContainer().lookup(MavenExecutionRequestPopulator.class);
            populator.populateDefaults(request);

            // this is needed to allow java profiles to get resolved; i.e. avoid during project builds:
            // [ERROR] Failed to determine Java version for profile java-1.5-detected @ org.apache.commons:commons-parent:22, /Users/alex/.m2/repository/org/apache/commons/commons-parent/22/commons-parent-22.pom, line 909, column 14
            request.setSystemProperties(System.getProperties());

            // and this is needed so that the repo session in the maven session
            // has a repo manager, and it points at the local repo
            // (cf MavenRepositorySystemUtils.newSession() which is what is otherwise done)
            DefaultMaven maven = (DefaultMaven) getContainer().lookup(Maven.class);
            DefaultRepositorySystemSession repoSession =
                    (DefaultRepositorySystemSession) maven.newRepositorySession(request);
            repoSession.setLocalRepositoryManager(
                    new SimpleLocalRepositoryManagerFactory().newInstance(repoSession,
                            new LocalRepository(request.getLocalRepository().getBasedir())));

            @SuppressWarnings("deprecation")
            MavenSession session = new MavenSession(getContainer(),
                    repoSession,
                    request, result);
            return session;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extends the super to use the new {@link #newMavenSession()} introduced here
     * which sets the defaults one expects from maven; the standard test case leaves a lot of things blank
     */
    @Override
    public MavenSession newMavenSession(MavenProject project) {
        MavenSession session = newMavenSession();
        session.setCurrentProject(project);
        session.setProjects(Arrays.asList(project));
        return session;
    }

    /**
     * As {@link #lookupConfiguredMojo(MavenProject, String)} but taking the pom file
     * and creating the {@link MavenProject}.
     */
    public Mojo lookupConfiguredMojo(File pom, String goal) throws Exception {
        assertNotNull(pom);
        assertTrue(pom.exists());

        ProjectBuildingRequest buildingRequest = newMavenSession().getProjectBuildingRequest();
        ProjectBuilder projectBuilder = lookup(ProjectBuilder.class);
        MavenProject project = projectBuilder.build(pom, buildingRequest).getProject();

        return lookupConfiguredMojo(project, goal);
    }

    public static String getBasedir() {

        String path = System.getProperty("basedir");
        return path != null ? path : (new File("")).getAbsolutePath();
    }

    public static File getTestFile(String path) {
        return getTestFile(getBasedir(), DefinitionFileFinder.modifyPathForOperatingFileSystem(path));
    }

    public static File getTestFile(String basedir, String path) {
        File root = new File(basedir);
        if (!root.isAbsolute()) {
            root = new File(getBasedir(), basedir);
        }

        return new File(root, path);
    }

}
