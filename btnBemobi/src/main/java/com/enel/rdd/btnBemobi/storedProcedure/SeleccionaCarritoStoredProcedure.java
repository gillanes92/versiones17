package com.enel.rdd.btnBemobi.storedProcedure;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

public class SeleccionaCarritoStoredProcedure {

	private CallableStatement callableStatement = null;
	private Connection conn = null;
	
	public SeleccionaCarritoStoredProcedure(Connection conn) throws SQLException{
		
		this.conn = conn;
		this.conn.setAutoCommit(false);
		callableStatement = this.conn.prepareCall("{? = call trxconc.sp_rlt_sel_cart_id(?) }");
 
	}
	
	public Map<String, Object> execute(Integer in_id){
		
		Map<String,Object> outParams = new HashMap<String, Object>();
		String id_cart = "", url = "";
 	
		try {
			
			callableStatement.setInt(1, in_id);
			
			callableStatement.registerOutParameter(1, Types.VARCHAR);
			callableStatement.registerOutParameter(2, Types.OTHER);

			callableStatement.execute();
			
 			outParams.put("out_cod_retorno", callableStatement.getString(1));
			
			ResultSet rs = (ResultSet) callableStatement.getObject(2);
			
			while (rs.next()) {
				id_cart= rs.getString("id_cart");
				url = rs.getString("url");
			}
			
			outParams.put("id_cart", id_cart);
			outParams.put("url", url);
			
			callableStatement.close();
			this.conn.close();
 
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			
			try {
				
				if(callableStatement != null) 
					callableStatement.close();
			
				if(this.conn != null)
					this.conn.close();
				
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
		}
		
		return outParams;
		
	}

}
