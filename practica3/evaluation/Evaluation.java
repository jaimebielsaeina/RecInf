import java.util.Map;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

	public static double precision(boolean[] relevantDocuments, Integer K)
	{
		if (K <= 0 || K > relevantDocuments.length) K = relevantDocuments.length;
	
		int numRelevantDocuments = 0;
	
		for (int i = 0; i < K; i++) 
				if (relevantDocuments[i]) numRelevantDocuments++;
	
		return (double) numRelevantDocuments / Integer.min(K, relevantDocuments.length);
	}

	public static double precision(boolean[] relevantDocuments) 
	{
		return precision(relevantDocuments, relevantDocuments.length);
	}

	public static double avgPrecision(boolean[] relevantDocuments, Integer K) 
	{
		double avgPrecision = 0.0;
		int numRelevantDocuments = 0;
		if (K <= 0 || K > relevantDocuments.length) K = relevantDocuments.length;
	
		for (int i = 0; i < K;) 
		{
			if (relevantDocuments[i++]) 
			{
				numRelevantDocuments++;
				avgPrecision += (double) numRelevantDocuments / i;
			}
		}
	
		return (double) avgPrecision / numRelevantDocuments;
	}

	public static double avgPrecision(boolean[] relevantDocuments) 
	{
		return avgPrecision(relevantDocuments, relevantDocuments.length);
	}

	/*public static double meanAvgPrecision(boolean[][] relevantDocuments, int K) 
	{
		if (K <= 0 || K > relevantDocuments.length) {
			throw new IllegalArgumentException("K must be greater than 0.");
		}
	
		double mapSum = 0.0;
	
		for (int i = 0; i < relevantDocuments.length; i++) {
			mapSum += precision(relevantDocuments[i], K);
		}
	
		return mapSum / relevantDocuments.length;
	}*/

	/*public static double meanAvgPrecision(boolean[][] relevantDocuments) 
	{
		return MeanAvgPrecision(relevantDocuments, relevantDocuments.length);
	}*/

	public static double recall(boolean[] relevantDocuments, int K, Integer numTotalRelevantDocuments) 
	{
		if (K <= 0 || K > relevantDocuments.length) K =	relevantDocuments.length;
	
		int numRelevantDocuments = 0;
	
		if (numTotalRelevantDocuments == 0) return 0.0; // Avoid division by zero
	
		for (int i = 0; i < K; i++) 
				if (relevantDocuments[i])
						numRelevantDocuments++;
	
		return (double) numRelevantDocuments / numTotalRelevantDocuments;
	}

	public static double recall(boolean[] relevantDocuments, Integer numTotalRelevantDocuments) 
	{
		return recall(relevantDocuments, relevantDocuments.length, numTotalRelevantDocuments);
	}

	/*public static double MeanAvgRecall(boolean[][] relevantDocuments, int K, Integer numTotalRelevantDocuments) 
	{
		if (K <= 0) {
			throw new IllegalArgumentException("K must be greater than 0.");
		}
	
		double marSum = 0.0;
	
		for (int i = 0; i < relevantDocuments.length; i++) {
			marSum += recall(relevantDocuments[i], K, numTotalRelevantDocuments);
		}
	
		return marSum / relevantDocuments.length;
	}*/

	/*public static double MeanAvgRecall(boolean[][] relevantDocuments, Integer numTotalRelevantDocuments)
	{
		return MeanAvgRecall(relevantDocuments, relevantDocuments.length, numTotalRelevantDocuments);
	}*/

	public static double fBettaScore(double betta, double precision, double recall) 
	{
		if (precision + recall == 0) {
			// Handle the case where both precision and recall are zero to avoid division by zero.
			return 0.0;
		} else {
			betta *= betta;
			return (1 + betta) * (precision * recall) / (betta * precision + recall);
		}
	}

	/*public static double MeanF1Score(boolean[][] relevantDocuments, Integer numTotalRelevantDocuments) 
	{
		double f1Sum = 0.0;

		for (int i = 0; i < relevantDocuments.length; i++) {
			f1Sum += fBettaScore(1, relevantDocuments[i], numTotalRelevantDocuments);
		}

		return f1Sum / relevantDocuments.length;
	}*/

	public static ArrayList<PRPoint> computePrecisionRecallCurve(boolean[] relevantDocuments, Integer numTotalRelevantDocuments) 
	{
		ArrayList<PRPoint> precisionRecallCurve = new ArrayList<>();
		
		int numRetrievedDocuments = 0;

		for (int i = 0; i < relevantDocuments.length; i++) 
		{
			if (relevantDocuments[i]) {
				numRetrievedDocuments++;

				double precision = (double) numRetrievedDocuments / (i + 1);
				double recall = (double) numRetrievedDocuments / numTotalRelevantDocuments;

				precisionRecallCurve.add(new PRPoint(recall, precision));
			}
		}

		return precisionRecallCurve;
	}

	/*public static ArrayList<PRPoint> interpolatePrecisionRecallCurve(ArrayList<PRPoint> points, int numPoints) {
		ArrayList<PRPoint> interpolatedCurve = new ArrayList<>();

		for (double i=0; i)

		return interpolatedCurve;
	}*/

	/*public static ArrayList<PRPoint> computeAveragePrecisionRecallCurve(boolean[][] relevantDocuments, Integer numTotalRelevantDocuments) 
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
	}*/

	public static ArrayList<PRPoint> applyStepFunction(ArrayList<PRPoint> precisionRecallPoints, int nPoints) 
	{
		// Copy points
		ArrayList<PRPoint> stepFunctionCurve = new ArrayList<>(precisionRecallPoints);
		ArrayList<PRPoint> res = new ArrayList<>();

		double maxP = 0;

		// Reverse traverse the array
		for (int i = stepFunctionCurve.size()-1; i >= 0; i--) 
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

		double requestedRecall = 1;
		double lastPrecission = 0;
		double[] precs = new double[nPoints];
		int oldCurvePoint = stepFunctionCurve.size()-1;
		int newCurvePoint = nPoints;

		while (newCurvePoint > 0) {
			while (oldCurvePoint >= 0 && stepFunctionCurve.get(oldCurvePoint).recall >= requestedRecall - 1e-6) {
				lastPrecission = stepFunctionCurve.get(oldCurvePoint--).precision;
			}
			precs[--newCurvePoint] = lastPrecission;
			requestedRecall -= 1.0/(nPoints-1);
		}

		for (int i=0; i<nPoints; i++) {
			res.add(new PRPoint(1.0*i/(nPoints-1), precs[i]));
		}

		return res;
	}

	/*public static ArrayList<PRPoint> reducePrecisionRecallCurve(ArrayList<PRPoint> curve, int n) 
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
	}*/


	public static void main (String[] args) {

		// Reads command line arguments.
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
		// Reads qRels file.
		Map<String, Map<String, Boolean>> qRels = new HashMap<String, Map<String, Boolean>>();
		try {
			BufferedReader qRelsFile = new BufferedReader(new FileReader(qRelsFileName));
			String line;
			while ((line = qRelsFile.readLine()) != null) {
				String[] parts = line.trim().split("[ \t]+");
				if (!qRels.containsKey(parts[0]))
						qRels.put(parts[0], new LinkedHashMap<String, Boolean>());
				qRels.get(parts[0]).put(parts[1], Integer.parseInt(parts[2]) == 1);
			}
			qRelsFile.close();
		} catch (Exception e) {
			System.err.println("Error reading qRels file: " + e.getMessage());
			System.exit(1);
		}
		
		// Reads results file.
		Map<String, Map<String, Boolean>> results = new HashMap<String, Map<String, Boolean>>();
		try {
			BufferedReader resultsFile = new BufferedReader(new FileReader(resultsFileName));
			String line;
			while ((line = resultsFile.readLine()) != null) {
				String[] parts = line.trim().split("[ \t]+");
				if (!results.containsKey(parts[0]))
						results.put(parts[0], new LinkedHashMap<String, Boolean>());
				results.get(parts[0]).put(parts[1], true);
			}
			resultsFile.close();
		} catch (Exception e) {
			System.err.println("Error reading results file: " + e.getMessage());
			System.exit(1);
		}

		try {
			// Opens output file.
			PrintWriter outputFile = new PrintWriter(outputFileName);
			System.out.println("Writing metrics to " + outputFileName + " ...");

			// Defines metrics to be analyzed.
			double precision, meanPrecision = 0;
			double recall, meanRecall = 0;
			double f1, meanF1 = 0;
			double prec10, meanPrec10 = 0;
			double avgPrec, map = 0;
			ArrayList<PRPoint> curve, interpolatedCurve;
			double[] interpolatedCurvePoints = new double[11];

			// For each information need:
			for (String qRel : qRels.keySet()) {

				// Gets information about its results.
				boolean[] areRelevant = new boolean[results.get(qRel).size()];
				int i = 0, totalRelevantes = 0;
				for (String infoNeed : results.get(qRel).keySet())
						areRelevant[i++] = qRels.get(qRel).get(infoNeed);
				for (Boolean oneIsRel : qRels.get(qRel).values())
						if (oneIsRel) totalRelevantes++;

				// Computes metrics using the obtained info.
				precision = precision(areRelevant, 0);
				recall = recall(areRelevant, 0, totalRelevantes);
				f1 = fBettaScore(1, precision, recall);
				prec10 = precision(areRelevant, 10);
				avgPrec = avgPrecision(areRelevant, 0);
				curve = computePrecisionRecallCurve(areRelevant, totalRelevantes);
				interpolatedCurve = applyStepFunction(curve, 11);

				// Updates sum of metrics.
				meanPrecision += precision;
				meanRecall += recall;
				meanF1 += f1;
				meanPrec10 += prec10;
				map += avgPrec;
				for (int j = 0; j < interpolatedCurve.size(); j++)
						interpolatedCurvePoints[j] += interpolatedCurve.get(j).precision;

				// Prints metrics.
				outputFile.println("INFORMATION_NEED " + qRel);
				outputFile.printf("precision %.3f\n", precision);
				outputFile.printf("recall %.3f\n", recall);
				outputFile.printf("F1 %.3f\n", f1);
				outputFile.printf("prec@10 %.3f\n", prec10);
				outputFile.printf("average_precision %.3f\n", avgPrec);
				outputFile.println("recall_precision");
				for (PRPoint point : curve)
						outputFile.printf("%.3f\t%.3f\n", point.recall, point.precision);
				outputFile.println("interpolated_recall_precision");
				for (PRPoint point : interpolatedCurve)
						outputFile.printf("%.3f\t%.3f\n", point.recall, point.precision);
				outputFile.println();

			}

			// Prints mean metrics.
			int numInfoNeeds = qRels.size();
			outputFile.println("TOTAL");	
			outputFile.printf("precision %.3f\n", meanPrecision / numInfoNeeds);
			outputFile.printf("recall %.3f\n", meanRecall / numInfoNeeds);
			outputFile.printf("F1 %.3f\n", meanF1 / numInfoNeeds);
			outputFile.printf("prec@10 %.3f\n", meanPrec10 / numInfoNeeds);
			outputFile.printf("MAP %.3f\n", map / numInfoNeeds);
			outputFile.println("interpolated_recall_precision");
			for (int i = 0; i < interpolatedCurvePoints.length; i++)
					outputFile.printf("%.3f\t%.3f\n", 1.0*i/(interpolatedCurvePoints.length-1), interpolatedCurvePoints[i] / numInfoNeeds);

			// Closes output file.
			outputFile.close();

			System.out.println("Done!");

		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			System.exit(1);
		}

	}
}
