package com.westlake.air.swathplatform.service.impl;

import com.thoughtworks.xstream.security.AnyTypePermission;
import com.westlake.air.swathplatform.domain.traml.*;
import com.westlake.air.swathplatform.service.XMLParseService;
import com.westlake.air.swathplatform.xml.AirXStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;

@Service("tramlService")
public class TraMLServiceImpl implements XMLParseService {

    @Autowired
    AirXStream airXStream;

    public Class<?>[] classes = new Class[]{
            Compound.class, CompoundList.class, Configuration.class, Contact.class, Cv.class, CvParam.class,
            Evidence.class, Instrument.class, IntermediateProduct.class, Interpretation.class, Modification.class,
            Peptide.class, Precursor.class, Prediction.class, Product.class, Protein.class, ProteinRef.class,
            Publication.class, RetentionTime.class, Software.class, SourceFile.class, Target.class, TargetList.class,
            TraML.class, Transition.class, UserParam.class, ValidationStatus.class
    };

    @PostConstruct
    public void init(){
        airXStream.addPermission(new AnyTypePermission());
        AirXStream.setupDefaultSecurity(airXStream);
        airXStream.processAnnotations(classes);
        airXStream.allowTypes(classes);
    }

    @Override
    public TraML parse(File file) {
        //XML反序列化
        TraML traML = new TraML();
        airXStream.fromXML(file, traML);
        return traML;
    }

    public String parse(TraML traML){
        return airXStream.toXML(traML);
    }
}
