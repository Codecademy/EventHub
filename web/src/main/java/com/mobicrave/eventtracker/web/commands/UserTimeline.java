package com.mobicrave.eventtracker.web.commands;

import com.google.gson.Gson;
import com.mobicrave.eventtracker.EventTracker;
import com.mobicrave.eventtracker.model.Event;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Path("/users/timeline")
public class UserTimeline extends Command {
  private final Gson gson;
  private final EventTracker eventTracker;

  @Inject
  public UserTimeline(Gson gson, EventTracker eventTracker) {
    this.gson = gson;
    this.eventTracker = eventTracker;
  }

  public synchronized void execute(final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {
    List<Event> userEvents = eventTracker.getUserEvents(
        request.getParameter("external_user_id"),
        Integer.parseInt(request.getParameter("offset")),
        Integer.parseInt(request.getParameter("num_records")));
    response.getWriter().println(gson.toJson(userEvents));
  }
}
