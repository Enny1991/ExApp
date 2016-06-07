//
// Created by Enea Ceolini on 08/02/16.
//
#include <iostream>
#include <Eigen/Dense>
#include <math.h>
#include <jni.h>
#include <android/log.h>

#define LOG_TAG "EIGEN"
#define CC 343
#define SWAP(a,b) do { double tmp = b ; b = a ; a = tmp ; } while(0)

#define LOGD(...) \
    __android_log_print(ANDROID_LOG_DEBUG, "EIGEN_DEBUG", __VA_ARGS__)

using namespace Eigen;
using namespace std;


Vector3f vec;
Vector3f vec2;
Vector3f vecRtrn;




void vecAdd(Vector3f vecA, Vector3f vecB){
    vecRtrn = vecA + vecB;
}

extern "C"
{
JNIEXPORT jfloatArray JNICALL Java_com_eneaceolini_exapp_JNImathActivity_test
        (JNIEnv *env, jobject obj, jfloatArray fltarray1, jfloatArray fltarray2)
{
return 0;
}

JNIEXPORT jfloatArray JNICALL Java_com_eneaceolini_EigenHelper_linearRegression
        (JNIEnv *pEnv, jobject obj, jfloatArray xin, jfloatArray yin)
{
    //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "beginning");
    int vecLen,i,j;

    jfloatArray resJNI;
    jfloat      *resArray;
    float       *real1;
    float       *real2;

    resJNI   = pEnv->NewFloatArray(2);
    if (resJNI == NULL) {
        return NULL; /* out of memoEigen ry error thrown */
    }
    //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "declaring");

    //jfloat A[2];
    jfloat *flt1 = pEnv->GetFloatArrayElements( xin, 0);
    jfloat *flt2 = pEnv->GetFloatArrayElements( yin, 0);

    real1 = (float*) flt1;
    real2 = (float*) flt2;

    vecLen = pEnv->GetArrayLength( yin);
    resJNI   = pEnv->NewFloatArray(2);
    resArray = pEnv->GetFloatArrayElements(resJNI,0);

    MatrixXf x(vecLen, 2);
    VectorXf y(vecLen);
    // loading
    //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "loading %d", vecLen);

    //Matrix3f A;
    //Vector3f b;
    //Vector3f x = A.colPivHouseholderQr().solve(b);
    for(i = 0; i < vecLen; i++){
        y(i) = real2[i];
        //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "load y %.4f",real1[i]);
        x(i,1) = real1[i];
        //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "load x 0,%.4f",real2[i]);
    }
//
    //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "loading 2");
    for(i = vecLen,j=0; i < vecLen * 2; i++,j++){
        x(j,0) = 1.f;
    }
//
//    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "before lr");
//
//    // regression
//    Vector2f Aret = (x.transpose() * x).inverse() * x.transpose() * y;
    Vector2f Aret = (x.transpose() * x).inverse() * x.transpose() * y;
//    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "after lr");

    for (i = 0; i < 2; i++) {
        resArray[i] = Aret(i);
        //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "aret %.4f", Aret(i));
    }


    pEnv->ReleaseFloatArrayElements( xin, flt1, 0);
    pEnv->ReleaseFloatArrayElements( yin, flt2, 0);
    pEnv->ReleaseFloatArrayElements(resJNI, resArray, 0);
    //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "cojkjkadvjkb");
    return resJNI;

}

static Matrix3f adjoint_sym3(Matrix3f M){
    Matrix3f A;
   float a,b,c,d,e,f;

    a = M(0,0); b = M(0,1); d = M(0,2);
                c = M(1,1); e = M(1,2);
                            f = M(2,2);

    A(0,0) = c*f - e*e;
    A(0,1) = -b*f + e*d;
    A(0,2) = b*e - c*d;

    A(1,0) = A(0,1);
    A(1,1) = a*f - d*d;
    A(1,2) = -a*e + b*d;

    A(2,0) = A(0,2);
    A(2,1) = A(1,2);
    A(2,2) = a*c - b*b;
    return A;

}

static MatrixXf cross_matrix(MatrixXf p){
    Matrix3f MP;
    MP << 0, p(2,0), -p(1,0),
        -p(2,0), 0, p(0,0),
        p(1,0), -p(0,0), 0;

    return MP;
}

static int decompose_degenerate_conic(Matrix3f c, Vector3f *m, Vector3f *l)
{
    float max;
    MatrixXf::Index maxRow, maxCol;
    Matrix3f C;
    FullPivLU<Matrix3f> lu_decomp(c);
    int rank = lu_decomp.rank();
    if (rank == 1)
    {
    C = c;
    }
    else
    {

        Matrix3f B = -adjoint_sym3(c);
        LOGD("First entry in c = %.4f", c(0,0));
        LOGD("First entry in ADJOINT = %.4f", B(0,0));
        max = B.diagonal().array().abs().maxCoeff(&maxRow);

        int i = maxRow;
        LOGD("max position %d", i);
        if(B(i,i) < 0){
            return 0;
        }
        double b = sqrt(B(i,i));
        LOGD("b = %.4f", b);
        MatrixXf p = B.block(0,i,3,1) / b;
        LOGD("Vector p: %.4f, %.4f, %.4f", p(0,0),p(1,0),p(2,0));
        Matrix3f Mp = cross_matrix(p);
        LOGD("MP => %.4f, %.4f, %.4f, %.4f, %.4f, %.4f, %.4f, %.4f, %.4f", Mp(0,0), Mp(0,1), Mp(0,2), Mp(1,0), Mp(1,1), Mp(1,2), Mp(2,0), Mp(2,1), Mp(2,2) );

        C = c + Mp;


    }

    max = C.cwiseAbs().maxCoeff(&maxRow, &maxCol);
    int jj = maxCol;
    int ii = maxRow;

    LOGD("Second round max = %.4f | maxRow = %d | maxCol = %d", max, ii,jj);
    LOGD("C => %.4f, %.4f, %.4f, %.4f, %.4f, %.4f, %.4f, %.4f, %.4f", C(0,0), C(0,1), C(0,2), C(1,0), C(1,1), C(1,2), C(2,0), C(2,1), C(2,2) );

    *m << C(0,jj), C(1,jj), C(2,jj);
    *l << C(ii,0), C(ii,1), C(ii,2);



    return 1;

}

static int solve(double a, double b, double c, double *x0, double *x1, double *x2){

      double q = (a * a - 3 * b);
      double r = (2 * a * a * a - 9 * a * b + 27 * c);

      double Q = q / 9;
      double R = r / 54;

      double Q3 = Q * Q * Q;
      double R2 = R * R;

      double CR2 = 729 * r * r;
      double CQ3 = 2916 * q * q * q;

      if (R == 0 && Q == 0)
        {
          *x0 = - a / 3 ;
          *x1 = - a / 3 ;
          *x2 = - a / 3 ;
         return 3 ;
      }
      else if (CR2 == CQ3)
        {


          double sqrtQ = sqrt (Q);

          if (R > 0)
            {
              *x0 = -2 * sqrtQ  - a / 3;
              *x1 = sqrtQ - a / 3;
              *x2 = sqrtQ - a / 3;
            }
          else
           {
              *x0 = - sqrtQ  - a / 3;
              *x1 = - sqrtQ - a / 3;
              *x2 = 2 * sqrtQ - a / 3;
            }
          return 3 ;
       }
      else if (R2 < Q3)
        {
          double sgnR = (R >= 0 ? 1 : -1);
          double ratio = sgnR * sqrt (R2 / Q3);
          double theta = acos (ratio);
          double norm = -2 * sqrt (Q);
         *x0 = norm * cos (theta / 3) - a / 3;
          *x1 = norm * cos ((theta + 2.0 * M_PI) / 3) - a / 3;
          *x2 = norm * cos ((theta - 2.0 * M_PI) / 3) - a / 3;


          if (*x0 > *x1)
            SWAP(*x0, *x1) ;

          if (*x1 > *x2)
           {
              SWAP(*x1, *x2) ;

            if (*x0 > *x1)
                SWAP(*x0, *x1) ;
          }

         return 3;
       }
     else
       {
         double sgnR = (R >= 0 ? 1 : -1);
         double A = -sgnR * pow (fabs (R) + sqrt (R2 - Q3), 1.0/3.0);
         double B = Q / A ;
         *x0 = A + B - a / 3;
         return 1;
       }
  }

static void point_in_line(Vector3f l, Vector3f *p1, Vector3f *p2){
    if(l(0) == 0 && l(1) == 0)
    {
        *p1 << 1, 0, 0;
        *p2 << 0, 1, 0;
    }
    else
    {
        *p2 << -l(1), l(0), 0;
        if (abs(l(0)) < abs(l(1)))
        {
            *p1 << 0, -l(2), l(1);
        }
        else
        {
             *p1 << -l(2), 0, l(0);
        }
    }
}

static MatrixXf intersect_conic_line(Matrix3f C, Vector3f l){

    Vector3f p1,p2;
    point_in_line(l, &p1, &p2);
    LOGD("p1 = %.4f, %.4f, %.4f", p1(0), p1(1), p1(2));
    LOGD("p2 = %.4f, %.4f, %.4f", p2(0), p2(1), p2(2));
    float k1, delta;
    MatrixXf P;
    float p1Cp1 = p1.transpose() * C * p1;
    float p2Cp2 = p2.transpose() * C * p2;
    float p1Cp2 = p1.transpose() * C * p2;

    LOGD("p1Cp1 %.4f", p1Cp1);
    LOGD("p2Cp2 %.4f", p2Cp2);
    LOGD("p1Cp2 %.4f", p1Cp2);

        if (p2Cp2 == 0) {
           k1 = -0.5 * p1Cp1 / p1Cp2;
           MatrixXf P(3,1);
           P << p1 + k1 * p2;
           return P;
        }else{
            delta = pow(p1Cp2, 2) - p1Cp1 * p2Cp2;
            if (delta >= 0){
                float deltaSqrt = sqrt(delta);
                float k1 = (-p1Cp2 + deltaSqrt)/p2Cp2;
                float k2 = (-p1Cp2 - deltaSqrt)/p2Cp2;
                Vector3f p3 = p1 + k1 * p2;
                Vector3f p4 = p1 + k2 * p2;
                LOGD("Looking at P3: %.4f %.4f %.4f", p3(0), p3(1), p3(2));
                LOGD("Looking at P4: %.4f %.4f %.4f", p4(0), p4(1), p4(2));
                MatrixXf P(3,2);
                P << p3, p4;
                return P;
            }
        }
    return P;

}


static MatrixXf complete_intersection(Matrix3f E1, Matrix3f E2){
    double r1, r2, r3;
    Matrix3f _tmp_E2 = -E2;
    Matrix3f EE = E1 * _tmp_E2.inverse();
    Matrix2f sub1E1 = EE.block(0,0,2,2);
    Matrix2f sub2E1 = EE.block(1,1,2,2);
    Matrix2f sub3E1;
    MatrixXf C;
    sub3E1 << EE(0,0), EE(0,2), EE(2,0), EE(2,2);
    int result_solve = solve(-EE.trace(), sub1E1.determinant() + sub2E1.determinant() + sub3E1.determinant() , -EE.determinant(), &r1, &r2, &r3);
    LOGD("First entry E1 = %.4f",E1(0,0));
    LOGD("First entry E2 = %.4f",E2(0,0));
    LOGD("EE.trace = %.4f", EE.trace());
    LOGD("Det SUB1 = %.4f", sub1E1.determinant());
    LOGD("ROOTS = %.4f, %.4f, %.4f", r1, r2, r3);

    Vector3f m,l;
    Matrix3f E0;
    int res;
    if(result_solve == 1){
        E0 << E1 + r1 * E2;
        res = decompose_degenerate_conic(E0, &l, &m);
    }
    else{
        E0 << E1 + r1 * E2;
        LOGD("First entry E0 with r1 %.4f", E0(0,0));
        res = decompose_degenerate_conic(E0, &l, &m);
        LOGD("result of dec %d", res);
        if(res == 0){
            E0 << E1 + r2 * E2;
            LOGD("First entry E0 with r2 %.4f", E0(0,0));
            res = decompose_degenerate_conic(E0, &l, &m);
            LOGD("result of dec %d", res);
        }
        if(res == 0){
            E0 << E1 + r3 * E2;
            LOGD("First entry E0 with r3 %.4f", E0(0,0));
            res = decompose_degenerate_conic(E0, &l, &m);
            LOGD("result of dec %d", res);
        }
    }

    if (res == 1){
    LOGD("values in l = %.4f, %.4f, %.4f", l(0), l(1), l(2));
    LOGD("values in m = %.4f, %.4f, %.4f", m(0), m(1), m(2));
    MatrixXf P1, P2;
    P1 = intersect_conic_line(E1, m);
    LOGD("BEFORE");
    P2 = intersect_conic_line(E1, l);
    LOGD("AFTER");
    MatrixXf C(3, P1.cols() + P2.cols());
    C << P1, P2;
    return C;
    }

    return C;

}

static MatrixXf find_intersections(Matrix3f E1, Matrix3f E2){
    LOGD("Calculating intersections");
    FullPivLU<Matrix3f> lu_decomp_1(E1);
    FullPivLU<Matrix3f> lu_decomp_2(E2);
    Matrix3f defE, fullE;
    int r1 = lu_decomp_1.rank();
    int r2 = lu_decomp_2.rank();

    if (r1 == 3 && r2 == 3){
        return complete_intersection(E1,E2);
    }
    else
    {
    if(r2 < 3){
        defE = E2;
        fullE = E1;
    }else{
        defE = E1;
        fullE = E2;
    }
    Vector3f l, m;
    decompose_degenerate_conic(defE, &m, &l);
    MatrixXf P1 = intersect_conic_line(fullE, m);
    MatrixXf P2 = intersect_conic_line(fullE, l);
    MatrixXf C(3, P1.cols() + P2.cols());

    C << P1, P2;

    LOGD("C is %d x %d", C.rows(), C.cols());
    return C;
    }


}

static Matrix3f create_hyperbola(float tdoa, float dist){
    LOGD("Creating Hyp");
    float a,b;
    Matrix3f ret;

    a = (abs(tdoa) / 2) * CC;
    b = sqrt(pow((dist / 2), 2) - pow(a, 2));

    ret << 1. / pow(a, 2), 0.,              0.,
           0.            , -1. / pow(b, 2), 0.,
           0.            , 0.             , -1;

    return ret;
}

static Matrix3f transform_coordinate(Matrix3f old_c, float translation_x, float translation_y, float rot_angle){
    LOGD("Transform Hyp");
    Matrix3f T;
    T << cos(rot_angle), -sin(rot_angle) ,translation_x * cos(rot_angle) - translation_y * sin(rot_angle),
          sin(rot_angle), cos(rot_angle)  ,translation_x * sin(rot_angle) + translation_y * cos(rot_angle),
                                  0,0,1;

    return T.transpose() * old_c * T;
}

JNIEXPORT jfloatArray JNICALL Java_com_eneaceolini_EigenHelper_localization
        (JNIEnv *pEnv, jobject obj, jfloatArray tdoas, jfloat translation_x, jfloat translation_y, jfloat rotation, jfloat dist)
{

        // I for now process only 1 intersection
        float   *all_tdoas;
        jfloatArray resJNI;
        jfloat      *resArray;
        LOGD("Entered native function");



        jfloat *jtdoas = pEnv->GetFloatArrayElements(tdoas, 0);
        all_tdoas = (float*) jtdoas;

        LOGD("TDOA 1 = %.4f", all_tdoas[0]);
        LOGD("TDOA 2 = %.4f", all_tdoas[1]);
        LOGD("T_X = %.4f", translation_x);
        LOGD("T_Y = %.4f", translation_y);
        LOGD("ROTATION = %.4f", rotation);
        LOGD("DIST = %.4f", dist);
        Matrix3f reference_C = create_hyperbola(all_tdoas[0], dist);
        Matrix3f other_C = transform_coordinate(create_hyperbola(all_tdoas[1], dist), translation_x, translation_y, rotation);

        MatrixXf result = find_intersections(reference_C, other_C);


        int num_inter = result.cols();
        LOGD("Num Int %d", num_inter);

            //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "cojkjkadvjkb");
        resJNI   = pEnv->NewFloatArray(2 * num_inter);
        if (resJNI == NULL) {
            return NULL; /* out of memoEigen ry error thrown */
        }

        resArray = pEnv->GetFloatArrayElements(resJNI,0);
        for(int i = 0, j=0; i < num_inter; i++, j+=2){
            resArray[j] = result(0,i) / result(2,i);
            resArray[j+1] = result(1,i) / result(2,i);
        }
        LOGD("EXITING");
        pEnv->ReleaseFloatArrayElements(resJNI, resArray, 0);
        return resJNI;

}

}

