package com.mobicrave.eventtracker.web.commands;

import com.google.gson.Gson;
import com.mobicrave.eventtracker.EventTracker;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Path("/users/values")
public class GetUserValues extends Command {
  private final Gson gson;
  private final EventTracker eventTracker;

  @Inject
  public GetUserValues(Gson gson, EventTracker eventTracker) {
    this.gson = gson;
    this.eventTracker = eventTracker;
  }

  public synchronized void execute(final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {
    String prefix = request.getParameter("prefix");
    prefix = (prefix == null ? "" : prefix);
    List<String> values = eventTracker.getUserValues(
        request.getParameter("user_key"),
        prefix);
    response.getWriter().println(gson.toJson(values));
  }
}
