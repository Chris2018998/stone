package org.stone.test.study;

import static org.stone.tools.CommonUtil.*;

public class IntegerBitTest {
    public static void main(String[]args){
        String byte32="01111111111111111111111111111111";
        int state =Integer.parseInt(byte32,2);
        int low = low16(state);
        int high = high16(state);
        System.out.println("int:" + state);
        System.out.println("low bit:" + low);
        System.out.println("high bit:" + high);
        int newVal = contact(high,low);
        System.out.println("newVal:" + newVal);
    }
}
