package probabilitymatrix;


import java.util.ArrayList;
import java.util.List;

public class TestMain {

    public static void main(String[] args) {
        //Matrix Initial_data = new Matrix(27,20);//建行一年期原始数据
        float [][] initialdata = new float[20][20];
        initialdata[0] = new float[]{9,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        initialdata[1] = new float[]{1,94,4,0,2,0,0,0,1,0,0,0,0,0,0,0,0,0,4,0};
        initialdata[2] = new float[]{0,13,147,37,22,12,5,9,5,5,1,0,0,0,1,0,0,0,33,0};
        initialdata[3] = new float[]{0,3,34,235,79,45,52,62,11,19,4,1,1,1,0,0,0,1,152,3};
        initialdata[4] = new float[]{0,0,10,53,1404,203,84,117,48,60,11,5,4,2,5,0,1,2,381,10};
        initialdata[5] = new float[]{0,0,2,7,172,7024,2426,1162,707,389,170,94,60,44,131,30,11,24,3584,338};
        initialdata[6] = new float[]{0,0,1,1,48,1292,8203,4484,2278,1055,469,291,157,78,275,54,27,45,7069,785};
        initialdata[7] = new float[]{0,0,0,0,12,406,2013,8779,3726,1631,657,442,242,147,348,83,32,57,7621,907};
        initialdata[8] = new float[]{0,0,0,0,0,108,527,2030,6512,2393,1090,639,321,211,411,109,63,90,7134,943};
        initialdata[9] = new float[]{0,0,0,0,0,56,190,596,1409,2814,1096,712,322,195,240,62,41,69,4635,1055};
        initialdata[10] = new float[]{0,0,0,0,0,18,69,183,459,660,931,612,266,147,146,43,18,48,2556,396};
        initialdata[11] = new float[]{0,0,0,0,0,12,39,95,238,357,438,748,303,170,159,39,23,52,1837,342};
        initialdata[12] = new float[]{0,0,0,0,0,4,15,30,77,119,132,191,235,146,88,28,19,27,1056,181};
        initialdata[13] = new float[]{0,0,0,0,0,2,6,25,48,60,74,102,118,236,103,34,20,40,782,154};
        initialdata[14] = new float[]{0,0,0,0,0,2,0,3,3,11,10,10,7,16,35,17,8,19,293,280};
        initialdata[15] = new float[]{0,0,0,0,0,0,1,0,1,3,2,2,5,5,6,15,8,7,159,45};
        initialdata[16] = new float[]{0,0,0,0,0,0,0,1,4,1,0,3,0,3,5,5,9,12,82,15};
        initialdata[17] = new float[]{0,0,0,0,0,0,1,1,1,1,3,1,3,3,2,5,6,45,270,132};
        initialdata[18] = new float[]{0,0,0,0,1,141,457,941,1466,1760,1577,1331,760,588,614,166,63,150,29593,1222};
        initialdata[19] = new float[]{0,0,0,0,0,1,1,5,4,0,5,4,2,1,7,2,2,8,5811,10034};
        float [][] transferdata = new float[initialdata.length][initialdata[0].length];
        float [] last_col;
        Result result = TransferProbabilityMatrix.get(initialdata);
//        for (int i=0;i<31;i++) {
//            if (i == 0) {
//                System.out.print(" 年份 \t");
//            } else {
//                System.out.print("第" + i + "年\t");
//            }
//        }
        System.out.println("CPD的值");
        int h=1;
        for (float[] item : result.getCpd()){
            if (h==19){
                System.out.print("无评级有信贷\t");
            }else {
                System.out.print("第" + h + "级\t");
            }
            for (Float i : item){
                System.out.print(i+"%\t\t");
            }
            System.out.println();
            h=h+1;
        }

        System.out.println("边际PD的值");
        int h1=1;
        for (float[] item : result.getPd()){
            if (h1==19){
                System.out.print("无评级有信贷\t");
            }else {
                System.out.print("第" + h1 + "级\t");
            }
            for (Float i : item){
                System.out.print(i+"%\t\t");
            }
            System.out.println();
            h1=h1+1;
        }


    }
}
