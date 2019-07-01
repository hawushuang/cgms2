/*
 * Module:	JNI Interface
 * Author:	Lvjianfeng
 * Date:	2012.12
 */

#include <jni.h>
#include <stddef.h>
#include <stdlib.h>
#include <fcntl.h>

#include "driver/drv.h"
#include "task_comm.h"

#include "jni_interface.h"


//Constant definition


//Type definition


//Private variable definition

static JavaVM *m_tp_JavaVM = NULL;
static jclass m_t_Class = NULL;
static jmethodID m_t_MethodIDHandleEvent = NULL;
static jmethodID m_t_MethodIDHandleCommand = NULL;

static const char *m_t_ClassPath = "com/microtechmd/pda/control/platform/JNIInterface";


//Private function declaration

static void JNI_Initialize(void);

static void JNI_Finalize(void);

static uint JNI_HandleEvent
        (
                uint8 u8_Address,
                uint8 u8_SourcePort,
                uint8 u8_TargetPort,
                uint8 u8_Event
        );

static uint JNI_HandleCommand
        (
                uint8 u8_Address,
                uint8 u8_SourcePort,
                uint8 u8_TargetPort,
                const task_comm_command *tp_Command,
                uint8 u8_Mode
        );


//Public function definition

/*
 * Class:     com_microtechmd_pda_control_platform_JNIInterface
 * Method:    open
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_microtechmd_pda_control_platform_JNIInterface_open
        (JNIEnv *tp_Env, jobject t_This) {
}


/*
 * Class:     com_microtechmd_pda_control_platform_JNIInterface
 * Method:    close
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_microtechmd_pda_control_platform_JNIInterface_close
        (JNIEnv *tp_Env, jobject t_This) {
}

int volbtch[] = {
        4210, 4206, 4204, 4202, 4200, 4195, 4187, 4179, 4171, 4160, 4153,
        4149, 4143, 4139, 4138, 4135, 4131, 4130, 4120, 4111, 4105,
        4097, 4089, 4083, 4079, 4075, 4072, 4065, 4058, 4051, 4045,
        4038, 4035, 4028, 4026, 4020, 4016, 4010, 4007, 4003, 3997,
        3995, 3992, 3989, 3985, 3981, 3978, 3975, 3973, 3970, 3968,
        3965, 3963, 3962, 3961, 3960, 3959, 3958, 3957, 3956, 3955,
        3954, 3953, 3952, 3951, 3950, 3949, 3948, 3947, 3946, 3945,
        3944, 3943, 3942, 3941, 3940, 3938, 3936, 3933, 3930, 3926,
        3921, 3916, 3910, 3903, 3896, 3888, 3878, 3870, 3859, 3852,
        3843, 3833, 3825, 3814, 3806, 3796, 3784, 3768, 3570, 3550,
};
int volbt[] = {
        4120, 4110, 4102, 4091, 4080, 4070, 4061, 4052, 4043,
        4031, 4023, 4012, 4003, 3993, 3984, 3975, 3969, 3959, 3949,
        3941, 3934, 3925, 3916, 3908, 3899, 3891, 3885, 3876, 3867,
        3859, 3853, 3844, 3837, 3831, 3822, 3816, 3808, 3804, 3796,
        3790, 3786, 3779, 3773, 3769, 3764, 3759, 3754, 3750, 3746,
        3742, 3738, 3736, 3732, 3727, 3723, 3719, 3718, 3714, 3709,
        3706, 3705, 3700, 3700, 3696, 3695, 3691, 3691, 3689, 3686,
        3685, 3682, 3682, 3680, 3677, 3674, 3673, 3671, 3668, 3664,
        3663, 3659, 3655, 3651, 3646, 3642, 3637, 3631, 3623, 3617,
        3609, 3599, 3589, 3579, 3569, 3556, 3542, 3524, 3497, 3470,
        3438, 3400,
};

static int bty_value[20];
static int bty_frist = 0;
static int bty_index = 0;

int readFromFile(const char *path, char *buf, size_t size) {
    if (!path)
        return -1;
    int fd = open(path, O_RDONLY, 0);
    if (fd == -1) {
        return -1;
    }

    size_t count = (size_t) read(fd, buf, size);
    if (count > 0) {
        count = (count < size) ? count : size - 1;
        while (count > 0 && buf[count - 1] == '\n') count--;
        buf[count] = '\0';
    } else {
        buf[0] = '\0';
    }

    close(fd);
    return (int) count;
}

int get_btlevel(int value, int *bt) {
    int i = 0;
    for (i = 0; i < 100; i++) {
        if (value > bt[i]) break;
    }
    return 100 - i;
}

int get_btylevel(int value) {
    int BTY_MAX = 10;
    int temp = 0;
    int i;
    int bty_leve;
    if ((bty_frist == 0) && (get_btlevel(value, volbtch) > 0)) {
        for (i = 0; i < BTY_MAX; i++) {
            bty_value[i] = get_btlevel(value, volbt);
        }
        bty_frist = 1;
    }

    bty_value[bty_index] = get_btlevel(value, volbt);
    bty_index++;
    if (bty_index == BTY_MAX) {
        bty_index = 0;
    }

    for (i = 0; i < BTY_MAX; i++) {
        temp = bty_value[i] + temp;
    }
    for (i = 0; i < BTY_MAX; i++) {
        LOGE("lijun >>>value[%d]:%d", i, bty_value[i]);
    }

    bty_leve = temp / BTY_MAX;
    return bty_leve;
}

JNIEXPORT jint JNICALL Java_com_microtechmd_pda_control_platform_JNIInterface_battery
        (JNIEnv *tp_Env, jobject t_This) {
    int value = 30000, nvalue, i;
    const int SIZE = 5;
    char buf[SIZE];
    char status[SIZE];
    for (i = 0; i < 1; i++) {
        if (readFromFile("/sys/devices/platform/omap/ti_tscadc/tiadc/iio:device0/in_voltage7_raw",
                         buf, SIZE) > 0) {

            if (atoi(buf) < 0) {
                continue;
            }
            nvalue = atoi(buf) * 100 * 6 * 18 / ((2 << 11) - 1);
            value = value > nvalue ? nvalue : value;
        }
    }

    readFromFile("/sys/class/power_supply/tps65217-ac/status", status, SIZE);
    LOGE("电池status:%s", status);
    if (status[0] == 'C') {
        return get_btlevel(value, volbtch);
    } else {
        return get_btylevel(value);
    }
}


/*
 * Class:     com_microtechmd_pda_control_platform_JNIInterface
 * Method:    turnOff
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_microtechmd_pda_control_platform_JNIInterface_turnOff
        (JNIEnv *tp_Env, jobject t_This) {
    TaskComm_TurnOffEncryption();
}
/*
 * Class:     com_microtechmd_pda_control_platform_JNIInterface
 * Method:    ready
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_microtechmd_pda_control_platform_JNIInterface_ready
        (JNIEnv *tp_Env, jobject t_This, jbyteArray t_Data) {
    jbyte *tp_Data;
    if (t_Data != NULL) {
        tp_Data = (*tp_Env)->GetByteArrayElements(tp_Env, t_Data, NULL);
    } else {
        tp_Data = NULL;
    }
    TaskComm_ReadyForEncryption((uint8 *) tp_Data);
}


/*
 * Class:     com_microtechmd_pda_control_platform_JNIInterface
 * Method:    send
 * Signature: (IIIIII[B)I
 */
JNIEXPORT jint JNICALL Java_com_microtechmd_pda_control_platform_JNIInterface_send
        (JNIEnv *tp_Env, jobject t_This, jint t_Address, jint t_SourcePort,
         jint t_TargetPort, jint t_Mode, jint t_Operation, jint t_Parameter,
         jbyteArray t_Data) {
    jbyte *tp_Data;
    jint t_Length;
    uint ui_Return;
    task_comm_command t_Command;


    LOGD("Send command begin");

    if (t_Data != NULL) {
        t_Length = (*tp_Env)->GetArrayLength(tp_Env, t_Data);
        tp_Data = (*tp_Env)->GetByteArrayElements(tp_Env, t_Data, NULL);
    } else {
        t_Length = 0;
        tp_Data = NULL;
    }

    t_Command.u8_Operation = (uint8) t_Operation;
    t_Command.u8_Parameter = (uint8) t_Parameter;
    t_Command.u8p_Data = (uint8 *) tp_Data;
    t_Command.u8_Length = (uint8) t_Length;

    ui_Return = TaskComm_Send((uint8) t_Address, (uint8) t_SourcePort,
                              (uint8) t_TargetPort, &t_Command, (uint8) t_Mode);

    if (t_Data != NULL) {
        (*tp_Env)->ReleaseByteArrayElements(tp_Env, t_Data, tp_Data, 0);
    }

    LOGD("Send command end");

    return (jint) ui_Return;
}


/*
 * Class:     com_microtechmd_pda_control_platform_JNIInterface
 * Method:    query
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_microtechmd_pda_control_platform_JNIInterface_query
        (JNIEnv *tp_Env, jobject t_This, jint t_Address) {
    uint ui_Value;


    ui_Value = 0;

    if (TaskComm_GetConfig((uint) t_Address, TASK_COMM_PARAM_BUSY,
                           (void *) &ui_Value) != FUNCTION_OK) {
        return (jint) FUNCTION_FAIL;
    }

    if (ui_Value != 0) {
        return (jint) FUNCTION_FAIL;
    }

    return (jint) FUNCTION_OK;
}


/*
 * Class:     com_microtechmd_pda_control_platform_JNIInterface
 * Method:    switchLink
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_com_microtechmd_pda_control_platform_JNIInterface_switchLink
        (JNIEnv *tp_Env, jobject t_This, jint t_Address, jint t_Value) {
    uint ui_Value;


    ui_Value = (uint) t_Value;

    if (TaskComm_SetConfig((uint) t_Address, TASK_COMM_PARAM_LINK,
                           (void *) &ui_Value) != FUNCTION_OK) {
        return (jint) FUNCTION_FAIL;
    }

    return (jint) FUNCTION_OK;
}


/*
 * Class:     com_microtechmd_pda_control_platform_JNIInterface
 * Method:    setLED
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_com_microtechmd_pda_control_platform_JNIInterface_setLED
        (JNIEnv *tp_Env, jobject t_This, jint t_Color, jint t_Brightness) {
    DrvLED_Set((int) t_Color, (int) t_Brightness);
}


/*
 * Class:     com_microtechmd_pda_control_platform_JNIInterface
 * Method:    getLED
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_microtechmd_pda_control_platform_JNIInterface_getLED
        (JNIEnv *tp_Env, jobject t_This, jint t_Color) {
    return (jint) DrvLED_Get((int) t_Color);
}


JNIEXPORT jint JNI_OnLoad(JavaVM *tp_JavaVM, void *vp_Reserved) {
    JNIEnv *tp_Env;
    jclass t_Class;


    LOGD("Load JNI library begin");

    m_tp_JavaVM = tp_JavaVM;

    if ((*tp_JavaVM)->GetEnv(tp_JavaVM, (void **) &tp_Env, JNI_VERSION_1_4) !=
        JNI_OK) {
        return JNI_ERR;
    }

    t_Class = (*tp_Env)->FindClass(tp_Env, m_t_ClassPath);

    if (t_Class == NULL) {
        return JNI_ERR;
    }

    m_t_Class = (*tp_Env)->NewWeakGlobalRef(tp_Env, t_Class);

    if (m_t_Class == NULL) {
        return JNI_ERR;
    }

    m_t_MethodIDHandleEvent = (*tp_Env)->GetStaticMethodID(tp_Env, t_Class,
                                                           "handleEvent", "(IIII)I");

    if (m_t_MethodIDHandleEvent == NULL) {
        return JNI_ERR;
    }

    m_t_MethodIDHandleCommand = (*tp_Env)->GetStaticMethodID(tp_Env, t_Class,
                                                             "handleCommand", "(IIIIII[B)I");

    if (m_t_MethodIDHandleCommand == NULL) {
        return JNI_ERR;
    }

    JNI_Initialize();

    LOGD("Load JNI library end");

    return JNI_VERSION_1_4;
}


JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *tp_JavaVM, void *vp_Reserved) {
    JNIEnv *tp_Env;


    LOGD("Unload JNI library begin");

    JNI_Finalize();

    if ((*tp_JavaVM)->GetEnv(tp_JavaVM, (void **) &tp_Env, JNI_VERSION_1_4)) {
        return;
    }

    (*tp_Env)->DeleteWeakGlobalRef(tp_Env, m_t_Class);

    LOGD("Unload JNI library end");

    return;
}


//Private function definition

static void JNI_Initialize(void) {
    task_comm_callback t_Callback;


    Drv_Initialize();
    TaskComm_Initialize();

    t_Callback.fp_HandleEvent = JNI_HandleEvent;
    t_Callback.fp_HandleCommand = JNI_HandleCommand;
    TaskComm_SetConfig(0, TASK_COMM_PARAM_CALLBACK, (const void *) &t_Callback);
}


static void JNI_Finalize(void) {
    TaskComm_Finalize();
    Drv_Finalize();
}


static uint JNI_HandleEvent
        (
                uint8 u8_Address,
                uint8 u8_SourcePort,
                uint8 u8_TargetPort,
                uint8 u8_Event
        ) {
    jint t_Return;
    jclass t_Class;
    JNIEnv *tp_Env;


    LOGD("Attach thread");

    if ((*m_tp_JavaVM)->AttachCurrentThread(m_tp_JavaVM, &tp_Env, NULL) < 0) {
        LOGE("Attach thread fail");
        return FUNCTION_FAIL;
    }

    if ((*tp_Env)->IsSameObject(tp_Env, m_t_Class, NULL)) {
        LOGE("Object release");
        t_Return = (jint) FUNCTION_FAIL;
    } else {
        t_Class = (*tp_Env)->NewLocalRef(tp_Env, m_t_Class);
        t_Return = (*tp_Env)->CallStaticIntMethod(tp_Env, t_Class,
                                                  m_t_MethodIDHandleEvent, (jint) u8_Address,
                                                  (jint) u8_SourcePort,
                                                  (jint) u8_TargetPort, (jint) u8_Event);
        (*tp_Env)->DeleteLocalRef(tp_Env, t_Class);
    }

    LOGD("Detach thread");

    if ((*m_tp_JavaVM)->DetachCurrentThread(m_tp_JavaVM) < 0) {
        LOGE("Detach thread fail");
        return FUNCTION_FAIL;
    }

    return (uint) t_Return;
}


static uint JNI_HandleCommand
        (
                uint8 u8_Address,
                uint8 u8_SourcePort,
                uint8 u8_TargetPort,
                const task_comm_command *tp_Command,
                uint8 u8_Mode
        ) {
    JNIEnv *tp_Env;
    jint t_Return;
    jclass t_Class;
    jbyteArray t_Data;


    LOGD("Attach thread");

    if ((*m_tp_JavaVM)->AttachCurrentThread(m_tp_JavaVM, &tp_Env, NULL) < 0) {
        LOGE("Attach thread fail");
        return FUNCTION_FAIL;
    }

    if (tp_Command->u8_Length > 0) {
        t_Data = (*tp_Env)->NewByteArray(tp_Env, (jsize) tp_Command->u8_Length);
        (*tp_Env)->SetByteArrayRegion(tp_Env, t_Data, 0,
                                      (jsize) tp_Command->u8_Length,
                                      (const jbyte *) tp_Command->u8p_Data);
    } else {
        t_Data = NULL;
    }


    if ((*tp_Env)->IsSameObject(tp_Env, m_t_Class, NULL)) {
        LOGE("Object release");
        t_Return = (jint) FUNCTION_FAIL;
    } else {
        t_Class = (*tp_Env)->NewLocalRef(tp_Env, m_t_Class);
        t_Return = (*tp_Env)->CallStaticIntMethod(tp_Env, t_Class,
                                                  m_t_MethodIDHandleCommand, (jint) u8_Address,
                                                  (jint) u8_SourcePort,
                                                  (jint) u8_TargetPort, (jint) u8_Mode,
                                                  (jint) tp_Command->u8_Operation,
                                                  (jint) tp_Command->u8_Parameter,
                                                  t_Data);
        (*tp_Env)->DeleteLocalRef(tp_Env, t_Class);
    }

    if (t_Data != NULL) {
        (*tp_Env)->DeleteLocalRef(tp_Env, t_Data);
    }

    LOGD("Detach thread");

    if ((*m_tp_JavaVM)->DetachCurrentThread(m_tp_JavaVM) < 0) {
        LOGE("Detach thread fail");
        return FUNCTION_FAIL;
    }

    return (uint) t_Return;
}
