package org.stone.test.beeop.poolkey;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;

/**
 * @author Chris Liao
 */
public class Tc0060ObjectCallTest {

    @Test
    public void testGetAuthor() throws Throwable {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            try (BeeObjectHandle<String, Book> bookHandle = os.getObjectHandle()) {
                long accessTime0 = bookHandle.getLastAccessedTime();

                Assertions.assertEquals("Bruce Eckel", bookHandle.call("getAuthor"));
                long accessTime1 = bookHandle.getLastAccessedTime();
                Assertions.assertTrue(accessTime1 > 0);
                Assertions.assertEquals("Bruce Eckel", bookHandle.call("getAuthor", new Class[0], new Object[0]));
                long accessTime2 = bookHandle.getLastAccessedTime();
                Assertions.assertTrue(accessTime2 > 0);
                Assertions.assertTrue(accessTime1 >= accessTime0);
                Assertions.assertTrue(accessTime2 >= accessTime1);
            }
        }
    }
}
