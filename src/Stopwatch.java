import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Stopwatch {
	//Stopwatch is paused when lastStart = 0
	private long lastStart = 0, accumTime = 0;
	private HashMap<String, Double> recordedTimes = new HashMap<>();

	//Resets the elapsed time on the stopwatch and pauses
	public void reset(){
		accumTime = 0;
		lastStart = 0;
	}

	//Resets the elapsed time on the stopwatch and pauses. Records the time previously tracked in a Hashmap linked to the provided label
	public void reset(String label){
		recordedTimes.put(label, nanos());
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

	public double secs(String label){
		return nanos(label) / 1000000000;
	}

	public double millis(String label){
		return nanos(label) / 1000000;
	}

	public double micros(String label){
		return nanos(label) / 1000;
	}

	public double nanos(String label){
		return recordedTimes.get(label);
	}

	public void printAllRecords(){
		Set<Map.Entry<String, Double>> record = recordedTimes.entrySet();

		for(Map.Entry<String, Double> entry : record){
			System.out.println(entry.getKey() + " : " + entry.getValue());
		}
	}
}
