package org.stone.beeop.objectsource;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.objects.InterruptionAction;
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.objects.JavaBookFactory;
import org.stone.beeop.objects.MockBlockPoolImplementation1;
import org.stone.beeop.objects.MockBlockPoolImplementation2;
import org.stone.beeop.objects.ObjectBorrowThread;
import org.stone.beeop.pool.ObjectPoolStatics;
import org.stone.beeop.pool.exception.ObjectGetInterruptedException;
import org.stone.beeop.pool.exception.ObjectGetTimeoutException;

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
            try {
                os.getObjectHandle();//second thread will be locked on read-lock
                Assert.fail("Os Lock timeout test failed");
            } catch (ObjectGetTimeoutException e) {
                Assert.assertTrue(e.getMessage().contains("Timeout on waiting for pool ready"));
            }
        }
    }

    public void testInterruptionOnDsRLock() throws Exception {
        BeeObjectSource os = new BeeObjectSource();
        os.setObjectFactory(new JavaBookFactory());
        os.setPoolImplementClassName(MockBlockPoolImplementation1.class.getName());

        ObjectBorrowThread firstThread = new ObjectBorrowThread(os);
        firstThread.start();

        if (waitUtilWaiting(firstThread)) {
            new InterruptionAction(Thread.currentThread()).start();

            try {
                os.getObjectHandle();
                Assert.fail("Ds Lock interruption test failed");
            } catch (ObjectGetInterruptedException e) {
                Assert.assertTrue(e.getMessage().contains("An interruption occurred while waiting for pool ready"));
            }
        }
    }

    public void testSuccessOnRLock() throws Exception {
        BeeObjectSource os = new BeeObjectSource();
        os.setObjectFactory(new JavaBookFactory());
        os.setPoolImplementClassName(MockBlockPoolImplementation2.class.getName());
        os.setMaxWait(TimeUnit.SECONDS.toMillis(10L));//timeout on wait

        ObjectBorrowThread firstThread = new ObjectBorrowThread(os);
        firstThread.start();

        if (waitUtilWaiting(firstThread)) {//block 1 second in pool instance creation
            BeeObjectHandle handle = null;
            try {
                handle = os.getObjectHandle();
            } catch (ObjectGetTimeoutException e) {
                fail("test failed on testSuccessOnRLock");
            } finally {
                if (handle != null) ObjectPoolStatics.oclose(handle);
                os.close();
            }
        }
    }

    public void testSuccessOnRLock2() throws Exception {
        BeeObjectSource os = new BeeObjectSource();
        os.setObjectFactory(new JavaBookFactory());
        os.setMaxWait(TimeUnit.SECONDS.toMillis(10L));//timeout on wait
        os.setPoolImplementClassName(MockBlockPoolImplementation2.class.getName());

        ObjectBorrowThread firstThread = new ObjectBorrowThread(os);
        firstThread.start();

        if (waitUtilWaiting(firstThread)) {//block 1 second in pool instance creation
            BeeObjectHandle handle = null;
            try {
                handle = os.getObjectHandle();
            } catch (ObjectGetTimeoutException e) {
                fail("test failed on testSuccessOnRLock");
            } finally {
                if (handle != null) ObjectPoolStatics.oclose(handle);
                os.close();
            }
        }
    }
}
