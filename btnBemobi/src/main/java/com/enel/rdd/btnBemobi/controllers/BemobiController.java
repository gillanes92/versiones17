package com.enel.rdd.btnBemobi.controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.enel.rdd.btnBemobi.dao.impl.CarritoDaoImpl;
import com.enel.rdd.btnBemobi.utils.AES;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@RestController
@RequestMapping("/bemobi")
public class BemobiController {
	
	
	@GetMapping({ "/callService" })
	public void callService(@RequestParam String token, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		
		
		System.out.println("ENTRANDO A BTNBEMOBI CALLSERVICE");
		
		Map<String, String> parametros = desencriptaToken(token);
		
		String resp = "";
		try {

			String properties = "btnBemobi.properties";
			String ambiente = getPropertie("ambiente", properties);
			String url_token = getPropertie("url_token_"+ambiente, properties);
			String username = getPropertie("username_"+ambiente, properties);
			String password = getPropertie("password_"+ambiente, properties);
			
			
			String authString = "Authorization: Basic " + org.apache.commons.codec.binary.Base64.encodeBase64String(String.format("%s:%s", username, password).getBytes());
			
			String[] commandToken = { "curl", "-w", ";%{http_code}", "--location", "--request", "POST", url_token, "--header", authString };

			System.out.println("CMD URL : " + Arrays.toString(commandToken));

			String resultToken = consulta(commandToken);

			System.out.println(resultToken);
			
			String tokenResult = resultToken.split(";")[0];
			String tokenCode = resultToken.split(";")[1];

			if(tokenCode.equals("200")) {
				
				JSONObject json = new JSONObject(tokenResult);
				
				String token_type = json.get("token_type").toString();
				String access_token = json.get("access_token").toString();
				
				String channel = getPropertie("channel_"+ambiente, properties);
				String corporate_id = getPropertie("corporate_id_"+ambiente, properties);
				String corporate_name = getPropertie("corporate_name_"+ambiente, properties);
				String url_crear = getPropertie("url_crear_"+ambiente, properties);
				String rut_enel= getPropertie("rut_enel", properties);
				String url_termino= getPropertie("url_termino_"+ambiente,properties);
				
				String monto = parametros.get("Monto");	
				String id_carro = parametros.get("Id_Carro");
				//String suministro = parametros.get("Num_Cliente");
				
				Date date = new Date();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM");
		        String fecha_a_m = sdf.format(date);
		        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd");
		        String fecha_a_m_d = sd.format(date);
				
				String body = "{"
						+ "    \\\"channel\\\": \\\""+channel+"\\\","
						+ "    \\\"corporate\\\": {"
						+ "        \\\"id\\\": \\\""+corporate_id+"\\\","
						+ "        \\\"name\\\": \\\""+corporate_name+"\\\""
						+ "    },"
						+ "    \\\"customer\\\": {"
						+ "        \\\"unit\\\": \\\""+id_carro+"\\\","
						+ "        \\\"document_number\\\": \\\""+rut_enel+"\\\","
						+ "        \\\"document_type\\\": \\\"RUT\\\""
						+ "    },"
						+ "    \\\"invoices\\\": ["
						+ "        {"
						+ "            \\\"id\\\": \\\""+id_carro+"\\\","
						+ "            \\\"reference_month\\\": \\\""+fecha_a_m+"\\\","
						+ "            \\\"due_date\\\": \\\""+fecha_a_m_d+"\\\","
						+ "            \\\"amount\\\": {"
						+ "                \\\"value\\\": "+monto+"00,"
						+ "                \\\"currency_code\\\": \\\"CLP\\\""
						+ "            }"
						+ "        }"
						+ "    ],"
						+ "    \\\"extra_params\\\": {"
						+ "        \\\"url_success\\\": \\\""+url_termino+id_carro+"&estado=OK\\\","
						+ "        \\\"url_error\\\": \\\""+url_termino+id_carro+"&estado=ER\\\","
						+ "        \\\"payment_method\\\": \\\"CREDIT\\\""
						+ "    }"
						+ "}";
				
				String[] commandRegistro = { "curl", "-w", ";%{http_code}", "--location", "--request", "POST", url_crear,
						"--header", "Authorization: "+token_type+" "+access_token , "--header", "Content-Type: application/json",
						"--data", body };

				System.out.println("CMD URL : " + Arrays.toString(commandRegistro));

				String resultRegistro = consulta(commandRegistro);

				System.out.println(resultRegistro);
				
				String registroResult = resultRegistro.split(";")[0];
				String registroCode = resultRegistro.split(";")[1];
				
				if(registroCode.equals("201") && (new JSONObject(registroResult)).has("sessionToken")) {
					
					json = new JSONObject(registroResult);
					
					resp = getPropertie("url_web_" + ambiente, properties) + json.get("sessionToken").toString();
					
				}else {
					String comprobante = parametros.get("Comprobante");
					Map<String, String> comprob = desencriptaToken(comprobante);
					resp = comprob.get("url_response")+"&transaction_status=ERR";
				}
				
			} else {
				
				String comprobante = parametros.get("Comprobante");
				Map<String, String> comprob = desencriptaToken(comprobante);
				resp = comprob.get("url_response") + "&transaction_status=ERR";
				
			}

			response.sendRedirect(resp);

		} catch (Exception e) {
			// TODO: handle exception

			e.printStackTrace();

			String comprobante = parametros.get("Comprobante");
			Map<String, String> comprob = desencriptaToken(comprobante);
			resp = comprob.get("url_response") + "&transaction_status=ERR";

			response.sendRedirect(resp);
		}

		
	}
	
	@GetMapping({ "/Termino" })
	public void termino(@RequestParam String id_transaccion, @RequestParam String estado, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		
		System.out.println("ENTRANDO A BTNBEMOBI TERMINO");

		String properties = "btnBemobi.properties";
		
		CarritoDaoImpl carritoDao = new CarritoDaoImpl();
		InitialContext initi = null;
		try {
			initi = new InitialContext();
		} catch (NamingException e2) {
			e2.printStackTrace();
		}
		DataSource dataSourceBTN = null;
		try {
			dataSourceBTN = (DataSource) initi.lookup("java:/CHILECTRA_BTN");
		} catch (NamingException e1) {
			e1.printStackTrace();
		}
		
		carritoDao.setDataSourceBTN(dataSourceBTN);
		
		try {
			
			System.out.println("id_transaccion: "+id_transaccion);
			System.out.println("estado: "+estado);
			
			Map<String, String> parametros = null;
			
			Map<String, Object> urlMap = carritoDao.obtenerUrl_OK(id_transaccion);

			String id_cart = "", tokenComprobante = "";

			if (urlMap.containsKey("out_cod_retorno") && urlMap.get("out_cod_retorno").equals("00")) {

				id_cart = (String) urlMap.get("id_cart");
				String url_respuesta = (String) urlMap.get("url");
				System.out.println("url_respuesta_cp: " + url_respuesta);

				if(url_respuesta.contains("btnRDD")) {
					
					tokenComprobante = url_respuesta.substring(url_respuesta.indexOf("token=")+"token=".length(), url_respuesta.length());
					
					parametros = desencriptaToken(tokenComprobante);
				}else {
					tokenComprobante = url_respuesta;
					
					parametros = desencriptaToken(tokenComprobante);
				}
			}
			
			if (estado.equals("OK")) {				
					
				String url_comprobante = getPropertie("url_comprobante", properties);
					
				String ambiente = getPropertie("ambiente", properties);
				if (ambiente.equals("QA")) {
					url_comprobante = "https://rddqa.enel.com" + url_comprobante;
				} else {
					url_comprobante = "https://rdd.enel.com" + url_comprobante;
				}
				
				Thread.sleep(Long.valueOf(getPropertie("sleep", properties)));
				
				response.sendRedirect(url_comprobante + "?transaction_id=" + id_cart);

			} else {
				
				String url_response_rechazo =  parametros.get("url_response")+"&transaction_status=ERR";
				String url_img_collector = parametros.get("url_img_collector");
				Date date = new Date();
				SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyy HH:mm:ss");
		        String fecha_pago_recaudador = sdf.format(date);
		        String titulo = acentos(getPropertie("titulo_rechazo_error", properties));
				String nombre = parametros.get("nombre");
				String mensaje = acentos(getPropertie("mensaje_rechazo_error", properties));
				String currency_symbol = parametros.get("currency_symbol");
				String monto_pagado = parametros.get("monto_pagado");
				String num_transaccion = parametros.get("num_transaccion");
				String medio_de_pago = parametros.get("btn_name");
				String observaciones = acentos(getPropertie("observacion_rechazo_error", properties));
				
				
				request.setAttribute("url_response", url_response_rechazo);
				request.setAttribute("url_img_collector", url_img_collector);
				request.setAttribute("fecha_pago_recaudador", fecha_pago_recaudador);
				request.setAttribute("titulo", titulo);
				request.setAttribute("nombre", nombre);
				request.setAttribute("mensaje", mensaje);
				request.setAttribute("currency_symbol", currency_symbol);
				request.setAttribute("monto_pagado", monto_pagado);
				request.setAttribute("num_transaccion", num_transaccion);
				request.setAttribute("medio_de_pago", medio_de_pago);
				request.setAttribute("observaciones", observaciones);
				
				
				request.getRequestDispatcher("/comprobante_error.jsp").forward(request, response);
			}

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
	}
	
	@PostMapping({ "/Notifica" })
	public ResponseEntity<String> notifica(InputStream incomingData, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		
		System.out.println("ENTRANDO A BTNBEMOBI NOTIFICA");
		
		CarritoDaoImpl carritoDao = new CarritoDaoImpl();
		InitialContext initi = null;
		try {
			initi = new InitialContext();
		} catch (NamingException e2) {
			e2.printStackTrace();
		}
		DataSource dataSourceBTN = null;
		try {
			dataSourceBTN = (DataSource) initi.lookup("java:/local");
		} catch (NamingException e1) {
			e1.printStackTrace();
		}
		
		String properties = "btnBemobi.properties";
		
		JSONObject reps = new JSONObject();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(new org.springframework.http.MediaType("application", "json", Charset.forName("UTF-8")));
		
		try {
			
			StringBuilder crunchifyBuilder = new StringBuilder();
			BufferedReader in = new BufferedReader(new InputStreamReader(incomingData));
			String line = null;
			while ((line = in.readLine()) != null) {
				crunchifyBuilder.append(line);
			}
			
			String json = crunchifyBuilder.toString().replace("\\r", "");
			json = json.replace("\\n", "");
			json = json.replace("\\", "");			
						
			JSONObject j = new JSONObject(json);

			System.out.println("json: " + j.toString());
			
			
			if(j.has("estado")) {
				
				String id = j.getString("id");
				String id_trx = j.getString("id_trx");
				String fecha_rendicion = j.getString("fecha_rendicion");				
				String fecha_pago_recaudador=j.getString("fecha_pago_recaudador");
				String hora_pago_recaudador=j.getString("hora_pago_recaudador");
				String monto_deuda=j.getString("monto_deuda");
				String tipo_pago=j.getString("tipo_pago");
				String estado = j.getString("estado");
				
				
				SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd");
				SimpleDateFormat fr = new SimpleDateFormat("yyyyMMdd");
				SimpleDateFormat hp = new SimpleDateFormat("HHmmSS");
		        SimpleDateFormat hpr = new SimpleDateFormat("HH:mm:SS");
				
				fecha_rendicion= sd.format(fr.parse(fecha_rendicion));
				fecha_pago_recaudador= sd.format(fr.parse(fecha_pago_recaudador));
				hora_pago_recaudador=hpr.format(hp.parse(hora_pago_recaudador));
				
				String notificacion = "";
				
				if(estado.equals("OK")) {
					
					Map<String, Object> urlMap = carritoDao.obtenerUrl_OK(id);
					
					String urlRespuesta = "", tokenComprobante = "";
					
					if (urlMap.containsKey("out_cod_retorno") && urlMap.get("out_cod_retorno").equals("00")) {
						System.out.println(urlMap.get("out_desc_retorno"));

						String url_respuesta = (String) urlMap.get("url");
						System.out.println("url_respuesta_cp: " + url_respuesta);

						urlRespuesta = url_respuesta.substring(0, url_respuesta.indexOf("?"));

						tokenComprobante = url_respuesta.substring(url_respuesta.indexOf("token=") + "token=".length(),
								url_respuesta.length());
					}
					String token = AES.decrypt(tokenComprobante);
					
					token = modificaParametro(token, "cod_recaudador", tipo_pago.equals("CRE") ? getPropertie("recaudador_cre", properties) : getPropertie("recaudador_deb", properties));
					
					token += "&fecha_pago_recaudador=" + fecha_pago_recaudador + " " + hora_pago_recaudador;
					token += "&fecha_rendicion=" + fecha_rendicion + " " + hora_pago_recaudador;
					token += "&formato_fecha=yyyy-MM-dd HH:mm:ss";
					token += "&monto_pago=" + monto_deuda.substring(0, monto_deuda.length() - 2);
					token += "&sesion_bco=";
					token += "&transaction_status=OK";

					String B64Notif = new String(Base64.getEncoder().encodeToString(token.getBytes()));
					
					notificacion = notificacion(urlRespuesta, B64Notif);				
					
				}
				
				
				if (notificacion.contains("EXITOSA")) {
					reps.put("message", "OK");
					return new ResponseEntity<>(reps.toString(),headers, HttpStatus.OK);
					
				}else {
					reps.put("message", "ER");
					return new ResponseEntity<>(reps.toString(),headers, HttpStatus.NOT_ACCEPTABLE);					
				}				
				
			}
			
			reps.put("message", getPropertie("msg_falta_dato", properties));
			return new ResponseEntity<>(reps.toString(),headers, HttpStatus.BAD_REQUEST);			

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			reps.put("message", getPropertie("msg_falla_servicio", properties));
			return new ResponseEntity<>(reps.toString(),headers, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
	}
	
	
	public Map<String, String> desencriptaToken(String token) {

		if (token != null) {
			token = token.replace("\\r", "");
			token = token.replace("\\n", "");
			token = token.replace("\\", "");
			token = token.replace(" ", "+");
		}

		token = AES.decrypt(token);

		String[] values = token.split("&", -1);

		Map<String, String> parametros = new HashMap<String, String>();

		for (int i = 0; i < values.length; i++) {

			try {

				String key = values[i].substring(0, values[i].indexOf("="));
				String value = values[i].substring(values[i].indexOf("=") + 1, values[i].length());

				parametros.put(key, value);
				System.out.println(key+": "+value);
			} catch (Exception ex) {

			}
		}
		return parametros;
	}
	
	private static String consulta(String[] comando) {

		ProcessBuilder process = new ProcessBuilder(comando);
		Process p = null;
		BufferedReader reader = null;
		StringBuilder builder = null;
		try {
			p = process.start();
			reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			p.destroy();
			try {
				reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return replaceCurl(builder.toString());

	}

	public static String replaceCurl(String result) {
		result = result.replace("\r", "");
		result = result.replace("\n", "");
		result = result.replace("\\", "");
		return result;
	}
	
	public String getPropertie(String Key, String arch_propertie) {
		System.out.println("getPropertie: " + arch_propertie);
		InputStream input = null;
		Properties prop = new Properties();
		File confDir = new File(System.getProperty("jboss.server.config.dir"));
		File fileProp = new File(confDir, arch_propertie);
		String result = null;
		try {
			input = new FileInputStream(fileProp);
			if (input != null) {
				prop.load(input);
				result = prop.getProperty(Key);
			}
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return "";
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return "";
		}
		System.out.println("resultPropertie: " + result);
		return result;
	}
	
	private String acentos(String texto) throws UnsupportedEncodingException {
		byte ptext[] = texto.getBytes("ISO-8859-1");

		return texto = new String(ptext, "UTF-8");
	}
	
	public String modificaParametro(String token, String parametro, String nuevoValor){
		
		String tokenNuevo = token;
		
		if(token.indexOf(parametro) != -1){
			
			Integer indiceInicial = token.indexOf(parametro)+(parametro+"=").length();
			String separadorValorAntiguo = token.substring(indiceInicial, token.length());
			Integer delta = separadorValorAntiguo.indexOf("&");
			tokenNuevo = token.substring(0, indiceInicial) + nuevoValor + token.substring(indiceInicial + delta, token.length()); 
		}
 
		return tokenNuevo;
	}
	
	private String notificacion(String urlRespuesta, String B64Notif) throws IOException {

		String[] command = { "curl", "--location", "--request", "GET", urlRespuesta + "?token=" + B64Notif };

		System.out.println("CMD : " + Arrays.toString(command));
		ProcessBuilder process = new ProcessBuilder(command);
		Process p;
		p = process.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		StringBuilder builder = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			builder.append(line);
			builder.append(System.getProperty("line.separator"));
		}
		String result = builder.toString();
		System.out.println("RESULT: " + result);

		return result.trim();
	}
	
}
