package com.senghol.knotx.utils;

import android.graphics.Color;

import java.util.LinkedList;
import java.util.Random;

public class RandomColorGenerator {
    private static final int[] COLORS = {

            Color.parseColor("#f94144"), // Light teal
            Color.parseColor("#f3722c"), // Yellowish
            Color.parseColor("#0d3082"), // Light orange
            Color.parseColor("#f9c74f"), // Reddish
            Color.parseColor("#90be6d"), // Light purple
            Color.parseColor("#43aa8b"),
            Color.parseColor("#577590"),

//            Color.parseColor("#361c0e"),
//            Color.parseColor("#570211"),
//            Color.parseColor("#7e3110"),
//            Color.parseColor("#004540"),
//            Color.parseColor("#032c4d"),
//            Color.parseColor("#360825"),
    };

    private static final LinkedList<Integer> recentColors = new LinkedList<>();

    public static int getRandomColor() {

        Random random = new Random();
        int color;

        do {
            color = COLORS[random.nextInt(COLORS.length)];
        } while (recentColors.contains(color));

        // Maintain the last 3 used colors
        recentColors.addLast(color);
        if (recentColors.size() > 3) {
            recentColors.removeFirst();
        }

        return color;
    }
}
