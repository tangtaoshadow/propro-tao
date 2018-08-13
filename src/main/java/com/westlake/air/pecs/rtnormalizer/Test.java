package com.westlake.air.pecs.rtnormalizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Test {
    public static void main(String[] args) {
        List<Float> testList = new ArrayList<>();
        testList.add(2f);
        testList.add(3f);
        testList.add(4f);

        testList.set(1, 5f);
        for(float i: testList){
            System.out.println(i);
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