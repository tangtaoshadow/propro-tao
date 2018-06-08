package com.westlake.air.swathplatform.parser;

import com.westlake.air.swathplatform.domain.db.TransitionDO;
import com.westlake.air.swathplatform.parser.model.traml.Precursor;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-07 11:07
 */
@Component
public class TsvParser {

    private static String PrecursorMz = "precursormz";
    private static String ProductMz = "productmz";
    private static String NormalizedRetentionTime = "tr_recalibrated";
    private static String TransitionName = "transition_name";
    private static String IsDecoy = "decoy";
    private static String ProductIonIntensity = "libraryintensity";
    private static String PeptideSequence = "peptidesequence";
    private static String ProteinName = "proteinname";
    private static String Annotation = "annotation";
    private static String FullUniModPeptideName = "fullunimodpeptidename";
    private static String PrecursorCharge = "precursorcharge";
    private static String PeptideGroupLabel = "peptidegrouplabel";
    private static String FragmentType = "fragmenttype";
    private static String FragmentCharge = "fragmentcharge";
    private static String FragmentSeriesNumber = "fragmentseriesnumber";

    public List<TransitionDO> parse(InputStream in,String libraryId) {
        List<TransitionDO> transitions = new ArrayList<>();

        try {
            InputStreamReader isr = new InputStreamReader(in, "UTF-8");
            BufferedReader reader = new BufferedReader(isr);
            String line = reader.readLine();
            HashMap<String, Integer> columnMap = parseColumns(line);

            while((line=reader.readLine())!=null){
                transitions.add(parseTransition(line, columnMap,libraryId));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return transitions;
    }

    private HashMap<String, Integer> parseColumns(String line) {
        String[] columns = line.split("\t");
        HashMap<String, Integer> columnMap = new HashMap<>();
        for (int i = 0; i < columns.length; i++) {
            columnMap.put(columns[i].toLowerCase(),i);
        }
        return columnMap;
    }

    private TransitionDO parseTransition(String line,HashMap<String, Integer> columnMap,String libraryId){
        String[] row = line.split("\t");
        TransitionDO transitionDO = new TransitionDO();
        transitionDO.setLibraryId(libraryId);
        transitionDO.setPrecursorMz(row[columnMap.get(PrecursorMz)]);
        transitionDO.setProductMz(row[columnMap.get(ProductMz)]);
        transitionDO.setNormalizedRetentionTime(row[columnMap.get(NormalizedRetentionTime)]);
        transitionDO.setTransitionName(row[columnMap.get(TransitionName)]);
        transitionDO.setIsDecoy(!row[columnMap.get(IsDecoy)].equals("0"));
        transitionDO.setProductIonIntensity(row[columnMap.get(ProductIonIntensity)]);
        transitionDO.setPeptideSequence(row[columnMap.get(PeptideSequence)]);
        transitionDO.setProteinName(row[columnMap.get(ProteinName)]);
        transitionDO.setAnnotation(row[columnMap.get(Annotation)]);
        transitionDO.setFullUniModPeptideName(row[columnMap.get(FullUniModPeptideName)]);
        transitionDO.setPrecursorCharge(Integer.parseInt(row[columnMap.get(PrecursorCharge)]));
        transitionDO.setPeptideGroupLabel(row[columnMap.get(PeptideGroupLabel)]);
        transitionDO.setFragmentType(row[columnMap.get(FragmentType)]);
        transitionDO.setFragmentCharge(Integer.parseInt(row[columnMap.get(FragmentCharge)]));
        transitionDO.setFragmentSeriesNumber(Integer.parseInt(row[columnMap.get(FragmentSeriesNumber)]));
        return transitionDO;
    }
}
