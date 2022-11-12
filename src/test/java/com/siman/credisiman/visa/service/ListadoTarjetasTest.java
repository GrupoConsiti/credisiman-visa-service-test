package com.siman.credisiman.visa.service;

import static org.junit.Assert.assertEquals;

import org.apache.xmlbeans.SimpleValue;
import org.apache.xmlbeans.XmlObject;
import org.junit.Test;

public class ListadoTarjetasTest {
	static private final String NS = "http://siman.com/ListadoTarjetas";
	static String[] listadoTarjetas = {"4000123456780002", "4000123456780001", "4000123456780000"};
	
    @Test
    public void obtenerDatosClienteOk() {
        XmlObject result = ListadoTarjetas.obtenerListadoTarjetas("SLV", "0398765432", "jndi/SimacSV", "jdbc/ORIONREPOSV", "http://soauat.siman.com:7003/v1/orion", "usuario", "600831, 600831, 600831");
        int i = 0;
        
        //Status
        assertEquals("00", ((SimpleValue) result.selectPath( "declare namespace ns='" + NS + "' " + ".//ns:statusCode")[0]).getStringValue());
        assertEquals("SUCCESS", ((SimpleValue) result.selectPath( "declare namespace ns='" + NS + "' " + ".//ns:status")[0]).getStringValue());
        
        //Data
        XmlObject[] tarjetas = result.selectPath( "declare namespace ns='" + NS + "' " + ".//ns:tarjetas");
        while (i < tarjetas.length) {
        	String numeroTarjeta = ((SimpleValue) tarjetas[i].selectPath( "declare namespace ns='" + NS + "' " + ".//ns:numeroTarjeta")[0]).getStringValue();
        	assertEquals(listadoTarjetas[i], numeroTarjeta);
        	i++;
        }
    }
}