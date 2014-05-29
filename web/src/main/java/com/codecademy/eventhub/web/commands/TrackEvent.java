package com.codecademy.eventhub.web.commands;

import com.codecademy.eventhub.EventHub;
import com.codecademy.eventhub.base.DateHelper;
import com.codecademy.eventhub.model.Event;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Path("/events/track")
public class TrackEvent extends Command {
  private final DateHelper dateHelper;
  private final EventHub eventHub;

  @Inject
  public TrackEvent(DateHelper dateHelper, EventHub eventHub) {
    this.dateHelper = dateHelper;
    this.eventHub = eventHub;
  }

  @Override
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
    response.getWriter().println(eventHub.addEvent(event));
  }
}
