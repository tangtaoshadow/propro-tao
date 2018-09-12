package com.westlake.air.pecs.constants;

import java.io.Serializable;
import java.text.Normalizer;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 09:38
 */
public enum ResultCode implements Serializable {

    /**
     * ******
     * 系统错误
     * *******
     */
    ERROR("SYSTEM_ERROR", "系统繁忙 ，请稍后再试!"),
    EXCEPTION("SYSTEM_EXCEPTION", "系统繁忙， 稍后再试!"),

    /**
     * ******
     * 常见通用错误
     * *******
     */
    OBJECT_CANNOT_BE_NULL("OBJECT_CANNOT_BE_NULL", "对象不能为空"),
    OBJECT_IS_EXISTED("OBJECT_IS_EXISTED", "对象已存在"),
    OBJECT_NOT_EXISTED("OBJECT_NOT_EXISTED", "对象不存在"),
    QUERY_ERROR("QUERY_ERROR", "获取数据失败"),
    INSERT_ERROR("INSERT_ERROR", "插入数据失败"),
    DUPLICATE_KEY_ERROR("DUPLICATE_KEY_ERROR", "插入数据失败,已有同名项存在"),
    SAVE_ERROR("SAVE_ERROR", "保存数据失败"),
    UPDATE_ERROR("UPDATE_ERROR", "更新数据失败"),
    DELETE_ERROR("UPDATE_ERROR", "删除数据失败"),
    PARSE_ERROR("PARSE_ERROR", "解析错误"),
    USER_NOT_EXISTED("USER_NOT_EXISTED", "该用户不存在"),
    ID_CANNOT_BE_NULL_OR_ZERO("ID_CANNOT_BE_NULL_OR_ZERO", "ID不能为空或者0"),
    LINE_IS_EMPTY("LINE_IS_EMPTY", "内容为空"),
    EXTRACT_FAILED("EXTRACT_FAILED", "解压缩失败"),

    /**
     * ******
     * library
     * *******
     */
    LIBRARY_NAME_CANNOT_BE_EMPTY("LIBRARY_NAME_CANNOT_BE_EMPTY","标准库名称不能为空"),
    LIBRARY_CANNOT_BE_EMPTY("LIBRARY_CANNOT_BE_EMPTY","标准库不能为空"),
    DATA_IS_EMPTY("DATA_IS_EMPTY","数据为空"),
    INPUT_FILE_TYPE_MUST_BE_TSV_OR_TRAML("INPUT_FILE_TYPE_MUST_BE_TSV_OR_TRAML","上传的文件格式必须为TSV或者是TraML"),

    /**
     * ******
     * experiment
     * *******
     */
    FILE_LOCATION_CANNOT_BE_EMPTY("FILE_LOCATION_CANNOT_BE_EMPTY","文件路径不能为空"),
    FILE_NOT_EXISTED("FILE_NOT_EXISTED","文件不存在"),
    FILE_NOT_SET("FILE_NOT_SET","文件未设定"),
    LIBRARY_NOT_EXISTED("LIBRARY_NOT_EXISTED","指定的库不存在"),

    /**
     * ******
     * ScanIndex
     * *******
     */
    SCAN_INDEX_LIST_MUST_BE_QUERY_WITH_EXPERIMENT_ID("SCAN_INDEX_LIST_MUST_BE_QUERY_WITH_EXPERIMENT_ID","ScanIndex的列表页面必须至少按照ExperimentId维度进行查询"),
    EXPERIMENT_NOT_EXISTED("EXPERIMENT_NOT_EXISTED","实验数据不存在"),
    SCAN_INDEX_NOT_EXISTED("SCAN_INDEX_NOT_EXISTED","对应的索引数据不存在"),

    /**
     * ******
     * Evolution Data
     * *******
     */
    EVOLUTION_DATA_NOT_EXISTED("EVOLUTION_DATA_NOT_EXISTED","卷积数据不存在,请到详细页面进行卷积操作"),
    ANALYSE_DATA_NOT_EXISTED("ANALYSE_DATA_NOT_EXISTED","卷积数据不存在"),
    ANALYSE_OVERVIEW_NOT_EXISTED("ANALYSE_OVERVIEW_NOT_EXISTED","卷积数据概览不存在"),
    ANALYSE_OVERVIEW_ID_CAN_NOT_BE_EMPTY("ANALYSE_OVERVIEW_ID_CAN_NOT_BE_EMPTY","卷积数据概览ID不能为空"),
    ONLY_SUPPORT_VERIFY_LIBRARY_SEARCH("ONLY_SUPPORT_VERIFY_LIBRARY_SEARCH","本接口只支持校准库的数据聚合查询"),

    /**
     * ******
     * RT Normalizer
     * *******
     */
    NOT_ENOUGH_IRT_PEPTIDES("NOT_ENOUGH_IRT_PEPTIDES","There are less than 2 iRT peptides, not enough for an RT correction"),

    /**
     * ******
     * TASK
     * *******
     */
    TASK_TEMPLATE_NOT_EXISTED("TASK_TEMPLATE_NOT_EXISTED","任务模板不存在"),
    ;

    private String code = "";
    private String message = "";

    private static final long serialVersionUID = -799302222165012777L;

    /**
     * @param code
     * @param message
     */
    ResultCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
