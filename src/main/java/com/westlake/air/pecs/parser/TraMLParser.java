package com.westlake.air.pecs.parser;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.transition.Annotation;
import com.westlake.air.pecs.domain.db.FragmentInfo;
import com.westlake.air.pecs.domain.db.LibraryDO;
import com.westlake.air.pecs.domain.db.PeptideDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.parser.model.traml.*;
import com.westlake.air.pecs.parser.xml.AirXStream;
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

        // parse product interpretation
            // TODO: transition.getProduct().getInterpretationList()

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
        if (annotations.contains("[")) {
            fi.setWithBrackets(true);
            annotations = annotations.replace("[", "").replace("]", "");
        }
        fi.setAnnotations(annotations);
        try {
            ResultDO<Annotation> annotationResult = parseAnnotation(fi.getAnnotations());
            Annotation annotation = annotationResult.getModel();
            fi.setAnnotation(annotation);
            fi.setCutInfo(annotation.getType()+annotation.getLocation()+(annotation.getCharge()==1?"":("^"+annotation.getCharge())));
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
        System.out.println(traML.getProteinList().size());

        HashMap<String, Peptide> peptideMap = makePeptideMap(traML.getCompoundList().getPeptideList());
        ResultDO<List<PeptideDO>> tranResult = new ResultDO<>(true);
        List<PeptideDO> transitions = new ArrayList<>();

        try {
            //开始插入前先清空原有的数据库数据
            ResultDO resultDOTmp = peptideService.deleteAllByLibraryId(library.getId());
            logger.info("删除旧数据完毕");

            if (resultDOTmp.isFailed()) {
                logger.error(resultDOTmp.getMsgInfo());
                return ResultDO.buildError(ResultCode.DELETE_ERROR);
            }

            int count = 0;
            PeptideDO lastPeptide = null;
            for (Transition transition : traML.getTransitionList()) {
                ResultDO<PeptideDO> resultDO = parseTransition(transition, peptideMap, library);

                if (resultDO.isFailed()) {
                    tranResult.addErrorMsg(resultDO.getMsgInfo());
                    continue;
                }

                PeptideDO peptide = resultDO.getModel();
                if (lastPeptide == null) {
                    lastPeptide = peptide;
                } else {
                    //如果是同一个肽段下的不同离子片段
                    if (lastPeptide.getPeptideRef().equals(peptide.getPeptideRef()) && lastPeptide.getIsDecoy().equals(peptide.getIsDecoy())) {
                        for (String key : peptide.getFragmentMap().keySet()) {
                            lastPeptide.putFragment(key, peptide.getFragmentMap().get(key));
                        }
                    } else {
                        transitions.add(lastPeptide);
                        lastPeptide = peptide;
                    }
                }
            }
            peptideService.insertAll(transitions, false);
            count += transitions.size();
            logger.info(count + "条数据插入成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
        tranResult.setModel(transitions);
        return tranResult;
    }
}
