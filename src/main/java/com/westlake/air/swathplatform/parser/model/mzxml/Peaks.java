package com.westlake.air.swathplatform.parser.model.mzxml;

import lombok.Data;

@Data
public class Peaks {

    @XStream
    protected byte[] value;
    @XmlAttribute
    @XmlJavaTypeAdapter(NonNegativeIntegerAdapter.class)
    protected Long precision;
    @XmlAttribute(required = true)
    protected String byteOrder;
    @XmlAttribute(required = true)
    protected String contentType;
    @XmlAttribute(required = true)
    protected String compressionType;
    @XmlAttribute(required = true)
    protected int compressedLen;
}
