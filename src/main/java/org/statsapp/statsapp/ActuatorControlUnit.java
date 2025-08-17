package org.statsapp.statsapp;

import java.util.*;

public class ActuatorControlUnit {

    // Command structure constants
    private static final byte[] HEADER = {(byte) 0xAA, (byte) 0xBB};
    private static final byte ID = (byte) 0x01;
    private static final byte DIGITAL_COMMAND_CODE = (byte) 0x10;
    private static final int PAYLOAD_SIZE = 28;

    // Enum for all available controls with their section and bit positions
    public enum Control {
        // Section 1 - Engines and Basic Lights
        ENGINE_ON(1, 0),
        ENGINE_OFF(1, 1),
        STARBOARD_ENGINE_ON(1, 2),
        STARBOARD_ENGINE_OFF(1, 3),
        PORT_ENGINE_ON(1, 4),
        PORT_ENGINE_OFF(1, 5),
        STARBOARD_LIGHTS_ON(1, 6),
        STARBOARD_LIGHTS_OFF(1, 7),
        PORT_LIGHTS_ON(1, 8),
        PORT_LIGHTS_OFF(1, 9),
        NAVIGATION_LIGHTS_ON(1, 10),
        NAVIGATION_LIGHTS_OFF(1, 11),
        LANDING_LIGHTS_ON(1, 12),
        LANDING_LIGHTS_OFF(1, 13),
        TAXI_LIGHTS_ON(1, 14),
        TAXI_LIGHTS_OFF(1, 15),

        // Section 2 - Communication and Navigation
        RADIO_ON(2, 0),
        RADIO_OFF(2, 1),
        GPS_ON(2, 2),
        GPS_OFF(2, 3),
        TRANSPONDER_ON(2, 4),
        TRANSPONDER_OFF(2, 5),
        AUTOPILOT_ON(2, 6),
        AUTOPILOT_OFF(2, 7),
        RADAR_ON(2, 8),
        RADAR_OFF(2, 9),

        // Section 3 - Alarms and Warning Systems
        ALARM1_ON(3, 0),
        ALARM1_OFF(3, 1),
        ALARM2_ON(3, 2),
        ALARM2_OFF(3, 3),
        FIRE_ALARM_ON(3, 4),
        FIRE_ALARM_OFF(3, 5),
        OVERHEAT_WARNING_ON(3, 6),
        OVERHEAT_WARNING_OFF(3, 7),
        LOW_FUEL_WARNING_ON(3, 8),
        LOW_FUEL_WARNING_OFF(3, 9),

        // Section 4 - Auxiliary Systems
        CABIN_LIGHTS_ON(4, 0),
        CABIN_LIGHTS_OFF(4, 1),
        AIR_CONDITIONING_ON(4, 2),
        AIR_CONDITIONING_OFF(4, 3),
        HEATING_ON(4, 4),
        HEATING_OFF(4, 5),
        VENTILATION_ON(4, 6),
        VENTILATION_OFF(4, 7);

        private final int section;
        private final int bitPosition;

        Control(int section, int bitPosition) {
            this.section = section;
            this.bitPosition = bitPosition;
        }

        public int getSection() { return section; }
        public int getBitPosition() { return bitPosition; }
    }

    /**
     * Creates a command for a single control
     * @param control The control to activate
     * @return Complete command packet as byte array
     */
    public static byte[] createCommand(Control control) {
        byte[] payload = new byte[PAYLOAD_SIZE];
        int byteOffset = (control.getSection() - 1) * 2;
        setBitInPayload(payload, byteOffset, control.getBitPosition());
        return buildCompleteCommand(payload);
    }

    /**
     * Creates a command for multiple controls
     * @param controls Array of controls to activate
     * @return Complete command packet as byte array
     */
    public static byte[] createCommand(Control... controls) {
        byte[] payload = new byte[PAYLOAD_SIZE];

        for (Control control : controls) {
            int byteOffset = (control.getSection() - 1) * 2;
            setBitInPayload(payload, byteOffset, control.getBitPosition());
        }

        return buildCompleteCommand(payload);
    }

    /**
     * Creates a command from control names (useful for UI integration)
     * @param controlNames String names of controls
     * @return Complete command packet as byte array
     */
    public static byte[] createCommandFromNames(String... controlNames) {
        List<Control> controls = new ArrayList<>();

        for (String name : controlNames) {
            try {
                Control control = Control.valueOf(name.toUpperCase());
                controls.add(control);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown control: " + name);
            }
        }

        return createCommand(controls.toArray(new Control[0]));
    }

    /**
     * Get all available controls as a map for UI generation
     */
    public static Map<String, String> getAvailableControls() {
        Map<String, String> controlsMap = new LinkedHashMap<>();

        for (Control control : Control.values()) {
            String friendlyName = control.name()
                    .replace("_", " ")
                    .toLowerCase();
            // Capitalize first letter of each word
            String[] words = friendlyName.split(" ");
            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
            controlsMap.put(control.name(), sb.toString().trim());
        }

        return controlsMap;
    }

    /**
     * UI Helper: Get controls grouped by category for better organization
     */
    public static Map<String, List<Control>> getControlsByCategory() {
        Map<String, List<Control>> categories = new LinkedHashMap<>();

        categories.put("Engines & Basic Lights", Arrays.asList(
                Control.ENGINE_ON, Control.ENGINE_OFF,
                Control.STARBOARD_ENGINE_ON, Control.STARBOARD_ENGINE_OFF,
                Control.PORT_ENGINE_ON, Control.PORT_ENGINE_OFF,
                Control.STARBOARD_LIGHTS_ON, Control.STARBOARD_LIGHTS_OFF,
                Control.PORT_LIGHTS_ON, Control.PORT_LIGHTS_OFF,
                Control.NAVIGATION_LIGHTS_ON, Control.NAVIGATION_LIGHTS_OFF,
                Control.LANDING_LIGHTS_ON, Control.LANDING_LIGHTS_OFF,
                Control.TAXI_LIGHTS_ON, Control.TAXI_LIGHTS_OFF
        ));

        categories.put("Communication & Navigation", Arrays.asList(
                Control.RADIO_ON, Control.RADIO_OFF,
                Control.GPS_ON, Control.GPS_OFF,
                Control.TRANSPONDER_ON, Control.TRANSPONDER_OFF,
                Control.AUTOPILOT_ON, Control.AUTOPILOT_OFF,
                Control.RADAR_ON, Control.RADAR_OFF
        ));

        categories.put("Alarms & Warnings", Arrays.asList(
                Control.ALARM1_ON, Control.ALARM1_OFF,
                Control.ALARM2_ON, Control.ALARM2_OFF,
                Control.FIRE_ALARM_ON, Control.FIRE_ALARM_OFF,
                Control.OVERHEAT_WARNING_ON, Control.OVERHEAT_WARNING_OFF,
                Control.LOW_FUEL_WARNING_ON, Control.LOW_FUEL_WARNING_OFF
        ));

        categories.put("Auxiliary Systems", Arrays.asList(
                Control.CABIN_LIGHTS_ON, Control.CABIN_LIGHTS_OFF,
                Control.AIR_CONDITIONING_ON, Control.AIR_CONDITIONING_OFF,
                Control.HEATING_ON, Control.HEATING_OFF,
                Control.VENTILATION_ON, Control.VENTILATION_OFF
        ));

        return categories;
    }

    /**
     * Helper method to find the opposite control (ON/OFF toggle)
     */
    public static Control getOppositeControl(Control control) {
        String name = control.name();
        if (name.endsWith("_ON")) {
            String offName = name.replace("_ON", "_OFF");
            return Control.valueOf(offName);
        } else if (name.endsWith("_OFF")) {
            String onName = name.replace("_OFF", "_ON");
            return Control.valueOf(onName);
        }
        return control; // No opposite found
    }

    /**
     * Sets a specific bit in the payload at the given byte offset and bit position
     */
    private static void setBitInPayload(byte[] payload, int byteOffset, int bitPosition) {
        int byteIndex = byteOffset + (bitPosition / 8);
        int bitIndex = bitPosition % 8;

        // Set the bit (using little-endian bit ordering)
        payload[byteIndex] |= (1 << bitIndex);
    }

    /**
     * Builds the complete command packet
     */
    private static byte[] buildCompleteCommand(byte[] payload) {
        int packetSize = HEADER.length + 1 + 1 + PAYLOAD_SIZE + 1 + 2;
        byte[] packet = new byte[packetSize];

        int offset = 0;

        // Add header
        System.arraycopy(HEADER, 0, packet, offset, HEADER.length);
        offset += HEADER.length;

        // Add ID
        packet[offset++] = ID;

        // Add command code
        packet[offset++] = DIGITAL_COMMAND_CODE;

        // Add payload
        System.arraycopy(payload, 0, packet, offset, PAYLOAD_SIZE);
        offset += PAYLOAD_SIZE;

        // Add FM byte
        byte fm = calculateFM(packet, offset);
        packet[offset++] = fm;

        // Add CRC
        short crc = calculateCRC(packet, offset);
        packet[offset++] = (byte) ((crc >> 8) & 0xFF);
        packet[offset++] = (byte) (crc & 0xFF);

        return packet;
    }

    private static byte calculateFM(byte[] packet, int length) {
        byte fm = 0;
        for (int i = 0; i < length; i++) {
            fm ^= packet[i];
        }
        return fm;
    }

    private static short calculateCRC(byte[] packet, int length) {
        int crc = 0xFFFF;
        for (int i = 0; i < length; i++) {
            crc ^= (packet[i] & 0xFF);
            for (int j = 0; j < 8; j++) {
                if ((crc & 1) != 0) {
                    crc = (crc >> 1) ^ 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }
        return (short) crc;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X ", b & 0xFF));
        }
        return result.toString().trim();
    }

    // Example usage demonstrating the dynamic system
    public static void main(String[] args) {
        System.out.println("=== Dynamic Actuator Control System ===\n");

        // Example 1: Single control - UI user clicks "Turn ON Starboard Lights"
        System.out.println("1. User clicks 'Turn ON Starboard Lights':");
        byte[] cmd1 = createCommand(Control.STARBOARD_LIGHTS_ON);
        System.out.println("Command (hex): " + bytesToHex(cmd1));
        System.out.println("Control location: Section " + Control.STARBOARD_LIGHTS_ON.getSection() +
                ", Bit " + Control.STARBOARD_LIGHTS_ON.getBitPosition() + "\n");

        // Example 2: Single control - UI user clicks "Turn ON Alarm 2"
        System.out.println("2. User clicks 'Turn ON Alarm 2':");
        byte[] cmd2 = createCommand(Control.ALARM2_ON);
        System.out.println("Command (hex): " + bytesToHex(cmd2));
        System.out.println("Control location: Section " + Control.ALARM2_ON.getSection() +
                ", Bit " + Control.ALARM2_ON.getBitPosition() + "\n");

        // Example 3: Multiple controls at once
        System.out.println("3. User selects multiple controls:");
        byte[] cmd3 = createCommand(
                Control.ENGINE_ON,
                Control.NAVIGATION_LIGHTS_ON,
                Control.RADIO_ON
        );
        System.out.println("Command (hex): " + bytesToHex(cmd3));
        System.out.println();

        // Example 4: Using string names (useful for web UI or config files)
        System.out.println("4. Using control names from UI:");
        byte[] cmd4 = createCommandFromNames("STARBOARD_LIGHTS_OFF", "AUTOPILOT_ON");
        System.out.println("Command (hex): " + bytesToHex(cmd4));
        System.out.println();

        // Example 5: Show available controls for UI generation
        System.out.println("5. Available controls for UI:");
        Map<String, List<Control>> categories = getControlsByCategory();
        for (Map.Entry<String, List<Control>> entry : categories.entrySet()) {
            System.out.println("\n" + entry.getKey() + ":");
            for (Control control : entry.getValue()) {
                System.out.println("  - " + control.name() + " (Section " +
                        control.getSection() + ", Bit " + control.getBitPosition() + ")");
            }
        }

        // Example 6: Toggle functionality
        System.out.println("\n6. Toggle example - if starboard lights are ON, turn them OFF:");
        Control opposite = getOppositeControl(Control.STARBOARD_LIGHTS_ON);
        System.out.println("Opposite of " + Control.STARBOARD_LIGHTS_ON.name() +
                " is " + opposite.name());
    }
}
