package eu.interedition.text.edit;

import com.google.common.base.Function;

import javax.annotation.Nullable;
import java.util.LinkedList;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class TextOperation extends LinkedList<TextOperation.Component> implements Function<TextSnapshot, TextSnapshot> {

  public void validate() {
    Component last = null;
    for (Component component : this) {
      if (component instanceof Skip) {
        validate(((Skip) component).length > 0, component);
        validate(last == null || !(last instanceof Skip), component);
      } else if (component instanceof InsertText) {
        validate(((InsertText) component).text.length() > 0, component);
      } else if (component instanceof InsertTombstones) {
        validate(((InsertTombstones) component).num > 0, component);
      } else if (component instanceof DeleteText) {
        validate(((DeleteText) component).length > 0, component);
      }
      last = component;
    }
  }

  protected void validate(boolean check, Component c) {
    if (!check) {
      throw new IllegalStateException(c.toString());
    }
  }

  @Nullable
  @Override
  public TextSnapshot apply(@Nullable TextSnapshot input) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public interface Component {
  }

  public class Skip implements Component {
    public final int length;

    public Skip(int length) {
      this.length = length;
    }
  }

  public class InsertText implements Component {
    public final String text;

    public InsertText(String text) {
      this.text = text;
    }
  }

  public class InsertTombstones implements Component {
    public final int num;

    public InsertTombstones(int num) {
      this.num = num;
    }
  }

  public class DeleteText implements Component {
    public final int length;

    public DeleteText(int length) {
      this.length = length;
    }
  }
}
