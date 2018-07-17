package com.westlake.air.swathplatform.domain.bean;

import lombok.Data;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-17 10:16
 */
@Data
public class TargetTransition {

    //对应的transition的Id,如果是MS1的则为对应的第一条transition的Id(一个MS1会对应多条transition记录)
    String transitionId;

    //对应的荷质比,如果是MS1则为肽段的荷质比,如果是MS2则为肽段碎片的荷质比
    Double mz;

    //对应的肽段全名(包含unimod)
    String peptide;

    Double rtStart;

    Double rtEnd;
}
