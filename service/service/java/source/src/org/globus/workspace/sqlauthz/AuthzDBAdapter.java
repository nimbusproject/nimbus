package org.globus.workspace.sqlauthz;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.Lager;
import org.globus.workspace.persistence.WorkspaceDatabaseException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by John Bresnahan
 * User: bresnaha
 * Date: May 17, 2010
 * Time: 9:38:44 AM
 * <p/>
 * org.globus.workspace.sqlauthz
 */
public class AuthzDBAdapter
{
    private static final String FIND_PARENT_OBJECT_BY_NAME = "Select id from objects where name = ? and parent_id is NULL and object_type = ?";
    private static final String FIND_OBJECT_BY_NAME = "Select id from objects where name = ? and parent_id = ? and object_type = ?";
    private static final String FIND_USER_BY_ALIAS = "Select user_id from user_alias where alias_name = ? and alias_type = ?";
    private static final String CHECK_PERMISSIONS = "Select access_type_id from object_acl where object_id = ? and user_id = ?";
    private static final String GET_DATA_KEY = "select data_key from objects where id = ?";
    private static final String CREATE_NEW_FILE = "insert into objects (name, owner_id, data_key, object_type, parent_id, creation_time) values(?, ?, ?, ?, ?, ?)";

    public static final int ALIAS_TYPE_S3 = 1;
    public static final int ALIAS_TYPE_DN = 2;
    
    public static final int OBJECT_TYPE_S3 = 1;


    private final DataSource            dataSource;

    private static final Log logger =
            LogFactory.getLog(AuthzDBAdapter.class.getName());

    public AuthzDBAdapter(
        DataSource                      dataSourceImpl)
    {
        if (dataSourceImpl == null)
        {
            throw new IllegalArgumentException("dataSourceImpl may not be null");
        }
        this.dataSource = dataSourceImpl;
    }

    public String getCanonicalUserIdFromS3(
        String                          name)
            throws   WorkspaceDatabaseException
    {
          return getCanonicalUserIdFromAlias(name, ALIAS_TYPE_S3);
    }

    public String getCanonicalUserIdFromDn(
        String                          name)
            throws   WorkspaceDatabaseException
    {
          return getCanonicalUserIdFromAlias(name, ALIAS_TYPE_DN);
    }

    private int getParentObject(
        Connection                      c,
        String                          objectName,
        int                             objectType)
            throws WorkspaceDatabaseException, SQLException
    {
        PreparedStatement pstmt = null;

        pstmt = c.prepareStatement(FIND_PARENT_OBJECT_BY_NAME);
        pstmt.setString(1, objectName);
        pstmt.setInt(2, objectType);
        ResultSet rs = pstmt.executeQuery();

        if(!rs.next())
        {
            logger.debug("pstmt " + pstmt.toString());
            pstmt.close();
            throw new WorkspaceDatabaseException("no such parent file found " + objectName + " " + objectType);
        }        
        int objectId = rs.getInt(1);
        pstmt.close();
        return objectId;    
    }

    public String getDataKey(
        int                             objectId)
            throws WorkspaceDatabaseException
    {
        Connection c = null;
        PreparedStatement pstmt = null;

        try
        {
            c = getConnection();
            pstmt = c.prepareStatement(GET_DATA_KEY);
            pstmt.setInt(1, objectId);
            ResultSet rs = pstmt.executeQuery();
            if(!rs.next())
            {
                throw new WorkspaceDatabaseException("no such file id found  " + objectId);
            }
            String dataKey = rs.getString(1);
            return dataKey;
        }
        catch(SQLException e)
        {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        }
        finally
        {
            try
            {
                if (pstmt != null)
                {
                    pstmt.close();
                }
                if (c != null)
                {
                    returnConnection(c);
                }
            }
            catch (SQLException sql)
            {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    public void newFile(
        String                          fileName,
        int                             parentId,
        String                          canonicalUser,
        String                          dataKey,
        int                             objectType)
            throws WorkspaceDatabaseException
    {
        Connection c = null;
        PreparedStatement pstmt = null;

        try
        {
            c = getConnection();
            pstmt = c.prepareStatement(CREATE_NEW_FILE);
            pstmt.setString(1, fileName);
            pstmt.setString(2, canonicalUser);
            pstmt.setString(3, dataKey);
            pstmt.setInt(4, objectType);
            pstmt.setInt(5, parentId);
            int rc = pstmt.executeUpdate();
            if(rc != 1)
            {
                throw new WorkspaceDatabaseException("did not insert the row properly");
            }
        }
        catch(SQLException e)
        {
            logger.error("an error occured looking up the file ", e);
            throw new WorkspaceDatabaseException(e);
        }
        finally
        {
            try
            {
                if (pstmt != null)
                {
                    pstmt.close();
                }
                if (c != null)
                {
                    returnConnection(c);
                }
            }
            catch (SQLException sql)
            {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    public int getFileID(
        String                          fileName,
        int                             parentId,
        int                             objectType)
            throws WorkspaceDatabaseException
    {

        Connection c = null;
        PreparedStatement pstmt = null;

        try
        {
            c = getConnection();
            if(parentId < 0)
            {
               logger.debug("There is no parent object so this must be a parent " + fileName);
               return getParentObject(c, fileName, objectType);
            }
            pstmt = c.prepareStatement(FIND_OBJECT_BY_NAME);
            pstmt.setString(1, fileName);
            pstmt.setInt(2, parentId);
            pstmt.setInt(3, objectType);
            ResultSet rs = pstmt.executeQuery();
            if(!rs.next())
            {
                return -1;
            }
            int objectId = rs.getInt(1);
            return objectId;
        }
        catch(SQLException e)
        {
            logger.error("an error occured looking up the file ", e);
            throw new WorkspaceDatabaseException(e);
        }
        finally
        {
            try
            {
                if (pstmt != null)
                {
                    pstmt.close();
                }
                if (c != null)
                {
                    returnConnection(c);
                }
            }
            catch (SQLException sql)
            {
                logger.error("SQLException in finally cleanup", sql);
            }
        }

    }
    
    public String getPermissions(
        int                             objectId,
        String                          userId)
            throws WorkspaceDatabaseException
    {
        Connection c = null;
        PreparedStatement pstmt = null;

        try
        {
            c = getConnection();
            pstmt = c.prepareStatement(CHECK_PERMISSIONS);
            pstmt.setInt(1, objectId);
            pstmt.setString(2, userId);
            ResultSet rs = pstmt.executeQuery();

            String perms = "";
            while(rs.next())
            {
                String ch = rs.getString(1);
                perms = perms + ch;
            }
            return perms;
        }
        catch(SQLException e)
        {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        }
        finally
        {
            try
            {
                if (pstmt != null)
                {
                    pstmt.close();
                }
                if (c != null)
                {
                    returnConnection(c);
                }
            }
            catch (SQLException sql)
            {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }


    public String getCanonicalUserIdFromAlias(
        String                          name,
        int                             type)
            throws   WorkspaceDatabaseException
    {
        Connection c = null;
        PreparedStatement pstmt = null;

        try
        {
            c = getConnection();
            pstmt = c.prepareStatement(FIND_USER_BY_ALIAS);
            pstmt.setString(1, name);
            pstmt.setInt(2, type);
            logger.debug("getting user " + pstmt.toString());
            ResultSet rs = pstmt.executeQuery();

            if(!rs.next())
            {                
                throw new WorkspaceDatabaseException("no such user found  " + name);
            }
            String canUserId = rs.getString(1);

            return canUserId;

        }
        catch(SQLException e)
        {
            logger.error("",e);
            throw new WorkspaceDatabaseException(e);
        }
        finally
        {
            try
            {
                if (pstmt != null)
                {
                    pstmt.close();
                }
                if (c != null)
                {
                    returnConnection(c);
                }
            }
            catch (SQLException sql)
            {
                logger.error("SQLException in finally cleanup", sql);
            }
        }
    }

    private Connection getConnection() throws SQLException
    {
        return this.dataSource.getConnection();
    }

    private void returnConnection(Connection connection)
    {
        if(connection != null)
        {
            try
            {
                connection.close();
            }
            catch(SQLException e)
            {
                logger.error("",e);
            }
        }
    }
}