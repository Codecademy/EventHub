package com.mobicrave.eventtracker.web.commands;

import com.google.gson.Gson;
import com.mobicrave.eventtracker.EventTracker;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Path("/events/keys")
public class GetEventKeys extends Command {
  private final Gson gson;
  private final EventTracker eventTracker;

  @Inject
  public GetEventKeys(Gson gson, EventTracker eventTracker) {
    this.gson = gson;
    this.eventTracker = eventTracker;
  }

  public synchronized void execute(final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {
    List<String> keys = eventTracker.getEventKeys(request.getParameter("event_type"));
    response.getWriter().println(gson.toJson(keys));
  }
}
