package com.ibm.common.core.mybatis.mapper;

import java.util.List;
import java.util.Map;

public interface BaseMapper<T> {

	public List<T> queryList(T t);
	
	public List<T> queryListByMap(Map<String, Object> searchMap);

	public T queryDetail(T t);
	
	public T queryDetailByMap(Map<String, Object> searchMap);

	public int insertDetail(T t);

	public int updateDetail(T t);
	
	public int updateDetailByMap(Map<String, Object> updateMap);

	public int deleteDetail(T t);
	
	public int deleteDetailByMap(Map<String, Object> updateMap);
	
	public int insertBatchByMap(Map<String, Object> insertMap);
	
	public int insertBatch(List<T> tList);
}