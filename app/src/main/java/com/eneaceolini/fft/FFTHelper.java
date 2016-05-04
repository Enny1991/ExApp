package com.eneaceolini.fft;

/**
 * Created by Enea on 28/08/15.
 * Project COCOHA
 */
public class FFTHelper {

        private static final String TAG = "FFTHelper";
        long startTime,stopTime;


        int n, m;

        // Lookup tables. Only need to recompute when size of FFT changes.
        double[] cos;
        double[] sin;

        public FFTHelper(int n) {
            this.n = n;
            this.m = (int) (Math.log(n) / Math.log(2));

            // Make sure n is a power of 2
            if (n != (1 << m))
                throw new RuntimeException("FFT length must be power of 2");

            // precompute tables
            cos = new double[n / 2];
            sin = new double[n / 2];

            for (int i = 0; i < n / 2; i++) {
                cos[i] = Math.cos(-2 * Math.PI * i / n);
                sin[i] = Math.sin(-2 * Math.PI * i / n);
            }

        }

    public double[] corr_fftw(double[] in1, double[] in2) {
        //executeJNI does a FFT of a real signal thus return only the non redundant complex transform
        //size(in) = n --> out[i] = conj(out[n-i]);
        double[] out = FFTW.execute_corr(in1, in2);
        return out;
    }

    public double[] test(double[] in1){
        //executeJNI does a FFT of a real signal thus return only the non redundant complex transform
        //size(in) = n --> out[i] = conj(out[n-i]);
        double[] out = FFTW.test(in1);
        return out;
    }

    public double[] beam_fftw(double[] in1, double[] in2, double theta){
        //executeJNI does a FFT of a real signal thus return only the non redundant complex transform
        //size(in) = n --> out[i] = conj(out[n-i]);
        double[] out = FFTW.execute_beam(in1,in2,theta);
        return out;
    }

        public double[][] fftw(double[] in){

            double[][] ret = new double[2][in.length];
            //executeJNI does a FFT of a real signal thus return only the non redundant complex transform
            //size(in) = n --> out[i] = conj(out[n-i]);
            double[] out = FFTW.execute(in);
            System.out.println(""+out.length);
            int n = out.length;
            for(int i = 0,j=0;i<n/2-1;i++,j+=2){
                ret[0][i] = out[j];
                ret[1][i] = out[j+1];
                ret[0][n-3-i] = out[j];
                ret[1][n-3-i] = - out[j+1]; //conjugated
            }
            return ret;
        }

        public void fft(double[] x, double[] y) {

            int i, j, k, n1, n2, a;
            double c, s, t1, t2;

            // Bit-reverse
            j = 0;
            n2 = n / 2;
            for (i = 1; i < n - 1; i++) {
                n1 = n2;
                while (j >= n1) {
                    j = j - n1;
                    n1 = n1 / 2;
                }
                j = j + n1;

                if (i < j) {
                    t1 = x[i];
                    x[i] = x[j];
                    x[j] = t1;
                    t1 = y[i];
                    y[i] = y[j];
                    y[j] = t1;
                }
            }

            // FFT
            //n1 = 0;
            n2 = 1;

            for (i = 0; i < m; i++) {
                n1 = n2;
                n2 = n2 + n2;
                a = 0;

                for (j = 0; j < n1; j++) {
                    c = cos[a];
                    s = sin[a];
                    a += 1 << (m - i - 1);

                    for (k = j; k < n; k = k + n2) {
                        t1 = c * x[k + n1] - s * y[k + n1];
                        t2 = s * x[k + n1] + c * y[k + n1];
                        x[k + n1] = x[k] - t1;
                        y[k + n1] = y[k] - t2;
                        x[k] = x[k] + t1;
                        y[k] = y[k] + t2;
                    }
                }
            }

        }

    public void ifft(double[] x,double[] y) {
        int N = x.length;
        // take conjugate
        for (int i = 0; i < N; i++) {
            y[i] = -y[i];
        }

        // compute forward FFT
        fft(x, y);

        // take conjugate again
        for (int i = 0; i < N; i++) {
            y[i] = -y[i];
        }

        // divide by N
        for (int i = 0; i < N; i++) {
            y[i] = y[i] * (1.0 / N);
            x[i] = x[i] * (1.0 / N);
        }
    }


    class SignalNotDefinedException extends Exception{

        public SignalNotDefinedException(String message){
            super(message);
        }

        public SignalNotDefinedException(Throwable throwable, String message){
            super(message,throwable);
        }

        public SignalNotDefinedException(Throwable throwable){
            super(throwable);
        }
    }





}
