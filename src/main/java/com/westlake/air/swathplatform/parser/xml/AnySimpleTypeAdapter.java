package com.westlake.air.swathplatform.parser.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class AnySimpleTypeAdapter extends XmlAdapter<String, String> {

    @Override
    public String unmarshal(String v) throws Exception {
        return v;
    }

    @Override
    public String marshal(String v) throws Exception {
        if (v != null)
            return v;
        else
            return null;
    }
}
