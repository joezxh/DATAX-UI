package net.iharding.modules.meta.reverse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.iharding.modules.meta.model.Dataset;
import net.iharding.modules.meta.model.DataSource;
import net.iharding.modules.meta.model.Database;
import net.iharding.modules.meta.model.DbColumn;
import net.iharding.modules.meta.model.MetaProperty;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.guess.sys.model.User;
import org.springframework.stereotype.Repository;

@Repository("MetaReverse12")
public class PhoenixMetaReverseImpl  extends JDBCMetaReverse {
	
	ObjectMapper mapper = new ObjectMapper();  

	@Override
	public DataSource reverseMeta(DataSource datasource, List<MetaProperty> mproes,User cuser) {
		Connection conn = null;
		ResultSet rsSchema = null;
		Statement stmt = null;
		try {
			conn = getConnection(mproes);
			stmt = conn.createStatement();
			rsSchema = stmt.executeQuery("select distinct TABLE_SCHEM from SYSTEM.CATALOG where  COLUMN_NAME is null");
			while (rsSchema.next()) {
				datasource.addDatabase(reverseDatabaseMeta(datasource, mproes, cuser, rsSchema.getString("TABLE_SCHEM")));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			closeResult(rsSchema);
			closeStatement(stmt);
			closeConnection(conn);
		}	
		return datasource;
	}
	@Override
	protected Connection getConnection(List<MetaProperty> mproes) throws ClassNotFoundException, SQLException {
		Class.forName(getMetaProperty("jdbcdriver", mproes).getPropertyValue());
		String jdbcurl=getMetaProperty("jdbcurl", mproes).getPropertyValue();
		return DriverManager.getConnection(jdbcurl);
	}

	@Override
	public Database reverseDatabaseMeta(DataSource datasource, List<MetaProperty> mproes, User cuser, String dbName) {
		Connection conn = null;
		ResultSet rs = null;
		Database db = null;
		Statement stmt = null;
		try {
			conn = getConnection(mproes);
			db = datasource.getDatabase(dbName, cuser);
			db.setDatasource(datasource);
			db.setDbname(dbName);
			db.setCheckLabel(1);
			stmt = conn.createStatement();
			db.setSchemaName(dbName);
			db.setRemark(db.getDbname());
			rs =stmt.executeQuery("select TABLE_NAME,TABLE_TYPE,COLUMN_COUNT,REMARKS from SYSTEM.CATALOG where  TABLE_SCHEM='"+dbName+"' and COLUMN_NAME is null");
			while (rs.next()) {
				String tableName = rs.getString("TABLE_NAME");
				if (StringUtils.isNotEmpty(tableName)) {
					Dataset table = this.reverseTableMeta(datasource, mproes, cuser, db,stmt, rs, tableName);
					db.addTable(table);
				}
			}
			rs.close();
			datasource.addDatabase(db);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResult(rs);
			closeStatement(stmt);
			closeConnection(conn);
		}
		return db;
	}
	
	private String getResults(List<MetaProperty> mproes,Dataset table){
		Statement stmt = null;
		ResultSet rs = null;
		Connection conn = null;
		try {
			conn = getConnection(mproes);
			stmt=conn.createStatement();
			rs=stmt.executeQuery("select * from "+table.getDatasetName());
			List<Map<String,String>> rows=new ArrayList<Map<String,String>>();
			int i=0;
			while (rs.next()) {
				Map<String,String> row=new HashMap<String,String>();
				for(DbColumn column:table.getColumns()){
					row.put(column.getColumnName(),rs.getString(column.getColumnName()));
				}
				rows.add(row);
				i++;
				if (i>10)break;
			}
			return mapper.writeValueAsString(rows);
		} catch (SQLException | ClassNotFoundException | IOException e) {
			e.printStackTrace();
		} finally {
			closeResult(rs);
			this.closeStatement(stmt);
			this.closeConnection(conn);
		}
		return null;
	}


	private Dataset reverseTableMeta(DataSource datasource, List<MetaProperty> mproes, User cuser, Database db,Statement stmt,  ResultSet rs, String tableName) {
		ResultSet rsColumn = null;
		Dataset table = null;
		try {
			if (StringUtils.isNotEmpty(tableName)) {
				table = db.getDBTable(tableName, cuser);// new DBTable();
				table.setDatasetName(tableName);
				table.setCheckLabel(1);
				String tablecomment = rs.getString("REMARKS");
				if (StringUtils.isNotEmpty(tablecomment)) {
					table.setDatasetPname(tablecomment);
					table.setRemark(tablecomment);
				} else {
					table.setDatasetPname(tableName);
					table.setRemark(tableName);
				}
				String tableType = rs.getString("TABLE_TYPE");
				if ("s".equalsIgnoreCase(tableType)) {
					table.setTableType(1);
				} else if ("v".equalsIgnoreCase(tableType)) {
					table.setTableType(2);
				}
				table.setClassName(this.sql2javaName(tableName));
				table.setDatabase(db);
				rsColumn = stmt.executeQuery("select COLUMN_NAME,COLUMN_FAMILY,DATA_TYPE,COLUMN_SIZE,DECIMAL_DIGITS,REMARKS from SYSTEM.CATALOG"
						+" where  TABLE_SCHEM='"+db.getDbname()+"' and TABLE_NAME='"+tableName+"' and COLUMN_NAME is not null");
				while (rsColumn.next()) {
					int colType = rsColumn.getInt("DATA_TYPE");
					String sqlColName = rsColumn.getString("COLUMN_NAME");
					if (StringUtils.isNotEmpty(sqlColName)) {
						DbColumn col = table.getNewDbColumn(sqlColName);
						int colTypeLength = 0;
						int decimalLength = 0;
						String tit = "";
						try {
							colTypeLength = rsColumn.getInt("COLUMN_SIZE");
							decimalLength = rsColumn.getInt("DECIMAL_DIGITS");
							tit = rsColumn.getString("REMARKS");
						} catch (Exception ex) {
						}
						col.setFieldCode(this.sql2javaName(sqlColName));
						col.setColumnName(sqlColName);
						if ("".equalsIgnoreCase(tit) || tit == null) {
							col.setColumnPname(sqlColName);
						} else {
							col.setColumnPname(tit);
						}
						col.setRemark(col.getColumnPname());
						col.setCheckLabel(1);
						col.setDbtable(table);
						col.setType(convert(colType, colTypeLength, decimalLength));
						table.addColumn(col);
					}
				}
				table.setColumnCount(table.getColumns().size());
				table.setSampleRows(this.getResults(mproes, table));
				rsColumn.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResult(rsColumn);
		}
		return table;
	}


	@Override
	public Dataset reverseTableMeta(DataSource datasource, List<MetaProperty> mproes, User cuser, String dbName, String tableName) {
		Connection conn = null;
		ResultSet rsColumn = null;
		ResultSet rs=null;
		Dataset table = null;
		Statement stmt = null;
		try {
			conn = getConnection(mproes);
			Database db = datasource.getDatabase(dbName, cuser);
			db.setDatasource(datasource);
			db.setDbname(dbName);
			db.setCheckLabel(1);
			stmt = conn.createStatement();
			if (StringUtils.isNotEmpty(tableName)) {
				table = db.getDBTable(tableName, cuser);// new DBTable();
				table.setDatasetName(tableName);
				table.setCheckLabel(1);
				rs =stmt.executeQuery("select TABLE_NAME,TABLE_TYPE,COLUMN_COUNT,REMARKS from SYSTEM.CATALOG where  TABLE_SCHEM='"+dbName+"' and COLUMN_NAME is null");
				String tablecomment = rs.getString("REMARKS");
				if (StringUtils.isNotEmpty(tablecomment)) {
					table.setDatasetPname(tablecomment);
					table.setRemark(tablecomment);
				} else {
					table.setDatasetPname(tableName);
					table.setRemark(tableName);
				}
				String tableType = rs.getString("TABLE_TYPE");
				if ("s".equalsIgnoreCase(tableType)) {
					table.setTableType(1);
				} else if ("v".equalsIgnoreCase(tableType)) {
					table.setTableType(2);
				}
				table.setColumnCount(rs.getInt("COLUMN_COUNT"));
				table.setClassName(this.sql2javaName(tableName));
				table.setDatabase(db);
				rsColumn = stmt.executeQuery("select COLUMN_NAME,COLUMN_FAMILY,DATA_TYPE,COLUMN_SIZE,DECIMAL_DIGITS,REMARKS from SYSTEM.CATALOG"
						+" where  TABLE_SCHEM='"+db.getDbname()+"' and TABLE_NAME='"+tableName+"' and COLUMN_NAME is not null");
				while (rsColumn.next()) {
					int colType = rsColumn.getInt("DATA_TYPE");
					String sqlColName = rsColumn.getString("COLUMN_NAME");
					if (StringUtils.isNotEmpty(sqlColName)) {
						DbColumn col = table.getNewDbColumn(sqlColName);
						int colTypeLength = 0;
						int decimalLength = 0;
						String tit = "";
						try {
							colTypeLength = rsColumn.getInt("COLUMN_SIZE");
							decimalLength = rsColumn.getInt("DECIMAL_DIGITS");
							tit = rsColumn.getString("REMARKS");
						} catch (Exception ex) {
						}
						col.setFieldCode(this.sql2javaName(sqlColName));
						col.setColumnName(sqlColName);
						if ("".equalsIgnoreCase(tit) || tit == null) {
							col.setColumnPname(sqlColName);
						} else {
							col.setColumnPname(tit);
						}
						col.setRemark(col.getColumnPname());
						col.setCheckLabel(1);
						col.setDbtable(table);
						col.setType(convert(colType, colTypeLength, decimalLength));
						table.addColumn(col);
					}
				}
				rsColumn.close();
				stmt.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			closeResult(rsColumn);
			closeResult(rs);
			closeStatement(stmt);
			closeConnection(conn);
		}
		return table;
	}
	
	

}
