package com.westlake.air.propro.domain.bean.aird;

import lombok.Data;

@Data
public class Compressor {

    public static String TARGET_MZ = "mz";
    public static String TARGET_INTENSITY = "intensity";

    public static String METHOD_ZLIB = "zlib";
    public static String METHOD_PFOR = "pFor";
    public static String METHOD_LOG10 = "log10";

    //压缩对象,支持mz和intensity两种
    String target;

    //压缩方法,使用分号隔开,目前支持PFor和Zlib两种
    String method;
}
