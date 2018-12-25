package com.westlake.air.pecs.parser;

import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.peptide.Annotation;
import com.westlake.air.pecs.domain.db.FragmentInfo;
import com.westlake.air.pecs.domain.db.LibraryDO;
import com.westlake.air.pecs.domain.db.PeptideDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.parser.model.traml.*;
import com.westlake.air.pecs.parser.xml.AirXStream;
import com.westlake.air.pecs.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
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
            Publication.class, RetentionTime.class, com.westlake.air.pecs.parser.model.traml.Software.class, SourceFile.class, Target.class, TargetList.class,
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
        peptideDO.setProteinName(peptide.getProteinRefList().get(0).getRef());
        peptideDO.setFullName(peptide.getUserParams().get(0).getValue());
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
    public ResultDO parseAndInsert(InputStream in, LibraryDO library, TaskDO taskDO) {
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
            for (Transition transition : traML.getTransitionList()) {
                ResultDO<PeptideDO> resultDO = parseTransition(transition, peptideMap, library);

                if (resultDO.isFailed()) {
                    tranResult.addErrorMsg(resultDO.getMsgInfo());
                    continue;
                }

                PeptideDO peptide = resultDO.getModel();
                PeptideDO existedPeptide = map.get(peptide.getPeptideRef()+"_"+peptide.getIsDecoy());
                if(existedPeptide == null){
                    map.put(peptide.getPeptideRef()+"_"+peptide.getIsDecoy(), peptide);
                }else{
                    for (String key : peptide.getFragmentMap().keySet()) {
                        existedPeptide.putFragment(key, peptide.getFragmentMap().get(key));
                    }
                }
            }
            ArrayList<PeptideDO> totalList = new ArrayList<PeptideDO>(map.values());
            int decoyCount = 0;
            int targetCount = 0;
            for(PeptideDO pt : totalList){
                if(pt.getIsDecoy()){
                    decoyCount++;
                }else{
                    targetCount++;
                }
            }
            logger.info("伪肽段:"+decoyCount);
            logger.info("真实肽段:"+targetCount);
            peptideService.insertAll(totalList, false);
            tranResult.setModel(totalList);
            taskDO.addLog(totalList.size() + "条数据插入成功");
            taskService.update(taskDO);
            logger.info(map.values().size() + "条数据插入成功");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return tranResult;
    }
}
