package com.codecademy.eventhub.web;

import com.codecademy.eventhub.EventHub;
import com.codecademy.eventhub.EventHubModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
import com.codecademy.eventhub.index.DatedEventIndexModule;
import com.codecademy.eventhub.index.PropertiesIndexModule;
import com.codecademy.eventhub.index.ShardedEventIndexModule;
import com.codecademy.eventhub.index.UserEventIndexModule;
import com.codecademy.eventhub.list.DmaIdListModule;
import com.codecademy.eventhub.storage.EventStorageModule;
import com.codecademy.eventhub.storage.UserStorageModule;
import com.codecademy.eventhub.web.commands.Command;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;

import javax.inject.Provider;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

public class EventHubHandler extends AbstractHandler implements Closeable {
  private final EventHub eventHub;
  private final Map<String, Provider<Command>> commandsMap;
  private boolean isLogging;

  public EventHubHandler(EventHub eventHub, Map<String, Provider<Command>> commandsMaps) {
    this.eventHub = eventHub;
    this.commandsMap = commandsMaps;
    isLogging = false;
  }

  @Override
  public void handle(String target, Request baseRequest, HttpServletRequest request,
      HttpServletResponse response) throws IOException, ServletException {
    if (isLogging) {
      System.out.println(request);
    }
    response.setStatus(HttpServletResponse.SC_OK);
    switch (target) {
      case "/debug":
        isLogging = !isLogging;
        baseRequest.setHandled(true);
        break;
      case "/varz":
        response.getWriter().println(eventHub.getVarz());
        baseRequest.setHandled(true);
        break;
      default:
        Provider<Command> commandProvider = commandsMap.get(target);
        if (commandProvider != null) {
          commandProvider.get().execute(request, response);
          baseRequest.setHandled(true);
        }
        break;
    }
  }

  @Override
  public void close() throws IOException {
    eventHub.close();
  }

  public static void main(String[] args) throws Exception {
    Properties properties = new Properties();
    properties.load(
        EventHub.class.getClassLoader().getResourceAsStream("hub.properties"));
    properties.load(
        EventHubHandler.class.getClassLoader().getResourceAsStream("web.properties"));
    properties.putAll(System.getProperties());

    Injector injector = Guice.createInjector(Modules.override(
        new DmaIdListModule(),
        new DatedEventIndexModule(),
        new ShardedEventIndexModule(),
        new PropertiesIndexModule(),
        new UserEventIndexModule(),
        new EventStorageModule(),
        new UserStorageModule(),
        new EventHubModule(properties)).with(new Module()));
    final EventHubHandler eventHubHandler = injector.getInstance(EventHubHandler.class);
    int port = injector.getInstance(Key.get(Integer.class, Names.named("eventhubhandler.port")));

    final Server server = new Server(port);
    @SuppressWarnings("ConstantConditions")
    String webDir = EventHubHandler.class.getClassLoader().getResource("frontend").toExternalForm();
    HashLoginService loginService = new HashLoginService();
    loginService.putUser(properties.getProperty("eventhubhandler.username"),
        new Password(properties.getProperty("eventhubhandler.password")), new String[]{"user"});

    server.addBean(loginService);

    ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
    Constraint constraint = new Constraint();
    constraint.setName("auth");
    constraint.setAuthenticate( true );
    constraint.setRoles(new String[] { "user", "admin" });
    ConstraintMapping mapping = new ConstraintMapping();
    mapping.setPathSpec( "/*" );
    mapping.setConstraint( constraint );
    securityHandler.setConstraintMappings(Collections.singletonList(mapping));
    securityHandler.setAuthenticator(new BasicAuthenticator());
    securityHandler.setLoginService(loginService);

    ResourceHandler resourceHandler = new ResourceHandler();
    resourceHandler.setDirectoriesListed(false);
    resourceHandler.setWelcomeFiles(new String[]{"main.html"});
    resourceHandler.setResourceBase(webDir);
    HandlerList handlers = new HandlerList();
    handlers.setHandlers(new Handler[]{new JsonpCallbackHandler(eventHubHandler), securityHandler});

    server.setHandler(handlers);
    securityHandler.setHandler(resourceHandler);

    server.start();
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        if (server.isStarted()) {
          try {
            server.stop();
            eventHubHandler.close();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    },"Stop Jetty Hook"));

    server.join();
  }
}
