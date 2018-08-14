package com.westlake.air.pecs.dao;

import com.westlake.air.pecs.domain.db.ConfigDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-14 10:08
 */
@Service
public class ConfigDAO {

    public static String CollectionName = "config";

    public final Logger logger = LoggerFactory.getLogger(ConfigDAO.class);

    @Autowired
    MongoTemplate mongoTemplate;

    public ConfigDO getConfig() {
        return mongoTemplate.findOne(null, ConfigDO.class, CollectionName);
    }

    public Boolean updateConfig(ConfigDO configDO) {

        if (configDO.getId() == null || configDO.getId().isEmpty()) {
            logger.error("Config Id cannot been empty");
            return false;
        }
        try {
            mongoTemplate.save(configDO, CollectionName);
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return false;
        }

    }
}
