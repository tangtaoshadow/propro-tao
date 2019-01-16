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
    IO_EXCEPTION("IO_EXCEPTION", "文件读写错误"),
    PARAMS_NOT_ENOUGH("PARAMS_NOT_ENOUGH", "入参不齐"),

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
    LIBRARY_NOT_EXISTED("LIBRARY_NOT_EXISTED","指定的库不存在"),
    SEARCH_FRAGMENT_LENGTH_MUST_BIGGER_THAN_3("SEARCH_FRAGMENT_LENGTH_MUST_BIGGER_THAN_3","搜索的片段长度至少为4"),

    /**
     * ******
     * experiment
     * *******
     */
    FILE_LOCATION_CANNOT_BE_EMPTY("FILE_LOCATION_CANNOT_BE_EMPTY","文件路径不能为空"),
    FILE_NOT_EXISTED("FILE_NOT_EXISTED","文件不存在"),
    FILE_NOT_SET("FILE_NOT_SET","文件未设定"),
    EXPERIMENT_INSERT_ERROR("EXPERIMENT_INSERT_ERROR","实验数据插入失败"),
    EXPERIMENT_MZXML_FILE_MUST_BE_CONVERTED_TO_AIRD_FORMAT_FILE_FIRST("EXPERIMENT_MZXML_FILE_MUST_BE_CONVERTED_TO_AIRD_FORMAT_FILE_FIRST","原始实验文件必须转换为Aird格式文件才可以进入下一步"),
    SWATH_INFORMATION_NOT_EXISTED("SWATH_INFORMATION_NOT_EXISTED","Swath的分块信息不存在,请确保Aird压缩过程正确"),
    IRT_FIRST("IRT_FIRST","请先对原始实验运行Irt计算"),
    IRT_EXCEPTION("IRT_EXCEPTION","iRT失败"),
    AIRD_COMPRESSION_FIRST("AIRD_COMPRESSION_FIRST","请先对原始实验进行压缩处理"),
    /**
     * ******
     * ScanIndex
     * *******
     */
    SCAN_INDEX_LIST_MUST_BE_QUERY_WITH_EXPERIMENT_ID("SCAN_INDEX_LIST_MUST_BE_QUERY_WITH_EXPERIMENT_ID","ScanIndex的列表页面必须至少按照ExperimentId维度进行查询"),
    EXPERIMENT_NOT_EXISTED("EXPERIMENT_NOT_EXISTED","实验数据不存在"),
    SCAN_INDEX_NOT_EXISTED("SCAN_INDEX_NOT_EXISTED","对应的索引数据不存在"),
    SPECTRUM_NOT_EXISTED("SPECTRUM_NOT_EXISTED","对应的原始谱图不存在"),

    /**
     * ******
     * Convolution Data
     * *******
     */
    CONVOLUTION_DATA_NOT_EXISTED("CONVOLUTION_DATA_NOT_EXISTED","卷积数据不存在,请到详细页面进行卷积操作"),
    CONVOLUTION_FAILED("CONVOLUTION_FAILED","卷积失败"),
    ANALYSE_DATA_NOT_EXISTED("ANALYSE_DATA_NOT_EXISTED","卷积数据不存在"),
    ANALYSE_DATA_ID_CANNOT_BE_EMPTY("ANALYSE_DATA_ID_CANNOT_BE_EMPTY","卷积数据ID不能为空"),
    ANALYSE_DATA_ARE_ALL_ZERO("ANALYSE_DATA_ARE_ALL_ZERO","卷积数据全部为零"),
    ANALYSE_OVERVIEW_NOT_EXISTED("ANALYSE_OVERVIEW_NOT_EXISTED","卷积数据概览不存在"),
    ANALYSE_OVERVIEW_ID_CAN_NOT_BE_EMPTY("ANALYSE_OVERVIEW_ID_CAN_NOT_BE_EMPTY","卷积数据概览ID不能为空"),
    ONLY_SUPPORT_VERIFY_LIBRARY_SEARCH("ONLY_SUPPORT_VERIFY_LIBRARY_SEARCH","本接口只支持校准库的数据聚合查询"),
    DATA_ID_CANNOT_BE_EMPTY("DATA_ID_CANNOT_BE_EMPTY","DataId不能为空"),
    FRAGMENT_CANNOT_BE_EMPTY_ALL("FRAGMENT_CANNOT_BE_EMPTY_ALL","肽段碎片不能全部为空"),
    FRAGMENT_LENGTH_IS_TOO_LONG("FRAGMENT_LENGTH_IS_TOO_LONG","设定的肽段碎片长度大于本身长度"),
    AIRC_FILE_PATH_NOT_EXISTED("AIRC_FILE_PATH_NOT_EXISTED","卷积文件路径不存在"),
    AIRC_FILE_NOT_EXISTED("AIRC_FILE_PATH_NOT_EXISTED","卷积文件不存在"),
    POSITION_DELTA_LIST_LENGTH_NOT_EQUAL_TO_MZMAP_PLUS_ONE("POSITION_DELTA_LIST_LENGTH_NOT_EQUAL_TO_MZMAP_PLUS_ONE","索引地址搜索块的数目与mzMap+1的结果不一致"),

    /**
     * ******
     * Scores
     * *******
     */
     ALL_SCORE_DATA_ARE_DECOY("ALL_SCORE_DATA_ARE_DECOY","所有的数据都是伪肽段数据,无法打分"),
     ALL_SCORE_DATA_ARE_REAL("ALL_SCORE_DATA_ARE_REAL","所有的数据都是真实肽段数据,未包含伪肽段,无法打分"),
     SCORES_NOT_EXISTED("SCORES_NOT_EXISTED","打分数据不存在"),
     SCORE_DISTRIBUTION_NOT_GENERATED_YET("SCORE_DISTRIBUTION_NOT_GENERATED_YET","子分数分布情况还未生成"),

    /**
     * ******
     * RT Normalizer
     * *******
     */
    NOT_ENOUGH_IRT_PEPTIDES("NOT_ENOUGH_IRT_PEPTIDES","There are less than 2 iRT peptides, not enough for an RT correction"),

    /**
     * ******
     * Analyse
     * *******
     */
    COMPARISON_OVERVIEW_IDS_MUST_BIGGER_THAN_TWO("COMPARISON_OVERVIEW_IDS_MUST_BIGGER_THAN_TWO","需要需要两个不同的卷积ID才可以开始比对"),

    /**
     * ******
     * TASK
     * *******
     */
    TASK_TEMPLATE_NOT_EXISTED("TASK_TEMPLATE_NOT_EXISTED","任务模板不存在"),

    /**
     * ******
     * AirdCompressor
     * *******
     */
    CREATE_FILE_FAILED("CREATE_FILE_FAILED","创建文件失败"),

    /**
     * ******
     * Project
     * ******
     */
    PROJECT_NAME_CANNOT_BE_EMPTY("PROJECT_NAME_CANNOT_BE_EMPTY","项目名称不能为空"),
    PROJECT_NOT_EXISTED("PROJECT_NOT_EXISTED","项目不能为空"),
    NO_EXPERIMENT_UNDER_PROJECT("NO_EXPERIMENT_UNDER_PROJECT","项目下没有实验数据"),

    /**
     * ******
     * SQL-COMMON
     * *******
     */
    CONNECTION_URL_CANNOT_BE_EMPTY("CONNECTION_URL_CANNOT_BE_EMPTY","数据库链接URL不能为空"),
    CONNECTION_FAILED("CONNECTION_FAILED","数据库连接失败"),
    CONNECTION_SUCCESS("CONNECTION_SUCCESS","数据库连接成功"),
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
