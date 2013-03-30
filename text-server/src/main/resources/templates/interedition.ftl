<#ftl>

<#macro page title header="">
<!DOCTYPE html>
<html lang="en">
<head>
    <meta http-equiv="Content-Type" content="text/html;charset=UTF-8">
    <title>${title} :: Interedition Text Server</title>

    <link rel="shortcut icon" href="${ap}/interedition_logo.ico" type="image/x-icon">
    <link rel="stylesheet" type="text/css" href="${yp}/cssfonts/cssfonts-min.css">
    <link rel="stylesheet" type="text/css" href="${yp}/cssgrids/cssgrids-min.css">
    <link rel="stylesheet" type="text/css" href="${yp}/cssreset/cssreset-min.css">
    <link rel="stylesheet" type="text/css" href="${yp}/cssbase/cssbase-min.css">
    <link rel="stylesheet" type="text/css" href="${yp}/cssbutton/cssbutton-min.css">

    <link rel="stylesheet" type="text/css" href="${ap}/main.css">

    <script type="text/javascript">var cp = "${cp?js_string}";</script>
    <script type="text/javascript" src="${yp}/yui/yui-min.js"></script>
${header}
</head>
<body class="yui3-skin-sam">
    <ol id="main-menu">
        <li><a href="${cp}/" title="Home">Index</a></li>
        <li><a href="${cp}/xml-extract" title="Home">Upload</a></li>
    </ol>
    <#nested>
<div id="footer">
    Copyright &copy; 2012-2013 The Interedition Development Group. All rights reserved. See the <a href="http://www.interedition.eu/" title="Interedition Homepage">Interedition Homepage</a> for further information.
</div>
</body>
</html>
</#macro>