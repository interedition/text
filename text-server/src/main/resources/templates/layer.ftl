<@ie.page "Layer">

<div id="editor"></div>

<script type="text/javascript">
    YUI().use("node", "event", "editor", function(Y) {
        Y.on("domready", function() {
            var editor = new Y.EditorBase({
                content: "Hello World",
                extracss: 'body { font-family: georgia; font-size: 131%; }'
            });

            //editor.plug(Y.Plugin.EditorBR);

            //Focusing the Editor when the frame is ready..
            editor.on('frame:ready', function() {
                this.focus();
            });

            //Rendering the Editor.
            editor.render('#editor');
        });

    });
</script>

<pre>${text?html}</pre>
</@ie.page>