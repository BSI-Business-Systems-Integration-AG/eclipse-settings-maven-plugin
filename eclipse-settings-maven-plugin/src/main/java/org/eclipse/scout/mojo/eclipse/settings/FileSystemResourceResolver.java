package org.eclipse.scout.mojo.eclipse.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class FileSystemResourceResolver implements ResourceResolver {
  private final File baseDirectory;

  public FileSystemResourceResolver(final File baseDirectory) {
    this.baseDirectory = Objects.requireNonNull(baseDirectory, "baseDirectory");
  }

  @Override
  public Resource getResource(final String path) throws IOException {
    final File res = new File(path);
    if (res.isAbsolute()) {
      return new FileSystemResource(res);
    }
    return new FileSystemResource(new File(baseDirectory, path).getAbsoluteFile());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  static class FileSystemResource implements Resource {
    private final File path;

    public FileSystemResource(final File path) {
      this.path = path;
    }

    @Override
    public InputStream getResourceAsStream() throws IOException {
      return new FileInputStream(path);
    }

    @Override
    public String toString() {
      return path.toString();
    }
  }
}
