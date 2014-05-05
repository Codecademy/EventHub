package com.codecademy.eventhub.storage;

import com.google.common.cache.Cache;
import com.codecademy.eventhub.model.Event;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class CachedEventStorage extends DelegateEventStorage {
  private final Cache<Long, Event> eventCache;

  public CachedEventStorage(EventStorage eventStorage, Cache<Long, Event> eventCache) {
    super(eventStorage);
    this.eventCache = eventCache;
  }

  @Override
  public Event getEvent(final long eventId) {
    try {
      return eventCache.get(eventId, new Callable<Event>() {
        @Override
        public Event call() {
          return CachedEventStorage.super.getEvent(eventId);
        }
      });
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getVarz(int indentation) {
    String indent  = new String(new char[indentation]).replace('\0', ' ');
    return String.format(
        "%s\n\n" +
        indent + this.getClass().getName() + "\n" +
        indent + "==================\n" +
        indent + "eventCache: %s",
        super.getVarz(indentation), eventCache.stats().toString());
  }
}
