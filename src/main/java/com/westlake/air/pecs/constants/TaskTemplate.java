package com.westlake.air.pecs.constants;

import com.alibaba.fastjson.JSONArray;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-15 09:42
 */
public enum TaskTemplate {

    UPLOAD_EXPERIMENT_FILE("UPLOAD_EXPERIMENT_FILE", 3,
            "['构建索引','索引构建完毕,开始存储索引','索引存储完毕,流程结束']",
            "/experiment/create"),

    UPLOAD_LIBRARY_FILE("UPLOAD_LIBRARY_FILE", 4,
            "['清理原有旧文件','解析并存储文件','统计标准库基本情况','存储完毕,流程结束']",
            "/library/create"),

    SWATH_CONVOLUTION("SWATH_CONVOLUTION", 8,
            "['基本条件检查','构建MS1卷积坐标','获取全部MS1谱图索引列表','解析MS1指定谱图','开始卷积MS1数据','获取Swath窗口信息','卷积MS2数据','卷积完毕']",
            "/experiment/extractor"),

    SWATH_WORKFLOW("SWATH_WORKFLOW", 0,
            "[]",
            "/task/swathworkflow"),

    MZXML_COMPRESSOR("MZXML_COMPRESSOR", 0,
            "[]",
            "/task/mzxmlcompressor"),

    ;

    String templateName;

    Integer stepNum;

    List<String> steps;

    String pagePath;

    TaskTemplate(String templateName, Integer stepNum, String steps, String pagePath) {
        this.templateName = templateName;
        this.stepNum = stepNum;
        this.steps = JSONArray.parseArray(steps, String.class);
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

    public List getSteps() {
        return steps;
    }

    public String getPagePath() {
        return pagePath;
    }
}
