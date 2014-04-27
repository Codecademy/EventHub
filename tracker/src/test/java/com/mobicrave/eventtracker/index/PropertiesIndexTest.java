package com.mobicrave.eventtracker.index;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Injector;
import com.mobicrave.eventtracker.integration.GuiceTestCase;
import com.mobicrave.eventtracker.model.Event;
import com.mobicrave.eventtracker.model.User;
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
    propertiesIndex.addUser(new User.Builder("user_id1", ImmutableMap.of("foo1", "bar1")).build());
    propertiesIndex.addUser(new User.Builder("user_id2", ImmutableMap.of("foo1", "bar2", "foo2", "bar3")).build());

    Assert.assertEquals(Lists.newArrayList("experiment", "hello", "treatment"), propertiesIndex.getEventKeys("signup"));
    Assert.assertEquals(Lists.newArrayList("foo1"), propertiesIndex.getEventValues("signup", "experiment", ""));
    Assert.assertEquals(Lists.newArrayList("bar1", "bar2"), propertiesIndex.getEventValues("signup", "treatment", ""));
    Assert.assertEquals(Lists.newArrayList("experiment", "treatment", "x"), propertiesIndex.getEventKeys("submission"));
    Assert.assertEquals(Lists.newArrayList("foo2"), propertiesIndex.getEventValues("submission", "experiment", ""));
    Assert.assertEquals(Lists.newArrayList("bar3"), propertiesIndex.getEventValues("submission", "treatment", ""));
    Assert.assertEquals(Lists.newArrayList("bar1", "bar2"), propertiesIndex.getEventValues("signup", "treatment", "bar"));
    Assert.assertEquals(Lists.newArrayList("bar1"), propertiesIndex.getEventValues("signup", "treatment", "bar1"));
    Assert.assertEquals(Lists.newArrayList("bar2"), propertiesIndex.getEventValues("signup", "treatment", "bar2"));

    Assert.assertEquals(Lists.newArrayList("foo1", "foo2"), propertiesIndex.getUserKeys());
    Assert.assertEquals(Lists.newArrayList("bar1", "bar2"), propertiesIndex.getUserValues("foo1", "b"));
    Assert.assertTrue(propertiesIndex.getUserValues("foo1", "c").isEmpty());
    Assert.assertEquals(Lists.newArrayList("bar3"), propertiesIndex.getUserValues("foo2", ""));
    propertiesIndex.close();

    propertiesIndex = propertiesIndexProvider.get();
    Assert.assertEquals(Lists.newArrayList("experiment", "hello", "treatment"), propertiesIndex.getEventKeys("signup"));
    Assert.assertEquals(Lists.newArrayList("foo1"), propertiesIndex.getEventValues("signup", "experiment", ""));
    Assert.assertEquals(Lists.newArrayList("bar1", "bar2"), propertiesIndex.getEventValues("signup", "treatment", ""));
    Assert.assertEquals(Lists.newArrayList("experiment", "treatment", "x"), propertiesIndex.getEventKeys("submission"));
    Assert.assertEquals(Lists.newArrayList("foo2"), propertiesIndex.getEventValues("submission", "experiment", ""));
    Assert.assertEquals(Lists.newArrayList("bar3"), propertiesIndex.getEventValues("submission", "treatment", ""));
    Assert.assertEquals(Lists.newArrayList("bar1", "bar2"), propertiesIndex.getEventValues("signup", "treatment", "bar"));
    Assert.assertEquals(Lists.newArrayList("bar1"), propertiesIndex.getEventValues("signup", "treatment", "bar1"));
    Assert.assertEquals(Lists.newArrayList("bar2"), propertiesIndex.getEventValues("signup", "treatment", "bar2"));

    Assert.assertEquals(Lists.newArrayList("foo1", "foo2"), propertiesIndex.getUserKeys());
    Assert.assertEquals(Lists.newArrayList("bar1", "bar2"), propertiesIndex.getUserValues("foo1", "b"));
    Assert.assertTrue(propertiesIndex.getUserValues("foo1", "c").isEmpty());
    Assert.assertEquals(Lists.newArrayList("bar3"), propertiesIndex.getUserValues("foo2", ""));
  }

  private Provider<PropertiesIndex> getPropertiesIndexProvider() {
    Properties prop = new Properties();
    prop.put("eventtracker.directory", getTempDirectory());

    Injector injector = createInjectorFor(prop, new PropertiesIndexModule());
    return injector.getProvider(PropertiesIndex.class);
  }
}
