package com.westlake.air.pecs.domain.db;

import com.westlake.air.pecs.domain.BaseDO;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 16:20
 */
@Data
@Document(collection = "analyseOverview")
public class AnalyseOverviewDO extends BaseDO {

    @Id
    String id;

    //对应实验的id
    String expId;

    //对应实验的名称
    String expName;

    //是否包含卷积结果文件
    Boolean hasAircFile;
    //卷积结果文件路径
    String aircPath;
    //卷积结果索引文件路径
    String aircIndexPath;

    //卷积代号
    String name;

    //标签
    String label;

    //标准库ID
    String libraryId;

    //标准库名称
    String libraryName;

    //使用的斜率,一般来源于experimentDO对象,也可以在执行单独的卷积步骤自由设定
    Double slope;

    //使用的截距,一般来源于experimentDO对象,也可以在执行单独的卷积步骤自由设定
    Double intercept;

    //在打分时设定的shape分的阈值,如果低于这个阈值的峰组会被直接忽略掉,从而节省运算时间
    Float shapeScoreThreshold;
    //在打分时设定的shapeWeight分的阈值,如果低于这个阈值的峰组会被直接忽略掉,从而节省运算时间
    Float shapeScoreWeightThreshold;

    //rt卷积窗口,一般设定为600或者800,一般来源于experimentDO对象,也可以在执行单独的卷积步骤自由设定
    Float rtExtractWindow;

    //mz卷积窗口,一般设定为0.05或者0.03,一般来源于experimentDO对象,也可以在执行单独的卷积步骤自由设定
    Float mzExtractWindow;

    //卷积实验的创建者
    String creator = "Admin";

    //最终使用的分类器:lda, xgboost
    String classifier = "lda";

    //在计算高斯平滑时使用的Sigma值,一般为6.25
    Float sigma;

    //在计算高斯平滑时使用的Sigma值,一般为0.01
    Float spacing;

    //卷积实验的创建时间
    Date createDate;

    //卷积实验的最后一次修改时间
    Date lastModifiedDate;

    //最终计算所得的子分数的权重,LDA算法才有
    HashMap<String, Double> weights = new HashMap<>();

    //最终计算鉴定到的肽段数目
    Integer matchedPeptideCount;

    //最终卷积到的肽段数目
    Integer totalPeptideCount;

    //对应标准库中的肽段数目
    Integer libraryPeptideCount;

    //备忘录
    String note;
}
