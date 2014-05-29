package com.codecademy.eventhub.web.commands;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.codecademy.eventhub.EventHub;
import com.codecademy.eventhub.base.DateHelper;
import com.codecademy.eventhub.model.Event;

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
  private final EventHub eventHub;

  @Inject
  public BatchTrackEvent(Gson gson, DateHelper dateHelper, EventHub eventHub) {
    this.gson = gson;
    this.dateHelper = dateHelper;
    this.eventHub = eventHub;
  }

  @Override
  public synchronized void execute(final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {

    List<Map<String, String>> events = gson.fromJson(
        request.getParameter("events"), new TypeToken<List<Map<String, String>>>() {}.getType());
    List<Long> eventIds = Lists.newArrayList();
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
      eventIds.add(eventHub.addEvent(event));
    }
    writer.println(gson.toJson(eventIds));
  }
}
