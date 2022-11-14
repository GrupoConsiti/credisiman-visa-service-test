package com.siman.credisiman.visa.service;

import com.credisiman.visa.soa.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.siman.credisiman.visa.dto.ConsultaMovimientosResponse;
import com.siman.credisiman.visa.dto.MovimientosResponse;
import com.siman.credisiman.visa.utils.ConnectionHandler;
import com.siman.credisiman.visa.utils.Message;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ConsultaMovimientos {
    private static final Logger log = LoggerFactory.getLogger(ConsultaMovimientos.class);
    private static final String namespace = "http://siman.com/ConsultaMovimientos";
    private static final String operationResponse = "ObtenerConsultaMovimientosResponse";

    public static XmlObject obtenerConsultaMovimientos(String pais, String numeroTarjeta, String fechaInicial,
                                                       String fechaFinal, String remoteJndiSunnel, String remoteJndiOrion, String siscardUrl, String siscardUser, String binCredisiman, String tipoTarjeta) {
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
        ConsultaMovimientosResponse response1;
        try {
            switch (tipoTarjeta) {
                case "P":
                    //datos tarjeta privada
                    response1 = obtenerDatosArca(numeroTarjeta, fechaInicial, fechaFinal, remoteJndiSunnel);
                    if (response1 != null) {
                        return estructura(response1);
                    } else {
                        log.info("obtenerConsultaMovimientos response = [" + message.genericMessage("ERROR", "400", "No se encontr� informaci�n con la identificaci�n proporcionada", namespace, operationResponse) + "]");
                        return message.genericMessage("ERROR", "400", "No se encontr� informaci�n con la tarjeta proporcionada", namespace, operationResponse);
                    }
                case "V":
                    //datos tarjeta visa
                    ConsultaMovimientosResponse response2 = obtenerDatosSiscard(pais, numeroTarjeta, fechaInicial, fechaFinal, siscardUrl);
                    if (response2.getMovimientos().size() > 0) {
                        return estructura(response2);
                    } else {
                        log.info("obtenerConsultaMovimientos response = [" + message.genericMessage("ERROR", "400", "No se encontr� informaci�n con la identificaci�n proporcionada", namespace, operationResponse) + "]");
                        return message.genericMessage("ERROR", "400", "No se encontr� informaci�n con la tarjeta proporcionada", namespace, operationResponse);
                    }
            }
        } catch (SQLException e) {
            log.error("SQL ERROR, " + e.getMessage());
            log.info("obtenerConsultaMovimientos response = [" + message.genericMessage("ERROR", "600", "Error general contacte al administrador del sistema...", namespace, operationResponse) + "]");
            return message.genericMessage("ERROR", "600", "Error general contacte al administrador del sistema...", namespace, operationResponse);
        } catch (Exception ex) {
            log.error("SERVICE ERROR, " + ex.getMessage());
            log.info("obtenerConsultaMovimientos response = [" + message.genericMessage("ERROR", "600", "Error general contacte al administrador del sistema...", namespace, operationResponse) + "]");
            return message.genericMessage("ERROR", "600", "Error general contacte al administrador del sistema...", namespace, operationResponse);
        }

        log.info("obtenerConsultaMovimientos response = [" + message.genericMessage("ERROR", "400", "No se encontr� informaci�n con la identificaci�n proporcionada", namespace, operationResponse) + "]");
        return message.genericMessage("ERROR", "400", "No se encontr� informaci�n con la tarjeta proporcionada", namespace, operationResponse);
    }

    public static XmlObject estructura(ConsultaMovimientosResponse response1) {
        //OBTENER DATOS
        XmlObject result = XmlObject.Factory.newInstance();
        XmlCursor cursor = result.newCursor();
        QName responseQName = new QName(namespace, operationResponse);
        cursor.toNextToken();
        cursor.beginElement(responseQName);
        cursor.insertElementWithText(new QName(namespace, "statusCode"), "00");
        cursor.insertElementWithText(new QName(namespace, "status"), "SUCCESS");
        cursor.insertElementWithText(new QName(namespace, "statusMessage"), "Proceso exitoso");

        for (int i = 0; i < response1.getMovimientos().size(); i++) {
            cursor.beginElement(new QName(namespace, "movimientos"));
            cursor.insertElementWithText(new QName(namespace, "tipoMovimiento"), response1.getMovimientos().get(i).getTipoMovimiento());
            cursor.insertElementWithText(new QName(namespace, "fechaMovimiento"), response1.getMovimientos().get(i).getFechaMovimiento());
            cursor.insertElementWithText(new QName(namespace, "fechaAplicacion"), response1.getMovimientos().get(i).getFechaConsumo());
            cursor.insertElementWithText(new QName(namespace, "moneda"), response1.getMovimientos().get(i).getCodigoMoneda());
            cursor.insertElementWithText(new QName(namespace, "monto"), response1.getMovimientos().get(i).getMonto());
            cursor.insertElementWithText(new QName(namespace, "numeroAutorizacion"), response1.getMovimientos().get(i).getNumeroAutorizacion());
            cursor.insertElementWithText(new QName(namespace, "comercio"), response1.getMovimientos().get(i).getDescripcionComercio());
            cursor.toParent();
        }

        cursor.toParent();
        log.info("obtenerConsultaMovimientos response = [" + result + "]");
        return result;
    }

    public static ConsultaMovimientosResponse obtenerDatosArca(String numeroTarjeta, String fechaInicial, String fechaFinal, String remoteJndiSunnel) throws Exception {
        String query = "SELECT * " +
                "    FROM (SELECT co.creditoperationid AS idOperacion, " +
                "                 'A' AS tipoOperacion, " +
                "                 TO_CHAR(co.operationdate,'YYYYMMDD') AS fechaTransaccion, " +
                "                 TO_CHAR(co.processingdate,'YYYYMMDD') AS fechaAplicacion, " +
                "                 co.sourcecurrencyid AS moneda, " +
                "                 co.sourceamount AS monto, " +
                "                 co.referencenumber AS numeroAutorizacion, " +
                "                 ot.description AS concepto, " +
                "                 b.description AS comercio " +
                "            FROM SUNNELP3.t_gcreditoperation co " +
                "                 INNER JOIN SUNNELP3.t_goperationtype ot " +
                "                    ON ot.operationtypeid = co.operationtypeid " +
                "                 INNER JOIN SUNNELP3.t_gcardaccount ca " +
                "                    ON ca.accountid = co.creditlineid " +
                "                 INNER JOIN SUNNELP3.t_gbranch b " +
                "                    ON b.branchid = co.branchid " +
                "           WHERE     ca.cardid = ? " +
                "                 AND co.operationdate BETWEEN TO_DATE ( ? , " +
                "                                                       'yyyymmdd') " +
                "                                          AND TO_DATE ( ? , " +
                "                                                       'yyyymmdd') " +
                "          UNION ALL " +
                "          SELECT do.debitoperationid AS idOperacion, " +
                "                 'C' AS tipoOperacion, " +
                "                 TO_CHAR(do.operationdate, 'YYYYMMDD' ) AS fechaTransaccion, " +
                "                 TO_CHAR(do.processingdate, 'YYYYMMDD' ) AS fechaAplicacion, " +
                "                 do.sourcecurrencyid AS moneda, " +
                "                 do.sourceamount AS monto, " +
                "                 pu.referencenumber AS numeroAutorizacion, " +
                "                 ot.description AS concepto, " +
                "                 b.description AS comercio " +
                "            FROM SUNNELP3.t_gdebitoperation do " +
                "                 INNER JOIN SUNNELP3.t_goperationtype ot " +
                "                    ON ot.operationtypeid = do.operationtypeid " +
                "                 INNER JOIN SUNNELP3.t_gpurchaseoperation pu " +
                "                    ON     pu.debitoperationid = do.debitoperationid " +
                "                       AND pu.creditlineid = do.creditlineid " +
                "                 INNER JOIN SUNNELP3.t_gcardaccount ca " +
                "                    ON ca.accountid = do.creditlineid " +
                "                 INNER JOIN SUNNELP3.t_gbranch b " +
                "                    ON b.branchid = do.branchid " +
                "           WHERE     ca.cardid = ? " +
                "                 AND do.operationdate BETWEEN TO_DATE ( ? , " +
                "                                                       'yyyymmdd') " +
                "                                          AND TO_DATE ( ? , " +
                "                                                       'yyyymmdd') " +
                "          UNION ALL " +
                "          SELECT cro.creditreversalopid AS idOperacion, " +
                "                 'RA' AS tipoOperacion, " +
                "                 TO_CHAR(cro.reversaldate, 'YYYYMMDD' ) AS fechaTransaccion, " +
                "                 TO_CHAR(cro.reversaldate, 'YYYYMMDD' ) AS fechaAplicacion, " +
                "                 co.sourcecurrencyid AS moneda, " +
                "                 cro.reversalamount AS monto, " +
                "                 cro.referencenumber AS numeroAutorizacion, " +
                "                 'Reversa Operaci�n: ' || cro.creditoperationid AS concepto, " +
                "                 b.description AS comercio " +
                "            FROM SUNNELP3.t_gcreditreversalop cro " +
                "                 INNER JOIN SUNNELP3.t_gcreditoperation co " +
                "                    ON     co.creditlineid = cro.creditlineid " +
                "                       AND co.creditoperationid = cro.creditoperationid " +
                "                 INNER JOIN SUNNELP3.t_gcardaccount ca " +
                "                    ON ca.accountid = cro.creditlineid " +
                "                 INNER JOIN SUNNELP3.t_gbranch b " +
                "                    ON b.branchid = cro.branchid " +
                "           WHERE     ca.cardid = ? " +
                "                 AND cro.reversaldate BETWEEN TO_DATE ( ? , " +
                "                                                       'yyyymmdd') " +
                "                                          AND TO_DATE ( ? , " +
                "                                                       'yyyymmdd') " +
                "          UNION ALL " +
                "          SELECT rdo.reversaldebitopid AS idOperacion, " +
                "                 'RC' AS tipoOperacion, " +
                "                 TO_CHAR(rdo.reversaldate, 'YYYYMMDD' ) AS fechaTransaccion, " +
                "                 TO_CHAR(rdo.reversaldate, 'YYYYMMDD' ) AS fechaAplicacion, " +
                "                 do.sourcecurrencyid AS moneda, " +
                "                 rdo.reversalamount AS monto, " +
                "                 rdo.referencenumber AS numeroAutorizacion, " +
                "                 'Reversa Operaci�n: ' || rdo.debitoperationid AS concepto, " +
                "                 b.description AS comercio " +
                "            FROM SUNNELP3.t_greversaldebitop rdo " +
                "                 INNER JOIN SUNNELP3.t_gdebitoperation do " +
                "                    ON     do.creditlineid = rdo.creditlineid " +
                "                       AND do.debitoperationid = rdo.debitoperationid " +
                "                 INNER JOIN SUNNELP3.t_gcardaccount ca " +
                "                    ON ca.accountid = rdo.creditlineid " +
                "                 INNER JOIN SUNNELP3.t_gbranch b " +
                "                    ON b.branchid = rdo.branchid " +
                "           WHERE     ca.cardid = ? " +
                "                 AND rdo.reversaldate BETWEEN TO_DATE ( ? , " +
                "                                                       'yyyymmdd') " +
                "                                          AND TO_DATE ( ? , " +
                "                                                       'yyyymmdd')) m " +
                "ORDER BY m.fechaTransaccion, m.idOperacion";//TODO obtener query ARCA


        ConnectionHandler connectionHandler = new ConnectionHandler();
        Connection conexion = connectionHandler.getConnection(remoteJndiSunnel);
        PreparedStatement sentencia = conexion.prepareStatement(query);

        sentencia.setString(1, numeroTarjeta); //TODO agregar parametros
        sentencia.setString(2, fechaInicial);
        sentencia.setString(3, fechaFinal);
        sentencia.setString(4, numeroTarjeta);
        sentencia.setString(5, fechaInicial);
        sentencia.setString(6, fechaFinal);
        sentencia.setString(7, numeroTarjeta);
        sentencia.setString(8, fechaInicial);
        sentencia.setString(9, fechaFinal);
        sentencia.setString(10, numeroTarjeta);
        sentencia.setString(11, fechaInicial);
        sentencia.setString(12, fechaFinal);

        ResultSet rs = sentencia.executeQuery();
        ConsultaMovimientosResponse response1 = new ConsultaMovimientosResponse();
        List<MovimientosResponse> listaMovimientos = new ArrayList<>();

        while (rs.next()) {
            MovimientosResponse movimientosResponse = new MovimientosResponse();
            //TODO agregar a xml
            movimientosResponse.setTipoMovimiento(rs.getString("tipoOperacion"));
            movimientosResponse.setFechaMovimiento(rs.getString("fechaTransaccion"));
            movimientosResponse.setFechaConsumo(rs.getString("fechaAplicacion"));
            movimientosResponse.setCodigoMoneda(rs.getString("moneda"));
            movimientosResponse.setMonto(rs.getString("monto"));
            movimientosResponse.setNumeroAutorizacion(rs.getString("numeroAutorizacion"));
            movimientosResponse.setDescripcionComercio(rs.getString("comercio"));
            listaMovimientos.add(movimientosResponse);
        }
        response1.setMovimientos(listaMovimientos);
        return response1;
    }


    //OBTENER DATOS TARJETA CREDISIMAN VISA
    public static ConsultaMovimientosResponse obtenerDatosSiscard(String pais, String numeroTarjeta
            , String fechaInicial, String fechaFinal, String siscardUrl) throws Exception {

        JSONObject jsonSend = new JSONObject(); //json a enviar
        jsonSend.put("country", pais)
                .put("process_Identifier", "consultaMovimientos")
                .put("numeroTarjeta", numeroTarjeta)
                .put("fechaInicial", fechaInicial)
                .put("fechaFinal", fechaFinal);

        HttpResponse<String> jsonResponse //realizar petici�n demiante unirest
                = Unirest.post(siscardUrl.concat("/consultaMovimientos"))
                .header("Content-Type", "application/json")
                .body(jsonSend.toString())
                .asString();

        //capturar respuesta
        JSONObject response = new JSONObject(jsonResponse
                .getBody()
                .replaceAll("\u200B", ""));
        return new ObjectMapper()
                .readValue(response.toString(), ConsultaMovimientosResponse.class);
    }

}
