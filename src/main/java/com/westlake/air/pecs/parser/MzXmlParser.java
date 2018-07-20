package com.westlake.air.pecs.parser;

import com.westlake.air.pecs.domain.bean.SimpleScanIndex;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import com.westlake.air.pecs.parser.model.mzxml.*;
import com.westlake.air.pecs.parser.xml.AirXStream;
import com.westlake.air.pecs.parser.xml.PrecursorMzConverter;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

@Component
public class MzXmlParser {

    public static int PRECISION_32 = 32;
    public static int PRECISION_64 = 64;

    @Autowired
    AirXStream airXStream;

    private static byte[] prefix = "<scanList>".getBytes();
    private static byte[] suffix = "</scanList>".getBytes();

    public final Logger logger = LoggerFactory.getLogger(MzXmlParser.class);

    public Class<?>[] classes = new Class[]{
            DataProcessing.class, Maldi.class, MsInstrument.class, MsRun.class, MzXML.class, NameValue.class,
            OntologyEntry.class, Operator.class, Orientation.class, ParentFile.class, Pattern.class,
            Peaks.class, Plate.class, PrecursorMz.class, Robot.class, Scan.class, ScanOrigin.class,
            Separation.class, SeparationTechnique.class, Software.class, Spot.class, Spotting.class,
    };

    public MzXmlParser() {
    }

    private void prepare() {

        airXStream.processAnnotations(classes);
        airXStream.allowTypes(classes);
//        airXStream.registerConverter(new PeaksConverter());
        airXStream.registerConverter(new PrecursorMzConverter());
    }

    //    public void parseAll(File file, LmsIndexer indexer) throws Exception {
//        prepare();
//        List<ScanIndexDO> indexList = indexer.index(file);
//        for (ScanIndexDO scanIndexDO : indexList) {
//
//        }
//    }

    public TreeMap<Double, Double> parseOne(RandomAccessFile raf, SimpleScanIndex index) throws IOException {
        prepare();
        TreeMap<Double, Double> hashMap = new TreeMap<>();
        raf.seek(index.getStart());
        byte[] reader = new byte[(int) (index.getEnd() - index.getStart())];
        raf.read(reader);
        Scan scan = new Scan();
        airXStream.fromXML(new String(reader), scan);
        if (scan.getPeaksList() != null && scan.getPeaksList().size() >= 1) {
            Peaks peaks = scan.getPeaksList().get(0);
//            hashMap = getPeakMap(peaks.getValue(), peaks.getPrecision(), false);
        }

        return hashMap;
    }

    public TreeMap<Double, Double> parseOne(File file, ScanIndexDO index) {
        prepare();
        TreeMap<Double, Double> hashMap = new TreeMap<>();
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            raf.seek(index.getStart());
            byte[] reader = new byte[(int) (index.getEnd() - index.getStart())];
            raf.read(reader);
            Scan scan = new Scan();
            airXStream.fromXML(new String(reader), scan);
            if (scan.getPeaksList() != null && scan.getPeaksList().size() >= 1) {
                Peaks peaks = scan.getPeaksList().get(0);
                hashMap = getPeakMap(peaks.getValue(), peaks.getPrecision(), true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return hashMap;
    }

    public TreeMap<Double, Double> getPeakMap(byte[] value, int precision, boolean isCompression) {

        double[] values = getValues(value, precision, isCompression);

        TreeMap<Double, Double> peakMap = new TreeMap<>();

        for (int peakIndex = 0; peakIndex < values.length - 1; peakIndex += 2) {
            // get the two value
            Double mz = values[peakIndex];
            Double intensity = values[peakIndex + 1];

            peakMap.put(mz, intensity);
        }

        return peakMap;
    }

    public TreeMap<Double, Double> getPeakMap(String value, int precision, boolean isCompression) {

        double[] values = getValues(value, precision, isCompression);

        TreeMap<Double, Double> peakMap = new TreeMap<>();

        for (int peakIndex = 0; peakIndex < values.length - 1; peakIndex += 2) {
            // get the two value
            Double mz = values[peakIndex];
            Double intensity = values[peakIndex + 1];

            peakMap.put(mz, intensity);
        }

        return peakMap;
    }

    public TreeMap<Double, Double> getPeakMap(String mz, String intensity, int mzPrecision, int intensityPrecision, boolean isCompression) {

        double[] mzValues = getValues(mz, mzPrecision, isCompression);
        double[] intensityValues = getValues(intensity, intensityPrecision, false);

        if (mzValues == null || intensityValues == null || mzValues.length != intensityValues.length) {
            return null;
        }
        TreeMap<Double, Double> peakMap = new TreeMap<>();
        for (int peakIndex = 0; peakIndex < mzValues.length; peakIndex++) {
            peakMap.put(mzValues[peakIndex], intensityValues[peakIndex]);
        }

        return peakMap;

    }

    private double[] getValues(byte[] value, int precision, boolean isCompression) {
        double[] values;

        ByteBuffer byteBuffer = ByteBuffer.wrap(value);

        if (isCompression) {
            Inflater decompresser = new Inflater();
            decompresser.setInput(byteBuffer.array());

            byte[] decompressedData = new byte[byteBuffer.capacity() * 10];

            try {
                int usedLength = decompresser.inflate(decompressedData);
                byteBuffer = ByteBuffer.wrap(decompressedData, 0, usedLength);
            } catch (DataFormatException e) {
                logger.error("Decompress failed!", e);
            }
        }

        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        if (precision == PRECISION_64) {
            values = new double[byteBuffer.asDoubleBuffer().capacity()];
            byteBuffer.asDoubleBuffer().get(values);
        } else {
            FloatBuffer floats = byteBuffer.asFloatBuffer();
            values = new double[floats.capacity()];

            for (int index = 0; index < floats.capacity(); index++) {
                values[index] = (double) floats.get(index);
            }
        }

        byteBuffer.clear();
        return values;
    }

    private double[] getValues(String value, int precision, boolean isCompression) {
        return getValues(new Base64().decode(value), precision, isCompression);
    }
}
