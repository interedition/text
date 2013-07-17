YUI.add("text-editor-canvas", function(Y) {
    Y.TextEditor = function() {

        // system
        var canvasContext;
        var canvasID;
        var asyncQueue;

        // text store
        var text;
        var annotations;

        // constants
        var startX, startY;
        var canvasWidth, canvasHeight;
        var refreshDelay;
        var textFillStyle;
        var lineHeight;
        var laneHeight;
        var annotationBarHeight;
        var resizeHandleWidth;

        // state
        var caretOffset;
        var renderCaretState;
        var lastCaretStateChangeTime;
        var lineStartOffsets;
        var lineCharacterPositions;
        var currentAnnotationBar;

        this.init = function(canvasID, width, height) {

            // setup canvas
            var canvas = document.getElementById(canvasID);
            this.canvasContext = canvas.getContext("2d");
            this.canvasContext.font = "12pt serif";
            this.canvasID = canvasID;

            // capture keyboard and mouse events
            Y.on('keydown', this.onControlKeyPress, document, this);
            Y.on('keypress', this.onKeyPress, document, this);
            Y.on('mousedown', this.onMouseDown, "#" + canvasID, this);
            Y.on('mouseup', this.onMouseUp, "#" + canvasID, this);
            Y.on('mousemove', this.onMouseMove, "#" + canvasID, this);

            // set constants
            this.startX = 10;
            this.startY = 30;
            this.laneHeight = 6;
            this.annotationBarHeight = 5;
            this.canvasWidth = width;
            this.canvasHeight = height;
            this.refreshDelay = 10;
            this.textFillStyle = "rgb(0,0,0)";
            this.resizeHandleWidth = 5;

            // initialize state
            this.caretOffset = 0;
            this.renderCareState = false;
            this.lastCaretStateChangeTime = 0;
            this.currentAnnotationBar = null;

            // test data
            //this.text = "";
            this.annotations = [];

            this.text = "My mistress' eyes are nothing like the sun;\nCoral is far more red than her lips' red:\nIf snow be white, why then her breasts are dun;\nIf hairs be wires, black wires grow on her head.\nI have seen roses damask'd, red and white,\nBut no such roses see I in her cheeks;\nAnd in some perfumes is there more delight\nThan in the breath that from my mistress reeks.\nI love to hear her speak,--yet well I know\nThat music hath a far more pleasing sound;\nI grant I never saw a goddess go,\nMy mistress when she walks, treads on the ground;\nAnd yet, by heaven, I think my love as rare\nAs any she belied with false compare.";
            //		this.text = "012345678901234567890\n";

            var defaultAnnotations = [
                { offset:0, range:20, style:"rgb(200,0,0)" },
                { offset:5, range:50, style:"rgb(0,200,0)" },
                { offset:10, range:5, style:"rgb(0,0,200)" },
                { offset:15, range:5, style:"rgb(200,200,0)" }
            ];

            this.setAnnotations( defaultAnnotations );

        };

        this.setAnnotations = function( updatedAnnotations ) {
            this.annotations = updatedAnnotations;
            this.lineHeight = 30 + (this.annotations.length * this.laneHeight);
        };

        this.startEditing = function() {
            this.asyncQueue = new Y.AsyncQueue();
            this.asyncQueue.add({
                fn: this.render,
                context: this,
                timeout: this.refreshDelay,
                until: function() { return false; }
            });
            this.asyncQueue.run();
        };

        this.stopEditing = function() {
            if( this.asyncQueue ) {
                this.asyncQueue.pause();
            }
        };

        this.onControlKeyPress = function(e) {
            // backspace
            if( e.keyCode == 8 ) {
                this.backspace();
                e.preventDefault();
            }
            // left arrow
            else if( e.keyCode == 37 ) {
                this.moveCaret(-1);
            }
            // right arrow
            else if( e.keyCode == 39 ) {
                this.moveCaret(1);
            }
            // up arrow
            else if( e.keyCode == 38 ) {
                this.gotoPrevLine();
            }
            // down arrow
            else if( e.keyCode == 40 ) {
                this.gotoNextLine();
            }
        };

        this.onKeyPress = function(e) {
            e.preventDefault();


            if( e.keyCode != 8 && !(e.keyCode >= 37 && e.keyCode <= 40)  ) {
                this.insertText( String.fromCharCode(e.keyCode) );
                this.moveCaret(1);
            }
        };

        this.mousePositionToCanvasPosition = function(mouseX,mouseY) {
            var canvas = Y.one('#'+this.canvasID);
            var point = canvas.getXY();
            var x = mouseX - point[0] - 2;
            var y = mouseY - point[1] - 3;
            return { x: x, y: y };
        };

        this.onMouseDown = function(e) {
            var clickPosition = this.mousePositionToCanvasPosition(e.pageX, e.pageY);
            this.currentAnnotationBar = this.positionToAnnotationBar(clickPosition);

            if( this.currentAnnotationBar ) {
                var bar = this.annotations[ this.currentAnnotationBar.id ].bars[ this.currentAnnotationBar.bar ];

                var endHandleX = bar.x + bar.w - this.resizeHandleWidth;
                var startHandleRect = { x: bar.x, y: bar.y, w: this.resizeHandleWidth, h: this.resizeHandleWidth };
                var endHandleRect = { x: endHandleX, y: bar.y, w: this.resizeHandleWidth, h: this.resizeHandleWidth };

                // did we hit start resize handle?
                if( this.isPointInRect(clickPosition, startHandleRect) ) {
                    this.currentAnnotationBar.command = "resize-start";
                }
                // did we hit end resize handle?
                else if( this.isPointInRect(clickPosition, endHandleRect) ) {
                    this.currentAnnotationBar.command = "resize-end";
                }
                // just moving
                else {
                    this.currentAnnotationBar.command = "move";
                }
            }
        };

        this.onMouseUp = function(e) {
            this.currentAnnotationBar = null;
        };

        this.onMouseMove = function(e) {
            // if we are dragging an annotation bar, interpret command
            if( this.currentAnnotationBar ) {
                var command = this.currentAnnotationBar.command;
                var clickPosition = this.mousePositionToCanvasPosition(e.pageX, e.pageY);
                var updatedOffset = this.positionToOffset( clickPosition.x, clickPosition.y );
                if( command == "move" ) {
                    this.annotations[ this.currentAnnotationBar.id ].offset = updatedOffset;
                } else if( command == "resize-start" ) {
                    var range = this.annotations[ this.currentAnnotationBar.id ].range;
                    var rangeDelta = this.annotations[ this.currentAnnotationBar.id ].offset - updatedOffset;
                    this.annotations[ this.currentAnnotationBar.id ].offset = updatedOffset;
                    this.annotations[ this.currentAnnotationBar.id ].range = range + rangeDelta;
                } else if( command == "resize-end" ) {
                    var originalOffset = this.annotations[ this.currentAnnotationBar.id ].offset;
                    this.annotations[ this.currentAnnotationBar.id ].range = updatedOffset - originalOffset;
                }
            }
        };

        this.gotoPrevLine = function() {
            // make sure there is a next line
            var currentLineNumber = this.offsetToLineNumber( this.caretOffset );
            var prevLineNumber = currentLineNumber - 1;

            if( prevLineNumber >= 0 ) {
                var currentX = this.offsetToCaretX( this.caretOffset );
                var prevLineY = this.lineNumberToCaretY(prevLineNumber);
                this.caretOffset = this.positionToOffset( currentX, prevLineY );
            }
        };

        this.gotoNextLine = function() {
            // make sure there is a next line
            var currentLineNumber = this.offsetToLineNumber( this.caretOffset );
            var nextLineNumber = currentLineNumber + 1;

            if( nextLineNumber < this.lineStartOffsets.length ) {
                var currentX = this.offsetToCaretX( this.caretOffset );
                var nextLineY = this.lineNumberToCaretY(nextLineNumber);
                this.caretOffset = this.positionToOffset( currentX, nextLineY );
            }
        };

        this.isPointInRect = function( point, rect ) {
            var top = rect.y;
            var left = rect.x;
            var right = rect.x + rect.w;
            var bottom = rect.y + rect.h;
            return ( point.x >= left && point.y >= top && point.x < right && point.y < bottom );
        };

        this.positionToAnnotationBar = function( point ) {

            for( var i=0; i < this.annotations.length; i++ ) {
                var annotation = this.annotations[i];

                for( var j=0; j < annotation.bars.length; j++ ) {
                    var bar = annotation.bars[j];
                    var barTop = bar.y;
                    var barLeft = bar.x;
                    var barRight = bar.x + bar.w;
                    var barBottom = bar.y + bar.h;

                    // did we hit this bar?
                    if( this.isPointInRect( point, bar ) ) {
                        return { id: i, annotation: annotation, bar: j };
                    }
                }
            }
            return null;
        };

        this.positionToOffset = function( x, y ) {

            var lineNumber = Math.floor( y / this.lineHeight );
            var lineStart = this.lineStartOffsets[lineNumber];
            var lineLength = this.lineCharacterPositions[lineNumber].length;

            // figure out closest character offset to (x,y)
            for( var i = 0; i < lineLength; i++ ) {
                var lineCharacterPosition = this.lineCharacterPositions[lineNumber][i];
                if( lineCharacterPosition >= x ) {
                    return i + lineStart;
                }
            }
            return lineStart + lineLength;
        };

        this.lineNumberToCaretY = function( lineNumber ) {
            return this.lineHeight * lineNumber;
        };

        this.offsetToCaretX = function( offset ) {
            var currentLineNumber = this.offsetToLineNumber( offset );
            var lineStart = this.lineStartOffsets[currentLineNumber];
            return this.lineCharacterPositions[currentLineNumber][ offset - lineStart ];
        };

        this.moveCaret = function( offset ) {

            var resultingPosition = this.caretOffset + offset;

            // clip caret position to bounds of text
            if( resultingPosition < 0 ) {
                resultingPosition = 0;
            } else if( resultingPosition > this.text.length ) {
                resultingPosition = this.text.length;
            }

            this.caretOffset = resultingPosition;
        };

        this.insertText = function( insertedText ) {
            if( this.caretOffset == 0 ) {
                // if you are at the beginning of the line, pre-prend the text
                this.text = insertedText + this.text;
            } else if( this.caretOffset == this.text.length ) {
                // if you are at the end of the line, append the text
                this.text = this.text + insertedText;
            } else {
                // if you are in the midst of the line, split string and insert in between split
                var firstPart = this.text.substr(0,this.caretOffset)
                var secondPart = this.text.substr(this.caretOffset,this.text.length);
                this.text = firstPart + insertedText + secondPart;
            }

            // scan through the annotations and update
            for( var i=0; i < this.annotations.length; i++ ) {
                var annotation = this.annotations[i];
                var annotationEnd = annotation.offset + annotation.range;

                if( this.caretOffset < annotation.offset ) {
                    annotation.offset = annotation.offset + 1;
                } else if( this.caretOffset <= annotationEnd ) {
                    annotation.range = annotation.range + 1;
                }
            }
        };

        this.backspace = function() {
            if( this.caretOffset != 0 ) {
                // if you are in the midst of the line, split string and insert in between split
                var firstPart = this.text.substr(0,this.caretOffset-1);
                var secondPart = this.text.substr(this.caretOffset,this.text.length);
                this.text = firstPart + secondPart;

                // scan through the annotations and update
                for( var i=0; i < this.annotations.length; i++ ) {
                    var annotation = this.annotations[i];
                    var annotationEnd = annotation.offset + annotation.range;

                    if( this.caretOffset <= annotation.offset ) {
                        annotation.offset = annotation.offset - 1;
                    } else if ( this.caretOffset <= annotationEnd ) {
                        annotation.range = annotation.range - 1;
                    }
                }

                // decrement caret position
                this.caretOffset = this.caretOffset - 1;
            }
        };

        this.render = function() {
            // clear the surface
            this.canvasContext.clearRect(0,0,this.canvasWidth,this.canvasHeight);

            // render text
            this.renderText(this.startX, this.startY);

            this.renderAnnotations(this.startX, this.startY+10);

        };

        this.renderCaret = function(x,y) {

            var currentTime = new Date().getTime();
            var elapsedTime = currentTime - this.lastCaretStateChangeTime;

            if( elapsedTime > 250 ) {
                this.renderCaretState = !this.renderCaretState;
                this.lastCaretStateChangeTime = currentTime;
            }

            if( this.renderCaretState ) {
                this.canvasContext.fillRect(x,y-12,2,12);
            }
        };

        this.offsetToLineNumber = function( offset ) {

            for( var i=1; i < this.lineStartOffsets.length; i++ ) {
                var lineOffset = this.lineStartOffsets[i];

                if( lineOffset > offset ) {
                    break;
                }
            }

            return i-1;
        };

        this.renderAnnotations = function(baseX,baseY) {

            for( var i=0; i < this.annotations.length; i++ ) {
                var annotation = this.annotations[i];
                annotation.bars = [];
                var barCount = 0;
                this.canvasContext.fillStyle = annotation.style;

                var annotatedText = this.text.substr(annotation.offset,annotation.range);

                // compute line numbers of start and end points
                var annotationEnd = annotation.offset + annotation.range;
                var startLine = this.offsetToLineNumber( annotation.offset );
                var endLine = this.offsetToLineNumber( annotationEnd-1 );

                // go through the lines
                for( var j = startLine; j <= endLine; j++ ) {
                    var x,y,w,h;

                    var lineStart = this.lineStartOffsets[j];
                    y = (baseY + (this.lineHeight*j)) + (this.laneHeight*i);
                    h = this.annotationBarHeight;
                    var renderStartHandle = false;
                    var renderEndHandle = false;

                    if( j == startLine ) {
                        x = this.lineCharacterPositions[j][annotation.offset-lineStart];
                        renderStartHandle = true;
                    } else {
                        x = 0;
                    }

                    if( j == endLine ) {
                        var barStart = (startLine == endLine) ? annotation.offset : this.lineStartOffsets[j];
                        var remainingText = this.text.substr(barStart,annotationEnd-barStart);
                        w = this.canvasContext.measureText(remainingText).width;
                        renderEndHandle = true;
                    } else {
                        w = this.canvasWidth;
                    }

                    // render the annotation bar
                    this.canvasContext.fillRect(x,y,w,h);

                    // render the resize handles
                    var currentStyle = this.canvasContext.fillStyle;
                    this.canvasContext.fillStyle = "rgb(0,0,0)";

                    if( renderStartHandle ) {
                        this.canvasContext.fillRect(x,y,this.resizeHandleWidth,h);
                    }

                    if( renderEndHandle ) {
                        this.canvasContext.fillRect((x+w-this.resizeHandleWidth),y,this.resizeHandleWidth,h);
                    }

                    this.canvasContext.fillStyle = currentStyle;

                    // store bar location for hit detection
                    annotation.bars[barCount++] = { x: x, y: y, w: w, h: h };
                }
            }

        };

        this.renderStaffBar = function(baseX,baseY) {
            var currentFillStyle = this.canvasContext.fillStyle;
            this.canvasContext.fillStyle = "rgb(196,196,196)";
            this.canvasContext.fillRect(baseX,baseY+8,1,this.lineHeight-30);
            this.canvasContext.fillStyle = currentFillStyle;
        };

        this.renderText = function(baseX,baseY) {

            // reset position buffers
            this.lineStartOffsets = [ 0 ];
            this.lineCharacterPositions = [[]];

            var currentLine = 0;
            var currentX = baseX;
            var currentY = baseY;
            this.canvasContext.fillStyle = this.textFillStyle;

            for( var i = 0; i < this.text.length; i++ ) {
                if( this.caretOffset == i) {
                    this.renderCaret(currentX,currentY);
                }

                var character = this.text.substr(i,1);

                if( character == "\n" ) {
                    this.renderStaffBar(currentX,currentY);
                    currentX = baseX;
                    currentY = currentY + this.lineHeight;
                    currentLine = currentLine + 1;
                    this.lineCharacterPositions[currentLine] = [];
                    this.lineStartOffsets[currentLine] = i+1;
                    this.renderStaffBar(currentX,currentY);
                } else {
                    this.lineCharacterPositions[currentLine][i - this.lineStartOffsets[currentLine]] = currentX;
                    this.canvasContext.fillText(character,currentX,currentY);
                    var lastGlyphWidth = this.canvasContext.measureText(character).width;
                    var nextCharPosition = currentX + lastGlyphWidth;

                    if( nextCharPosition >= this.canvasWidth ) {
                        this.renderStaffBar(currentX,currentY);
                        currentX = baseX;
                        currentY = currentY + this.lineHeight;
                        currentLine = currentLine + 1;
                        this.lineStartOffsets[currentLine] = i+1;
                        this.renderStaffBar(currentX,currentY);
                    } else {
                        currentX = nextCharPosition;
                    }
                }
            }

        };

    }
}, "0.0", {
    requires: [ "node", "event", "event-key", "console", "async-queue", "dump", "escape" ]
});
