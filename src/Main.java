import java.io.*;
import java.util.*;

import cs.threephase.FullCube;
import cs.threephase.Search;


public class Main {

	public static void main(String[] args) {
		Stopwatch sw = new Stopwatch();

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

		FullCube cube = new FullCube(new Random(System.nanoTime()));

        Search search = new Search();
		search.with_rotation = false;

		sw.start();

		search.calc(cube);

		System.out.println(sw.millis());
		System.out.println(search.getSolution());

		sw.reset();
		sw.start();

		Byte[] sol = search.byteSolve(cube);
		
		sw.stop();

		for(int i = 0; i < sol.length; i++){
			System.out.print(sol[i].toString() + " ");
		}

		System.out.println();
		System.out.println(sw.millis());

	}
}
