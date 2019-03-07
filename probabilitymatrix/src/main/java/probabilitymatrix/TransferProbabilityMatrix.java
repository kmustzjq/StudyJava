package probabilitymatrix;

import java.text.NumberFormat;

public class TransferProbabilityMatrix {

    public static float[][] transfer(float[][] a){
        //转化后的数据用来表示
        float [][] transferdata = new float[a.length][a[0].length];
        for (int i=0;i<a.length-1;i++){
            float sum = 0;
            for (int j=0;j<a[0].length;j++) {
                sum = sum + a[i][j];
            }
            for (int k=0;k<a[0].length;k++) {
                transferdata[i][k]=a[i][k]/sum;
            }
        }
        transferdata[a.length-1]=new float[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1};
        return transferdata;
    }

    //选取最后一列的值
    public static float[] select(float[][] a){
        float [] last_col = new float[a.length];
        for (int i=0;i<a.length-1;i++){
                    last_col[i] = a[i][a[0].length-1];
             }
        last_col[a.length-1]=1;
        return last_col;
    }
    //数组和向量相乘
    public static float[] multiply(float[][] s1,float[] s2){
        float [] s3 = new float[s1.length];
        if (s1[0].length != s2.length){
            System.out.println("矩阵不满足相乘的条件");
        }else {
            for (int i = 0; i < s1.length; i++) {
                float sum=0;
                for (int j = 0; j < s1[0].length; j++) {
                    sum = sum + s1[i][j] * s2[j];
                    s3[i]=sum;
                }
            }
        }
        return s3;
    }

    //计算CPD
    public static float[] calculatecpd(float[][] a,float[] b,int n){
        int i=1;
        while (i<n) {
            b = TransferProbabilityMatrix.multiply(a, b);
            i+=1;
        }
        return b;
    }

    public static float[] calculate_marginalpd(float[][] a,float[]b,int y){
        float[] c = new float[b.length];
        float[] c1;
        float[] c2;
        if (y == 1){
            c = TransferProbabilityMatrix.calculatecpd(a,b,y);
        }else {
            c1 = TransferProbabilityMatrix.calculatecpd(a,b,y);
            c2 = TransferProbabilityMatrix.calculatecpd(a,b,y-1);
            for (int i=0;i<c1.length;i++){
                c[i]=c1[i]-c2[i];
            }
        }
        return c;
    }

    /**传入一个二维数组和一个年份，如果没传入年份，默认为30
     *
     * @param f
     * @return 一个二维数组边际PD和CPD(20行30列)
     */
    public static Result get(float[][] f){
        return get(f,30);
    }
    public static Result get(float[][] f,int year){
        Result result = new Result();
        NumberFormat nfcpd= NumberFormat.getNumberInstance();
        nfcpd.setMaximumFractionDigits(2);
        NumberFormat nfpd= NumberFormat.getNumberInstance();
        nfpd.setMaximumFractionDigits(4);
        float [][] transferdata = new float[f.length][f[0].length];
        float [] last_col;
        //转化后的数据用来表示
        transferdata=TransferProbabilityMatrix.transfer(f);
        //选取最后一列
        last_col=TransferProbabilityMatrix.select(transferdata);
        float [] cpd;float [] marginalpd;int n=1;
        float[][] cpdl = new float[f.length][year];
        float[][] pdl = new float[f.length][year];
        while (n <= year) {// 一年一循环
            cpd = TransferProbabilityMatrix.calculatecpd(transferdata, last_col, n);
            marginalpd = TransferProbabilityMatrix.calculate_marginalpd(transferdata, last_col, n);
            for (int i=0;i<cpd.length;i++){
                cpdl[i][n-1]=Float.valueOf(nfcpd.format((double)cpd[i]*100));
                pdl[i][n-1]=Float.valueOf(nfpd.format((double)(marginalpd[i]*100)));
                //cpdl[i][n-1]=cpd[i]*100;
                //pdl[i][n-1]=marginalpd[i]*100;
            }
            n+=1;
        }
        result.setCpd(cpdl);
        result.setPd(pdl);
        return result;
    }
}


