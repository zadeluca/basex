package org.basex.fs;

import static org.basex.Text.*;
import static org.basex.util.Token.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.basex.io.PrintOutput;
import org.basex.util.Array;
import org.basex.util.BoolList;
import org.basex.util.GetOpts;
import org.basex.util.IntList;
import org.basex.util.Token;
import org.basex.util.TokenList;

/**
 * Performs a ls command.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Hannes Schwarz - Hannes.Schwarz@gmail.com
 */
public final class Ls extends FSCmd {
  /** Slider width. */
  public final SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd hh:mm");
  /** Also lists sub directories. */
  private boolean fRecursive;
  /** Lists files beginning with . */
  private boolean fListDot;
  /** Prints long version. */
  private boolean fLong;
  /** Human readable format. */
  private boolean fHuman;
  /** Sort by file size. */
  private boolean fSize;
  /** Sort by modification time. */
  private boolean fTime;
  /** Sort reverse. */
  private boolean fReverse;
  /** Specified path. */
  private String path;

  @Override
  public void args(final String args) throws FSException {
    // get all Options
    final GetOpts g = new GetOpts(args, "ahlrRSt");
    while(g.more()) {
      final int ch = checkOpt(g);
      switch (ch) {
        case 'a':
          fListDot = true;
          break;
        case 'h':
          fHuman = true;
          break;
        case 'l':
          fLong = true;
          break;
        case 'r':
          fReverse = true;
          break;
        case 'R':
          fRecursive = true;
          break;
        case 'S':
          fSize = true;
          break;
        case 't':
          fTime = true;
          break;
      }
    }
    path = g.getPath();
  }

  @Override
  public void exec(final PrintOutput out) throws IOException {
    // if there is path expression set new pre value
    if(path != null) curPre(path);

    // go to work
    if(fRecursive) {
      lsRecursive(curPre, out);
    } else {
      print(fs.children(curPre), out);
    }
  }

  /**
   * Recursively lists sub directories encountered.
   *
   * @param pre Value of dir
   * @param out output stream
   * @throws IOException in case of problems with the PrintOutput
   */
  private void lsRecursive(final int pre, final PrintOutput out)
      throws IOException {

    for(final int dir : print(fs.children(pre), out)) {
      if(!fListDot) {
        // don´t crawl dirs starting with '.'
        if(startsWith(fs.name(dir), '.')) continue;
      }
      out.print(NL);
      out.print(fs.path(dir));
      out.print(NL);
      lsRecursive(dir, out);
    }
  }

  /**
   * Prints the result.
   * @param result - array to print
   * @param out output stream
   * @throws IOException in case of problems with the PrintOutput
   * @return list of directories found
   */
  private int[] print(final int[] result, final PrintOutput out)
    throws IOException {
    
    // cache list entries
    int n = result.length;
    final TokenList names = new TokenList(n);
    final TokenList sizes = new TokenList(n);
    final TokenList times = new TokenList(n);
    final BoolList dirs = new BoolList(n);
    final IntList nodes = new IntList(n);
    // max width of file names and sizes
    int maxN = 1, maxS = 1;
    int c = 0;
    for(final int pre : result) {
      byte[] tok = fs.name(pre);
      names.add(fs.name(pre));
      maxN = Math.max(maxN, tok.length);
      tok = fs.size(pre);
      sizes.add(tok);
      maxS = Math.max(maxS, tok.length);
      times.add(fs.time(pre));
      dirs.add(fs.isDir(pre));
      nodes.add(c++);
    }
    
    // define sort key and sort entries
    final TokenList sort = fSize ? sizes : fTime ? times : names;
    final boolean num = fSize || fTime;
    nodes.sort(Array.finish(sort.finish(), n), num, !(num ^ fReverse));

    // calculate output widths
    int col = 0;
    int mcl = 80 / maxN;
    maxN += 2;
    maxS += 2;

    // output list and cache directory entries
    final IntList dpre = new IntList();
    for(int i = 0; i < nodes.size; i++) {
      c = nodes.get(i);
      
      final boolean dir = dirs.list[c];
      if(dir) dpre.add(result[c]);
      
      final byte[] name = names.list[c];
      // do not print files starting with .
      if(!fListDot && startsWith(name, '.')) continue;

      if(fLong) {
        final byte[] size = sizes.list[c];
        out.print(dir ? 'd' : 'f');
        out.print(maxS, fHuman ? token(format(toLong(size))) : size);
        out.print(18, time(times.list[c]));
        out.print("  ");
        out.println(name);
      } else {
        out.print(name, maxN);
        col = (col + 1) % mcl;
        if(col == 0) out.print(NL);
      }
    }
    if(col != 0) out.print(NL);
    
    return dpre.finish();
  }

  /**
   * Returns the time as formatted string.
   * @param time time value
   * @return file name.
   */
  public byte[] time(final byte[] time) {
    return token(date.format(new Date(Token.toLong(time) * 60000)));
  }
}
