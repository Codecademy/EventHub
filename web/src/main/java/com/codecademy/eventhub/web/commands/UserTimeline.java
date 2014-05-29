package com.codecademy.eventhub.web.commands;

import com.google.gson.Gson;
import com.codecademy.eventhub.EventHub;
import com.codecademy.eventhub.model.Event;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Path("/users/timeline")
public class UserTimeline extends Command {
  private final Gson gson;
  private final EventHub eventHub;

  @Inject
  public UserTimeline(Gson gson, EventHub eventHub) {
    this.gson = gson;
    this.eventHub = eventHub;
  }

  @Override
  public synchronized void execute(final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {
    List<Event> userEvents = eventHub.getUserEvents(
        request.getParameter("external_user_id"),
        Integer.parseInt(request.getParameter("offset")),
        Integer.parseInt(request.getParameter("num_records")));
    response.getWriter().println(gson.toJson(userEvents));
  }
}
