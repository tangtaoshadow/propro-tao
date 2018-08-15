package com.westlake.air.pecs.constants;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-15 09:42
 */
public enum TaskTemplate {

    SWATH_WORKFLOW("SWATH_WORKFLOW","A complete swath workflow"),
    MZXML_COMPRESSOR("MZXML_COMPRESSOR","Compress the 64-bit MzXML to 32-bit MzXML"),

    ;

    String templateName;

    String description;

    TaskTemplate(String templateName, String description){
        this.templateName = templateName;
        this.description = description;
    }

    public String getTemplateName(){
        return templateName;
    }

    public String getDescription(){
        return description;
    }
}
