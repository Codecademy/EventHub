package com.codecademy.eventhub.index;

import com.google.inject.Injector;
import com.codecademy.eventhub.integration.GuiceTestCase;
import org.junit.Assert;
import org.junit.Test;

import javax.inject.Provider;
import java.util.Properties;

public class DatedEventIndexTest extends GuiceTestCase {
  @Test
  public void testAll() throws Exception {
    Provider<DatedEventIndex> datedEventIndexProvider = getDatedEventIndexProvider();

    DatedEventIndex datedEventIndex = datedEventIndexProvider.get();
    String[] dates = new String[] { "20130101", "20130102", "20131111", "20131201" };

    datedEventIndex.addEvent(1, dates[0]);
    datedEventIndex.addEvent(2, dates[0]);
    datedEventIndex.addEvent(3, dates[1]);
    datedEventIndex.addEvent(4, dates[1]);
    datedEventIndex.addEvent(5, dates[1]);
    datedEventIndex.addEvent(15, dates[1]);
    datedEventIndex.addEvent(16, dates[2]);
    datedEventIndex.addEvent(17, dates[2]);
    datedEventIndex.addEvent(18, dates[3]);
    datedEventIndex.addEvent(19, dates[3]);

    Assert.assertEquals(3, datedEventIndex.findFirstEventIdOnDate(1, 1));
    Assert.assertEquals(3, datedEventIndex.findFirstEventIdOnDate(2, 1));
    Assert.assertEquals(16, datedEventIndex.findFirstEventIdOnDate(2, 2));

    datedEventIndex.close();
    datedEventIndex = datedEventIndexProvider.get();

    Assert.assertEquals(3, datedEventIndex.findFirstEventIdOnDate(1, 1));
    Assert.assertEquals(3, datedEventIndex.findFirstEventIdOnDate(2, 1));
    Assert.assertEquals(16, datedEventIndex.findFirstEventIdOnDate(2, 2));
  }

  private Provider<DatedEventIndex> getDatedEventIndexProvider() {
    Properties prop = new Properties();
    prop.put("eventhub.directory", getTempDirectory());

    Injector injector = createInjectorFor(prop, new DatedEventIndexModule());
    return injector.getProvider(DatedEventIndex.class);
  }
}
