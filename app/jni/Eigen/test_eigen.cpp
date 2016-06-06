//
// Created by Enea Ceolini on 08/02/16.
//
#include <iostream>
#include <Eigen/Dense>
#include <math.h>
#include <jni.h>
#include <android/log.h>

#define LOG_TAG "EIGEN"
#define C 343 // 343 m/s
#define SWAP(a,b) do { double tmp = b ; b = a ; a = tmp ; } while(0)
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



JNIEXPORT jfloatArray JNICALL Java_com_eneaceolini_EigenHelper_localization
        (JNIEnv *pEnv, jobject obj, jfloatArray tdoas, jfloat translation_x, jfloat translation_y, jfloat rotation, jfloat dist)
{

        // I for now process only 1 intersection
        float   *all_tdoas;



        jfloat *jtdoas = pEnv->GetFloatArrayElements(tdoas, 0);
        all_tdoas = (float* jtdoas)

        Matrix3f reference_C = create_hyperbola(all_tdoas[0], dist);
        Matrix3f other_C = transform_coordinate(create_hyperbola(all_tdoas[1], dist), translation_x, translation_y, rotation);

        MatrixXf result = find_intersections(reference_c, other_C)



        // transform


}


static Matrix3f create_hyperbola(float tdoa, float dist){
    float a,b;
    Matrix3f ret;

    a = (abs(tdoa) / 2) * C
    b = sqrt(pow((dist / 2), 2) - pow(a, 2));

    ret << 1. / pow(a, 2), 0.,              0.,
           0.            , -1. / pow(b, 2), 0.,
           0.            , 0.             , -1;

    return ret;
}

static Matrix3f transform_coordinates(Matrix3f old_c, float* translation_x, float* translation_y, float rot_angle){
    Matrix3f T;
    T << cos(rot_angle), -sin(rot_angle) ,translation_x * cos(rot_angle) - translation_y * sin(rot_angle),
          sin(rot_angle), cos(rot_angle)  ,translation_x * sin(rot_angle) + translation_y * cos(rot_angle),
                                  0,0,1;

    return T
}

static MatrixXf find_intersection(Matrix3f C1, Matrix3f C2 Matrix){

    int r1 = C1.rank();
    int r2 = C2.rank();
    Vector
    if (r1 == 3 && r2 == 2){
        return complete_intersection(C1,C2);
    }
    else{
    if(r2 < 3){
        Matrix3f defE = C2;
        Matrix3f fullE = C1;
    }else{
        Matrix3f defE = C1;
        Matrix3f fullE = C2;
    }
    Vector3f l, m;
    decompose_degenerate_conic(defE, &m, &l);
    VectorXf P1 = intersect_conic_line(fullE,m);
    VectorXf P2 = intersect_conic_line(fullE,l);
    MatrixXf C(3, P1.cols() + P2.cols());
    C << P1, P2;
    }
    return C;

}

static MatrixXf complete_intersection(Matrix3f C1, Matrix3f C2){
    double *r1, *r2, *r3;
    Matrix3f _tmp_C2 = -C2
    Matrix3f EE = C1 * _tmp_C2.inverse();
    Matrix2f sub1C1 = EE.block(0,0,2,2);
    Matrix2f sub2C1 = EE.block(1,1,2,2);
    Matrix2f sub3C1;
    sub3C1 << EE(0,0), EE(0,2), EE(2,0), EE(2,2);
    solve(-EE.trace(), ( sub1C1.determinant() + sub2C1.determinant() + sub3C1.determinant() , -EE.determinant(), r1,r2,r3);
    // TODO check real
    Matrix3f E0(C1.rows(), C1.cols());
    E0 << C1 + *x1 * C2;
    Vector3f m,l;
    decompose_conic(E0, &m, &l);

    intersectConicLine(C1,m, &P1);
    intersectConicLine(C1,l, &P2);
    MatrixXf C(3, P1.cols() + P2.cols());
    C << P1, P2;
    return C;

}

static VectorXf intersect_conic_line(Matrix3X C, Vector2f l){

    Vector3f p1,p2;
    get_point_on_line(l, &p1, &p2);

    float p1Cp1 = p1.transpose() * C * p1;
    float p2Cp2 = p2.transpose() * C * p2;
    float p1Cp2 = p1.transpose() * C * p2;

        if (p2Cp2 == 0) %linear{
           k1 = -0.5 * p1Cp1 / p1Cp2;
           MatrixXf P(3,1);
           P << p1 + k1 * p2;
        }else{
            delta = pow(p1Cp2, 2) - p1Cp1 * p2Cp2;
            if (delta >= 0){
                float deltaSqrt = sqrt(delta);
                float k1 = (-p1Cp2 + deltaSqrt)/p2Cp2;
                float k2 = (-p1Cp2 - deltaSqrt)/p2Cp2;
                MatrixXf P(3,2);
                P << p1 + k1 * p2, p1 + k2 * p2;
            }
        }
    return P;

}

public void point_in_line(Vector2f l, Vector3f* p1, Vector3f* p2){
    if(l(0) == 0 && l(1) == 0)
    {
        *p1 << 1, 0, 0;
        *p2 << 0, 1, 0,
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

static float* solve(double a, double b, double c, double *x0, double *x1, double *x2){

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

static void decompose_conic(Matrix3f c, Vector3f* m, Vector3f* l )
{
    Matrix3f C;
    if (c.rank() == 1)
    {
    C = c;
    }
    else
    {
        MatrixXf::Index maxRow, maxCol;
        Matrix4f B = -adjoint_sym3(c);
        float max = m.diagonal().abs().maxCoeff(&maxRow);
        int i = maxRow;
        if(B(i,i) < 0){
            return;
        }
        double b = sqrt(B(i,i));
        MatrixXf p = B.block(0,i,3,1) / b;
        Matrix3f Mp = cross_matrix(p);
        C = c + Mp;


    }

    max = C.maxCoeff(&maxRow, &maxCol);

    *m << C(maxRow,0), C(maxRow,1), C(maxRow,2);
    *l << C(0,maxCol), C(1,maxCol), C(2, maxCol);
    return

}

static Matrix3f adjoint_sym3(Matrix3f M){
    Matrix3f A;
    int a,b,c,d,e,f;

    a = M(0,0); b = M(0,1); d = M(0,2);
                c = M(1,1); e = M(1,2);
                            f = M(2,2);

    A(0,0) = c*f - e*e;
    A(0,1) = -b*f + e*d;
    A(0,2) = b*e - c*d;

    A(1,0) = A(1,2);
    A(1,1) = a*f - d*d;
    A(1,2) = -a*e + b*d;

    A(2,0) = A(1,3);
    A(2,1) = A(2,3);
    A(2,2) = a*c - b*b;
    return A;

}

static MatrixXf cross_matrix(MatrixXf p)
    Matrix3f MP;
    MP << 0, p(0,2), -p(0,1),
        -p(0,2), 0, p(0,0),
        p(0,1), -p(0,0), 0;

    return MP
}

