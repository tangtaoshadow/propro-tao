package com.westlake.air.swathplatform.service;

import com.westlake.air.swathplatform.domain.ResultDO;
import com.westlake.air.swathplatform.domain.db.LibraryDO;
import com.westlake.air.swathplatform.domain.query.LibraryQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;


/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 09:36
 */
public interface LibraryService {

    ResultDO<List<LibraryDO>> getList(LibraryQuery query);

    List<LibraryDO> getAll();

    ResultDO save(LibraryDO libraryDO);

    ResultDO update(LibraryDO libraryDO);

    ResultDO delete(String id);

    ResultDO<LibraryDO> getById(String id);

    ResultDO<LibraryDO> getByName(String name);
}
