package com.westlake.air.swathplatform.repository;

import com.westlake.air.swathplatform.domain.db.LibraryDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-04 21:54
 */
public interface LibraryRepo extends MongoRepository<LibraryDO, String> {

    LibraryDO getById(String id);

    LibraryDO getByName(String name);

    Page<LibraryDO> findByName(String name, Pageable page);
}
