Skip to content
This repository  
Search
Pull requests
Issues
Gist
 @Enny1991
 Unwatch 1
  Star 0
  Fork 0 Enny1991/ExApp
 Code  Issues 0  Pull requests 0  Wiki  Pulse  Graphs  Settings
Branch: master Find file Copy pathExApp/app/jni/fftw3/Android.mk
bbdcb7a  on Aug 30, 2015
@Enny1991 Enny1991 add FFTW native library
1 contributor
RawBlameHistory     36 lines (30 sloc)  1.14 KB
LOCAL_PATH := $(call my-dir)
    include $(CLEAR_VARS)

    include $(LOCAL_PATH)/api/sources.mk
    include $(LOCAL_PATH)/dft/sources.mk
    include $(LOCAL_PATH)/dft/scalar/sources.mk
    include $(LOCAL_PATH)/dft/scalar/codelets/sources.mk
    include $(LOCAL_PATH)/kernel/sources.mk
    include $(LOCAL_PATH)/rdft/sources.mk
    include $(LOCAL_PATH)/rdft/scalar/sources.mk
    include $(LOCAL_PATH)/rdft/scalar/r2cb/sources.mk
    include $(LOCAL_PATH)/rdft/scalar/r2cf/sources.mk
    include $(LOCAL_PATH)/rdft/scalar/r2r/sources.mk
    include $(LOCAL_PATH)/reodft/sources.mk

    LOCAL_MODULE := fftw3
    LOCAL_C_INCLUDES := $(LOCAL_PATH) \
        $(LOCAL_PATH)/api \
        $(LOCAL_PATH)/dft \
        $(LOCAL_PATH)/dft/scalar \
        $(LOCAL_PATH)/dft/scalar/codelets \
        $(LOCAL_PATH)/kernel \
        $(LOCAL_PATH)/rdft \
        $(LOCAL_PATH)/rdft/scalar \
        $(LOCAL_PATH)/rdft/scalar/r2cb \
        $(LOCAL_PATH)/rdft/scalar/r2cf \
        $(LOCAL_PATH)/rdft/scalar/r2r \
        $(LOCAL_PATH)/reodft \
        $(LOCAL_PATH)/../../..

    # Use APP_OPTIM in Application.mk
    LOCAL_CFLAGS := -g


    include $(BUILD_SHARED_LIBRARY)
Status API Training Shop Blog About Pricing
© 2016 GitHub, Inc. Terms Privacy Security Contact Help