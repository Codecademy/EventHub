package com.mobicrave.eventtracker.web.commands;

import com.mobicrave.eventtracker.EventTracker;
import com.mobicrave.eventtracker.base.DateHelper;
import com.mobicrave.eventtracker.model.Event;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Path("/events/track")
public class TrackEvent extends Command {
  private final DateHelper dateHelper;
  private final EventTracker eventTracker;

  @Inject
  public TrackEvent(DateHelper dateHelper, EventTracker eventTracker) {
    this.dateHelper = dateHelper;
    this.eventTracker = eventTracker;
  }

  public synchronized void execute(final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {
    String date = request.getParameter("date");
    if (date == null) {
      date = dateHelper.getDate();
    }
    Event event = new Event.Builder(
        request.getParameter("event_type"),
        request.getParameter("external_user_id"),
        date,
        toProperties(request)).build();
    response.getWriter().println(eventTracker.addEvent(event));
  }
}
