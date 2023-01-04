package tokyocabinet;

import org.junit.Test;

public class CustomLoaderTest {

    @Test
    public void load() {
        Loader.load();
        Loader.load();
        Loader.load();
        Loader.load();
        Loader.load();
    }

}