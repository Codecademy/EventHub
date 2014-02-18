package com.mobicrave.eventtracker.integration;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Properties;

public class GuiceTestCase {
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  protected Injector createInjectorFor(Properties properties, Module... modules) {
    return Guice.createInjector(Modules.override(modules).with(new ConfigModule(properties)));
  }

  protected String getTempDirectory() {
    try {
      return folder.newFolder("junit-test").getCanonicalPath() + "/";
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static class ConfigModule extends AbstractModule {
    private final Properties properties;

    private ConfigModule(Properties properties) {
      this.properties = properties;
    }

    @Override
    protected void configure() {
      Names.bindProperties(binder(), properties);
    }
  }
}
