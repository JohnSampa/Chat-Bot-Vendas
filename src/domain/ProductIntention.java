package domain;

import java.util.List;

public class ProductIntention extends Intention {
    public ProductIntention(List<String> intentions, Runnable runnable) {
        super(intentions, runnable);
    }
}
