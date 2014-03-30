package com.mobicrave.eventtracker.web.commands;

import com.google.gson.Gson;
import com.mobicrave.eventtracker.EventTracker;
import com.mobicrave.eventtracker.model.Event;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Path("/events/view")
public class ViewEvent extends Command {
  private final Gson gson;
  private final EventTracker eventTracker;

  @Inject
  public ViewEvent(Gson gson, EventTracker eventTracker) {
    this.gson = gson;
    this.eventTracker = eventTracker;
  }

  public synchronized void execute(final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {
    Event event = eventTracker.getEvent(Long.parseLong(request.getParameter("event_id")));
    response.getWriter().println(gson.toJson(event));
  }
}
