/*
 * Module:	JNI Interface
 * Author:	Lvjianfeng
 * Date:	2012.12
 */

#include <jni.h>
#include <stddef.h>

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
        )
{
    jint t_Return;
    jclass t_Class;
    JNIEnv *tp_Env;


    LOGD("Attach thread");

    if ((*m_tp_JavaVM)->AttachCurrentThread(m_tp_JavaVM, &tp_Env, NULL) < 0)
    {
        LOGE("Attach thread fail");
        return FUNCTION_FAIL;
    }

    if ((*tp_Env)->IsSameObject(tp_Env, m_t_Class, NULL))
    {
        LOGE("Object release");
        t_Return = (jint)FUNCTION_FAIL;
    }
    else
    {
        t_Class = (*tp_Env)->NewLocalRef(tp_Env, m_t_Class);
        t_Return = (*tp_Env)->CallStaticIntMethod(tp_Env, t_Class,
                                                  m_t_MethodIDHandleEvent, (jint)u8_Address, (jint)u8_SourcePort,
                                                  (jint)u8_TargetPort, (jint)u8_Event);
        (*tp_Env)->DeleteLocalRef(tp_Env, t_Class);
    }

    LOGD("Detach thread");

    if ((*m_tp_JavaVM)->DetachCurrentThread(m_tp_JavaVM) < 0)
    {
        LOGE("Detach thread fail");
        return FUNCTION_FAIL;
    }

    return (uint)t_Return;
}


static uint JNI_HandleCommand
(
	uint8 u8_Address,
	uint8 u8_SourcePort,
	uint8 u8_TargetPort,
	const task_comm_command *tp_Command,
	uint8 u8_Mode
)
{
	JNIEnv *tp_Env;
	jint t_Return;
	jclass t_Class;
	jbyteArray t_Data;


	LOGD("Attach thread");

	if ((*m_tp_JavaVM)->AttachCurrentThread(m_tp_JavaVM, &tp_Env, NULL) < 0)
	{
		LOGE("Attach thread fail");
		return FUNCTION_FAIL;
	}

	if (tp_Command->u8_Length > 0)
	{
		t_Data = (*tp_Env)->NewByteArray(tp_Env, (jsize)tp_Command->u8_Length);
		(*tp_Env)->SetByteArrayRegion(tp_Env, t_Data, 0,
			(jsize)tp_Command->u8_Length, (const jbyte *)tp_Command->u8p_Data);
	}
	else
	{
		t_Data = NULL;
	}


	if ((*tp_Env)->IsSameObject(tp_Env, m_t_Class, NULL))
	{
		LOGE("Object release");
		t_Return = (jint)FUNCTION_FAIL;
	}
	else
	{
		t_Class = (*tp_Env)->NewLocalRef(tp_Env, m_t_Class);
		t_Return = (*tp_Env)->CallStaticIntMethod(tp_Env, t_Class,
			m_t_MethodIDHandleCommand, (jint) u8_Address, (jint) u8_SourcePort,
			(jint) u8_TargetPort, (jint) u8_Mode,
			(jint) tp_Command->u8_Operation, (jint) tp_Command->u8_Parameter,
			t_Data);
		(*tp_Env)->DeleteLocalRef(tp_Env, t_Class);
	}

	if (t_Data != NULL)
	{
		(*tp_Env)->DeleteLocalRef(tp_Env, t_Data);
	}

	LOGD("Detach thread");

	if ((*m_tp_JavaVM)->DetachCurrentThread(m_tp_JavaVM) < 0)
	{
		LOGE("Detach thread fail");
		return FUNCTION_FAIL;
	}

	return (uint)t_Return;
}
