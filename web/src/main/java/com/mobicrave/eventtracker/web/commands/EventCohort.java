package com.mobicrave.eventtracker.web.commands;

import com.google.gson.Gson;
import com.mobicrave.eventtracker.EventTracker;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Path("/events/cohort")
public class EventCohort extends Command {
  private final Gson gson;
  private final EventTracker eventTracker;

  @Inject
  public EventCohort(Gson gson, EventTracker eventTracker) {
    this.gson = gson;
    this.eventTracker = eventTracker;
  }

  public synchronized void execute(final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {
    int[][] retentionTable = eventTracker.getRetentionTable(
        request.getParameter("start_date"),
        request.getParameter("end_date"),
        Integer.parseInt(request.getParameter("num_days_per_row")),
        Integer.parseInt(request.getParameter("num_columns")),
        request.getParameter("row_event_type"),
        request.getParameter("column_event_type")
    );
    response.getWriter().println(gson.toJson(retentionTable));
  }
}
