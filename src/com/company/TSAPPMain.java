package com.company;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Krishna on 10/4/2015.
 */
public class TSAPPMain {
    public static void main(String[] args) {

        boolean endOfSession = true;
        String messageToSend;

        while(true) {
            try {
                BufferedReader messageFromUser = new BufferedReader(new InputStreamReader(System.in));
                messageToSend = messageFromUser.readLine();

                String[] arg = messageToSend.split(" ");
                if (arg.length < 4) {
                    System.out.println("Insufficient arguments");
                } else {
                    String type = arg[1];
                    switch (type) {
                        case "-s":
                            if (arg[2].equals("-T") && arg[3] != null && arg.length >= 6) {
                                setup_server(arg);
                            }
                            break;
                        case "-c":
                            if (arg.length >= 4) {
                                setup_client(arg);
                            }
                            break;
                        case "-p":
                            if(arg.length >= 5){
                                setup_proxy(arg);
                            }
                            break;
                        default:
                            System.out.println("Improper arguments. Use -s/-c/-p");
                            break;
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /***
     * Sets up the Proxy
     * @param arg
     * @throws UnknownHostException
     */
    private static void setup_proxy(String[] arg) throws UnknownHostException {
        List<String> arguments = Arrays.asList(arg);
        if(!(arguments.indexOf("-t") > -1 && arguments.indexOf("-u") > -1)) {
            InetAddress DEST_IP = InetAddress.getByName(arguments.get(arguments.indexOf("-p") + 1));
            int PROXY_UDP_PORT = Integer.parseInt(arguments.get(arguments.size() - 2));
            int PROXY_TCP_PORT = Integer.parseInt(arguments.get(arguments.size() - 1));

            if (DEST_IP != null) {
                TSAPPProxy proxy = new TSAPPProxy(DEST_IP, PROXY_UDP_PORT, PROXY_TCP_PORT);
                proxy.InitiateProxyReceivers();
                if (arguments.contains("--proxy-udp")) {
                    int DEST_UDP_PORT = Integer.parseInt(arguments.get(arguments.indexOf("--proxy-udp") + 1));
                    proxy.setDestUDPReceiverPort(DEST_UDP_PORT);
                    if (arguments.indexOf("-u") > -1) {
                        proxy.setConnectionType("UDP");
                    }

                }
                if (arguments.contains("--proxy-tcp")) {
                    int DEST_TCP_PORT = Integer.parseInt(arguments.get(arguments.indexOf("--proxy-tcp") + 1));
                    proxy.setDestTCPReceiverPort(DEST_TCP_PORT);
                    if (arguments.indexOf("-t") > -1) {
                        proxy.setConnectionType("TCP");
                    }
                }
            }
        }
        else
            System.out.println("Proxy cannot use both TCP and UDP.");
    }

    /***
     * Sets up the Client
     * @param arg
     * @throws IOException
     */
    private static void setup_client(String[] arg) throws IOException {
        try {
            int length = arg.length;
            String messageToSend = "GETTIME";
            String type = "UDP";


            InetAddress DEST_IP = InetAddress.getByName(arg[2]);
            int DEST_PORT = Integer.parseInt(arg[arg.length - 1]);
            TSAPPClient client = new TSAPPClient(DEST_IP, DEST_PORT);
            List<String> arguments = Arrays.asList(arg);

            if(arguments.contains("-z"))
                client.setTimeFormat(1);
            if (length >= 4) {

                type = arguments.indexOf("-t") > -1 ? "TCP" : "UDP";
                if (arguments.indexOf("-T") > -1) {
                    String time = arguments.get(arguments.indexOf("-T") + 1);
                    if (time != null) {
                        String userName = arguments.get(arguments.indexOf("--user") + 1);
                        String password = arguments.get(arguments.indexOf("--pass") + 1);

                        messageToSend = "SETTIME" + "#" + time + "#"
                                + "USERNAME" + "#" + userName + "#"
                                + "PASSWORD" + "#" +password + "#"
                                + "TYPE" + "#" +type;
                    }
                }

            } else
                return;
            if (type == "UDP") {
                if(arguments.contains("-n")){
                    int n = Integer.parseInt(arguments.get(arguments.indexOf("-n") + 1));
                    for(int i = 0 ;i < n; i++) {
                        client.InitiateUDPHandler(messageToSend);
                    }
                }
                else
                    client.InitiateUDPHandler(messageToSend);

            } else if (type == "TCP") {
                if(arguments.contains("-n")){
                    int n = Integer.parseInt(arguments.get(arguments.indexOf("-n") + 1));
                    for(int i = 0 ;i < n; i++) {
                        client.InitiateTCPHandler(messageToSend);
                    }
                }
                else
                    client.InitiateTCPHandler(messageToSend);

            }
        }
        catch(Exception e)
        {
            System.out.println("Improper arguments. Check IP/PORT");
        }
    }

    /***
     * Sets up the Server
     * @param arg
     */
    private static void setup_server(String[] arg) {
        try {
            long time = Long.parseLong(arg[3]);
            int udp_port = Integer.parseInt(arg[arg.length - 2]);
            int tcp_port = Integer.parseInt(arg[arg.length - 1]);

            TSAPPServer server = new TSAPPServer(time, udp_port, tcp_port);
            List<String> arguments = Arrays.asList(arg);
            if (arguments.contains("--user") && arguments.contains("--pass")) {
                String user = arguments.get(arguments.indexOf("--user") + 1);
                String pwd = arguments.get(arguments.indexOf("--pass") + 1);
                server.setCredentials(user, pwd);
            }
            server.initiate();
        }
        catch (NumberFormatException e)
        {
            e.printStackTrace();
        }
    }
}
