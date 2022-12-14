package tokyocabinet;

import com.fizzed.jne.JNE;

/**
 * Custom double-locked safe loading of native libs.
 */
public class CustomLoader {

  static private boolean LOADED = false;

  static public void loadLibrary() {
    if (LOADED) {
      return;
    }
    synchronized (CustomLoader.class) {
      if (!LOADED) {
        LOADED = true;
        JNE.loadLibrary("tokyocabinet");
        JNE.loadLibrary("jtokyocabinet");
      }
    }
  }

}