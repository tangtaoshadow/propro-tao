package com.westlake.air.swathplatform.parser.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class NonNegativeIntegerAdapter extends XmlAdapter<String, Long> {

    @Override
    public Long unmarshal(String v) throws Exception {
        return Long.valueOf(v);
    }

    @Override
    public String marshal(Long v) throws Exception {
        if (v != null)
            return v.toString();
        else
            return null;
    }
}
