/*
 * Module:	Global include file
 * Author:	Lvjianfeng
 * Date:	2011.8
 */

#ifndef _GLOBAL_H_
#define _GLOBAL_H_

#include <sys/types.h>
#include "android/log.h"
#include "devcomm_config.h"


//Constant define

#define LOG_ENABLE			1

#ifndef NULL
	#define NULL			0
#endif

#define REG_MASK_1_BIT		0x01
#define REG_MASK_2_BIT		0x03
#define REG_MASK_3_BIT		0x07
#define REG_MASK_4_BIT		0x0F
#define REG_MASK_5_BIT		0x1F
#define REG_MASK_6_BIT		0x3F
#define REG_MASK_7_BIT		0x7F
#define REG_MASK_8_BIT		0xFF

#define REG_WRITE_FIELD(reg, field, mask, value)	{(reg) = ((reg) & (~((mask) << (field)))) | \
													((value) << (field));}
#define REG_READ_FIELD(reg, field, mask)			(((reg) >> (field)) & (mask))
#define REG_SET_BIT(reg, field)						((reg) |= (1 << (field)))
#define REG_CLEAR_BIT(reg, field)					((reg) &= ~(1 << (field)))
#define REG_REVERSE_BIT(reg, field)					((reg) ^= (1 << (field)))
#define REG_GET_BIT(reg, field)						((reg) & (1 << (field)))

#if LOG_ENABLE != 0
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  __FUNCTION__, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, __FUNCTION__, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, __FUNCTION__, fmt, ##args)
#else
#define LOGI(fmt, args...)
#define LOGD(fmt, args...)
#define LOGE(fmt, args...)
#endif


//Type definition

typedef int8_t				int8;
typedef uint8_t				uint8;
typedef int8_t				sint8;
typedef int16_t				int16;
typedef uint16_t			uint16;
typedef int16_t				sint16;
typedef int32_t				int32;
typedef uint32_t			uint32;
typedef int32_t				sint32;
typedef int64_t				int64;
typedef uint64_t			uint64;
typedef int64_t				sint64;
typedef float				float32;
typedef double				float64;

//typedef uint32			uint;
//typedef sint32			sint;

typedef enum 
{
	FUNCTION_OK = 1,
	FUNCTION_FAIL = 0
} function_return;

#endif
