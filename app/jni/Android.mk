LOCAL_PATH := $(call my-dir)
SUPERPOWERED_PATH := $(LOCAL_PATH)/Superpowered



include $(CLEAR_VARS)
LOCAL_MODULE    := test_eigen
LOCAL_SRC_FILES := Eigen/test_eigen.cpp
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog
include $(BUILD_SHARED_LIBRARY)    # this actually builds libmod1.so

include $(CLEAR_VARS)
#include $(LOCAL_PATH)/fftw3/project/jni/Android.mk
include $(LOCAL_PATH)/fftw3/api/sources.mk
include $(LOCAL_PATH)/fftw3/dft/sources.mk
include $(LOCAL_PATH)/fftw3/dft/scalar/sources.mk
include $(LOCAL_PATH)/fftw3/dft/scalar/codelets/sources.mk
include $(LOCAL_PATH)/fftw3/kernel/sources.mk
include $(LOCAL_PATH)/fftw3/rdft/sources.mk
include $(LOCAL_PATH)/fftw3/rdft/scalar/sources.mk
include $(LOCAL_PATH)/fftw3/rdft/scalar/r2cb/sources.mk
include $(LOCAL_PATH)/fftw3/rdft/scalar/r2cf/sources.mk
include $(LOCAL_PATH)/fftw3/rdft/scalar/r2r/sources.mk
include $(LOCAL_PATH)/fftw3/reodft/sources.mk

# static library info
LOCAL_MODULE := fftw3
LOCAL_SRC_FILES := fftw3/build/lib/libfftw3.a
LOCAL_EXPORT_C_INCLUDES := fftw3/build/include
LOCAL_C_INCLUDES := $(LOCAL_PATH) \
        $(LOCAL_PATH)/fft3w/api \
        $(LOCAL_PATH)/fft3w/dft \
        $(LOCAL_PATH)/fft3w/dft/scalar \
        $(LOCAL_PATH)/fft3w/dft/scalar/codelets \
        $(LOCAL_PATH)/fft3w/kernel \
        $(LOCAL_PATH)/fft3w/rdft \
        $(LOCAL_PATH)/fft3w/rdft/scalar \
        $(LOCAL_PATH)/fft3w/rdft/scalar/r2cb \
        $(LOCAL_PATH)/fft3w/rdft/scalar/r2cf \
        $(LOCAL_PATH)/fft3w/rdft/scalar/r2r \
        $(LOCAL_PATH)/fft3w/reodft \

    # Use APP_OPTIM in Application.mk
    LOCAL_CFLAGS := -g
include $(PREBUILT_STATIC_LIBRARY)

# wrapper info
include $(CLEAR_VARS)
LOCAL_MODULE    := fftw_jni
LOCAL_SRC_FILES := fftw3/vuild/lib/libfftw3.a
LOCAL_C_INCLUDES := $(LOCAL_PATH) \
        $(LOCAL_PATH)/fft3w/api \
        $(LOCAL_PATH)/fft3w/dft \
        $(LOCAL_PATH)/fft3w/dft/scalar \
        $(LOCAL_PATH)/fft3w/dft/scalar/codelets \
        $(LOCAL_PATH)/fft3w/kernel \
        $(LOCAL_PATH)/fft3w/rdft \
        $(LOCAL_PATH)/fft3w/rdft/scalar \
        $(LOCAL_PATH)/fft3w/rdft/scalar/r2cb \
        $(LOCAL_PATH)/fft3w/rdft/scalar/r2cf \
        $(LOCAL_PATH)/fft3w/rdft/scalar/r2r \
        $(LOCAL_PATH)/fft3w/reodft \
        $(LOCAL_PATH)/fft3w/build/include \

LOCAL_SRC_FILES := fftw_jni.c
LOCAL_STATIC_LIBRARIES := fftw3
LOCAL_LDLIBS    := -llog -lz -lm $(LOCAL_PATH)/fftw3/build/lib/libfftw3.a
include $(BUILD_SHARED_LIBRARY)





