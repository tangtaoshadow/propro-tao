package com.westlake.air.swathplatform.parser.model.mzxml;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import lombok.Data;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-06 09:18
 */
@Data
public class PrecursorMz {

    Float value;

    @XStreamAsAttribute
    Long precursorScanNum;

    @XStreamAsAttribute
    Float precursorIntensity;

    @XStreamAsAttribute
    Integer precursorCharge;

    @XStreamAsAttribute
    String possibleCharges;

    @XStreamAsAttribute
    Float windowWideness;

    @XStreamAsAttribute
    String activationMethod;
}
