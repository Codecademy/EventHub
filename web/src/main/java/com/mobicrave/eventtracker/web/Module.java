package com.mobicrave.eventtracker.web;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.mobicrave.eventtracker.EventTracker;
import com.mobicrave.eventtracker.base.KeyValueCallback;
import com.mobicrave.eventtracker.index.DatedEventIndex;
import com.mobicrave.eventtracker.index.PropertiesIndex;
import com.mobicrave.eventtracker.index.ShardedEventIndex;
import com.mobicrave.eventtracker.index.UserEventIndex;
import com.mobicrave.eventtracker.model.Event;
import com.mobicrave.eventtracker.model.User;
import com.mobicrave.eventtracker.storage.BloomFilteredEventStorage;
import com.mobicrave.eventtracker.storage.BloomFilteredUserStorage;
import com.mobicrave.eventtracker.web.commands.Command;
import com.mobicrave.eventtracker.web.commands.Path;
import org.reflections.Reflections;

import javax.inject.Named;
import javax.inject.Provider;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

public class Module extends AbstractModule {
  private static final String PACKAGE_NAME = "com.mobicrave.eventtracker.web.commands";

  @Override
  protected void configure() {}

  @Provides
  private Gson getGson() {
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.setPrettyPrinting();
    gsonBuilder.registerTypeAdapter(User.class, new UserJsonSerializer());
    gsonBuilder.registerTypeAdapter(Event.class, new EventJsonSerializer());
    return gsonBuilder.create();
  }

  @Provides
  @Singleton
  public EventTracker getEventTracker(
      @Named("eventtracker.directory") String directory,
      ShardedEventIndex shardedEventIndex,
      DatedEventIndex datedEventIndex,
      PropertiesIndex propertiesIndex,
      UserEventIndex userEventIndex,
      BloomFilteredEventStorage eventStorage,
      BloomFilteredUserStorage userStorage) {
    return new EventTracker(directory, shardedEventIndex, datedEventIndex, propertiesIndex,
        userEventIndex, eventStorage, userStorage);
  }

  @Provides
  private EventTrackerHandler getEventTrackerHandler(Injector injector, EventTracker eventTracker)
      throws ClassNotFoundException {
    Map<String, Provider<Command>> commandsMap = Maps.newHashMap();
    Reflections reflections = new Reflections(PACKAGE_NAME);
    Set<Class<? extends Command>> commandClasses = reflections.getSubTypesOf(Command.class);
    for (Class<? extends Command> commandClass : commandClasses) {
      String path = commandClass.getAnnotation(Path.class).value();
      //noinspection unchecked
      commandsMap.put(path, (Provider<Command>) injector.getProvider(commandClass));
    }
    return new EventTrackerHandler(eventTracker, commandsMap);
  }

  private static class UserJsonSerializer implements JsonSerializer<User> {
    @Override
    public JsonElement serialize(User user, Type type, JsonSerializationContext jsonSerializationContext) {
      final JsonObject jsonObject = new JsonObject();
      user.enumerate(new KeyValueCallback() {
        @Override
        public void callback(String key, String value) {
          jsonObject.addProperty(key, value);
        }
      });
      return jsonObject;
    }
  }

  private static class EventJsonSerializer implements JsonSerializer<Event> {
    @Override
    public JsonElement serialize(Event event, Type type, JsonSerializationContext jsonSerializationContext) {
      final JsonObject jsonObject = new JsonObject();
      event.enumerate(new KeyValueCallback() {
        @Override
        public void callback(String key, String value) {
          jsonObject.addProperty(key, value);
        }
      });
      return jsonObject;
    }
  }
}
