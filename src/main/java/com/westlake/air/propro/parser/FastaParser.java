package com.westlake.air.propro.parser;

import org.junit.Test;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * Created by Nico Wang Ruimin
 * Time: 2019-02-01 16:18
 * <p>
 * Fasta文件的存储格式为（一行Protein信息，跟着多行该protein的完整序列）
 */
@Component
public class FastaParser {
    public HashMap<String, HashSet<String>> parse(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            String lastProteinMessage = line;
            //设置初始容量为128，避免重新分配空间造成的性能损失
            StringBuilder lastSequence = new StringBuilder(128);
            HashMap<String, HashSet<String>> proteinPeptideMap = new HashMap<>();
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(">")) {
                    //进入下一个protein的分析
                    //此时lastSequence里面存有了当前protein的sequence
                    HashSet<String> enzymedSequence = getEnzymeResult(lastSequence.toString());
                    proteinPeptideMap.put(lastProteinMessage, enzymedSequence);

                    //分离为peptide之后，存储新protein的信息，并清空lastSequence
                    lastProteinMessage = line;
                    lastSequence.setLength(0);
                } else {
                    lastSequence.append(line);
                }
            }
            HashSet<String> enzymedSequence = getEnzymeResult(lastSequence.toString());
            proteinPeptideMap.put(lastProteinMessage, enzymedSequence);
            return proteinPeptideMap;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    /**
     * 将K，R作为切分sequence的标志物
     * @param proteinSequence
     * @return
     */
    private HashSet<String> getEnzymeResult(String proteinSequence) {
        String[] result = proteinSequence.replaceAll("K", "K|").replaceAll("R", "R|").split("\\|");
        HashSet<String> peptideSet = new HashSet<>();
        for(String peptide: result){
            if(peptide.length() >= 12 && peptide.length() <=22){
                peptideSet.add(peptide);
            }
        }
        return peptideSet;
    }

    @Test
    public void test(){
        long startTime = System.currentTimeMillis();
        File file = new File("D:\\data\\swissprot_human_20180209_target_IRT_contaminant.fasta");
        HashMap<String, HashSet<String>> result = parse(file);
        HashSet<String> uniquePeptide = getUniquePeptide(result);
        System.out.println("parseFinished.");
        System.out.println("time used: " + (System.currentTimeMillis() - startTime));
    }

    private HashSet<String> getUniquePeptide(HashMap<String,HashSet<String>> originMap){
        HashSet<String> uniquePeptides = new HashSet<>();
        HashSet<String> allPeptides = new HashSet<>();
        for (HashSet<String> peptideSet: originMap.values()){
            for (String peptide: peptideSet){
                if (allPeptides.contains(peptide) && uniquePeptides.contains(peptide)){
                    //若之前出现过，且在Unique中，移除
                    uniquePeptides.remove(peptide);
                }else {
                    //若之前没出现过，暂且放在Unique中
                    uniquePeptides.add(peptide);
                }
                allPeptides.add(peptide);
            }
        }
        return uniquePeptides;
    }
}
