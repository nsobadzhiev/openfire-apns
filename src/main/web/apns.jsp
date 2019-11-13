<%@ page import="java.io.File,
                 java.util.List,
                 org.jivesoftware.openfire.XMPPServer,
                 org.jivesoftware.util.*,
                 com.feinfone.apns.ApnsPlugin,
                 org.apache.commons.fileupload.FileItem,
                 org.apache.commons.fileupload.disk.DiskFileItemFactory,
                 org.apache.commons.fileupload.servlet.ServletFileUpload,
                 org.apache.commons.fileupload.FileUploadException"
    errorPage="error.jsp"
%>

<%  // Get parameters
    boolean save = request.getParameter("save") != null;
    boolean success = request.getParameter("success") != null;
    boolean error = request.getParameter("error") != null;
    String teamId = ParamUtils.getParameter(request, "teamId");
    String keyId = ParamUtils.getParameter(request, "keyId");
    String topic = ParamUtils.getParameter(request, "topic");
    String badge = ParamUtils.getParameter(request, "badge");
    String sound = ParamUtils.getParameter(request, "sound");
    String production = ParamUtils.getParameter(request, "production");

    ApnsPlugin plugin = (ApnsPlugin) XMPPServer.getInstance().getPluginManager().getPlugin("openfire-apns");

    // Handle a save
    if (save) {
        plugin.setKeyId(teamId);
        plugin.setTeamId(teamId);
        plugin.setTopic(topic);
        plugin.setBadge(badge);
        plugin.setSound(sound);
        plugin.setProduction(production);

        try {
            List<FileItem> multiParts = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);

            for (FileItem item : multiParts) {
                if (!item.isFormField()) {
                    String filename = item.getName();
                    item.write(new File(ApnsPlugin.keystorePath()));
                }
            }
            response.sendRedirect("apns.jsp?success=true");
            return;
        } catch (Exception e) {
            response.sendRedirect("apns.jsp?error=true");
            return;
        }

    }

    teamId = plugin.getTeamId();
    keyId = plugin.getKeyId();
    topic = plugin.getTopic();
    badge = Integer.toString(plugin.getBadge());
    sound = plugin.getSound();
    production = plugin.getProduction() ? "true" : "false";
%>

<html>
    <head>
        <title>APNS Settings Properties</title>
        <meta name="pageID" content="apns-settings"/>
    </head>
    <body>

<%  if (success) { %>
    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0"></td>
        <td class="jive-icon-label">
            APNS certificate updated successfully.
        </td></tr>
    </tbody>
    </table>
    </div><br>
<% } %>

<form action="apns.jsp?save" method="post" enctype="multipart/form-data">

<div class="jive-contentBoxHeader">APNS certificate</div>
<div class="jive-contentBox">
    <label for="file">p8 key:</label>
    <input type="file" name="file" />
    <br>

    <label for="teamId">Team ID:</label>
    <input type="text" name="teamId" value="<%= teamId %>" />
    <br>

    <label for="keyId">Key ID:</label>
    <input type="text" name="keyId" value="<%= keyId %>" />
    <br>

    <label for="topic">Topic:</label>
    <input type="text" name="topic" value="<%= topic %>" />
    <br>

    <label for="badge">payload badge</label>
    <input type="badge" name="badge" value="<%= badge %>" />
    <br>

    <label for="sound">payload sound</label>
    <input type="badge" name="sound" value="<%= sound %>" />
    <br>

    <label for="production">sandbox or production</label>
    <input type="radio" name="production" value="false" <%= production.equals("true") ? "" : "checked" %>>Sandbox
    <input type="radio" name="production" value="true" <%= production.equals("true") ? "checked" : "" %>>Production
</div>
<input type="submit" value="Save">
</form>


</body>
</html>
