package com.codecademy.eventhub.web.commands;

import com.codecademy.eventhub.EventHub;
import com.google.gson.Gson;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Path("/events/values")
public class GetEventValues extends Command {
  private final Gson gson;
  private final EventHub eventHub;

  @Inject
  public GetEventValues(Gson gson, EventHub eventHub) {
    this.gson = gson;
    this.eventHub = eventHub;
  }

  @Override
  public synchronized void execute(final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {
    String prefix = request.getParameter("prefix");
    prefix = (prefix == null ? "" : prefix);
    List<String> values = eventHub.getEventValues(
        request.getParameter("event_type"),
        request.getParameter("event_key"),
        prefix);
    response.getWriter().println(gson.toJson(values));
  }
}
