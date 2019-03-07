package test;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

public class testregress4 {
	public static void main(String args[]) {

		double[] y = { 1.603006329, 1.368339018, 0.998852008, 0.775988486, 0.887876844, 0.861582171, 0.851213249,
				1.040126997, 0.815564707, 0.598436757, -0.000341525, -0.349320175, -0.555323188, -0.885448975,
				-0.959210606, -1.179771698, -1.184092404, -0.991116744, -0.888787938, -0.690169421, -0.518917806,
				-0.439448262 };
		double[][] x = { { 0.081, 0.076 }, { 0.076, 0.075 }, { 0.075, 0.081 }, { 0.081, 0.079 }, { 0.079, 0.076 },
				{ 0.076, 0.079 }, { 0.079, 0.077 }, { 0.077, 0.074 }, { 0.074, 0.075 }, { 0.075, 0.071 },
				{ 0.071, 0.072 }, { 0.072, 0.07 }, { 0.07, 0.07 }, { 0.07, 0.069 }, { 0.069, 0.068 }, { 0.068, 0.067 },
				{ 0.067, 0.067 }, { 0.067, 0.067 }, { 0.067, 0.068 }, { 0.068, 0.102 }, { 0.102, 0.1 },
				{ 0.1, 0.094 } };
		OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
		regression.newSampleData(y, x);
		System.out.println(regression.calculateTotalSumOfSquares());
		 double[][] rm=regression.estimateRegressionParametersVariance();
//		double[][] rm = regression.calculateHat().getData();
		// System.out.println("rm.length="+rm.length);
		for (int i = 0; i < rm.length; i++) {
			double[] rmi = rm[i];
			// System.out.println("rm["+i+"]="+rm[i]);
			String sb = new String();
			sb = "{";
			for (int j = 0; j < rmi.length; j++) {
				sb = sb + "," + rmi[j];
			}
			sb = sb + "}";
			System.out.println("rm[" + i + "]=" + sb);
		}
		

//		RealMatrix matrix = new Array2DRowRealMatrix(x);
//		matrix = matrix.multiply(matrix.transpose());
//		matrix = MatrixUtils.inverse(matrix);
//		double[][] matrixdata = matrix.getData();
////		System.out.println("getMartrixResult=" + getMartrixResult(matrixdata));
//		for (int i = 0; i < matrixdata.length; i++) {
//			double[] matrixdatai = matrixdata[i];
//			String sb = new String();
//			sb = "{";
//			for (int j = 0; j < matrixdatai.length; j++) {
//				sb = sb + "," + matrixdatai[j];
//			}
//			sb = sb + "}";
//			System.out.println("matrixdata[" + i + "]=" + sb);
//		}
		double cii = rm[rm.length - 1][rm.length - 1];
		double[] beta = regression.estimateRegressionParameters();
		double t = beta[2] / Math.sqrt(cii * regression.calculateResidualSumOfSquares() / (x.length - 3));
		System.out.println("cii="+cii+" t=" + t+" regression.calculateResidualSumOfSquares()="+regression.calculateResidualSumOfSquares());
	}

	
}
