import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class PacketInfo {
    private String docolumn;
    private String ifcolumn;
    private final BooleanProperty done = new SimpleBooleanProperty(false);

    public PacketInfo() {
    }

    public String getDocolumn() {
        return docolumn;
    }

    public void setDocolumn(String docolumn) {
        this.docolumn = docolumn;
    }

    public String getIfcolumn() {
        return ifcolumn;
    }

    public void setIfcolumn(String ifcolumn) {
        this.ifcolumn = ifcolumn;
    }

    public boolean isDone() {
        return done.get();
    }

    public void setDone(boolean done) {
        this.done.set(done);
    }

    public BooleanProperty doneProperty() {
        return done;
    }
}
