package com.enel.rdd.btnBemobi.dao.impl;

import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import com.enel.rdd.btnBemobi.dao.CarritoDao;
import com.enel.rdd.btnBemobi.storedProcedure.SeleccionaCarritoStoredProcedure;

public class CarritoDaoImpl implements CarritoDao  {

	private DataSource dataSourceBTN;
	
	public void setDataSourceBTN(DataSource dataSourceBTN) {
		this.dataSourceBTN = dataSourceBTN;
	}
	
	@Override
	public Map<String, Object> obtenerUrl_OK(String in_id) throws SQLException {
		System.out.println("in_id:"+in_id);
		SeleccionaCarritoStoredProcedure selCarStoredProcedure = new SeleccionaCarritoStoredProcedure(dataSourceBTN.getConnection());
		Map<String, Object> response = selCarStoredProcedure.execute(Integer.valueOf(in_id));
 
		return response;
	}
}
