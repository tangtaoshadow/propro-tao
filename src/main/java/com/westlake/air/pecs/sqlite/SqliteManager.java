package com.westlake.air.pecs.sqlite;

import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component("sqliteManager")
public class SqliteManager {

    public static String testUrl = "jdbc:sqlite:D:\\data\\airus_data\\airus_data.sqlite";
    public final Logger logger = LoggerFactory.getLogger(SqliteManager.class);

    private ConcurrentHashMap<String, Connection> connectionMap = new ConcurrentHashMap<>();

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

    public void insert(String url, ExperimentDO experimentDO, List<ScanIndexDO> scanIndexList){
        Connection c = null;
        try {
            c = getConnection(url);
            c.setAutoCommit(false);
            Statement stmt = c.createStatement();
            ResultSet resultSet = getFileInfo(stmt);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if(c != null){
                try {
                    c.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private ResultSet getFileInfo(Statement stmt) throws SQLException {
        stmt.executeQuery("SELECT * FROM main.file_info ");
        return null;
    }

}
