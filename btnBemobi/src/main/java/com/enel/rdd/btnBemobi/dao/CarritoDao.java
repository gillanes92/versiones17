package com.enel.rdd.btnBemobi.dao;

import java.sql.SQLException;
import java.util.Map;

public interface CarritoDao {

	public Map<String, Object> obtenerUrl_OK(String in_id) throws SQLException;
	
}
