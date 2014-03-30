package com.mobicrave.eventtracker.web.commands;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.mobicrave.eventtracker.Criterion;
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
    List<Criterion> eventCriteria = getCriteria(request.getParameterValues("eck"),
        request.getParameterValues("ecv"));
    List<Criterion> userCriteria = getCriteria(request.getParameterValues("uck"),
        request.getParameterValues("ucv"));

    int[] funnelCounts = eventTracker.getFunnelCounts(
        request.getParameter("start_date"),
        request.getParameter("end_date"),
        request.getParameterValues("funnel_steps[]"),
        Integer.parseInt(request.getParameter("num_days_to_complete_funnel")),
        eventCriteria,
        userCriteria);
    response.getWriter().println(gson.toJson(funnelCounts));
  }

  private List<Criterion> getCriteria(String[] criterionKeys, String[] criterionValues) {
    List<Criterion> eventCriteria = Lists.newArrayList();
    if (criterionKeys != null) {
      for (int i = 0; i < criterionKeys.length; i++) {
        eventCriteria.add(new Criterion(criterionKeys[i], criterionValues[i]));
      }
    }
    return eventCriteria;
  }
}
