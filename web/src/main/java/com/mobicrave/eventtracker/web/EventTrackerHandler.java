package com.mobicrave.eventtracker.web;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.mobicrave.eventtracker.Criterion;
import com.mobicrave.eventtracker.EventTracker;
import com.mobicrave.eventtracker.base.DateHelper;
import com.mobicrave.eventtracker.model.Event;
import com.mobicrave.eventtracker.model.User;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EventTrackerHandler extends AbstractHandler {
  private final EventTracker eventTracker;
  private final DateHelper dateHelper;

  public EventTrackerHandler(EventTracker eventTracker, DateHelper dateHelper) {
    this.eventTracker = eventTracker;
    this.dateHelper = dateHelper;
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
        response.getWriter().println(eventId);
        break;
      case "/get_event_types":
        response.getWriter().println(getEventTypes());
      case "/add_event_type":
        addEventType(request);
        response.getWriter().println("OK");
        break;
      case "/count_funnel_steps":
        int[] funnelSteps = countFunnelSteps(request);
        response.getWriter().println(Arrays.toString(funnelSteps));
        break;
      case "/varz":
        response.getWriter().println(eventTracker.getVarz());
        break;
    }
    baseRequest.setHandled(true);
  }

  private String getEventTypes() {
    Gson gson = new Gson();
    return gson.toJson(eventTracker.getEventTypes());
  }

  private long addUser(HttpServletRequest request) {
    return eventTracker.addUser(new User.Builder(
        request.getParameter("external_user_id"),
        toProperties(request)).build());
  }

  private void addEventType(HttpServletRequest request) {
    eventTracker.addEventType(request.getParameter("event_type"));
  }

  private synchronized long addEvent(final HttpServletRequest request) {
    String date = request.getParameter("date");
    if (date == null) {
      date = dateHelper.getDate();
    }
    Event event = new Event.Builder(
        request.getParameter("event_type"),
        request.getParameter("external_user_id"),
        date,
        toProperties(request)).build();
    return eventTracker.addEvent(event);
  }

  private int[] countFunnelSteps(HttpServletRequest request) {
    List<Criterion> eventCriteria = getCriterions(request.getParameterValues("eck"),
        request.getParameterValues("ecv"));
    List<Criterion> userCriteria = getCriterions(request.getParameterValues("uck"),
        request.getParameterValues("ucv"));
    return eventTracker.getCounts(
        request.getParameter("start_date"),
        request.getParameter("end_date"),
        request.getParameterValues("funnel_steps"),
        Integer.parseInt(request.getParameter("num_days_to_complete_funnel")),
        eventCriteria,
        userCriteria);
  }

  private List<Criterion> getCriterions(String[] criteriaKeys, String[] criteriaValues) {
    List<Criterion> eventCriteria = Lists.newArrayList();
    if (criteriaKeys != null) {
      for (int i = 0; i < criteriaKeys.length; i++) {
        eventCriteria.add(new Criterion(criteriaKeys[i], criteriaValues[i]));
      }
    }
    return eventCriteria;
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

    final EventTracker eventTracker = EventTracker.build(directory);
    EventTrackerHandler eventHandler = new EventTrackerHandler(eventTracker, new DateHelper());
    final Server server = new Server(8080);
    String webDir = EventTrackerHandler.class.getClassLoader().getResource("frontend").toExternalForm();

    ResourceHandler resourceHandler = new ResourceHandler();
    resourceHandler.setDirectoriesListed(false);
    resourceHandler.setWelcomeFiles(new String[] { "main.html" });
    resourceHandler.setResourceBase(webDir);
    HandlerList handlers = new HandlerList();
    handlers.setHandlers(new Handler[] { resourceHandler, eventHandler });
    server.setHandler(handlers);

    server.start();
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        if (server.isStarted()) {
          try {
            server.stop();
            eventTracker.close();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    },"Stop Jetty Hook"));

    server.join();
  }
}