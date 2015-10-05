package com.company;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

/**
 * Proxy handles messages between origin and destination using TCP or UDP, whichever the proxy is configured.
 * Created by Krishna on 10/3/2015.
 */

public class TSAPPProxy {
    private static int DEST_UDPRECEIVER_PORT;
    private static int DEST_TCPRECEIVER_PORT;
    private static int PROXY_UDPRECEIVER_PORT;
    private static int PROXY_TCPRECEIVER_PORT;
    private static InetAddress DEST_IPADDRESS;
    private static String CONNECTION_TYPE = "UDP";

    public TSAPPProxy(InetAddress ipAddress, int udp_port, int tcp_port)
    {
        DEST_IPADDRESS = ipAddress;
        PROXY_UDPRECEIVER_PORT = udp_port;
        PROXY_TCPRECEIVER_PORT = tcp_port;
    }

    public static void setDestUDPReceiverPort(int dest_udp_port)
    {
        DEST_UDPRECEIVER_PORT = dest_udp_port;
    }
    public static void setDestTCPReceiverPort(int dest_tcp_port)
    {
        DEST_TCPRECEIVER_PORT = dest_tcp_port;
    }

    public void InitiateProxyReceivers()
    {
        try {
            Thread t2 = new Thread(new UDPReceiver());
            t2.start();
            Thread t1 = new Thread(new TCPReceiver());
            t1.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void setConnectionType(String type)
    {
        CONNECTION_TYPE = type;
    }

    /***
     * TCP Receiver that keeps listening on the specified port
     */
    public class TCPReceiver extends Thread{

        private BufferedReader receiverBuffer;
        private ServerSocket receiverSocket;
        private Socket connectionSocket;
        private boolean isDone = false;
        String tcp_messageReceived;

        public TCPReceiver() throws IOException {
            receiverSocket = new ServerSocket(PROXY_TCPRECEIVER_PORT);
        }

        @Override
        public void run() {
            while(!isDone) {
                try {
                    //System.out.println("TCP Server running PORT :" + receiverSocket.getLocalPort());
                    connectionSocket = receiverSocket.accept();
                    receiverBuffer = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
                    tcp_messageReceived = receiverBuffer.readLine();
                    System.out.println("TCP Message Received:" + tcp_messageReceived);

                    //Read message and check connecion type

                    if(CONNECTION_TYPE.equals("TCP")) {

                        Socket responseSocket = new Socket(DEST_IPADDRESS, DEST_TCPRECEIVER_PORT);
                        RandomPORTTCP2TCPResponseHandler(tcp_messageReceived, responseSocket, connectionSocket);

                    }
                    else
                    {
                        RandomPORTUDP2TCPResponseHandler(tcp_messageReceived, DEST_IPADDRESS, DEST_UDPRECEIVER_PORT, connectionSocket);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        connectionSocket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }

        public void stopTCPReceiver() throws IOException {
            isDone = true;
            connectionSocket.close();

        }
    }


    /***
     * UDP Receiver that keeps listening on the specified port
     */
    public class UDPReceiver extends Thread{
        private DatagramSocket clientSocket;
        private DatagramPacket pktToReceive;
        private byte[] messageReceived = new byte[1024];
        private boolean isDone = false;
        String udp_messageReceived;

        public UDPReceiver() throws SocketException {
            clientSocket = new DatagramSocket(PROXY_UDPRECEIVER_PORT);
        }

        @Override
        public void run() {
            while(!isDone) {
                try {
                    pktToReceive = new DatagramPacket(messageReceived, messageReceived.length);
                    //System.out.println("UDP Server running PORT :" + clientSocket.getLocalPort());
                    clientSocket.receive(pktToReceive);
                    udp_messageReceived = new String(pktToReceive.getData());
                    System.out.println("UDP Message Received:" + udp_messageReceived);

                    //Read message and check connecion type

                    if(CONNECTION_TYPE.equals("UDP")) {

                        RandomPORTUDP2UDPResponseHandler(udp_messageReceived, DEST_IPADDRESS, DEST_UDPRECEIVER_PORT,
                                pktToReceive.getAddress(), pktToReceive.getPort(), clientSocket);

                    }
                    else
                    {
                        Socket responseSocket = new Socket(DEST_IPADDRESS, DEST_TCPRECEIVER_PORT);
                        RandomPORTTCP2UDPResponseHandler(udp_messageReceived, responseSocket,
                                pktToReceive.getAddress(), pktToReceive.getPort(), clientSocket);
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    clientSocket.close();
                }
            }
        }

        public void stopUDPReceiver()
        {
            isDone = true;
            clientSocket.close();

        }
    }

    /***
     * Sends and receives TCP messages on any available port
     */
    public static class RandomTCPTransmitter extends Thread {
        private Socket clientSocket;
        private String messageToSend;
        private DataOutputStream streamToServer;
        private boolean isDone = false;

        //Receiver
        private BufferedReader receiverBuffer;
        private ServerSocket receiverSocket;
        private Socket connectionSocket;
        String tcp_messageResponse;

        public RandomTCPTransmitter(String message, Socket clientSocket) throws IOException {
            this.clientSocket = clientSocket;
            streamToServer = new DataOutputStream(clientSocket.getOutputStream());
            messageToSend = message;
            receiverSocket = new ServerSocket(0);
        }

        @Override
        public void run() {
            try {
                streamToServer.writeBytes(messageToSend + '\n');
                receiverBuffer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                tcp_messageResponse = receiverBuffer.readLine();
                System.out.println("TCP Response:" + tcp_messageResponse);

                if(tcp_messageResponse.contains("INVALID LOGIN"))
                {
                    System.out.println("Invalid credentials");
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

        public String getResponse()
        {
            return tcp_messageResponse;
        }

        public void stopTCPTransmitter() throws IOException {
            isDone = true;
            clientSocket.close();

        }
    }

    /***
     * Sends and receives UDP messages on any available port
     */
    public static class RandomUDPTransmitter extends Thread {
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


        public RandomUDPTransmitter(String message, InetAddress ipAddress, int port) throws SocketException {
            clientSocket = new DatagramSocket();
            udp_messageRequest = message;
            serverAddress = ipAddress;
            serverPort = port;
        }

        @Override
        public void run() {
            try {
                //Transmitter
                messageToSend = new byte[udp_messageRequest.getBytes().length];
                messageToSend = udp_messageRequest.getBytes();
                pktToSend = new DatagramPacket(messageToSend, messageToSend.length, serverAddress, serverPort);
                clientSocket.send(pktToSend);

                //Receiver
                pktToReceive = new DatagramPacket(messageReceived, messageReceived.length);
                System.out.println("UDP Client Receiver running on PORT:" + clientSocket.getLocalPort());
                clientSocket.receive(pktToReceive);
                udp_messageResponse = new String(pktToReceive.getData());
                System.out.println("UDP Response:" + udp_messageResponse);

                if(udp_messageResponse.contains("INVALID LOGIN"))
                {
                    System.out.println("Invalid credentials");
                }


            } catch (SocketException e) {
                e.printStackTrace();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                clientSocket.close();
            }
        }

        public String getResponse()
        {
            return udp_messageResponse;
        }
    }

    /***
     * Uses RandomTCP Transmitter to send and receive messages to destination and responds to origin via TCP
     * @param message
     * @param responseSocket
     * @param connectionSocket
     * @throws IOException
     */
    public void RandomPORTTCP2TCPResponseHandler(String message, Socket responseSocket, Socket connectionSocket) throws IOException {
        Long currentTime = System.currentTimeMillis();
        try {
            RandomTCPTransmitter trans = new RandomTCPTransmitter(message, responseSocket);
            Thread t1 = new Thread(trans);
            t1.start();

            t1.join();
            System.out.println("RTT:" + (System.currentTimeMillis() - currentTime));
            String response = trans.getResponse().trim() + "#" + (System.currentTimeMillis() - currentTime);
            System.out.println(response);
            DataOutputStream streamToServer = new DataOutputStream(connectionSocket.getOutputStream());
            streamToServer.writeBytes(response + "\n");
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            try {
                DataOutputStream streamToServer = new DataOutputStream(connectionSocket.getOutputStream());
                streamToServer.writeBytes("Connection Timed out." + "#" + (System.currentTimeMillis() - currentTime) + "\n");
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }
    }

    /***
     * Uses RandomTCP Transmitter to send and receive messages to destination and responds to origin via UDP
     * @param message
     * @param responseSocket
     * @param responseAddress
     * @param responsePort
     * @param connectionSocket
     * @throws IOException
     */
    public void RandomPORTTCP2UDPResponseHandler(String message, Socket responseSocket, InetAddress responseAddress, int responsePort, DatagramSocket connectionSocket) throws IOException {
        Long currentTime = System.currentTimeMillis();
        try {
            RandomTCPTransmitter trans = new RandomTCPTransmitter(message, responseSocket);
            Thread t1 = new Thread(trans);
            t1.start();

            t1.join();
            System.out.println("RTT:" +(System.currentTimeMillis() - currentTime));
            message = (trans.getResponse().trim() + "#" + (System.currentTimeMillis() - currentTime));
            byte[] response = message.getBytes();
            System.out.println(message);
            DatagramPacket pktToSend = new DatagramPacket(response, response.length,
                    responseAddress, responsePort);
            System.out.println("Message sent to IP: " + responseAddress + " PORT: " + responsePort);
            connectionSocket.send(pktToSend);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            byte[] response = ("Connection Timedout." + "#" + (System.currentTimeMillis() - currentTime)).getBytes();
            DatagramPacket pktToSend = new DatagramPacket(response, response.length,
                    responseAddress, responsePort);
            System.out.println("Message sent to IP: " + responseAddress + " PORT: " + responsePort);
            connectionSocket.send(pktToSend);
            try {
                connectionSocket.send(pktToSend);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    /***
     * Uses RandomUDP Transmitter to send and receive messages to destination and responds to origin via UDP
     * @param message
     * @param DEST_IPADDRESS
     * @param DEST_UDPRECEIVER_PORT
     * @param responseAddress
     * @param responsePort
     * @param responseSocket
     * @throws IOException
     */
    public void RandomPORTUDP2UDPResponseHandler(String message, InetAddress DEST_IPADDRESS, int DEST_UDPRECEIVER_PORT, InetAddress responseAddress, int responsePort, DatagramSocket responseSocket) throws IOException {
        Long currentTime = System.currentTimeMillis();
        try {
            RandomUDPTransmitter trans = new RandomUDPTransmitter(message, DEST_IPADDRESS, DEST_UDPRECEIVER_PORT);
            Thread t1 = new Thread(trans);
            t1.start();
            t1.join();
            System.out.println("RTT:" +(System.currentTimeMillis() - currentTime));
            message = (trans.getResponse().trim() + "#" + (System.currentTimeMillis() - currentTime));
            System.out.println(message);
            byte[] response = message.getBytes();
            DatagramPacket pktToSend = new DatagramPacket(response, response.length,
                    responseAddress, responsePort);

            System.out.println("Message sent to IP: " + responseAddress + " PORT: " + responsePort);
            responseSocket.send(pktToSend);
        } catch (InterruptedException e) {
            e.printStackTrace();
            byte[] response = ("Connection Timedout." + "#" + (System.currentTimeMillis() - currentTime)).getBytes();
            DatagramPacket pktToSend = new DatagramPacket(response, response.length,
                    responseAddress, responsePort);

            System.out.println("Message sent to IP: " + responseAddress + " PORT: " + responsePort);
            try {
                responseSocket.send(pktToSend);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    /***
     * Uses RandomUDP Transmitter to send and receive messages to destination and responds to origin via TCP
     * @param message
     * @param DEST_IPADDRESS
     * @param DEST_UDPRECEIVER_PORT
     * @param connectionSocket
     * @throws IOException
     */
    public void RandomPORTUDP2TCPResponseHandler(String message, InetAddress DEST_IPADDRESS, int DEST_UDPRECEIVER_PORT, Socket connectionSocket) throws IOException {
        Long currentTime = System.currentTimeMillis();
        try {
            RandomUDPTransmitter trans = new RandomUDPTransmitter(message, DEST_IPADDRESS, DEST_UDPRECEIVER_PORT);
            Thread t1 = new Thread(trans);
            t1.start();
            t1.join();
            System.out.println("RTT:" + (System.currentTimeMillis() - currentTime));
            String response = trans.getResponse().trim() + "#" + (System.currentTimeMillis() - currentTime);
            System.out.println(response);
            DataOutputStream streamToServer = new DataOutputStream(connectionSocket.getOutputStream());
            streamToServer.writeBytes(response + "\n");
        } catch (InterruptedException e) {
            e.printStackTrace();
            try {
                DataOutputStream streamToServer = new DataOutputStream(connectionSocket.getOutputStream());
                streamToServer.writeBytes("Connection Timed out." + "#" + (System.currentTimeMillis() - currentTime) + "\n");
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
}
