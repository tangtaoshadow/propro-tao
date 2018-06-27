package com.westlake.air.swathplatform.domain.db;

import com.westlake.air.swathplatform.domain.BaseDO;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotEmpty;
import java.util.Date;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-04 21:16
 */
@Data
@Document(collection = "library")
public class LibraryDO extends BaseDO {

    private static final long serialVersionUID = -3258829839160856625L;

    @Id
    String id;

    @Indexed(unique = true)
    @NotEmpty(message = "name cannot be empty!!!")
    String name;

    String description;

    String instrument;

    Long proteinCount;

    Long peptideCount;

    Long totalCount;

    Long totalTargetCount;

    Long totalDecoyCount;

    Date createDate;

    Date lastModifiedDate;
}
