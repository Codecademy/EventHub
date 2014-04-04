package com.mobicrave.eventtracker.web.commands;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.mobicrave.eventtracker.Filter;
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
    List<Filter> eventFilters = getFilters(request.getParameterValues("efk"),
        request.getParameterValues("efv"));
    List<Filter> userFilters = getFilters(request.getParameterValues("ufk"),
        request.getParameterValues("ufv"));

    int[] funnelCounts = eventTracker.getFunnelCounts(
        request.getParameter("start_date"),
        request.getParameter("end_date"),
        request.getParameterValues("funnel_steps[]"),
        Integer.parseInt(request.getParameter("num_days_to_complete_funnel")),
        eventFilters,
        userFilters);
    response.getWriter().println(gson.toJson(funnelCounts));
  }

  private List<Filter> getFilters(String[] filterKeys, String[] filterValues) {
    List<Filter> eventFilters = Lists.newArrayList();
    if (filterKeys != null) {
      for (int i = 0; i < filterKeys.length; i++) {
        eventFilters.add(new Filter(filterKeys[i], filterValues[i]));
      }
    }
    return eventFilters;
  }
}
