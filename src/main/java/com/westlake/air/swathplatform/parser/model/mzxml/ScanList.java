package com.westlake.air.swathplatform.parser.model.mzxml;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"scan"})
@XmlRootElement(name = "scanList")

public class ScanList implements Serializable, MzXMLObject {

    private final static long serialVersionUID = 322L;
    @XmlElement(required = true)
    protected List<Scan> scan;

    public List<Scan> getScan() {
        if (scan == null) {
            scan = new ArrayList<>();
        }
        return scan;
    }

    public void setScan(List<Scan> scan) {
        this.scan = scan;
    }
}
