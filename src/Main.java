import java.io.*;
import java.util.*;
import java.util.concurrent.TimeoutException;

import cs.threephase.FullCube;
import cs.threephase.Search;
import cs.threephase.Tools;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.*;

import org.bytedeco.opencv.global.opencv_imgproc;


public class Main {

	public static final Stopwatch stopWatch = new Stopwatch();

	private static final boolean USING_SOLVER = true;
	private static final boolean USING_ARDUINO = false;
	private static final boolean USING_WEBCAM = false;

	private static final boolean TESTING_SOLVER = true && USING_SOLVER;
	private static final boolean TESTING_ARDUINO = false && USING_ARDUINO;
	private static final boolean TESTING_WEBCAM = false && USING_WEBCAM;
	private static final boolean TESTING_SOLVER_ARDUINO = true && USING_SOLVER && USING_ARDUINO;
	

	public static void main(String[] args) throws TimeoutException {
		printStatusUpdate("PROGRAM START");

		SerialDevice arduino;
		OpenCVFrameGrabber grabber;

		if(USING_SOLVER){
			printStatusUpdate(">>> INITIALIZE SOLVER");
			//Initializing .data files
			initializeSolveData();
			printStatusUpdate("<<< SOLVER INITIALIZED");;
		}

		if(USING_ARDUINO){
			printStatusUpdate(">>> CONNECT ARDUINO");
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

			printStatusUpdate("<<< ARDUINO CONNECTED");
			delay(1000);
		}

		if(USING_WEBCAM){
			printStatusUpdate(">>> CONNECT WEBCAM");

			System.out.println("Connecting to webcam...");
			grabber = new OpenCVFrameGrabber(0); // 0 for default camera

			try{
				grabber.start();
			}catch(Exception e){
				e.printStackTrace();
			}

			printStatusUpdate("<<< WEBCAM CONNECTED");
		}
	
		printStatusUpdate(" ## SYSTEM INIT COMPLETE ##");

		if(TESTING_SOLVER){
			FullCube cube = new FullCube();
			System.out.println(cube);
		}

		if(TESTING_ARDUINO){
			stopWatch.start();
			arduino.send(new byte[]{101, 101, 101, 101});
	
			delay(500);

			arduino.printAllMessagesDebug();
		}
	
		if(TESTING_WEBCAM){
			CanvasFrame canvasFrame = new CanvasFrame("Webcam");
			OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

			while (canvasFrame.isVisible()) {
				stopWatch.reset();
				stopWatch.start();
				Frame frame = null;
				try{
					frame = grabber.grab();
				}catch(Exception e){
					e.printStackTrace();
				}
				Mat mat = converter.convert(frame);
				
				// Access pixel data
				BytePointer data = mat.data();

				int width = mat.cols();
				int height = mat.rows();
				int channels = mat.channels();

				int[] rgb = getRGBAt(data, width/2, height/2, width, height);
				
				System.out.printf("%d\t%d\t%d\t%f\n", rgb[0], rgb[1], rgb[2], stopWatch.millis());

				canvasFrame.showImage(converter.convert(mat));
			}
			
			try{
				grabber.stop();
				grabber.close();
				converter.close();
			}catch(Exception e){
				e.printStackTrace();
			}
			canvasFrame.dispose();
		}



		if(TESTING_SOLVER_ARDUINO){
			FullCube cube = new FullCube(new Random(System.nanoTime()));

			Search search = new Search();
			search.with_rotation = false;

			Byte[] sol = search.byteSolve(cube);
			String solStr = search.solve(cube);

			System.out.println("Solve Sequence (Length = " + sol.length + "): ");
			for(int i = 0; i < sol.length; i++){
				System.out.print(sol[i].toString() + " ");
			}
			System.out.println("\n\nSolve Sequence (Length = " + sol.length + "): " + solStr);

			System.out.println();
			arduino.send(sol);
		}






		printStatusUpdate("PROGRAM CLEANUP");
		delay(1000);
		if(USING_ARDUINO){
			arduino.printAllMessagesDebug();
			arduino.closeDevice();
		}
		printStatusUpdate("MAIN TERMINATED");
	}

	private static int[] getRGBAt(BytePointer data, int x, int y, int w, int h){
		int ind = y * w * 3 + x * 3;
		return new int[]{data.get(ind + 2)&0xFF,data.get(ind + 1)&0xFF,data.get(ind)&0xFF};
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
			Tools.initFrom(dis);
			dis.close();
		} catch (IOException e) {
			e.printStackTrace();
			try {
				DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream("threephase.data")));
				Tools.saveTo(dos);
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
