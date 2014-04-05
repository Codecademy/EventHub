package com.mobicrave.eventtracker.web.commands;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.mobicrave.eventtracker.storage.filter.Filter;
import com.mobicrave.eventtracker.EventTracker;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Path("/events/funnel")
public class EventFunnel extends Command {
  private final Gson gson;
  private final EventTracker eventTracker;

  @Inject
  public EventFunnel(Gson gson, EventTracker eventTracker) {
    this.gson = gson;
    this.eventTracker = eventTracker;
  }

  public synchronized void execute(final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {
    String[] funnelSteps = request.getParameterValues("funnel_steps[]");
    List<Filter> eventFilters = Lists.newArrayList();
    for (int i = 0; i < funnelSteps.length; i++) {
      Filter filter = getFilter(
          merge(request.getParameterValues("efk"), request.getParameterValues("efk" + i)),
          merge(request.getParameterValues("efk"), request.getParameterValues("efv" + i)));
      eventFilters.add(filter);
    }
    Filter userFilter = getFilter(request.getParameterValues("ufk"),
        request.getParameterValues("ufv"));

    int[] funnelCounts = eventTracker.getFunnelCounts(
        request.getParameter("start_date"),
        request.getParameter("end_date"),
        funnelSteps,
        Integer.parseInt(request.getParameter("num_days_to_complete_funnel")),
        eventFilters,
        userFilter);
    response.getWriter().println(gson.toJson(funnelCounts));
  }

}
