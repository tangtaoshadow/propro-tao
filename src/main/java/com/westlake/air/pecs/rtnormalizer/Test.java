package com.westlake.air.pecs.rtnormalizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Test {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int rounds = sc.nextInt();
        for(int i=0; i<rounds; i++){
            float result;
            float number = sc.nextInt();
            if(number <= 5000){
                System.out.println("0");
            }else {
                number -= 5000;
                if(number <= 3000){
                    result = number * 0.03f;
                }else if(number <= 12000){
                    result = 3000 * 0.03f + (number - 3000) * 0.1f;
                }else if(number <= 25000){
                    result = 3000 * 0.03f + 9000 * 0.1f + (number - 12000) * 0.2f;
                }else if(number <= 35000){
                    result = 3000 * 0.03f + 9000 * 0.1f + 13000 * 0.2f + (number - 25000) * 0.25f;
                }else if(number <= 55000){
                    result = 3000 * 0.03f + 9000 * 0.1f + 13000 * 0.2f + 10000 * 0.25f + (number - 35000) * 0.3f;
                }else if(number <= 80000){
                    result = 3000 * 0.03f + 9000 * 0.1f + 13000 * 0.2f + 10000 * 0.25f + 20000 * 0.3f + (number - 55000) * 0.35f;
                }else {
                    result = 3000 * 0.03f + 9000 * 0.1f + 13000 * 0.2f + 10000 * 0.25f + 20000 * 0.3f + 25000 * 0.35f + (number - 80000) * 0.45f;
                }
                float res = result - (int)result;
                if(res>=0.5){
                    System.out.println((int)result + 1);
                }else {
                    System.out.println((int)result);
                }
            }
        }

    }
}



// Scanner sc = new Scanner(System.in);
//         System.out.println("请输入你的姓名：");
//         String name = sc.nextLine();
//
//    int rounds = sc.nextInt();
//    int[] output = new int[rounds];
//        for(int i=0; i<rounds; i++){
//        int rows = sc.nextInt();
//        int columns = sc.nextInt();
//        sc.nextLine();
//        char[][] matrix = new char[rows][columns];
//        for(int j=0; j<rows; j++){
//        matrix[j] = sc.nextLine().toCharArray();
//        }
//        char[] target = sc.nextLine().toCharArray();
//
//        for(int height=0; height<rows; height++){
//        for(int width = 0; width < columns; width++){
//        if(matrix[height][width] == target[0]){
//        boolean match = false;
//        if(width + target.length-1 < columns){
//        match = true;
//        for(int k=1; k<target.length; k++){
//        if(!(matrix[height][width+k] == target[k])){
//        match = false;
//        }
//        }
//        if(match){
//        output[i]++;
//        }
//        }
//        if(height + target.length -1 < rows){
//        match = true;
//        for(int k=1; k<target.length; k++){
//        if(!(matrix[height + k][width] == target[k])){
//        match = false;
//        }
//        }
//        if(match){
//        output[i]++;
//        }
//        }
//        if(width + target.length-1 < columns && height + target.length -1 < rows){
//        match = true;
//        for(int k=1; k<target.length; k++){
//        if(!(matrix[height + k][width + k] == target[k])){
//        match = false;
//        }
//        }
//        if(match){
//        output[i]++;
//        }
//        }
//        }
//        }
//        }
//        System.out.println(output[i]);
//        }