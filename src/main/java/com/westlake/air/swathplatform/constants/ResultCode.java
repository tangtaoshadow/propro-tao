package com.westlake.air.swathplatform.constants;

import java.io.Serializable;

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
    SAVE_ERROR("SAVE_ERROR", "保存数据失败"),
    UPDATE_ERROR("UPDATE_ERROR", "更新数据失败"),
    DELETE_ERROR("UPDATE_ERROR", "删除数据失败"),
    PARSE_ERROR("PARSE_ERROR", "解析错误"),
    USER_NOT_EXISTED("USER_NOT_EXISTED", "该用户不存在"),
    ID_CANNOT_BE_NULL_OR_ZERO("ID_CANNOT_BE_NULL_OR_ZERO", "ID不能为空或者0"),

    /**
     * ******
     * library表
     * *******
     */
    LIBRARY_NAME_CANNOT_BE_EMPTY("LIBRARY_NAME_CANNOT_BE_EMPTY","标准库名称不能为空"),

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
