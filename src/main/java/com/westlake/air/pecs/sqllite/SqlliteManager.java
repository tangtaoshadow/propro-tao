package com.westlake.air.pecs.sqllite;

import com.westlake.air.pecs.constants.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.ConcurrentHashMap;

@Component("sqlliteManager")
public class SqlliteManager {

    public static String testUrl = "jdbc:sqlite:D:\\data\\airus_data\\airus_data.sqllite";
    public final Logger logger = LoggerFactory.getLogger(SqlliteManager.class);

    ConcurrentHashMap<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public Connection getConnection(String url) {
        if(url == null || url.isEmpty()){
            logger.error(ResultCode.CONNECTION_URL_CANNOT_BE_EMPTY.getMessage());
            return null;
        }
        if (connectionMap.get(url) != null) {
            return connectionMap.get(url);
        } else {
            Connection c = null;

            try {
                c = DriverManager.getConnection(testUrl);
            } catch (Exception e) {
                logger.error(ResultCode.CONNECTION_FAILED.getMessage()+"Url:" + url, e);
            }

            if (c != null) {
                connectionMap.put(url, c);
                System.out.println(ResultCode.CONNECTION_SUCCESS.getMessage()+"Url:" + url);
            } else {
                logger.error(ResultCode.CONNECTION_FAILED.getMessage()+"Url:" + url);
            }
            return c;
        }
    }

    public void removeConnection(String url) {
        connectionMap.remove(url);
    }

}
