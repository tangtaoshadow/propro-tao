package com.westlake.air.swathplatform.domain.query;

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
public class LibraryQuery extends PageQuery {

    private static final long serialVersionUID = -3258829839160856625L;

    String id;

    String name;

    Date createDate;

    Date lastModifiedDate;
}
