package com.siman.credisiman.visa.service;

import com.credisiman.visa.soa.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.siman.credisiman.visa.dto.solicitudreposiciontarjeta.SolicitudReposicionTarjetaResponse;
import com.siman.credisiman.visa.utils.Message;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;

public class SolicitudReposicionTarjeta {
    private static final Logger log = LoggerFactory.getLogger(SolicitudReposicionTarjeta.class);

	public static XmlObject crearSolicitudReposicionTarjeta(String pais, String numeroTarjeta, String nombreEmbozar,
			String razonReposicion, String remoteJndiSunnel, String remoteJndiOrion, String siscardUrl,
			String siscardUser, String binCredisiman) {
        String namespace = "http://siman.com/SolicitudReposicionTarjeta";
        String operationResponse = "ObtenerSolicitudReposicionTarjetaResponse";
        SolicitudReposicionTarjetaResponse response1 = new SolicitudReposicionTarjetaResponse();

        XmlObject result = XmlObject.Factory.newInstance();
        XmlCursor cursor = result.newCursor();
        QName responseQName = new QName(namespace, operationResponse);

        //validar campos requeridos
        Utils utils = new Utils();
        Message message = new Message();

        if (!utils.validateNotNull(pais)) {
            return message.genericMessage("ERROR", "025", "El campo pais es obligatorio", namespace, operationResponse);
        }
        if (!utils.validateNotNull(numeroTarjeta)) {
            return message.genericMessage("ERROR", "025", "El campo n�mero tarjeta es obligatorio", namespace, operationResponse);
        }
        if (!utils.validateNotNull(nombreEmbozar)) {
            return message.genericMessage("ERROR", "025", "El campo nombre embozar es obligatorio", namespace, operationResponse);
        }
        if (!utils.validateNotNull(razonReposicion)) {
            return message.genericMessage("ERROR", "025", "El campo raz�n reposici�n es obligatorio", namespace, operationResponse);
        }

        //validar longitudes
        if (!utils.validateLongitude(pais,3)) {
            return message.genericMessage("ERROR", "025", "La longitud del campo pais debe ser menor o igual a 3", namespace, operationResponse);
        }
        if (!utils.validateLongitude(numeroTarjeta  ,16)) {
            return message.genericMessage("ERROR", "025", "La longitud del campo n�mero tarjeta debe ser menor o igual a 16", namespace, operationResponse);
        }
        if (!utils.validateLongitude(nombreEmbozar  ,29)) {
            return message.genericMessage("ERROR", "025", "La longitud del campo nombre embozar debe ser menor o igual a 29", namespace, operationResponse);
        }
        if (!utils.validateLongitude(nombreEmbozar  ,1)) {
            return message.genericMessage("ERROR", "025", "La longitud del campo raz�n reposici�n debe ser  1", namespace, operationResponse);
        }

        //OBTENER DATOS
        try {
            JSONObject jsonSend = new JSONObject(); //json a enviar
            jsonSend.put("country", pais)
                    .put("processIdentifier", "solicitudTarjetaReposicion")
                    .put("numeroTarjeta", numeroTarjeta)
                    .put("nombreEmbozar", nombreEmbozar)
                    .put("razonReposicion", razonReposicion)
                    .put("localidadTramite", "01")
                    .put("localidadRetiro", "01")
                    .put("indicadorCobroxReposicion", "S");

            HttpResponse<String> jsonResponse //realizar petici�n demiante unirest
                    = Unirest
                    .post(siscardUrl.concat("/solicitudTarjetaReposicion"))
                    .header("Content-Type", "application/json")
                    .body(jsonSend.toString())
                    .asString();

            //capturar respuesta
            JSONObject response = new JSONObject(jsonResponse
                    .getBody()
                    .replaceAll("\u200B", ""));
            response1 = new ObjectMapper()
                    .readValue(response.toString(), SolicitudReposicionTarjetaResponse.class);
        } catch (Exception e) {
            log.info(e.getMessage());
            return message.genericMessage("ERROR", "600", "Error general contacte al administrador del sistema...", namespace, operationResponse);
        }

        cursor.toNextToken();
        cursor.beginElement(responseQName);
        cursor.insertElementWithText(new QName(namespace, "statusCode"), response1.getStatusCode());
        cursor.insertElementWithText(new QName(namespace, "status"), response1.getStatus());
        cursor.insertElementWithText(new QName(namespace, "statusMessage"), response1.getStatusMessage());
        cursor.insertElementWithText(new QName(namespace, "nuevoNumeroTarjeta"), response1.getNuevoNumeroTarjeta());
        cursor.toParent();

        log.info("obtenerSolicitudReposicionTarjeta response = [" + result.toString() + "]");
        return result;
    }
}