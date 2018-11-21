package com.westlake.air.pecs.constants;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-15 09:42
 */
public enum TaskTemplate {

    /**
     * default template
     */
    DEFAULT("DEFAULT", ""),

    /**
     * upload experiment file
     */
    UPLOAD_EXPERIMENT_FILE("UPLOAD_EXPERIMENT_FILE", "/experiment/create"),

    /**
     * upload library file(including standard library and irt library)
     */
    UPLOAD_LIBRARY_FILE("UPLOAD_LIBRARY_FILE", "/library/create"),

    /**
     * extract for mzxml with standard library
     */
    EXTRACTOR("EXTRACTOR", "/experiment/extractor"),

    /**
     * compute irt for slope and intercept
     */
    IRT("IRT", "/experiment/irt"),

    /**
     * compute sub scores
     */
    SCORE("SCORE", ""),

    /**
     * the whole workflow for swath including(irt -> extractor -> sub scores -> final score)
     */
    SWATH_WORKFLOW("SWATH_WORKFLOW", ""),

    /**
     * compress the mzxml and sort the mzxml scan for swath
     */
    COMPRESSOR_AND_SORT("COMPRESSOR_AND_SORT", ""),

    /**
     * compress to lms Format
     */
    COMPRESSOR_TO_LMS("COMPRESSOR_TO_LMS", ""),

    /**
     * export subscores tsv file for pyprophet
     */
    EXPORT_SUBSCORES_TSV_FILE_FOR_PYPROPHET("EXPORT_SUBSCORES_TSV_FILE_FOR_PYPROPHET", ""),

    /**
     * generate all the score types' distribute map
     */
    BUILD_SCORE_DISTRIBUTE("BUILD_SCORE_DISTRIBUTE", ""),

    /**
     * task for any tests
     */
    TEST("TEST", ""),



    ;

    String name;

    String pagePath;

    TaskTemplate(String templateName, String pagePath) {
        this.name = templateName;
        this.pagePath = pagePath;
    }

    public static TaskTemplate getByName(String name) {
        for (TaskTemplate template : values()) {
            if (template.getName().equals(name)) {
                return template;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public String getPagePath() {
        return pagePath;
    }
}
