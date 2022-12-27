package com.siman.credisiman.visa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.siman.credisiman.visa.dto.bloqueodesbloqueotarjeta.BloqueoDesbloqueoTarjetaResponse;
import com.siman.credisiman.visa.utils.ConnectionHandler;
import com.siman.credisiman.visa.utils.Message;
import com.siman.credisiman.visa.utils.Utils;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class BloqueoDesbloqueoTarjeta {
    private static final Logger log = LoggerFactory.getLogger(BloqueoDesbloqueoTarjeta.class);
    private static final String namespace = "http://siman.com/BloqueoDesbloqueoTarjeta";
    private static final String operationResponse = "ObtenerBloqueoDesbloqueoTarjetaResponse";

    public static XmlObject obtenerBloqueoDesbloqueoTarjeta(String pais, String numeroTarjeta, String estadoDeseado,
                                                            String motivo, String remoteJndiSunnel, String remoteJndiOrion, String siscardUrl, String siscardUser,
                                                            String binCredisiman, String tipoTarjeta) {
        //OBTENER DATOS

        //validar campos requeridos
        Utils utils = new Utils();
        Message message = new Message();

        if (utils.validateNotNull(pais) || utils.validateNotEmpty(pais)) {
            log.info("pais required");
            return message.genericMessage("ERROR", "025", "El campo pais es obligatorio", namespace, operationResponse);
        }
        if (utils.validateNotNull(numeroTarjeta) || utils.validateNotEmpty(numeroTarjeta)) {
            log.info("numero tarjeta required");
            return message.genericMessage("ERROR", "025", "El campo n�mero tarjeta es obligatorio", namespace, operationResponse);
        }
        if (utils.validateNotNull(motivo) || utils.validateNotEmpty(motivo)) {
            log.info("motivo required");
            return message.genericMessage("ERROR", "025", "El campo motivo es obligatorio", namespace, operationResponse);
        }
        if (utils.validateNotNull(estadoDeseado) || utils.validateNotEmpty(estadoDeseado)) {
            log.info("estado deseado required");
            return message.genericMessage("ERROR", "025", "El campo estado deseado es obligatorio", namespace, operationResponse);
        }

        //validar longitudes
        if (!utils.validateLongitude(pais, 3)) {
            log.info("pais, size overload");
            return message.genericMessage("ERROR", "025", "La longitud del campo pais debe ser menor o igual a 3", namespace, operationResponse);
        }
        if (!utils.validateLongitude(numeroTarjeta, 16)) {
            log.info("identificacion, size overload");
            return message.genericMessage("ERROR", "025",
                    "La longitud del campo n�mero tarjeta debe ser menor o igual a 16", namespace, operationResponse);
        }

        try {
            switch (tipoTarjeta) {
                case "P":
                    //datos tarjeta privada
                    BloqueoDesbloqueoTarjetaResponse response1= null;
                    response1 = obtenerDatosArca(numeroTarjeta, estadoDeseado, remoteJndiSunnel);
                    if (response1 != null) {
                        return estructura(response1);
                    } else {
                        return message.genericMessage("ERROR", "400", "Tarjeta no encontrada ", namespace, operationResponse);
                    }
                case "V":
                    //datos tarjeta visa
                    BloqueoDesbloqueoTarjetaResponse response2 = obtenerDatosSiscard(pais, numeroTarjeta, estadoDeseado, motivo, siscardUrl);
                    if (response2 != null) {
                        return estructura(response2);
                    } else {
                        return message.genericMessage("ERROR", "400", "Tarjeta no encontrada ", namespace, operationResponse);
                    }
                default:
                    return message.genericMessage("ERROR", "400", "Tipo Tarjeta no valida", namespace, operationResponse);

            }
        } catch (Exception e) {
            log.info("ObtenerBloqueoDesbloqueoTarjeta response = [" + message.genericMessage("ERROR", "600", "Error general contacte al administrador del sistema...", namespace, operationResponse) + "]");
            return message.genericMessage("ERROR", "600", "Error general contacte al administrador del sistema...", namespace, operationResponse);
        }

    }

    public static BloqueoDesbloqueoTarjetaResponse obtenerDatosArca(String numeroTarjeta, String estadoDeseado,
                                                                    String remoteJndiSunnel) throws Exception {
        BloqueoDesbloqueoTarjetaResponse response = new BloqueoDesbloqueoTarjetaResponse();
//        BLOQUEO TEMPORAL (estadoDeseado 1)
        String query1 = "UPDATE SUNNELP3.t_gcard SET riskconditionind = 'T', riskconditiondate = CURRENT_DATE, " +
                "riskcondreasoncodeid = 1 WHERE cardid = ? AND riskcondreasoncodeid IS NULL";

//        BLOQUEO PERMANENTE (estadoDeseado 2)
        String query2 = "UPDATE SUNNELP3.t_gcard SET riskconditionind = 'T', riskconditiondate = CURRENT_DATE, " +
                "riskcondreasoncodeid = 2 WHERE cardid = ? ";

//        DESBLOQUEO TEMPORAL (estadoDeseado 0)
        String query3 = "UPDATE SUNNELP3.t_gcard SET riskconditionind = 'F', riskconditiondate = null, " +
                "riskcondreasoncodeid = null WHERE cardid = ? AND riskcondreasoncodeid = 1";

        ConnectionHandler connectionHandler = new ConnectionHandler();
        Connection conexion = connectionHandler.getConnection(remoteJndiSunnel);
        int result=0;

        switch (estadoDeseado) {
            case "1":
                PreparedStatement ps1 = conexion.prepareStatement(query1);
                ps1.setString(1, numeroTarjeta);
                result = ps1.executeUpdate();
                ps1.close();
                if (result > 0) {
                    response.setStatusMessage("Proceso exitoso");
                    response.setStatusCode("00");
                    response.setStatus("SUCCESS");
                    log.info(new ObjectMapper().writeValueAsString(response));
                }else{
                    response.setStatusMessage("La tarjeta ya se encuentra bloqueada");
                    response.setStatusCode("00");
                    response.setStatus("ERROR");
                    log.info(new ObjectMapper().writeValueAsString(response));
                }
                break;
            case "2":
                PreparedStatement ps2 = conexion.prepareStatement(query2);
                ps2.setString(1, numeroTarjeta);
                result = ps2.executeUpdate();
                ps2.close();
                if (result > 0) {
                    response.setStatusMessage("Proceso exitoso");
                    response.setStatusCode("00");
                    response.setStatus("SUCCESS");
                    log.info(new ObjectMapper().writeValueAsString(response));
                }else{
                    response.setStatusMessage("La tarjeta ya se encuentra bloqueada");
                    response.setStatusCode("00");
                    response.setStatus("ERROR");
                    log.info(new ObjectMapper().writeValueAsString(response));
                }
                break;
            case "0":
                PreparedStatement ps3 = conexion.prepareStatement(query3);
                ps3.setString(1, numeroTarjeta);
                result = ps3.executeUpdate();
                ps3.close();
                if (result > 0) {
                    response.setStatusMessage("Proceso exitoso");
                    response.setStatusCode("00");
                    response.setStatus("SUCCESS");
                    log.info(new ObjectMapper().writeValueAsString(response));
                }else{
                    response.setStatusMessage("La tarjeta ya se encuentra desbloqueada");
                    response.setStatusCode("00");
                    response.setStatus("ERROR");
                    log.info(new ObjectMapper().writeValueAsString(response));
                }
                break;
            default:
                response.setStatusMessage("Estado deseado no valido");
                response.setStatusCode("400");
                response.setStatus("ERROR");
                log.info(new ObjectMapper().writeValueAsString(response));
                break;
        }
        conexion.close();
        return response;
    }

    public static BloqueoDesbloqueoTarjetaResponse obtenerDatosSiscard(String pais, String numeroTarjeta, String estadoDeseado, String motivo,
                                                                       String siscardUrl) throws Exception {
        BloqueoDesbloqueoTarjetaResponse response1 = new BloqueoDesbloqueoTarjetaResponse();
        JSONObject jsonSend = new JSONObject(); //json a enviar
        jsonSend.put("country", pais)
                .put("processIdentifier", "BloqueoDesbloqueoTarjetas")
                .put("numeroTarjeta", numeroTarjeta)
                .put("estadodeseado", estadoDeseado)
                .put("motivoCancelacion", motivo);

        HttpResponse<String> jsonResponse //realizar petici�n mediante unirest
                = Unirest.post(siscardUrl.concat("/bloqueoDesbloqueoTarjetas"))
                .header("Content-Type", "application/json")
                .body(jsonSend.toString())
                .asString();

        //capturar respuesta
        JSONObject response = new JSONObject(jsonResponse
                .getBody()
                .replaceAll("\u200B", ""));
        response1 = new ObjectMapper()
                .readValue(response.toString(), BloqueoDesbloqueoTarjetaResponse.class);

        log.info(new ObjectMapper().writeValueAsString(response1));

        return response1;
    }

    public static XmlObject estructura(BloqueoDesbloqueoTarjetaResponse response1) throws Exception {
        XmlObject result = XmlObject.Factory.newInstance();
        XmlCursor cursor = result.newCursor();
        QName responseQName = new QName(namespace, operationResponse);

        cursor.toNextToken();
        cursor.beginElement(responseQName);

        cursor.insertElementWithText(new QName(namespace, "statusCode"), response1.getStatusCode());
        cursor.insertElementWithText(new QName(namespace, "status"), response1.getStatus());
        cursor.insertElementWithText(new QName(namespace, "statusMessage"), response1.getStatusMessage());
        cursor.toParent();

        log.info("obtenerBloqueoDesbloqueoTarjeta response = [" + result + "]");
        return result;
    }
}
