package org.basex.core.cmd;

import static org.basex.core.Text.*;
import java.io.IOException;
import org.basex.core.CommandBuilder;
import org.basex.core.Context;
import org.basex.core.Main;
import org.basex.core.Command;
import org.basex.core.ProgressException;
import org.basex.core.Prop;
import org.basex.data.DOTSerializer;
import org.basex.data.Result;
import org.basex.data.SerializerProp;
import org.basex.data.XMLSerializer;
import org.basex.io.CachedOutput;
import org.basex.io.IO;
import org.basex.io.NullOutput;
import org.basex.io.PrintOutput;
import org.basex.query.QueryException;
import org.basex.query.QueryProcessor;
import org.basex.query.item.Item;
import org.basex.query.iter.Iter;
import org.basex.util.Performance;

/**
 * Abstract class for database queries.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
public abstract class AQuery extends Command {
  /** Query result. */
  protected Result result;

  /** Query processor. */
  private QueryProcessor qp;
  /** Parsing time. */
  private long pars;
  /** Compilation time. */
  private long comp;
  /** Evaluation time. */
  private long eval;
  /** Printing time. */
  private long prnt;

  /**
   * Protected constructor.
   * @param p command properties
   * @param a arguments
   */
  protected AQuery(final int p, final String... a) {
    super(p, a);
  }

  /**
   * Returns a new query instance.
   * @param query query
   * @return query instance
   */
  protected final boolean query(final String query) {
    final int runs = Math.max(1, prop.num(Prop.RUNS));
    String err = null;
    try {
      // define serialization parameters
      final SerializerProp sp = new SerializerProp(prop.get(Prop.SERIALIZER));
      if(prop.is(Prop.WRAPOUTPUT)) {
        sp.set(SerializerProp.S_WRAP_PRE, NAMELC);
        sp.set(SerializerProp.S_WRAP_URI, URL);
      }

      final boolean ser = prop.is(Prop.SERIALIZE);
      long hits = 0;
      int updates = 0;
      for(int i = 0; i < runs; i++) {
        final Performance per = new Performance();

        qp = new QueryProcessor(query, context);
        progress(qp);

        qp.parse();
        pars += per.getTime();
        if(i == 0) plan(qp, false);
        qp.compile();
        comp += per.getTime();
        if(i == 0) plan(qp, true);

        final PrintOutput po = i == 0 && ser ? out : new NullOutput(!ser);
        XMLSerializer xml;

        if(context.prop.is(Prop.CACHEQUERY)) {
          result = qp.execute();
          eval += per.getTime();
          xml = qp.getSerializer(po, sp);
          result.serialize(xml);
          hits = result.size();
        } else {
          final Iter ir = qp.iter();
          eval += per.getTime();
          hits = 0;
          Item it = ir.next();
          xml = qp.getSerializer(po, sp);
          while(it != null) {
            checkStop();
            xml.openResult();
            it.serialize(xml);
            xml.closeResult();
            it = ir.next();
            hits++;
          }
        }
        updates = qp.updates();
        xml.close();
        qp.close();
        prnt += per.getTime();
      }
      // dump some query info
      if(prop.is(Prop.QUERYINFO)) evalInfo(hits, updates, runs);
      out.flush();
      return info(NL + QUERYEXEC, perf.getTimer(runs));
    } catch(final QueryException ex) {
      Main.debug(ex);
      err = ex.getMessage();
    } catch(final IOException ex) {
      Main.debug(ex);
      err = ex.getMessage();
    } catch(final ProgressException ex) {
      err = PROGERR;
    }
    // close processor after exceptions
    try { if(qp != null) qp.close(); } catch(final IOException ex) { }
    return error(err);
  }

  /**
   * Checks if the query performs updates.
   * @param ctx context reference
   * @param qu query
   * @return result of check
   */
  protected final boolean updating(final Context ctx, final String qu) {
    return QueryProcessor.updating(ctx, qu);
  }

  /**
   * Performs the first argument as XQuery and returns a node set.
   */
  protected final void queryNodes() {
    try {
      result = new QueryProcessor(args[0], context).queryNodes();
    } catch(final QueryException ex) {
      Main.debug(ex);
      error(ex.getMessage());
    }
  }

  @Override
  public final Result result() {
    return result;
  }

  /**
   * Adds evaluation information to the information string.
   * @param hits information
   * @param updates updated items
   * @param runs number of runs
   */
  private void evalInfo(final long hits, final long updates, final int runs) {
    info("");
    info("");
    info(QUERYSTRING + qp.query());
    info(qp.info());
    info("");
    info(QUERYPARSE + Performance.getTimer(pars, runs));
    info(QUERYCOMPILE + Performance.getTimer(comp, runs));
    info(QUERYEVALUATE + Performance.getTimer(eval, runs));
    info(QUERYPRINT + Performance.getTimer(prnt, runs));
    info(QUERYTOTAL + Performance.getTimer(pars + comp + eval + prnt, runs));
    info(QUERYHITS + hits + " " + (hits == 1 ? VALHIT : VALHITS));
    info(QUERYUPDATED + updates + " " + (updates == 1 ? VALHIT : VALHITS));
    info(QUERYPRINTED + Performance.format(out.size()));
    info(QUERYMEM, Performance.getMem());
  }

  /**
   * Creates query plans.
   * @param qu query reference
   * @param c compiled flag
   */
  private void plan(final QueryProcessor qu, final boolean c) {
    if(c != prop.is(Prop.COMPPLAN)) return;

    // show dot plan
    try {
      if(prop.is(Prop.DOTPLAN)) {
        final CachedOutput co = new CachedOutput();
        final DOTSerializer d = new DOTSerializer(co, prop.is(Prop.DOTCOMPACT));
        qu.plan(d);
        d.close();
        final String dot = "plan.dot";
        IO.get(dot).write(co.toArray());

        if(prop.is(Prop.DOTDISPLAY))
          new ProcessBuilder(prop.get(Prop.DOTTY), dot).start();
      }
      // show XML plan
      if(prop.is(Prop.XMLPLAN)) {
        final CachedOutput co = new CachedOutput();
        qu.plan(new XMLSerializer(co));
        info(NL + QUERYPLAN);
        info(co.toString());
      }
    } catch(final Exception ex) {
      Main.debug(ex);
    }
  }

  @Override
  public void build(final CommandBuilder cb) {
    cb.init().xquery(0);
  }
}