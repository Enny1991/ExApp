#include <jni.h>
#include <android/log.h>

#include <unistd.h>
#include <stdio.h>
#include <stdint.h>

#include <fftw3.h>
#include <math.h>

#define LOG_TAG "FFTW_JNI"
#define FS 44100
#define DIST 0.14
#define C 343

#define LOGD(...) \
    __android_log_print(ANDROID_LOG_DEBUG, "FFTW_DEBUG", __VA_ARGS__)

static int           count_exec          = 0;
static int           threads_enabled     = 0;
static int           threads_initialized = 0;
static fftw_complex  *alpha0 = NULL;
static fftw_complex  *out                = NULL;
static fftw_complex  *test_out          = NULL;
static fftw_complex  *out1                = NULL;
static int           out_size            = 0;
double               *out_res            = NULL;
double               *out_out            = NULL;
double               *test_out_out            = NULL;
double               *beam_out_out            = NULL;
static fftw_complex  *out2                = NULL;
static fftw_complex  *beam_out                = NULL;
static fftw_complex  *weg2                = NULL;
static fftw_complex  *weg1                = NULL;
double               *out_res2            = NULL;
double               *fCenter            = NULL;
double               *zeta            = NULL;
fftw_plan plan1_forward, plan2_forward, plan_backward,plan1_forward_b, plan2_forward_b, plan_backward_b;


static void log_callback(void* ptr, int level, const char* fmt, va_list vl) {
  __android_log_vprint(ANDROID_LOG_INFO, LOG_TAG, fmt, vl);
}

inline static void alloc_fftw(int num) {
    if (out) {
        fftw_free(out);
    }
    if (out2) {
        fftw_free(out2);
    }

    if (out_res) {
        fftw_free(out_res);
    }
    if (out_res2) {
        fftw_free(out_res2);
    }

    out_size = num;
    alpha0      = fftw_malloc(sizeof(fftw_complex));
    out1      = fftw_malloc(sizeof(fftw_complex) * (num/2 + 1));
    out_res  = fftw_malloc(sizeof(double) * num);
    out_out  = fftw_malloc(sizeof(double) * num);
    test_out_out  = fftw_malloc(sizeof(double) * num);
    beam_out_out  = fftw_malloc(sizeof(double) * num);
    out2      = fftw_malloc(sizeof(fftw_complex) * (num/2 + 1));
    beam_out      = fftw_malloc(sizeof(fftw_complex) * (num/2 + 1));
    fCenter      = fftw_malloc(sizeof(double) * (num/2 + 1));
    zeta      = fftw_malloc(sizeof(double) * (num/2 + 1));
    test_out      = fftw_malloc(sizeof(fftw_complex) * (num/2 + 1));
    weg1      = fftw_malloc(sizeof(fftw_complex) * (num/2 + 1));
    weg2      = fftw_malloc(sizeof(fftw_complex) * (num/2 + 1));

}

inline static void execute_fftw(double *in, int num) {
  fftw_plan plan;
  int i, j;

  if (out_size < num) {
    alloc_fftw(num);
  }

  plan = fftw_plan_dft_r2c_1d(num, in, out, FFTW_ESTIMATE);
  fftw_execute(plan);

  fftw_destroy_plan(plan);

  for (i = 0, j = 0; i < (num/2); i++, j+= 2) {
    out_res[j]   = out[i][0];
    out_res[j+1] = out[i][1];
  }

}

inline static void execute_fft_simple(double *in, int num) {
  fftw_plan plan;
  fftw_plan plan_backward;
  int i, j;

  if (out_size < num) {
    alloc_fftw(num);
  }

    LOGD("BEFORE r2c");

  plan = fftw_plan_dft_r2c_1d(num, in, test_out, FFTW_ESTIMATE);
  fftw_execute(plan);

    LOGD("FORWARD done");

   plan_backward = fftw_plan_dft_c2r_1d(num, test_out, test_out_out, FFTW_ESTIMATE);
   fftw_execute(plan_backward);

   LOGD("BACKWARD done");

   fftw_destroy_plan(plan);
   fftw_destroy_plan(plan_backward);

}

inline static void calculate_corr(double *in1, double *in2, int num) {


  int i, j;
    double tmp_re, tmp_im,ab;

//  if (out_size < num) {
//    alloc_fftw(num);
//  }

    plan1_forward = fftw_plan_dft_r2c_1d(num, in1, out1, FFTW_ESTIMATE);
    plan2_forward = fftw_plan_dft_r2c_1d(num, in2, out2, FFTW_ESTIMATE);
    if(plan1_forward == NULL)
        __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Forward1 FALSE");
    if(plan2_forward == NULL)
        __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Forward2 FALSE");
    fftw_execute(plan1_forward);
    fftw_execute(plan2_forward);



    //TODO add the generalized correlation
    //multiply
    for(i = 0; i < (num/2 + 1); i++){
        tmp_re = out1[i][0] * out2[i][0] + out1[i][1] * out2[i][1];
        tmp_im = out1[i][1] * out2[i][0] - out1[i][0] * out2[i][1];
        ab = sqrt(tmp_re * tmp_re + tmp_im * tmp_im);
        out2[i][0] = tmp_re / ab;
        out2[i][1] = tmp_im / ab;
        //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Executing FFTW %.4f %.4f", tmp_re, tmp_im);
    }

    if(plan1_forward == NULL)
        __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Forward1 FALSE");
    plan_backward = fftw_plan_dft_c2r_1d(num, out2, out_out, FFTW_ESTIMATE);
    fftw_execute(plan_backward);

    //fftw_destroy_plan(plan1_forward);
    //fftw_destroy_plan(plan2_forward);
    //fftw_destroy_plan(plan_backward);

}

inline static void delay_and_sum(double *in1, double *in2, int num, double theta) {
  //fftw_plan plan1_forward, plan2_forward, plan_backward;

  int i, j;

    plan1_forward_b = fftw_plan_dft_r2c_1d(num, in1, out1, FFTW_ESTIMATE);
    plan2_forward_b = fftw_plan_dft_r2c_1d(num, in2, out2, FFTW_ESTIMATE);

    fftw_execute(plan1_forward_b);
    fftw_execute(plan2_forward_b);

    // Calculating weight function

    alpha0[0][0] = 1. / ((double) 2);
    alpha0[0][1] = 0.;

    for(i = 0; i < (num/2 + 1); i++){
        fCenter[i] = ((double) FS / num) * i;

        zeta[i] = 2 * M_PI * fCenter[i] * DIST * sin(theta) / C;

        weg1[i][0] = cos(-zeta[i]) * 1. / ((double) 2);
        weg1[i][1] = sin(-zeta[i]) * 1. / ((double) 2);
        weg2[i][0] = alpha0[0][0];
        weg2[i][1] = alpha0[0][1];
    }

    //multiply
    for(i = 0; i < (num/2 + 1); i++){
        beam_out[i][0] = out1[i][0] * weg1[i][0] - out1[i][1] * weg1[i][1] + out2[i][0] * weg2[i][0] - out2[i][1] * weg2[i][1];
        beam_out[i][1] = out1[i][1] * weg1[i][0] + out1[i][0] * weg1[i][1] + out2[i][1] * weg2[i][0] + out2[i][0] * weg2[i][1];
        //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Executing FFTW %.4f %.4f", tmp_re, tmp_im);
    }


    plan_backward_b = fftw_plan_dft_c2r_1d(num, beam_out, beam_out_out, FFTW_ESTIMATE);
    fftw_execute(plan_backward_b);
    //fftw_destroy_plan(plan1_forward);
    //fftw_destroy_plan(plan2_forward);
    //fftw_destroy_plan(plan_backward);
}

inline static void delay_and_sub(double *in1, double *in2, int num, double theta) {
  //fftw_plan plan1_forward, plan2_forward, plan_backward;

  int i, j;

    plan1_forward_b = fftw_plan_dft_r2c_1d(num, in1, out1, FFTW_ESTIMATE);
    plan2_forward_b = fftw_plan_dft_r2c_1d(num, in2, out2, FFTW_ESTIMATE);

    fftw_execute(plan1_forward_b);
    fftw_execute(plan2_forward_b);

    // Calculating weight function

    alpha0[0][0] = 1. / ((double) 2);
    alpha0[0][1] = 0.;

    for(i = 0; i < (num/2 + 1); i++){
        fCenter[i] = ((double) FS / num) * i;

        zeta[i] = 2 * M_PI * fCenter[i] * DIST * sin(theta) / C;

        weg1[i][0] = cos(-zeta[i]) * 1. / ((double) 2);
        weg1[i][1] = sin(-zeta[i]) * 1. / ((double) 2);
        weg2[i][0] = alpha0[0][0];
        weg2[i][1] = alpha0[0][1];
    }

    //multiply
    for(i = 0; i < (num/2 + 1); i++){
        beam_out[i][0] = out1[i][0] * weg1[i][0] - out1[i][1] * weg1[i][1] - out2[i][0] * weg2[i][0] + out2[i][1] * weg2[i][1];
        beam_out[i][1] = out1[i][1] * weg1[i][0] + out1[i][0] * weg1[i][1] - out2[i][1] * weg2[i][0] - out2[i][0] * weg2[i][1];
        //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Executing FFTW %.4f %.4f", tmp_re, tmp_im);
    }


    plan_backward_b = fftw_plan_dft_c2r_1d(num, beam_out, beam_out_out, FFTW_ESTIMATE);
    fftw_execute(plan_backward_b);
    //fftw_destroy_plan(plan1_forward);
    //fftw_destroy_plan(plan2_forward);
    //fftw_destroy_plan(plan_backward);
}

// br.usp.ime.dspbenchmarking.algorithms.fftw
JNIEXPORT jboolean JNICALL Java_com_eneaceolini_fft_FFTW_areThreadsEnabled(JNIEnv *pEnv, jobject pObj) {
  return JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_eneaceolini_fft_FFTW_removeThreadsJNI(JNIEnv *pEnv, jobject pObj) {

}

JNIEXPORT void JNICALL Java_com_eneaceolini_fft_FFTW_initThreadsJNI(JNIEnv *pEnv, jobject pObj, jint num_of_threads) {

}

JNIEXPORT void JNICALL Java_com_eneaceolini_fft_FFTW_setup(JNIEnv *pEnv, jobject pObj, jint length) {

        alloc_fftw(length);

}


JNIEXPORT void JNICALL Java_com_eneaceolini_fft_FFTW_corrJNI(JNIEnv *pEnv, jobject pObj, jdoubleArray in1,  jdoubleArray in2, jdoubleArray buffer) {

        jdouble      *elements;
        jdouble      *elements_2;
        jdouble      *elements_3;
        double       *real1;
        double       *real2;
        double       *real3;
        jboolean     isCopy1, isCopy2;
        jint         n_elemens;
        jdoubleArray resJNI;
        jdouble      *resArray;
        int i, len, n_elements;

        elements   = (*pEnv)->GetDoubleArrayElements(pEnv, in1, &isCopy1);
        n_elements = (*pEnv)->GetArrayLength(pEnv, in1);
        elements_2   = (*pEnv)->GetDoubleArrayElements(pEnv, in2, &isCopy1);
        elements_3   = (*pEnv)->GetDoubleArrayElements(pEnv, buffer, &isCopy1);

        //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Executing FFTW %d %d %d", threads_enabled, threads_initialized,
        //                    n_elements);

        real1 = (double*) elements;
        real2 = (double*) elements_2;
        real3 = (double*) elements_3;

        calculate_corr(real1, real2, n_elements);
        len = n_elements;
        //resJNI   = (*pEnv)->NewDoubleArray(pEnv, len);
        //resArray = (*pEnv)->GetDoubleArrayElements(pEnv, resJNI, &isCopy2);
        for (i = 0; i < len; i++) {
            real3[i] = out_out[i] / len;
            //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Executing FFTW %.4f", out_out[i]);
        }
        (*pEnv)->ReleaseDoubleArrayElements(pEnv, in1, elements, JNI_FALSE);
        (*pEnv)->ReleaseDoubleArrayElements(pEnv, in2, elements_2, JNI_FALSE);
        (*pEnv)->ReleaseDoubleArrayElements(pEnv, buffer, elements_3, JNI_FALSE);
        //(*pEnv)->ReleaseDoubleArrayElements(pEnv, resJNI, resArray, JNI_FALSE);
        return ;
}

JNIEXPORT void JNICALL Java_com_eneaceolini_fft_FFTW_DelayAndSum(JNIEnv *pEnv, jobject pObj, jdoubleArray in1,  jdoubleArray in2, jdoubleArray buffer, jdouble theta, jboolean sum) {

    jdouble      *elements;
    jdouble      *elements_2;
    jdouble      *elements_3;
    double       *real1;
    double       *real2;
    double       *real3;
    jboolean     isCopy1, isCopy2;
    jint         n_elemens;
    jdoubleArray resJNI;
    jdouble      *resArray;
    int i, len, n_elements;

    elements   = (*pEnv)->GetDoubleArrayElements(pEnv, in1, &isCopy1);
    n_elements = (*pEnv)->GetArrayLength(pEnv, in1);
    elements_2   = (*pEnv)->GetDoubleArrayElements(pEnv, in2, &isCopy1);
    elements_3   = (*pEnv)->GetDoubleArrayElements(pEnv, buffer, &isCopy1);

    //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Executing FFTW %d %d %d", threads_enabled, threads_initialized,
    //                    n_elements);

    real1 = (double*) elements;
    real2 = (double*) elements_2;
    real3 = (double*) elements_3;
    if(sum)
        delay_and_sum(real1, real2, n_elements, theta * M_PI / 180);
    else
        delay_and_sub(real1, real2, n_elements, theta * M_PI / 180);

    len = n_elements;
    //resJNI   = (*pEnv)->NewDoubleArray(pEnv, len);
    //resArray = (*pEnv)->GetDoubleArrayElements(pEnv, resJNI, &isCopy2);
    for (i = 0; i < len; i++) {
        real3[i] = beam_out_out[i] / len;
    }
    (*pEnv)->ReleaseDoubleArrayElements(pEnv, in1, elements, JNI_FALSE);
    (*pEnv)->ReleaseDoubleArrayElements(pEnv, in2, elements_2, JNI_FALSE);
    (*pEnv)->ReleaseDoubleArrayElements(pEnv, buffer, elements_3, JNI_FALSE);
    //(*pEnv)->ReleaseDoubleArrayElements(pEnv, resJNI, resArray, JNI_FALSE);
    return ;
}

JNIEXPORT jdoubleArray JNICALL Java_com_eneaceolini_fft_FFTW_TEST(JNIEnv *pEnv, jobject pObj, jdoubleArray in1) {

    jdouble      *elements;
    jdouble      *elements_2;
    double       *real1;
    double       *real2;
    jboolean     isCopy1, isCopy2;
    jint         n_elemens;
    jdoubleArray resJNI;
    jdouble      *resArray;
    int i, len, n_elements;

    elements   = (*pEnv)->GetDoubleArrayElements(pEnv, in1, &isCopy1);
    n_elements = (*pEnv)->GetArrayLength(pEnv, in1);

    //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Executing FFTW %d %d %d", threads_enabled, threads_initialized,
    //                    n_elements);

    real1 = (double*) elements;
    LOGD("IN THE FUNC");
    execute_fft_simple(real1, n_elements);
    len = n_elements;
    resJNI   = (*pEnv)->NewDoubleArray(pEnv, len);
    resArray = (*pEnv)->GetDoubleArrayElements(pEnv, resJNI, &isCopy2);
    for (i = 0; i < len; i++) {
        resArray[i] = test_out_out[i] / len;
        //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Executing FFTW %.4f", out_out[i]);
    }
    (*pEnv)->ReleaseDoubleArrayElements(pEnv, in1, elements, JNI_FALSE);
    (*pEnv)->ReleaseDoubleArrayElements(pEnv, resJNI, resArray, JNI_FALSE);
    return resJNI;
}
