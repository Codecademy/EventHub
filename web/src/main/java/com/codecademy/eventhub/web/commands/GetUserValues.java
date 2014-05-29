package com.codecademy.eventhub.web.commands;

import com.codecademy.eventhub.EventHub;
import com.google.gson.Gson;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Path("/users/values")
public class GetUserValues extends Command {
  private final Gson gson;
  private final EventHub eventHub;

  @Inject
  public GetUserValues(Gson gson, EventHub eventHub) {
    this.gson = gson;
    this.eventHub = eventHub;
  }

  @Override
  public synchronized void execute(final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {
    String prefix = request.getParameter("prefix");
    prefix = (prefix == null ? "" : prefix);
    List<String> values = eventHub.getUserValues(
        request.getParameter("user_key"),
        prefix);
    response.getWriter().println(gson.toJson(values));
  }
}
