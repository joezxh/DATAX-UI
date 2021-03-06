package net.iharding.modules.meta.service;

import java.util.List;

import net.iharding.modules.meta.model.DataSource;
import net.iharding.modules.meta.model.DataSourceWrapper;
import net.iharding.modules.meta.model.MetaProperty;
import net.iharding.modules.meta.model.TreeNode;

import org.guess.core.service.BaseService;

/**
 * 
 * @ClassName: DataSource
 * @Description: DataSourceservice
 * @author zhangxuhui
 * @date 2014-8-5 下午02:04:46
 *
 */
public interface DataSourceService extends BaseService<DataSource, Long> {
	/**
	 * 查询获取数据源信息
	 * 
	 * @param id
	 * @return
	 */
	public DataSourceWrapper getDataSourceWrapper(Long id);

	/**
	 * 保存数据源参数配置信息
	 * 
	 * @param obj
	 * @param properties
	 */
	public void saveSetupParam(DataSource obj, List<MetaProperty> properties);

	/**
	 * 导入数据源元数据
	 * 
	 * @param id
	 * @return
	 */
	public DataSource importMeta(Long id) throws Exception ;
	/**
	 * 根据数据库和数据源定义
	 * @param dsid
	 * @param dbname
	 * @return
	 * @throws Exception
	 */
	public DataSource importDbMeta(Long dsid,String dbname) throws Exception ;
	/**
	 * 根据数据源和表定义
	 * @param dsid
	 * @param dbname
	 * @return
	 * @throws Exception
	 */
	public DataSource importTableMeta(Long dsid,String dbname,String tableName) throws Exception ;
	
	public List<MetaProperty> getProperties(Integer dbtype,Long id);
	
	/**
	 * 获取数据源
	 * @return
	 */
	public List<DataSource> getCDataSources();
	
	/**
	 * 获取元数据树
	 * @return
	 */
	public List<TreeNode> getMetaDBTree();

}
