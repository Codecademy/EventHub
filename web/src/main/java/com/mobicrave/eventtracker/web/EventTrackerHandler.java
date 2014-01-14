package com.mobicrave.eventtracker.web;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.mobicrave.eventtracker.*;
import com.mobicrave.eventtracker.index.EventIndex;
import com.mobicrave.eventtracker.index.UserEventIndex;
import com.mobicrave.eventtracker.model.Event;
import com.mobicrave.eventtracker.model.User;
import com.mobicrave.eventtracker.storage.EventStorage;
import com.mobicrave.eventtracker.storage.JournalEventStorage;
import com.mobicrave.eventtracker.storage.JournalUserStorage;
import com.mobicrave.eventtracker.storage.UserStorage;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class EventTrackerHandler extends AbstractHandler {
  private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("yyyyMMdd");
  private final EventTracker eventTracker;

  public EventTrackerHandler(EventTracker eventTracker) {
    this.eventTracker = eventTracker;
  }

  public void handle(String target, Request baseRequest, HttpServletRequest request,
      HttpServletResponse response) throws IOException, ServletException {
    response.setStatus(HttpServletResponse.SC_OK);
    switch (target) {
      case "/register_user":
        long userId = addUser(request);
        response.getWriter().println(userId);
        break;
      case "/track_event":
        long eventId = addEvent(request);
//        System.out.println(request.getParameter("date"));
        response.getWriter().println(eventId);
        break;
      case "/add_event_type":
        eventTracker.addEventType(request.getParameter("event_type"));
        response.getWriter().println("OK");
        break;
      case "/count_funnel_steps":
        int[] funnelSteps = countFunnelSteps(request);
        response.getWriter().println(Arrays.toString(funnelSteps));
        break;
    }
    baseRequest.setHandled(true);
  }

  private long addUser(HttpServletRequest request) {
    return eventTracker.addUser(new User.Builder(
        request.getParameter("external_user_id"),
        toProperties(request)).build());
  }

  private int[] countFunnelSteps(HttpServletRequest request) {
    return eventTracker.getCounts(
        request.getParameter("start_date"),
        request.getParameter("end_date"),
        request.getParameterValues("funnel_steps"),
        Integer.parseInt(request.getParameter("num_days_to_complete_funnel")));
  }

  private long addEvent(final HttpServletRequest request) {
    String date = request.getParameter("date");
    if (date == null) {
      date = new DateTime().toString(FORMATTER);
    }
    Event event = new Event.Builder(
        request.getParameter("event_type"),
        request.getParameter("external_user_id"),
        date,
        toProperties(request)).build();
    return eventTracker.addEvent(event);
  }

  private Map<String, String> toProperties(final HttpServletRequest request) {
    return Maps.asMap(request.getParameterMap().keySet(), new Function<String, String>() {
      @Override
      public String apply(String parameterName) {
        return request.getParameter(parameterName);
      }
    });
  }

  public static void main(String[] args) throws Exception {
    final String directory = "/tmp/event_tracker/";
    final String eventIndexDirectory = directory + "/event_index/";
    final String userEventIndexDirectory = directory + "/user_event_index/";
    final String eventStorageDirectory = directory + "/event_storage/";
    final String userStorageDirectory = directory + "/user_storage/";

    final EventIndex eventIndex = EventIndex.build(eventIndexDirectory);
    final UserEventIndex userEventIndex = UserEventIndex.build(userEventIndexDirectory);
    final EventStorage eventStorage = JournalEventStorage.build(eventStorageDirectory);
    final UserStorage userStorage = JournalUserStorage.build(userStorageDirectory);
//    final EventStorage eventStorage = MemEventStorage.build();
//    final UserStorage userStorage = MemUserStorage.build();

    EventTracker eventTracker = new EventTracker(eventIndex, userEventIndex, eventStorage, userStorage);

    EventTrackerHandler eventHandler = new EventTrackerHandler(eventTracker);
    final Server server = new Server(8080);
    server.setHandler(eventHandler);

    server.start();
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        if (server.isStarted()) {
          try {
            server.stop();
            eventStorage.close();
            userStorage.close();
            eventIndex.close();
            userEventIndex.close();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    },"Stop Jetty Hook"));

    server.join();
  }
}