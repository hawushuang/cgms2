
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE	:=	jni_interface
LOCAL_SRC_FILES	:=	jni_interface.c \
					task_comm.c \
					driver/drv.c \
					driver/drv_led.c \
					driver/drv_uart.c \
					lib_checksum.c \
					lib_queue.c \
					lib_frame.c \
					devcomm.c
LOCAL_LDLIBS	:=	-llog

include $(BUILD_SHARED_LIBRARY)

APP_PLATFORM := android-9