package tricatch.gotpache.pass;

public interface Stopable extends Runnable {

    public void stop();

    public String getName();
}
