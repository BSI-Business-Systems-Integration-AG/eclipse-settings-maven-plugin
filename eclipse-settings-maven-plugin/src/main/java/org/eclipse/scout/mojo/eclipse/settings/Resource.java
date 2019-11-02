package org.eclipse.scout.mojo.eclipse.settings;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represent a {@link Resource}.
 *
 * @author gael.lhez
 */
public interface Resource {

  /**
   * Get the resource as stream, fail if not existing.
   *
   * @return an {@link InputStream}.
   * @throws IOException in case of IO error.
   */
  InputStream getResourceAsStream() throws IOException;

  /**
   * Return a representation of this as a String.
   * <p>
   * Implementations should override {@link #toString()} to provide a meaningful description of the {@link Resource}.
   *
   * @return representation of {@link Resource}, for example its path.
   */
  @Override
  String toString();
}
