package com.westlake.air.pecs.constants;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-15 09:42
 */
public enum TaskTemplate {

    UPLOAD_EXPERIMENT_FILE("UPLOAD_EXPERIMENT_FILE","UPLOAD_EXPERIMENT_FILE","/experiment/create"),
    UPLOAD_LIBRARY_FILE("UPLOAD_LIBRARY_FILE","UPLOAD_LIBRARY_FILE","/library/create"),
    SWATH_WORKFLOW("SWATH_WORKFLOW","A complete swath workflow","/task/swathworkflow"),
    MZXML_COMPRESSOR("MZXML_COMPRESSOR","Compress the 64-bit MzXML to 32-bit MzXML","/task/mzxmlcompressor"),

    ;

    String templateName;

    String description;

    String pagePath;

    TaskTemplate(String templateName, String description, String pagePath){
        this.templateName = templateName;
        this.description = description;
        this.pagePath = pagePath;
    }

    public static TaskTemplate getByName(String templateName){
        for(TaskTemplate template : values()){
            if(template.getDescription().equals(templateName)){
                return template;
            }
        }
        return null;
    }

    public String getTemplateName(){
        return templateName;
    }

    public String getDescription(){
        return description;
    }

    public String getPagePath(){
        return pagePath;
    }
}
