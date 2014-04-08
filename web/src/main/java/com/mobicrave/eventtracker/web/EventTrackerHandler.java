package com.mobicrave.eventtracker.web;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
import com.mobicrave.eventtracker.EventTracker;
import com.mobicrave.eventtracker.EventTrackerModule;
import com.mobicrave.eventtracker.index.DatedEventIndexModule;
import com.mobicrave.eventtracker.index.PropertiesIndexModule;
import com.mobicrave.eventtracker.index.ShardedEventIndexModule;
import com.mobicrave.eventtracker.index.UserEventIndexModule;
import com.mobicrave.eventtracker.list.DmaIdListModule;
import com.mobicrave.eventtracker.storage.EventStorageModule;
import com.mobicrave.eventtracker.storage.UserStorageModule;
import com.mobicrave.eventtracker.web.commands.Command;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;

import javax.inject.Provider;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Properties;

public class EventTrackerHandler extends AbstractHandler implements Closeable {
  private final EventTracker eventTracker;
  private final Map<String, Provider<Command>> commandsMap;
  private boolean isLogging;

  public EventTrackerHandler(EventTracker eventTracker, Map<String, Provider<Command>> commandsMaps) {
    this.eventTracker = eventTracker;
    this.commandsMap = commandsMaps;
    isLogging = false;
  }

  public void handle(String target, Request baseRequest, HttpServletRequest request,
      HttpServletResponse response) throws IOException, ServletException {
    if (isLogging) {
      System.out.println(request);
    }
    response.setStatus(HttpServletResponse.SC_OK);
    switch (target) {
      case "/debug":
        isLogging = !isLogging;
        break;
      case "/varz":
        response.getWriter().println(eventTracker.getVarz());
        break;
      default:
        commandsMap.get(target).get().execute(request, response);
        break;
    }

    baseRequest.setHandled(true);
  }

  @Override
  public void close() throws IOException {
    eventTracker.close();
  }

  public static void main(String[] args) throws Exception {
    Properties properties = new Properties();
    properties.load(
        EventTracker.class.getClassLoader().getResourceAsStream("tracker.properties"));
    properties.load(
        EventTrackerHandler.class.getClassLoader().getResourceAsStream("web.properties"));
    properties.putAll(System.getProperties());

    Injector injector = Guice.createInjector(Modules.override(
        new DmaIdListModule(),
        new DatedEventIndexModule(),
        new ShardedEventIndexModule(),
        new PropertiesIndexModule(),
        new UserEventIndexModule(),
        new EventStorageModule(),
        new UserStorageModule(),
        new EventTrackerModule(properties)).with(new Module()));
    final EventTrackerHandler eventTrackerHandler = injector.getInstance(EventTrackerHandler.class);
    int port = injector.getInstance(Key.get(Integer.class, Names.named("eventtrackerhandler.port")));

    final Server server = new Server(port);
    @SuppressWarnings("ConstantConditions")
    String webDir = EventTrackerHandler.class.getClassLoader().getResource("frontend").toExternalForm();

    ResourceHandler resourceHandler = new ResourceHandler();
    resourceHandler.setDirectoriesListed(false);
    resourceHandler.setWelcomeFiles(new String[]{"main.html"});
    resourceHandler.setResourceBase(webDir);
    HandlerList handlers = new HandlerList();
    handlers.setHandlers(new Handler[] { new JsonpCallbackHandler(eventTrackerHandler), resourceHandler });

    server.setHandler(handlers);

    server.start();
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        if (server.isStarted()) {
          try {
            server.stop();
            eventTrackerHandler.close();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    },"Stop Jetty Hook"));

    server.join();
  }

  public static class FilterServletOutputStream extends ServletOutputStream {
    private DataOutputStream stream;

    public FilterServletOutputStream(OutputStream output) {
      stream = new DataOutputStream(output);
    }

    public void write(int b) throws IOException {
      stream.write(b);
    }

    public void write(byte[] b) throws IOException {
      stream.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
      stream.write(b, off, len);
    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {}
  }

  public static class GenericResponseWrapper extends HttpServletResponseWrapper {
    private ByteArrayOutputStream output;

    public GenericResponseWrapper(HttpServletResponse response) {
      super(response);
      output = new ByteArrayOutputStream();
    }

    public byte[] getData() {
      return output.toByteArray();
    }

    public ServletOutputStream getOutputStream() {
      return new FilterServletOutputStream(output);
    }

    public PrintWriter getWriter() {
      return new PrintWriter(getOutputStream(), true);
    }
  }

  // modified from http://jpgmr.wordpress.com/2010/07/28/tutorial-implementing-a-servlet-filter-for-jsonp-callback-with-springs-delegatingfilterproxy/#1
  public static class JsonpCallbackHandler extends AbstractHandler {
    private final Handler handler;

    public JsonpCallbackHandler(Handler handler) {
      this.handler = handler;
    }

    public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
      Map<String, String[]> params = httpServletRequest.getParameterMap();

      if(params.containsKey("callback")) {
        OutputStream out = httpServletResponse.getOutputStream();
        GenericResponseWrapper wrapper = new GenericResponseWrapper(httpServletResponse);

        try {
          handler.handle(s, request, httpServletRequest, wrapper);
          if (httpServletResponse.getStatus() >= 400) {
            out.write((params.get("callback")[0] + "({error: 'error'});").getBytes());
          } else {
            out.write((params.get("callback")[0] + "(").getBytes());
            out.write(wrapper.getData());
            out.write(");".getBytes());
          }

          wrapper.setContentType("text/javascript;charset=UTF-8");
          out.close();
        } catch (Exception e) {
          out.write((params.get("callback")[0] + "({error: 'error'});").getBytes());
          wrapper.setContentType("text/javascript;charset=UTF-8");
          out.close();
          throw e;
        }
      } else {
        handler.handle(s, request, httpServletRequest, httpServletResponse);
      }
    }
  }
}
