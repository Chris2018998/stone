package org.stone.beeop.objectsource;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.objects.InterruptionAction;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.objects.JavaBookFactory;
import org.stone.beeop.objects.MockBlockPoolImplementation1;
import org.stone.beeop.objects.MockBlockPoolImplementation2;
import org.stone.beeop.objects.ObjectBorrowThread;

import java.util.concurrent.TimeUnit;

import static org.stone.base.TestUtil.waitUtilWaiting;

public class Tc0031ObjectSourceLockTest extends TestCase {

    public void testWaitTimeoutOnDsRLock() throws Exception {
        BeeObjectSource os = new BeeObjectSource();
        os.setObjectFactory(new JavaBookFactory());
        os.setMaxWait(TimeUnit.MILLISECONDS.toMillis(500L));//timeout on wait
        os.setPoolImplementClassName(MockBlockPoolImplementation1.class.getName());

        ObjectBorrowThread firstThread = new ObjectBorrowThread(os);
        firstThread.start();

        if (waitUtilWaiting(firstThread)) {
            ObjectBorrowThread secondThread = new ObjectBorrowThread(os);
            secondThread.start();
            secondThread.join();
            Assert.assertEquals("Timeout on waiting for pool ready", secondThread.getFailureCause().getMessage());
        }
    }

    public void testInterruptionOnDsRLock() throws Exception {
        BeeObjectSource os = new BeeObjectSource();
        os.setObjectFactory(new JavaBookFactory());
        os.setPoolImplementClassName(MockBlockPoolImplementation1.class.getName());

        ObjectBorrowThread firstThread = new ObjectBorrowThread(os);
        firstThread.start();

        if (waitUtilWaiting(firstThread)) {
            ObjectBorrowThread secondThread = new ObjectBorrowThread(os);
            secondThread.start();
            new InterruptionAction(secondThread).start();
            secondThread.join();
            Assert.assertEquals("An interruption occurred while waiting for pool ready", secondThread.getFailureCause().getMessage());
        }
    }

    public void testSuccessOnRLock() throws Exception {
        BeeObjectSource os = new BeeObjectSource();
        os.setObjectFactory(new JavaBookFactory());
        os.setPoolImplementClassName(MockBlockPoolImplementation2.class.getName());
        os.setMaxWait(TimeUnit.SECONDS.toMillis(10L));//timeout on wait

        ObjectBorrowThread firstThread = new ObjectBorrowThread(os);
        ObjectBorrowThread secondThread = new ObjectBorrowThread(os);
        firstThread.start();

        if (waitUtilWaiting(firstThread)) {//block 1 second in pool instance creation
            secondThread.start();
            secondThread.join();
            Assert.assertNull(secondThread.getFailureCause());
            Assert.assertNotNull(secondThread.getObjectHandle());
        }
    }

    public void testSuccessOnRLock2() throws Exception {
        BeeObjectSource os = new BeeObjectSource();
        JavaBookFactory objectKey = new JavaBookFactory();

        os.setObjectFactory(new JavaBookFactory());
        os.setMaxWait(TimeUnit.SECONDS.toMillis(10L));//timeout on wait
        os.setPoolImplementClassName(MockBlockPoolImplementation2.class.getName());

        ObjectBorrowThread firstThread = new ObjectBorrowThread(os, null, objectKey);
        ObjectBorrowThread secondThread = new ObjectBorrowThread(os, null, objectKey);
        firstThread.start();

        if (waitUtilWaiting(firstThread)) {//block 1 second in pool instance creation
            secondThread.start();
            secondThread.join();
            Assert.assertNull(secondThread.getFailureCause());
            Assert.assertNotNull(secondThread.getObjectHandle());
        }
    }
}
