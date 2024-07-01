public class Stopwatch {
	//Stopwatch is paused when lastStart = 0
	private long lastStart = 0, accumTime = 0;

	//Resets the elapsed time on the stopwatch and pauses
	public void reset(){
		accumTime = 0;
		lastStart = 0;
	}

	//Starts timing
	public void start(){
		if(lastStart == 0)
			lastStart = System.nanoTime();
	}

	//Stops timing and records elapsed time
	public void stop(){
		if(lastStart != 0){
			accumTime += System.nanoTime() - lastStart;
			lastStart = 0;
		}
	}

	public double secs(){
		return nanos() / 1000000000;
	}

	public double millis(){
		return nanos() / 1000000;
	}

	public double micros(){
		return nanos() / 1000;
	}

	public double nanos(){
		return lastStart == 0 ? accumTime : accumTime + System.nanoTime() - lastStart;
	}
}
