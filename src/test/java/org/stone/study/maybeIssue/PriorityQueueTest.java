/*
 * Copyright Chris2018998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.stone.study;

import java.util.PriorityQueue;

/**
 * PriorityQueue Test
 *
 * @author Chris Liao
 */
public class PriorityQueueTest {

    public static void main(String[] args) {
        PriorityQueue<Integer> q3 = new PriorityQueue<>(5);
        q3.offer(new Integer(3));
        q3.offer(new Integer(2));
        q3.offer(new Integer(1));

        PriorityQueue<Integer> q4 = new PriorityQueue<>(5);
        q4.offer(new Integer(4));
        q4.offer(new Integer(3));
        q4.offer(new Integer(2));
        q4.offer(new Integer(1));

        System.out.println("q3:" + q3);
        //print: q3:[1, 3, 2] why not be [1, 2, 3]?
        System.out.println("q4:" + q4);
        //print: q4:[1, 2, 3, 4]
    }
}
