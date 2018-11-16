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
    public static final double MATH_ROUND_PRECISION = 1000000d;

    //RT Normalizer
    //not static final
    public static final boolean CHECK_SPACINGS = true;//////////////
    public static final double SPACING_DIFFERENCE = 1.5d;
    public static final double SPACING_DIFFERENCE_GAP = 4d;
    public static final double SIGNAL_TO_NOISE_LIMIT = 1.0d;
    public static final int MISSING_LIMIT = 1;
    public static final double THRESHOLD = 0.000001d;
    public static final double AUTO_MAX_STDEV_FACTOR = 3.0d;
    public static final int MIN_REQUIRED_ELEMENTS = 10;

    //not static final
//    public static final double NOISE_FOR_EMPTY_WINDOW = Math.pow(10.0,20);
    public static final double NOISE_FOR_EMPTY_WINDOW = 2.0d;
    public static final double STOP_AFTER_INTENSITY_RATIO = 0.0001d;
    public static final double MIN_RSQ = 0.95d;
    public static final double MIN_COVERAGE = 0.6d;

    //not static final
    public static final String CHROMATOGRAM_PICKER_METHOD = "legacy";
//    public static final String CHROMATOGRAM_PICKER_METHOD = "corrected";

    public static final double PEAK_WIDTH = 40d;///
    public static final boolean ESTIMATE_BEST_PEPTIDES = false;
    public static final double INITIAL_QUALITY_CUTOFF = 0.5d;
    public static final double OVERALL_QUALITY_CUTOFF = 5.5d;
    public static final int RT_BINS = 10;
    public static final int MIN_PEPTIDES_PER_BIN = 1;
    public static final int MIN_BINS_FILLED = 8;

    //Extractor
    public static final String TRAFO_INVERT_MODEL = "LINEAR";
    public static final double RT_EXTRACTION_WINDOW = 600.0d;
    public static final float MZ_EXTRACTION_WINDOW = 0.05f;
    public static final float EXTRA_RT_EXTRACTION_WINDOW = 0.0f;

    public static final double DIA_EXTRACT_WINDOW = 0.05d;
    public static final int DIA_NR_ISOTOPES = 4;
    public static final int DIA_NR_CHARGES = 4;
    public static final double C13C12_MASSDIFF_U = 1.0033548d;
    public static final double PEAK_BEFORE_MONO_MAX_PPM_DIFF = 20.0d;
    public static final double DIA_BYSERIES_PPM_DIFF = 10.0d;
    public static final double DIA_BYSERIES_INTENSITY_MIN = 300.0d;

    public static final double C = 4.9384d;
    public static final double H = 7.7583d;
    public static final double N = 1.3577d;
    public static final double O = 1.4773d;
    public static final double S = 0.0417d;
    public static final double P = 0d;
    public static final double PROTON_MASS_U = 1.007276466771d;

    public static final int SCORE_RANGE = 20;

    public static final double EMG_CONST = 2.4055;
    public static final int EMG_MAX_ITERATION = 500;

    public static final String SUFFIX_AIRUS_INFO = ".aird.json";
    public static final String SUFFIX_AIRUS_DATA = ".aird";
    public static final String CHANGE_LINE = "\r\n";
    public static final String TAB = "\t";

    public static final String AIRD_COMPRESSION_TYPE_ZLIB = "zlib";
    public static final String AIRD_PRECISION_32 = "32";

}
