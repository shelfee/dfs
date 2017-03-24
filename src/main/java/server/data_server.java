package server;

import java.io.File;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by shelfee on 2016/12/10.
 */
public class data_server {
   // public static Map<String, Long> FileSign = Collections.synchronizedMap(new HashMap<String, Long>());
    public static Map clients = Collections.synchronizedMap(new HashMap<String, Long>());
    public static boolean close = false;
    public static String rootpath;
    public static String keep_port;
    public static String service_port;
    public static void main(String argv[]) {
        rootpath = "root" + argv[2] + "/";
        try {
            File root = new File(rootpath);
            if (!root.exists()){
                root.mkdirs();
            }
            else{
                System.out.println(root.getAbsolutePath());
            }
            InetAddress addr = InetAddress.getLocalHost();
            String ip = addr.getHostAddress();//获得本机Ip
            keep_port = argv[1];
            service_port = argv[2];

            String hostName1 = "rmi://" + ip + ":" + String.valueOf(keep_port) + "/keeper";
            System.out.println(hostName1);
            LocateRegistry.createRegistry(Integer.parseInt(keep_port));
            data_server_keeper t1 = data_server_keeperImpl.getInstance();
            Naming.rebind(hostName1, t1);

            String hostName = "rmi://" + ip + ":" + String.valueOf(service_port) + "/server";
            System.out.println(hostName);
            LocateRegistry.createRegistry(Integer.parseInt(service_port));
            data_service t = data_serviceImpl.getInstance(argv[0]);
            Naming.rebind(hostName, t);
            while(true){
                if(close == true){
                    System.out.println("Sleep Node " + ip);
                    System.exit(0);
                }
                data_serviceImpl.centerServer.alive(ip, keep_port, service_port);
                Iterator<Map.Entry<String, Long>> it = clients.entrySet().iterator();
                long time = System.currentTimeMillis();
                while (it.hasNext()) {
                    Map.Entry<String, Long> entry = it.next();
                    if(time - entry.getValue() >3000000 && entry.getValue() != 0){
                        //System.out.println(entry.getKey() + "time out.");
                        it.remove();
                    }
                }
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Close Node!");
            System.exit(0);
        }

    }
}
