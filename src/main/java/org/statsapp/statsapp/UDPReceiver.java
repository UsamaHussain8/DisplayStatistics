package org.statsapp.statsapp;

import dk.dma.ais.message.AisMessage;
import dk.dma.ais.sentence.SentenceException;
import dk.dma.ais.sentence.Vdm;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UDPReceiver {
    private static final int port = 4001;
    public static void receiveAisMessage() throws SocketException {
        DatagramSocket socket = new DatagramSocket(port);
        System.out.println("UDP Receiver listening on port " + port);
        System.out.println("Socket bound to: " + socket.getLocalSocketAddress());

        // Buffer to receive data
        byte[] buffer = new byte[1024];

        int messageCount = 0;

        try {
            while (true) {
                // Create packet to receive data
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                System.out.println("Waiting for packet...");

                // Receive packet (this will block until data arrives)
                socket.receive(packet);

                messageCount++;

                // Extract data from packet
                String receivedMessage = new String(packet.getData(), 0, packet.getLength());

                System.out.println("\n=== Message " + messageCount + " ===");
                System.out.println("From: " + packet.getAddress() + ":" + packet.getPort());
                System.out.println("Length: " + packet.getLength() + " bytes");
                System.out.println("Content: " + receivedMessage.trim());
                String aisMessage = receivedMessage.trim();
                Vdm vdm = new Vdm();
//                try {
//                    vdm.parse(aisMessage);
//                    System.out.println("----- Message ID from VDM: " + vdm.getMsgId() + " -----");
//                }
//                catch(SentenceException ex) {
//                    throw new SentenceException(ex.getMessage());
//                }

                System.out.println("Raw bytes: " + java.util.Arrays.toString(
                        java.util.Arrays.copyOf(packet.getData(), packet.getLength())
                ));
                System.out.println("========================\n");
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }
}
