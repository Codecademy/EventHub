package com.mobicrave.eventtracker.web.commands;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public abstract class Command {
  public abstract void execute(final HttpServletRequest request, final HttpServletResponse response) throws IOException;

  protected Map<String, String> toProperties(final HttpServletRequest request) {
    return Maps.asMap(request.getParameterMap().keySet(), new Function<String, String>() {
      @Override
      public String apply(String parameterName) {
        return request.getParameter(parameterName);
      }
    });
  }
}
