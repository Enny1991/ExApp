package com.eneaceolini.exapp;

/**
 * Created by Enea on 21/05/15.
 */
public class BSplines {

    public double bspline(int degree, double x)
    {
        double betta;
        double t;
        betta = 0;
        if (degree == 0)
        {
            if ((x > -0.5) && (x < 0.5))
            {
                betta = 1.0;
            }
            else if (Math.abs(x) == 0.5)
            {
                betta = 0.5;
            }
            else if (Math.abs(x) > 0.5)
            {
                betta = 0.0;
            }
        }
        else if (degree == 1)
        {
            if ((x <= -1) || (x >= 1))
            {
                betta = 0.0;
            }
            else if ((x > -1) && (x < 0))
            {
                betta = x + 1;
            }
            else if ((x > 0) && (x < 1))
            {
                betta = -x + 1;
            }
            else if (x == 0)
            {
                betta = 1.0;
            }
        }
        else if (degree == 2)
        {
            t = 1.5;
            if ((x <= (0 - t)) || (x >= (3 - t)))
            {
                betta = 0.0;
            }
            else if ((x >= (0 - t)) && (x < (1 - t)))
            {
                betta = ((x + t) * (x + t)) / 2.0;
            }
            else if ((x >= (1 - t)) && (x < (2 - t)))
            {
                betta = ((x + t) * (x + t) - 3.0 * (x - 1 + t) * (x - 1 + t)) / 2.0;
            }
            else if ((x >= (2 - t)) && (x < (3 - t)))
            {
                betta = ((x + t) * (x + t) - 3.0 * (x - 1 + t) * (x - 1 + t) +
                        3.0 * (x - 2 + t) * (x - 2 + t)) / 2.0;
            }
        }
        else if (degree == 3)
        {
            if ((Math.abs(x) >= 0) && (Math.abs(x) < 1))
            {
                betta = 2.0 / 3.0 - Math.abs(x) * Math.abs(x) +
                        (Math.abs(x) * Math.abs(x) * Math.abs(x)) / 2.0;
            }
            else if ((Math.abs(x) >= 1) && (Math.abs(x) < 2))
            {
                betta = ((2 - Math.abs(x)) * (2 - Math.abs(x)) *
                        (2 - Math.abs(x))) / 6.0;
            }
            else if (Math.abs(x) >= 2)
            {
                betta = 0.0;
            }
        }
        return betta;
    }
}
