package com.westlake.air.pecs.parser;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.Annotation;
import com.westlake.air.pecs.domain.db.LibraryDO;
import com.westlake.air.pecs.domain.db.TransitionDO;
import com.westlake.air.pecs.parser.model.traml.*;
import com.westlake.air.pecs.parser.xml.AirXStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
public class TransitionTraMLParser extends BaseTransitionParser{

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

    public ResultDO<TransitionDO> parseTransition(Transition transition, HashMap<String, Peptide> peptideMap, LibraryDO library, boolean justReal) {
        ResultDO<TransitionDO> resultDO = new ResultDO<>(true);
        TransitionDO transitionDO = new TransitionDO();
        transitionDO.setLibraryId(library.getId());
        transitionDO.setLibraryName(library.getName());

        // parse transition attribution
        boolean isDecoy = transition.getPeptideRef().toLowerCase().contains("decoy");
        if (justReal && isDecoy) {
            return null;
        }
        transitionDO.setIsDecoy(isDecoy);
        // parse transition id to the name
        transitionDO.setName(transition.getId());
        // parse transition cvparams
        List<CvParam> listCvParams = transition.getCvParams();
        for (CvParam cvParam : listCvParams) {
            if (cvParam.getName().equals("product ion intensity")) {
                transitionDO.setIntensity(Double.valueOf(cvParam.getValue()));
            } else if (cvParam.getName().equals("decoy SRM transition")) {
                transitionDO.setIsDecoy(true);
            } else if (cvParam.getName().equals("target SRM transition")) {
                transitionDO.setIsDecoy(false);
            }
        }
        // parse transition userparam
        List<UserParam> listUserParams = transition.getUserParams();
        for (UserParam userParam : listUserParams) {
            if (userParam.getName().equals("annotation")) {
                transitionDO.setAnnotations(userParam.getValue());
            }
        }
        // parse precursor
        listCvParams = transition.getPrecursor().getCvParams();
        for (CvParam cvParam : listCvParams) {
            if (cvParam.getName().equals("isolation window target m/z")) {
                transitionDO.setPrecursorMz(Double.valueOf(cvParam.getValue()));
            }
            if (cvParam.getName().equals("charge state")) {
                transitionDO.setPrecursorCharge(Integer.valueOf(cvParam.getValue()));
            }
        }
        // parse product cvParams
        listCvParams = transition.getProduct().getCvParams();
        for (CvParam cvParam : listCvParams) {
            if (cvParam.getName().equals("isolation window target m/z")) {
                transitionDO.setProductMz(Double.valueOf(cvParam.getValue()));
            }
            if (cvParam.getName().equals("charge state")) {
                transitionDO.setProductCharge(Integer.valueOf(cvParam.getValue()));
            }
        }
        // parse product interpretation
            // ToDO: transition.getProduct().getInterpretationList()

        // parse rt, sequence, full name, protein name from peptideMap
        Peptide peptide = peptideMap.get(transition.getPeptideRef());
        String rt = peptide.getRetentionTimeList().get(0).getCvParams().get(0).getValue();
        transitionDO.setRt(Double.valueOf(rt));
        transitionDO.setSequence(peptide.getSequence());
        transitionDO.setProteinName(peptide.getProteinRefList().get(0).getRef());
        transitionDO.setFullName(peptide.getUserParams().get(0).getValue());
        transitionDO.setPeptideRef(transitionDO.getFullName()+"_"+transitionDO.getPrecursorCharge());
        // parse annotations
        String annotations = transitionDO.getAnnotations();
        if (annotations.contains("[")) {
            transitionDO.setWithBrackets(true);
            annotations = annotations.replace("[", "").replace("]", "");
        }
        transitionDO.setAnnotations(annotations);
        try {
            ResultDO<Annotation> annotationResult = parseAnnotation(transitionDO.getAnnotations());
            Annotation annotation = annotationResult.getModel();
            transitionDO.setAnnotation(annotation);
            transitionDO.setCutInfo(annotation.getType()+annotation.getLocation()+(annotation.getCharge()==1?"":("^"+annotation.getCharge())));
            resultDO.setModel(transitionDO);
        } catch (Exception e) {
            resultDO.setSuccess(false);
            resultDO.setMsgInfo("Line插入错误(Sequence未知)");
            logger.error(transitionDO.getLibraryId() + ":" + transitionDO.getAnnotation(), e);
            return resultDO;
        }

        parseModification(transitionDO);

        return resultDO;
    }

    @Override
    public ResultDO parseAndInsert(InputStream in, LibraryDO library, boolean justReal) {
        TraML traML = parse(in);
        System.out.println(traML.getProteinList().size());

        HashMap<String, Peptide> peptideMap = makePeptideMap(traML.getCompoundList().getPeptideList());
        ResultDO<List<TransitionDO>> tranResult = new ResultDO<>(true);
        List<TransitionDO> transitions = new ArrayList<>();

        try {
            //开始插入前先清空原有的数据库数据
            ResultDO resultDOTmp = transitionService.deleteAllByLibraryId(library.getId());
            logger.info("删除旧数据完毕");

            if (resultDOTmp.isFailed()) {
                logger.error(resultDOTmp.getMsgInfo());
                return ResultDO.buildError(ResultCode.DELETE_ERROR);
            }

            int count = 0;
            for (Transition transition : traML.getTransitionList()) {
                ResultDO<TransitionDO> resultDO = parseTransition(transition, peptideMap, library, justReal);
                if (resultDO == null) {
                    continue;
                }
                if (resultDO.isFailed()) {
                    tranResult.addErrorMsg(resultDO.getMsgInfo());
                } else {
                    transitions.add(resultDO.getModel());
                }
                if (transitions.size() > Constants.MAX_INSERT_RECORD_FOR_TRANSITION) {
                    count += Constants.MAX_INSERT_RECORD_FOR_TRANSITION;
                    transitionService.insertAll(transitions, false);
                    logger.info(count + "条数据插入成功");
                    transitions = new ArrayList<>();
                }
            }
            transitionService.insertAll(transitions, false);
            count += transitions.size();
            logger.info(count + "条数据插入成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
        tranResult.setModel(transitions);
        return tranResult;
    }
}
