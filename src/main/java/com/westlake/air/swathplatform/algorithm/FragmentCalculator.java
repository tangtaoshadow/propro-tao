package com.westlake.air.swathplatform.algorithm;

import com.westlake.air.swathplatform.constants.ResidueType;
import com.westlake.air.swathplatform.domain.bean.Annotation;
import com.westlake.air.swathplatform.domain.bean.FragmentResult;
import com.westlake.air.swathplatform.domain.bean.PeptideFormula;
import com.westlake.air.swathplatform.domain.db.TransitionDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-12 10:32
 */
@Component
public class FragmentCalculator {

    public final Logger logger = LoggerFactory.getLogger(FragmentCalculator.class);

    public FragmentResult getResult(TransitionDO transitionDO) {
        FragmentResult result = new FragmentResult(transitionDO);
        String sequence = transitionDO.getPeptideSequence();
        List<Annotation> annotationList = transitionDO.getAnnotations();
        Annotation annotation = annotationList.get(0);
        PeptideFormula formula = null;
        double weight = 0;
        if(annotation.getType().equals(ResidueType.AIon)||
                annotation.getType().equals(ResidueType.BIon)||
                annotation.getType().equals(ResidueType.CIon)){
            formula = new PeptideFormula(sequence.substring(0,annotation.getLocation()),annotation.getCharge(), annotation.getType());
        }else if(annotation.getType().equals(ResidueType.XIon)||
                annotation.getType().equals(ResidueType.YIon)||
                annotation.getType().equals(ResidueType.ZIon)){
            formula = new PeptideFormula(sequence.substring(sequence.length()-annotation.getLocation(),sequence.length()),annotation.getCharge(), annotation.getType());
        }else{
            logger.error("解析出未识别离子类型:"+transitionDO.getAnnotation());
            return null;
        }
        result.setMonoWeight(formula.getMonoWeight());
        result.setAverageWeight(formula.getAverageWeight());
        return result;
    }


}
