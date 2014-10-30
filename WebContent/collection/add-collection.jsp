<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="cyanos" %>
<%@ page buffer="12kb" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" %>
<%@ page import="edu.uic.orjala.cyanos.Collection,
	edu.uic.orjala.cyanos.User,
	edu.uic.orjala.cyanos.Project,
	edu.uic.orjala.cyanos.web.servlet.CollectionServlet,
	edu.uic.orjala.cyanos.sql.SQLData,
	edu.uic.orjala.cyanos.sql.SQLCollection,
	edu.uic.orjala.cyanos.DataException,
	java.util.Arrays,
	java.util.Date,
	java.text.SimpleDateFormat" %>
<% 	String contextPath = request.getContextPath(); 
	User userObj = CollectionServlet.getUser(request); 
	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
%><!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<cyanos:header title="Add Collection"/>
</head>
<body>
<cyanos:menu helpModule="<%= CollectionServlet.HELP_MODULE %>"/>

<div class='content'>
<p align="CENTER"><font size="+2" >Add Collection</font></p>
<hr width="75%">
<% 
if (request.getParameter("addCollection") != null) {
	String colID = request.getParameter("colID");
	String projectID = request.getParameter("project");
	Collection collection = null;
	try {
	if (projectID != null && projectID.length() > 0) {
		collection = SQLCollection.createInProject((SQLData) request.getAttribute(CollectionServlet.DATASOURCE), colID, projectID);
	} else if (colID != null) {
		collection = SQLCollection.create((SQLData) request.getAttribute(CollectionServlet.DATASOURCE), colID);
	}

	if (collection != null && collection.first()) { 
		request.setAttribute(CollectionServlet.ATTR_COLLECTION, collection);
%><jsp:forward page="/collection.jsp">
<jsp:param value="" name="updateCollection"/>
</jsp:forward><% 
	} 
	} catch (DataException e ) {
		out.print("<p align='center'><b>Error: ");
		out.print(e.getMessage());
		out.print("</b></p>");
	}
}
%><form method="post" action="collection">
<input type="hidden" name="form" value="add">
<table class="species" style="width:80%; margin-left:auto; margin-right:auto">
<% String value = request.getParameter("colID"); %>
<tr><td>Collection ID:</td><td><input type="text" name="colID" value="<c:out value="<%= value %>"/>"></td></tr>
<% value = request.getParameter("colDate"); if ( value == null ) { value = format.format(new Date()); }  %>
<tr><td>Collection Date:</td><td><cyanos:calendar-field fieldName="colDate"/></td></tr>
<% value = request.getParameter("collector"); %>
<tr><td>Collected by:</td><td><input type="text" name="collector" value="<c:out value="<%= value %>"/>"></td></tr>
<% value = request.getParameter("loc_name"); %>
<tr><td>Location name:</td><td><input type="text" name="loc_name" value="<c:out value="<%= value %>"/>" size="40"></td></tr>
<% value = request.getParameter("lat"); %>
<tr><td>Coordinates:</td><td id="coords"><input id="pos_lat" type="text" name="lat" size="10" value="<c:out value="<%= value %>"/>"> 
<% value = request.getParameter("long"); %>
<input  id="pos_long" type="text" name="long" size="10" value="<c:out value="<%= value %>"/>">
<script>
function updatePosition (position) { 
	var field = document.getElementById("pos_lat");
	field.value = position.coords.latitude;
	field = document.getElementById("pos_long");
	field.value = position.coords.longitude;
	field = document.getElementById("pos_acc");
	field.value = position.coords.accuracy.toFixed(0) + " m";
}

if ( navigator.geolocation ) {
	var button = document.createElement("button");
	button.type = "button";
	button.innerHTML = "Use current location";
	button.onclick = function () { navigator.geolocation.getCurrentPosition(updatePosition);};
	var cell = document.getElementById("coords");
	cell.appendChild(button);
}
</script>
</td></tr>
<% value = request.getParameter("loc_prec"); %>
<tr><td>Precision (m):</td><td><input id="pos_acc" type="text" name="loc_prec" size="10" value="<c:out value="<%= value %>"/>"></td></tr>
<tr><td>Project:</td><td>
<jsp:include page="/includes/project-popup.jsp">
<jsp:param value="project" name="fieldName"/></jsp:include>
</td></tr>
<% value = request.getParameter("notes"); %>
<tr><td valign=top>Notes:</td><td><textarea rows="7" cols="70" name="notes"><c:out value="<%= value %>" default="" /></textarea></td></tr>
<tr><td colspan="2" align="CENTER"><button type="submit" name="addCollection">Create</button>
<input type="RESET"></td></tr>
</table>
</form>

</div>
</body>
</html>