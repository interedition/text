<#ftl>

<#macro page title header="">
<!DOCTYPE html>
<html lang="en">
<head>
    <meta http-equiv="Content-Type" content="text/html;charset=UTF-8">
    <title>${title} :: Interedition Text Repository</title>

    <link rel="shortcut icon" href="${cp}/static/interedition_logo.ico" type="image/x-icon">
    <link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/combo?3.7.2/build/cssfonts/cssfonts-min.css&3.7.2/build/cssgrids/cssgrids-min.css&3.7.2/build/cssreset/cssreset-min.css&3.7.2/build/cssbase/cssbase-min.css&3.7.2/build/cssbutton/cssbutton-min.css">
    <link rel="stylesheet" type="text/css" href="${cp}/static/main.css">

    <script type="text/javascript">var cp = "${cp?js_string}";</script>
    <script type="text/javascript" src="http://yui.yahooapis.com/combo?3.7.2/build/yui/yui-min.js"></script>
${header}
</head>
<body class="yui3-skin-sam">
    <#nested>
<div id="footer">
    Copyright &copy; 2011, 2012 The Interedition Development Group. All rights reserved. See the <a href="http://www.interedition.eu/" title="Interedition Homepage">Interedition Homepage</a> for further information.
</div>
</body>
</html>
</#macro>