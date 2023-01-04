package tokyocabinet;

import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class DBTest {

    @Test
    public void hdb() {
        HDB db = new HDB();

        if (!db.open("target/"+ UUID.randomUUID()+".tch", HDB.OWRITER | HDB.OCREAT)){
            int ecode = db.ecode();
            throw new RuntimeException("TokyoCabinet open error: " + db.errmsg(ecode));
        }

        db.put("hello".getBytes(), "world".getBytes());

        assertThat(db.get("hello".getBytes()), is("world".getBytes()));
    }

    @Test
    public void bdb() {
        BDB db = new BDB();

        if (!db.open("target/"+ UUID.randomUUID()+".tcb", HDB.OWRITER | HDB.OCREAT)){
            int ecode = db.ecode();
            throw new RuntimeException("TokyoCabinet open error: " + db.errmsg(ecode));
        }

        db.put("hello".getBytes(), "world".getBytes());

        assertThat(db.get("hello".getBytes()), is("world".getBytes()));
    }

}