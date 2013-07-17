/*
 * Copyright (c) 2013 The Interedition Development Group.
 *
 * This file is part of CollateX.
 *
 * CollateX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CollateX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CollateX.  If not, see <http://www.gnu.org/licenses/>.
 */
YUI.add("interedition-text", function(Y) {

    var NS = Y.namespace("interedition");

    NS.AnnotatedText = function(id, content, annotations, segment, length) {
        this.id = id;
        this._content = (content || "");
        this.annotations = (annotations || []);
        this.length = (length || this.content.length);
        this.segment = (segment || [0, length]);
    };

    Y.extend(NS.AnnotatedText, Object, {
        offset: function() {
            return this.segment[0];
        },
        content: function(segment) {
            if (!segment) return this._content;
            var offset = this.offset(), length = this._content.length;
            return this._content.substring(Math.max(0, segment[0] - offset), Math.min(segment[1] - offset, length));
        },
        milestones: function() {
            if (this._milestones) return this._milestones;

            var milestones = [];
            Y.Array.each(this.annotations, function (a) {
                Y.Array.each(a.targets, function (t) {
                    if (t[0] == this.id) {
                        var ms = 0, me = milestones.length, start = t[1], end = t[2];
                        while (ms < me && milestones[ms] < start) ms++;
                        if (ms == me || milestones[ms] != start) {
                            milestones.splice(ms, 0, start);
                            me++;
                        }
                        me--;
                        while (me > ms && milestones[me] > end) me--;
                        if (milestones[me] != end) milestones.splice(me + 1, 0, end);
                    }
                }, this);
            }, this);

            if (milestones.length == 0 || milestones[0] > this.segment[0]) milestones.unshift(this.segment[0]);
            if (milestones.length == 1 || milestones[milestones.length - 1] < this.segment[1]) milestones.push(this.segment[1]);

            return (this._milestones = milestones);
        },
        lineBreaks: function() {
            var lb = this._content.indexOf("\n", 0), offset = this.offset(), newLines = [];
            while (lb != -1) {
                newLines.push(offset + lb);
                lb = this._content.indexOf("\n", lb + 1);
            }
            return newLines;
        },
        index: function() {
            if (this._index) return this._index;

            var annotations = [];
            for (var a = 0, ac = this.annotations.length; a < ac; a++) {
                var annotation = this.annotations[a], targets = annotation.targets;
                for (var at = 0, atc = targets.length; at < atc; at++) {
                    var target = targets[at];
                    if (this.id == target[0]) annotations.push([[target[1], target[2]], annotation]);
                }
            }
            return (this._index = new Y.interedition.SegmentIndex(annotations));
        }
    });
}, '0.1', {
    requires: ['array-extras', 'segment-index']
});