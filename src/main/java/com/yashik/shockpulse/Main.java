package com.yashik.shockpulse;

import com.yashik.shockpulse.model.Product;

public class Main {
    public static final String GREEN = "\u001B[32m";
    public static final String CYAN = "\u001B[36m";
    public static final String RESET = "\u001B[0m";

    public static void main(String[] args) {
        System.out.println(CYAN + "======================================================" + RESET);
        System.out.println(CYAN + "   SHOCKPULSE: FLASH-SALE & INVENTORY ENGINE v1.0     " + RESET);
        System.out.println(CYAN + "======================================================" + RESET);

        Product sample = new Product("SP-101", "Mechanical Keyboard", "Peripherals", 2499.00, 5);
        System.out.println(GREEN + "Boot check passed! Loaded sample product:" + RESET);
        System.out.println(sample);
    }
}
