package com.codecademy.eventhub.web.commands;

import com.google.gson.Gson;
import com.codecademy.eventhub.EventHub;
import com.codecademy.eventhub.model.User;
import com.codecademy.eventhub.storage.filter.Filter;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Path("/users/find")
public class FindUser extends Command {
  private final Gson gson;
  private final EventHub eventHub;

  @Inject
  public FindUser(Gson gson, EventHub eventHub) {
    this.gson = gson;
    this.eventHub = eventHub;
  }

  @Override
  public synchronized void execute(final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {
    Filter filter = getFilter(
        request.getParameterValues("ufk[]"),
        request.getParameterValues("ufv[]"));
    List<User> users = eventHub.findUsers(filter);
    response.getWriter().println(gson.toJson(users));
  }
}
