package com.codecademy.eventhub.web.commands;

import com.codecademy.eventhub.EventHub;
import com.google.gson.Gson;
import com.codecademy.eventhub.model.Event;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Path("/events/view")
public class ViewEvent extends Command {
  private final Gson gson;
  private final EventHub eventHub;

  @Inject
  public ViewEvent(Gson gson, EventHub eventHub) {
    this.gson = gson;
    this.eventHub = eventHub;
  }

  @Override
  public synchronized void execute(final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {
    Event event = eventHub.getEvent(Long.parseLong(request.getParameter("event_id")));
    response.getWriter().println(gson.toJson(event));
  }
}
