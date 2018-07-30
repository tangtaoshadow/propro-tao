package com.westlake.air.pecs.rtnormalizer;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.LibraryDO;
import com.westlake.air.pecs.domain.db.TransitionDO;
import com.westlake.air.pecs.domain.query.LibraryQuery;
import com.westlake.air.pecs.domain.query.TransitionQuery;
import com.westlake.air.pecs.service.TransitionService;
import com.westlake.air.pecs.service.impl.LibraryServiceImpl;
import com.westlake.air.pecs.service.impl.TransitionServiceImpl;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-07-28 21-30
 */
public class RTNormalizeUtil extends Params {

    public static String getLibraryId(String libraryName){
        LibraryQuery libraryQuery = new LibraryQuery();
        LibraryServiceImpl libraryService = new LibraryServiceImpl();
        libraryQuery.setName(libraryName);
        ResultDO<List<LibraryDO>> resultDO = libraryService.getList(libraryQuery);
        if(resultDO.isSuccess()){
            return resultDO.getModel().get(0).getId();
        }else{
            System.out.println(resultDO.getErrorList());
            return null;
        }
    }

    public static float[] getRTRange(String libraryId){
        TransitionServiceImpl transitionService = new TransitionServiceImpl();
        List<TransitionDO> transitionDOList = transitionService.getAllByLibraryId(libraryId);
        float[] rtRange = new float[2];
        rtRange[0] = Float.MAX_VALUE; //min RT
        rtRange[1] = Float.MIN_VALUE; //max RT
        for(TransitionDO transitionDO: transitionDOList){
            double rt = transitionDO.getRt();
            if(rt<rtRange[0]) rtRange[0]=(float)rt;
            if(rt>rtRange[1]) rtRange[1]=(float)rt;
        }
        return rtRange;
    }

    public static TransitionDO getTransitionDOById(String libraryId, String id){
        TransitionQuery transitionQuery = new TransitionQuery();
        TransitionServiceImpl transitionService = new TransitionServiceImpl();
        transitionQuery.setId(id);
        transitionQuery.setLibraryId(libraryId);
        ResultDO<List<TransitionDO>> resultDO = transitionService.getList(transitionQuery);
        if(resultDO.isSuccess()){
            return resultDO.getModel().get(0);
        }else {
            System.out.println(resultDO.getErrorList());
            return null;
        }
    }

    public static float getRsq(List<float[]> pairs){
        //step1 compute mean
        float sumX = 0, sumY = 0;
        int length = pairs.size();
        for(float[] pair : pairs){
            sumX += pair[0];
            sumY += pair[1];
        }
        float meanX = sumX / length;
        float meanY = sumY / length;

        //step2 compute variance
        sumX = 0; sumY = 0;
        for(float[] pair : pairs){
            sumX += Math.pow(pair[0] - meanX, 2);
            sumY += Math.pow(pair[1] - meanY, 2);
        }
        float varX = sumX / (length - 1);
        float varY = sumY / (length - 1);

        //step3 compute covariance
        float sum = 0;
        for(float[] pair: pairs){
            sum += (pair[0] - meanX) * (pair[1] - meanY);
        }
        float covXY = sum / length;

        //step4 calculate R^2
        return (covXY * covXY) / (varX * varY);
    }


}
