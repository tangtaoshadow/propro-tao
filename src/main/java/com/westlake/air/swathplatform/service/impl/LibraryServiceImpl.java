package com.westlake.air.swathplatform.service.impl;

import com.westlake.air.swathplatform.constants.ResultCode;
import com.westlake.air.swathplatform.domain.ResultDO;
import com.westlake.air.swathplatform.domain.db.LibraryDO;
import com.westlake.air.swathplatform.repository.LibraryRepo;
import com.westlake.air.swathplatform.service.LibraryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 09:45
 */
@Service("libraryService")
public class LibraryServiceImpl implements LibraryService {

    public final Logger logger = LoggerFactory.getLogger(LibraryServiceImpl.class);

    @Autowired
    LibraryRepo libraryRepo;

    @Override
    public List<LibraryDO> findAll() {
        return libraryRepo.findAll();
    }

    @Override
    public Page<LibraryDO> findAll(PageRequest pageRequest) {
        return libraryRepo.findAll(pageRequest);
    }

    @Override
    public Page<LibraryDO> findAllByName(String name, PageRequest pageRequest) {
        return libraryRepo.findByName(name, pageRequest);
    }

    @Override
    public ResultDO save(LibraryDO libraryDO) {
        if(libraryDO.getName() == null || libraryDO.getName().isEmpty()){
            return ResultDO.buildError(ResultCode.LIBRARY_NAME_CANNOT_BE_EMPTY);
        }
        try{
            libraryDO.setCreateDate(new Date());
            libraryDO.setLastModifiedDate(new Date());
            libraryRepo.insert(libraryDO);
            return ResultDO.build(libraryDO);
        }catch (Exception e){
            logger.warn(e.getMessage());
            ResultDO resultDO = new ResultDO(false);
            resultDO.setErrorResult(ResultCode.INSERT_ERROR.getCode(),e.getMessage());
            return resultDO;
        }
    }

    @Override
    public ResultDO update(LibraryDO libraryDO) {
        if(libraryDO.getId() == null || libraryDO.getId().isEmpty()){
            return ResultDO.buildError(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
        if(libraryDO.getName() == null || libraryDO.getName().isEmpty()){
            return ResultDO.buildError(ResultCode.LIBRARY_NAME_CANNOT_BE_EMPTY);
        }

        try{
            libraryDO.setLastModifiedDate(new Date());
            libraryRepo.save(libraryDO);
            return ResultDO.build(libraryDO);
        }catch (Exception e){
            logger.warn(e.getMessage());
            ResultDO resultDO = new ResultDO(false);
            resultDO.setErrorResult(ResultCode.INSERT_ERROR.getCode(),e.getMessage());
            return resultDO;
        }
    }

    @Override
    public ResultDO delete(String id) {
        if(id == null || id.isEmpty()){
            return ResultDO.buildError(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
        try{
            libraryRepo.deleteById(id);
            return new ResultDO(true);
        }catch (Exception e){
            logger.warn(e.getMessage());
            ResultDO resultDO = new ResultDO(false);
            resultDO.setErrorResult(ResultCode.DELETE_ERROR.getCode(),e.getMessage());
            return resultDO;
        }
    }

    @Override
    public ResultDO<LibraryDO> getById(String id) {
        if(id == null || id.isEmpty()){
            return ResultDO.buildError(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }

        try{
            LibraryDO libraryDO = libraryRepo.getById(id);
            if(libraryDO == null){
                return ResultDO.buildError(ResultCode.OBJECT_NOT_EXISTED);
            }else{
                return ResultDO.build(libraryDO);
            }
        }catch (Exception e){
            logger.warn(e.getMessage());
            ResultDO resultDO = new ResultDO(false);
            resultDO.setErrorResult(ResultCode.QUERY_ERROR.getCode(),e.getMessage());
            return resultDO;
        }
    }

    @Override
    public ResultDO<LibraryDO> getByName(String name) {
        if(name == null || name.isEmpty()){
            return ResultDO.buildError(ResultCode.LIBRARY_NAME_CANNOT_BE_EMPTY);
        }

        try{
            LibraryDO libraryDO = libraryRepo.getByName(name);
            if(libraryDO == null){
                return ResultDO.buildError(ResultCode.OBJECT_NOT_EXISTED);
            }else{
                return ResultDO.build(libraryDO);
            }
        }catch (Exception e){
            logger.warn(e.getMessage());
            ResultDO resultDO = new ResultDO(false);
            resultDO.setErrorResult(ResultCode.QUERY_ERROR.getCode(),e.getMessage());
            return resultDO;
        }
    }
}
