import java.io.*;
import java.util.*;
import java.util.concurrent.TimeoutException;

import cs.threephase.FullCube;
import cs.threephase.Search;


public class Main {

	public static final Stopwatch stopWatch = new Stopwatch();

	private static final boolean USING_SOLVER = true;
	private static final boolean TESTING_SOLVER = true && USING_SOLVER;
	private static final boolean USING_ARDUINO = false;
	private static final boolean TESTING_ARDUINO = true && USING_ARDUINO;

	public static void main(String[] args) throws TimeoutException {
		printStatusUpdate("PROGRAM START");

		SerialDevice arduino;

		if(USING_SOLVER){
			//Initializing .data files
			initializeSolveData();

			printStatusUpdate("SOLVER INITIALIZED");
		}

		if(USING_ARDUINO){
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
				if(i == 10*10){
					throw new TimeoutException("Arduino failed to connect, try again");
				}
			}

			printStatusUpdate("ARDUINO CONNECTED");
			delay(1000);
		}

		if(TESTING_SOLVER){
			FullCube cube = new FullCube(new Random(System.nanoTime()));

			Search search = new Search();
			search.with_rotation = false;
			stopWatch.start();

			Byte[] sol = search.byteSolve(cube);

			stopWatch.stop();

			for(int i = 0; i < sol.length; i++){
				System.out.print(sol[i].toString() + " ");
			}
			System.out.println("\n" + stopWatch.millis());

			System.out.println(sol.length);
		}

		if(TESTING_ARDUINO){
			stopWatch.start();
			arduino.send(new byte[]{101, 101, 101, 101});
	
			for(int i = 0; i < 6*10; i++){
				delay(100);
				System.out.println(i + " " + stopWatch.millis());
			}
	
			arduino.printAllMessagesDebug();
		}
	
		printStatusUpdate("PROGRAM TERMINATED");
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
		delay(100);
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
