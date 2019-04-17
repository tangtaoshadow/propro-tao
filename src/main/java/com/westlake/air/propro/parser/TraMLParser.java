package com.westlake.air.propro.parser;

import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.peptide.Annotation;
import com.westlake.air.propro.domain.db.FragmentInfo;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.PeptideDO;
import com.westlake.air.propro.domain.db.TaskDO;
import com.westlake.air.propro.parser.model.traml.*;
import com.westlake.air.propro.parser.xml.AirXStream;
import com.westlake.air.propro.service.TaskService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@Component("traMLParser")
public class TraMLParser extends BaseLibraryParser {

    @Autowired
    AirXStream airXStream;
    @Autowired
    TaskService taskService;

    public Class<?>[] classes = new Class[]{
            Compound.class, CompoundList.class, Configuration.class, Contact.class, Cv.class, CvParam.class,
            Evidence.class, Instrument.class, IntermediateProduct.class, Interpretation.class, Modification.class,
            Peptide.class, Precursor.class, Prediction.class, Product.class, Protein.class, ProteinRef.class,
            Publication.class, RetentionTime.class, com.westlake.air.propro.parser.model.traml.Software.class, SourceFile.class, Target.class, TargetList.class,
            TraML.class, Transition.class, UserParam.class, ValidationStatus.class
    };

    private void prepare(){
        airXStream.processAnnotations(classes);
        airXStream.allowTypes(classes);
    }

    public TraML parse(File file) {
        prepare();
        TraML traML = new TraML();
        airXStream.fromXML(file, traML);
        return traML;
    }

    public String parse(TraML traML){
        prepare();
        return airXStream.toXML(traML);
    }

    public TraML parse(InputStream ins) {
        prepare();
        TraML traML = new TraML();
        airXStream.fromXML(ins, traML);
        return traML;
    }

    public HashMap<String, Peptide> makePeptideMap(List<Peptide> peptideList) {
        HashMap<String, Peptide> peptideMap = new HashMap<>();
        for (Peptide peptide : peptideList) {
            peptideMap.put(peptide.getId(), peptide);
        }
        return peptideMap;
    }

    public ResultDO<PeptideDO> parseTransition(Transition transition, HashMap<String, Peptide> peptideMap, LibraryDO library) {
        ResultDO<PeptideDO> resultDO = new ResultDO<>(true);
        PeptideDO peptideDO = new PeptideDO();
        peptideDO.setLibraryId(library.getId());
        peptideDO.setLibraryName(library.getName());

        FragmentInfo fi = new FragmentInfo();

        // parse transition attribution
        boolean isDecoy = transition.getPeptideRef().toLowerCase().contains("decoy");
        peptideDO.setIsDecoy(isDecoy);
        // parse transition cvparams
        List<CvParam> listCvParams = transition.getCvParams();
        for (CvParam cvParam : listCvParams) {
            if (cvParam.getName().equals("product ion intensity")) {
                fi.setIntensity(Double.valueOf(cvParam.getValue()));
            } else if (cvParam.getName().equals("decoy SRM transition")) {
                peptideDO.setIsDecoy(true);
            } else if (cvParam.getName().equals("target SRM transition")) {
                peptideDO.setIsDecoy(false);
            }
        }
        // parse transition userparam
        List<UserParam> listUserParams = transition.getUserParams();
        if(listUserParams != null){
            for (UserParam userParam : listUserParams) {
                if (userParam.getName().equals("annotation")) {
                    fi.setAnnotations(userParam.getValue());
                }
            }
        }

        // parse precursor
        listCvParams = transition.getPrecursor().getCvParams();
        if(listCvParams != null){
            for (CvParam cvParam : listCvParams) {
                if (cvParam.getName().equals("isolation window target m/z")) {
                    peptideDO.setMz(Double.valueOf(cvParam.getValue()));
                }
                //charge state以Peptide部分带电量为准
                if (cvParam.getName().equals("charge state")) {
                    peptideDO.setCharge(Integer.valueOf(cvParam.getValue()));
                }
            }
        }

        // parse product cvParams
        listCvParams = transition.getProduct().getCvParams();
        if(listCvParams != null){
            for (CvParam cvParam : listCvParams) {
                if (cvParam.getName().equals("isolation window target m/z")) {
                    fi.setMz(Double.valueOf(cvParam.getValue()));
                }
                if (cvParam.getName().equals("charge state")) {
                    fi.setCharge(Integer.valueOf(cvParam.getValue()));
                }
            }
        }

        // parse rt, sequence, full name, protein name from peptideMap
        Peptide peptide = peptideMap.get(transition.getPeptideRef());
        String rt = peptide.getRetentionTimeList().get(0).getCvParams().get(0).getValue();
        peptideDO.setRt(Double.valueOf(rt));
        peptideDO.setSequence(peptide.getSequence());
        peptideDO.setProteinName(peptide.getProteinRefList().get(0).getRef().replace("DECOY_",""));
        peptideDO.setFullName(peptide.getUserParams().get(0).getValue());
        peptideDO.setTargetSequence(removeUnimod(peptideDO.getFullName()));
        for(CvParam cvParam : peptide.getCvParams()){
            if(cvParam.getName().equals("charge state")){
                peptideDO.setCharge(Integer.valueOf(cvParam.getValue()));
                peptideDO.setPeptideRef(peptideDO.getFullName()+"_"+ peptideDO.getCharge());
            }
        }

        // parse annotations
        String annotations = fi.getAnnotations();
        fi.setAnnotations(annotations);
        try {
            ResultDO<Annotation> annotationResult = parseAnnotation(fi.getAnnotations());
            Annotation annotation = annotationResult.getModel();
            fi.setAnnotation(annotation);
            fi.setCutInfo(annotation.toCutInfo());
            peptideDO.putFragment(fi.getCutInfo(), fi);
            resultDO.setModel(peptideDO);
        } catch (Exception e) {
            resultDO.setSuccess(false);
            resultDO.setMsgInfo("Line插入错误(Sequence未知)");
            logger.error(peptideDO.getLibraryId() + ":" + fi.getAnnotation(), e);
            return resultDO;
        }

        parseModification(peptideDO);

        return resultDO;
    }

    @Override
    public ResultDO parseAndInsert(InputStream in, LibraryDO library, HashSet<String> fastaUniqueSet, HashSet<String> prmPeptideRefSet, String libraryId, TaskDO taskDO) {
        TraML traML = parse(in);

        HashMap<String, Peptide> peptideMap = makePeptideMap(traML.getCompoundList().getPeptideList());
        ResultDO<List<PeptideDO>> tranResult = new ResultDO<>(true);

        try {
            //开始插入前先清空原有的数据库数据
            ResultDO resultDOTmp = peptideService.deleteAllByLibraryId(library.getId());
            logger.info("删除旧数据完毕");

            if (resultDOTmp.isFailed()) {
                logger.error(resultDOTmp.getMsgInfo());
                return ResultDO.buildError(ResultCode.DELETE_ERROR);
            }

            HashMap<String, PeptideDO> map = new HashMap<>();
            HashSet<String> fastaDropPep = new HashSet<>();
            HashSet<String> libraryDropPep = new HashSet<>();
            HashSet<String> fastaDropProt = new HashSet<>();
            HashSet<String> libraryDropProt = new HashSet<>();
            HashSet<String> uniqueProt = new HashSet<>();
            for (Transition transition : traML.getTransitionList()) {
                if(!prmPeptideRefSet.isEmpty() && !isPrmPeptideRef(transition, peptideMap, prmPeptideRefSet)){
                    continue;
                }
                ResultDO<PeptideDO> resultDO = parseTransition(transition, peptideMap, library);

                if (resultDO.isFailed()) {
                    tranResult.addErrorMsg(resultDO.getMsgInfo());
                    continue;
                }
                PeptideDO peptide = resultDO.getModel();
                setUnique(peptide, fastaUniqueSet, fastaDropPep, libraryDropPep, fastaDropProt, libraryDropProt, uniqueProt);
                addFragment(peptide, map);
            }
            for (PeptideDO peptideDO: map.values()){
                prmPeptideRefSet.remove(peptideDO.getPeptideRef());
            }
            library.setFastaDeWeightPepCount(fastaDropPep.size());
            library.setFastaDeWeightProtCount(getDropCount(fastaDropProt, uniqueProt));
            library.setLibraryDeWeightPepCount(libraryDropPep.size());
            library.setLibraryDeWeightProtCount(getDropCount(libraryDropProt, uniqueProt));
            if(!fastaUniqueSet.isEmpty()) {
                System.out.println("fasta额外检出：" + fastaDropPep.size() + "个Peptide");
            }
            ArrayList<PeptideDO> peptides = new ArrayList<PeptideDO>(map.values());
            if (libraryId != null && !libraryId.isEmpty()) {
                List<PeptideDO> irtPeps = peptideService.getAllByLibraryId(libraryId);
                for (PeptideDO peptideDO: irtPeps){
                    if (map.containsKey(peptideDO.getPeptideRef()+"_"+peptideDO.getIsDecoy())){
                        map.get(peptideDO.getPeptideRef()+"_"+peptideDO.getIsDecoy()).setProteinName("IRT");
                        continue;
                    }
                    if (prmPeptideRefSet.contains(peptideDO.getPeptideRef())){
                        prmPeptideRefSet.remove(peptideDO.getPeptideRef());
                    }
                    peptideDO.setProteinName("IRT");
                    peptideDO.setId(null);
                    peptideDO.setLibraryId(library.getId());
                    peptideDO.setLibraryName(library.getName());
                    peptides.add(peptideDO);
                }
            }
            peptideService.insertAll(peptides, false);
            tranResult.setModel(peptides);
            taskDO.addLog(peptides.size() + "条数据插入成功");
            taskService.update(taskDO);
            logger.info(peptides.size() + "条肽段数据插入成功,其中Unique蛋白质种类有" + uniqueProt.size() + "个");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tranResult;
    }

    private boolean isPrmPeptideRef(Transition transition, HashMap<String, Peptide> peptideMap, HashSet<String> peptideRefList){
        Peptide peptide = peptideMap.get(transition.getPeptideRef());
        String charge = "";
        for(CvParam cvParam : peptide.getCvParams()){
            if(cvParam.getName().equals("charge state")){
                charge = cvParam.getValue();
            }
        }
        String fullName = peptide.getUserParams().get(0).getValue();
        return peptideRefList.contains(fullName+"_"+charge);
    }


}
