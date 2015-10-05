package com.company;

import com.sun.org.apache.xpath.internal.SourceTree;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.sql.Time;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * The TSAPP Client queries the time server for the server time or sets the server time
 * using TCP or UDP, whichever is specified.
 *
 * Created by Krishna on 10/2/2015.
 */
public class TSAPPClient {
    private static InetAddress DEST_IP;
    private static int DEST_PORT;
    private static int timeFormat;

    public TSAPPClient(InetAddress address, int port)
    {
        DEST_IP = address;
        DEST_PORT = port;
        timeFormat = 0;
    }

    public void InitiateUDPHandler(String messageToSend) throws SocketException {
        UDPTransmitter udpTransmitter = new UDPTransmitter(messageToSend, DEST_IP, DEST_PORT);
        new Thread(udpTransmitter).start();
    }

    public void InitiateTCPHandler(String messageToSend) throws IOException {
        TCPTransmitter tcpTransmitter = new TCPTransmitter(messageToSend, DEST_IP, DEST_PORT);
        new Thread(tcpTransmitter).start();
    }

    public void setTimeFormat(int i) {
        if(i == 1)
            timeFormat = 1;
    }

    /***
     * Handles UDP Connections - Transmitting and Receiving
     */
    public static class UDPTransmitter extends Thread {
        public DatagramSocket clientSocket;
        //Transmitter
        private DatagramPacket pktToSend;
        private byte[] messageToSend;
        private InetAddress serverAddress;
        private int serverPort;
        private boolean isDone = false;
        public String udp_messageRequest;
        //Receiver
        public String udp_messageResponse;
        private DatagramPacket pktToReceive;
        private byte[] messageReceived = new byte[1024];


        public UDPTransmitter(String message, InetAddress ipAddress, int port) throws SocketException {
            clientSocket = new DatagramSocket();
            udp_messageRequest = message;
            serverAddress = ipAddress;
            serverPort = port;
        }

        @Override
        public void run() {
            try {
                Long currentTime = System.currentTimeMillis();
                //Transmitter
                messageToSend = new byte[udp_messageRequest.getBytes().length];
                messageToSend = udp_messageRequest.getBytes();
                pktToSend = new DatagramPacket(messageToSend, messageToSend.length, serverAddress, serverPort);
                clientSocket.send(pktToSend);

                //Receiver
                pktToReceive = new DatagramPacket(messageReceived, messageReceived.length);
                //System.out.println("UDP Client Receiver running on PORT:" + clientSocket.getLocalPort());
                clientSocket.receive(pktToReceive);
                udp_messageResponse = new String(pktToReceive.getData()).trim();
                //System.out.println("UDP Response:" + udp_messageResponse);

                if(udp_messageResponse.contains("INVALID LOGIN"))
                {
                    System.out.println("Invalid credentials");
                }

                List<String> response = Arrays.asList(udp_messageResponse.split("#"));
                System.out.println( "HOP" + "\t|| " + "TIME REACHED");
                for(int i = 1; i < response.size(); i ++ ) {
                    currentTime += Long.parseLong(response.get(i).trim());
                    System.out.println( i + "\t|| " + (currentTime % 1000) + "ms");
                }
                if(timeFormat == 1)
                    System.out.println("Server Time: " + response.get(0));
                else
                    System.out.println("Server Time: " + response.get(0));

            } catch (SocketException e) {
                e.printStackTrace();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                clientSocket.close();
            }
        }
    }

    /***
     * Handles TCP Connections - Transmitting and Receiving
     */
    public static class TCPTransmitter extends Thread {
        private Socket clientSocket;
        private String messageToSend;
        private DataOutputStream streamToServer;
        private boolean isDone = false;

        //Receiver
        private BufferedReader receiverBuffer;
        private ServerSocket receiverSocket;
        private Socket connectionSocket;
        String tcp_messageResponse;

        public TCPTransmitter(String message, InetAddress ipAddress, int port) throws IOException {
            clientSocket = new Socket(ipAddress, port);
            streamToServer = new DataOutputStream(clientSocket.getOutputStream());
            messageToSend = message;
            receiverSocket = new ServerSocket(0);
        }

        @Override
        public void run() {
                try {
                    Long currentTime = System.currentTimeMillis();

                    streamToServer.writeBytes(messageToSend + '\n');
                    receiverBuffer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    tcp_messageResponse = receiverBuffer.readLine();
                    System.out.println("TCP Response:" + tcp_messageResponse);

                    if(tcp_messageResponse.contains("INVALID LOGIN"))
                    {
                        System.out.println("Invalid credentials");
                    }

                    List<String> response = Arrays.asList(tcp_messageResponse.split("#"));
                    System.out.println( "HOP" + "\t|| " + "TIME REACHED");
                    for(int i = 1; i < response.size(); i ++ ) {
                        currentTime += Long.parseLong(response.get(i).trim());
                        System.out.println( i + "\t|| " + (currentTime % 1000) + "ms");
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        clientSocket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
        }

        public void stopTCPTransmitter() throws IOException {
            isDone = true;
            clientSocket.close();

        }
    }




}
