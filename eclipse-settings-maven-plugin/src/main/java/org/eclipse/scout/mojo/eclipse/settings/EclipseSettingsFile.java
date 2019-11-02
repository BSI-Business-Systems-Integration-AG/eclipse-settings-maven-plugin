package org.eclipse.scout.mojo.eclipse.settings;

public class EclipseSettingsFile {
  private String name;
  private String location;

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(final String location) {
    this.location = location;
  }
}
