package com.eneaceolini.exapp;

/**
 * Created by Enea on 21/05/15.
 */
public class CubicInterpolation1d {

    private double[] mirrorW1d(double s[]) {
        double[] s_mirror = new double[s.length + 3];
        s_mirror[0] = s[1];
        for (int k = 0; k < s.length; k++) {
            s_mirror[k + 1] = s[k];
        }
        s_mirror[s_mirror.length - 2] = s[s.length - 2];
        return s_mirror;
    }

    public double[] coeffs(double s[]) {
        DirectBsplFilter1d directFilter =
                new DirectBsplFilter1d(s.length);
        double coeffs[] = directFilter.filter(s);
        return mirrorW1d(coeffs);
    }

    public double interp(double coeffs_mirror[], double x1) {
        BSplines bS = new BSplines();
        int k = (int) Math.floor(x1);
        return coeffs_mirror[k + 0] * bS.bspline(3, x1 - k + 1) +
                coeffs_mirror[k + 1] * bS.bspline(3, x1 - k + 0) +
                coeffs_mirror[k + 2] * bS.bspline(3, x1 - k - 1) +
                coeffs_mirror[k + 3] * bS.bspline(3, x1 - k - 2);
    }

    public double[] interpolate(double s[], int rate) {
        double coeffs_mirror[] = coeffs(s);
        double s_interp[] = new double[rate * s.length - (rate - 1)];
        for (int k = 0; k < s_interp.length; k++) {
            s_interp[k] = interp(coeffs_mirror, k * (1.0 / rate));
        }
        return s_interp;
    }
}
