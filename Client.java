
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {

    public static void main(String[] args) {

        System.out.print("Masukkan nama node sekarang : ");
        Scanner scan = new Scanner(System.in);
        String cmd = scan.nextLine();
        cmd = cmd.toUpperCase();
        MainClass main = new MainClass(cmd);
        main.startAlgorithm();
    }

}

class MainClass {

    private final int receiverPort = 8090;
    private String thisNode;
    private ArrayList<Process> neighbor;
    private ThreadEventListener list;
    private ArrayList<Record> table;

    public MainClass(String thisNode) {
        this.thisNode = thisNode;
        table = new ArrayList<Record>();
        //set max size

    }

    private boolean addToTable(Record r) {
        if (table.size() < 6) {
            //max is 5 record
            table.add(r);
            return true;
        } else {

            return false;
        }
    }

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
                            list.print("Menyambungkan ke " + ipAddr + ":" + receiverPort + " (t/o 5 detik)");
                            //connect to neighbor

                            SocketAddress adr = new InetSocketAddress(ipAddr, receiverPort);

                            Socket connect = new Socket();
                            try {
                                connect.connect(adr, 5000);
                            } catch (SocketTimeoutException ste) {
                                list.print("1ce error " + ste.getMessage());
                            }

                            if (connect.isConnected()) {
                                list.print("Berhasil tersambung.");
                                Process p = new Process(pName, connect.getInetAddress().getHostName(), receiverPort, cost);
                                p.setSocket(connect);
                                neighbor.add(p);
                                list.print(p.toString() + " ditambahkan sebagai tetangga (" + neighbor.size() + ")");
                            } else {
                                list.print("Gagal tersambung.");
                            }
                        } catch (IOException ex) {
                            list.print("(1a) error " + ex.getMessage());
                        }
                    } else {
                        list.print("(1b) Error ex : connect 192.168.100.16 20");
                    }
                    break;
                case "q": {
                    isExit = true;
                }
                break;
                case "":
                    break;
                case " ":
                    break;
                case "send": {
                    //send to all neighbor
                    if (!neighbor.isEmpty()) {
                        list.print("Mengirim dari node " + thisNode);
                        for (Process p : neighbor) {
                            //send to all neighbors

                            if (thisNode.equals("A")) {
                                //if it is starting point
                                table.clear();
                                Record record = new Record();
                                record.setFrom(thisNode);
                                record.setTo(p.getName());
                                record.setDistance(p.getCost());
                                table.add(record);
                                list.print(String.valueOf(table.size()));
                                list.print(table.get(0).toString());

                            }
                            send(p, table);
                        }
                    } else {
                        list.print("Belum ada node tetangga");
                    }

                }
                break;
                default:
                    list.print("Perintah tidak dikenal.");
                    break;
            }
        }
    }

    private boolean searchinTable(String to) {
        for (Record record : table) {
            if (record.getTo().equals(to)) {
                return false;
            }
        }
        return true;
    }

    private void send(Process p, ArrayList<Record> t) {

        try {
            ObjectOutputStream oos = new ObjectOutputStream(p.getSocket().getOutputStream());
            list.print("Mengirim data ke node " + p.getName());
            oos.writeObject(t);
            oos.flush();
            //oos.close();
        } catch (IOException ioe) {
            list.print("1cbc error " + ioe.getMessage());
        }

    }
}

interface ThreadEventListener {

    void print(String m);

}

class Node implements Serializable {

    private String name;
    private String ip;
    private int port;
    private String source;
    private int cost;

    public Node(String name, String ip, int port, int cost) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.source = source;
        this.cost = cost;
    }

    public int getCost() {
        return cost;
    }

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getSource() {
        return source;
    }

}

class Process extends Node {

    private Socket s;

    public Process(String name, String ip, int port, int cost) {
        super(name, ip, port, cost);
    }

    public Socket getSocket() {
        return s;
    }

    public void setSocket(Socket s) {
        this.s = s;
    }

    public Node toNode() {
        Node n = new Node(super.getName(), super.getIp(), super.getPort(), super.getCost());
        return n;
    }

    @Override
    public String toString() {
        return super.getName() + " " + s.getInetAddress().getHostAddress() + ":" + s.getPort();
    }

}

//Network Util digunakan untuk mendapatkan ip address sekarang dalam jaringan.
final class NetworkUtil {

    private static String currentHostIpAddress;

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

/*---------------------------------------SERVER--------------------------------------------*/
class Listener implements Runnable {

    private ThreadEventListener ev;
    private int port;
    private ServerSocket serverSocket;
    private ObjectInputStream in;

    public Listener(ThreadEventListener e, int p) {
        try {
            this.ev = e;
            this.port = p;
            serverSocket = new ServerSocket(port);

            ev.print("Receiver listens on " + NetworkUtil.getCurrentEnvironmentNetworkIp() + ":" + port);

        } catch (IOException ex) {
            ev.print("(1f) error Could not listen on port " + port);
            System.exit(-1);
        }
    }

    @Override
    public void run() {

        boolean listening = true;
        /*
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                ev.print("Listening...");
                while (listening) {
                    new MultiServerThread(serverSocket.accept(), ev).start();
                }*/
        while (listening) {
            try {
                ArrayList<Record> table;

                in = new ObjectInputStream(serverSocket.accept().getInputStream());
                table = (ArrayList<Record>) in.readObject();

                MultiServerThread th = new MultiServerThread(table, ev);
                th.start();
            } catch (IOException ex) {
                ev.print("(1fv) error Could not listen on port " + port);
                System.exit(-1);
            } catch (ClassNotFoundException ex) {
                ev.print("(1fvd) error Could not listen on port " + port);
                System.exit(-1);

            }
        }

    }

}

class MultiServerThread extends Thread {

    //private Socket socket = null;
    private ThreadEventListener ev;
    private ArrayList<Record> table;

    public MultiServerThread(ArrayList<Record> t, ThreadEventListener e) {
        super("MultiServer");

        //  this.socket = socket;
        ev = e;
        table = t;

    }

    public void run() {
        boolean listening = true;
        while (listening) {

            for (Record r : table) {
                ev.print(r.toString());
            }
            listening = false;
            //socket.close();
            //in.close();
        }

    }

}

class Record implements Serializable {

    private String from;
    private String to;
    private int distance;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    @Override
    public String toString() {
        return "Record{" + "from=" + from + ", to=" + to + ", distance=" + distance + '}';
    }

}
