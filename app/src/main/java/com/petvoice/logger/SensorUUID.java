package com.petvoice.logger;

import java.util.HashMap;

public class SensorUUID {
        private static HashMap<String, String> attributes = new HashMap();
        public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
        public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

        static {
            // Sample Services.
            attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
            attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
            // Sample Characteristics.
            attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
            attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");


            attributes.put(("0000fe59-0000-1000-8000-00805f9b34fb").toLowerCase(), "DeviceFirmwareUpdate");
            attributes.put(("6E400001-B5A3-F393-E0A9-E50E24DCCA9E").toLowerCase(), "Nordic_UART");
            attributes.put(("1074f00d-8a96-fe1e-c5a5-a27d11f5c777").toLowerCase(), "MEMS_Service");
            attributes.put(("1074f0ce-8a96-fe1e-c5a5-a27d11f5c777").toLowerCase(), "TEMP_Service");
            attributes.put(("12345678-1234-5678-1234-56789abcdef0").toLowerCase(), "Bleno_Service");


            attributes.put(("8EC90001-F315-4F60-9FB8-838830DAEA50").toLowerCase(), "DFUControlPoint");
            attributes.put(("8EC90002-F315-4F60-9FB8-838830DAEA50").toLowerCase(), "DFUPacket");
            attributes.put(("8EC90003-F315-4F60-9FB8-838830DAEA50").toLowerCase(), "DFU_NOBOND");
            attributes.put(("8EC90004-F315-4F60-9FB8-838830DAEA50").toLowerCase(), "DFU_BOND");
            attributes.put(("6E400002-B5A3-F393-E0A9-E50E24DCCA9E").toLowerCase(), "TX");
            attributes.put(("6E400003-B5A3-F393-E0A9-E50E24DCCA9E").toLowerCase(), "RX");
            attributes.put(("1074beef-8a96-fe1e-c5a5-a27d11f5c777").toLowerCase(), "MEMS_DATA");
            attributes.put(("1074cafe-8a96-fe1e-c5a5-a27d11f5c777").toLowerCase(), "MEMS_CONF");
            attributes.put(("107401CE-8a96-fe1e-c5a5-a27d11f5c777").toLowerCase(), "MEMS_POW ");
            attributes.put(("12345678-1234-5678-1234-56789abcdef1").toLowerCase(), "Bleno_Char");
        }

        public static String lookup(String uuid, String defaultName) {
            String name = attributes.get(uuid.toLowerCase());
            return name == null ? defaultName : name;
        }

}
