<@ie.page "Home">
<h1>Text Repository Server API</h1>
<h2>Adding XML documents to the text repository</h2>
<h3>method=POST path=/xml-transform</h3>
<p>Feed XML documents to the text repository</p>
<p>Example: </p>
<pre> find . -name '*.xml' -exec curl -v -X POST -d @{} localhost:8080/xml-transform/ -H "Content-Type: application/xml" \;</pre>
<p>Returns </p>
<pre>
    HTTP/1.1 201 Created
    Location: http://localhost:8080/1080
    ...
    </pre>


<h2>Adding layers and annotations</h2>
<h3>method=POST path=/</h3>
<p>Examples:</p>
<ol>
    <li><p>Creation of a base layer:</p>
	<pre>curl -i -X POST  -d '{"name":"layerName", "text":"abcd..."}' http://localhost:8080/ \
	-H "Content-Type: application/json"  -H "Accept: application/json"</pre>
        <p>Returns the id</p>
        <p>Example: </p><pre>{"id": 15}</pre>
    </li>
    <li><p> Create an annotation</p>
	<pre>
	curl -i -X POST http://localhost:8080/ -H "Content-Type: application/json" -H "Accept: application/json" -d \
{
    "name": "layerName",
    "text": "comments...",
    "targets": [
        {
            "id": 10,
            "ranges": [
                [
                    10,
                    20
                ],
                [
                    30,
                    40
                ]
            ]
        },
        {
            "id": 11,
            "ranges": [
                [
                    10,
                    20
                ],
                [
                    30,
                    40
                ],
                [
                    50,
                    60
                ]
            ]
        }
    ]
}</pre>
        <p>Returns the id</p>
        <p>Example: </p><pre>{"id": 15}</pre>
    </li>
</ol>

<h2>Get layers and annotations</h2>
<h3>method=GET path=/{id}</h3>
<p>Get a layer by its <em>id</em></p>
<p>Example:</p>
<pre>http://localhost:8080/2049</pre>
<p>Returns a layer</p>
<p>Examples:</p> <pre>
{
    "id": 2049,
    "name": "layerName",
    "text": "comments...",
    "targets": [
        {
            "id": 10,
            "ranges": [
                [
                    10,
                    20
                ],
                [
                    30,
                    40
                ]
            ]
        },
        {
            "id": 11,
            "ranges": [
                [
                    10,
                    20
                ],
                [
                    30,
                    40
                ],
                [
                    50,
                    60
                ]
            ]
        }
    ]
}
	</pre>

<h2>Query the text repository</h2>

<h3>method=GET path=/?q={query}</h3>

<p>Query the text repository with query language expressions</p>
<!--
        (and (sexpression1) (sexpression2)),
        (or (sexpression1) (sexpression2)),
        (text ID),
        (name localname namespace),
        (overlaps number1 number2),
        (length number),
        (range number1 number2), (number1 number2),
        (any),
        (none)
    -->
<p>Examples:</p>
<ol>
    <li><pre>curl 'http://localhost:8080/?q=(range 20 30)' -H "Accept: application/json"</pre>
        <p>Returns an array of ids</p>
        <p>Example: </p><pre>[{"id": 15},{"id": 20}]</pre>

        <pre>curl 'http://localhost:8080/?q=(any)' -H "Accept: application/json"</pre></li>
    <li><p>Returns all layers</p>
        <p>Example: </p><pre>[{"name":["http://www.w3.org/XML/1998/namespace","document"],"anchors":[],"data":null,"id":2},{"name":["http://interedition.eu/ns","text"],"anchors":[{"t":{"name":["http://www.w3.org/XML/1998/namespace","document"],"id":2},"range":[0,1101]}],"data":null,"id":4},...]</pre>
    </li>
</ol>

<h2>Combining getting layers and annotations with query</h2>
<h3>method=GET path=/{id}?q={query}</h3>
<p>Query a layer</p>
<p>Example:</p>
<pre>curl 'http://localhost:8080/2020?q=(range 20 30)' -H "Accept: application/json"</pre>
<p>Returns an array of ids</p>
<p>Example: </p><pre>[{"id": 15}]</pre>
</@ie.page>