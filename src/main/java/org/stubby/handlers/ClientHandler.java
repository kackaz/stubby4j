package org.stubby.handlers;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.stubby.database.Repository;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Alexander Zagniotov
 * @since 6/17/12, 11:25 PM
 */
public final class ClientHandler extends AbstractHandler {

   private final static long serialVersionUID = 159L;
   private final Repository repository;

   public ClientHandler(final Repository repository) {
      this.repository = repository;
   }

   @Override
   public void handle(final String target,
                      final Request baseRequest,
                      final HttpServletRequest request,
                      final HttpServletResponse response) throws IOException, ServletException {

      baseRequest.setHandled(true);
      response.setHeader("Server", HandlerUtils.constructHeaderServerName());
      response.setHeader("Date", new Date().toString());
      response.setCharacterEncoding("UTF-8");

      String postBody = null;
      if (request.getMethod().toLowerCase().equals("post")) {

         postBody = HandlerUtils.inputStreamToString(request.getInputStream());
         if (postBody == null || postBody.isEmpty()) {
            response.setContentType("text/plain;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Oh oh :( \n\nBad request, POST body is missing");
            return;
         }
      }

      final Map<String, String> responseBody = repository.retrieveResponseFor(constructFullURI(request), request.getMethod(), postBody);
      if (responseBody.size() == 1) {
         response.setStatus(HttpServletResponse.SC_NOT_FOUND);
         response.getWriter().println(responseBody.get(Repository.NOCONTENT_MSG_KEY));
         return;
      }

      setResponseHeaders(responseBody, response);
      response.setStatus(Integer.parseInt(responseBody.get(Repository.TBL_COLUMN_STATUS)));
      response.getWriter().println(responseBody.get(Repository.TBL_COLUMN_BODY));
   }

   private String constructFullURI(final HttpServletRequest request) {
      final String pathInfo = request.getPathInfo();
      final String queryStr = request.getQueryString();
      final String queryString = (queryStr == null || queryStr.equals("")) ? "" : String.format("?%s", request.getQueryString());
      return String.format("%s%s", pathInfo, queryString);
   }

   private void setResponseHeaders(final Map<String, String> responseBody, final HttpServletResponse response) {

      final List<String> nonHeaderProperties = Arrays.asList(
            Repository.TBL_COLUMN_BODY,
            Repository.NOCONTENT_MSG_KEY,
            Repository.TBL_COLUMN_STATUS);

      for (Map.Entry<String, String> entry : responseBody.entrySet()) {
         if (nonHeaderProperties.contains(entry.getKey())) {
            continue;
         }
         response.setHeader(entry.getKey(), entry.getValue());
      }
   }
}