package com.westlake.air.pecs.parser.model.mzxml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import lombok.Data;

import javax.xml.datatype.Duration;
import java.util.List;

/**
 * The actual MS data is here
 */
@Data
@XStreamAlias("scan")
public class Scan {

    @XStreamImplicit(itemFieldName = "scanOrigin")
    List<ScanOrigin> scanOriginList;

    @XStreamImplicit(itemFieldName = "precursorMz")
    List<PrecursorMz> precursorMzList;

    Maldi maldi;

    /**
     * This is the actual data encoded using base64. Byte order must be network. The order of the pairs must be m/z – intensity.
     */
    @XStreamImplicit(itemFieldName = "peaks")
    List<Peaks> peaksList;

    @XStreamImplicit(itemFieldName = "nameValue")
    List<NameValue> nameValueList;

    @XStreamImplicit(itemFieldName = "scan")
    List<Scan> scanList;

    @XStreamImplicit(itemFieldName = "comment")
    List<String> commentList;

    @XStreamAsAttribute
    Long num;

    @XStreamAsAttribute
    Long msLevel;

    @XStreamAsAttribute
    Long peaksCount;

    @XStreamAsAttribute
    String polarity;

    @XStreamAsAttribute
    String scanType;

    @XStreamAsAttribute
    String filterLine;

    @XStreamAsAttribute
    Boolean centroided;

    @XStreamAsAttribute
    Boolean deisotoped;

    @XStreamAsAttribute
    Boolean chargeDeconvoluted;

    @XStreamAsAttribute
    Duration retentionTime;

    @XStreamAsAttribute
    Float ionisationEnergy;

    @XStreamAsAttribute
    Float collisionEnergy;

    @XStreamAsAttribute
    Float cidGasPressure;

    @XStreamAsAttribute
    Float startMz;

    @XStreamAsAttribute
    Float endMz;

    @XStreamAsAttribute
    Float lowMz;

    @XStreamAsAttribute
    Float highMz;

    @XStreamAsAttribute
    Float basePeakMz;

    @XStreamAsAttribute
    Float basePeakIntensity;

    @XStreamAsAttribute
    Float totIonCurrent;

    @XStreamAsAttribute
    Integer msInstrumentID;

    @XStreamAsAttribute
    Float compensationVoltage;
}
