package domain;

import java.util.List;

public class Intention {

    private List<String> intentions;

    private Runnable runnable;

    public Intention(List<String> intentions, Runnable runnable) {
        this.intentions = intentions;
        this.runnable = runnable;
    }

    public List<String> getIntentions() {
        return intentions;
    }

    public void setIntentions(List<String> intentions) {
        this.intentions = intentions;
    }

    public Runnable getRunnable() {
        return runnable;
    }

    public void setRunnable(Runnable runnable) {
        this.runnable = runnable;
    }
}
