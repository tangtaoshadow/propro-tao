package com.westlake.air.pecs.service;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.LibraryDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.domain.query.LibraryQuery;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
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

    String getNameById(String id);

    ResultDO parseAndInsertTsv(LibraryDO library, InputStream in, String fileName, boolean justReal, TaskDO taskDO);

    void countAndUpdateForLibrary(LibraryDO library);

    void uploadFile(LibraryDO library, InputStream in, String fileName, Boolean justReal, TaskDO taskDO);
}
