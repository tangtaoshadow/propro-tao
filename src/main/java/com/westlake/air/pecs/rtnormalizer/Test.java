package com.westlake.air.pecs.rtnormalizer;

import com.westlake.air.pecs.domain.bean.RtIntensityPairs;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-02 20：37
 */
public class Test {
    public static void main(String args[]){

        // gaussFilter test
        Float[] rt = new Float[9];
        Float[] intensity = new Float[9];
        for(int i = 0; i<9;i++){
            rt[i] = 500f + 0.03f*i;
            intensity[i] = 0f;
        }
        intensity[3] = 1.0f;
        intensity[4] = 0.8f;
        intensity[5] = 1.2f;
        RtIntensityPairs rtIntensityPairs = new RtIntensityPairs(rt, intensity);

        RtIntensityPairs filteredPairs = new GaussFilter().filter(rtIntensityPairs, 30/8, 0.01f);

        System.out.println("Test Finish.");
    }
}
