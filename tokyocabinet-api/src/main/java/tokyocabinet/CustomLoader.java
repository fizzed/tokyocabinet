package tokyocabinet;

import com.fizzed.jne.JNE;
import com.fizzed.jne.MemoizedRunner;

/**
 * Custom safe run once loading of native libs.
 */
public class CustomLoader {

  static private final MemoizedRunner loader = new MemoizedRunner();

  static public void loadLibrary() {
    loader.once(() -> {
      JNE.loadLibrary("jtokyocabinet");
    });
  }

}