
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
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
    private ArrayList<Socket> neighbor;
    
    public void startAlgorithm() {
        //initializing event listener
        neighbor = new ArrayList<Socket>();
        ThreadEventListener list = new ThreadEventListener() {
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
        //read user input
        Scanner scan = new Scanner(System.in);
        String cmd = scan.nextLine();
        //split each command by whitespce
        String args[] = cmd.split(" ");
        switch (args[0]) {
            case "connect":
                //check if 3 argument is passed
                if (args.length > 1) {
                    try {
                        //correct
                        String ipAddr = args[1];
                        
                        list.print("Menyambungkan ke "+ipAddr + ":" + receiverPort);
                        Socket connect = new Socket(ipAddr, receiverPort);
                        SocketAddress sa = connect.getRemoteSocketAddress();
                        connect.connect(sa, 10);
                        
                        if (connect.isConnected()) {
                            list.print("Berhasil tersambung.");
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
        }
    }
}

interface ThreadEventListener {
    
    void print(String m);
}

class Listener implements Runnable {
    
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
            ev.print("Menunggu kiriman...");
            Socket sock = receiverSocket.accept();
        } catch (IOException ex) {
            ev.print("error " + ex.getMessage());
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
