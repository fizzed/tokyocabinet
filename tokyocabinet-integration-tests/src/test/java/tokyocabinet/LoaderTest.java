package tokyocabinet;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class LoaderTest {

    @Test
    public void load() {
        HDB db = new HDB();
        if (!db.open("target/casket.tch", HDB.OWRITER | HDB.OCREAT)){
            int ecode = db.ecode();
            throw new RuntimeException("TokyoCabinet open error: " + db.errmsg(ecode));
        }

        db.put("hello".getBytes(), "world".getBytes());

        assertThat(db.get("hello".getBytes()), is("world".getBytes()));
    }

}