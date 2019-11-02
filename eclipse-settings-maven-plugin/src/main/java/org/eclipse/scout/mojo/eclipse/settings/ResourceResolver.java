package org.eclipse.scout.mojo.eclipse.settings;

import java.io.IOException;

/**
 * Resolve resource.
 *
 * @author gael.lhez
 */
public interface ResourceResolver {
  /**
   * Get some resource.
   *
   * @param path path to the resource
   * @return return a {@link Resource}
   * @throws java.io.FileNotFoundException fail if resource is not found.
   * @throws IOException in case of other IO exception.
   */
  Resource getResource(String path) throws IOException;
}
