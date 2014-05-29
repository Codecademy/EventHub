package com.codecademy.eventhub.web.commands;

import com.codecademy.eventhub.EventHub;
import com.google.gson.Gson;
import com.codecademy.eventhub.storage.filter.Filter;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Path("/events/cohort")
public class EventCohort extends Command {
  private final Gson gson;
  private final EventHub eventHub;

  @Inject
  public EventCohort(Gson gson, EventHub eventHub) {
    this.gson = gson;
    this.eventHub = eventHub;
  }

  @Override
  public synchronized void execute(final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {
    Filter rowEventFilter = getFilter(request.getParameterValues("refk[]"),
        request.getParameterValues("refv[]"));
    Filter columnEventFilter = getFilter(request.getParameterValues("cefk[]"),
        request.getParameterValues("cefv[]"));

    int[][] retentionTable = eventHub.getRetentionTable(
        request.getParameter("start_date"),
        request.getParameter("end_date"),
        Integer.parseInt(request.getParameter("num_days_per_row")),
        Integer.parseInt(request.getParameter("num_columns")),
        request.getParameter("row_event_type"),
        request.getParameter("column_event_type"),
        rowEventFilter,
        columnEventFilter);
    response.getWriter().println(gson.toJson(retentionTable));
  }
}
