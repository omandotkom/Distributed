
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {
    
    public static void main(String[] args) {
        MainClass main = new MainClass();
        main.startAlgorithm();
    }
}

class MainClass {
    
    private final int receiverPort = 8090;
    private final int senderPort = 9000;
    private ArrayList<Process> neighbor;
    private ThreadEventListener list;

    public void startAlgorithm() {
        boolean isExit = false;
//initializing event listener
        neighbor = new ArrayList<Process>();
        list = new ThreadEventListener() {
            @Override
            public void print(String m) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
                LocalDateTime now = LocalDateTime.now();
                System.out.println(dtf.format(now) + "> " + m);
            }
            
        };
        //assign thread with listener and port
        Thread thread = new Thread(new Listener(list, receiverPort));
        //run the thread
        thread.start();
        list.print("Current ip address : " + NetworkUtil.getCurrentEnvironmentNetworkIp());
        
        while (!isExit) {

//read user input
            Scanner scan = new Scanner(System.in);
            System.out.print("command > ");
            String cmd = scan.nextLine();
            //split each command by whitespce
            String args[] = cmd.split(" ");
            switch (args[0]) {
                case "connect":
                    //check if 3 argument is passed
                    if (args.length > 3) {
                        try {
                            //correct
                            String pName = args[1];
                            String ipAddr = args[2];
                            int cost = Integer.valueOf(args[3]);
                            list.print("Menyambungkan ke " + ipAddr + ":" + receiverPort);
                            //connect to neighbor
                            Socket connect = new Socket(ipAddr, receiverPort);
                            if (connect.isConnected()) {
                                list.print("Berhasil tersambung.");
                                Process p = new Process(pName, connect, cost);
                                neighbor.add(p);
                                list.print(p.toString() + " ditambahkan sebagai tetangga");
                            } else {
                                list.print("Gagal tersambung.");
                            }
                        } catch (IOException ex) {
                            list.print("error " + ex.getMessage());
                        }
                    } else {
                        list.print("Error ex : connect 192.168.100.16 20");
                    }
                    break;
                case "q": {
                    isExit = true;
                }
                break;
                case "send": {
                    //send to all neighbor
                    for (Process p : neighbor) {
                        //send to all neighbors
                        send(p);
                    }
                }
                break;
            }
        }
    }
    
    private void send(Process p) {
        try {
            
            ObjectOutputStream oos = new ObjectOutputStream(p.getSocket().getOutputStream());
            list.print("Mengirim data ke node " + p.getName());
            oos.writeObject(p.getCost());
        } catch (IOException ex) {
            list.print("error " + ex.getMessage());
        }
    }
}

interface ThreadEventListener {
    
    void print(String m);
    
}

interface Status {
    
    void close();
}

class Process implements Serializable {
    
    private String name;
    private Socket s;
    private int cost;
    
    public Process(String name, Socket s, int cost) {
        this.name = name;
        this.s = s;
        this.cost = cost;
    }
    
    public int getCost() {
        return cost;
    }
    
    public void setCost(int cost) {
        this.cost = cost;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Socket getSocket() {
        return s;
    }
    
    public void setS(Socket s) {
        this.s = s;
    }
    
    @Override
    public String toString() {
        return this.name + s.getInetAddress().getHostAddress() + ":" + s.getPort();
    }
    
}

class Listener implements Runnable, Status {
    
    private ServerSocket receiverSocket;
    private ThreadEventListener ev;
    private int port;
    
    public Listener(ThreadEventListener e, int p) {
        this.ev = e;
        this.port = p;
        ev.print("Receiver listens on " + NetworkUtil.getCurrentEnvironmentNetworkIp() + ":" + port);
    }
    
    @Override
    public void run() {
        try {
            
            receiverSocket = new ServerSocket(port);
            while (true) {
                ev.print("Menunggu kiriman...");
                Socket sock = receiverSocket.accept();
                ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
                String message = ois.readObject().toString();
                ev.print("Isi pesan adalah " + message);
            }
        } catch (IOException ex) {
            ev.print("error " + ex.getMessage());
        } catch (ClassNotFoundException ex) {
            ev.print("error " + ex.getMessage());
           
        }
    }
    
    @Override
    public void close() {
        //trying to close server
        if (receiverSocket != null) {
            if (!receiverSocket.isClosed()) {
                try {
                    ev.print("Menutuput socket receiver...");
                    receiverSocket.close();
                    if (receiverSocket.isClosed()) {
                        ev.print("Berhasil menutup.");
                    }
                } catch (IOException ex) {
                    ev.print("Gagal menutup receiver socket, " + ex.getMessage());
                }
            }
        }
    }
    
}
//Network Util digunakan untuk mendapatkan ip address sekarang dalam jaringan.

final class NetworkUtil {

    /**
     * The current host IP address is the IP address from the device.
     */
    private static String currentHostIpAddress;

    /**
     * @return the current environment's IP address, taking into account the
     * Internet connection to any of the available machine's Network interfaces.
     * Examples of the outputs can be in octats or in IPV6 format.      <pre>
     *         ==> wlan0
     *
     *         fec0:0:0:9:213:e8ff:fef1:b717%4
     *         siteLocal: true
     *         isLoopback: false isIPV6: true
     *         130.212.150.216 <<<<<<<<<<<------------- This is the one we want to grab so that we can.
     *         siteLocal: false                          address the DSP on the network.
     *         isLoopback: false
     *         isIPV6: false
     *
     *         ==> lo
     *         0:0:0:0:0:0:0:1%1
     *         siteLocal: false
     *         isLoopback: true
     *         isIPV6: true
     *         127.0.0.1
     *         siteLocal: false
     *         isLoopback: true
     *         isIPV6: false
     * </pre>
     */
    public static String getCurrentEnvironmentNetworkIp() {
        if (currentHostIpAddress == null) {
            Enumeration<NetworkInterface> netInterfaces = null;
            try {
                netInterfaces = NetworkInterface.getNetworkInterfaces();
                
                while (netInterfaces.hasMoreElements()) {
                    NetworkInterface ni = netInterfaces.nextElement();
                    Enumeration<InetAddress> address = ni.getInetAddresses();
                    while (address.hasMoreElements()) {
                        InetAddress addr = address.nextElement();
                        //                      log.debug("Inetaddress:" + addr.getHostAddress() + " loop? " + addr.isLoopbackAddress() + " local? "
                        //                            + addr.isSiteLocalAddress());
                        if (!addr.isLoopbackAddress() && addr.isSiteLocalAddress()
                                && !(addr.getHostAddress().indexOf(":") > -1)) {
                            currentHostIpAddress = addr.getHostAddress();
                        }
                    }
                }
                if (currentHostIpAddress == null) {
                    currentHostIpAddress = "127.0.0.1";
                }
                
            } catch (SocketException e) {
//                log.error("Somehow we have a socket error acquiring the host IP... Using loopback instead...");
                currentHostIpAddress = "127.0.0.1";
            }
        }
        return currentHostIpAddress;
    }
}
