package com.mobicrave.eventtracker.web.commands;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.mobicrave.eventtracker.EventTracker;
import com.mobicrave.eventtracker.base.DateHelper;
import com.mobicrave.eventtracker.model.Event;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

@Path("/events/batch_track")
public class BatchTrackEvent extends Command {
  private final Gson gson;
  private final DateHelper dateHelper;
  private final EventTracker eventTracker;

  @Inject
  public BatchTrackEvent(Gson gson, DateHelper dateHelper, EventTracker eventTracker) {
    this.gson = gson;
    this.dateHelper = dateHelper;
    this.eventTracker = eventTracker;
  }

  public synchronized void execute(final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {

    List<Map<String, String>> events = gson.fromJson(
        request.getParameter("events"), new TypeToken<List<Map<String, String>>>() {}.getType());
    PrintWriter writer = response.getWriter();
    for (Map<String, String> eventMap : events) {
      String date = eventMap.get("date");
      if (date == null) {
        date = dateHelper.getDate();
      }
      Event event = new Event.Builder(
          eventMap.get("event_type"),
          eventMap.get("external_user_id"),
          date,
          eventMap).build();
      writer.println(eventTracker.addEvent(event));
    }
  }
}
