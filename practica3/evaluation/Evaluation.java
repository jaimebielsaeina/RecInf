import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;


public class Evaluation {

	// Precision-recall point.
	public static class PRPoint
	{
		// Recall and precision values.
		public double recall;
		public double precision;

		// Constructor.
		public PRPoint(double recall, double precision) {
			this.recall = recall;
			this.precision = precision;
		}
	}

	/**
	 * Computes the precision of the top K retrieved documents.
	 * @param retrievedDocuments Array of booleans order by preference indicating
	 *							 whether a document is relevant or not.
	 * @param K Number of retrieved documents to consider.
	 * @return Precision at K.
	 */
	public static double precision(boolean[] retrievedDocuments, Integer K)
	{
		// If K is not between 1 and the number of retrieved documents,
		// sets it to the number of retrieved documents.
		if (K <= 0 || K > retrievedDocuments.length) K = retrievedDocuments.length;
	
		// Counts the number of relevant documents in the top K retrieved documents.
		int truePositives = 0;
		for (int i = 0; i < K; i++) if (retrievedDocuments[i]) truePositives++;
	
		// Returns the precision using the first K documents.
		return (double) truePositives / Integer.min(K, retrievedDocuments.length);
	}

	/**
	 * Computes the precision of all the retrieved documents.
	 * @param retrievedDocuments Array of booleans order by preference indicating
	 *							 whether a document is relevant or not.
	 * @return Precision at K.
	 */
	public static double precision(boolean[] retrievedDocuments) 
	{
		return precision(retrievedDocuments, retrievedDocuments.length);
	}

	/**
	 * Computes the average precision of the top K retrieved documents.
	 * @param retrievedDocuments Array of booleans order by preference indicating
	 *							 whether a document is relevant or not.
	 * @param K Number of retrieved documents to consider.
	 * @return Average precision at K.
	 */
	public static double avgPrecision(boolean[] retrievedDocuments, Integer K) 
	{
		// If K is not between 1 and the number of retrieved documents,
		// sets it to the number of retrieved documents.
		if (K <= 0 || K > retrievedDocuments.length) K = retrievedDocuments.length;
	
		double sumPrecisions = 0.0;
		int truePositives = 0;
	
		// Gets the sum of precisions at each relevant document.
		for (int i = 0; i < K;) 
				if (retrievedDocuments[i++]) {
					truePositives++;
					sumPrecisions += (double) truePositives / i;
		}
	
		// Returns the average precision using the first K documents.
		return (double) sumPrecisions / truePositives;
	}

	/**
	 * Computes the average precision of all the retrieved documents.
	 * @param retrievedDocuments Array of booleans order by preference indicating
	 *							 whether a document is relevant or not.
	 * @return Average precision at K.
	 */
	public static double avgPrecision(boolean[] retrievedDocuments) 
	{
		return avgPrecision(retrievedDocuments, retrievedDocuments.length);
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

	/**
	 * Computes the recall of the top K retrieved documents.
	 * @param retrievedDocuments Array of booleans order by preference indicating
	 *							 whether a document is relevant or not.
	 * @param K Number of retrieved documents to consider.
	 * @param relevantDocuments Number of relevant documents.
	 * @return Recall at K.
	 */
	public static double recall(boolean[] retrievedDocuments, int K, Integer relevantDocuments) 
	{
		// If K is not between 1 and the number of retrieved documents,
		// sets it to the number of retrieved documents.
		if (K <= 0 || K > retrievedDocuments.length) K = retrievedDocuments.length;
	
		// Avoids division by zero.
		if (relevantDocuments == 0) return 0.0;
	
		// Counts the number of relevant documents in the top K retrieved documents.
		int truePositives = 0;
		for (int i = 0; i < K; i++) if (retrievedDocuments[i]) truePositives++;
	
		// Returns the recall using the first K documents.
		return (double) truePositives / relevantDocuments;
	}

	/**
	 * Computes the recall of all the retrieved documents.
	 * @param retrievedDocuments Array of booleans order by preference indicating
	 *							 whether a document is relevant or not.
	 * @param numTotalRelevantDocuments Number of relevant documents.
	 * @return Recall at K.
	 */
	public static double recall(boolean[] retrievedDocuments, Integer numTotalRelevantDocuments) 
	{
		return recall(retrievedDocuments, retrievedDocuments.length, numTotalRelevantDocuments);
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

	/**
	 * Computes the F-betta score using some precision and recall value.
	 * @param betta Beta value.
	 * @param precision Precision.
	 * @param recall Recall.
	 * @return F-betta score.
	 */
	public static double fBettaScore(double betta, double precision, double recall) 
	{
		// Both precision and recall cannot bt 0 at the same time.
		if (precision + recall == 0) return 0.0;

		// Returns the F-betta score.
		betta *= betta;
		return (1 + betta) * (precision * recall) / (betta * precision + recall);
	}

	/*public static double MeanF1Score(boolean[][] relevantDocuments, Integer numTotalRelevantDocuments) 
	{
		double f1Sum = 0.0;

		for (int i = 0; i < relevantDocuments.length; i++) {
			f1Sum += fBettaScore(1, relevantDocuments[i], numTotalRelevantDocuments);
		}

		return f1Sum / relevantDocuments.length;
	}*/

	/**
	 * Computes the precision-recall curve of the retrieved documents, getting a
	 * point for each relevant document.
	 * @param retrievedDocuments Array of booleans order by preference indicating
	 *							 whether a document is relevant or not.
	 * @param numTotalRelevantDocuments Number of relevant documents.
	 * @return ArrayList containing the points of the precision-recall curve.
	 */
	public static ArrayList<PRPoint> precisionRecallCurve(boolean[] retrievedDocuments, Integer numTotalRelevantDocuments) 
	{
		// Structure to store the points of the precision-recall curve.
		ArrayList<PRPoint> pcCurve = new ArrayList<>();
		
		int numRetrievedDocuments = 0;

		// Gets a point for each relevant document:
		for (int i = 0; i < retrievedDocuments.length; i++) 
				if (retrievedDocuments[i]) {
					numRetrievedDocuments++;
					// Computes the precision and recall values.
					double precision = (double) numRetrievedDocuments / (i + 1);
					double recall = (double) numRetrievedDocuments / numTotalRelevantDocuments;
					// Adds the point to the curve.
					pcCurve.add(new PRPoint(recall, precision));
		}

		return pcCurve;
	}

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

	/**
	 * Interpolates a step function curve.
	 * @param precisionRecallPoints Points of the step function curve.
	 * @param nPoints Number of points of the interpolated curve.
	 * @return ArrayList containing nPoints points forming the interpolated curve.
	 */
	public static ArrayList<PRPoint> interpolateStepFunction(ArrayList<PRPoint> precisionRecallPoints, int nPoints) 
	{
		// Structure to store the points of the interpolated curve.
		double[] precs = new double[nPoints];

		// Requested recall for the next point, and maximum precision found so far (reverse iteration).
		double requestedRecall = 1;
		double lastPrecission = 0;

		// Indexes to iterate over the points of the original and interpolated curve.
		int oldCurvePoint = precisionRecallPoints.size()-1;
		int newCurvePoint = nPoints;

		// While there are points to interpolate:
		while (newCurvePoint > 0) {
			// Gets the maximum precision for the requested recall value.
			// (Using 1e-6 as a tolerance to avoid double errors).
			while (oldCurvePoint >= 0 && precisionRecallPoints.get(oldCurvePoint).recall >= requestedRecall - 1e-6) {
				// If the precision is greater than the maximum found so far, updates it.
				lastPrecission = Double.max(lastPrecission, precisionRecallPoints.get(oldCurvePoint--).precision);
			}
			// Adds the point to the interpolated curve.
			precs[--newCurvePoint] = lastPrecission;
			// Updates the requested recall value.
			requestedRecall -= 1.0/(nPoints-1);
		}

		// Converts the array of precisions to an ArrayList of PRPoints, and returns it.
		ArrayList<PRPoint> res = new ArrayList<>();
		for (int i=0; i<nPoints; i++) 
				res.add(new PRPoint(1.0*i/(nPoints-1), precs[i]));
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
				curve = precisionRecallCurve(areRelevant, totalRelevantes);
				interpolatedCurve = interpolateStepFunction(curve, 11);

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
