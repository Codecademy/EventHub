package com.codecademy.eventhub.web;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

// modified from http://jpgmr.wordpress.com/2010/07/28/tutorial-implementing-a-servlet-filter-for-jsonp-callback-with-springs-delegatingfilterproxy/#1
public class JsonpCallbackHandler extends AbstractHandler {
  private final Handler handler;

  public JsonpCallbackHandler(Handler handler) {
    this.handler = handler;
  }

  @Override
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

  private static class FilterServletOutputStream extends ServletOutputStream {
    private DataOutputStream stream;

    public FilterServletOutputStream(OutputStream output) {
      stream = new DataOutputStream(output);
    }

    @Override
    public void write(int b) throws IOException {
      stream.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
      stream.write(b);
    }

    @Override
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

  private static class GenericResponseWrapper extends HttpServletResponseWrapper {
    private ByteArrayOutputStream output;

    public GenericResponseWrapper(HttpServletResponse response) {
      super(response);
      output = new ByteArrayOutputStream();
    }

    public byte[] getData() {
      return output.toByteArray();
    }

    @Override
    public ServletOutputStream getOutputStream() {
      return new FilterServletOutputStream(output);
    }

    @Override
    public PrintWriter getWriter() {
      return new PrintWriter(getOutputStream(), true);
    }
  }
}
