package com.company;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;


/**
 * Server listens on UDP and TCP on two specified ports and responds on any one of the transmitters using UDP or TCP.
 * Created by Krishna on 10/1/2015.
 */
public class TSAPPServer {

    private static long serverTime;
    private static int UDP_RECEIVER_PORT;
    private static int TCP_RECEIVER_PORT;
    private static String userName;
    private static String password;

    public TSAPPServer(long time, int udp_port, int tcp_port)
    {
        serverTime = time;
        UDP_RECEIVER_PORT = udp_port;
        TCP_RECEIVER_PORT = tcp_port;
    }

    public void initiate() {
        try {
            Thread t1 = new Thread (new UDPReceiver());
            t1.start();
            Thread t2 = new Thread (new TCPReceiver());
            t2.start();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /***
     * UDP Receiver that listens on the UDP RECEIVER PORT
     */
    public class UDPReceiver extends Thread{
        private DatagramSocket clientSocket;
        private DatagramPacket pktToReceive;
        private byte[] messageReceived = new byte[1024];
        private boolean isDone = false;
        String udp_messageReceived;

        public UDPReceiver() throws SocketException {
            clientSocket = new DatagramSocket(UDP_RECEIVER_PORT);
        }

        @Override
        public void run() {
            while(!isDone) {
                try {
                    Long currenTime = System.currentTimeMillis();
                    pktToReceive = new DatagramPacket(messageReceived, messageReceived.length);
                    System.out.println("UDP Server running PORT :" + clientSocket.getLocalPort());
                    clientSocket.receive(pktToReceive);
                    udp_messageReceived = new String(pktToReceive.getData());
                    System.out.println("UDP Message Received:" + udp_messageReceived);

                    if(!udp_messageReceived.equals("GETTIME")) {
                        List<String> msgs = Arrays.asList(udp_messageReceived.split("#"));

                        if(msgs.contains("SETTIME")) {
                            long time = Long.parseLong(msgs.get(msgs.indexOf("SETTIME") + 1));
                            String userName = msgs.get(msgs.indexOf("USERNAME") + 1);
                            String password = msgs.get(msgs.indexOf("PASSWORD") + 1);

                            if(userName.equals(TSAPPServer.userName) && password.equals(TSAPPServer.password)) {
                                setServerTime(time);
                                System.out.println("Server time set");
                            }
                            else
                            {
                                String response = "INVALID LOGIN".toString();
                                Thread t1 = new Thread(new UDPTransmitter(response, pktToReceive.getAddress(), pktToReceive.getPort()));
                                t1.start();
                                continue;
                            }
                        }
                    }

                    Calendar cal_Two = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    cal_Two.setTimeInMillis(serverTime);

                    //Send response
                    System.out.println("Message sent to IP: " + pktToReceive.getAddress() + " PORT: " + pktToReceive.getPort());
                    String response = (cal_Two.toInstant() + "#" + (System.currentTimeMillis() - currenTime)).toString();
                    Thread t1 = new Thread(new UDPTransmitter(response, pktToReceive.getAddress(), pktToReceive.getPort()));
                    t1.start();
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
     * UDP Transmitter that sends messages to any given IP and port
     */
    public class UDPTransmitter extends Thread {
        private DatagramPacket pktToSend;
        private byte[] messageToSend;
        private InetAddress serverAddress;
        private int serverPort;
        private boolean isDone = false;
        public String udp_messageRequest;
        public DatagramSocket clientSocket;

        public UDPTransmitter(String message, InetAddress ipAddress, int port) throws SocketException {
            clientSocket = new DatagramSocket();
            udp_messageRequest = message;
            serverAddress = ipAddress;
            serverPort = port;
        }

        @Override
        public void run() {
            try {
                messageToSend = new byte[udp_messageRequest.getBytes().length];
                messageToSend = udp_messageRequest.getBytes();
                pktToSend = new DatagramPacket(messageToSend, messageToSend.length, serverAddress, serverPort);
                System.out.println("UDP Message Sent:" + udp_messageRequest);
                clientSocket.send(pktToSend);

            } catch (SocketException e) {
                e.printStackTrace();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                clientSocket.close();
            }

        }

        public void stopUDPTransmitter()
        {
            isDone = true;
            clientSocket.close();

        }
    }

    /***
     * TCP Receiver that listens on the TCP RECEIVER PORT
     */
    public class TCPReceiver extends Thread{

        private BufferedReader receiverBuffer;
        private ServerSocket receiverSocket;
        private Socket connectionSocket;
        private boolean isDone = false;
        String tcp_messageResponse;

        public TCPReceiver() throws IOException {
            receiverSocket = new ServerSocket(TCP_RECEIVER_PORT);
        }

        @Override
        public void run() {
            while(!isDone) {
                Long currentTime = System.currentTimeMillis();
                try {
                    System.out.println("TCP Server running PORT :" + receiverSocket.getLocalPort());
                    connectionSocket = receiverSocket.accept();
                    receiverBuffer = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
                    tcp_messageResponse = receiverBuffer.readLine();
                    System.out.println("TCP Message Received:" + tcp_messageResponse);


                    List<String> msgs = Arrays.asList(tcp_messageResponse.split("#"));

                    if(msgs.contains("SETTIME")) {
                        long time = Long.parseLong(msgs.get(msgs.indexOf("SETTIME") + 1));
                        String userName = msgs.get(msgs.indexOf("USERNAME") + 1);
                        String password = msgs.get(msgs.indexOf("PASSWORD") + 1);

                        if(userName.equals(TSAPPServer.userName) && password.equals(TSAPPServer.password)) {
                            setServerTime(time);
                            System.out.println("Server time set");
                        }else
                        {
                            String response = "INVALID LOGIN".toString();
                            Thread t1 = new Thread(new TCPTransmitter(response, connectionSocket));
                            t1.start();
                            continue;
                        }
                    }
                    Calendar cal_Two = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    cal_Two.setTimeInMillis(serverTime);

                    //Send response
                    String response = (cal_Two.toInstant() + "#" + (System.currentTimeMillis() - currentTime)).toString();
                    Thread t1 = new Thread(new TCPTransmitter(response, connectionSocket));
                    t1.start();
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
     * TCP Transmitter that sends messages to any given IP and port
     */
    public class TCPTransmitter extends Thread {
        //private Socket clientSocket;
        private String messageToSend;
        private DataOutputStream streamToServer;
        private boolean isDone = false;

        public TCPTransmitter(String message, Socket clientSocket) throws IOException {
            streamToServer = new DataOutputStream(clientSocket.getOutputStream());
            messageToSend = message;
        }

        @Override
        public void run() {
            try {
                streamToServer.writeBytes(messageToSend + '\n');
                System.out.println("TCP Message Sent:" + messageToSend);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void stopTCPTransmitter() throws IOException {
            isDone = true;
        }
    }

    public void setServerTime(long time)
    {
        serverTime = time;
    }

    public void setCredentials(String username, String pwd)
    {
        userName = username;
        password = pwd;
    }
}
