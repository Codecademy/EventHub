package com.mobicrave.eventtracker.index;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Injector;
import com.mobicrave.eventtracker.integration.GuiceTestCase;
import com.mobicrave.eventtracker.model.Event;
import org.junit.Assert;
import org.junit.Test;

import javax.inject.Provider;
import java.util.List;
import java.util.Properties;

public class PropertiesIndexTest extends GuiceTestCase {
  @Test
  public void testAll() throws Exception {
    final String USER_ID = "foo";
    final String DATE = "20140101";
    Provider<PropertiesIndex> propertiesIndexProvider = getPropertiesIndexProvider();

    PropertiesIndex propertiesIndex = propertiesIndexProvider.get();
    List<Event> events = Lists.newArrayList(
        new Event.Builder("signup", USER_ID, DATE, ImmutableMap.of("experiment", "foo1", "treatment", "bar1")).build(),
        new Event.Builder("signup", USER_ID, DATE, ImmutableMap.of("experiment", "foo1", "treatment", "bar2")).build(),
        new Event.Builder("signup", USER_ID, DATE, ImmutableMap.of("hello", "world")).build(),
        new Event.Builder("signup", USER_ID, DATE, ImmutableMap.of("", "")).build(),
        new Event.Builder("submission", USER_ID, DATE, ImmutableMap.of("experiment", "foo2", "treatment", "bar3")).build(),
        new Event.Builder("submission", USER_ID, DATE, ImmutableMap.of("experiment", "foo2", "treatment", "bar3")).build(),
        new Event.Builder("submission", USER_ID, DATE, ImmutableMap.of("x", "y")).build()
    );
    for (Event event : events) {
      propertiesIndex.addEvent(event);
    }

    Assert.assertEquals(Lists.newArrayList("experiment", "hello", "treatment"), propertiesIndex.getKeys("signup"));
    Assert.assertEquals(Lists.newArrayList("foo1"), propertiesIndex.getValues("signup", "experiment"));
    Assert.assertEquals(Lists.newArrayList("bar1", "bar2"), propertiesIndex.getValues("signup", "treatment"));
    Assert.assertEquals(Lists.newArrayList("experiment", "treatment", "x"), propertiesIndex.getKeys("submission"));
    Assert.assertEquals(Lists.newArrayList("foo2"), propertiesIndex.getValues("submission", "experiment"));
    Assert.assertEquals(Lists.newArrayList("bar3"), propertiesIndex.getValues("submission", "treatment"));
    propertiesIndex.close();

    propertiesIndex = propertiesIndexProvider.get();
    Assert.assertEquals(Lists.newArrayList("experiment", "hello", "treatment"), propertiesIndex.getKeys("signup"));
    Assert.assertEquals(Lists.newArrayList("foo1"), propertiesIndex.getValues("signup", "experiment"));
    Assert.assertEquals(Lists.newArrayList("bar1", "bar2"), propertiesIndex.getValues("signup", "treatment"));
    Assert.assertEquals(Lists.newArrayList("experiment", "treatment", "x"), propertiesIndex.getKeys("submission"));
    Assert.assertEquals(Lists.newArrayList("foo2"), propertiesIndex.getValues("submission", "experiment"));
    Assert.assertEquals(Lists.newArrayList("bar3"), propertiesIndex.getValues("submission", "treatment"));
  }

  private Provider<PropertiesIndex> getPropertiesIndexProvider() {
    Properties prop = new Properties();
    prop.put("eventtracker.directory", getTempDirectory());

    Injector injector = createInjectorFor(prop, new PropertiesIndexModule());
    return injector.getProvider(PropertiesIndex.class);
  }
}
