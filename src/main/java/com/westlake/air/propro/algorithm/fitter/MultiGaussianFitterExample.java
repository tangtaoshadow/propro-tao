package com.westlake.air.propro.algorithm.fitter;

import com.alibaba.fastjson.JSON;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nico Wang
 * Time: 2018-12-19 19:40
 */
public class MultiGaussianFitterExample {
        public static void main(String args[]){
            double[] time = new double[]{440,442.255,442.26,442.265,442.27,442.275,442.28,442.285,442.29,442.295,442.3,442.306,442.311,442.316};
            double[] intensity = new double[]{12000,6879.5,20147.9,37398.6,73930.4 ,119117,156101.7,131480.3,78947.5,27729.5,1630.5,2305.9,4962.2,3866.3};
            // Collect data.
            final WeightedObservedPoints obs = new WeightedObservedPoints();
            for(int i=0; i<time.length; i++){
                obs.add(time[i], intensity[i]);
            }

            final MultiGaussianFitter fitter = MultiGaussianFitter.create();

            long startTime = System.currentTimeMillis();
            final double[] coeff = fitter.fit(obs.toList());
            System.out.println("time:" + (System.currentTimeMillis() - startTime));

            List<Double> mean = new ArrayList<>();
            List<Double> norm = new ArrayList<>();
            List<Double> sigma = new ArrayList<>();
            for(int i=0; i<coeff.length/3; i++){
                norm.add(coeff[3 * i]);
                mean.add(coeff[3 * i + 1]);
                sigma.add(coeff[3 * i + 2]);
            }
            System.out.println("norm: "+ JSON.toJSON(norm));
            System.out.println("mean: "+ JSON.toJSON(mean));
            System.out.println("sigma: "+ JSON.toJSON(sigma));
            for(int i=0; i<coeff.length/3; i++){
                for(WeightedObservedPoint point: obs.toList()){
                    System.out.println(coeff[3 * i] * Math.exp(-Math.pow(point.getX()-coeff[3 * i + 1],2)/(2*coeff[3 * i + 2] *coeff[3 * i + 2])));
                }
                System.out.println("");
            }
            System.out.println(JSON.toJSON(coeff));
        }
//    @Test
//    public void MultiGaussianFitterExample(){
//        int step = 1;
//        String line = getFullLine(10);
//        List<String> shapeList = printShape(line, step);
//        printAll(1,5,1,shapeList);
//    }
//
//    private void printAll(int startWidth, int endWidth, int step, List<String> shapeList){
//        int lengthForOne = shapeList.get(0).length();
//        String voidLine = "";
//        for (int i=0; i<lengthForOne; i++){
//            voidLine += " ";
//        }
//        List<String> voidList = new ArrayList<>();
//        for (int i=0; i<shapeList.size(); i++){
//            voidList.add(voidLine);
//        }
//        List<String> finalResult = new ArrayList<>();
//        int endHalfCount = endWidth/2 + endWidth%2;
//        for (int i= startWidth; i<endWidth; i+=step * 2){
//            int startHalfCount = i/2 + i%2;
//            printLargeLine(i, startHalfCount, endHalfCount, step, voidList, shapeList);
//        }
//        for (int i= endWidth; i>=startWidth; i-=step * 2){
//            int startHalfCount = i/2 + i%2;
//            printLargeLine(i, startHalfCount, endHalfCount, step, voidList, shapeList);
//        }
//
//    }
//
//    private void printLargeLine(int startWidth, int startHalfCount, int endHalfCount, int step, List<String> voidList, List<String> shapeList){
//        List<List<String>> largeList = new ArrayList<>();
//        for (int j=endHalfCount-startHalfCount; j>0; j-=step){
//            largeList.add(voidList);
//        }
//        for (int j=0; j<startWidth; j++){
//            largeList.add(shapeList);
//        }
//        for (int j=endHalfCount-startHalfCount; j>0; j-=step){
//            largeList.add(voidList);
//        }
//        String[] tempList = combineList(largeList);
//        printList(tempList);
//    }
//
//    private void printList(String[] list){
//        for (String line: list){
//            System.out.println(line);
//        }
//    }
//    private String[] combineList(List<List<String>> largeList){
//        String[] result = new String[largeList.get(0).size()];
//        for (int i=0; i<largeList.size(); i++){
//            List<String> list = largeList.get(i);
//            for (int j=0; j<list.size(); j++){
//                if (result[j] == null){
//                    result[j] = "";
//                }
//                result[j] += list.get(j);
//            }
//        }
//        return result;
//    }
//
//    private List<String> printShape(String line, int step){
//        List<String> output = new ArrayList<>();
//        int downSide = 1;
//        int upSide = line.length()/2-(1-line.length()%2);
//        while (upSide >0){
//            output.add(getLine(line, upSide, true));
//            upSide -= step;
//        }
//        output.add(line);
//        while (line.length() - 2*downSide >=(2-line.length()%2)){
//            output.add(getLine(line, downSide, true));
//            downSide += step;
//        }
//        return output;
//    }
//
//    private String getFullLine(int length){
//        String line = "1234567654321";
////        for (int i=0; i<length; i++){
////            line += 2 * i + 1;
////        }
//        return line;
//    }
//
//    private String getLine(String line, int step, boolean isMid){
//        String newLine = "";
//        for (int i=0 ; i<step; i++){
//            newLine += " ";
//        }
//        if (isMid){
//            newLine += cutFromMid(line, step);
//        }else {
//            newLine += cutFromOut(line, step);
//        }
//        for (int i=0 ; i<step; i++){
//            newLine += " ";
//        }
//        return newLine;
//    }
//
//    //step >=1
//    private String cutFromMid(String line, int step){
//        String newLine = "";
//        int length = line.length();
//        if (length%2 == 0) {
//            //even
//            //mid position
//            int midIndexLeft = length/2 - 1;
//            int midIndexRight = midIndexLeft + 1;
//            for (int i=0; i<= midIndexLeft - step; i++){
//                newLine += line.toCharArray()[i];
//            }
//            for (int i= midIndexRight + step; i < length; i++){
//                newLine += line.toCharArray()[i];
//            }
//        }else {
//            //odd
//            int midIndex = length / 2;
//            for (int i=0; i<midIndex - step + 1; i++){
//                newLine += line.toCharArray()[i];
//            }
//            for (int i= midIndex + step + 1; i<length; i++){
//                newLine += line.toCharArray()[i];
//            }
//        }
//        return newLine;
//    }
//
//    private String cutFromOut(String line, int step){
//        String newLine = "";
//        for (int i=step; i<line.length()-step; i++){
//            newLine += line.toCharArray()[i];
//        }
//        return newLine;
//    }
}





