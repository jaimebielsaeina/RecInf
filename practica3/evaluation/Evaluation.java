import java.util.Map;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Arrays;
import java.util.ArrayList;


public class Evaluation {


	public static class PRPoint 
	{
		public double recall;
		public double precision;

		public PRPoint(double recall, double precision) {
			this.recall = recall;
			this.precision = precision;
		}
	}

	public static double AvgPrecision(boolean[] relevantDocuments, Integer K)
	{
		if (K <= 0)
			K = relevantDocuments.length;
	
		int numRelevantDocuments = 0;
	
		for (int i = 0; i < K; i++) 
				if (relevantDocuments[i]) numRelevantDocuments++;
	
		return (double) numRelevantDocuments / Integer.min(K, relevantDocuments.length);
	}

	public static double AvgPrecision(boolean[] relevantDocuments) 
	{
		return AvgPrecision(relevantDocuments, relevantDocuments.length);
	}

	public static double MeanAvgPrecision(boolean[][] relevantDocuments, int K) 
	{
		if (K <= 0) {
			throw new IllegalArgumentException("K must be greater than 0.");
		}
	
		double mapSum = 0.0;
	
		for (int i = 0; i < relevantDocuments.length; i++) {
			mapSum += AvgPrecision(relevantDocuments[i], K);
		}
	
		return mapSum / relevantDocuments.length;
	}

	public static double MeanAvgPrecision(boolean[][] relevantDocuments) 
	{
		return MeanAvgPrecision(relevantDocuments, relevantDocuments.length);
	}

	public static double AvgRecall(boolean[] relevantDocuments, int K, Integer numTotalRelevantDocuments) 
	{
		if (K <= 0) {
			throw new IllegalArgumentException("K must be greater than 0.");
		}
	
		int numRelevantDocuments = 0;
	
		if (numTotalRelevantDocuments == 0) {
			return 0.0; // Avoid division by zero
		}
	
		for (int i = 0; i < K && i < relevantDocuments.length; i++) 
		{
			if (relevantDocuments[i]) {
				numRelevantDocuments++;
			}
		}
	
		return (double) numRelevantDocuments / numTotalRelevantDocuments;
	}

	public static double AvgRecall(boolean[] relevantDocuments, Integer numTotalRelevantDocuments) 
	{
		return AvgRecall(relevantDocuments, relevantDocuments.length, numTotalRelevantDocuments);
	}

	public static double MeanAvgRecall(boolean[][] relevantDocuments, int K, Integer numTotalRelevantDocuments) 
	{
		if (K <= 0) {
			throw new IllegalArgumentException("K must be greater than 0.");
		}
	
		double marSum = 0.0;
	
		for (int i = 0; i < relevantDocuments.length; i++) {
			marSum += AvgRecall(relevantDocuments[i], K, numTotalRelevantDocuments);
		}
	
		return marSum / relevantDocuments.length;
	}

	public static double MeanAvgRecall(boolean[][] relevantDocuments, Integer numTotalRelevantDocuments)
	{
		return MeanAvgRecall(relevantDocuments, relevantDocuments.length, numTotalRelevantDocuments);
	}
	

	public static double F1Score(boolean[] relevantDocuments, Integer numTotalRelevantDocuments) 
	{
		double precision = AvgPrecision(relevantDocuments);
		double recall = AvgRecall(relevantDocuments, numTotalRelevantDocuments);

		if (precision + recall == 0) {
			// Handle the case where both precision and recall are zero to avoid division by zero.
			return 0.0;
		} else {
			return 2 * (precision * recall) / (precision + recall);
		}
	}

	public static double F1Score(double precision, double recall) 
	{
		if (precision + recall == 0) {
			// Handle the case where both precision and recall are zero to avoid division by zero.
			return 0.0;
		} else {
			return 2 * (precision * recall) / (precision + recall);
		}
	}

	public static double MeanF1Score(boolean[][] relevantDocuments, Integer numTotalRelevantDocuments) 
	{
		double f1Sum = 0.0;

		for (int i = 0; i < relevantDocuments.length; i++) {
			f1Sum += F1Score(relevantDocuments[i], numTotalRelevantDocuments);
		}

		return f1Sum / relevantDocuments.length;
	}

	public static ArrayList<PRPoint> computePrecisionRecallCurve(boolean[] relevantDocuments, Integer numTotalRelevantDocuments) 
	{
		ArrayList<PRPoint> precisionRecallCurve = new ArrayList<>();
		
		int numRetrievedDocuments = 0;

		for (int i = 0; i < relevantDocuments.length; i++) 
		{
			if (relevantDocuments[i]) {
				numRetrievedDocuments++;
			}

			double precision = (double) numRetrievedDocuments / (i + 1);
			double recall = (double) numRetrievedDocuments / numTotalRelevantDocuments;

			precisionRecallCurve.add(new PRPoint(recall, precision));
		}

		return precisionRecallCurve;
	}

	public static ArrayList<PRPoint> computeAveragePrecisionRecallCurve(boolean[][] relevantDocuments, Integer numTotalRelevantDocuments) 
	{
		ArrayList<PRPoint> averagePrecisionRecallCurve = new ArrayList<>();
		int length = relevantDocuments[0].length;
		
		for (int i = 0; i < relevantDocuments.length; i++) 
		{
			ArrayList<PRPoint> precisionRecallCurve = computePrecisionRecallCurve(relevantDocuments[i], numTotalRelevantDocuments);
			
			// Average the points
			for (int j = 0; j < precisionRecallCurve.size(); j++) 
			{
				if (i == 0) 
				{
					PRPoint point = precisionRecallCurve.get(j);
					point.precision /= length;
					point.recall /= length;

					averagePrecisionRecallCurve.add(point);
				}
				else 
				{
					PRPoint point = averagePrecisionRecallCurve.get(j);
					point.precision += precisionRecallCurve.get(j).precision/length;
					point.recall += precisionRecallCurve.get(j).recall/length;

					averagePrecisionRecallCurve.set(j, point);
				}
			}
		}
		
		return averagePrecisionRecallCurve;
	}

	public static ArrayList<PRPoint> applyStepFunction(ArrayList<PRPoint> precisionRecallPoints) 
	{
		// Copy points
		ArrayList<PRPoint> stepFunctionCurve = new ArrayList<>(precisionRecallPoints); 

		double maxP = 0;

		// Reverse traverse the array
		for (int i = stepFunctionCurve.size(); i > 0; i--) 
		{
			// If the point is higher than the highest point at the right
			if (stepFunctionCurve.get(i).precision > maxP) {
				// Update the maximum found
				maxP = stepFunctionCurve.get(i).precision;
			}
			else {
				// Interpolate the curve point
				PRPoint p = stepFunctionCurve.get(i);
				p.precision = maxP;

				stepFunctionCurve.set(i, p);
			}
		}

		return stepFunctionCurve;
	}

	public static ArrayList<PRPoint> reducePrecisionRecallCurve(ArrayList<PRPoint> curve, int n) 
	{
		if (n <= 0) {
			throw new IllegalArgumentException("The number of points (n) must be greater than 0.");
		}
	
		int totalPoints = curve.size();
	
		if (totalPoints <= n) {
			return curve; // No reduction needed
		}
	
		double stepSize = (double) totalPoints / (n - 1);
		ArrayList<PRPoint> reducedCurve = new ArrayList<>();
	
		for (int i = 0; i < n; i++) {
			int index = (int) Math.round(i * stepSize);
			reducedCurve.add(curve.get(index));
		}
	
		return reducedCurve;
	}


	public static void main (String[] args) {

		String qRelsFileName = "", resultsFileName = "", outputFileName = "";
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
				case "-qrels":		qRelsFileName = args[++i]; break;
				case "-results":	resultsFileName = args[++i]; break;
				case "-output":		outputFileName = args[++i]; break;
				default:
					System.err.println("Unknown option: " + args[i]);
					System.exit(1);
			}
		}
		if (qRelsFileName.equals("") || resultsFileName.equals("") || outputFileName.equals("")) {
			System.err.println("Missing parameters. Correct usage is:");
			System.err.println("java Evaluation -qrels <qrels_file> -results <results_file> -output <output_file>");
			System.exit(1);
		}
		// Read qRels file
		Map<String, Map<String, Boolean>> qRels = new HashMap<String, Map<String, Boolean>>();
		try {
			BufferedReader qRelsFile = new BufferedReader(new FileReader(qRelsFileName));
			String line;
			while ((line = qRelsFile.readLine()) != null) {
				String[] parts = line.trim().split("[ \t]+");
				if (!qRels.containsKey(parts[0]))
						qRels.put(parts[0], new HashMap<String, Boolean>());
				qRels.get(parts[0]).put(parts[1], Integer.parseInt(parts[2]) == 1);
			}
			qRelsFile.close();
		} catch (Exception e) {
			System.err.println("Error reading qRels file: " + e.getMessage());
			System.exit(1);
		}
		
		// Read results file
		Map<String, TreeMap<String, Boolean>> results = new HashMap<String, TreeMap<String, Boolean>>();
		try {
			BufferedReader resultsFile = new BufferedReader(new FileReader(resultsFileName));
			String line;
			while ((line = resultsFile.readLine()) != null) {
				String[] parts = line.trim().split("[ \t]+");
				if (!results.containsKey(parts[0]))
						results.put(parts[0], new TreeMap<String, Boolean>());
				results.get(parts[0]).put(parts[1], true);
			}
			resultsFile.close();
		} catch (Exception e) {
			System.err.println("Error reading results file: " + e.getMessage());
			System.exit(1);
		}

		for (String qRel : qRels.keySet()) {
			boolean[] areRelevant = new boolean[results.get(qRel).size()];
			int i = 0, totalRelevantes = 0;
			for (String infoNeed : results.get(qRel).keySet()) {
				areRelevant[i++] = qRels.get(qRel).get(infoNeed);
				if (qRels.get(qRel).get(infoNeed))
						totalRelevantes++;
			}
			for (i = 0; i < areRelevant.length; i++) {
				System.out.print(areRelevant[i] ? "1" : "0");
			}

			System.out.println();
			System.out.println("Necesidad de informacion: " + qRel);
			double precision = AvgPrecision(areRelevant, 0);
			double recall = AvgRecall(areRelevant, 0, totalRelevantes);
			System.out.println("precision " + precision);
			System.out.println("recall " + recall);
			System.out.println("F1 " + F1Score(precision, recall));
		}


	}
}
