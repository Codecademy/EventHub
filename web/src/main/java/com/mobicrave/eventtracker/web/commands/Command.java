package com.mobicrave.eventtracker.web.commands;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mobicrave.eventtracker.storage.filter.And;
import com.mobicrave.eventtracker.storage.filter.ExactMatch;
import com.mobicrave.eventtracker.storage.filter.Filter;
import com.mobicrave.eventtracker.storage.filter.TrueFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
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

  protected String[] merge(String[] x, String[] y) {
    if (x == null & y == null) {
      return null;
    } else if (x == null) {
      return y.clone();
    } else if (y == null) {
      return x.clone();
    }

    String[] ret = new String[x.length + y.length];
    System.arraycopy(x, 0, ret, 0, x.length);
    System.arraycopy(y, 0, ret, x.length, y.length);
    return ret;
  }

  protected Filter getFilter(String[] filterKeys, String[] filterValues) {
    if (filterKeys == null || filterValues == null) {
      return TrueFilter.INSTANCE;
    }

    List<Filter> eventFilters = Lists.newArrayList();
    for (int i = 0; i < filterKeys.length; i++) {
      eventFilters.add(new ExactMatch(filterKeys[i], filterValues[i]));
    }
    return new And(eventFilters);
  }
}
