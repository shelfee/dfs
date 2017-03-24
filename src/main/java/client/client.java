package client;
import server.data_service;

import java.io.*;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;

/**
 * Created by shelfee on 2016/12/11.
 */
public class client {
    public static String getRandomString() { //length表示生成字符串的长度
        String base = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random(System.currentTimeMillis());
        StringBuffer sb = new StringBuffer();
        int length = random.nextInt(55) + 5;
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }
    public static void main(String [] argv) throws IOException {
        String hostName = null;
        long realTime = 0;
        hostName = "rmi://192.168.1.100:8010/center";
        server.center_service centerServer = null;
        try {
            centerServer = (server.center_service) Naming.lookup(hostName);
        } catch (NotBoundException e) {
            e.printStackTrace();
            return;
        }
        String serverSocket = null;

        data_service server = null;

        String serverHost = null;
        String id = "";
        while(true) {
            id = getRandomString();
            try {
                Thread.sleep(1000);
                serverSocket = centerServer.allocate();
                if (serverSocket == null) {
                    System.out.println("No server,wait!");
                    continue;
                }
                serverHost = "rmi://" + serverSocket + "/server";
                try {
                    server = (data_service) Naming.lookup(serverHost);
                    System.out.println("ServerIP:" + serverSocket);
                    System.out.println("Time:" + server.getTime());

                    while (true) {
                        System.out.print("Input command:");
                        Scanner in = new Scanner(System.in);
                        String command = in.nextLine();
                        String[] comms = command.split(" ");
                        boolean flag = false;
                        String dir = null;

                        if (comms.length == 3)
                            if (comms[0].equals("get")) {
                                byte[] data = server.download(comms[1], id);
                                flag = util.file_byte.ByteArray2File(data, comms[2]);
                            } else if (comms[0].equals("put"))
                                flag = server.upload(comms[2], util.file_byte.File2ByteArray(comms[1]), id);
                        if (comms.length == 2)
                            if (comms[0].equals("rm"))
                                flag = server.removeFile(comms[1], id);
                        if (comms.length == 1)
                            if (comms[0].equals("dir")) {
                                dir = server.showDir(id);
                                if (dir != null && !dir.equals("")) {
                                    flag = true;
                                }
                                System.out.print(dir);
                            } else if (comms[0].equals("login")) {
                                String showLogin = server.login(id);
                                System.out.println(showLogin);
                                if(showLogin.equals("Server is full!")) {
                                    break;
                                }
                                flag = true;
                            } else if (comms[0].equals("exit")) {
                                flag = server.exit(id);
                            }
                        if (!flag)
                            System.out.println("Error!");
                    }
                } catch (Exception e) {
                    System.out.println("reconnect to a new data server!");
                }
            } catch (Exception e) {
                System.out.println("Error!");
            }
        }
    }
}
