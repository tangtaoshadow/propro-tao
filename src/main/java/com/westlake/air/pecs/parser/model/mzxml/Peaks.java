package com.westlake.air.pecs.parser.model.mzxml;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.westlake.air.pecs.parser.xml.PeaksConverter;
import lombok.Data;

@Data
@XStreamConverter(PeaksConverter.class)
public class Peaks {

    byte[] value;

    @XStreamAsAttribute
    protected Integer precision;

    @XStreamAsAttribute
    String byteOrder;

    @XStreamAsAttribute
    String contentType;

    @XStreamAsAttribute
    String compressionType;

    @XStreamAsAttribute
    Integer compressedLen;
}
