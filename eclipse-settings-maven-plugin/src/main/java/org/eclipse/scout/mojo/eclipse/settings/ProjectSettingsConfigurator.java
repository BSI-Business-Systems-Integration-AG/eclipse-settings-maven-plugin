package org.eclipse.scout.mojo.eclipse.settings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name = "eclipse-settings", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresDependencyResolution = ResolutionScope.NONE)
public class ProjectSettingsConfigurator extends AbstractMojo {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProjectSettingsConfigurator.class);

  /**
   * The Maven Project.
   */
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  protected MavenProject project;

  /**
   * The Plugin Descriptor
   */
  @Parameter(defaultValue = "${plugin}", readonly = true, required = true)
  private PluginDescriptor plugin;

  /**
   * The maven-eclipse-plugin allows you to move settings files from one
   * location to another. You use that to put each configuration file from
   * your settings JAR in the right location:
   *
   * <pre>
   * &lt;plugin&gt;
   *     &lt;...&gt;
   *     &lt;configuration&gt;
   *         &lt;additionalConfig&gt;
   *             &lt;file&gt;
   *                 &lt;name&gt;.settings/org.eclipse.jdt.core.prefs&lt;/name&gt;
   *                 &lt;location&gt;/org.eclipse.jdt.core.prefs&lt;/location&gt;
   *             &lt;/file&gt;
   *             &lt;file&gt;
   *                 &lt;name&gt;.settings/org.eclipse.jdt.ui.prefs&lt;/name&gt;
   *                &lt;location&gt;/org.eclipse.jdt.ui.prefs&lt;/location&gt;
   *             &lt;/file&gt;
   *             &lt;!-- and more... --&gt;
   *         &lt;/additionalConfig&gt;
   *     &lt;/configuration&gt;
   * &lt;/plugin&gt;
   * </pre>
   */
  @Parameter
  private EclipseSettingsFile[] additionalConfig;

  /**
   * The maven-eclipse-plugin allows you to move settings files from one
   * location to another. You use that to put each configuration file from
   * your settings in the right location:
   *
   * <pre>
   * &lt;plugin&gt;
   *     &lt;...&gt;
   *     &lt;configuration&gt;
   *         &lt;localAdditionalConfig&gt;
   *             &lt;file&gt;
   *                 &lt;name&gt;${session.executionRootDirectory}/.settings/org.eclipse.jdt.core.prefs&lt;/name&gt;
   *                 &lt;location&gt;/org.eclipse.jdt.core.prefs&lt;/location&gt;
   *             &lt;/file&gt;
   *             &lt;file&gt;
   *                 &lt;name&gt;${session.executionRootDirectory}/.settings/org.eclipse.jdt.ui.prefs&lt;/name&gt;
   *                &lt;location&gt;/org.eclipse.jdt.ui.prefs&lt;/location&gt;
   *             &lt;/file&gt;
   *             &lt;!-- and more... --&gt;
   *         &lt;/localAdditionalConfig&gt;
   *     &lt;/configuration&gt;
   * &lt;/plugin&gt;
   * </pre>
   *
   * Note that a JAR is the recommended way, as it generate a stable path
   * (session.executionRootDirectory} might not correspond to the actual path.
   */
  @Parameter
  private EclipseSettingsFile[] localAdditionalConfig;

  /**
   * Set this to <code>true</code> to bypass project settings configuration.
   */
  @Parameter
  private boolean skip;

  @Component
  private BuildContext buildContext;

  public void setSkip(final boolean skip) {
    this.skip = skip;
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      if (skip) {
        LOGGER.info("Skipping project settings configuration.");
      } else if (configureEclipseMeta()) {
        LOGGER.info("Project configured.");
      } else {
        LOGGER.error("Project not configured.");
      }
    } catch (final IOException e) {
      LOGGER.error("Failure during settings configuration", e);
    }
  }

  private boolean configureEclipseMeta() throws IOException, MojoExecutionException {
    if (additionalConfig == null || additionalConfig.length <= 0) {
      LOGGER.warn("No settings specified.");
      return false;
    }

    final List<Artifact> artifacts = collectArtifacts();

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Resolved {} artifacts", artifacts.size());
      for (final Artifact artifact : artifacts) {
        LOGGER.debug(artifact.toString());
      }
    }

    writeAdditionalConfig(additionalConfig, new ArtifactResourceResolver(artifacts));
    writeAdditionalConfig(localAdditionalConfig, new FileSystemResourceResolver(project.getBasedir()));
    return true;
  }

  private List<Artifact> collectArtifacts() {
    final List<Plugin> plugins = new ArrayList<>();
    final PluginManagement pluginManagement = project.getBuild().getPluginManagement();
    if (null != pluginManagement) {
      plugins.addAll(pluginManagement.getPlugins());
    }
    plugins.addAll(project.getBuildPlugins());

    final Map<String, Artifact> artifactMap = plugin.getArtifactMap();

    final List<Artifact> artifacts = new ArrayList<>();
    for (final Plugin plugin : plugins) {
      if (pluginFilter(plugin)) {
        for (final Dependency dependency : plugin.getDependencies()) {
          final Artifact artifact = artifactMap.get(ArtifactUtils.versionlessKey(dependency.getGroupId(), dependency.getArtifactId()));
          if (null != artifact) {
            artifacts.add(artifact);
          }
        }
      }
    }
    return artifacts;
  }

  private boolean pluginFilter(final Plugin plugin) {
    return this.plugin.getPlugin().getKey().equals(plugin.getKey());
  }

  private void writeAdditionalConfig(final EclipseSettingsFile[] additionalConfig, final ResourceResolver resolver)
      throws MojoExecutionException {
    if (additionalConfig == null || additionalConfig.length == 0) {
      return;
    }
    LOGGER.info("Copying {} resources using {} resolver.", additionalConfig.length, resolver);

    final List<File> updatedFiles = new ArrayList<>();
    for (final EclipseSettingsFile config : additionalConfig) {
      final String location = config.getLocation();
      final File target = new File(project.getBasedir(), config.getName());

      try {
        final Resource source = resolver.getResource(location);

        /*
         * if we try to write to a directory, this will fail. We don't try to behave like mv which
         * relocate the
         * source into the folder (eg: mv a b will give b/a).
         */
        if (target.isDirectory()) {
          LOGGER.warn("{} is a directory, ignoring.", target.getAbsolutePath());
          continue;
        }

        target.getParentFile().mkdirs();

        LOGGER.info("Copying {} to {}", source, target);
        try (InputStream inStream = source.getResourceAsStream();
            OutputStream outStream = new FileOutputStream(target)) {
          IOUtil.copy(inStream, outStream);
        }
        updatedFiles.add(target);
      } catch (final IOException e) {
        throw new MojoExecutionException(String.format("Unable to copy %s to %s", location, target.getAbsolutePath()),
            e);
      } finally {
        for (final File file : updatedFiles) {
          buildContext.refresh(file);
        }
      }
    }
  }

}
