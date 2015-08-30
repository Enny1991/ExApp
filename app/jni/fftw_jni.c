#include <jni.h>
#include <android/log.h>

#include <unistd.h>
#include <stdio.h>
#include <stdint.h>

#include <fftw3.h>
#include <math.h>

#define LOG_TAG "FFTW_JNI"

static int           count_exec          = 0;
static int           threads_enabled     = 0;
static int           threads_initialized = 0;
static fftw_complex  *out                = NULL;
static int           out_size            = 0;
double               *out_res            = NULL;

static void log_callback(void* ptr, int level, const char* fmt, va_list vl) {
  __android_log_vprint(ANDROID_LOG_INFO, LOG_TAG, fmt, vl);
}

inline static void alloc_fftw(int num) {
  if (out) {
    fftw_free(out);
  }

  if (out_res) {
    fftw_free(out_res);
  }

  out_size = num;
  out      = fftw_malloc(sizeof(fftw_complex) * (num/2 + 1));
  out_res  = fftw_malloc(sizeof(double) * (num/2 + 1) * 2);
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

// br.usp.ime.dspbenchmarking.algorithms.fftw
JNIEXPORT jboolean JNICALL Java_com_eneaceolini_exapp_FFTW_areThreadsEnabled(JNIEnv *pEnv, jobject pObj) {
  return JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_eneaceolini_exapp_FFTW_removeThreadsJNI(JNIEnv *pEnv, jobject pObj) {

}

JNIEXPORT void JNICALL Java_com_eneaceolini_exapp_FFTW_initThreadsJNI(JNIEnv *pEnv, jobject pObj, jint num_of_threads) {

}

JNIEXPORT jdoubleArray JNICALL Java_com_eneaceolini_exapp_FFTW_executeJNI(JNIEnv *pEnv, jobject pObj, jdoubleArray in) {

  jdouble      *elements;
  double       *real;
  jboolean     isCopy1, isCopy2;
  jint         n_elemens;
  jdoubleArray resJNI;
  jdouble      *resArray;
  int i, len, n_elements;

  elements   = (*pEnv)->GetDoubleArrayElements(pEnv, in, &isCopy1);
  n_elements = (*pEnv)->GetArrayLength(pEnv, in);

  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Executing FFTW %d %d %d", threads_enabled, threads_initialized,
                      n_elements);

  real = (double*) elements;

  execute_fftw(real, n_elements);

  len = (n_elements/2 + 1) * 2;
  resJNI   = (*pEnv)->NewDoubleArray(pEnv, len);
  resArray = (*pEnv)->GetDoubleArrayElements(pEnv, resJNI, &isCopy2);

  for (i = 0; i < len; i++) {
    resArray[i] = out_res[i];
  }

  (*pEnv)->ReleaseDoubleArrayElements(pEnv, in, elements, JNI_FALSE);
  (*pEnv)->ReleaseDoubleArrayElements(pEnv, resJNI, resArray, JNI_FALSE);

  return resJNI;
}