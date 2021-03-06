<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="cyanos" tagdir="/WEB-INF/tags" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" %>
<%@ page import="edu.uic.orjala.cyanos.web.servlet.MainServlet,
	edu.uic.orjala.cyanos.web.listener.AppConfigListener,
	edu.uic.orjala.cyanos.CyanosObject,
	edu.uic.orjala.cyanos.BasicObject,
	edu.uic.orjala.cyanos.User,
	java.sql.PreparedStatement,
	java.math.BigDecimal,
	java.math.MathContext,
	java.sql.Connection,
	java.sql.ResultSet,
	java.sql.Statement,
	java.text.DateFormat" %>
<!DOCTYPE html>
<html>
<head>
<cyanos:header title="Cyanos Queries"/>
<style type="text/css">
h2 { text-align:center; }
</style>
</head>
<body>
<cyanos:menu helpModule="<%= MainServlet.HELP_MODULE %>"/>
<div id="content">
<h1>Database Reports</h1>
<ul>
<li><a href="inoclist.jsp">Large scale inoculations awaiting harvest</a></li>
<li><a href="harvestlist.jsp">Harvests since</a></li>
<li><a href="extractList.jsp">Extracts made since</a></li>
<li><a href="strainStatus.jsp">Strain status including harvest, extract and separation</a></li>
<li><a href="assayList.jsp">Assay summary by strain</a></li>
<li><a href="activeList.jsp">Assay summary by target</a></li>
</ul>
</div>
</body>
</html>