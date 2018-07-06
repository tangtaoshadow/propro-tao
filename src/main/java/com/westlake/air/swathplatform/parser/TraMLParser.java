package com.westlake.air.swathplatform.parser;

import com.thoughtworks.xstream.security.AnyTypePermission;
import com.westlake.air.swathplatform.parser.model.mzxml.*;
import com.westlake.air.swathplatform.parser.model.traml.*;
import com.westlake.air.swathplatform.parser.model.traml.Software;
import com.westlake.air.swathplatform.parser.xml.AirXStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.regex.Pattern;

@Component
public class TraMLParser {

    @Autowired
    AirXStream airXStream;

    public Class<?>[] classes = new Class[]{
            Compound.class, CompoundList.class, Configuration.class, Contact.class, Cv.class, CvParam.class,
            Evidence.class, Instrument.class, IntermediateProduct.class, Interpretation.class, Modification.class,
            Peptide.class, Precursor.class, Prediction.class, Product.class, Protein.class, ProteinRef.class,
            Publication.class, RetentionTime.class, com.westlake.air.swathplatform.parser.model.traml.Software.class, SourceFile.class, Target.class, TargetList.class,
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
}
