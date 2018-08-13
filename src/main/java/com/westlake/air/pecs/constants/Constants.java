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

    //Extractor
    public static final String TRAFO_INVERT_MODEL = "LINEAR";
    public static final float RT_EXTRACTION_WINDOW = 600.0f;
    public static final float MZ_EXTRACTION_WINDOW = 0.05f;
    public static final float EXTRA_RT_EXTRACTION_WINDOW = 0.0f;

    public static final float DIA_EXTRACT_WINDOW = 0.05f;
    public static final int DIA_NR_ISOTOPES = 4;
    public static final int DIA_NR_CHARGES = 4;
    public static final float C13C12_MASSDIFF_U = 1.0033548f;
    public static final float PEAK_BEFORE_MONO_MAX_PPM_DIFF = 20.0f;

    public static final float C = 4.9384f;
    public static final float H = 7.7583f;
    public static final float N = 1.3577f;
    public static final float O = 1.4773f;
    public static final float S = 0.0417f;
    public static final float P = 0f;


    public static String EXP_SUFFIX_MZXML = "mzxml";
    public static String EXP_SUFFIX_MZML = "mzml";
}
