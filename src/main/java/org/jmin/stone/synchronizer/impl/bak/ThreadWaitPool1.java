///*
// * Copyright(C) Chris2018998(cn)
// *
// * Contact:Chris2018998@tom.com
// *
// * Licensed under GNU Lesser General Public License v2.1
// */
//package org.jmin.stone.synchronizer.impl.bak;
//
//import org.jmin.stone.synchronizer.impl.ThreadNode;
//import org.jmin.stone.synchronizer.impl.ThreadParkerFactory;
//
//import java.util.*;
//import java.util.concurrent.ConcurrentLinkedDeque;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.TimeoutException;
//import java.util.concurrent.locks.LockSupport;
//
//import static org.jmin.stone.synchronizer.impl.ThreadNodeState.*;
//import static org.jmin.stone.synchronizer.impl.ThreadNodeUpdater.casNodeState;
//
///**
// * Wait Pool,threads will leave from pool under three situation
// * 1: wakeup by other thread
// * 2: wait timeout
// * 3: wait interrupted
// *
// * @author Chris Liao
// * @version 1.0
// */
//public class ThreadWaitPool1 {
//
//    //temp util ThreadNodeChain is stable
//    private ConcurrentLinkedDeque<ThreadNode> waitQueue = new ConcurrentLinkedDeque<>();
//
//    //****************************************************************************************************************//
//    //                                          1:Sleep Methods(return state)                                         //
//    //****************************************************************************************************************//
//    //1.1: Uninterruptibly util wakeup
//    public Object sleepUninterruptibly(Object type) {
//        try {
//            ThreadParkerFactory.ThreadParker parker = ThreadParkerFactory.create(0, false);
//            parker.setAutoClearInterruptedInd(true);
//            return doSleep(type, parker);
//        } catch (TimeoutException e) {
//            //in fact,TimeoutException never be thrown out here
//            return null;
//        } catch (InterruptedException e) {
//            //in fact,InterruptedException never be thrown out here
//            return null;
//        }
//    }
//
//    //1.2: sleep util wakeup or interrupted
//    public Object sleep(Object type) throws InterruptedException {
//        try {
//            return doSleep(type, ThreadParkerFactory.create(0, false));
//        } catch (TimeoutException e) {
//            //in fact,TimeoutException never be thrown out here
//            return null;
//        }
//    }
//
//    //1.3: sleep util time point or wakeup or interrupted
//    public Object sleep(Object type, Date utilDate) throws InterruptedException, TimeoutException {
//        return doSleep(type, ThreadParkerFactory.create(utilDate.getTime(), true));
//    }
//
//    //1.4: sleep time out elapsed or wakeup or interrupted
//    public Object sleep(Object type, long timeOut, TimeUnit unit) throws InterruptedException, TimeoutException {
//        return doSleep(type, ThreadParkerFactory.create(unit.toNanos(timeOut), false));
//    }
//
//    //sleep implement method
//    private Object doSleep(Object type, ThreadParkerFactory.ThreadParker parker) throws InterruptedException, TimeoutException {
//        //1:create node and add to queue
//        ThreadNode node = new ThreadNode();
//        node.setValue(type);
//        waitQueue.offer(node);
//
//        //2: get interruptable indicator
//        boolean allowInterruptable = parker.allowInterruptable();
//
//        //3: spin control
//        while (true) {
//            boolean interrupted = parker.park();//maybe clear interrupted flag here
//            Object currentState = node.getState();//waken up
//
//            if (currentState == WAITING) {//timeout or interrupted
//                if (interrupted) {
//                    if (allowInterruptable) {
//                        node.setState(INTERRUPTED);
//                        waitQueue.remove(node);
//                        throw new InterruptedException();
//                    }
//                } else if (casNodeState(node, WAITING, TIMEOUT)) {
//                    waitQueue.remove(node);
//                    throw new TimeoutException();
//                } else {
//                    return node.getState();
//                }
//            } else {
//                return currentState;
//            }
//        }
//    }
//
//    //****************************************************************************************************************//
//    //                                          2: Wake one Methods(return true when success                          //
//    //****************************************************************************************************************//
//    //WAITING ---> NOTIFIED
//    public boolean wakeOne() {
//        return wakeOne(true);
//    }
//
//    //WAITING ---> NOTIFIED
//    public boolean wakeOne(boolean fromHead) {
//        ThreadNode node;
//        if (fromHead) {
//            while ((node = waitQueue.pollFirst()) != null) {
//                if (casNodeState(node, WAITING, NOTIFIED)) {
//                    if (!node.getThread().isInterrupted()) {
//                        LockSupport.unpark(node.getThread());
//                        return true;
//                    }
//                }
//            }
//        } else {
//            while ((node = waitQueue.pollLast()) != null) {
//                if (casNodeState(node, WAITING, NOTIFIED)) {
//                    if (!node.getThread().isInterrupted()) {
//                        LockSupport.unpark(node.getThread());
//                        return true;
//                    }
//                }
//            }
//        }
//        return false;
//    }
//
//    //WAITING ---> NOTIFIED
//    public boolean wakeOneByType(Object type) {
//        return wakeOneByType(type, true);
//    }
//
//    //WAITING ---> NOTIFIED
//    public boolean wakeOneByType(Object type, boolean fromHead) {
//        ThreadNode node;
//        Iterator<ThreadNode> iterator = fromHead ? waitQueue.iterator() : waitQueue.descendingIterator();
//        while (iterator.hasNext()) {
//            node = iterator.next();
//            if (type.equals(node.getValue()) && casNodeState(node, WAITING, NOTIFIED)) {
//                if (!node.getThread().isInterrupted()) {
//                    LockSupport.unpark(node.getThread());
//                    iterator.remove();
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
//
//    //WAITING ---> updType
//    public boolean casWakeOneByType(Object type, Object updType) {
//        return casWakeOneByType(type, updType, true);
//    }
//
//    //WAITING ---> updType
//    public boolean casWakeOneByType(Object type, Object updType, boolean fromHead) {
//        ThreadNode node;
//        Iterator<ThreadNode> iterator = fromHead ? waitQueue.iterator() : waitQueue.descendingIterator();
//        while (iterator.hasNext()) {
//            node = iterator.next();
//            if (type.equals(node.getValue()) && casNodeState(node, WAITING, updType)) {
//                if (!node.getThread().isInterrupted()) {
//                    LockSupport.unpark(node.getThread());
//                    iterator.remove();
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
//
//    //****************************************************************************************************************//
//    //                                          3: Wake some Methods(return number of wakeup count)                   //
//    //****************************************************************************************************************//
//    //WAITING ---> NOTIFIED
//    public int wakeAll() {
//        int count = 0;
//        ThreadNode node;
//        while ((node = waitQueue.poll()) != null) {
//            if (casNodeState(node, WAITING, NOTIFIED)) {
//                if (!node.getThread().isInterrupted()) {
//                    LockSupport.unpark(node.getThread());
//                    count++;
//                }
//            }
//        }
//        return count;
//    }
//
//    //WAITING ---> NOTIFIED
//    public int wakeByType(Object type) {
//        int count = 0;
//        ThreadNode node;
//        Iterator<ThreadNode> iterator = waitQueue.iterator();
//        while (iterator.hasNext()) {
//            node = iterator.next();
//            if (type.equals(node.getValue()) && casNodeState(node, WAITING, NOTIFIED)) {
//                if (!node.getThread().isInterrupted()) {
//                    LockSupport.unpark(node.getThread());
//                    count++;
//                }
//            }
//        }
//        return count;
//    }
//
//    //WAITING ---> NOTIFIED
//    public int wakeByTypes(Collection typeList) {
//        int count = 0;
//        ThreadNode node;
//        Iterator<ThreadNode> iterator = waitQueue.iterator();
//        while (iterator.hasNext()) {
//            node = iterator.next();
//            if (typeList.contains(node.getValue()) && casNodeState(node, WAITING, NOTIFIED)) {
//                if (!node.getThread().isInterrupted()) {
//                    LockSupport.unpark(node.getThread());
//                    count++;
//                }
//            }
//        }
//        return count;
//    }
//
//    //WAITING ---> updType
//    public int casWakeByType(Object type, Object updType) {
//        int count = 0;
//        ThreadNode node;
//        Iterator<ThreadNode> iterator = waitQueue.iterator();
//        while (iterator.hasNext()) {
//            node = iterator.next();
//            if (type.equals(node.getValue()) && casNodeState(node, WAITING, updType)) {
//                if (!node.getThread().isInterrupted()) {
//                    LockSupport.unpark(node.getThread());
//                    count++;
//                }
//            }
//        }
//        return count;
//    }
//
//    //****************************************************************************************************************//
//    //                                          4: Monitor Methods(State == WAITING )                                 //
//    //****************************************************************************************************************//
//    protected final int getQueueLength() {
//        int count = 0;
//        Iterator<ThreadNode> iterator = waitQueue.iterator();
//        while (iterator.hasNext()) {
//            ThreadNode node = iterator.next();
//            if (node.getState() == WAITING) count++;
//        }
//        return count;
//    }
//
//    protected Collection<Thread> getQueuedThreads() {
//        List<Thread> threadList = new LinkedList<>();
//        Iterator<ThreadNode> iterator = waitQueue.iterator();
//        while (iterator.hasNext()) {
//            ThreadNode node = iterator.next();
//            if (node.getState() == WAITING) threadList.add(node.getThread());
//        }
//        return threadList;
//    }
//
//    protected final int getQueueLength(Object type) {
//        int count = 0;
//        Iterator<ThreadNode> iterator = waitQueue.iterator();
//        while (iterator.hasNext()) {
//            ThreadNode node = iterator.next();
//            if (type.equals(node.getValue()) && node.getState() == WAITING) count++;
//        }
//        return count;
//    }
//
//    protected Collection<Thread> getQueuedThreads(Object type) {
//        List<Thread> threadList = new LinkedList<>();
//        Iterator<ThreadNode> iterator = waitQueue.iterator();
//        while (iterator.hasNext()) {
//            ThreadNode node = iterator.next();
//            if (type.equals(node.getValue()) && node.getState() == WAITING) threadList.add(node.getThread());
//        }
//        return threadList;
//    }
//}
//
//
//
