package org.basex.build.fs.metadata;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.basex.build.Builder;
import org.basex.util.Token;
import static org.basex.build.fs.FSText.*;

/**
 * TIF meta data extractor.
 * 
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Christian Gruen
 */
public final class TIFExtractor extends EXIFExtractor {
  @Override
  public void extract(final Builder listener, final File f) throws IOException {
    
    //byte[] data = new BufferInput(f, 10).firstBlock();
    final BufferedInputStream in =
      new BufferedInputStream(new FileInputStream(f));

    // check if the header is valid
    if(!checkEndian(in)) {
      in.close();
      return;
    }

    // check magic number
    if(getShort(in) != 0x2a) {
      in.close();
      return;
    }
    skip(in, getInt(in) - 8);

    // extract image dimensions
    int c = getShort(in);
    skip(in, 4);
    
    int w = -1;
    int h = -1;
    
    while(c-- >= 0 && w == -1 || h == -1) {
      final int b = getShort(in);
      skip(in, 6);
      if(b == 0x100) w = getInt(in);
      if(b == 0x101) h = getInt(in);
    }
    in.close();
    if(w == -1 || h == -1) return;

    // open image tag
    listener.startElem(IMAGE, atts.set(TYPE, TYPEJPG));
    listener.nodeAndText(WIDTH, atts.reset(), Token.token(w));
    listener.nodeAndText(HEIGHT, atts, Token.token(h));
    listener.endElem(IMAGE);
  }
}
