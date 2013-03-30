<@ie.page "Text #" + id?html>

<div class="yui3-g">
    <div class="yui3-u-3-4"><div class="content">
        <div id="text"></div>
    </div></div>
    <div class="yui3-u-1-4"><div class="content">
        <div id="annotations"></div>
    </div></div>
</div>


<script type="text/javascript" src="${ap}/rangy-1.3alpha.772/rangy-core.js"></script>
<script type="text/javascript" src="${ap}/rangy-1.3alpha.772/rangy-textrange.js"></script>
<script type="text/javascript" src="${ap}/segment-index.js"></script>
<script type="text/javascript" src="${ap}/text.js"></script>
<script type="text/javascript">
    YUI().use("node", "event", "interedition-text", "json-stringify", function(Y) {
        Y.on("domready", function() {
            text = new Y.interedition.AnnotatedText(${id?c}, "${text?js_string}", ${annotations}, ${segment}, ${length?c});

            var milestones = text.milestones(), lineBreaks = text.lineBreaks();

            var container = Y.one("#text"), annotationsContainer = Y.one("#annotations");

            var line = null, newLine = function() {
                line = container.appendChild("<p></p>");
            };

            var segments = {}, segment = function(start, end) {
                if (line == null) newLine();
                segments[line.appendChild("<span></span>").set("text", text.content([start, end])).generateID()] = [start, end];
            };

            for (var m = 1, mc = milestones.length, l = 0, lc = lineBreaks.length; m < mc; m++) {
                var start = milestones[m - 1], end = milestones[m];
                while (l < lc) {
                    var lb = lineBreaks[l];
                    if (lb <= start) {
                        newLine();
                        ++l;
                    } else if (lb > start && lb < end) {
                        segment(start, lb);
                        start = lb;
                    } else {
                        break;
                    }
                }
                if (start < end) segment(start, end);
            }

            var selection = rangy.getSelection();
            container.on("mouseup", function(e) {
                selection.refresh();
                var a = Y.one(selection.anchorNode).ancestor("span", true),
                    b = Y.one(selection.focusNode).ancestor("span", true);

                if (a == null || b == null) return;

                var segmentA = segments[a.generateID()],
                    segmentB = segments[b.generateID()];

                if (segmentA == null || segmentB == null) return;

                var ao = segmentA[0] + selection.anchorOffset,
                    bo = segmentB[0] + selection.focusOffset,
                    segment = [Math.min(ao, bo), Math.max(ao, bo)];

                annotationsContainer.empty();
                if (segment[0] < segment[1]) {
                    Y.Array.each(text.index().find(segment), function(a) {
                        this.appendChild("<p></p>").set("text", Y.JSON.stringify(a));
                    }, annotationsContainer);
                }
            });
        });
    });
</script>
</@ie.page>