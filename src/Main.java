import java.io.*;
import java.util.*;
import java.util.concurrent.TimeoutException;

import cs.threephase.FullCube;
import cs.threephase.Search;


public class Main {

	public static final Stopwatch stopWatch = new Stopwatch();

	public static void main(String[] args) throws TimeoutException {
		printStatusUpdate("PROGRAM START");

		SerialDevice arduino;

		//Initializing .data files
		
		initializeSolveData();

		printStatusUpdate("SOLVER INITIALIZED");

		System.out.println("Connecting to Arduino...");

		try {
			//use {cd /dev} in terminal to get list of all ports
			arduino = new SerialDevice("tty.usbmodem1101", 115200){
				@Override
				public void messageReceived(byte[] msg){
					Main.stopWatch.stop();
				}
			};
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		for(int i = 0; arduino.numMessagesReceived() == 0; i++){
			delay(100);

			//5 second timeout
			if(i == 5*10){
				throw new TimeoutException("Arduino failed to connect, try again");
			}
		}

		printStatusUpdate("ARDUINO CONNECTED");
		delay(1000);

		stopWatch.start();
		arduino.send((byte)'2');

		for(int i = 0; i < 4*10; i++){
			delay(100);
			System.out.println(i + " " + stopWatch.millis());
		}

		arduino.printAllMessagesDebug();

		// FullCube cube = new FullCube(new Random(System.nanoTime()));

        // Search search = new Search();
		// search.with_rotation = false;
		// sw.start();

		// Byte[] sol = search.byteSolve(cube);

		// sw.stop();

		// for(int i = 0; i < sol.length; i++){
		// 	System.out.print(sol[i].toString() + " ");
		// }
		// System.out.println("\n" + sw.millis());

		// System.out.println(sol.length);

	}

	public static void printByteArrayAsString(byte[] arr){
		for(byte b : arr) System.out.print((char)b);
	}

	private static void initializeSolveData(){
		try {
			DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream("twophase.data")));
			cs.min2phase.Tools.initFrom(dis);
			dis.close();
		} catch (IOException e) {
			e.printStackTrace();
			try {
				DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream("twophase.data")));
				cs.min2phase.Tools.saveTo(dos);
				dos.close();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}

		try {
			DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream("threephase.data")));
			cs.threephase.Tools.initFrom(dis);
			dis.close();
		} catch (IOException e) {
			e.printStackTrace();
			try {
				DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream("threephase.data")));
				cs.threephase.Tools.saveTo(dos);
				dos.close();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}
	}

	private static void printStatusUpdate(String str){
		System.out.println("\n======================== " + str + " ========================\n");
		delay(100);
	}

	public static void delay(int ms){
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
