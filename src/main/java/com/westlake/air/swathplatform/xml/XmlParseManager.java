package com.westlake.air.swathplatform.xml;

import com.thoughtworks.xstream.security.AnyTypePermission;
import com.westlake.air.swathplatform.domain.traml.*;

import java.io.File;

/**
 * 转换器的管理类,单例模式,使用XStream库进行序列化和反序列化
 */
public class XmlParseManager {

    TraMLXStream traMLXStream;
    public static Class<?>[] classes = new Class[]{
            Compound.class, CompoundList.class, Configuration.class, Contact.class, Cv.class, CvParam.class,
            Evidence.class, Instrument.class, IntermediateProduct.class, Interpretation.class, Modification.class,
            Peptide.class, Precursor.class, Prediction.class, Product.class, Protein.class, ProteinRef.class,
            Publication.class, RetentionTime.class, Software.class, SourceFile.class, Target.class, TargetList.class,
            TraML.class, Transition.class, UserParam.class, ValidationStatus.class
    };

    private static XmlParseManager instance;

    private XmlParseManager() {
        traMLXStream = new TraMLXStream();
        traMLXStream.addPermission(new AnyTypePermission());
        TraMLXStream.setupDefaultSecurity(traMLXStream);
        traMLXStream.processAnnotations(classes);
        traMLXStream.allowTypes(classes);
    }

    public synchronized static XmlParseManager getInstance() {
        if (instance == null) {
            instance = new XmlParseManager();
        }
        return instance;
    }

    public TraML parse(File file) {
        //XML反序列化
        TraML traML = new TraML();
        traMLXStream.fromXML(file, traML);
        return traML;
    }

    public String parse(TraML traML){
        return traMLXStream.toXML(traML);
    }
}
