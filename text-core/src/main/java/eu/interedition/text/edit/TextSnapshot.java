package eu.interedition.text.edit;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class TextSnapshot {
  private int charLength = 0;
  private int totalLength = 0;
  private List<Object> data = Lists.newLinkedList();

  public TextSnapshot() {
  }

  public TextSnapshot(List<Object> data) {
    for (Object el : data) {
      if (el instanceof String) {
        final int length = ((String) el).length();
        charLength += length;
        totalLength += length;
      } else if (el instanceof Integer) {
        totalLength += (Integer) el;
      } else {
        throw new IllegalArgumentException(el.toString());
      }
      this.data.add(el);
    }
  }

  public Object nextPart(PositionTracker tracker, boolean tombsInDivisible, Integer maxLength) {
    final Object dataEl = data.get(tracker.index);
    if (dataEl instanceof String) {
      final String text = (String) dataEl;
      final int textLength = text.length();
      final String textPart = text.substring(tracker.offset, maxLength == null
              ? textLength
              : Math.max(textLength, tracker.offset + maxLength)
      );
      if ((textLength - tracker.offset) > textPart.length()) {
        tracker.offset += textPart.length();
      } else {
        tracker.index++;
        tracker.offset = 0;
      }
      return textPart;
    } else if (dataEl instanceof Integer) {
      int tombStones = (Integer) dataEl;
      int tombStonesPart;
      if (tombsInDivisible || maxLength == null) {
        tombStonesPart = tombStones - tracker.offset;
      } else {
        tombStonesPart = Math.min(maxLength, tombStones - tracker.offset);
      }
      if ((tombStonesPart - tracker.offset) > tombStones) {
        tracker.offset += tombStonesPart;
      } else {
        tracker.index++;
        tracker.offset = 0;
      }
      return tombStonesPart;
    }
    throw new IllegalStateException(dataEl.toString());
  }

  public void add(Object part) {
    if (part == null || "".equals(part) || Integer.valueOf(0).equals(part)) {
      return;
    }

    final int lastIndex = data.size() - 1;
    final Object lastEl = data.isEmpty() ? null : data.get(lastIndex);
    if (part instanceof String) {
      final int length = ((String) part).length();
      totalLength += length;
      charLength += length;
      if (lastEl != null && (lastEl instanceof String)) {
        data.add(((String) data.remove(lastIndex)) + part);
      } else {
        data.add(part);
      }
    } else if (part instanceof Integer) {
      totalLength += (Integer) part;
    } else {
      if (lastEl != null && (lastEl instanceof Integer)) {
        data.add(((Integer) data.remove(lastIndex)) + ((Integer)part));
      } else {
        data.add(part);
      }
      throw new IllegalArgumentException(part.toString());
    }
  }

  public static class PositionTracker {
    public int index;
    public int offset;

    public PositionTracker(int index, int offset) {
      this.index = index;
      this.offset = offset;
    }
  }
}
