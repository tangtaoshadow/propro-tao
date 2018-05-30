package com.westlake.air.swathplatform.domain.traml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import lombok.Data;

import java.util.List;

/**
 * Instrument configuration used in the testing, validation or optimization of the transitions
 */
@Data
@XStreamAlias("Configuration")
public class Configuration {

    @XStreamImplicit(itemFieldName="cvParam")
    List<CvParam> cvParams;

    @XStreamImplicit(itemFieldName="userParam")
    List<UserParam> userParams;

    @XStreamImplicit(itemFieldName = "ValidationStatus")
    List<ValidationStatus> validationStatusList;
}
