package com.mobicrave.eventtracker.web.commands;

import com.google.gson.Gson;
import com.mobicrave.eventtracker.EventTracker;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Path("/events/types")
public class GetEventTypes extends Command {
  private final Gson gson;
  private final EventTracker eventTracker;

  @Inject
  public GetEventTypes(Gson gson, EventTracker eventTracker) {
    this.gson = gson;
    this.eventTracker = eventTracker;
  }

  public synchronized void execute(final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {
    List<String> eventTypes = eventTracker.getEventTypes();
    response.getWriter().println(gson.toJson(eventTypes));
  }
}
