import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This example shows how new databases can be created.
 * Documentation: http://basex.org/api
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 */
public final class CreateExample {
  /**
   * Main method.
   * @param args command-line arguments
   */
  public static void main(final String[] args) {
    new CreateExample();
  }

  /**
   * Constructor.
   */
  private CreateExample() {
    try {
      // create session
      BaseXClient session =
        new BaseXClient("localhost", 1984, "admin", "admin");

      try {
        // define input stream
        InputStream bais =
          new ByteArrayInputStream("<xml>Hello World!</xml>".getBytes());

        // create new database
        session.create("database", bais);
        System.out.println(session.info());

        // run query on database
        System.out.println(session.execute("xquery /"));

      } catch(IOException ex) {
        // print exception
        ex.printStackTrace();
      }

      // close session
      session.close();

    } catch(IOException ex) {
      // print exception
      ex.printStackTrace();
    }
  };
}
