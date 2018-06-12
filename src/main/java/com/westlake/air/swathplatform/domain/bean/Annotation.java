package com.westlake.air.swathplatform.domain.bean;

import lombok.Data;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-12 10:40
 *
 * for example
 * b22-18^2/-0.02
 * type=b
 * location=22
 * adjust=-18
 * charge=2
 * deviation=-0.02
 */
@Data
public class Annotation {

    /**
     * a,b,c,x,y,z,
     * http://www.matrixscience.com/help/fragmentation_help.html
     */
    String type;

    /**
     * 切片位置
     */
    int location;

    /**
     * 误差
     */
    double deviation = 0.00;

    /**
     * 默认为1
     */
    int charge = 1;

    /**
     * 校准
     */
    int adjust = 0;

    boolean isIsotope = false;


}
