package com.westlake.air.swathplatform.service;

import com.westlake.air.swathplatform.domain.ResultDO;
import com.westlake.air.swathplatform.domain.db.LibraryDO;
import com.westlake.air.swathplatform.domain.traml.Peptide;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;


/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 09:36
 */
public interface DecoyGeneratorService {

    Peptide reverse(Peptide peptide);

    Peptide pseudoReverse(Peptide peptide);

    Peptide shuffle(Peptide peptide);
}
