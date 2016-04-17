//
// Created by Enea Ceolini on 08/02/16.
//
#include <iostream>
#include <Eigen/Dense>
#include <math.h>
#include <jni.h>

using namespace Eigen;

Vector3f vec;
Vector3f vec2;
Vector3f vecRtrn;


void vecLoad(float x, float y, float z, float x2, float y2, float z2){

    vec(0) = x;
    vec(1) = y;
    vec(2) = z;
    vec2(0) = x2;
    vec2(1) = y2;
    vec2(2) = z2;

}

void vecAdd(Vector3f vecA, Vector3f vecB){
    vecRtrn = vecA + vecB;
}

extern "C"
{
JNIEXPORT jfloatArray JNICALL Java_com_eneaceolini_exapp_JNImathActivity_test
        (JNIEnv *env, jobject obj, jfloatArray fltarray1, jfloatArray fltarray2)
{

    jfloatArray result;
    result = env->NewFloatArray(3);
    if (result == NULL) {
        return NULL; /* out of memory error thrown */
    }

    jfloat array1[3];
    jfloat* flt1 = env->GetFloatArrayElements( fltarray1,0);
    jfloat* flt2 = env->GetFloatArrayElements( fltarray2,0);


    vecLoad(flt1[0], flt1[1], flt1[2], flt2[0], flt2[1], flt2[2]);
    vecAdd(vec, vec2);

    array1[0] = vecRtrn[0];
    array1[1] = vecRtrn[1];
    array1[2] = vecRtrn[2];

    env->ReleaseFloatArrayElements(fltarray1, flt1, 0);
    env->ReleaseFloatArrayElements(fltarray2, flt2, 0);
    env->SetFloatArrayRegion(result, 0, 3, array1);
    return result;

}
}