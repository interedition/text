<@ie.page "Home">

<#list texts as t>
    <p><a href="${cp}/text/${t?c?url}" title="Text">${t?html}</a></p>
</#list>

</@ie.page>