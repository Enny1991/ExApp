//
// Created by Enea Ceolini on 08/02/16.
//
#include <iostream>
#include <Eigen/Dense>
#include <math.h>
#include <jni.h>
#include <android/log.h>

#define LOG_TAG "EIGEN"

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


}