<%@ page isErrorPage="true" import="java.io.*,javax.servlet.*" %>
<%
    request.getSession().setAttribute("javax.servlet.error.exception", exception);
    response.setContentType("text/xml;charset=UTF-8");
    response.setStatus(200);
    response.sendRedirect(request.getContextPath() + "/Error.xhtml");
%>