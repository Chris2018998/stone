package org.stone.test.beeop.objectsource;

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
public class Tc0081PooledObjectCallTest {

    @Test
    public void testGetAuthor() throws Throwable {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            try (BeeObjectHandle<String, Book> bookHandle = os.getObjectHandle()) {
                long accessTime0 = bookHandle.getLastAccessedTime();

                try {
                    bookHandle.call(null);
                    Assertions.fail("[Tc0081PooledObjectCallTest.testGetAuthor]failed");
                } catch (Exception e) {
                    Assertions.assertEquals("Method name can't be null or be blank", e.getMessage());
                }

                Assertions.assertEquals("Bruce Eckel", bookHandle.call("getAuthor"));
                long accessTime1 = bookHandle.getLastAccessedTime();

                bookHandle.call("setAuthor", new Class[]{String.class}, new Object[]{"Bruce Eckel2"});
                long accessTime2 = bookHandle.getLastAccessedTime();

                Assertions.assertEquals("Bruce Eckel2", bookHandle.call("getAuthor"));
                long accessTime3 = bookHandle.getLastAccessedTime();

                Assertions.assertTrue(accessTime2 > 0);
                Assertions.assertTrue(accessTime1 >= accessTime0);
                Assertions.assertTrue(accessTime2 >= accessTime1);
                Assertions.assertTrue(accessTime3 >= accessTime2);
            }


        }
    }
}
