package com.westlake.air.pecs.constants;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-15 09:42
 */
public enum TaskTemplate {

    UPLOAD_EXPERIMENT_FILE("UPLOAD_EXPERIMENT_FILE", "/experiment/create"),

    UPLOAD_LIBRARY_FILE("UPLOAD_LIBRARY_FILE", "/library/create"),

    SWATH_CONVOLUTION("SWATH_CONVOLUTION", "/experiment/extractor"),

    SWATH_WORKFLOW("SWATH_WORKFLOW", "/task/swathworkflow"),

    MZXML_COMPRESSOR("MZXML_COMPRESSOR", "/task/mzxmlcompressor"),

    ;

    String templateName;

    String pagePath;

    TaskTemplate(String templateName, String pagePath) {
        this.templateName = templateName;
        this.pagePath = pagePath;
    }

    public static TaskTemplate getByName(String templateName) {
        for (TaskTemplate template : values()) {
            if (template.getTemplateName().equals(templateName)) {
                return template;
            }
        }
        return null;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getPagePath() {
        return pagePath;
    }
}
