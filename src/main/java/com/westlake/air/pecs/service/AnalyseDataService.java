package com.westlake.air.pecs.service;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.AnalyseOverviewDO;
import com.westlake.air.pecs.domain.db.simple.TransitionGroup;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.query.AnalyseDataQuery;
import com.westlake.air.pecs.domain.query.PageQuery;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 16:02
 */
public interface AnalyseDataService {

    List<AnalyseDataDO> getAllByOverviewId(String overviewId);

    Long count(AnalyseDataQuery query);

    ResultDO<List<AnalyseDataDO>> getList(AnalyseDataQuery query);

    ResultDO insert(AnalyseDataDO dataDO);

    ResultDO insertAll(List<AnalyseDataDO> convList, boolean isDeleteOld);

    ResultDO delete(String id);

    ResultDO deleteAllByOverviewId(String overviewId);

    ResultDO<AnalyseDataDO> getById(String id);

    /**
     * 获取MS1的卷积卷积信息
     *
     * @param overviewId
     * @param peptideRef
     * @return
     */
    ResultDO<AnalyseDataDO> getMS1Data(String overviewId, String peptideRef);

    /**
     * 获取MS2的卷积数据
     *
     * @param overviewId
     * @param peptideRef
     * @param cutInfo
     * @return
     */
    ResultDO<AnalyseDataDO> getMS2Data(String overviewId, String peptideRef, String cutInfo);

    /**
     * 获取MS2的卷积组数据
     *
     * @param overviewId
     * @param peptideRef
     * @return
     */
    ResultDO<List<AnalyseDataDO>> getMS2DataList(String overviewId, String peptideRef, Boolean isDecoy);

    List<TransitionGroup> getTransitionGroup(List<AnalyseDataDO> dataList);

    /**
     * 分页获取TransitonGroup,本函数只针对卷积实验对应的标准库,如果要使用校准库进行操作,
     * 请调用com.westlake.air.pecs.service.AnalyseDataService#getIrtTransitionGroup(java.lang.String, java.lang.String)函数
     *
     * @param overviewDO
     * @return
     */
    List<TransitionGroup> getTransitionGroup(AnalyseOverviewDO overviewDO);

    /**
     * 获取iRT的TransitionGroup,由于数据量比较小,因此算法采用一次性获取的方式从数据库中读取列表,本函数可以保证所有的库文件中的transition都会出现在
     * 最终的TransitionGroup中,不论其是否在原始数据中被卷积到.
     *
     * @param overviewId
     * @param iRtlibraryId
     * @return
     */
    List<TransitionGroup> getIrtTransitionGroup(String overviewId, String iRtlibraryId);

    List<TransitionGroup> getIrtTransitionGroup(List<AnalyseDataDO> dataList, String iRtlibraryId);


}
