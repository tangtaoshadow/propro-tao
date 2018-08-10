package com.westlake.air.pecs.constants;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-27 21:05
 */
public class Constants {

    //每批处理的数据
    public static final int MAX_PAGE_SIZE_FOR_FRAGMENT = 100000;
    public static final int DECOY_GENERATOR_TRY_TIMES = 10;
    public static final int MAX_INSERT_RECORD_FOR_TRANSITION = 100000;

    //RT Normalizer
    public static final float SIGNAL_TO_NOISE_LIMIT = 1.0f;
    public static final int MISSING_LIMIT = 1;
    public static final float THRESHOLD = 0.01f;
    public static final float AUTO_MAX_STDEV_FACTOR = 3.0f;
    public static final int MIN_REQUIRED_ELEMENTS = 10;
    public static final float NOISE_FOR_EMPTY_WINDOW = (float) Math.pow(10.0,20);
    public static final float STOP_AFTER_INTENSITY_RATIO = 0.0001f;
    public static final float MIN_RSQ = 0.95f;
    public static final float MIN_COVERAGE = 0.6f;
    public static final int RT_BINS = 10;
    public static final int MIN_PEPTIDES_PER_BIN = 1;
    public static final int MIN_BINS_FILLED = 8;

    public static String EXP_SUFFIX_MZXML = "mzxml";
    public static String EXP_SUFFIX_MZML = "mzml";
}
