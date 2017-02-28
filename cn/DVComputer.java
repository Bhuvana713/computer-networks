import java.io.*;
import java.util.*;
import java.net.*;
public class DVComputer {
	static File file;
	static String curr;
	static String[] totalN;
	static int[] dataAP;
	static String[] dataA;
	static double[][] dataN;
	static int output=1;
	static double[] dataL;
	static String[] dataH;
	static DatagramSocket sock;

	public static <T> void display(T[] a) {
		for(int i=0;i<a.length;i++)
			System.out.print(a[i].toString()+"\t");
		System.out.println();
	}

	public static void initialize(String path,String total_nodes) {
		file=new File(path);
		try {
			BufferedReader read=new BufferedReader(new FileReader(file));
			int len=Integer.parseInt(read.readLine());
			sock=new DatagramSocket(Integer.parseInt(read.readLine()));
			read.close();
			String[] t=file.getName().split(".dat");
			curr=t[0];
			dataA=new String[len];
			dataAP=new int[len];
			totalN=total_nodes.split(":");
			int total_nodes_count=totalN.length;
			dataN=new double[len+1][total_nodes_count];
			dataL=new double[total_nodes_count];
			Arrays.fill(dataN[len],Double.MAX_VALUE);
			dataN[len][getIndex(curr)]=0;
			dataH=new String[total_nodes_count];
		}
		catch(Exception e) { 
			e.printStackTrace();
		}
	}

	public static void read() {
		try {
			if(output==1) {
				Arrays.fill(dataL,Double.MAX_VALUE);
				dataL[getIndex(curr)]=0;
			}
			BufferedReader read=new BufferedReader(new FileReader(file));
			int len=Integer.parseInt(read.readLine());
			Arrays.fill(dataN[len],Double.MAX_VALUE);
			dataN[len][getIndex(curr)]=0;
			read.readLine();
			for(int i=0;i<len;i++) {
				String t1=read.readLine();
				String[] temp=t1.split(" ");
				double cost=Double.parseDouble(temp[1]);
				temp=temp[0].split(":");
				dataN[len][getIndex(temp[0])]=cost;
				dataA[i]=temp[0];
				dataL[getIndex(temp[0])]=(output!=1) ? dataL[getIndex(temp[0])] : cost;
				dataAP[i]=Integer.parseInt(temp[1]);
				dataH[getIndex(temp[0])]=(output!=1) ? dataH[getIndex(temp[0])] : temp[0];
			}
			read.close();
		}
		catch(Exception e) { 
			e.printStackTrace();
		}
	}

	public static int getIndex(String a) {
		for(int i=0;i<totalN.length;i++)
			if(totalN[i].equals(a))
				return i;
		return -1;
	}

	public static void printV() {
		System.out.println("> output number "+output++);
		for(int i=0;i<totalN.length;i++)
			if(!totalN[i].equals(curr)) {
				String dest=totalN[i];
				System.out.print("shortest path "+curr+"-"+dest+": ");
				if(dataL[i]==Double.MAX_VALUE)
					System.out.println("no route found");
				else
					System.out.println("the next hop is "+dataH[i]+" and the cost is "+dataL[i]);
			}
	}

	public synchronized static void compute(String[] vector,int port) {
		int ind=0;
		for(;ind<dataAP.length;ind++)
			if(dataAP[ind]==port)
				break;
		if(ind==dataAP.length)
			return;
		for(int i=0;i<vector.length;i++)
			dataN[ind][i]=Double.parseDouble(vector[i]);
	}

	public static void update() {
		if(output==1)
			return;
		int ind=dataA.length;
		for(int i=0;i<dataA.length;i++) {
			for(int j=0;j<dataL.length;j++) {
				if(!totalN[j].equals(curr)) {
					if(i==0) {
						dataL[j]=dataN[ind][getIndex(dataA[i])]+dataN[i][j];
						dataH[j]=dataA[i];
					}
					else {
						dataH[j]=(dataL[j]<=dataN[ind][getIndex(dataA[i])]+dataN[i][j]) ? dataH[j] : dataA[i];
						dataL[j]=(dataL[j]<=dataN[ind][getIndex(dataA[i])]+dataN[i][j]) ? dataL[j] : (dataN[ind][getIndex(dataA[i])]+dataN[i][j]);
					}
				}
			}
		}
	}

	public static void get() {
		DatagramPacket packet=null;
		while(true) {
			try {
				byte[] recieved=new byte[1024];
				packet=new DatagramPacket(recieved,recieved.length);
				sock.receive(packet);
				//System.out.println();
				new Handler("compute",new String(packet.getData(),0,packet.getLength()),packet.getPort()).start();
			}
			catch(Exception e) {
				System.out.println(e);
			 }
		}
	}
	
	public static void put() {
		DatagramPacket packet;
		try {
			for(int i=0;i<dataA.length;i++) {
				String sent="";
				for(int j=0;j<totalN.length;j++)
					if(dataA[i].equals(dataH[j]))
						sent+=Double.MAX_VALUE+":";
					else
						sent+=dataL[j]+":";
				packet=new DatagramPacket(sent.getBytes(),sent.getBytes().length);
				packet.setAddress(InetAddress.getByName("localhost"));
				packet.setPort(dataAP[i]);
				sock.send(packet);
				//System.out.println(dataA[i]+" - "+sent);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		try {
			initialize(args[0],args[1]);
			Handler reading=new Handler("get");
			reading.start();
			Handler writing=new Handler("put");
			writing.start();
		}
		catch(Exception e) { 
			e.printStackTrace();
		}
	}
}
class Handler extends Thread {
	String control;
	int p;
	String info;
	public Handler(String control) {
		this.control=control;
	}

	public Handler(String control,String info,int p) {
		this.control=control;
		this.p=p;
		this.info=info;
	}
	
	public void run() {
		if(this.control.equalsIgnoreCase("get"))
			DVComputer.get();
		else if(this.control.equalsIgnoreCase("put")) {
			while(true) {
				try {
					DVComputer.read();
					DVComputer.update();
					DVComputer.printV();
					DVComputer.put();
					this.sleep(15*1000);
				}
				catch(Exception e) { }
			}
		}
		else if(this.control.equalsIgnoreCase("compute"))
			DVComputer.compute(this.info.split(":"),this.p);
	}
}