package com.westlake.air.pecs.service;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.LibraryDO;
import com.westlake.air.pecs.domain.query.LibraryQuery;

import java.util.List;


/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 09:36
 */
public interface LibraryService {

    ResultDO<List<LibraryDO>> getList(LibraryQuery query);

    List<LibraryDO> getAll();

    List<LibraryDO> getSimpleAll(Integer type);

    ResultDO save(LibraryDO libraryDO);

    ResultDO update(LibraryDO libraryDO);

    ResultDO delete(String id);

    ResultDO<LibraryDO> getById(String id);

    ResultDO<LibraryDO> getByName(String name);
}
