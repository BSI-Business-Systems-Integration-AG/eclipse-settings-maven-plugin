package org.eclipse.scout.mojo.eclipse.settings;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;

public class ArtifactResourceResolver implements ResourceResolver {
  private final List<ArtifactJarFile> artifacts;

  public ArtifactResourceResolver(final List<Artifact> artifacts) throws IOException {
    this.artifacts = newArtifactJarFiles(artifacts);
  }

  @Override
  public Resource getResource(final String resource) throws IOException {
    final String path = resource.startsWith("/") ? resource.substring(1) : resource;

    for (final ArtifactJarFile artifact : this.artifacts) {
      final JarEntry entry = artifact.getJarEntry(path);
      if (null != entry) {
        return new ArtifactResourceImpl(artifact, entry);
      }
    }
    throw new FileNotFoundException(String.format("Unable to locate resource [%s] in artifacts", resource));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  private static List<ArtifactJarFile> newArtifactJarFiles(final List<Artifact> artifacts) throws IOException {
    final List<ArtifactJarFile> artifactJarFiles = new ArrayList<>(artifacts.size());
    for (final Artifact artifact : artifacts) {
      artifactJarFiles.add(new ArtifactJarFile(artifact));
    }
    return artifactJarFiles;
  }

  private static class ArtifactResourceImpl implements Resource {
    private final ArtifactJarFile artifactJarFile;
    private final JarEntry entry;

    public ArtifactResourceImpl(final ArtifactJarFile artifactJarFile, final JarEntry entry) {
      this.artifactJarFile = artifactJarFile;
      this.entry = entry;
    }

    @Override
    public InputStream getResourceAsStream() throws IOException {
      return artifactJarFile.getInputStream(entry);
    }

    @Override
    public String toString() {
      return artifactJarFile.artifact.toString() + "@" + entry;
    }

  }

  private static class ArtifactJarFile extends JarFile {
    private final Artifact artifact;

    private ArtifactJarFile(final Artifact artifact) throws IOException {
      super(artifact.getFile());
      this.artifact = artifact;
    }

  }
}
